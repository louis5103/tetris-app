package seoultech.se.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * GameModeConfig 클래스 테스트
 * 
 * Phase 1에서 추가된 새로운 필드와 프리셋 메서드를 테스트합니다.
 */
@DisplayName("GameModeConfig 테스트")
class GameModeConfigTest {
    
    @Test
    @DisplayName("클래식 프리셋 - 기본 SRS 활성화")
    void testClassicPresetDefault() {
        // Given & When
        GameModeConfig config = GameModeConfig.classic();
        
        // Then
        assertEquals(GameplayType.CLASSIC, config.getGameplayType(), 
            "클래식 모드의 게임플레이 타입은 CLASSIC이어야 합니다.");
        assertTrue(config.isSrsEnabled(), 
            "클래식 모드는 기본적으로 SRS가 활성화되어야 합니다.");
        assertEquals(1.0, config.getDropSpeedMultiplier(), 
            "클래식 모드의 낙하 속도는 기본값이어야 합니다.");
    }
    
    @Test
    @DisplayName("클래식 프리셋 - SRS 비활성화")
    void testClassicPresetWithSrsDisabled() {
        // Given & When
        GameModeConfig config = GameModeConfig.classic(false);
        
        // Then
        assertEquals(GameplayType.CLASSIC, config.getGameplayType());
        assertFalse(config.isSrsEnabled(), 
            "SRS를 비활성화한 클래식 모드는 SRS가 꺼져있어야 합니다.");
    }
    
    @Test
    @DisplayName("클래식 프리셋 - SRS 활성화")
    void testClassicPresetWithSrsEnabled() {
        // Given & When
        GameModeConfig config = GameModeConfig.classic(true);
        
        // Then
        assertEquals(GameplayType.CLASSIC, config.getGameplayType());
        assertTrue(config.isSrsEnabled(), 
            "SRS를 활성화한 클래식 모드는 SRS가 켜져있어야 합니다.");
    }
    
    @Test
    @DisplayName("아케이드 프리셋 - 모든 설정 확인")
    void testArcadePreset() {
        // Given & When
        GameModeConfig config = GameModeConfig.arcade();
        
        // Then
        assertEquals(GameplayType.ARCADE, config.getGameplayType(), 
            "아케이드 모드의 게임플레이 타입은 ARCADE여야 합니다.");
        assertTrue(config.isSrsEnabled(), 
            "아케이드 모드는 SRS가 활성화되어야 합니다.");
        assertEquals(1.5, config.getDropSpeedMultiplier(), 
            "아케이드 모드의 낙하 속도는 1.5배여야 합니다.");
        assertEquals(300, config.getLockDelay(), 
            "아케이드 모드의 락 딜레이는 300ms여야 합니다.");
    }
    
    @Test
    @DisplayName("Builder 패턴 - gameplayType 설정")
    void testBuilderWithGameplayType() {
        // Given & When
        GameModeConfig config = GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .build();
        
        // Then
        assertEquals(GameplayType.ARCADE, config.getGameplayType());
    }
    
    @Test
    @DisplayName("Builder 패턴 - srsEnabled 설정")
    void testBuilderWithSrsDisabled() {
        // Given & When
        GameModeConfig config = GameModeConfig.builder()
            .srsEnabled(false)
            .build();
        
        // Then
        assertFalse(config.isSrsEnabled());
    }
    
    @Test
    @DisplayName("Builder 패턴 - 모든 새 필드 설정")
    void testBuilderWithAllNewFields() {
        // Given & When
        GameModeConfig config = GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .srsEnabled(false)
            .dropSpeedMultiplier(2.0)
            .build();
        
        // Then
        assertEquals(GameplayType.ARCADE, config.getGameplayType());
        assertFalse(config.isSrsEnabled());
        assertEquals(2.0, config.getDropSpeedMultiplier());
    }
    
    @Test
    @DisplayName("기본값 테스트 - gameplayType과 srsEnabled")
    void testDefaultValues() {
        // Given & When
        GameModeConfig config = GameModeConfig.builder().build();
        
        // Then
        assertEquals(GameplayType.CLASSIC, config.getGameplayType(), 
            "gameplayType의 기본값은 CLASSIC이어야 합니다.");
        assertTrue(config.isSrsEnabled(), 
            "srsEnabled의 기본값은 true여야 합니다.");
    }
    
    @Test
    @DisplayName("커스텀 빌더 - arcade 기반 변경")
    void testCustomBuilderFromArcade() {
        // Given
        GameModeConfig arcade = GameModeConfig.arcade();
        
        // When - arcade 모드를 참고하여 새로운 설정 생성
        GameModeConfig modified = GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .srsEnabled(false)  // SRS만 비활성화
            .dropSpeedMultiplier(1.5)
            .lockDelay(300)
            .build();
        
        // Then
        assertEquals(GameplayType.ARCADE, modified.getGameplayType(), 
            "게임플레이 타입은 ARCADE여야 합니다.");
        assertFalse(modified.isSrsEnabled(), 
            "SRS는 비활성화되어야 합니다.");
        assertEquals(1.5, modified.getDropSpeedMultiplier(), 
            "낙하 속도는 arcade와 동일해야 합니다.");
    }
}
