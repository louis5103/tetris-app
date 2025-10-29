package seoultech.se.core.item;

import lombok.Getter;

/**
 * 아이템 타입 열거형
 * 
 * 각 아이템의 종류를 정의하고, 확장 가능한 구조를 제공합니다.
 * 새로운 아이템을 추가하려면 이 enum에 값을 추가하면 됩니다.
 */
@Getter
public enum ItemType {
    
    /**
     * 폭탄 아이템
     * 반경 2칸의 정사각형 범위를 지웁니다.
     */
    BOMB("Bomb", "💣", "Clears a 5x5 area around the item"),
    
    /**
     * 십자 아이템
     * 아이템의 행과 열을 모두 지웁니다.
     */
    PLUS("Plus", "➕", "Clears the entire row and column"),
    
    /**
     * 속도 초기화 아이템
     * 소프트 드롭 속도를 초기 값으로 되돌립니다.
     */
    SPEED_RESET("Speed Reset", "⚡", "Resets soft drop speed to initial value"),
    
    /**
     * 보너스 점수 아이템
     * 즉시 보너스 점수를 부여합니다.
     */
    BONUS_SCORE("Bonus Score", "⭐", "Grants bonus score points");
    
    /**
     * 아이템 표시 이름
     */
    private final String displayName;
    
    /**
     * 아이템 아이콘 (이모지)
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
