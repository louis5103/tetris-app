package seoultech.se.client.ui;

import java.util.Optional;

import javafx.scene.input.KeyEvent;
import seoultech.se.client.model.GameAction;
import seoultech.se.client.service.KeyMappingService;
import seoultech.se.core.command.Direction;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.command.HardDropCommand;
import seoultech.se.core.command.HoldCommand;
import seoultech.se.core.command.MoveCommand;
import seoultech.se.core.command.PauseCommand;
import seoultech.se.core.command.ResumeCommand;
import seoultech.se.core.command.RotateCommand;
import seoultech.se.core.model.enumType.RotationDirection;

/**
 * 키보드 입력을 처리하고 GameCommand로 변환하는 클래스
 * 
 * 이 클래스는 다음과 같은 작업을 수행합니다:
 * - 키보드 이벤트를 받아서 GameAction으로 변환 (KeyMappingService 사용)
 * - GameAction을 적절한 GameCommand로 변환
 * - 게임 상태에 따른 입력 필터링 (게임 오버 시 입력 무시)
 * 
 * GameController에서 입력 처리 책임을 분리하여
 * 단일 책임 원칙(SRP)을 준수합니다.
 */
public class InputHandler {
    
    /**
     * 입력 처리 콜백 인터페이스
     */
    @FunctionalInterface
    public interface InputCallback {
        /**
         * 유효한 커맨드가 생성되었을 때 호출됩니다
         * 
         * @param command 실행할 GameCommand
         */
        void onCommandGenerated(GameCommand command);
    }
    
    /**
     * 게임 상태 제공 인터페이스
     * 입력 필터링을 위해 필요한 최소한의 게임 상태만 제공
     */
    public interface GameStateProvider {
        /**
         * 게임이 종료되었는지 확인합니다
         * 
         * @return 게임 오버 상태면 true
         */
        boolean isGameOver();
        
        /**
         * 게임이 일시정지되었는지 확인합니다
         * 
         * @return 일시정지 상태면 true
         */
        boolean isPaused();
    }
    
    private final KeyMappingService keyMappingService;
    private InputCallback callback;
    private GameStateProvider gameStateProvider;
    
    /**
     * InputHandler 생성자
     * 
     * @param keyMappingService 키 매핑 서비스
     */
    public InputHandler(KeyMappingService keyMappingService) {
        this.keyMappingService = keyMappingService;
    }
    
    /**
     * 입력 콜백을 설정합니다
     * 
     * @param callback 입력 콜백
     */
    public void setCallback(InputCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 게임 상태 제공자를 설정합니다
     * 
     * @param gameStateProvider 게임 상태 제공자
     */
    public void setGameStateProvider(GameStateProvider gameStateProvider) {
        this.gameStateProvider = gameStateProvider;
    }
    
    /**
     * 키 입력을 처리하고 Command로 변환합니다
     * 
     * @param event 키보드 이벤트
     */
    public void handleKeyPress(KeyEvent event) {
        // 게임 오버 상태 체크
        if (gameStateProvider != null && gameStateProvider.isGameOver()) {
            return;
        }
        
        // KeyMappingService로 키를 GameAction으로 변환
        Optional<GameAction> actionOpt = keyMappingService.getAction(event.getCode());
        
        if (actionOpt.isEmpty()) {
            return; // 매핑되지 않은 키는 무시
        }
        
        GameAction action = actionOpt.get();
        
        // 일시정지 상태 체크: PAUSE_RESUME 액션만 허용
        if (gameStateProvider != null && gameStateProvider.isPaused()) {
            if (action != GameAction.PAUSE_RESUME) {
                return; // 일시정지 중에는 PAUSE_RESUME 외의 키 무시
            }
        }
        
        GameCommand command = createCommandFromAction(action);
        
        // Command가 생성되었으면 콜백 호출
        if (command != null && callback != null) {
            callback.onCommandGenerated(command);
        }
        
        event.consume();
    }
    
    /**
     * GameAction을 GameCommand로 변환합니다
     * 
     * @param action 게임 액션
     * @return 생성된 GameCommand, 변환 불가능한 경우 null
     */
    private GameCommand createCommandFromAction(GameAction action) {
        switch (action) {
            case MOVE_LEFT:
                return new MoveCommand(Direction.LEFT);
                
            case MOVE_RIGHT:
                return new MoveCommand(Direction.RIGHT);
                
            case MOVE_DOWN:
                return new MoveCommand(Direction.DOWN);
                
            case ROTATE_CLOCKWISE:
                return new RotateCommand(RotationDirection.CLOCKWISE);
                
            case ROTATE_COUNTER_CLOCKWISE:
                return new RotateCommand(RotationDirection.COUNTER_CLOCKWISE);
                
            case HARD_DROP:
                return new HardDropCommand();
                
            case HOLD:
                return new HoldCommand();
                
            case PAUSE_RESUME:
                // Pause/Resume 토글
                if (gameStateProvider != null && gameStateProvider.isPaused()) {
                    return new ResumeCommand();
                } else {
                    return new PauseCommand();
                }
                
            default:
                return null;
        }
    }
    
    /**
     * 키보드 컨트롤을 씬에 설정합니다
     * 
     * @param gridPane 키 이벤트를 받을 GridPane (게임 보드)
     */
    public void setupKeyboardControls(javafx.scene.layout.GridPane gridPane) {
        if (gridPane.getScene() != null) {
            // Scene이 이미 존재하면 즉시 설정
            gridPane.getScene().setOnKeyPressed(this::handleKeyPress);
            System.out.println("⌨️  Keyboard controls enabled");
        } else {
            // Scene이 아직 없으면 리스너로 대기 (한 번만 등록)
            gridPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && oldScene == null) {
                    newScene.setOnKeyPressed(this::handleKeyPress);
                    System.out.println("⌨️  Keyboard controls enabled");
                }
            });
        }
    }
}
