package seoultech.se.core.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.engine.item.ItemEffect;
import seoultech.se.core.engine.item.impl.PlusItem;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.enumType.Color;

/**
 * Plus 아이템 중력 버그 재현 테스트
 * 
 * 문제 상황:
 * - Plus 아이템을 rotate하여 가장 밑 부분이 (17, 8)에 위치
 * - Plus가 row 17과 column 8을 제거해야 함
 * - 하지만 중력 적용 후 (17, 8)만 남아야 하는데 다른 블록들도 남아있음
 */
@DisplayName("🐛 Plus 아이템 중력 버그 재현")
class PlusItemGravityBugTest {
    
    private GameState gameState;
    private PlusItem plusItem;
    
    @BeforeEach
    void setUp() {
        gameState = new GameState(10, 20);
        plusItem = new PlusItem();
    }
    
    /**
     * 로그에서의 실제 시나리오 재현:
     * - Z-block을 rotate하여 Plus 아이템으로 (16, 8)에 배치
     * - 로그: "Clearing row 16 (2 blocks)"
     * - 로그: "Clearing column 8 (3 blocks, excluding intersection)"
     */
    @Test
    @DisplayName("🔴 Plus at (16, 8): row 16 + column 8 제거 후 중력")
    void testPlusItem_At16_8_ShouldClearRowAndColumn() {
        // Given: Z-block이 하단에 쌓여있는 상태 재현
        // Row 18, 19는 거의 가득 참 (I-block과 J-block이 놓여있음)
        
        // Row 19 (바닥) - I-block 수평 (X=2~5)
        for (int x = 2; x <= 5; x++) {
            gameState.getGrid()[19][x].setOccupied(true);
            gameState.getGrid()[19][x].setColor(Color.CYAN);
        }
        
        // Row 19 - J-block 일부 (X=0~2)
        gameState.getGrid()[19][0].setOccupied(true);
        gameState.getGrid()[19][0].setColor(Color.BLUE);
        gameState.getGrid()[19][1].setOccupied(true);
        gameState.getGrid()[19][1].setColor(Color.BLUE);
        gameState.getGrid()[19][2].setOccupied(true);
        gameState.getGrid()[19][2].setColor(Color.BLUE);
        
        // Row 18 - T-block, O-block, S-block, L-block으로 채워짐
        for (int x = 0; x < 10; x++) {
            if (x != 8) { // Column 8만 비워둠
                gameState.getGrid()[18][x].setOccupied(true);
                gameState.getGrid()[18][x].setColor(Color.ORANGE);
            }
        }
        
        // Row 17 - S-block 일부 (X=8~9에 블록)
        gameState.getGrid()[17][8].setOccupied(true);
        gameState.getGrid()[17][8].setColor(Color.GREEN);
        gameState.getGrid()[17][9].setOccupied(true);
        gameState.getGrid()[17][9].setColor(Color.GREEN);
        
        // Row 16 - Z-block with Plus item (회전된 상태)
        // Z-block rotated: 
        //     X
        //    XX
        //    X
        // Pivot at (16, 8)이면 row 16에 X=7, 8이 있고
        gameState.getGrid()[16][7].setOccupied(true);
        gameState.getGrid()[16][7].setColor(Color.RED);
        gameState.getGrid()[16][8].setOccupied(true);
        gameState.getGrid()[16][8].setColor(Color.RED);
        
        // Row 15에도 Z-block 일부
        gameState.getGrid()[15][8].setOccupied(true);
        gameState.getGrid()[15][8].setColor(Color.RED);
        
        // Row 17, column 8에도 Z-block
        gameState.getGrid()[17][7].setOccupied(true);
        gameState.getGrid()[17][7].setColor(Color.RED);
        
        System.out.println("\n=== Before Plus Item ===");
        printBoard(gameState);
        
        // When: Plus 아이템을 (16, 8)에 적용
        ItemEffect effect = plusItem.apply(gameState, 16, 8);
        
        System.out.println("\n=== After Plus Item ===");
        printBoard(gameState);
        
        // Then: Row 16과 Column 8이 제거되어야 함
        // Row 16 전체가 비어있어야 함
        for (int x = 0; x < 10; x++) {
            // Row 16은 제거되었으므로 위의 블록들이 내려와야 함
            // 실제로는 row 15의 블록이 row 16으로 이동
        }
        
        // Column 8 전체가 비어있거나 중력으로 떨어진 블록만 있어야 함
        // (16, 8) 교차점은 이미 row 제거에서 제거됨
        
        // 🔴 핵심 검증: (17, 8)은 Plus가 제거하지 못한 위치가 아님!
        // (17, 8)은 column 8에 포함되므로 제거되어야 함
        // 로그에 "Clearing column 8 (3 blocks, excluding intersection)"라고 나왔다면
        // row 16이 아닌 다른 row의 column 8 블록들이 제거된 것
        
        // 중력 적용 후 각 열의 블록이 바닥까지 떨어져야 함
        assertBlocksHaveProperGravity(gameState);
    }
    
