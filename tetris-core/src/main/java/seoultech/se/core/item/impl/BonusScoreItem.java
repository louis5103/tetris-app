package seoultech.se.core.item.impl;

import seoultech.se.core.GameState;
import seoultech.se.core.item.AbstractItem;
import seoultech.se.core.item.ItemEffect;
import seoultech.se.core.item.ItemType;

/**
 * 보너스 점수 아이템
 * 
 * 즉시 보너스 점수를 부여합니다.
 * 
 * 효과:
 * - 고정된 보너스 점수 획득
 * - 레벨에 따라 점수 배율 적용 가능
 * 
 * 사용 예시:
 * - 간단하게 점수를 올리고 싶을 때
 * - 다른 아이템보다 효과가 약하지만 안정적인 보상
 */
public class BonusScoreItem extends AbstractItem {
    
    /**
     * 기본 보너스 점수
     */
    private static final int BASE_BONUS = 500;
    
    /**
     * 레벨당 추가 점수 배율
     */
    private static final int LEVEL_MULTIPLIER = 50;
    
    /**
     * 생성자
     */
    public BonusScoreItem() {
        super(ItemType.BONUS_SCORE);
    }
    
    /**
     * 보너스 점수 효과 적용
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
        
        // 레벨에 따른 보너스 점수 계산
        int currentLevel = gameState.getLevel();
        int bonusScore = BASE_BONUS + (currentLevel * LEVEL_MULTIPLIER);
        
        // 점수 즉시 추가
        gameState.setScore(gameState.getScore() + bonusScore);
        
        String message = String.format("⭐ Bonus score! +%d points (Level %d)", 
            bonusScore, currentLevel);
        
        System.out.println(message);
        
        return ItemEffect.success(ItemType.BONUS_SCORE, 0, bonusScore, message);
    }
}
