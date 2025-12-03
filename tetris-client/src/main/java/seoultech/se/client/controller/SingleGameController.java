package seoultech.se.client.controller;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import seoultech.se.client.strategy.LocalExecutionStrategy;
import seoultech.se.client.ui.GameLoopManager;
import seoultech.se.core.GameState;
import seoultech.se.core.command.Direction;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.command.MoveCommand;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.engine.factory.GameEngineFactory;

@Component
@Scope("prototype")
public class SingleGameController extends BaseGameController {

    private GameLoopManager gameLoopManager;
    private LocalExecutionStrategy executionStrategy;

    @Override
    protected void onInitComplete() {
        System.out.println("🎮 [SingleGameController] Initializing Single Player Mode...");
        
        // 1. 전략 설정
        GameEngineFactory factory = new GameEngineFactory();
        GameEngine gameEngine = factory.createGameEngine(gameModeConfig);
        this.executionStrategy = new LocalExecutionStrategy(gameEngine);
        boardController.setExecutionStrategy(executionStrategy);
        
        // 2. 게임 루프 설정
        this.gameLoopManager = new GameLoopManager(gameModeConfig.getDropSpeedMultiplier());
        this.gameLoopManager.setCallback(this::onGameLoopTick);
        
        // 3. 상대방 보드 숨김 (싱글 플레이에서는 불필요)
        if (opponentContainer != null) {
            opponentContainer.setVisible(false);
            opponentContainer.setManaged(false);
        }
    }

    @Override
    public void startGame() {
        System.out.println("▶️ [SingleGameController] Starting Game Loop");
        if (gameOverLabel != null) gameOverLabel.setVisible(false);
        popupManager.hideAllPopups();
        if (gameLoopManager != null) gameLoopManager.start();
        boardGridPane.requestFocus();
    }

    @Override
    public void cleanup() {
        System.out.println("🧹 [SingleGameController] Cleanup");
        if (gameLoopManager != null) {
            gameLoopManager.stop();
            gameLoopManager = null;
        }
        // ✅ 입력 차단 제거: cleanup()은 게임 종료 시 호출되며, InputHandler의 isGameOver() 체크로 자동 차단됨
    }

    @Override
    protected void handleCommand(GameCommand command) {
        GameState oldState = boardController.getGameState().deepCopy();
        GameState newState = boardController.executeCommand(command);
        
        updateUI(oldState, newState);
    }
    
    private boolean onGameLoopTick() {
        // Skip gravity while UI animations are in progress
        if (isAnimating()) {
            System.out.println("⏯️ [Loop] Gravity tick skipped (animating)");
            return true;
        }
        GameState currentState = boardController.getGameState();
        if (currentState.isGameOver()) return false;
        if (currentState.isPaused()) return true;
        
        // 중력 적용
        GameState oldState = currentState.deepCopy();
        GameState newState = boardController.executeCommand(new MoveCommand(Direction.DOWN));
        
        updateUI(oldState, newState);
        
        return true;
    }
    


    @Override
    protected void onPause() {
        if (gameLoopManager != null) gameLoopManager.pause();
        notificationManager.showLineClearType("⏸️ PAUSED");
    }

    @Override
    protected void onResume() {
        if (gameLoopManager != null) gameLoopManager.resume();
        notificationManager.hideAllNotifications();
        boardController.executeCommand(new seoultech.se.core.command.ResumeCommand());
    }
    
    // ✅ 애니메이션 훅 제거: 애니메이션은 이제 UI 전용이므로 게임 루프를 차단하지 않음
    // onLineClearAnimationStart(), onLineClearAnimationEnd() 오버라이드 제거됨

    // 아이템 시스템: autoUse=true로 설정되어 있어 자동으로 즉시 적용됨
}
