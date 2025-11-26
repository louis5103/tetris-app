package seoultech.se.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.EnumSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.item.ItemManager;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.Color;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * 실제 게임 플레이를 시뮬레이션하는 통합 테스트
 */
public class RealGamePlaySimulationTest {
    
    private ArcadeGameEngine arcadeEngine;
    private ItemManager itemManager;
    private GameState gameState;
    
    @BeforeEach
    public void setUp() {
        System.out.println("\n========== Real Game Play Simulation Setup ==========");

        // Stateless 리팩토링: GameModeConfig로 생성
        GameModeConfig config = GameModeConfig.arcade();
        arcadeEngine = new ArcadeGameEngine(config);
        itemManager = new ItemManager(0.1, EnumSet.allOf(ItemType.class));

        gameState = new GameState(10, 20);
        
        TetrominoType[] nextQueue = new TetrominoType[7];
        for (int i = 0; i < nextQueue.length; i++) {
            nextQueue[i] = TetrominoType.O; // O 블록 (2x2) 사용
        }
        gameState.setNextQueue(nextQueue);
        
        gameState.setCurrentTetromino(new Tetromino(TetrominoType.O));
        gameState.setCurrentX(4); // 중앙
        gameState.setCurrentY(0);
        
        System.out.println("✅ Setup complete - Board: 10x20, Using O blocks");
        System.out.println("=======================================================\n");
    }
    
    @Test
    public void testRealGameScenario_10LinesCleared() {
        System.out.println("\n========== Real Game Scenario: 10 Lines Cleared ==========");
        
        int linesCleared = 0;
        int blockCount = 0;
        
        // 10줄을 채우기 위해서는 5개의 O 블록이 각 줄마다 필요 (10칸 / 2칸 = 5개)
        // 총 50개의 O 블록 필요
        
        while (linesCleared < 10) {
            blockCount++;
            
            // O 블록을 맨 아래에 배치
            gameState.setCurrentY(18); // 맨 아래
            
            // X 위치를 변경하면서 줄을 채움
            int xPos = ((blockCount - 1) % 5) * 2; // 0, 2, 4, 6, 8 순서로
            gameState.setCurrentX(xPos);
            
            System.out.println("Block " + blockCount + ": Placing O block at (" + xPos + ", 18)");
            
            // 블록 고정 전 보드 상태 출력
            printBottomRows(gameState, 2);
            
            // 블록 고정
            GameState newState = arcadeEngine.lockTetromino(gameState);
            
            // 이번 turn에 클리어된 줄 수
            int thisRoundCleared = newState.getLastLinesCleared();
            linesCleared += thisRoundCleared;

            System.out.println("   → Lines cleared this round: " + thisRoundCleared);
            System.out.println("   → Total lines cleared: " + linesCleared);
            System.out.println("   → Lines until next item: " + newState.getLinesUntilNextItem());
            
            if (thisRoundCleared > 0) {
                System.out.println("   ✅ Line(s) cleared!");
            }
            
            // 블록 고정 후 보드 상태 출력
            printBottomRows(newState, 2);
            System.out.println();
            
            // 아이템 생성 체크
            if (newState.getNextBlockItemType() != null) {
                System.out.println("   🎁 ITEM GENERATED: " + newState.getNextBlockItemType());
                assertNotNull(newState.getNextBlockItemType(), "Item should be generated after 10 lines");
                break;
            }
            
            // 다음 블록 준비
            gameState = newState;
            gameState.setCurrentTetromino(new Tetromino(TetrominoType.O));
            gameState.setCurrentY(0);
            
            // 무한 루프 방지
            if (blockCount > 60) {
                System.out.println("❌ Test failed: Too many blocks placed without clearing 10 lines");
                fail("Could not clear 10 lines after " + blockCount + " blocks");
                break;
            }
        }
        
        System.out.println("=============================================================");
        System.out.println("Test completed:");
        System.out.println("  - Blocks placed: " + blockCount);
        System.out.println("  - Lines cleared: " + linesCleared);
        System.out.println("  - Item generated: " + (gameState.getNextBlockItemType() != null));
        System.out.println("=============================================================\n");
        
        assertTrue(linesCleared >= 10, "Should have cleared at least 10 lines");
    }
    
    @Test
    public void testManualLineFill() {
        System.out.println("\n========== Manual Line Fill Test ==========");
        
        // 수동으로 맨 아래 줄을 완전히 채우기
        int bottomRow = gameState.getBoardHeight() - 1;
        
        System.out.println("Filling bottom row (" + bottomRow + ") manually...");
        for (int col = 0; col < gameState.getBoardWidth(); col++) {
            gameState.getGrid()[bottomRow][col].setOccupied(true);
            gameState.getGrid()[bottomRow][col].setColor(Color.CYAN);
        }
        
        printBottomRows(gameState, 3);
        
        // 블록을 맨 위에 배치하고 고정
        gameState.setCurrentX(4);
        gameState.setCurrentY(0);
        
        System.out.println("Locking tetromino...");
        GameState newState = arcadeEngine.lockTetromino(gameState);
        
        System.out.println("Lines cleared: " + newState.getLastLinesCleared());
        
        printBottomRows(newState, 3);
        
        assertEquals(1, newState.getLastLinesCleared(), "Should clear exactly 1 line");
        
        System.out.println("================================================\n");
    }
    
    // Helper method to print bottom rows of the board
    private void printBottomRows(GameState state, int rowCount) {
        System.out.println("   Board (bottom " + rowCount + " rows):");
        for (int row = state.getBoardHeight() - rowCount; row < state.getBoardHeight(); row++) {
            System.out.print("   Row " + row + ": [");
            for (int col = 0; col < state.getBoardWidth(); col++) {
                System.out.print(state.getGrid()[row][col].isOccupied() ? "█" : "·");
            }
            System.out.println("]");
        }
    }
}
