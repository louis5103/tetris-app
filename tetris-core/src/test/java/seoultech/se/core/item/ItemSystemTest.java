package seoultech.se.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.item.impl.BombItem;
import seoultech.se.core.item.impl.BonusScoreItem;
import seoultech.se.core.item.impl.PlusItem;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.enumType.Color;

/**
 * 아이템 시스템 통합 테스트
 */
class ItemSystemTest {
    
    private GameState gameState;
    private ItemManager itemManager;
    
    @BeforeEach
    void setUp() {
        // 10x20 보드 생성
        gameState = new GameState(10, 20);
        
        // 모든 아이템 활성화된 ItemManager 생성
        itemManager = new ItemManager(0.1, EnumSet.allOf(ItemType.class));
        
        // 아이템 등록
        itemManager.registerItem(new BombItem());
        itemManager.registerItem(new PlusItem());
        itemManager.registerItem(new BonusScoreItem());
    }
    
    @Test
    void testBombItem_ClearsCells() {
        // Given: 5x5 영역에 블록 배치
        for (int r = 3; r <= 7; r++) {
            for (int c = 3; c <= 7; c++) {
                gameState.getGrid()[r][c] = Cell.of(Color.RED);
            }
        }
        
        // When: 중심 (5, 5)에 폭탄 사용
        BombItem bomb = new BombItem();
        ItemEffect effect = bomb.apply(gameState, 5, 5);
        
        // Then: 25개 블록 제거, 125점 획득
        assertTrue(effect.isSuccess());
        assertEquals(25, effect.getBlocksCleared());
        assertEquals(125, effect.getBonusScore());
        assertEquals(ItemType.BOMB, effect.getItemType());
    }
    
    @Test
    void testPlusItem_ClearsRowAndColumn() {
        // Given: 특정 행과 열에 블록 배치
        // 5번 행 전체 채우기
        for (int c = 0; c < 10; c++) {
            gameState.getGrid()[5][c] = Cell.of(Color.BLUE);
        }
        // 5번 열 전체 채우기
        for (int r = 0; r < 20; r++) {
            if (r != 5) { // 교차점 제외
                gameState.getGrid()[r][5] = Cell.of(Color.GREEN);
            }
        }
        
        // When: (5, 5)에 Plus 아이템 사용
        PlusItem plus = new PlusItem();
        ItemEffect effect = plus.apply(gameState, 5, 5);
        
        // Then: 행 10개 + 열 19개 = 29개 제거
        assertTrue(effect.isSuccess());
        assertEquals(29, effect.getBlocksCleared());
        assertEquals(145, effect.getBonusScore());
    }
    
    @Test
    void testBonusScoreItem_AddsScore() {
        // Given: 레벨 5
        gameState.setLevel(5);
        long initialScore = gameState.getScore();
        
        // When: 보너스 점수 아이템 사용
        BonusScoreItem bonusScore = new BonusScoreItem();
        ItemEffect effect = bonusScore.apply(gameState, 0, 0);
        
        // Then: 500 + (5 × 50) = 750점 추가
        assertTrue(effect.isSuccess());
        assertEquals(750, effect.getBonusScore());
        assertEquals(initialScore + 750, gameState.getScore());
    }
    
    @Test
    void testItemManager_GeneratesRandomItem() {
        // Given: ItemManager 설정됨
        
        // When: 랜덤 아이템 10개 생성
        int generatedCount = 0;
        for (int i = 0; i < 100; i++) {
            Item item = itemManager.generateRandomItem();
            if (item != null) {
                generatedCount++;
                assertTrue(item.isEnabled());
            }
        }
        
        // Then: 아이템이 생성되어야 함
        assertTrue(generatedCount > 0);
    }
    
    @Test
    void testItemManager_ShouldDropItem() {
        // Given: 10% 드롭률
        
        // When: 1000번 체크
        int dropCount = 0;
        for (int i = 0; i < 1000; i++) {
            if (itemManager.shouldDropItem()) {
                dropCount++;
            }
        }
        
        // Then: 약 100번 (10%) 드롭되어야 함 (80~120 범위)
        assertTrue(dropCount >= 50 && dropCount <= 150, 
            "Expected drop count around 100, but was: " + dropCount);
    }
    
    @Test
    void testItemManager_EnableDisableItem() {
        // Given: BOMB 활성화
        assertTrue(itemManager.isItemEnabled(ItemType.BOMB));
        
        // When: BOMB 비활성화
        itemManager.disableItem(ItemType.BOMB);
        
        // Then: BOMB은 생성되지 않아야 함
        assertFalse(itemManager.isItemEnabled(ItemType.BOMB));
        
        // 100번 시도해도 BOMB은 생성 안 됨
        for (int i = 0; i < 100; i++) {
            Item item = itemManager.generateRandomItem();
            if (item != null) {
                assertNotEquals(ItemType.BOMB, item.getType());
            }
        }
    }
    
    @Test
    void testItemConfig_ArcadeDefault() {
        // When: 아케이드 기본 설정 생성
        ItemConfig config = ItemConfig.arcadeDefault();
        
        // Then: 모든 아이템 활성화, 10% 드롭률
        assertTrue(config.isEnabled());
        assertEquals(0.1, config.getDropRate());
        assertEquals(4, config.getEnabledItems().size());
        assertEquals(3, config.getMaxInventorySize());
        assertFalse(config.isAutoUse());
    }
    
    @Test
    void testItemConfig_WithSpecificItems() {
        // When: BOMB과 PLUS만 활성화
        ItemConfig config = ItemConfig.withItems(ItemType.BOMB, ItemType.PLUS);
        
        // Then: 2개 아이템만 활성화
        assertTrue(config.isItemEnabled(ItemType.BOMB));
        assertTrue(config.isItemEnabled(ItemType.PLUS));
        assertFalse(config.isItemEnabled(ItemType.SPEED_RESET));
        assertFalse(config.isItemEnabled(ItemType.BONUS_SCORE));
    }
    
    @Test
    void testItemConfig_Disabled() {
        // When: 비활성화 설정
        ItemConfig config = ItemConfig.disabled();
        
        // Then: 아이템 시스템 완전 비활성화
        assertFalse(config.isEnabled());
        assertEquals(0.0, config.getDropRate());
        assertEquals(0, config.getEnabledItems().size());
    }
    
    @Test
    void testItemEffect_None() {
        // When: 효과 없음
        ItemEffect effect = ItemEffect.none();
        
        // Then: 실패 상태
        assertFalse(effect.isSuccess());
        assertEquals(0, effect.getBlocksCleared());
        assertEquals(0, effect.getBonusScore());
    }
}
