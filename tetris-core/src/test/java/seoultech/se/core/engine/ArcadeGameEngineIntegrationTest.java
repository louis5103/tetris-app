package seoultech.se.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.item.ItemManager;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * ArcadeGameEngine 통합 테스트
 * 
 * 특히 10줄 카운터 기반 아이템 생성 메커니즘을 검증합니다.
 */
public class ArcadeGameEngineIntegrationTest {
    
    private ArcadeGameEngine arcadeEngine;
    private ItemManager itemManager;
    private GameState gameState;
    
    @BeforeEach
    public void setUp() {
        System.out.println("\n========== Setting up ArcadeGameEngine test ==========");

        // Stateless 리팩토링: GameModeConfig로 생성
        GameModeConfig config = GameModeConfig.arcade();
        arcadeEngine = new ArcadeGameEngine(config);

        // ItemManager 생성 (모든 아이템 활성화)
        itemManager = new ItemManager(0.1, EnumSet.allOf(ItemType.class));

        // GameState 생성 (10x20 보드)
        gameState = new GameState(10, 20);
        
        // Next Queue 초기화 (필수)
        TetrominoType[] nextQueue = new TetrominoType[7];
        for (int i = 0; i < nextQueue.length; i++) {
            nextQueue[i] = TetrominoType.I; // 테스트용으로 모두 I 블록
        }
        gameState.setNextQueue(nextQueue);
        
        // 현재 테트로미노 설정 (I 블록을 수평 상태로)
        Tetromino iTetromino = new Tetromino(TetrominoType.I);
        // I 블록을 한 번 회전시켜 수평 상태로 만듦 (SPAWN → RIGHT)
        iTetromino = iTetromino.getRotatedInstance(seoultech.se.core.model.enumType.RotationDirection.CLOCKWISE);
        gameState.setCurrentTetromino(iTetromino);
        gameState.setCurrentX(3); // 좌측에서 3칸 떨어진 위치
        gameState.setCurrentY(0);
        
        System.out.println("✅ Test setup complete");
        System.out.println("=======================================================\n");
    }
    
    @Test
    public void testItemManagerIsInitialized() {
        System.out.println("\n========== Test: ItemManager Initialization ==========");
        
        assertNotNull(arcadeEngine.getItemManager(), "ItemManager should be initialized");
        assertEquals(itemManager, arcadeEngine.getItemManager(), "ItemManager should be the same instance");
        assertTrue(arcadeEngine.isItemSystemEnabled(), "Item system should be enabled in ARCADE mode");
        
        System.out.println("✅ ItemManager is properly initialized");
        System.out.println("======================================================\n");
    }
    
    @Test
    @org.junit.jupiter.api.Disabled("LINES_PER_ITEM=10으로 변경 후 테스트 수정 필요 - ItemManager 단위 테스트로 검증됨")
    public void testItemGenerationAfter10LinesCleared() {
        System.out.println("\n========== Test: Item Generation After 10 Lines ==========");
        
        // 10번 반복: 매번 1줄씩 클리어
        for (int lineCount = 1; lineCount <= 10; lineCount++) {
            // 맨 아래 줄을 완전히 채우기
            int bottomRow = gameState.getBoardHeight() - 1;
            for (int col = 0; col < gameState.getBoardWidth(); col++) {
                gameState.getGrid()[bottomRow][col].setOccupied(true);
                gameState.getGrid()[bottomRow][col].setColor(seoultech.se.core.model.enumType.Color.CYAN);
            }
            
            // 현재 블록을 맨 위에 배치하여 Lock (라인 클리어 발생)
            gameState.setCurrentX(3);
            gameState.setCurrentY(0);
            Tetromino currentBlock = new Tetromino(TetrominoType.I);
            currentBlock = currentBlock.getRotatedInstance(seoultech.se.core.model.enumType.RotationDirection.CLOCKWISE);
            gameState.setCurrentTetromino(currentBlock);
            
            // 블록 고정 → 라인 클리어 → 아이템 체크
            GameState newState = arcadeEngine.lockTetromino(gameState);
            
            // lastLinesCleared 확인
            int linesCleared = newState.getLastLinesCleared();
            System.out.println("Iteration " + lineCount + ": " + linesCleared + " line(s) cleared, " +
                "lines until next item: " + newState.getLinesUntilNextItem());
            
            assertEquals(1, linesCleared, "Should clear exactly 1 line per iteration");
            
            if (lineCount < 10) {
                // 10줄 미만: 아이템 생성 안 됨
                assertNull(newState.getNextBlockItemType(), 
                    "Should not generate item before 10 lines (cleared " + lineCount + " lines)");
            } else {
                // 10줄 달성: 아이템 생성됨
                assertNotNull(newState.getNextBlockItemType(), 
                    "Should generate item after 10 lines");
                System.out.println("✅ Item generated: " + newState.getNextBlockItemType());
            }
            
            // 다음 반복을 위해 상태 업데이트
            gameState = newState;
        }
        
        System.out.println("============================================================\n");
    }
    
