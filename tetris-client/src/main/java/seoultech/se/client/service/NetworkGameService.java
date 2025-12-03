package seoultech.se.client.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javafx.application.Platform;
import seoultech.se.backend.mapper.GameStateDtoToGameStateMapper;
import seoultech.se.backend.mapper.GameStateMapper;
import seoultech.se.backend.network.P2PService;
import seoultech.se.core.GameState;
import seoultech.se.core.command.Direction;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.command.MoveCommand;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.dto.P2PPacket;
import seoultech.se.core.dto.PlayerInputDto;
import seoultech.se.core.dto.ServerStateDto;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.TetrominoType;
import seoultech.se.core.random.RandomGenerator;
import seoultech.se.core.random.TetrominoGenerator;

/**
 * P2P 게임 로직 관리 서비스 (Host Logic 포함)
 * 
 * 역할:
 * - 호스트: 게임 로직 수행 (GameSession 로직 복제 및 간소화)
 * - 게스트: 입력 전송 및 상태 수신
 */
@Service
public class NetworkGameService {

    @Autowired private P2PService p2pService;
    @Autowired private GameStateMapper gameStateMapper; // Server Mapper (State -> DTO)
    @Autowired private GameStateDtoToGameStateMapper dtoToStateMapper; // Client Mapper (DTO -> State)

    private GameEngine gameEngine;
    private boolean isHost;
    private volatile boolean isRunning;
    
    // 호스트가 관리하는 두 개의 상태
    private GameState myState; // Host State (Player A)
    private GameState opponentState; // Guest State (Player B)
    
    // 블록 생성기 및 대기열 관리
    private final Map<Boolean, TetrominoGenerator> generators = new HashMap<>(); // true=Host, false=Guest
    private final Map<Boolean, Integer> pendingAttacks = new HashMap<>(); // 공격 라인
    
    // 틱 관리
    private long lastTickTimeMy;
    private long lastTickTimeOpponent;
    
    // 콜백 (UI 업데이트용)
    private Consumer<GameState> onMyStateUpdate;
    private Consumer<GameState> onOpponentStateUpdate;
    private Consumer<Boolean> onGameResult; // true=Win, false=Lose

    private Consumer<Void> onGameStart;
    private volatile boolean isConnected = false;

    /**
     * P2P 게임 시작 (대기 상태 진입)
     */
    public void startP2PGame(boolean isHost, Consumer<GameState> onMyStateUpdate, Consumer<GameState> onOpponentStateUpdate, Consumer<Void> onGameStart, Consumer<Boolean> onGameResult) {
        this.isHost = isHost;
        this.onMyStateUpdate = onMyStateUpdate;
        this.onOpponentStateUpdate = onOpponentStateUpdate;
        this.onGameStart = onGameStart;
        this.onGameResult = onGameResult;
        this.isRunning = true;
        this.isConnected = false;
        
        // 1. 패킷 수신 리스너 설정
        p2pService.setOnPacketReceived(this::handlePacket);
        
        if (isHost) {
            System.out.println("👑 [P2P] HOST waiting for connection...");
            initializeHostGame();
            // Host는 Guest의 HANDSHAKE를 기다림 (게임 루프는 연결 후 시작되어야 함)
        } else {
            System.out.println("👤 [P2P] GUEST connecting...");
            // Guest는 HANDSHAKE 전송
            sendHandshake();
        }
    }

    private void sendHandshake() {
        // HANDSHAKE 패킷 전송 (실제 UDP 포트 포함)
        P2PPacket packet = new P2PPacket();
        packet.setType("HANDSHAKE");
        packet.setUdpPort(p2pService.getLocalPort()); // 실제 UDP 리스닝 포트 전달
        p2pService.sendPacket(packet);
        System.out.println("📡 [P2P] Sent HANDSHAKE with UDP port: " + p2pService.getLocalPort());
    }

