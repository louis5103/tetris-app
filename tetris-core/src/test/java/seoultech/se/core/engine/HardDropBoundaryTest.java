package seoultech.se.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * HardDrop 경계값 테스트
 * 
 * 이 테스트는 기존 테스트에서 누락된 중요한 경계 케이스를 검증합니다:
 * 1. 블록이 이미 Y=18까지 쌓여있을 때 hardDrop
 * 2. I 블록이 Y=18에 정확히 착지하는 경우
 * 3. 무게추가 재낙하할 때 Y=19를 초과하지 않는지
 */
@DisplayName("HardDrop 경계값 테스트 (버그 수정 검증)")
class HardDropBoundaryTest {
    
    private ArcadeGameEngine arcadeEngine;
    private ClassicGameEngine classicEngine;
    
    @BeforeEach
    void setUp() {
        GameModeConfig config = GameModeConfig.builder()
            .linesPerItem(10)
            .enabledItemTypes(Set.of(ItemType.WEIGHT_BOMB))
            .build();
        
        arcadeEngine = new ArcadeGameEngine(config);
        classicEngine = new ClassicGameEngine(config);
    }
    
    @Test
    @DisplayName("🔴 버그 재현: I 블록 하드드롭 시 Y=19 초과 방지")
    void testHardDrop_ShouldNotExceedBoardHeight() {
        // Given: 빈 보드
        GameState state = new GameState(10, 20);
        
        // I 블록 생성 (가로 4칸)
        Tetromino iBlock = new Tetromino(TetrominoType.I);
        state.setCurrentTetromino(iBlock);
        state.setCurrentX(3); // 중심을 X=3으로 (블록 범위: X=1~4)
        state.setCurrentY(0); // 최상단에서 시작
        
        // When: Hard Drop
        GameState result = classicEngine.hardDrop(state);
        
        // Then: Grid에 블록이 Y=19에 고정되어야 함 (가로 배치이므로 1줄)
        int blockCount = 0;
        int maxY = -1;
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                if (result.getGrid()[row][col].isOccupied()) {
                    blockCount++;
                    maxY = Math.max(maxY, row);
                }
            }
        }
        assertEquals(4, blockCount, "I 블록(가로)은 4개 블록이어야 함");
        assertEquals(19, maxY, "I 블록의 최하단은 Y=19여야 함 (실제: " + maxY + ")");
    }
    
    @Test
    @DisplayName("🔴 경계 케이스: Y=18에서 Y=19로 1칸 hardDrop")
    void testHardDrop_OneStepToBottom() {
        // Given: Y=18에 블록 위치
        GameState state = new GameState(10, 20);
        Tetromino oBlock = new Tetromino(TetrominoType.O);
        state.setCurrentTetromino(oBlock);
        state.setCurrentX(4);
        state.setCurrentY(18);
        
        // When: Hard Drop (1칸만 내려감)
        GameState result = classicEngine.hardDrop(state);
        
        // Then: O-블록이 Y=18~19에 고정되어야 함
        boolean hasBlock = false;
        for (int row = 18; row < 20; row++) {
            for (int col = 4; col <= 5; col++) {
                if (result.getGrid()[row][col].isOccupied()) {
                    hasBlock = true;
                }
            }
        }
        assertTrue(hasBlock, "O-블록이 Y=18~19 범위에 고정되어야 함");
    }
    
    @Test
    @DisplayName("🔴 무게추 재낙하: Y=19 초과 방지")
    void testWeightBomb_ReDrop_ShouldNotExceedBoardHeight() {
        // Given: 무게추 블록 생성
        GameState state = new GameState(10, 20);
        
        // 하단에 블록 배치 (Y=18~19)
        for (int col = 0; col < 10; col++) {
            if (col < 1 || col > 4) {
                state.getGrid()[18][col].setOccupied(true);
                state.getGrid()[19][col].setOccupied(true);
            }
        }
        
        Tetromino weightBomb = new Tetromino(TetrominoType.WEIGHT_BOMB);
        state.setCurrentTetromino(weightBomb);
        state.setCurrentX(1);
        state.setCurrentY(14); // 중간에서 시작
        state.setCurrentItemType(ItemType.WEIGHT_BOMB);
        
        // When: Lock (무게추는 경로를 삭제한 후 재낙하)
        GameState result = arcadeEngine.lockTetromino(state);
        
        // Then: Y 좌표가 19 이하여야 함
        int finalY = result.getLastLockedY();
        assertTrue(finalY >= 0 && finalY < 20, 
            "무게추 재낙하 후 Y 좌표는 0~19 범위여야 함 (실제: " + finalY + ")");
    }
    
    @Test
    @DisplayName("✅ 정상 케이스: 빈 보드에서 hardDrop (기존 테스트)")
    void testHardDrop_EmptyBoard_WorksFine() {
        // Given: 빈 보드
        GameState state = new GameState(10, 20);
        Tetromino tBlock = new Tetromino(TetrominoType.T);
        state.setCurrentTetromino(tBlock);
        state.setCurrentX(5);
        state.setCurrentY(0);
        
        // When: Hard Drop
        GameState result = classicEngine.hardDrop(state);
        
        // Then: 정상 작동
        int finalY = result.getLastLockedY();
        assertTrue(finalY >= 17 && finalY < 20, 
            "빈 보드에서는 Y=17~19 사이에 착지해야 함 (실제: " + finalY + ")");
    }
    
    @Test
    @DisplayName("🔴 I 블록 세로 배치: Y=16~19 범위에서 hardDrop")
    void testHardDrop_IBlock_Vertical_NearBottom() {
        // Given: I 블록 세로 배치 (4칸 높이)
        GameState state = new GameState(10, 20);
        
        // 빈 보드 (I 블록이 바닥까지 떨어질 수 있도록)
        
        Tetromino iBlock = new Tetromino(TetrominoType.I);
        // I 블록을 세로로 회전
        iBlock = iBlock.getRotatedInstance(seoultech.se.core.model.enumType.RotationDirection.CLOCKWISE);
        
        state.setCurrentTetromino(iBlock);
        state.setCurrentX(5);
        state.setCurrentY(0); // 최상단에서 시작
        
        // When: Hard Drop
        GameState result = classicEngine.hardDrop(state);
        
        // Then: I 블록(세로 4칸)이 바닥(Y=16~19)에 고정되어야 함
        int totalBlocks = 0;
        int minY = 20, maxY = -1;
        int minX = 10, maxX = -1;
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                if (result.getGrid()[row][col].isOccupied()) {
                    totalBlocks++;
                    minY = Math.min(minY, row);
                    maxY = Math.max(maxY, row);
                    minX = Math.min(minX, col);
                    maxX = Math.max(maxX, col);
                }
            }
        }
        assertEquals(4, totalBlocks, "I 블록은 4칸이어야 함 (실제: " + totalBlocks + "개)");
        assertTrue(maxY <= 19, "블록의 최하단은 Y=19 이하여야 함 (실제: " + maxY + ")");
        assertTrue(minY >= 16, "I 블록(세로)은 Y=16~19에 있어야 함 (실제: " + minY + "~" + maxY + ")");
    }
}
