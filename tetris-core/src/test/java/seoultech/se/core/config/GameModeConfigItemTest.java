package seoultech.se.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.enumType.Difficulty;

/**
 * GameModeConfig 아이템 시스템 테스트
 * 
 * Arcade 모드에서 아이템이 제대로 활성화되는지 검증합니다.
 */
@DisplayName("GameModeConfig 아이템 시스템 테스트")
class GameModeConfigItemTest {

    @Test
    @DisplayName("Arcade 모드 - 아이템 타입이 정상적으로 설정되어야 함")
    void testArcadeModeWithEnabledItems() {
        // Given: 활성화할 아이템 타입 설정
        Set<ItemType> enabledItems = EnumSet.of(
            ItemType.LINE_CLEAR,
            ItemType.WEIGHT_BOMB,
            ItemType.PLUS,
            ItemType.SPEED_RESET,
            ItemType.BONUS_SCORE,
            ItemType.BOMB
        );
        
        // When: GameModeConfig 생성
        GameModeConfig config = GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .difficulty(Difficulty.NORMAL)
            .linesPerItem(1)
            .enabledItemTypes(enabledItems)
            .build();
        
        // Then: 아이템 시스템이 활성화되어야 함
        System.out.println("🔍 [Test] Config created:");
        System.out.println("   - linesPerItem: " + config.getLinesPerItem());
        System.out.println("   - enabledItemTypes: " + config.getEnabledItemTypes());
        System.out.println("   - isItemSystemEnabled: " + config.isItemSystemEnabled());
        
        assertNotNull(config.getEnabledItemTypes(), "enabledItemTypes should not be null");
        assertFalse(config.getEnabledItemTypes().isEmpty(), "enabledItemTypes should not be empty");
        assertEquals(6, config.getEnabledItemTypes().size(), "Should have 6 enabled items");
        assertTrue(config.isItemSystemEnabled(), "Item system should be enabled");
        
        // 개별 아이템 확인
        assertTrue(config.getEnabledItemTypes().contains(ItemType.LINE_CLEAR));
        assertTrue(config.getEnabledItemTypes().contains(ItemType.WEIGHT_BOMB));
        assertTrue(config.getEnabledItemTypes().contains(ItemType.PLUS));
        assertTrue(config.getEnabledItemTypes().contains(ItemType.SPEED_RESET));
        assertTrue(config.getEnabledItemTypes().contains(ItemType.BONUS_SCORE));
        assertTrue(config.getEnabledItemTypes().contains(ItemType.BOMB));
    }
    
    @Test
    @DisplayName("Arcade 모드 - null enabledItemTypes는 빈 Set으로 처리되어야 함")
    void testArcadeModeWithNullEnabledItems() {
        // When: enabledItemTypes를 null로 설정
        GameModeConfig config = GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .difficulty(Difficulty.NORMAL)
            .linesPerItem(1)
            .enabledItemTypes(null)
            .build();
        
        // Then: 빈 Set이 반환되어야 함
        assertNotNull(config.getEnabledItemTypes(), "Should return empty set instead of null");
        assertTrue(config.getEnabledItemTypes().isEmpty(), "Should be empty");
        assertFalse(config.isItemSystemEnabled(), "Item system should be disabled with null items");
    }
    
    @Test
    @DisplayName("Arcade 모드 - 빈 enabledItemTypes는 아이템 시스템 비활성화")
    void testArcadeModeWithEmptyEnabledItems() {
        // When: 빈 Set으로 설정
        GameModeConfig config = GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .difficulty(Difficulty.NORMAL)
            .linesPerItem(10)
            .enabledItemTypes(EnumSet.noneOf(ItemType.class))
            .build();
        
        // Then: 아이템 시스템이 비활성화되어야 함
        assertNotNull(config.getEnabledItemTypes());
        assertTrue(config.getEnabledItemTypes().isEmpty());
        assertFalse(config.isItemSystemEnabled(), "Item system should be disabled with empty items");
    }
    
    @Test
    @DisplayName("Arcade 모드 - linesPerItem이 0이면 아이템 시스템 비활성화")
    void testArcadeModeWithZeroLinesPerItem() {
        // Given
        Set<ItemType> enabledItems = EnumSet.of(ItemType.LINE_CLEAR, ItemType.BOMB);
        
        // When: linesPerItem을 0으로 설정
        GameModeConfig config = GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .difficulty(Difficulty.NORMAL)
            .linesPerItem(0)
            .enabledItemTypes(enabledItems)
            .build();
        
        // Then: 아이템 시스템이 비활성화되어야 함
        assertFalse(config.isItemSystemEnabled(), "Item system should be disabled with linesPerItem=0");
    }
    
    @Test
    @DisplayName("createDefaultArcade() 헬퍼 메서드 - 아이템 시스템 활성화 확인")
    void testCreateDefaultArcadeHasEnabledItems() {
        // When: 기본 Arcade 설정 생성
        GameModeConfig config = GameModeConfig.createDefaultArcade();
        
        // Then: 아이템 시스템이 활성화되어야 함
        System.out.println("🔍 [Test] Default Arcade Config:");
        System.out.println("   - linesPerItem: " + config.getLinesPerItem());
        System.out.println("   - enabledItemTypes: " + config.getEnabledItemTypes());
        System.out.println("   - isItemSystemEnabled: " + config.isItemSystemEnabled());
        
        assertTrue(config.isItemSystemEnabled(), "Default Arcade should have items enabled");
        assertFalse(config.getEnabledItemTypes().isEmpty(), "Default Arcade should have enabled items");
        assertTrue(config.getLinesPerItem() > 0, "linesPerItem should be positive");
    }
}