    /**
     * 패킷 수신 처리 (메인 스레드 아님)
     */
    private void handlePacket(P2PPacket packet) {
        System.out.println("📬 [P2P " + (isHost ? "Host" : "Guest") + "] Packet received: " + packet.getType());
        
        if ("HANDSHAKE".equals(packet.getType())) {
            if (isHost && !isConnected) {
                System.out.println("✅ [P2P] Handshake received from Guest!");
                
                // 릴레이 모드가 아니면 Guest의 실제 UDP 포트로 재연결
                if (!p2pService.isRelayMode() && packet.getUdpPort() != null) {
                    String guestIp = p2pService.getOpponentIp();
                    if (guestIp == null) {
                        System.err.println("❌ [P2P Host] Cannot reconnect - Guest IP is null!");
                        return;
                    }
                    int guestUdpPort = packet.getUdpPort();
                    p2pService.connectToPeer(guestIp, guestUdpPort);
                    System.out.println("🔄 [P2P Host] Reconnected to Guest's UDP port: " + guestUdpPort);
                } else if (p2pService.isRelayMode()) {
                    System.out.println("🔄 [Relay] Already connected via relay server - skipping reconnect");
                }
                isConnected = true;
                sendHandshake(); // ACK with Host's UDP port
                startGameLoop();
                // 즉시 초기 상태 전송
                broadcastState();
            } else if (!isHost && !isConnected) {
                System.out.println("✅ [P2P] Handshake received from Host!");
                
                // 릴레이 모드가 아니면 Host의 실제 UDP 포트로 재연결
                if (!p2pService.isRelayMode() && packet.getUdpPort() != null) {
                    String hostIp = p2pService.getOpponentIp();
                    if (hostIp == null) {
                        System.err.println("❌ [P2P Guest] Cannot reconnect - Host IP is null!");
                        return;
                    }
                    int hostUdpPort = packet.getUdpPort();
                    p2pService.connectToPeer(hostIp, hostUdpPort);
                    System.out.println("🔄 [P2P Guest] Reconnected to Host's UDP port: " + hostUdpPort);
                } else if (p2pService.isRelayMode()) {
                    System.out.println("🔄 [Relay] Already connected via relay server - skipping reconnect");
                }
                isConnected = true;
                notifyGameStart();
                System.out.println("🎮 [P2P Guest] Waiting for initial STATE from Host...");
            }
        } else if (isConnected) {
            if ("INPUT".equals(packet.getType()) && isHost) {
                System.out.println("📥 [P2P Host] INPUT packet received!");
                if (packet.getInput() != null && packet.getInput().getCommand() != null) {
                    System.out.println("   └ Command: " + packet.getInput().getCommand().getType());
                }
                processGuestInput(packet.getInput());
            } else if ("STATE".equals(packet.getType()) && !isHost) {
                System.out.println("📥 [P2P Guest] STATE packet detected, processing...");
                ServerStateDto state = packet.getState();
                if (state != null) {
                    System.out.println("   └ STATE details: myGameState=" + (state.getMyGameState() != null) + 
                        ", opponentGameState=" + (state.getOpponentGameState() != null));
                }
                processStateUpdate(state);
            } else if ("GAME_OVER".equals(packet.getType())) {
                System.out.println("💀 [P2P] GAME_OVER packet received from opponent");

                // 게임 루프 즉시 중지
                isRunning = false;
                System.out.println("🛑 [P2P] Game loop stopped (isRunning = false)");

                if (packet.getIsWinner() != null) {
                    boolean amIWinner = packet.getIsWinner();

                    // 승패 여부와 관계없이 상대방 게임 오버 상태 업데이트
                    handleOpponentGameOver();

                    Platform.runLater(() -> {
                        if (onGameResult != null) {
                            System.out.println("💀 [P2P] Calling onGameResult with: " + amIWinner);
                            onGameResult.accept(amIWinner);
                        }
                    });
                } else {
                    // isWinner가 null인 경우에도 상대방이 죽었으므로 나는 승리
                    handleOpponentGameOver();
                    Platform.runLater(() -> {
                        if (onGameResult != null) {
                            System.out.println("💀 [P2P] Calling onGameResult with: true (null case)");
                            onGameResult.accept(true); // 상대방 사망 = 나 승리
                        }
                    });
                }
            } else {
                System.out.println("⚠️ [P2P] Unhandled packet - Type: " + packet.getType() + ", isHost: " + isHost + ", isConnected: " + isConnected);
            }
        } else {
            System.out.println("⚠️ [P2P] Packet ignored - not connected yet");
        }
    }

