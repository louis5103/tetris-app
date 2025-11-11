package seoultech.se.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * ItemManager 테스트
 * 
 * 특히 10줄 카운터 기반 아이템 생성을 검증합니다.
 */
public class ItemManagerTest {
    
    private ItemManager itemManager;
    
    @BeforeEach
    public void setUp() {
        // 모든 아이템 활성화, 10% 드롭률 (하지만 카운터 기반이므로 확률은 무시됨)
        itemManager = new ItemManager(0.1, EnumSet.allOf(ItemType.class));
    }
    
    @Test
    public void testItemGenerationAfter10Lines() {
        System.out.println("\n========== Test: Item Generation After 10 Lines ==========");
        
        // 초기 상태: 10줄까지 남음
        assertEquals(10, itemManager.getLinesUntilNextItem());
        
        // 1줄 클리어 × 9번 = 9줄 클리어
        for (int i = 1; i <= 9; i++) {
            ItemType result = itemManager.checkAndGenerateItem(1);
            assertNull(result, "Should not generate item before 10 lines (cleared " + i + " lines)");
            System.out.println("After clearing " + i + " line(s): " + itemManager.getLinesUntilNextItem() + " lines until next item");
        }
        
        // 남은 줄: 1줄
        assertEquals(1, itemManager.getLinesUntilNextItem());
        
        // 10번째 줄 클리어 → 아이템 생성!
        ItemType result = itemManager.checkAndGenerateItem(1);
        assertNotNull(result, "Should generate item after 10 lines");
        System.out.println("✅ Item generated after 10 lines: " + result);
        
        // 카운터 리셋 확인
        assertEquals(10, itemManager.getLinesUntilNextItem());
        
        System.out.println("========================================================\n");
    }
    
    @Test
    public void testItemGenerationWithMultipleLinesCleared() {
        System.out.println("\n========== Test: Item Generation With Multiple Lines Cleared ==========");
        
        // 4줄 클리어 (Tetris)
        ItemType result1 = itemManager.checkAndGenerateItem(4);
        assertNull(result1, "Should not generate item after 4 lines");
        assertEquals(6, itemManager.getLinesUntilNextItem());
        System.out.println("After clearing 4 lines: " + itemManager.getLinesUntilNextItem() + " lines until next item");
        
        // 3줄 클리어 (Triple)
        ItemType result2 = itemManager.checkAndGenerateItem(3);
        assertNull(result2, "Should not generate item after 7 lines total");
        assertEquals(3, itemManager.getLinesUntilNextItem());
        System.out.println("After clearing 7 lines total: " + itemManager.getLinesUntilNextItem() + " lines until next item");
        
        // 3줄 클리어 → 총 10줄, 아이템 생성!
        ItemType result3 = itemManager.checkAndGenerateItem(3);
        assertNotNull(result3, "Should generate item after 10 lines total");
        System.out.println("✅ Item generated after 10 lines: " + result3);
        
        // 카운터는 10 - 3 = 7 (초과분이 있으므로)
        // 아니다 - 코드를 보면 10으로 리셋됨
        assertEquals(10, itemManager.getLinesUntilNextItem());
        
        System.out.println("==================================================================\n");
    }
    
    @Test
    public void testItemGenerationWithOverflow() {
        System.out.println("\n========== Test: Item Generation With Overflow ==========");
        
        // 9줄 클리어
        itemManager.checkAndGenerateItem(9);
        assertEquals(1, itemManager.getLinesUntilNextItem());
        System.out.println("After clearing 9 lines: " + itemManager.getLinesUntilNextItem() + " lines until next item");
        
        // 4줄 클리어 (총 13줄) → 아이템 생성하고 카운터 리셋
        ItemType result = itemManager.checkAndGenerateItem(4);
        assertNotNull(result, "Should generate item when counter goes negative");
        System.out.println("✅ Item generated after 13 lines total: " + result);
        
        // 카운터는 10으로 리셋됨 (코드 확인 필요)
        assertEquals(10, itemManager.getLinesUntilNextItem());
        
        System.out.println("=========================================================\n");
    }
    
    @Test
    public void testNoItemGenerationWithZeroLines() {
        System.out.println("\n========== Test: No Item Generation With Zero Lines ==========");
        
        // 0줄 클리어 → 아이템 생성 안 됨
        ItemType result = itemManager.checkAndGenerateItem(0);
        assertNull(result, "Should not generate item with 0 lines cleared");
        assertEquals(10, itemManager.getLinesUntilNextItem());
        
        System.out.println("✅ No item generated with 0 lines cleared");
        System.out.println("==============================================================\n");
    }
    
    @Test
    public void testGeneratedItemTypeIsEnabled() {
        System.out.println("\n========== Test: Generated Item Type Is Enabled ==========");
        
        // 10줄 클리어하여 아이템 생성
        ItemType result = itemManager.checkAndGenerateItem(10);
        assertNotNull(result, "Should generate item after 10 lines");
        
        // 생성된 아이템이 활성화된 아이템인지 확인
        assertTrue(itemManager.isItemEnabled(result), "Generated item should be enabled");
        System.out.println("✅ Generated item " + result + " is enabled");
        
        System.out.println("==========================================================\n");
    }
}
