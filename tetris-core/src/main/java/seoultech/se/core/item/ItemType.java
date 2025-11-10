package seoultech.se.core.item;

import lombok.Getter;

/**
 * 아이템 타입 열거형
 * 
 * Req2 필수 아이템만 정의합니다.
 * 새로운 아이템을 추가하려면 이 enum에 값을 추가하면 됩니다.
 */
@Getter
public enum ItemType {
    
    /**
     * 줄 삭제 아이템 ('L')
     * 
     * Req2 명세:
     * - 블록 내에 'L' 문자로 표시
     * - 블록 고정 시 'L'이 위치한 줄을 즉시 삭제
     * - 해당 줄이 꽉 차있지 않아도 삭제됨
     * - 삭제된 줄에 대해서도 기존 방식대로 점수 계산
     */
    LINE_CLEAR("Line Clear", "L", "Clears the line where 'L' is placed"),
    
    /**
     * 무게추 아이템
     * 
     * Req2 명세:
     * - 총 4칸 너비의 특수 블록
     * - 초기: 좌우 이동 가능
     * - 바닥/블록에 닿으면: 좌우 이동 불가, 아래로만 이동
     * - 떨어지면서 아래에 있는 모든 블록 제거
     */
    WEIGHT_BOMB("Weight Bomb", "⚓", "Clears all blocks below while falling");
    
    /**
     * 아이템 표시 이름
     */
    private final String displayName;
    
    /**
     * 아이템 아이콘
     */
    private final String icon;
    
    /**
     * 아이템 설명
     */
    private final String description;
    
    ItemType(String displayName, String icon, String description) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
    }
    
    /**
     * 아이템 타입을 문자열로 반환
     * 
     * @return 표시 이름
     */
    @Override
    public String toString() {
        return displayName;
    }
}
