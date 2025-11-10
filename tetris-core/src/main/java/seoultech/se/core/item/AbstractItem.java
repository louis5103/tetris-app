package seoultech.se.core.item;

import lombok.Getter;

/**
 * 추상 아이템 클래스
 * 
 * Item 인터페이스의 기본 구현을 제공하는 추상 클래스입니다.
 * 모든 구체적인 아이템은 이 클래스를 상속받아 구현합니다.
 * 
 * 설계 원칙:
 * - Template Method Pattern: 공통 로직을 제공하고 세부 구현은 하위 클래스에 위임
 * - 재사용성: 중복 코드 제거
 */
@Getter
public abstract class AbstractItem implements Item {
    
    /**
     * 아이템 타입
     */
    private final ItemType type;
    
    /**
     * 아이템 활성화 여부
     */
    private boolean enabled;
    
    /**
     * 생성자
     * 
     * @param type 아이템 타입
     */
    protected AbstractItem(ItemType type) {
        this.type = type;
        this.enabled = true; // 기본적으로 활성화
    }
    
    /**
     * 아이템 활성화 상태 확인
     * 
     * @return 활성화 여부
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 아이템 활성화/비활성화
     * 
     * @param enabled 활성화 여부
     */
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * 문자열 표현
     * 
     * @return 아이템 이름
     */
    @Override
    public String toString() {
        return type.getDisplayName() + " " + type.getIcon() + 
               (enabled ? " [활성화]" : " [비활성화]");
    }
}
