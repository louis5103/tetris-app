package seoultech.se.client.config;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import seoultech.se.core.config.DifficultySettings;
import seoultech.se.core.model.enumType.Difficulty;

/**
 * 난이도 시스템 초기화 컴포넌트
 * 
 * <p>애플리케이션 시작 시 자동으로 실행되어 Difficulty enum을
 * application.yml의 설정값으로 초기화합니다.</p>
 * 
 * <h3>동작 순서:</h3>
 * <ol>
 *   <li>Spring Boot가 DifficultyConfigProperties에 YAML 값 바인딩</li>
 *   <li>@PostConstruct로 이 클래스의 initialize() 메서드 자동 실행</li>
 *   <li>Difficulty.initialize()를 호출하여 enum 초기화</li>
 *   <li>초기화 결과를 콘솔에 출력</li>
 * </ol>
 * 
 * <h3>로그 출력 예시:</h3>
 * <pre>
 * ========================================
 * [Difficulty System] Initialization Started
 * ========================================
 * 
 * 📋 Loaded Configuration:
 *   EASY   - DifficultyLevel{displayName='쉬움', iBlock=1.2, speedInc=0.8, score=1.2, lockDelay=1.2}
 *   NORMAL - DifficultyLevel{displayName='보통', iBlock=1.0, speedInc=1.0, score=1.0, lockDelay=1.0}
 *   HARD   - DifficultyLevel{displayName='어려움', iBlock=0.8, speedInc=1.2, score=0.8, lockDelay=0.8}
 * 
 * ✅ [Difficulty] Initialized from config:
 *    EASY   - DifficultySettings(displayName=쉬움, iBlockMultiplier=1.2, ...)
 *    NORMAL - DifficultySettings(displayName=보통, iBlockMultiplier=1.0, ...)
 *    HARD   - DifficultySettings(displayName=어려움, iBlockMultiplier=0.8, ...)
 * 
 * ========================================
 * [Difficulty System] Initialization Completed ✅
 * ========================================
 * </pre>
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 3
 */
@Component
@RequiredArgsConstructor
public class DifficultyInitializer {
    
    /**
     * Spring Boot가 자동으로 주입하는 난이도 설정
     */
    private final DifficultyConfigProperties difficultyConfig;
    
    /**
     * 애플리케이션 시작 시 자동 실행
     * 
     * <p>@PostConstruct 어노테이션에 의해 Bean 생성 직후 자동으로 호출됩니다.</p>
     * 
     * <h3>수행 작업:</h3>
     * <ul>
     *   <li>application.yml에서 로드한 설정을 DifficultySettings로 변환</li>
     *   <li>Difficulty enum을 해당 설정으로 초기화</li>
     *   <li>초기화 결과를 콘솔에 출력</li>
     *   <li>설정 유효성 검증</li>
     * </ul>
     */
    @PostConstruct
    public void initialize() {
        printInitializationHeader();
        
        try {
            // 1. application.yml에서 로드한 설정 출력
            printLoadedConfiguration();
            
            // 2. DifficultySettings 생성
            DifficultySettings easySettings = difficultyConfig.toEasySettings();
            DifficultySettings normalSettings = difficultyConfig.toNormalSettings();
            DifficultySettings hardSettings = difficultyConfig.toHardSettings();
            
            // 3. Difficulty enum 초기화
            Difficulty.initialize(easySettings, normalSettings, hardSettings);
            
            // 4. 초기화 완료 메시지
            printInitializationComplete();
            
        } catch (Exception e) {
            printInitializationError(e);
            throw new RuntimeException("Failed to initialize Difficulty system", e);
        }
    }
    
    /**
     * 초기화 시작 헤더 출력
     */
    private void printInitializationHeader() {
        System.out.println("\n========================================");
        System.out.println("[Difficulty System] Initialization Started");
        System.out.println("========================================\n");
    }
    
    /**
     * 로드된 설정 출력
     */
    private void printLoadedConfiguration() {
        System.out.println("📋 Loaded Configuration:");
        System.out.println("  EASY   - " + difficultyConfig.getEasy());
        System.out.println("  NORMAL - " + difficultyConfig.getNormal());
        System.out.println("  HARD   - " + difficultyConfig.getHard());
        System.out.println();
    }
    
    /**
     * 초기화 완료 메시지 출력
     */
    private void printInitializationComplete() {
        System.out.println("\n========================================");
        System.out.println("[Difficulty System] Initialization Completed ✅");
        System.out.println("========================================\n");
    }
    
    /**
     * 초기화 에러 메시지 출력
     * 
     * @param e 발생한 예외
     */
    private void printInitializationError(Exception e) {
        System.err.println("\n========================================");
        System.err.println("[Difficulty System] Initialization FAILED ❌");
        System.err.println("========================================");
        System.err.println("Error: " + e.getMessage());
        e.printStackTrace();
        System.err.println("========================================\n");
    }
    
    /**
     * 현재 초기화된 난이도 설정 조회
     * 
     * <p>디버깅 및 테스트 목적으로 사용됩니다.</p>
     * 
     * @return 현재 DifficultyConfigProperties
     */
    public DifficultyConfigProperties getDifficultyConfig() {
        return difficultyConfig;
    }
}
