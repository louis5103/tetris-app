package seoultech.se.core.model.enumType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import seoultech.se.core.config.DifficultySettings;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Difficulty Enum 단위 테스트
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 2
 */
@DisplayName("Difficulty Enum 테스트")
class DifficultyTest {
    
    @Test
    @DisplayName("기본값 초기화 확인")
    void testDefaultInitialization() {
        // EASY
        assertEquals("쉬움", Difficulty.EASY.getDisplayName());
        assertEquals(1.2, Difficulty.EASY.getIBlockMultiplier(), 0.001);
        assertEquals(0.8, Difficulty.EASY.getSpeedIncreaseMultiplier(), 0.001);
        
        // NORMAL
        assertEquals("보통", Difficulty.NORMAL.getDisplayName());
        assertEquals(1.0, Difficulty.NORMAL.getIBlockMultiplier(), 0.001);
        
        // HARD
        assertEquals("어려움", Difficulty.HARD.getDisplayName());
        assertEquals(0.8, Difficulty.HARD.getIBlockMultiplier(), 0.001);
        assertEquals(1.2, Difficulty.HARD.getSpeedIncreaseMultiplier(), 0.001);
    }
    
    @Test
    @DisplayName("외부 설정으로 초기화")
    void testInitializeWithCustomSettings() {
        // 커스텀 설정 생성
        DifficultySettings customEasy = DifficultySettings.builder()
            .displayName("매우 쉬움")
            .iBlockMultiplier(2.0)
            .speedIncreaseMultiplier(0.5)
            .scoreMultiplier(1.5)
            .lockDelayMultiplier(1.5)
            .build();
        
        DifficultySettings customNormal = DifficultySettings.createNormalDefaults();
        DifficultySettings customHard = DifficultySettings.createHardDefaults();
        
        // 초기화
        Difficulty.initialize(customEasy, customNormal, customHard);
        
        // 검증
        assertEquals("매우 쉬움", Difficulty.EASY.getDisplayName());
        assertEquals(2.0, Difficulty.EASY.getIBlockMultiplier(), 0.001);
        assertEquals(0.5, Difficulty.EASY.getSpeedIncreaseMultiplier(), 0.001);
    }
    
    @Test
    @DisplayName("Convenience getter 메서드")
    void testConvenienceGetters() {
        Difficulty easy = Difficulty.EASY;
        
        assertNotNull(easy.getDisplayName());
        assertTrue(easy.getIBlockMultiplier() > 0);
        assertTrue(easy.getSpeedIncreaseMultiplier() > 0);
        assertTrue(easy.getScoreMultiplier() > 0);
        assertTrue(easy.getLockDelayMultiplier() > 0);
    }
    
    @Test
    @DisplayName("fromName 메서드 - 정상 케이스")
    void testFromNameSuccess() {
        assertEquals(Difficulty.EASY, Difficulty.fromName("EASY"));
        assertEquals(Difficulty.EASY, Difficulty.fromName("easy"));
        assertEquals(Difficulty.NORMAL, Difficulty.fromName("NORMAL"));
        assertEquals(Difficulty.HARD, Difficulty.fromName("HARD"));
    }
    
    @Test
    @DisplayName("fromName 메서드 - 잘못된 이름")
    void testFromNameInvalid() {
        // 잘못된 이름은 NORMAL 반환
        assertEquals(Difficulty.NORMAL, Difficulty.fromName("INVALID"));
        assertEquals(Difficulty.NORMAL, Difficulty.fromName(null));
        assertEquals(Difficulty.NORMAL, Difficulty.fromName(""));
    }
    
    @Test
    @DisplayName("toString 메서드")
    void testToString() {
        String result = Difficulty.EASY.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("EASY"));
        assertTrue(result.contains("쉬움"));
    }
    
    @Test
    @DisplayName("getSettings 메서드")
    void testGetSettings() {
        DifficultySettings settings = Difficulty.EASY.getSettings();
        
        assertNotNull(settings);
        assertEquals("쉬움", settings.getDisplayName());
    }
    
    @Test
    @DisplayName("모든 난이도 열거")
    void testAllDifficulties() {
        Difficulty[] difficulties = Difficulty.values();
        
        assertEquals(3, difficulties.length);
        assertEquals(Difficulty.EASY, difficulties[0]);
        assertEquals(Difficulty.NORMAL, difficulties[1]);
        assertEquals(Difficulty.HARD, difficulties[2]);
    }
}
