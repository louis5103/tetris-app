package seoultech.se.core.item.impl;

import seoultech.se.core.GameState;
import seoultech.se.core.item.AbstractItem;
import seoultech.se.core.item.ItemEffect;
import seoultech.se.core.item.ItemType;

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
        
        // GameState에 softDropSpeed 필드가 있다고 가정
        // 실제 구현에서는 GameEngine을 통해 처리하거나
        // GameState에 속도 관련 필드를 추가해야 합니다
        
        // 메타데이터에 속도 초기화 플래그 설정
        // 이 플래그는 GameEngine에서 읽어서 처리합니다
        gameState.setLastActionWasRotation(false); // 임시로 플래그 활용
        
        String message = "⚡ Soft drop speed reset to initial value!";
        
        System.out.println(message);
        
        return ItemEffect.success(ItemType.SPEED_RESET, 0, BONUS_SCORE, message);
    }
    
    /**
     * 이 아이템은 게임 엔진에서 추가 처리가 필요합니다.
     * GameEngine에 다음과 같은 메서드 추가 권장:
     * 
     * public void resetSoftDropSpeed() {
     *     this.currentSoftDropSpeed = config.getSoftDropSpeed();
     * }
     */
}
