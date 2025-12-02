package seoultech.se.client.service;

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
import seoultech.se.core.engine.factory.GameEngineFactory;
import seoultech.se.core.model.enumType.Difficulty;

/**
 * P2P 게임 로직 관리 서비스 (Host Logic 포함)
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
    private GameState myState; // Host State
    private GameState opponentState; // Guest State
    
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
        // 1. 엔진 생성 (Classic, Normal) - TODO: 설정 연동
        GameModeConfig config = GameModeConfig.createDefaultClassic();
        // GameEngineFactory는 stateless 엔진을 반환하므로 new 사용 가능 (또는 Bean 주입)
        // 여기서는 편의상 Core 내부 구현체 직접 사용 (주의: 의존성)
        this.gameEngine = new seoultech.se.core.engine.impl.ClassicGameEngine(config);
        
        // 2. 초기 상태 생성
        this.myState = new GameState(10, 20); // Host
        this.opponentState = new GameState(10, 20); // Guest
        
        // 3. 첫 블록 생성 등 초기화 로직 (간소화)
        // 실제로는 GameSession.spawnNextBlock() 로직이 필요함.
        // 여기서는 생략하거나 GameSession 로직을 복사해야 함.
        
        // 4. 게임 루프 시작
        new Thread(this::hostGameLoop).start();
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
                Thread.sleep(100); // 100ms Tick
                
                // 1. 중력 적용 (Host & Guest)
                applyGravity(myState);
                applyGravity(opponentState);
                
                // 2. 상태 전송 (Host -> Guest)
                broadcastState();
                
                // 3. Host UI 업데이트
                Platform.runLater(() -> {
                    if (onMyStateUpdate != null) onMyStateUpdate.accept(myState);
                    if (onOpponentStateUpdate != null) onOpponentStateUpdate.accept(opponentState);
                });
                
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void applyGravity(GameState state) {
        if (state == null || state.isGameOver()) return;
        
        seoultech.se.core.command.GameCommand down = new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.DOWN);
        GameState next = gameEngine.executeCommand(down, state);
        
        // 상태 업데이트 (간소화: 불변성 고려 필요하지만 여기선 덮어쓰기)
        // 실제로는 GameSession처럼 복잡한 로직(Locking, Spawning) 필요
        if (next != null) {
            // state = next; // 참조 변경은 지역변수라 안됨. 필드 업데이트 필요하지만 구조상 복잡.
            // 임시: 그냥 덮어쓰기 (GameState가 Mutable하다면)
            // GameEngine은 새로운 State를 반환하므로, 필드를 업데이트해야 함.
            if (state == myState) myState = next;
            else opponentState = next;
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
            .build();
            
        p2pService.sendState(guestDto);
    }
    
    /**
     * [Host] 게스트 입력 처리
     */
    private void processGuestInput(PlayerInputDto input) {
        if (input == null || opponentState == null) return;
        
        GameState next = gameEngine.executeCommand(input.getCommand(), opponentState);
        if (next != null) opponentState = next;
        
        broadcastState(); // 즉시 반응성 위해 전송
    }
    
    /**
     * [Common] 내 입력 전송
     */
    public void sendMyInput(GameCommand command) {
        if (isHost) {
            // 호스트: 내 입력 즉시 처리
            GameState next = gameEngine.executeCommand(command, myState);
            if (next != null) myState = next;
            
            // UI 즉시 업데이트 & 게스트에게 알림
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