    private void startGameLoop() {
        notifyGameStart();
        new Thread(this::hostGameLoop).start();
    }

    private void notifyGameStart() {
        if (onGameStart != null) {
            Platform.runLater(() -> onGameStart.accept(null));
        }
    }
    
    /**
     * 호스트 초기화 (게임 엔진 및 상태 생성)
     */
    private void initializeHostGame() {
        // 1. 엔진 생성 (Classic, Normal)
        GameModeConfig config = GameModeConfig.createDefaultClassic();
        this.gameEngine = new seoultech.se.core.engine.ClassicGameEngine(config);
        
        // 2. 생성기 초기화
        seoultech.se.core.model.enumType.Difficulty difficulty = config.getDifficulty();
        generators.put(true, new TetrominoGenerator(new RandomGenerator(), difficulty));
        generators.put(false, new TetrominoGenerator(new RandomGenerator(), difficulty));
        
        pendingAttacks.put(true, 0);
        pendingAttacks.put(false, 0);
        
        // 3. 초기 상태 생성 및 첫 블록 스폰
        this.myState = new GameState(10, 20); // Host
        spawnNextBlock(this.myState, true);
        
        this.opponentState = new GameState(10, 20); // Guest
        spawnNextBlock(this.opponentState, false);
        
        lastTickTimeMy = System.currentTimeMillis();
        lastTickTimeOpponent = System.currentTimeMillis();
        
        // 4. 게임 루프 시작은 startGameLoop()에서 함
    }
    
    /**
     * [Guest] 서버 상태 수신 및 UI 반영
     */
    private void processStateUpdate(ServerStateDto dto) {
        if (dto == null) {
            System.err.println("⚠️ [P2P Guest] Received null state!");
            return;
        }
        
        // DTO -> GameState 변환
        // Host가 이미 Guest 관점으로 보냈으므로 그대로 사용
        // dto.myGameState = Guest 자신의 상태
        // dto.opponentGameState = Host의 상태
        GameState myNewState = dtoToStateMapper.toGameState(dto.getMyGameState());
        GameState oppNewState = dtoToStateMapper.toGameState(dto.getOpponentGameState());
        
        System.out.println("📦 [P2P Guest] State received - My: " + (myNewState != null) + ", Opp: " + (oppNewState != null));
        if (myNewState != null) {
            System.out.println("   └ My state details: currentTetromino=" + (myNewState.getCurrentTetromino() != null) + 
                ", x=" + myNewState.getCurrentX() + ", y=" + myNewState.getCurrentY() +
                ", score=" + myNewState.getScore() + ", lines=" + myNewState.getLinesCleared());
            
            // Grid 확인
            int filledCells = 0;
            if (myNewState.getGrid() != null) {
                for (int row = 0; row < myNewState.getGrid().length; row++) {
                    for (int col = 0; col < myNewState.getGrid()[row].length; col++) {
                        if (myNewState.getGrid()[row][col] != null && myNewState.getGrid()[row][col].isOccupied()) {
                            filledCells++;
                        }
                    }
                }
            }
            System.out.println("   └ Grid: filled cells = " + filledCells);
        }
        
        // UI 업데이트 (Platform.runLater)
        Platform.runLater(() -> {
            if (onMyStateUpdate != null && myNewState != null) {
                System.out.println("🔄 [P2P Guest] Calling myStateUpdate callback...");
                onMyStateUpdate.accept(myNewState);
                System.out.println("🎮 [P2P Guest] My state updated");
            } else {
                if (onMyStateUpdate == null) System.err.println("❌ [P2P Guest] onMyStateUpdate callback is NULL!");
                if (myNewState == null) System.err.println("❌ [P2P Guest] myNewState is NULL!");
            }
            if (onOpponentStateUpdate != null && oppNewState != null) {
                onOpponentStateUpdate.accept(oppNewState);
                System.out.println("👥 [P2P Guest] Opponent state updated");
            }
        });
    }
    
