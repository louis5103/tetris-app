package seoultech.se.client.ui;

import javafx.animation.AnimationTimer;
import seoultech.se.client.constants.UIConstants;
import seoultech.se.core.GameState;

/**
 * 게임 루프를 관리하는 클래스
 * 
 * 이 클래스는 다음과 같은 작업을 수행합니다:
 * - 게임 루프 설정 및 실행
 * - 블록 자동 낙하 타이밍 관리
 * - 게임 시작/일시정지/재개 제어
 * - 레벨에 따른 낙하 속도 조정
 * 
 * GameController에서 게임 루프 관리 책임을 분리하여
 * 단일 책임 원칙(SRP)을 준수합니다.
 */
public class GameLoopManager {
    
    /**
     * 게임 루프 콜백 인터페이스
     * 
     * 게임 루프에서 실행할 로직을 정의합니다.
     */
    @FunctionalInterface
    public interface GameLoopCallback {
        /**
         * 게임 루프 틱마다 호출됩니다
         * 
         * @return 게임이 계속되어야 하면 true, 종료되어야 하면 false
         */
        boolean onTick();
    }
    
    private AnimationTimer gameLoop;
    private GameLoopCallback callback;
    private long lastUpdateTime = 0;
    private long dropInterval;
    private double speedMultiplier = 1.0; // 속도 배율
    
    /**
     * GameLoopManager 생성자
     */
    public GameLoopManager() {
        this.dropInterval = UIConstants.INITIAL_DROP_INTERVAL_NS;
    }
    
    /**
     * GameLoopManager 생성자 (속도 배율 지정)
     * 
     * @param speedMultiplier 낙하 속도 배율 (1.0 = 기본, 2.0 = 2배 빠름)
     */
    public GameLoopManager(double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
        this.dropInterval = (long) (UIConstants.INITIAL_DROP_INTERVAL_NS / speedMultiplier);
    }
    
    /**
     * 게임 루프 콜백을 설정합니다
     * 
     * @param callback 게임 루프에서 실행할 콜백
     */
    public void setCallback(GameLoopCallback callback) {
        this.callback = callback;
        setupGameLoop();
    }
    
    /**
     * 게임 루프를 설정합니다
     */
    private void setupGameLoop() {
        gameLoop = new AnimationTimer() {
            private long frameCount = 0;
            private long lastLogTime = 0;
            
            @Override
            public void handle(long now) {
                frameCount++;
                
                // 처음 5프레임과 이후 60프레임마다 로그 출력
                if (frameCount <= 5 || (now - lastLogTime) >= 1_000_000_000L) {
                    lastLogTime = now;
                }
                
                if (callback == null) {
                    System.err.println("❌ [GameLoopManager] Callback is null!");
                    return;
                }

                if (now - lastUpdateTime >= dropInterval) {
                    try {
                        boolean shouldContinue = callback.onTick();

                        if (!shouldContinue) {
                            stop();
                            return;
                        }

                        lastUpdateTime = now;
                    } catch (Exception e) {
                        System.err.println("❌ [GameLoopManager] Exception in game loop:");
                        e.printStackTrace();
                        stop();
                    }
                }
            }
        };
        System.out.println("🎮 [GameLoopManager] Game loop setup complete with interval: " + dropInterval + "ns");
    }
    
    /**
     * 게임을 시작합니다
     */
    public void start() {
        if (gameLoop != null) {
            lastUpdateTime = System.nanoTime();
            gameLoop.start();
        } else {
            System.err.println("❌ [GameLoopManager] Cannot start - gameLoop is null!");
        }
    }
    
    /**
     * 게임을 일시정지합니다
     */
    public void pause() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }
    
    /**
     * 게임을 재개합니다
     */
    public void resume() {
        if (gameLoop != null) {
            lastUpdateTime = System.nanoTime();
            gameLoop.start();
        }
    }
    
    /**
     * 게임 루프를 완전히 중지합니다
     */
    public void stop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }
    
    /**
     * 게임 상태에 따라 낙하 속도를 업데이트합니다
     * 
     * @param gameState 현재 게임 상태
     */
    public void updateDropSpeed(GameState gameState) {
        long baseInterval = Math.max(
            UIConstants.MIN_DROP_INTERVAL_NS,
            UIConstants.INITIAL_DROP_INTERVAL_NS - 
            (gameState.getLevel() * UIConstants.DROP_INTERVAL_DECREASE_PER_LEVEL_NS)
        );
        // 속도 배율 적용
        dropInterval = (long) (baseInterval / speedMultiplier);
    }
    
    /**
     * 현재 낙하 간격을 반환합니다
     * 
     * @return 낙하 간격 (나노초)
     */
    public long getDropInterval() {
        return dropInterval;
    }
    
    /**
     * 낙하 간격을 직접 설정합니다
     * 
     * @param dropInterval 낙하 간격 (나노초)
     */
    public void setDropInterval(long dropInterval) {
        this.dropInterval = dropInterval;
    }
    
    /**
     * 게임 루프가 실행 중인지 확인합니다
     * 
     * @return 실행 중이면 true
     */
    public boolean isRunning() {
        // AnimationTimer는 실행 상태를 직접 확인할 방법이 없으므로
        // 외부에서 관리해야 합니다
        return gameLoop != null;
    }
    
    /**
     * 리소스를 정리합니다
     * 
     * GameLoopManager가 더 이상 사용되지 않을 때 호출되어야 합니다.
     */
    public void cleanup() {
        stop();
        gameLoop = null;
        callback = null;
    }
}
