package seoultech.se.core.engine.item.impl;

import seoultech.se.core.GameState;
import seoultech.se.core.engine.item.AbstractItem;
import seoultech.se.core.engine.item.ItemEffect;
import seoultech.se.core.engine.item.ItemType;

/**
 * 속도 초기화 아이템
 * 
 * 소프트 드롭으로 누적된 낙하 속도를 초기 값으로 되돌립니다.
 * 
 * 효과:
 * - GameState 내부의 소프트 드롭 속도 관련 변수를 초기화
 * - 빠른 속도로 인한 어려움을 해소
 * 
 * 사용 예시:
 * - 소프트 드롭이 너무 빨라져서 컨트롤이 어려운 경우
 * - 고레벨에서 속도를 잠시 늦추고 싶을 때
 * 
 * 참고:
 * - 이 아이템은 게임 엔진과의 연동이 필요합니다
 * - GameEngine에 softDropSpeed 초기화 메서드가 있어야 합니다
 */
public class SpeedResetItem extends AbstractItem {
    
    /**
     * 보너스 점수
     */
    private static final int BONUS_SCORE = 100;
    
    /**
     * 생성자
     */
    public SpeedResetItem() {
        super(ItemType.SPEED_RESET);
    }
    
    /**
     * 속도 초기화 효과 적용
     * 
     * 🎮 구현 방식:
     * 1. softDropSpeedMultiplier를 1.0으로 리셋
     * 2. speedResetRequested 플래그를 true로 설정
     * 3. BoardController/GameLoop가 이 플래그를 감지하고 타이머 속도 조정
     * 
     * @param gameState 게임 상태
     * @param row 사용하지 않음
     * @param col 사용하지 않음
     * @return 아이템 효과
     */
    @Override
    public ItemEffect apply(GameState gameState, int row, int col) {
        if (!isEnabled()) {
            return ItemEffect.none();
        }
        
        System.out.println("⚡ [SpeedResetItem] Applying SPEED_RESET effect");
        System.out.println("   - Previous speed multiplier: " + gameState.getSoftDropSpeedMultiplier());
        
        // 🎮 GAME UX: 소프트 드롭 속도를 초기값(1.0)으로 리셋
        gameState.setSoftDropSpeedMultiplier(1.0);
        
        // 🎮 플래그 설정: BoardController/GameLoop가 이 플래그를 감지하고 타이머 조정
        gameState.setSpeedResetRequested(true);
        
        String message = "⚡ Speed Reset! 속도가 초기값으로 돌아갑니다.";
        
        System.out.println("   - New speed multiplier: " + gameState.getSoftDropSpeedMultiplier());
        System.out.println("   - Speed reset requested: " + gameState.isSpeedResetRequested());
        System.out.println("✅ [SpeedResetItem] " + message);
        
        return ItemEffect.success(ItemType.SPEED_RESET, 0, BONUS_SCORE, message);
    }
    
    /**
     * 🎮 BoardController/GameLoop 연동 가이드:
     * 
     * BoardController 또는 GameLoop에서 다음과 같이 처리:
     * 
     * <pre>
     * // 매 프레임 또는 타이머 업데이트 시
     * if (gameState.isSpeedResetRequested()) {
     *     // 타이머 속도를 초기값으로 리셋
     *     double newInterval = baseDropInterval / gameState.getSoftDropSpeedMultiplier();
     *     updateTimerInterval(newInterval);
     *     
     *     // 플래그 리셋
     *     gameState.setSpeedResetRequested(false);
     * }
     * </pre>
     * 
     * 또는 소프트 드롭 속도 계산 시:
     * 
     * <pre>
     * double currentSpeed = baseSpeed * gameState.getSoftDropSpeedMultiplier();
     * </pre>
     */
}
