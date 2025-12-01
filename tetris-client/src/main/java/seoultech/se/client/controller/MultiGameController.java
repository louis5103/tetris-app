package seoultech.se.client.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import seoultech.se.client.service.GameApiService;
import seoultech.se.client.strategy.NetworkExecutionStrategy;
import seoultech.se.client.ui.OpponentBoardView;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;

@Component
@Scope("prototype")
public class MultiGameController extends BaseGameController {

    @Autowired
    private GameApiService gameApiService;

    private NetworkExecutionStrategy executionStrategy;
    private OpponentBoardView opponentBoardView;
    private String sessionId;

    /**
     * 멀티플레이 모드 초기화 (외부 호출)
     */
    public void initMultiplayer(seoultech.se.client.strategy.NetworkExecutionStrategy strategy, String sessionId) {
        this.executionStrategy = strategy;
        this.sessionId = sessionId;
        
        // NetworkCallback 연결
        strategy.setupMultiplayMode(
            sessionId,
            boardController.getGameState(),
            this::onMyStateUpdate,
            this::onOpponentStateUpdate,
            this::onAttackLinesReceived
        );
        
        boardController.setExecutionStrategy(strategy);
        
        // 서버에 게임 시작 알림
        if (gameApiService != null) {
            gameApiService.startGame(sessionId);
        }
    }

    @Override
    protected void onInitComplete() {
        System.out.println("🌐 [MultiGameController] Initializing Multiplayer Mode...");
        
        // 상대방 보드 설정
        this.opponentBoardView = new OpponentBoardView();
        if (opponentContainer != null) {
            opponentContainer.getChildren().setAll(opponentBoardView);
            opponentContainer.setVisible(true);
            opponentContainer.setManaged(true);
        }
        
        // 아이템 인벤토리 숨김 (멀티플레이는 아이템 미지원 가정, 혹은 추후 추가)
        if (itemInventoryContainer != null) {
            itemInventoryContainer.setVisible(false);
            itemInventoryContainer.setManaged(false);
        }
        
        // 멀티플레이는 Pause 불가
        if (inputHandler != null) {
            inputHandler.setMultiplayerMode(true);
        }
    }

    @Override
    public void startGame() {
        System.out.println("▶️ [MultiGameController] Game Started (Server Auth)");
        if (gameOverLabel != null) gameOverLabel.setVisible(false);
        popupManager.hideAllPopups();
        boardGridPane.requestFocus();
        // 멀티플레이는 GameLoopManager를 사용하지 않음 (서버 중력)
    }

    @Override
    public void cleanup() {
        System.out.println("🧹 [MultiGameController] Cleanup");
        if (executionStrategy != null) {
            executionStrategy.cleanup();
        }
        // ✅ 입력 차단 제거: cleanup()은 게임 종료 시 호출되며, InputHandler의 isGameOver() 체크로 자동 차단됨
    }

    @Override
    protected void handleCommand(GameCommand command) {
        // Client-side Prediction (NetworkExecutionStrategy가 처리)
        GameState oldState = boardController.getGameState().deepCopy(); // 예측용 복사본은 불필요할 수 있으나 UI 갱신용
        // executeCommand 내부에서 서버 전송 및 로컬 예측 상태 반환
        GameState predictedState = boardController.executeCommand(command);
        
        // 로컬 예측 렌더링 (반응성 향상)
        updateUI(oldState, predictedState);
    }

    // --- Network Callbacks ---

    private void onMyStateUpdate(GameState newState) {
        Platform.runLater(() -> {
            // 서버 상태로 보정 (Reconciliation)
            GameState oldState = boardController.getGameState();
            boardController.setGameState(newState);
            updateUI(oldState, newState);
        });
    }

    private void onOpponentStateUpdate(GameState opponentState) {
        Platform.runLater(() -> {
            if (opponentBoardView != null) {
                opponentBoardView.update(opponentState);
            }
        });
    }

    private void onAttackLinesReceived(int lines) {
        Platform.runLater(() -> {
            notificationManager.showAttackNotification(lines);
        });
    }
    
    // 멀티플레이는 Pause 불가
    @Override protected void onPause() {}
    @Override protected void onResume() {}
}
