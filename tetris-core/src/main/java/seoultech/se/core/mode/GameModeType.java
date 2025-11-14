package seoultech.se.core.mode;

/**
 * 게임 모드 타입
 * 
 * 테트리스 게임에서 지원하는 다양한 모드를 정의합니다.
 * 
 * 현재 지원 모드:
 * - SINGLE: 싱글플레이어 (로컬 게임)
 * - ITEM: 아이템 모드 (랜덤 아이템 드롭)
 * - MULTI: 멀티플레이어 (대전 모드)
 * 
 * 향후 추가 예정:
 * - SPEED_RUN: 스피드런 모드
 * - ENDLESS: 무한 모드
 * - AI_BATTLE: AI 대전 모드
 */
public enum GameModeType {
    /**
     * Classic 모드 (기본 테트리스)
     * - 아이템 없는 순수 테트리스
     * - 기본 규칙만 적용
     */
    CLASSIC("클래식", "아이템 없는 순수 테트리스"),
    
    /**
     * 싱글플레이어 모드
     * - 혼자서 즐기는 기본 모드
     * - 네트워크 연결 불필요
     * - 점수와 레벨만 관리
     */
    SINGLE("싱글플레이", "혼자서 즐기는 기본 모드"),
    
    /**
     * 아이템 모드
     * - 랜덤 아이템이 등장
     * - 특수 효과 활성화
     * - 전략적 플레이 가능
     */
    ITEM("아이템 모드", "랜덤 아이템이 등장하는 모드"),
    
    /**
     * 멀티플레이어 모드
     * - 다른 플레이어와 대전
     * - 네트워크 연결 필요
     * - 공격/방어 시스템
     */
    MULTI("멀티플레이", "다른 플레이어와 대전하는 모드");
    
    private final String displayName;
    private final String description;
    
    GameModeType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * 화면에 표시할 이름
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 모드 설명
     */
    public String getDescription() {
        return description;
    }
}
