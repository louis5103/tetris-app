package seoultech.se.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.ArcadeGameEngine;
import seoultech.se.core.engine.item.ItemEffect;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.engine.item.impl.BombItem;
import seoultech.se.core.engine.item.impl.LineClearItem;
import seoultech.se.core.engine.item.impl.PlusItem;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.Color;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * 경계값 테스트 (Boundary Value Testing)
 * 
 * 모든 경계선 상황에서 아이템이 정상 작동하는지 검증:
 * - 좌측 경계 (X=0)
 * - 우측 경계 (X=9, boardWidth-1)
 * - 하단 경계 (Y=19, boardHeight-1)
 * - 상단 경계 (Y=0)
 * - 모서리 (코너) 위치
 */
@DisplayName("아이템 경계값 테스트")
public class BoundaryValueItemTest {
    
    private GameState gameState;
    private ArcadeGameEngine engine;
    private BombItem bombItem;
    private PlusItem plusItem;
    private LineClearItem lineClearItem;
    
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    
    @BeforeEach
    void setUp() {
        gameState = new GameState(BOARD_WIDTH, BOARD_HEIGHT);
        
        GameModeConfig config = GameModeConfig.builder()
            .linesPerItem(1)
            .itemAutoUse(true)
            .enabledItemTypes(new HashSet<>(Arrays.asList(
                ItemType.BOMB, ItemType.PLUS, ItemType.LINE_CLEAR
            )))
            .build();
        
        engine = new ArcadeGameEngine(config);
        bombItem = new BombItem();
        plusItem = new PlusItem();
        lineClearItem = new LineClearItem();
    }
    
    // ==================== BOMB 아이템 경계값 테스트 ====================
    
