package seoultech.se.core.model.enumType;

import lombok.Getter;

/**
 * 테트리스 게임 난이도 열거형 (단순화)
 * 
 * <p>세 가지 난이도를 제공합니다:</p>
 * <ul>
 *   <li>EASY: 쉬움 - 속도 느림, 락 딜레이 김, 점수 높음</li>
 *   <li>NORMAL: 보통 - 기본 설정</li>
 *   <li>HARD: 어려움 - 속도 빠름, 락 딜레이 짧음, 점수 낮음</li>
 * </ul>
 * 
 * <p>배율은 GameModeConfigFactory에서 적용됩니다.</p>
 * 
 * <p>사용 예시:</p>
 * <pre>{@code
 * Difficulty difficulty = Difficulty.NORMAL;
 * String displayName = difficulty.getDisplayName();  // "보통"
 * }</pre>
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 2 (리팩토링: Phase 5)
 */
@Getter
public enum Difficulty {
    
    /**
     * 쉬움 모드
     * <ul>
     *   <li>속도: 80% (느림)</li>
     *   <li>락 딜레이: 120% (김)</li>
     *   <li>점수: 120%</li>
     * </ul>
     */
    EASY("쉬움"),
    
    /**
     * 보통 모드 (기본값)
     * <ul>
     *   <li>모든 값이 100% (기본)</li>
     * </ul>
     */
    NORMAL("보통"),
    
    /**
     * 어려움 모드
     * <ul>
     *   <li>속도: 120% (빠름)</li>
     *   <li>락 딜레이: 80% (짧음)</li>
     *   <li>점수: 80%</li>
     * </ul>
     */
    HARD("어려움");
    
    /**
     * 난이도 표시 이름
     */
    private final String displayName;
    
    /**
     * 생성자
     * 
     * @param displayName 표시 이름
     */
    Difficulty(String displayName) {
        this.displayName = displayName;
    }
    
    /**
     * 난이도 이름으로 검색
     * 
     * @param name 난이도 이름 (대소문자 무시)
     * @return 해당 난이도, 없으면 NORMAL
     */
    public static Difficulty fromName(String name) {
        if (name == null) {
            return NORMAL;
        }
        
        try {
            return Difficulty.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ Unknown difficulty: " + name + ", using NORMAL");
            return NORMAL;
        }
    }
    
    /**
     * 문자열 표현
     * 
     * @return "난이도명(표시이름)"
     */
    @Override
    public String toString() {
        return name() + "(" + displayName + ")";
    }
}
