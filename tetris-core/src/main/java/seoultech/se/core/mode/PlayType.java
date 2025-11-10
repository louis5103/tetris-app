package seoultech.se.core.mode;

/**
 * 플레이 타입을 정의하는 열거형
 * 
 * <p>이 열거형은 게임의 플레이 방식(싱글/멀티)을 나타냅니다.</p>
 * 
 * @since 1.0
 * @author Tetris Team
 */
public enum PlayType {
    
    /**
     * 로컬 싱글 플레이 모드
     * 혼자서 게임을 즐길 수 있습니다.
     */
    LOCAL_SINGLE("로컬 싱글", "혼자 플레이", "네트워크 연결 없이 로컬에서 플레이합니다."),
    
    /**
     * 온라인 멀티 플레이 모드
     * 다른 플레이어와 함께 게임을 즐길 수 있습니다.
     */
    ONLINE_MULTI("온라인 멀티", "다른 플레이어와 대결", "온라인으로 다른 플레이어와 경쟁합니다.");
    
    private final String displayName;
    private final String description;
    private final String details;
    
    /**
     * PlayType 생성자
     * 
     * @param displayName UI에 표시될 이름
     * @param description 모드 설명
     * @param details 상세 설명
     */
    PlayType(String displayName, String description, String details) {
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
     * 온라인 플레이가 필요한지 확인합니다.
     * 
     * @return 온라인 플레이 필요 여부
     */
    public boolean requiresOnline() {
        return this == ONLINE_MULTI;
    }
    
    /**
     * 기본 플레이 타입을 반환합니다.
     * 
     * @return LOCAL_SINGLE 타입
     */
    public static PlayType getDefault() {
        return LOCAL_SINGLE;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}