    @Test
    @DisplayName("BOMB - 좌측 하단 모서리 (0, 19)")
    void testBombAtBottomLeftCorner() {
        fillBottomRows(3); // 하단 3줄 채우기
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = bombItem.apply(gameState, 19, 0);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "BOMB should succeed at bottom-left corner");
        assertTrue(afterCount < beforeCount, "Blocks should be cleared");
        assertNoInvalidCells(); // 유효하지 않은 셀이 없어야 함
    }
    
    @Test
    @DisplayName("BOMB - 우측 하단 모서리 (9, 19)")
    void testBombAtBottomRightCorner() {
        fillBottomRows(3);
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = bombItem.apply(gameState, 19, 9);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "BOMB should succeed at bottom-right corner");
        assertTrue(afterCount < beforeCount, "Blocks should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("BOMB - 좌측 상단 모서리 (0, 0)")
    void testBombAtTopLeftCorner() {
        fillTopRows(3); // 상단 3줄 채우기
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = bombItem.apply(gameState, 0, 0);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "BOMB should succeed at top-left corner");
        assertTrue(afterCount < beforeCount, "Blocks should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("BOMB - 우측 상단 모서리 (9, 0)")
    void testBombAtTopRightCorner() {
        fillTopRows(3);
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = bombItem.apply(gameState, 0, 9);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "BOMB should succeed at top-right corner");
        assertTrue(afterCount < beforeCount, "Blocks should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("BOMB - 좌측 경계 중간 (0, 10)")
    void testBombAtLeftEdge() {
        fillMiddleArea();
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = bombItem.apply(gameState, 10, 0);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "BOMB should succeed at left edge");
        assertTrue(afterCount < beforeCount, "Blocks should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("BOMB - 우측 경계 중간 (9, 10)")
    void testBombAtRightEdge() {
        fillMiddleArea();
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = bombItem.apply(gameState, 10, 9);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "BOMB should succeed at right edge");
        assertTrue(afterCount < beforeCount, "Blocks should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("BOMB - 보드 밖 좌표 (-1, 5)")
    void testBombOutsideLeft() {
        fillMiddleArea();
        
        ItemEffect effect = bombItem.apply(gameState, 5, -1);
        
        assertFalse(effect.isSuccess(), "BOMB should fail outside board");
        assertEquals(0, effect.getBlocksCleared(), "No blocks should be cleared");
    }
    
    @Test
    @DisplayName("BOMB - 보드 밖 좌표 (10, 5)")
    void testBombOutsideRight() {
        fillMiddleArea();
        
        ItemEffect effect = bombItem.apply(gameState, 5, 10);
        
        assertFalse(effect.isSuccess(), "BOMB should fail outside board");
        assertEquals(0, effect.getBlocksCleared(), "No blocks should be cleared");
    }
    
    @Test
    @DisplayName("BOMB - 보드 밖 좌표 (5, -1)")
    void testBombOutsideTop() {
        fillTopRows(3);
        
        ItemEffect effect = bombItem.apply(gameState, -1, 5);
        
        assertFalse(effect.isSuccess(), "BOMB should fail outside board");
        assertEquals(0, effect.getBlocksCleared(), "No blocks should be cleared");
    }
    
    @Test
    @DisplayName("BOMB - 보드 밖 좌표 (5, 20)")
    void testBombOutsideBottom() {
        fillBottomRows(3);
        
        ItemEffect effect = bombItem.apply(gameState, 20, 5);
        
        assertFalse(effect.isSuccess(), "BOMB should fail outside board");
        assertEquals(0, effect.getBlocksCleared(), "No blocks should be cleared");
    }
    
    // ==================== PLUS 아이템 경계값 테스트 ====================
    
    @Test
    @DisplayName("PLUS - 좌측 하단 모서리 (0, 19)")
    void testPlusAtBottomLeftCorner() {
        fillBottomRows(5);
        fillLeftColumn(5);
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = plusItem.apply(gameState, 19, 0);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "PLUS should succeed at bottom-left corner");
        assertTrue(afterCount < beforeCount, "Blocks should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("PLUS - 우측 하단 모서리 (9, 19)")
    void testPlusAtBottomRightCorner() {
        fillBottomRows(5);
        fillRightColumn(5);
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = plusItem.apply(gameState, 19, 9);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "PLUS should succeed at bottom-right corner");
        assertTrue(afterCount < beforeCount, "Blocks should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("PLUS - 좌측 경계 (0, 10)")
    void testPlusAtLeftEdge() {
        fillMiddleArea();
        fillLeftColumn(10);
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = plusItem.apply(gameState, 10, 0);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "PLUS should succeed at left edge");
        assertTrue(afterCount < beforeCount, "Blocks should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("PLUS - 우측 경계 (9, 10)")
    void testPlusAtRightEdge() {
        fillMiddleArea();
        fillRightColumn(10);
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = plusItem.apply(gameState, 10, 9);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "PLUS should succeed at right edge");
        assertTrue(afterCount < beforeCount, "Blocks should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("PLUS - 상단 경계 (5, 0)")
    void testPlusAtTopEdge() {
        fillTopRows(5);
        fillColumn(5, 5);
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = plusItem.apply(gameState, 0, 5);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "PLUS should succeed at top edge");
        assertTrue(afterCount < beforeCount, "Blocks should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("PLUS - 하단 경계 (5, 19)")
    void testPlusAtBottomEdge() {
        fillBottomRows(5);
        fillColumn(5, 10);
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = plusItem.apply(gameState, 19, 5);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "PLUS should succeed at bottom edge");
        assertTrue(afterCount < beforeCount, "Blocks should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("PLUS - 보드 밖 좌표")
    void testPlusOutsideBoard() {
        fillMiddleArea();
        
        ItemEffect effect1 = plusItem.apply(gameState, 10, -1);
        assertFalse(effect1.isSuccess(), "PLUS should fail outside board (left)");
        
        ItemEffect effect2 = plusItem.apply(gameState, 10, 10);
        assertFalse(effect2.isSuccess(), "PLUS should fail outside board (right)");
        
        ItemEffect effect3 = plusItem.apply(gameState, -1, 5);
        assertFalse(effect3.isSuccess(), "PLUS should fail outside board (top)");
        
        ItemEffect effect4 = plusItem.apply(gameState, 20, 5);
        assertFalse(effect4.isSuccess(), "PLUS should fail outside board (bottom)");
    }
    
    // ==================== LINE_CLEAR 아이템 경계값 테스트 ====================
    
    @Test
    @DisplayName("LINE_CLEAR - 최상단 줄 (row 0)")
    void testLineClearAtTopRow() {
        // 최상단 줄에 'L' 마커 추가
        for (int col = 0; col < BOARD_WIDTH; col++) {
            gameState.getGrid()[0][col].setOccupied(true);
            gameState.getGrid()[0][col].setColor(Color.CYAN);
        }
        gameState.getGrid()[0][5].setItemMarker(ItemType.LINE_CLEAR);
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = lineClearItem.apply(gameState, 0, 0);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "LINE_CLEAR should succeed at top row");
        assertEquals(beforeCount - BOARD_WIDTH, afterCount, "Entire row should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("LINE_CLEAR - 최하단 줄 (row 19)")
    void testLineClearAtBottomRow() {
        // 최하단 줄에 'L' 마커 추가
        for (int col = 0; col < BOARD_WIDTH; col++) {
            gameState.getGrid()[19][col].setOccupied(true);
            gameState.getGrid()[19][col].setColor(Color.CYAN);
        }
        gameState.getGrid()[19][5].setItemMarker(ItemType.LINE_CLEAR);
        
        int beforeCount = countOccupiedCells();
        ItemEffect effect = lineClearItem.apply(gameState, 19, 0);
        int afterCount = countOccupiedCells();
        
        assertTrue(effect.isSuccess(), "LINE_CLEAR should succeed at bottom row");
        assertEquals(beforeCount - BOARD_WIDTH, afterCount, "Entire row should be cleared");
        assertNoInvalidCells();
    }
    
    @Test
    @DisplayName("LINE_CLEAR - 여러 줄에 마커 (0, 10, 19)")
    void testLineClearMultipleRowsAtBoundaries() {
        // 상단, 중간, 하단에 'L' 마커 추가
        for (int col = 0; col < BOARD_WIDTH; col++) {
            gameState.getGrid()[0][col].setOccupied(true);
            gameState.getGrid()[0][col].setColor(Color.CYAN);
            gameState.getGrid()[10][col].setOccupied(true);
            gameState.getGrid()[10][col].setColor(Color.GREEN);
            gameState.getGrid()[19][col].setOccupied(true);
            gameState.getGrid()[19][col].setColor(Color.RED);
        }
        gameState.getGrid()[0][0].setItemMarker(ItemType.LINE_CLEAR);
        gameState.getGrid()[10][5].setItemMarker(ItemType.LINE_CLEAR);
        gameState.getGrid()[19][9].setItemMarker(ItemType.LINE_CLEAR);
        
        ItemEffect effect = lineClearItem.apply(gameState, 0, 0);
        
        assertTrue(effect.isSuccess(), "LINE_CLEAR should succeed with multiple markers");
        assertEquals(3 * BOARD_WIDTH, effect.getBlocksCleared(), "All 3 rows should be cleared");
        assertNoInvalidCells();
    }
    
    // ==================== 통합 경계값 테스트 (ArcadeGameEngine) ====================
    
    @Test
    @DisplayName("통합 테스트 - 좌측 하단 블록 고정 + BOMB 자동 발동")
    void testIntegratedLockAtBottomLeft() {
        fillBottomRows(5);
        
        // 좌측 하단에 블록 생성
        Tetromino tetromino = new Tetromino(TetrominoType.O);
        gameState.setCurrentTetromino(tetromino);
        gameState.setCurrentX(0); // 좌측 경계
        gameState.setCurrentY(17); // 하단 근처
        gameState.setCurrentItemType(ItemType.BOMB);
        
        int beforeCount = countOccupiedCells();
        GameState newState = engine.lockTetromino(gameState);
        int afterCount = countOccupiedCellsInState(newState);
        
        assertNotNull(newState, "New state should not be null");
        assertFalse(newState.isGameOver(), "Game should not be over");
        assertTrue(afterCount < beforeCount + 4, "BOMB effect should clear blocks");
        assertNoInvalidCellsInState(newState);
    }
    
    @Test
    @DisplayName("통합 테스트 - 우측 상단 블록 고정 + PLUS 자동 발동")
    void testIntegratedLockAtTopRight() {
        fillTopRows(5);
        fillRightColumn(10);
        
        // 우측 근처 안전한 위치에 블록 생성 (게임 오버 방지)
        Tetromino tetromino = new Tetromino(TetrominoType.O);
        gameState.setCurrentTetromino(tetromino);
        gameState.setCurrentX(7); // 우측 근처
        gameState.setCurrentY(8); // 상단보다 아래
        gameState.setCurrentItemType(ItemType.PLUS);
        
        int beforeCount = countOccupiedCells();
        GameState newState = engine.lockTetromino(gameState);
        int afterCount = countOccupiedCellsInState(newState);
        
        assertNotNull(newState, "New state should not be null");
        assertFalse(newState.isGameOver(), "Game should not be over");
        assertTrue(afterCount < beforeCount + 4, "PLUS effect should clear blocks");
        assertNoInvalidCellsInState(newState);
    }
    
    @Test
    @DisplayName("통합 테스트 - 중앙 블록 고정 + 모든 아이템 타입")
    void testIntegratedLockAtCenter() {
        fillMiddleArea();
        
        for (ItemType itemType : Arrays.asList(ItemType.BOMB, ItemType.PLUS)) {
            GameState testState = new GameState(BOARD_WIDTH, BOARD_HEIGHT);
            fillMiddleAreaInState(testState);
            
            Tetromino tetromino = new Tetromino(TetrominoType.T);
            testState.setCurrentTetromino(tetromino);
            testState.setCurrentX(4); // 중앙
            testState.setCurrentY(9); // 중앙
            testState.setCurrentItemType(itemType);
            
            int beforeCount = countOccupiedCellsInState(testState);
            GameState newState = engine.lockTetromino(testState);
            int afterCount = countOccupiedCellsInState(newState);
            
            assertNotNull(newState, "New state should not be null for " + itemType);
            assertTrue(afterCount < beforeCount + 4, 
                itemType + " effect should clear blocks at center");
            assertNoInvalidCellsInState(newState);
        }
    }
    
    @Test
    @DisplayName("스트레스 테스트 - 모든 모서리에서 순차적 아이템 발동")
    void testStressAllCornersSequential() {
        int[][] corners = {
            {0, 0},    // 좌상
            {0, 9},    // 우상
            {19, 0},   // 좌하
            {19, 9}    // 우하
        };
        
        for (int[] corner : corners) {
            int row = corner[0];
            int col = corner[1];
            
            // 각 모서리에서 BOMB 테스트
            GameState testState = new GameState(BOARD_WIDTH, BOARD_HEIGHT);
            fillAreaAround(testState, row, col, 3);
            
            Tetromino tetromino = new Tetromino(TetrominoType.O);
            testState.setCurrentTetromino(tetromino);
            testState.setCurrentX(col);
            testState.setCurrentY(Math.max(0, row - 1));
            testState.setCurrentItemType(ItemType.BOMB);
            
            GameState newState = engine.lockTetromino(testState);
            
            assertNotNull(newState, 
                String.format("State should not be null at corner (%d, %d)", row, col));
            assertNoInvalidCellsInState(newState);
        }
    }
    
    // ==================== 유틸리티 메서드 ====================
    
    private void fillBottomRows(int rowCount) {
        for (int row = BOARD_HEIGHT - rowCount; row < BOARD_HEIGHT; row++) {
            for (int col = 0; col < BOARD_WIDTH; col++) {
                gameState.getGrid()[row][col].setOccupied(true);
                gameState.getGrid()[row][col].setColor(Color.GRAY);
            }
        }
    }
    
    private void fillTopRows(int rowCount) {
        for (int row = 0; row < rowCount; row++) {
            for (int col = 0; col < BOARD_WIDTH; col++) {
                gameState.getGrid()[row][col].setOccupied(true);
                gameState.getGrid()[row][col].setColor(Color.GRAY);
            }
        }
    }
    
    private void fillLeftColumn(int rowCount) {
        for (int row = BOARD_HEIGHT - rowCount; row < BOARD_HEIGHT; row++) {
            gameState.getGrid()[row][0].setOccupied(true);
            gameState.getGrid()[row][0].setColor(Color.BLUE);
        }
    }
    
    private void fillRightColumn(int rowCount) {
        for (int row = BOARD_HEIGHT - rowCount; row < BOARD_HEIGHT; row++) {
            gameState.getGrid()[row][BOARD_WIDTH - 1].setOccupied(true);
            gameState.getGrid()[row][BOARD_WIDTH - 1].setColor(Color.BLUE);
        }
    }
    
    private void fillColumn(int col, int rowCount) {
        for (int row = BOARD_HEIGHT - rowCount; row < BOARD_HEIGHT; row++) {
            gameState.getGrid()[row][col].setOccupied(true);
            gameState.getGrid()[row][col].setColor(Color.BLUE);
        }
    }
    
    private void fillMiddleArea() {
        for (int row = 8; row < 15; row++) {
            for (int col = 2; col < 8; col++) {
                gameState.getGrid()[row][col].setOccupied(true);
                gameState.getGrid()[row][col].setColor(Color.ORANGE);
            }
        }
    }
    
    private void fillMiddleAreaInState(GameState state) {
        for (int row = 8; row < 15; row++) {
            for (int col = 2; col < 8; col++) {
                state.getGrid()[row][col].setOccupied(true);
                state.getGrid()[row][col].setColor(Color.ORANGE);
            }
        }
    }
    
    private void fillAreaAround(GameState state, int centerRow, int centerCol, int radius) {
        int startRow = Math.max(0, centerRow - radius);
        int endRow = Math.min(BOARD_HEIGHT - 1, centerRow + radius);
        int startCol = Math.max(0, centerCol - radius);
        int endCol = Math.min(BOARD_WIDTH - 1, centerCol + radius);
        
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                state.getGrid()[row][col].setOccupied(true);
                state.getGrid()[row][col].setColor(Color.MAGENTA);
            }
        }
    }
    
    private int countOccupiedCells() {
        return countOccupiedCellsInState(gameState);
    }
    
    private int countOccupiedCellsInState(GameState state) {
        int count = 0;
        for (int row = 0; row < BOARD_HEIGHT; row++) {
            for (int col = 0; col < BOARD_WIDTH; col++) {
                if (state.getGrid()[row][col].isOccupied()) {
                    count++;
                }
            }
        }
        return count;
    }
    
    private void assertNoInvalidCells() {
        assertNoInvalidCellsInState(gameState);
    }
    
    private void assertNoInvalidCellsInState(GameState state) {
        Cell[][] grid = state.getGrid();
        for (int row = 0; row < BOARD_HEIGHT; row++) {
            for (int col = 0; col < BOARD_WIDTH; col++) {
                assertNotNull(grid[row][col], 
                    String.format("Cell at (%d, %d) should not be null", row, col));
                
                // 셀이 occupied면 color가 있어야 함
                if (grid[row][col].isOccupied()) {
                    assertNotNull(grid[row][col].getColor(), 
                        String.format("Occupied cell at (%d, %d) should have color", row, col));
                }
            }
        }
    }
}
