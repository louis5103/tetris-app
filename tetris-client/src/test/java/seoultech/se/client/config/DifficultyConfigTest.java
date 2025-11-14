package seoultech.se.client.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import seoultech.se.core.config.DifficultySettings;
import seoultech.se.core.model.enumType.Difficulty;

/**
 * DifficultyConfig 시스템 통합 테스트
 * 
 * <p>Spring Boot 통합 테스트로 다음을 검증합니다:</p>
 * <ul>
 *   <li>application.yml에서 설정 로딩</li>
 *   <li>DifficultyConfigProperties 바인딩</li>
 *   <li>DifficultyInitializer 자동 실행</li>
 *   <li>Difficulty enum 초기화</li>
 *   <li>각 난이도별 설정값 정확성</li>
 * </ul>
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 3
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true",
    "javafx.enabled=false"
})
@DisplayName("Difficulty Config 통합 테스트")
class DifficultyConfigTest {
    
    @Autowired
    private DifficultyConfigProperties difficultyConfig;
    
    @Autowired
    private DifficultyInitializer difficultyInitializer;
    
    // =========================================================================
    // 1. Spring Boot 통합 테스트
    // =========================================================================
    
    @Test
    @DisplayName("1-1. DifficultyConfigProperties Bean이 정상적으로 로드되어야 함")
    void testConfigPropertiesBeanLoaded() {
        // Given & When: Spring Boot가 자동으로 Bean 생성
        
        // Then: Bean이 null이 아니어야 함
        assertNotNull(difficultyConfig, "DifficultyConfigProperties Bean이 null입니다");
        
        System.out.println("✅ DifficultyConfigProperties Bean 로드 성공");
        System.out.println("   " + difficultyConfig);
    }
    
    @Test
    @DisplayName("1-2. DifficultyInitializer Bean이 정상적으로 로드되어야 함")
    void testInitializerBeanLoaded() {
        // Given & When: Spring Boot가 자동으로 Bean 생성
        
        // Then: Bean이 null이 아니어야 함
        assertNotNull(difficultyInitializer, "DifficultyInitializer Bean이 null입니다");
        
        System.out.println("✅ DifficultyInitializer Bean 로드 성공");
    }
    
    // =========================================================================
    // 2. application.yml 바인딩 테스트
    // =========================================================================
    
    @Test
    @DisplayName("2-1. Easy 모드 설정이 application.yml에서 정확히 바인딩되어야 함")
    void testEasyConfigBinding() {
        // Given: application.yml의 Easy 설정
        // tetris.difficulty.easy:
        //   display-name: "쉬움"
        //   i-block-multiplier: 1.2
        //   speed-increase-multiplier: 0.8
        //   score-multiplier: 1.2
        //   lock-delay-multiplier: 1.2
        
        // When: DifficultyConfigProperties에서 Easy 설정 조회
        var easyLevel = difficultyConfig.getEasy();
        
        // Then: 값이 application.yml과 일치해야 함
        assertEquals("쉬움", easyLevel.getDisplayName(), "Easy displayName이 일치하지 않음");
        assertEquals(1.2, easyLevel.getIBlockMultiplier(), 0.001, "Easy iBlockMultiplier가 일치하지 않음");
        assertEquals(0.8, easyLevel.getSpeedIncreaseMultiplier(), 0.001, "Easy speedIncreaseMultiplier가 일치하지 않음");
        assertEquals(0.5, easyLevel.getScoreMultiplier(), 0.001, "Easy scoreMultiplier가 일치하지 않음 (SRS 표준: 낮은 난이도 = 낮은 배율)");
        assertEquals(1.2, easyLevel.getLockDelayMultiplier(), 0.001, "Easy lockDelayMultiplier가 일치하지 않음");
        
        System.out.println("✅ Easy 모드 바인딩 성공: " + easyLevel);
    }
    
    @Test
    @DisplayName("2-2. Normal 모드 설정이 application.yml에서 정확히 바인딩되어야 함")
    void testNormalConfigBinding() {
        // Given: application.yml의 Normal 설정
        
        // When: DifficultyConfigProperties에서 Normal 설정 조회
        var normalLevel = difficultyConfig.getNormal();
        
        // Then: 모든 값이 1.0 (기본값)이어야 함
        assertEquals("보통", normalLevel.getDisplayName());
        assertEquals(1.0, normalLevel.getIBlockMultiplier(), 0.001);
        assertEquals(1.0, normalLevel.getSpeedIncreaseMultiplier(), 0.001);
        assertEquals(1.0, normalLevel.getScoreMultiplier(), 0.001);
        assertEquals(1.0, normalLevel.getLockDelayMultiplier(), 0.001);
        
        System.out.println("✅ Normal 모드 바인딩 성공: " + normalLevel);
    }
    
    @Test
    @DisplayName("2-3. Hard 모드 설정이 application.yml에서 정확히 바인딩되어야 함")
    void testHardConfigBinding() {
        // Given: application.yml의 Hard 설정
        
        // When: DifficultyConfigProperties에서 Hard 설정 조회
        var hardLevel = difficultyConfig.getHard();
        
        // Then: 값이 application.yml과 일치해야 함
        assertEquals("어려움", hardLevel.getDisplayName());
        assertEquals(0.8, hardLevel.getIBlockMultiplier(), 0.001);
        assertEquals(1.2, hardLevel.getSpeedIncreaseMultiplier(), 0.001);
        assertEquals(1.5, hardLevel.getScoreMultiplier(), 0.001, "Hard scoreMultiplier가 일치하지 않음 (SRS 표준: 높은 난이도 = 높은 배율)");
        assertEquals(0.8, hardLevel.getLockDelayMultiplier(), 0.001);
        
        System.out.println("✅ Hard 모드 바인딩 성공: " + hardLevel);
    }
    
    // =========================================================================
    // 3. DifficultySettings 변환 테스트
    // =========================================================================
    
    @Test
    @DisplayName("3-1. DifficultyConfigProperties → DifficultySettings 변환이 정상 작동해야 함")
    void testConfigToSettingsConversion() {
        // When: 각 난이도를 DifficultySettings로 변환
        DifficultySettings easySettings = difficultyConfig.toEasySettings();
        DifficultySettings normalSettings = difficultyConfig.toNormalSettings();
        DifficultySettings hardSettings = difficultyConfig.toHardSettings();
        
        // Then: 변환된 객체가 null이 아니어야 함
        assertNotNull(easySettings, "Easy DifficultySettings가 null");
        assertNotNull(normalSettings, "Normal DifficultySettings가 null");
        assertNotNull(hardSettings, "Hard DifficultySettings가 null");
        
        // And: 값이 정확히 변환되어야 함
        assertEquals("쉬움", easySettings.getDisplayName());
        assertEquals(1.2, easySettings.getIBlockMultiplier(), 0.001);
        
        assertEquals("보통", normalSettings.getDisplayName());
        assertEquals(1.0, normalSettings.getIBlockMultiplier(), 0.001);
        
        assertEquals("어려움", hardSettings.getDisplayName());
        assertEquals(0.8, hardSettings.getIBlockMultiplier(), 0.001);
        
        System.out.println("✅ Config → Settings 변환 성공");
        System.out.println("   Easy:   " + easySettings);
        System.out.println("   Normal: " + normalSettings);
        System.out.println("   Hard:   " + hardSettings);
    }
    
    @Test
    @DisplayName("3-2. 변환된 DifficultySettings의 검증이 성공해야 함")
    void testConvertedSettingsValidation() {
        // When: DifficultySettings로 변환
        DifficultySettings easySettings = difficultyConfig.toEasySettings();
        DifficultySettings normalSettings = difficultyConfig.toNormalSettings();
        DifficultySettings hardSettings = difficultyConfig.toHardSettings();
        
        // Then: 검증이 예외 없이 통과해야 함
        assertDoesNotThrow(() -> easySettings.validate(), "Easy 설정 검증 실패");
        assertDoesNotThrow(() -> normalSettings.validate(), "Normal 설정 검증 실패");
        assertDoesNotThrow(() -> hardSettings.validate(), "Hard 설정 검증 실패");
        
        System.out.println("✅ 모든 DifficultySettings 검증 통과");
    }
    
    @Test
    @DisplayName("3-3. isValid() 메서드가 정상 작동해야 함")
    void testIsValidMethod() {
        // When: isValid() 호출
        boolean isValid = difficultyConfig.isValid();
        
        // Then: true를 반환해야 함
        assertTrue(isValid, "설정이 유효하지 않음");
        
        System.out.println("✅ isValid() 메서드 검증 성공");
    }
    
    // =========================================================================
    // 4. Difficulty enum 초기화 테스트
    // =========================================================================
    
    @Test
    @DisplayName("4-1. Difficulty enum이 @PostConstruct로 자동 초기화되어야 함")
    void testDifficultyEnumInitialized() {
        // Given: DifficultyInitializer의 @PostConstruct가 이미 실행됨
        
        // When: Difficulty enum에서 값 조회
        String easyDisplayName = Difficulty.EASY.getDisplayName();
        String normalDisplayName = Difficulty.NORMAL.getDisplayName();
        String hardDisplayName = Difficulty.HARD.getDisplayName();
        
        // Then: application.yml의 값이 반영되어야 함
        assertEquals("쉬움", easyDisplayName, "Easy displayName이 초기화되지 않음");
        assertEquals("보통", normalDisplayName, "Normal displayName이 초기화되지 않음");
        assertEquals("어려움", hardDisplayName, "Hard displayName이 초기화되지 않음");
        
        System.out.println("✅ Difficulty enum 자동 초기화 성공");
        System.out.println("   EASY:   " + Difficulty.EASY);
        System.out.println("   NORMAL: " + Difficulty.NORMAL);
        System.out.println("   HARD:   " + Difficulty.HARD);
    }
    
    @Test
    @DisplayName("4-2. Difficulty enum의 모든 설정값이 정확해야 함")
    void testDifficultyEnumAllValues() {
        // When: Difficulty enum에서 모든 값 조회
        
        // Then: Easy 모드 검증
        assertEquals(1.2, Difficulty.EASY.getIBlockMultiplier(), 0.001);
        assertEquals(0.8, Difficulty.EASY.getSpeedIncreaseMultiplier(), 0.001);
        assertEquals(0.5, Difficulty.EASY.getScoreMultiplier(), 0.001);  // SRS 표준: 낮은 난이도 = 낮은 배율
        assertEquals(1.2, Difficulty.EASY.getLockDelayMultiplier(), 0.001);
        
        // And: Normal 모드 검증
        assertEquals(1.0, Difficulty.NORMAL.getIBlockMultiplier(), 0.001);
        assertEquals(1.0, Difficulty.NORMAL.getSpeedIncreaseMultiplier(), 0.001);
        assertEquals(1.0, Difficulty.NORMAL.getScoreMultiplier(), 0.001);
        assertEquals(1.0, Difficulty.NORMAL.getLockDelayMultiplier(), 0.001);
        
        // And: Hard 모드 검증
        assertEquals(0.8, Difficulty.HARD.getIBlockMultiplier(), 0.001);
        assertEquals(1.2, Difficulty.HARD.getSpeedIncreaseMultiplier(), 0.001);
        assertEquals(1.5, Difficulty.HARD.getScoreMultiplier(), 0.001);  // SRS 표준: 높은 난이도 = 높은 배율
        assertEquals(0.8, Difficulty.HARD.getLockDelayMultiplier(), 0.001);
        
        System.out.println("✅ Difficulty enum 모든 설정값 검증 성공");
    }
    
    // =========================================================================
    // 5. 전체 통합 테스트
    // =========================================================================
    
    @Test
    @DisplayName("5. 전체 시스템 통합: YAML → ConfigProperties → Difficulty enum")
    void testFullIntegration() {
        // Given: Spring Boot 시작 시
        // 1. application.yml 로드
        // 2. DifficultyConfigProperties에 바인딩
        // 3. DifficultyInitializer.@PostConstruct 실행
        // 4. Difficulty.initialize() 호출
        
        // When: 전체 시스템이 초기화된 상태
        
        // Then: 모든 단계가 정상 작동해야 함
        assertNotNull(difficultyConfig, "ConfigProperties가 null");
        assertNotNull(difficultyInitializer, "Initializer가 null");
        assertTrue(difficultyConfig.isValid(), "설정이 유효하지 않음");
        
        // And: Difficulty enum이 올바르게 초기화되어야 함
        assertEquals("쉬움", Difficulty.EASY.getDisplayName());
        assertEquals("보통", Difficulty.NORMAL.getDisplayName());
        assertEquals("어려움", Difficulty.HARD.getDisplayName());
        
        // And: 게임 로직에서 사용 가능해야 함
        double easyIBlockMultiplier = Difficulty.EASY.getIBlockMultiplier();
        assertTrue(easyIBlockMultiplier > 1.0, "Easy 모드는 I-block이 증가해야 함");
        
        double hardIBlockMultiplier = Difficulty.HARD.getIBlockMultiplier();
        assertTrue(hardIBlockMultiplier < 1.0, "Hard 모드는 I-block이 감소해야 함");
        
        System.out.println("\n========================================");
        System.out.println("✅ 전체 시스템 통합 테스트 성공!");
        System.out.println("========================================");
        System.out.println("YAML → ConfigProperties → Difficulty enum");
        System.out.println("모든 단계가 정상 작동합니다.");
        System.out.println("========================================\n");
    }
}
