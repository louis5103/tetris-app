package seoultech.se.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.engine.item.ItemManager;
import seoultech.se.core.engine.item.ItemType;

/**
 * ItemManager 테스트 (Stateless 리팩토링)
 *
 * 특히 10줄 카운터 기반 아이템 생성을 검증합니다.
 * ItemManager는 더 이상 상태를 갖지 않으므로 GameState를 통해 테스트합니다.
 */
public class ItemManagerTest {

    private ItemManager itemManager;
    private GameState gameState;

    @BeforeEach
    public void setUp() {
        // 모든 아이템 활성화, 10% 드롭률 (하지만 카운터 기반이므로 확률은 무시됨)
        itemManager = new ItemManager(0.1, EnumSet.allOf(ItemType.class));
        // Stateless 리팩토링: GameState 생성
        gameState = new GameState(10, 20);
    }

    @Test
    public void testItemGenerationAfter10Lines() {
        System.out.println("\n========== Test: Item Generation After 10 Lines ==========");

        // 초기 상태: 10줄까지 남음 (GameState에서 확인)
        assertEquals(10, gameState.getLinesUntilNextItem());

        // 1줄 클리어 × 9번 = 9줄 클리어
        for (int i = 1; i <= 9; i++) {
            gameState = itemManager.checkAndGenerateItem(gameState, 1);
            assertNull(gameState.getNextBlockItemType(), "Should not generate item before 10 lines (cleared " + i + " lines)");
            System.out.println("After clearing " + i + " line(s): " + gameState.getLinesUntilNextItem() + " lines until next item");
        }

        // 남은 줄: 1줄
        assertEquals(1, gameState.getLinesUntilNextItem());

        // 10번째 줄 클리어 → 아이템 생성!
        gameState = itemManager.checkAndGenerateItem(gameState, 1);
        assertNotNull(gameState.getNextBlockItemType(), "Should generate item after 10 lines");
        System.out.println("✅ Item generated after 10 lines: " + gameState.getNextBlockItemType());

        // 카운터 리셋 확인
        assertEquals(10, gameState.getLinesUntilNextItem());

        System.out.println("========================================================\n");
    }
    
    @Test
    public void testItemGenerationWithMultipleLinesCleared() {
        System.out.println("\n========== Test: Item Generation With Multiple Lines Cleared ==========");

        // 4줄 클리어 (Tetris)
        gameState = itemManager.checkAndGenerateItem(gameState, 4);
        assertNull(gameState.getNextBlockItemType(), "Should not generate item after 4 lines");
        assertEquals(6, gameState.getLinesUntilNextItem());
        System.out.println("After clearing 4 lines: " + gameState.getLinesUntilNextItem() + " lines until next item");

        // 3줄 클리어 (Triple)
        gameState = itemManager.checkAndGenerateItem(gameState, 3);
        assertNull(gameState.getNextBlockItemType(), "Should not generate item after 7 lines total");
        assertEquals(3, gameState.getLinesUntilNextItem());
        System.out.println("After clearing 7 lines total: " + gameState.getLinesUntilNextItem() + " lines until next item");

        // 3줄 클리어 → 총 10줄, 아이템 생성!
        gameState = itemManager.checkAndGenerateItem(gameState, 3);
        assertNotNull(gameState.getNextBlockItemType(), "Should generate item after 10 lines total");
        System.out.println("✅ Item generated after 10 lines: " + gameState.getNextBlockItemType());

        // 카운터는 10 - 3 = 7 (초과분이 있으므로)
        // 아니다 - 코드를 보면 10으로 리셋됨
        assertEquals(10, gameState.getLinesUntilNextItem());

        System.out.println("==================================================================\n");
    }

    @Test
    public void testItemGenerationWithOverflow() {
        System.out.println("\n========== Test: Item Generation With Overflow ==========");

        // 9줄 클리어
        gameState = itemManager.checkAndGenerateItem(gameState, 9);
        assertEquals(1, gameState.getLinesUntilNextItem());
        System.out.println("After clearing 9 lines: " + gameState.getLinesUntilNextItem() + " lines until next item");

        // 4줄 클리어 (총 13줄) → 아이템 생성하고 카운터 리셋
        gameState = itemManager.checkAndGenerateItem(gameState, 4);
        assertNotNull(gameState.getNextBlockItemType(), "Should generate item when counter goes negative");
        System.out.println("✅ Item generated after 13 lines total: " + gameState.getNextBlockItemType());

        // 카운터는 10으로 리셋됨 (코드 확인 필요)
        assertEquals(10, gameState.getLinesUntilNextItem());

        System.out.println("=========================================================\n");
    }

    @Test
    public void testNoItemGenerationWithZeroLines() {
        System.out.println("\n========== Test: No Item Generation With Zero Lines ==========");

        // 0줄 클리어 → 아이템 생성 안 됨
        gameState = itemManager.checkAndGenerateItem(gameState, 0);
        assertNull(gameState.getNextBlockItemType(), "Should not generate item with 0 lines cleared");
        assertEquals(10, gameState.getLinesUntilNextItem());

        System.out.println("✅ No item generated with 0 lines cleared");
        System.out.println("==============================================================\n");
    }

    @Test
    public void testGeneratedItemTypeIsEnabled() {
        System.out.println("\n========== Test: Generated Item Type Is Enabled ==========");

        // 10줄 클리어하여 아이템 생성
        gameState = itemManager.checkAndGenerateItem(gameState, 10);
        ItemType result = gameState.getNextBlockItemType();
        assertNotNull(result, "Should generate item after 10 lines");

        // 생성된 아이템이 활성화된 아이템인지 확인
        assertTrue(itemManager.isItemEnabled(result), "Generated item should be enabled");
        System.out.println("✅ Generated item " + result + " is enabled");

        System.out.println("==========================================================\n");
    }
}
