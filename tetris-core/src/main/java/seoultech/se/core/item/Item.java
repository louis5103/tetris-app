package seoultech.se.core.item;

import seoultech.se.core.GameState;

/**
 * 아이템 인터페이스
 * 
 * 모든 아이템이 구현해야 하는 기본 인터페이스입니다.
 * Strategy Pattern을 사용하여 각 아이템의 효과를 캡슐화합니다.
 * 
 * 설계 원칙:
 * - 단일 책임: 각 아이템은 자신의 효과만 책임집니다
 * - 개방-폐쇄: 새로운 아이템 추가 시 기존 코드 수정 불필요
 * - 의존성 역전: GameState에 의존하여 느슨한 결합 유지
 */
public interface Item {
    
    /**
     * 아이템 타입 반환
     * 
     * @return 아이템 타입
     */
    ItemType getType();
    
    /**
     * 아이템이 활성화되었는지 확인
     * 설정에서 비활성화된 아이템은 false를 반환합니다.
     * 
     * @return 활성화 여부
     */
    boolean isEnabled();
    
    /**
     * 아이템 활성화/비활성화 설정
     * 
     * @param enabled 활성화 여부
     */
    void setEnabled(boolean enabled);
    
    /**
     * 아이템 효과를 적용합니다
     * 
     * @param gameState 현재 게임 상태
     * @param row 아이템이 적용될 행 (아이템마다 의미가 다를 수 있음)
     * @param col 아이템이 적용될 열 (아이템마다 의미가 다를 수 있음)
     * @return 효과 적용 결과 (점수, 제거된 블록 수 등)
     */
    ItemEffect apply(GameState gameState, int row, int col);
    
    /**
     * 아이템 이름 반환
     * 
     * @return 아이템 표시 이름
     */
    default String getName() {
        return getType().getDisplayName();
    }
    
    /**
     * 아이템 설명 반환
     * 
     * @return 아이템 설명
     */
    default String getDescription() {
        return getType().getDescription();
    }
    
    /**
     * 아이템 아이콘 반환
     * 
     * @return 아이템 아이콘
     */
    default String getIcon() {
        return getType().getIcon();
    }
}
