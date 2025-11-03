package seoultech.se.core.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DifficultySettings 단위 테스트
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 1
 */
@DisplayName("DifficultySettings 테스트")
class DifficultySettingsTest {
    
    @Test
    @DisplayName("Easy 모드 기본값 생성")
    void testCreateEasyDefaults() {
        DifficultySettings easy = DifficultySettings.createEasyDefaults();
        
        assertNotNull(easy);
        assertEquals("쉬움", easy.getDisplayName());
        assertEquals(1.2, easy.getIBlockMultiplier(), 0.001);
        assertEquals(0.8, easy.getSpeedIncreaseMultiplier(), 0.001);
        assertEquals(1.2, easy.getScoreMultiplier(), 0.001);
        assertEquals(1.2, easy.getLockDelayMultiplier(), 0.001);
    }
    
    @Test
    @DisplayName("Normal 모드 기본값 생성")
    void testCreateNormalDefaults() {
        DifficultySettings normal = DifficultySettings.createNormalDefaults();
        
        assertNotNull(normal);
        assertEquals("보통", normal.getDisplayName());
        assertEquals(1.0, normal.getIBlockMultiplier(), 0.001);
        assertEquals(1.0, normal.getSpeedIncreaseMultiplier(), 0.001);
        assertEquals(1.0, normal.getScoreMultiplier(), 0.001);
        assertEquals(1.0, normal.getLockDelayMultiplier(), 0.001);
    }
    
    @Test
    @DisplayName("Hard 모드 기본값 생성")
    void testCreateHardDefaults() {
        DifficultySettings hard = DifficultySettings.createHardDefaults();
        
        assertNotNull(hard);
        assertEquals("어려움", hard.getDisplayName());
        assertEquals(0.8, hard.getIBlockMultiplier(), 0.001);
        assertEquals(1.2, hard.getSpeedIncreaseMultiplier(), 0.001);
        assertEquals(0.8, hard.getScoreMultiplier(), 0.001);
        assertEquals(0.8, hard.getLockDelayMultiplier(), 0.001);
    }
    
    @Test
    @DisplayName("Builder 패턴으로 생성")
    void testBuilderPattern() {
        DifficultySettings custom = DifficultySettings.builder()
            .displayName("커스텀")
            .iBlockMultiplier(1.5)
            .speedIncreaseMultiplier(0.9)
            .scoreMultiplier(1.1)
            .lockDelayMultiplier(1.3)
            .build();
        
        assertNotNull(custom);
        assertEquals("커스텀", custom.getDisplayName());
        assertEquals(1.5, custom.getIBlockMultiplier(), 0.001);
        assertEquals(0.9, custom.getSpeedIncreaseMultiplier(), 0.001);
        assertEquals(1.1, custom.getScoreMultiplier(), 0.001);
        assertEquals(1.3, custom.getLockDelayMultiplier(), 0.001);
    }
    
    @Test
    @DisplayName("검증 성공 - 유효한 값")
    void testValidateSuccess() {
        DifficultySettings settings = DifficultySettings.createNormalDefaults();
        
        // 예외가 발생하지 않아야 함
        assertDoesNotThrow(() -> settings.validate());
    }
    
    @Test
    @DisplayName("검증 실패 - displayName null")
    void testValidateFailureNullDisplayName() {
        DifficultySettings settings = DifficultySettings.builder()
            .displayName(null)
            .iBlockMultiplier(1.0)
            .build();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> settings.validate()
        );
        
        assertTrue(exception.getMessage().contains("Display name"));
    }
    
    @Test
    @DisplayName("검증 실패 - I-block multiplier 범위 초과")
    void testValidateFailureIBlockMultiplierOutOfRange() {
        DifficultySettings settings = DifficultySettings.builder()
            .displayName("테스트")
            .iBlockMultiplier(5.0)  // 3.0 초과
            .build();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> settings.validate()
        );
        
        assertTrue(exception.getMessage().contains("I-block multiplier"));
    }
    
    @Test
    @DisplayName("검증 실패 - 음수 multiplier")
    void testValidateFailureNegativeMultiplier() {
        DifficultySettings settings = DifficultySettings.builder()
            .displayName("테스트")
            .scoreMultiplier(-0.5)  // 음수
            .build();
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> settings.validate()
        );
        
        assertTrue(exception.getMessage().contains("Score multiplier"));
    }
    
    @Test
    @DisplayName("toString 메서드")
    void testToString() {
        DifficultySettings settings = DifficultySettings.createEasyDefaults();
        
        String result = settings.toString();
        
        assertNotNull(result);
        assertTrue(result.contains("쉬움"));
        assertTrue(result.contains("1.20"));
        assertTrue(result.contains("0.80"));
    }
    
    @Test
    @DisplayName("경계값 테스트 - 최소값 (0.1)")
    void testBoundaryMinimum() {
        DifficultySettings settings = DifficultySettings.builder()
            .displayName("최소")
            .iBlockMultiplier(0.1)
            .speedIncreaseMultiplier(0.1)
            .scoreMultiplier(0.1)
            .lockDelayMultiplier(0.1)
            .build();
        
        assertDoesNotThrow(() -> settings.validate());
    }
    
    @Test
    @DisplayName("경계값 테스트 - 최대값 (3.0)")
    void testBoundaryMaximum() {
        DifficultySettings settings = DifficultySettings.builder()
            .displayName("최대")
            .iBlockMultiplier(3.0)
            .speedIncreaseMultiplier(3.0)
            .scoreMultiplier(3.0)
            .lockDelayMultiplier(3.0)
            .build();
        
        assertDoesNotThrow(() -> settings.validate());
    }
}
