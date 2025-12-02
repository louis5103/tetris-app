package seoultech.se.client.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javafx.application.Platform;
import seoultech.se.backend.mapper.GameStateDtoToGameStateMapper;
import seoultech.se.backend.mapper.GameStateMapper;
import seoultech.se.backend.network.P2PService;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
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

    /**
     * P2P 게임 시작
     */
    public void startP2PGame(boolean isHost, Consumer<GameState> onMyStateUpdate, Consumer<GameState> onOpponentStateUpdate) {
        this.isHost = isHost;
        this.onMyStateUpdate = onMyStateUpdate;
        this.onOpponentStateUpdate = onOpponentStateUpdate;
        this.isRunning = true;
        
        // 1. 패킷 수신 리스너 설정
        p2pService.setOnPacketReceived(this::handlePacket);
        
        if (isHost) {
            System.out.println("👑 [P2P] Starting as HOST");
            initializeHostGame();
        } else {
            System.out.println("👤 [P2P] Starting as GUEST");
        }
    }
    
    /**
     * 호스트 초기화 (게임 엔진 및 상태 생성)
     */
    private void initializeHostGame() {
        // 1. 엔진 생성 (Classic, Normal)
        GameModeConfig config = GameModeConfig.createDefaultClassic();
        this.gameEngine = new seoultech.se.core.engine.impl.ClassicGameEngine(config);
        
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
        
        // 4. 게임 루프 시작
        new Thread(this::hostGameLoop).start();
    }
    
    /**
     * 다음 블록 생성 및 스폰 (GameSession 로직 복제)
     */
    private void spawnNextBlock(GameState state, boolean isHostPlayer) {
        TetrominoGenerator generator = generators.get(isHostPlayer);
        if (generator == null) return;

        // 새 테트로미노 생성
        TetrominoType nextType = generator.next();
        Tetromino newTetromino = new Tetromino(nextType);

        // 초기 위치 설정
        int startX = (state.getBoardWidth() - newTetromino.getCurrentShape()[0].length) / 2;
        int startY = 0;

        state.setCurrentTetromino(newTetromino);
        state.setCurrentX(startX);
        state.setCurrentY(startY);
        state.setHoldUsedThisTurn(false);

        // 아이템 타입 설정 (있다면)
        state.setCurrentItemType(state.getNextBlockItemType());
        state.setNextBlockItemType(null);
        state.setWeightBombLocked(false);

        // Next Queue 업데이트 (표시용)
        TetrominoType[] queue = state.getNextQueue();
        // TetrominoGenerator는 peekNext 메서드가 없으므로 간단히 기본값으로 설정
        // 실제로는 Generator를 개선하여 Preview를 지원해야 함 (현재는 I로 고정)
        for (int i = 0; i < queue.length; i++) {
            queue[i] = TetrominoType.I; 
        }
    }
    
    /**
     * 패킷 수신 처리 (메인 스레드 아님)
     */
    private void handlePacket(P2PPacket packet) {
        if ("INPUT".equals(packet.getType()) && isHost) {
            // 호스트: 게스트의 입력 수신 -> 처리
            processGuestInput(packet.getInput());
        } else if ("STATE".equals(packet.getType()) && !isHost) {
            // 게스트: 호스트가 보낸 상태 수신 -> UI 업데이트
            processStateUpdate(packet.getState());
        }
    }
    
    /**
     * [Guest] 서버 상태 수신 및 UI 반영
     */
    private void processStateUpdate(ServerStateDto dto) {
        if (dto == null) return;
        
        // DTO -> GameState 변환
        GameState myNewState = dtoToStateMapper.toGameState(dto.getMyGameState());
        GameState oppNewState = dtoToStateMapper.toGameState(dto.getOpponentGameState());
        
        // UI 업데이트 (Platform.runLater)
        Platform.runLater(() -> {
            if (onMyStateUpdate != null && myNewState != null) onMyStateUpdate.accept(myNewState);
            if (onOpponentStateUpdate != null && oppNewState != null) onOpponentStateUpdate.accept(oppNewState);
        });
    }
    
    /**
     * [Host] 게임 루프
     */
    private void hostGameLoop() {
        while (isRunning) {
            try {
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
            seoultech.se.core.command.GameCommand down = new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.DOWN);
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
            
        p2pService.sendState(guestDto);
    }
    
    /**
     * [Host] 게스트 입력 처리
     */
    private void processGuestInput(PlayerInputDto input) {
        if (input == null || opponentState == null || opponentState.isGameOver()) return;
        
        executeAndCheck(input.getCommand(), opponentState, false);
        broadcastState(); // 즉시 반응성 위해 전송
    }
    
    /**
     * [Common] 내 입력 전송
     */
    public void sendMyInput(GameCommand command) {
        if (isHost) {
            // 호스트: 내 입력 즉시 처리
            if (myState == null || myState.isGameOver()) return;
            executeAndCheck(command, myState, true);
            
            // UI 즉시 업데이트
            Platform.runLater(() -> {
                if (onMyStateUpdate != null) onMyStateUpdate.accept(myState);
            });
            broadcastState();
            
        } else {
            // 게스트: 입력 전송
            PlayerInputDto input = PlayerInputDto.builder()
                .command(command)
                .build();
            p2pService.sendInput(input);
        }
    }
    
    public void stop() {
        isRunning = false;
    }
}
