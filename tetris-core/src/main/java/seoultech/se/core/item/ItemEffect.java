package seoultech.se.core.item;

import lombok.Builder;
import lombok.Getter;

/**
 * 아이템 효과 결과
 * 
 * 아이템 적용 후의 결과를 담는 불변 객체입니다.
 * 점수 변화, 제거된 블록 수 등의 정보를 포함합니다.
 * 
 * 설계 원칙:
 * - 불변성(Immutability): 생성 후 수정 불가
 * - 값 객체(Value Object): 효과의 결과를 표현
 */
@Getter
@Builder
public class ItemEffect {
    
    /**
     * 효과가 성공적으로 적용되었는지 여부
     */
    @Builder.Default
    private final boolean success = true;
    
    /**
     * 제거된 블록의 수
     */
    @Builder.Default
    private final int blocksCleared = 0;
    
    /**
     * 추가된 보너스 점수
     */
    @Builder.Default
    private final int bonusScore = 0;
    
    /**
     * 효과 설명 메시지
     */
    @Builder.Default
    private final String message = "";
    
    /**
     * 아이템 타입
     */
    private final ItemType itemType;
    
    /**
     * 총 점수 변화량을 반환합니다
     * 
     * @return 보너스 점수
     */
    public int getScoreChange() {
        return bonusScore;
    }
    
    /**
     * 빈 효과 (효과 없음)
     * 
     * @return 효과가 없는 ItemEffect 객체
     */
    public static ItemEffect none() {
        return ItemEffect.builder()
            .success(false)
            .message("No effect applied")
            .build();
    }
    
    /**
     * 성공 효과
     * 
     * @param itemType 아이템 타입
     * @param blocksCleared 제거된 블록 수
     * @param bonusScore 보너스 점수
     * @param message 메시지
     * @return 성공 ItemEffect 객체
     */
    public static ItemEffect success(ItemType itemType, int blocksCleared, int bonusScore, String message) {
        return ItemEffect.builder()
            .success(true)
            .itemType(itemType)
            .blocksCleared(blocksCleared)
            .bonusScore(bonusScore)
            .message(message)
            .build();
    }
}