    /**
     * [Host] 게임 루프
     */
    private void hostGameLoop() {
        while (isRunning) {
            try {
                // 게임오버 체크 (양쪽 중 하나라도 끝나면 루프 종료)
                if (myState != null && myState.isGameOver()) {
                    System.out.println("🛑 [P2P Host] Host game over detected in loop");
                    isRunning = false;
                    // Host가 죽음 -> 결과 전송 및 팝업 표시
                    sendGameResult(true); // Guest에게 "너 이김" 전송
                    Platform.runLater(() -> {
                        System.out.println("💀 [P2P Host] Triggering Local Game Result: LOSE (from loop)");
                        if (onGameResult != null) {
                            onGameResult.accept(false); // 나(Host)는 패배
                        }
                    });
                    break;
                }

                if (opponentState != null && opponentState.isGameOver()) {
                    System.out.println("🛑 [P2P Host] Guest game over detected in loop");
                    isRunning = false;
                    // Guest가 죽음 -> 결과 전송 및 팝업 표시
                    sendGameResult(false); // Guest에게 "너 짐" 전송
                    Platform.runLater(() -> {
                        System.out.println("💀 [P2P Host] Triggering Local Game Result: WIN (from loop)");
                        if (onGameResult != null) {
                            onGameResult.accept(true); // 나(Host)는 승리
                        }
                    });
                    break;
                }

                long currentTime = System.currentTimeMillis();

                // 1. 중력 적용 (Host & Guest) - 개별 틱 타임 관리
                processGravity(myState, true, currentTime);
                processGravity(opponentState, false, currentTime);

                // 2. 상태 전송 (Host -> Guest)
                broadcastState();

                // 3. Host UI 업데이트
                Platform.runLater(() -> {
                    if (onMyStateUpdate != null) onMyStateUpdate.accept(myState);
                    if (onOpponentStateUpdate != null) onOpponentStateUpdate.accept(opponentState);
                });

                Thread.sleep(50); // 50ms Tick (20fps)

            } catch (InterruptedException e) {
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("🛑 [P2P Host] Game loop ended");
    }
    
    /**
     * 다음 블록 생성 및 스폰 (GameSession과 동일한 로직)
     */
    private void spawnNextBlock(GameState state, boolean isHostPlayer) {
        TetrominoGenerator generator = generators.get(isHostPlayer);
        if (generator == null) {
            System.err.println("❌ [P2P] No generator for player: " + (isHostPlayer ? "Host" : "Guest"));
            return;
        }

        // 새 테트로미노 생성
        TetrominoType nextType = generator.next();
        Tetromino newTetromino = new Tetromino(nextType);

        // 초기 위치 설정
        int startX = (state.getBoardWidth() - newTetromino.getCurrentShape()[0].length) / 2;
        int startY = 0;

        state.setCurrentTetromino(newTetromino);
        state.setCurrentX(startX);
        state.setCurrentY(startY);
        state.setHoldUsedThisTurn(false); // 새 블록이므로 Hold 재사용 가능

        // 아이템 타입 설정 (있다면)
        state.setCurrentItemType(state.getNextBlockItemType());
        state.setNextBlockItemType(null);
        state.setWeightBombLocked(false); // 무게추 초기화

        // Next Queue 업데이트 (표시용)
        TetrominoType[] queue = state.getNextQueue();
        // TetrominoGenerator는 peekNext 메서드가 없으므로 간단히 기본값으로 설정
        for (int i = 0; i < queue.length; i++) {
            queue[i] = TetrominoType.I; // 기본값
        }
        
        // 게임 오버 체크 (블록이 스폰 위치에서 충돌하는 경우)
        if (state.isGameOver()) {
            System.out.println("💀 [P2P] Game Over detected in spawnNextBlock");
            System.out.println("   └ Player: " + (isHostPlayer ? "Host" : "Guest"));
            System.out.println("   └ GameOverReason: " + state.getGameOverReason());

            if (isHostPlayer) {
                // Host가 죽음 -> Host 패배, Guest 승리
                System.out.println("💀 [P2P Host] Sending GAME_OVER to Guest (isWinner=true)");
                sendGameResult(true); // Guest에게 "너 이김" 전송
                Platform.runLater(() -> {
                    System.out.println("💀 [P2P Host] Triggering Local Game Result: LOSE");
                    if (onGameResult != null) {
                        onGameResult.accept(false); // 나(Host)는 패배
                    } else {
                        System.err.println("❌ [P2P Host] onGameResult callback is NULL!");
                    }
                });
            } else {
                // Guest가 죽음 -> Guest 패배, Host 승리
                System.out.println("💀 [P2P Host] Guest died - Sending GAME_OVER to Guest (isWinner=false)");
                sendGameResult(false); // Guest에게 "너 짐" 전송
                Platform.runLater(() -> {
                    System.out.println("💀 [P2P Host] Triggering Local Game Result: WIN");
                    if (onGameResult != null) {
                        onGameResult.accept(true); // 나(Host)는 승리
                    } else {
                        System.err.println("❌ [P2P Host] onGameResult callback is NULL!");
                    }
                });
            }
            isRunning = false;
            System.out.println("🛑 [P2P] isRunning set to false");
        }
    }
    
    /**
     * 중력 처리 (개별 플레이어)
     */
    private void processGravity(GameState state, boolean isHostPlayer, long currentTime) {
        if (state == null || state.isGameOver()) return;
        
        long lastTick = isHostPlayer ? lastTickTimeMy : lastTickTimeOpponent;
        // 레벨에 따른 속도 계산 (간소화: 기본 1초, 레벨당 0.1초 감소)
        long interval = Math.max(100, 1000 - (state.getLevel() - 1) * 100);
        
        if (currentTime - lastTick >= interval) {
            GameCommand down = new MoveCommand(Direction.DOWN);
            executeAndCheck(down, state, isHostPlayer);
            
            if (isHostPlayer) lastTickTimeMy = currentTime;
            else lastTickTimeOpponent = currentTime;
        }
    }
    
    /**
     * 명령 실행 및 후처리 (블록 잠금, 줄 삭제, 공격 등)
     */
    private void executeAndCheck(GameCommand command, GameState state, boolean isHostPlayer) {
        GameState next = gameEngine.executeCommand(command, state);
        
        if (next != null) {
            // 상태 갱신 (여기서는 참조가 변경되지 않고 내부 상태만 변경됨을 가정)
            // 하지만 GameEngine은 새 객체를 반환할 수도 있으므로 덮어쓰기 필요
            if (isHostPlayer) myState = next;
            else opponentState = next;
            state = next; // 지역 변수 갱신
            
            // 블록이 잠겼는지 확인 (currentTetromino가 null이 됨)
            if (state.getCurrentTetromino() == null && !state.isGameOver()) {
                // 줄 삭제 및 공격 처리
                processAttackLines(state, isHostPlayer);
                
                // 새 블록 생성
                spawnNextBlock(state, isHostPlayer);
            }
        }
    }
    
    /**
     * 공격 라인 처리 (Host Logic)
     */
    private void processAttackLines(GameState state, boolean isAttackerHost) {
        int linesCleared = state.getLastLinesCleared();
        if (linesCleared > 1) {
            int attackAmount = linesCleared - 1;
            boolean targetIsHost = !isAttackerHost;
            
            // 공격 대기열에 추가
            pendingAttacks.merge(targetIsHost, attackAmount, Integer::sum);
            System.out.println("⚔️ [P2P] Attack: " + (isAttackerHost ? "Host" : "Guest") + " -> " + attackAmount + " lines");
        }
        
        // 나에게 온 공격 처리 (블록 잠글 때 처리)
        int received = pendingAttacks.get(isAttackerHost);
        if (received > 0) {
            pendingAttacks.put(isAttackerHost, 0);
            state.addGarbageLines(received);
            System.out.println("🛡️ [P2P] " + (isAttackerHost ? "Host" : "Guest") + " received " + received + " lines");
        }
    }
    
    /**
     * [Host] 상태 브로드캐스트
     */
    private void broadcastState() {
        // Guest 입장에서의 상태 DTO 생성 (My=Guest, Opponent=Host)
        ServerStateDto guestDto = ServerStateDto.builder()
            .myGameState(gameStateMapper.toDto(opponentState, 0))
            .opponentGameState(gameStateMapper.toDto(myState, 0))
            .gameOver(myState.isGameOver() || opponentState.isGameOver())
            .events(new ArrayList<>()) // 이벤트는 별도 처리 필요하지만 일단 빈 리스트
            .build();
        
        System.out.println("📤 [P2P Host] Broadcasting STATE - Guest.currentTetromino: " + 
            (opponentState.getCurrentTetromino() != null) + ", Host.currentTetromino: " + 
            (myState.getCurrentTetromino() != null));
            
        p2pService.sendState(guestDto);
    }
    
    /**
     * [Host] 게스트 입력 처리
     */
    private void processGuestInput(PlayerInputDto input) {
        if (!isRunning) return;
        if (input == null || opponentState == null || opponentState.isGameOver()) {
            System.out.println("⚠️ [P2P Host] Cannot process guest input - input:" + (input != null) + ", state:" + (opponentState != null));
            return;
        }
        
        System.out.println("📨 [P2P Host] Processing guest input: " + input.getCommand().getType());
        executeAndCheck(input.getCommand(), opponentState, false);
        broadcastState(); // 즉시 반응성 위해 전송
    }
    
    /**
     * [Common] 내 입력 전송
     */
    public void sendMyInput(GameCommand command) {
        if (!isRunning) return;
        if (isHost) {
            // 호스트: 내 입력 즉시 처리
            if (myState == null || myState.isGameOver()) return;
            System.out.println("⌨️ [P2P Host] Input: " + command.getType());
            executeAndCheck(command, myState, true);
            
            // UI 즉시 업데이트
            Platform.runLater(() -> {
                if (onMyStateUpdate != null) onMyStateUpdate.accept(myState);
            });
            broadcastState();
            
        } else {
            // 게스트: 입력 전송
            System.out.println("⌨️ [P2P Guest] Sending input: " + command.getType());
            PlayerInputDto input = PlayerInputDto.builder()
                .command(command)
                .build();
            p2pService.sendInput(input);
        }
    }
    
    public void stop() {
        isRunning = false;
        p2pService.close();
        System.out.println("🛑 [NetworkGameService] Stopped");
    }
    
    /**
     * 게임 오버 결과 전송
     */
    private void sendGameResult(boolean isWinnerForRecipient) {
        System.out.println("📡 [P2P] sendGameResult called - isWinner for recipient: " + isWinnerForRecipient);
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    P2PPacket packet = P2PPacket.builder()
                        .type("GAME_OVER")
                        .gameOver(true)
                        .isWinner(isWinnerForRecipient)
                        .build();
                    p2pService.sendPacket(packet);
                    System.out.println("📤 [P2P] Sent GAME_OVER packet (Attempt " + (i+1) + "/10, isWinner=" + isWinnerForRecipient + ")");
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    System.out.println("⚠️ [P2P] sendGameResult interrupted at attempt " + (i+1));
                    break;
                }
            }
            System.out.println("✅ [P2P] Finished sending GAME_OVER packets (10 attempts completed)");
        }).start();
    }
    
    /**
     * 상대방 게임 오버 처리
     */
    private void handleOpponentGameOver() {
        Platform.runLater(() -> {
            System.out.println("💀 [P2P] Opponent game over received");
            if (isHost && opponentState != null) {
                opponentState.setGameOver(true);
                System.out.println("💀 [P2P Host] Guest game over - updating opponent state");
                if (onOpponentStateUpdate != null) {
                    onOpponentStateUpdate.accept(opponentState);
                }
            } else if (!isHost && opponentState != null) {
                opponentState.setGameOver(true);
                System.out.println("💀 [P2P Guest] Host game over - updating opponent state");
                if (onOpponentStateUpdate != null) {
                    onOpponentStateUpdate.accept(opponentState);
                }
            }
        });
    }
    
    public void setOnDisconnect(Runnable callback) {
        // TODO: Implement disconnect detection (timeout or packet)
    }
}