    @Test
    @DisplayName("🔴 Plus 중력 버그: 특정 블록이 공중에 남는 문제")
    void testPlusItem_GravityBug_FloatingBlocks() {
        // Given: 간단한 시나리오
        // Row 19: 바닥에 블록 몇 개
        gameState.getGrid()[19][0].setOccupied(true);
        gameState.getGrid()[19][1].setOccupied(true);
        gameState.getGrid()[19][2].setOccupied(true);
        gameState.getGrid()[19][9].setOccupied(true);
        
        // Row 16: Plus의 중심 (row 전체 채움)
        for (int x = 0; x < 10; x++) {
            gameState.getGrid()[16][x].setOccupied(true);
            gameState.getGrid()[16][x].setColor(Color.RED);
        }
        
        // Column 8: 위에서 아래로 블록 배치
        for (int y = 10; y < 16; y++) {
            gameState.getGrid()[y][8].setOccupied(true);
            gameState.getGrid()[y][8].setColor(Color.BLUE);
        }
        
        // Row 17, 18에도 일부 블록
        gameState.getGrid()[17][8].setOccupied(true);
        gameState.getGrid()[17][7].setOccupied(true);
        gameState.getGrid()[18][8].setOccupied(true);
        gameState.getGrid()[18][5].setOccupied(true);
        
        System.out.println("\n=== Before Plus at (16, 8) ===");
        printBoard(gameState);
        
        // When: Plus at (16, 8)
        ItemEffect effect = plusItem.apply(gameState, 16, 8);
        
        System.out.println("\n=== After Plus at (16, 8) ===");
        printBoard(gameState);
        
        // Then: 검증
        // 1. Row 16은 완전히 제거되어야 함 (중력 후 다른 블록이 내려올 수 있음)
        // 2. Column 8은 완전히 제거되어야 함 (교차점 제외하지만 이미 row에서 제거됨)
        // 3. 모든 블록이 중력에 의해 아래로 떨어져야 함
        
        assertBlocksHaveProperGravity(gameState);
        
        // Column 8 위쪽에는 블록이 없어야 함 (중력으로 떨어졌으므로)
        for (int y = 0; y < 16; y++) {
            assertFalse(gameState.getGrid()[y][8].isOccupied(),
                "Column 8 at Y=" + y + " should be empty after Plus + gravity");
        }
    }
    
    /**
     * 중력이 제대로 적용되었는지 확인:
     * 모든 블록 아래에 빈 공간이 없어야 함
     */
    private void assertBlocksHaveProperGravity(GameState gameState) {
        Cell[][] grid = gameState.getGrid();
        int height = gameState.getBoardHeight();
        int width = gameState.getBoardWidth();
        
        for (int col = 0; col < width; col++) {
            boolean foundEmpty = false;
            for (int row = height - 1; row >= 0; row--) {
                if (!grid[row][col].isOccupied()) {
                    foundEmpty = true;
                } else if (foundEmpty) {
                    // 빈 공간 위에 블록이 있으면 중력이 제대로 적용되지 않은 것
                    fail(String.format("Block at (%d, %d) is floating! Empty space exists below at column %d", 
                        row, col, col));
                }
            }
        }
    }
    
    private void printBoard(GameState gameState) {
        Cell[][] grid = gameState.getGrid();
        int height = gameState.getBoardHeight();
        int width = gameState.getBoardWidth();
        
        System.out.println("   0 1 2 3 4 5 6 7 8 9");
        for (int y = 0; y < height; y++) {
            System.out.printf("%2d ", y);
            for (int x = 0; x < width; x++) {
                System.out.print(grid[y][x].isOccupied() ? "█ " : "· ");
            }
            System.out.println();
        }
    }
}
