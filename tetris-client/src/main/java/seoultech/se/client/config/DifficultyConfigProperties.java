package seoultech.se.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;
import seoultech.se.core.config.DifficultySettings;

/**
 * 난이도 설정을 위한 Configuration Properties
 * 
 * <p>application.yml의 tetris.difficulty.* 값을 자동으로 매핑합니다.</p>
 * 
 * <h3>매핑 구조:</h3>
 * <pre>
 * tetris:
 *   difficulty:
 *     easy:
 *       display-name: "쉬움"
 *       i-block-multiplier: 1.2
 *       speed-increase-multiplier: 0.8
 *       score-multiplier: 1.2
 *       lock-delay-multiplier: 1.2
 *     normal:
 *       display-name: "보통"
 *       i-block-multiplier: 1.0
 *       ...
 *     hard:
 *       display-name: "어려움"
 *       i-block-multiplier: 0.8
 *       ...
 * </pre>
 * 
 * <h3>장점:</h3>
 * <ul>
 *   <li>타입 안전성 (YAML → Java 객체 자동 변환)</li>
 *   <li>IDE 자동완성 지원</li>
 *   <li>Spring Boot의 검증 기능 활용 가능</li>
 *   <li>테스트 용이</li>
 * </ul>
 * 
 * <h3>사용 예시:</h3>
 * <pre>{@code
 * @Autowired
 * private DifficultyConfigProperties difficultyConfig;
 * 
 * DifficultySettings easySettings = difficultyConfig.toEasySettings();
 * }</pre>
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 3
 */
@Configuration
@ConfigurationProperties(prefix = "tetris.difficulty")
@Getter
@Setter
public class DifficultyConfigProperties {
    
    /**
     * Easy 모드 설정
     */
    private DifficultyLevel easy = new DifficultyLevel();
    
    /**
     * Normal 모드 설정
     */
    private DifficultyLevel normal = new DifficultyLevel();
    
    /**
     * Hard 모드 설정
     */
    private DifficultyLevel hard = new DifficultyLevel();
    
    /**
     * Easy 설정을 DifficultySettings로 변환
     * 
     * @return Easy 모드의 DifficultySettings
     */
    public DifficultySettings toEasySettings() {
        return DifficultySettings.builder()
                .displayName(easy.getDisplayName())
                .iBlockMultiplier(easy.getIBlockMultiplier())
                .speedIncreaseMultiplier(easy.getSpeedIncreaseMultiplier())
                .scoreMultiplier(easy.getScoreMultiplier())
                .lockDelayMultiplier(easy.getLockDelayMultiplier())
                .build();
    }
    
    /**
     * Normal 설정을 DifficultySettings로 변환
     * 
     * @return Normal 모드의 DifficultySettings
     */
    public DifficultySettings toNormalSettings() {
        return DifficultySettings.builder()
                .displayName(normal.getDisplayName())
                .iBlockMultiplier(normal.getIBlockMultiplier())
                .speedIncreaseMultiplier(normal.getSpeedIncreaseMultiplier())
                .scoreMultiplier(normal.getScoreMultiplier())
                .lockDelayMultiplier(normal.getLockDelayMultiplier())
                .build();
    }
    
    /**
     * Hard 설정을 DifficultySettings로 변환
     * 
     * @return Hard 모드의 DifficultySettings
     */
    public DifficultySettings toHardSettings() {
        return DifficultySettings.builder()
                .displayName(hard.getDisplayName())
                .iBlockMultiplier(hard.getIBlockMultiplier())
                .speedIncreaseMultiplier(hard.getSpeedIncreaseMultiplier())
                .scoreMultiplier(hard.getScoreMultiplier())
                .lockDelayMultiplier(hard.getLockDelayMultiplier())
                .build();
    }
    
    /**
     * 설정 유효성 검증
     * 
     * @return 모든 난이도 설정이 유효하면 true
     */
    public boolean isValid() {
        try {
            toEasySettings().validate();
            toNormalSettings().validate();
            toHardSettings().validate();
            return true;
        } catch (IllegalArgumentException e) {
            System.err.println("⚠️ [DifficultyConfig] Invalid settings: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 난이도 레벨 설정 클래스
     * 
     * <p>각 난이도(Easy, Normal, Hard)의 세부 설정을 저장합니다.</p>
     */
    @Getter
    @Setter
    public static class DifficultyLevel {
        
        /**
         * 표시 이름 (예: "쉬움", "보통", "어려움")
         * 기본값: "Unknown"
         */
        private String displayName = "Unknown";
        
        /**
         * I형 블록 출현 확률 배율
         * <ul>
         *   <li>1.0: 기본 (14.3%)</li>
         *   <li>1.2: 20% 증가 (17.1%)</li>
         *   <li>0.8: 20% 감소 (11.8%)</li>
         * </ul>
         * 기본값: 1.0
         */
        private double iBlockMultiplier = 1.0;
        
        /**
         * 레벨업 시 속도 증가율 배율
         * <ul>
         *   <li>1.0: 기본</li>
         *   <li>0.8: 20% 완만 (쉬움)</li>
         *   <li>1.2: 20% 급격 (어려움)</li>
         * </ul>
         * 기본값: 1.0
         */
        private double speedIncreaseMultiplier = 1.0;
        
        /**
         * 점수 배율
         * <ul>
         *   <li>1.0: 기본</li>
         *   <li>1.2: 20% 증가 (쉬움)</li>
         *   <li>0.8: 20% 감소 (어려움)</li>
         * </ul>
         * 기본값: 1.0
         */
        private double scoreMultiplier = 1.0;
        
        /**
         * 락 딜레이 배율
         * <ul>
         *   <li>1.0: 기본</li>
         *   <li>1.2: 20% 증가 (쉬움)</li>
         *   <li>0.8: 20% 감소 (어려움)</li>
         * </ul>
         * 기본값: 1.0
         */
        private double lockDelayMultiplier = 1.0;
        
        /**
         * 문자열 표현
         * 
         * @return 설정 정보 문자열
         */
        @Override
        public String toString() {
            return String.format(
                "DifficultyLevel{displayName='%s', iBlock=%.1f, speedInc=%.1f, score=%.1f, lockDelay=%.1f}",
                displayName, iBlockMultiplier, speedIncreaseMultiplier, 
                scoreMultiplier, lockDelayMultiplier
            );
        }
    }
    
    /**
     * 전체 설정 문자열 표현
     * 
     * @return 전체 난이도 설정 정보
     */
    @Override
    public String toString() {
        return String.format(
            "DifficultyConfigProperties{\n  easy=%s,\n  normal=%s,\n  hard=%s\n}",
            easy, normal, hard
        );
    }
}