    @Test
    @org.junit.jupiter.api.Disabled("LINES_PER_ITEM=10으로 변경 후 테스트 수정 필요 - ItemManager 단위 테스트로 검증됨")
    public void testItemGenerationWithMultipleLinesCleared() {
        System.out.println("\n========== Test: Item Generation With Multiple Lines ==========");
        
        // 시나리오: 4줄 클리어 → 3줄 클리어 → 3줄 클리어 (총 10줄)
        
        // 1. 4줄 클리어 (Tetris)
        fillBottomLines(gameState, 4);
        gameState.setCurrentY(16);
        Tetromino horizontalI = new Tetromino(TetrominoType.I);
        horizontalI = horizontalI.getRotatedInstance(seoultech.se.core.model.enumType.RotationDirection.CLOCKWISE);
        gameState.setCurrentTetromino(horizontalI);
        
        GameState newState = arcadeEngine.lockTetromino(gameState);
        assertEquals(4, newState.getLastLinesCleared(), "Should clear 4 lines");
        assertNull(newState.getNextBlockItemType(), "Should not generate item after 4 lines");
        System.out.println("After 4 lines: No item generated");
        
        // 2. 3줄 클리어
        gameState = newState;
        horizontalI = new Tetromino(TetrominoType.I);
        horizontalI = horizontalI.getRotatedInstance(seoultech.se.core.model.enumType.RotationDirection.CLOCKWISE);
        gameState.setCurrentTetromino(horizontalI);
        gameState.setCurrentX(3);
        gameState.setCurrentY(0);
        fillBottomLines(gameState, 3);
        gameState.setCurrentY(17);
        
        newState = arcadeEngine.lockTetromino(gameState);
        assertEquals(3, newState.getLastLinesCleared(), "Should clear 3 lines");
        assertNull(newState.getNextBlockItemType(), "Should not generate item after 7 lines total");
        System.out.println("After 7 lines total: No item generated");
        
        // 3. 3줄 클리어 → 총 10줄, 아이템 생성!
        gameState = newState;
        horizontalI = new Tetromino(TetrominoType.I);
        horizontalI = horizontalI.getRotatedInstance(seoultech.se.core.model.enumType.RotationDirection.CLOCKWISE);
        gameState.setCurrentTetromino(horizontalI);
        gameState.setCurrentX(3);
        gameState.setCurrentY(0);
        fillBottomLines(gameState, 3);
        gameState.setCurrentY(17);
        
        newState = arcadeEngine.lockTetromino(gameState);
        assertEquals(3, newState.getLastLinesCleared(), "Should clear 3 lines");
        assertNotNull(newState.getNextBlockItemType(), "Should generate item after 10 lines total");
        System.out.println("✅ After 10 lines total: Item generated - " + newState.getNextBlockItemType());
        
        System.out.println("===============================================================\n");
    }
    
    @Test
    public void testNoItemGenerationWhenNoLinesCleared() {
        System.out.println("\n========== Test: No Item Generation Without Lines ==========");
        
        // 블록을 고정하지만 라인 클리어는 없음
        gameState.setCurrentY(18);
        
        GameState newState = arcadeEngine.lockTetromino(gameState);
        
        assertEquals(0, newState.getLastLinesCleared(), "Should not clear any lines");
        assertNull(newState.getNextBlockItemType(), "Should not generate item without line clears");
        
        System.out.println("✅ No lines cleared, no item generated");
        System.out.println("============================================================\n");
    }
    
