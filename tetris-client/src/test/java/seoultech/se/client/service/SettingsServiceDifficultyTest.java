package seoultech.se.client.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import seoultech.se.core.model.enumType.Difficulty;

/**
 * Phase 5 통합 테스트: UI에서 난이도 선택 기능 검증
 * 
 * <p>테스트 범위:</p>
 * <ul>
 *   <li>SettingsService의 난이도 저장/로드</li>
 *   <li>난이도 기본값 설정</li>
 *   <li>난이도 변경 및 저장</li>
 *   <li>Difficulty enum 변환</li>
 * </ul>
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 5
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application.yml")
@DisplayName("Phase 5: UI 난이도 선택 기능 통합 테스트")
class SettingsServiceDifficultyTest {
    
    @Autowired
    private SettingsService settingsService;
    
    @BeforeEach
    void setUp() {
        // 각 테스트 전에 기본값으로 복원
        settingsService.restoreDefaults();
    }
    
    // =========================================================================
    // 1. 난이도 기본값 테스트
    // =========================================================================
    
    @Test
    @DisplayName("1-1. 기본 난이도는 NORMAL이어야 함")
    void testDefaultDifficultyIsNormal() {
        // When: 기본값 로드
        Difficulty difficulty = settingsService.getCurrentDifficulty();
        
        // Then: NORMAL이어야 함
        assertEquals(Difficulty.NORMAL, difficulty,
            "기본 난이도가 NORMAL이 아닙니다");
        
        System.out.println("✅ 기본 난이도: " + difficulty.getDisplayName());
    }
    
    @Test
    @DisplayName("1-2. 난이도 Property가 null이 아니어야 함")
    void testDifficultyPropertyNotNull() {
        // When: 난이도 Property 가져오기
        var property = settingsService.difficultyProperty();
        
        // Then: null이 아니어야 함
        assertNotNull(property, "Difficulty Property가 null입니다");
        assertNotNull(property.get(), "Difficulty Property 값이 null입니다");
        
        System.out.println("✅ Difficulty Property: " + property.get());
    }
    
    // =========================================================================
    // 2. 난이도 변경 및 저장 테스트
    // =========================================================================
    
    @Test
    @DisplayName("2-1. Easy 난이도로 변경 가능해야 함")
    void testChangeDifficultyToEasy() {
        // Given: 기본 난이도 (NORMAL)
        assertEquals(Difficulty.NORMAL, settingsService.getCurrentDifficulty());
        
        // When: Easy로 변경
        settingsService.difficultyProperty().set("difficultyEasy");
        
        // Then: Easy가 되어야 함
        assertEquals(Difficulty.EASY, settingsService.getCurrentDifficulty());
        
        System.out.println("✅ 난이도를 Easy로 변경 성공");
    }
    
    @Test
    @DisplayName("2-2. Hard 난이도로 변경 가능해야 함")
    void testChangeDifficultyToHard() {
        // Given: 기본 난이도 (NORMAL)
        assertEquals(Difficulty.NORMAL, settingsService.getCurrentDifficulty());
        
        // When: Hard로 변경
        settingsService.difficultyProperty().set("difficultyHard");
        
        // Then: Hard가 되어야 함
        assertEquals(Difficulty.HARD, settingsService.getCurrentDifficulty());
        
        System.out.println("✅ 난이도를 Hard로 변경 성공");
    }
    
    @Test
    @DisplayName("2-3. 난이도 변경 후 저장/로드가 정상 작동해야 함")
    void testSaveAndLoadDifficulty() {
        // Given: Easy로 변경 후 저장
        settingsService.difficultyProperty().set("difficultyEasy");
        settingsService.saveSettings();
        
        // When: 설정 다시 로드
        settingsService.loadSettings();
        
        // Then: 여전히 Easy여야 함
        assertEquals(Difficulty.EASY, settingsService.getCurrentDifficulty());
        
        System.out.println("✅ 난이도 저장/로드 정상 작동");
    }
    
    // =========================================================================
    // 3. Difficulty enum 변환 테스트
    // =========================================================================
    
    @Test
    @DisplayName("3-1. difficultyEasy → EASY 변환 확인")
    void testConvertEasyDifficulty() {
        // When: difficultyEasy 설정
        settingsService.difficultyProperty().set("difficultyEasy");
        Difficulty difficulty = settingsService.getCurrentDifficulty();
        
        // Then: EASY enum이어야 함
        assertEquals(Difficulty.EASY, difficulty);
        assertEquals("쉬움", difficulty.getDisplayName());
        assertEquals(1.2, difficulty.getIBlockMultiplier(), 0.001);
        assertEquals(1.2, difficulty.getScoreMultiplier(), 0.001);
        
        System.out.println("✅ Easy 변환: " + difficulty.getDisplayName());
    }
    
    @Test
    @DisplayName("3-2. difficultyNormal → NORMAL 변환 확인")
    void testConvertNormalDifficulty() {
        // When: difficultyNormal 설정
        settingsService.difficultyProperty().set("difficultyNormal");
        Difficulty difficulty = settingsService.getCurrentDifficulty();
        
        // Then: NORMAL enum이어야 함
        assertEquals(Difficulty.NORMAL, difficulty);
        assertEquals("보통", difficulty.getDisplayName());
        assertEquals(1.0, difficulty.getIBlockMultiplier(), 0.001);
        assertEquals(1.0, difficulty.getScoreMultiplier(), 0.001);
        
        System.out.println("✅ Normal 변환: " + difficulty.getDisplayName());
    }
    
    @Test
    @DisplayName("3-3. difficultyHard → HARD 변환 확인")
    void testConvertHardDifficulty() {
        // When: difficultyHard 설정
        settingsService.difficultyProperty().set("difficultyHard");
        Difficulty difficulty = settingsService.getCurrentDifficulty();
        
        // Then: HARD enum이어야 함
        assertEquals(Difficulty.HARD, difficulty);
        assertEquals("어려움", difficulty.getDisplayName());
        assertEquals(0.8, difficulty.getIBlockMultiplier(), 0.001);
        assertEquals(0.8, difficulty.getScoreMultiplier(), 0.001);
        
        System.out.println("✅ Hard 변환: " + difficulty.getDisplayName());
    }
    
    @Test
    @DisplayName("3-4. 잘못된 값은 NORMAL로 폴백해야 함")
    void testInvalidDifficultyFallbackToNormal() {
        // When: 잘못된 값 설정
        settingsService.difficultyProperty().set("invalidDifficulty");
        Difficulty difficulty = settingsService.getCurrentDifficulty();
        
        // Then: NORMAL로 폴백되어야 함
        assertEquals(Difficulty.NORMAL, difficulty);
        
        System.out.println("✅ 잘못된 값 → NORMAL 폴백");
    }
    
    // =========================================================================
    // 4. 전체 시스템 통합 테스트
    // =========================================================================
    
    @Test
    @DisplayName("4. 전체 시스템 통합: 난이도 설정 → 저장 → 로드 → 변환")
    void testFullDifficultyWorkflow() {
        System.out.println("\n========================================");
        System.out.println("전체 난이도 워크플로우 테스트");
        System.out.println("========================================\n");
        
        // 1. 기본값 확인
        System.out.println("1️⃣ 기본값 확인:");
        Difficulty defaultDifficulty = settingsService.getCurrentDifficulty();
        assertEquals(Difficulty.NORMAL, defaultDifficulty);
        System.out.println("   ✅ 기본값: " + defaultDifficulty.getDisplayName() + "\n");
        
        // 2. Easy로 변경
        System.out.println("2️⃣ Easy로 변경:");
        settingsService.difficultyProperty().set("difficultyEasy");
        settingsService.saveSettings();
        Difficulty easyDifficulty = settingsService.getCurrentDifficulty();
        assertEquals(Difficulty.EASY, easyDifficulty);
        System.out.println("   ✅ Easy 설정 완료");
        System.out.println("   - I-Block: " + easyDifficulty.getIBlockMultiplier() + "x");
        System.out.println("   - Score: " + easyDifficulty.getScoreMultiplier() + "x\n");
        
        // 3. 로드 후 확인
        System.out.println("3️⃣ 설정 로드 후 확인:");
        settingsService.loadSettings();
        Difficulty loadedDifficulty = settingsService.getCurrentDifficulty();
        assertEquals(Difficulty.EASY, loadedDifficulty);
        System.out.println("   ✅ Easy 유지됨\n");
        
        // 4. Hard로 변경
        System.out.println("4️⃣ Hard로 변경:");
        settingsService.difficultyProperty().set("difficultyHard");
        settingsService.saveSettings();
        Difficulty hardDifficulty = settingsService.getCurrentDifficulty();
        assertEquals(Difficulty.HARD, hardDifficulty);
        System.out.println("   ✅ Hard 설정 완료");
        System.out.println("   - I-Block: " + hardDifficulty.getIBlockMultiplier() + "x");
        System.out.println("   - Score: " + hardDifficulty.getScoreMultiplier() + "x\n");
        
        // 5. 기본값 복원
        System.out.println("5️⃣ 기본값 복원:");
        settingsService.restoreDefaults();
        Difficulty restoredDifficulty = settingsService.getCurrentDifficulty();
        assertEquals(Difficulty.NORMAL, restoredDifficulty);
        System.out.println("   ✅ Normal로 복원됨\n");
        
        System.out.println("========================================");
        System.out.println("✅ 전체 워크플로우 성공!");
        System.out.println("========================================\n");
    }
}
