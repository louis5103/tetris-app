package seoultech.se.core.config;

/**
 * 게임플레이 타입을 정의하는 열거형
 * 
 * <p>이 열거형은 테트리스 게임의 다양한 게임플레이 모드를 나타냅니다.</p>
 * 
 * @since 1.0
 * @author Tetris Team
 */
public enum GameplayType {
    
    /**
     * 클래식 모드
     * 전통적인 테트리스 게임플레이를 제공합니다.
     */
    CLASSIC("클래식", "전통적인 테트리스 게임", "표준 속도와 난이도로 진행되는 기본 모드"),
    
    /**
     * 아케이드 모드
     * 빠르고 박진감 넘치는 게임플레이를 제공합니다.
     */
    ARCADE("아케이드", "빠르고 박진감 넘치는 모드", "블록 낙하 속도가 빠르고 높은 점수 배율 적용");
    
    private final String displayName;
    private final String description;
    private final String details;
    
    /**
     * GameplayType 생성자
     * 
     * @param displayName UI에 표시될 이름
     * @param description 모드 설명
     * @param details 상세 설명
     */
    GameplayType(String displayName, String description, String details) {
        this.displayName = displayName;
        this.description = description;
        this.details = details;
    }
    
    /**
     * UI에 표시될 이름을 반환합니다.
     * 
     * @return 표시 이름
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * 모드 설명을 반환합니다.
     * 
     * @return 설명
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * 상세 설명을 반환합니다.
     * 
     * @return 상세 설명
     */
    public String getDetails() {
        return details;
    }
    
    /**
     * 기본 게임플레이 타입을 반환합니다.
     * 
     * @return CLASSIC 타입
     */
    public static GameplayType getDefault() {
        return CLASSIC;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