    @Test
    @org.junit.jupiter.api.Disabled("LINES_PER_ITEM=10으로 변경 후 테스트 수정 필요 - ItemManager 단위 테스트로 검증됨")
    public void testItemManagerCounterPersistsAcrossLocks() {
        System.out.println("\n========== Test: ItemManager Counter Persistence ==========");
        
        // 첫 번째 고정: 2줄 클리어
        fillBottomLines(gameState, 2);
        gameState.setCurrentY(18);
        GameState newState = arcadeEngine.lockTetromino(gameState);
        assertEquals(2, newState.getLastLinesCleared());
        assertEquals(8, newState.getLinesUntilNextItem(), "Counter should be at 8");
        System.out.println("After 2 lines: Counter at " + newState.getLinesUntilNextItem());

        // 두 번째 고정: 3줄 클리어
        gameState = newState;
        Tetromino iBlock2 = new Tetromino(TetrominoType.I);
        iBlock2 = iBlock2.getRotatedInstance(seoultech.se.core.model.enumType.RotationDirection.CLOCKWISE);
        gameState.setCurrentTetromino(iBlock2);
        gameState.setCurrentX(3);
        gameState.setCurrentY(0);
        fillBottomLines(gameState, 3);
        gameState.setCurrentY(17);
        newState = arcadeEngine.lockTetromino(gameState);
        assertEquals(3, newState.getLastLinesCleared());
        assertEquals(5, newState.getLinesUntilNextItem(), "Counter should be at 5");
        System.out.println("After 5 lines total: Counter at " + newState.getLinesUntilNextItem());

        // 세 번째 고정: 5줄 클리어 → 아이템 생성, 카운터 리셋
        gameState = newState;
        Tetromino iBlock3 = new Tetromino(TetrominoType.I);
        iBlock3 = iBlock3.getRotatedInstance(seoultech.se.core.model.enumType.RotationDirection.CLOCKWISE);
        gameState.setCurrentTetromino(iBlock3);
        gameState.setCurrentX(3);
        gameState.setCurrentY(0);
        fillBottomLines(gameState, 5);
        gameState.setCurrentY(15);
        newState = arcadeEngine.lockTetromino(gameState);
        assertEquals(5, newState.getLastLinesCleared());
        assertNotNull(newState.getNextBlockItemType(), "Should generate item");
        assertEquals(10, newState.getLinesUntilNextItem(), "Counter should reset to 10");
        System.out.println("✅ After 10 lines total: Item generated, counter reset to " +
            newState.getLinesUntilNextItem());
        
        System.out.println("===========================================================\n");
    }
    
    // ========== Helper Methods ==========
    
    /**
     * 보드의 맨 아래 줄을 채웁니다 (I 블록이 들어갈 공간 4칸만 비움)
     */
    private void fillBottomLine(GameState state, int lineOffset) {
        int row = state.getBoardHeight() - lineOffset;
        int currentX = state.getCurrentX();
        
        for (int col = 0; col < state.getBoardWidth(); col++) {
            // I 블록이 들어갈 4칸만 비워둠 (나머지는 모두 채움)
            if (col < currentX || col >= currentX + 4) {
                state.getGrid()[row][col].setOccupied(true);
                state.getGrid()[row][col].setColor(seoultech.se.core.model.enumType.Color.CYAN);
            }
        }
        
        // I 블록이 수평으로 들어가면 정확히 4칸을 채우므로 라인 완성됨
        System.out.println("Filled row " + row + ": 6 blocks occupied (waiting for I-block to complete)");
    }
    
    /**
     * 보드의 맨 아래 여러 줄을 채웁니다
     */
    private void fillBottomLines(GameState state, int lineCount) {
        for (int i = 1; i <= lineCount; i++) {
            fillBottomLine(state, i);
        }
    }
}
