package seoultech.se.core.model.enumType;

import lombok.Getter;
import seoultech.se.core.config.DifficultySettings;

/**
 * 테트리스 게임 난이도 열거형
 * 
 * <p>세 가지 난이도를 제공합니다:</p>
 * <ul>
 *   <li>EASY: 쉬움 - I형 블록 증가, 속도 완만, 점수 증가</li>
 *   <li>NORMAL: 보통 - 기본 설정</li>
 *   <li>HARD: 어려움 - I형 블록 감소, 속도 급격, 점수 감소</li>
 * </ul>
 * 
 * <p>각 난이도는 DifficultySettings 객체를 가지며,
 * application.yml에서 로드한 설정으로 초기화할 수 있습니다.</p>
 * 
 * <p>사용 예시:</p>
 * <pre>{@code
 * // 기본값 사용
 * Difficulty easy = Difficulty.EASY;
 * double iBlockMultiplier = easy.getIBlockMultiplier();
 * 
 * // application.yml에서 초기화
 * Difficulty.initialize(easySettings, normalSettings, hardSettings);
 * }</pre>
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 2
 */
@Getter
public enum Difficulty {
    
    /**
     * 쉬움 모드
     * <ul>
     *   <li>I형 블록: 20% 증가</li>
     *   <li>속도 증가: 20% 완만</li>
     *   <li>점수: 20% 증가</li>
     *   <li>락 딜레이: 20% 증가</li>
     * </ul>
     */
    EASY(DifficultySettings.createEasyDefaults()),
    
    /**
     * 보통 모드 (기본값)
     * <ul>
     *   <li>모든 값이 1.0x (기본)</li>
     * </ul>
     */
    NORMAL(DifficultySettings.createNormalDefaults()),
    
    /**
     * 어려움 모드
     * <ul>
     *   <li>I형 블록: 20% 감소</li>
     *   <li>속도 증가: 20% 급격</li>
     *   <li>점수: 20% 감소</li>
     *   <li>락 딜레이: 20% 감소</li>
     * </ul>
     */
    HARD(DifficultySettings.createHardDefaults());
    
    /**
     * 난이도별 설정
     */
    private DifficultySettings settings;
    
    /**
     * 생성자
     * 
     * @param defaultSettings 기본 설정
     */
    Difficulty(DifficultySettings defaultSettings) {
        this.settings = defaultSettings;
    }
    
    /**
     * 외부 설정으로 초기화
     * 
     * <p>Spring Boot의 application.yml에서 로드한 설정으로
     * 각 난이도의 값을 오버라이드합니다.</p>
     * 
     * <p>사용 시점:</p>
     * <ul>
     *   <li>애플리케이션 시작 시 (@PostConstruct)</li>
     *   <li>DifficultyInitializer에서 호출</li>
     * </ul>
     * 
     * @param easySettings Easy 모드 설정
     * @param normalSettings Normal 모드 설정
     * @param hardSettings Hard 모드 설정
     */
    public static void initialize(
            DifficultySettings easySettings,
            DifficultySettings normalSettings,
            DifficultySettings hardSettings) {
        
        // 검증
        easySettings.validate();
        normalSettings.validate();
        hardSettings.validate();
        
        // 설정 적용
        EASY.settings = easySettings;
        NORMAL.settings = normalSettings;
        HARD.settings = hardSettings;
        
        // 로그 출력
        System.out.println("✅ [Difficulty] Initialized from config:");
        System.out.println("   EASY   - " + EASY.settings);
        System.out.println("   NORMAL - " + NORMAL.settings);
        System.out.println("   HARD   - " + HARD.settings);
    }
    
    // =========================================================================
    // Convenience Getters - DifficultySettings의 값을 직접 접근
    // =========================================================================
    
    /**
     * 표시 이름 반환
     * 
     * @return 난이도 표시 이름 (예: "쉬움", "보통", "어려움")
     */
    public String getDisplayName() {
        return settings.getDisplayName();
    }
    
    /**
     * I형 블록 출현 확률 배율 반환
     * 
     * @return I형 블록 배율 (예: 1.0, 1.2, 0.8)
     */
    public double getIBlockMultiplier() {
        return settings.getIBlockMultiplier();
    }
    
    /**
     * 속도 증가율 배율 반환
     * 
     * @return 속도 증가율 배율
     */
    public double getSpeedIncreaseMultiplier() {
        return settings.getSpeedIncreaseMultiplier();
    }
    
    /**
     * 점수 배율 반환
     * 
     * @return 점수 배율
     */
    public double getScoreMultiplier() {
        return settings.getScoreMultiplier();
    }
    
    /**
     * 락 딜레이 배율 반환
     * 
     * @return 락 딜레이 배율
     */
    public double getLockDelayMultiplier() {
        return settings.getLockDelayMultiplier();
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
        return name() + "(" + getDisplayName() + ")";
    }
}
