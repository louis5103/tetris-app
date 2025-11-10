package seoultech.se.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameEngine;
import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * 아이템 블록 Lock 테스트
 * 
 * 테스트 시나리오:
 * 1. 아이템 블록 Lock 시 Grid에 고정되지 않고 사라짐
 * 2. GameOver 임계값에서 아이템 블록은 GameOver되지 않음
 * 3. Lock 후 아이템 효과가 정상적으로 적용됨
 */
class ItemBlockLockTest {

    private GameEngine gameEngine;
    private GameState gameState;

    @BeforeEach
    void setUp() {
        // 아케이드 모드 설정으로 게임 엔진 초기화
        GameModeConfig config = GameModeConfig.arcade();
        gameEngine = new GameEngine();
        gameEngine.initialize(config);
        
        gameState = new GameState(10, 20);
    }

    @Test
    @DisplayName("아이템 블록은 Lock 시 Grid에 고정되지 않음")
    void testItemBlockDoesNotLockToGrid() {
        // Given: 테트로미노 생성 및 아이템 타입 설정
        Tetromino tetromino = new Tetromino(TetrominoType.I);
        gameState.setCurrentTetromino(tetromino);
        gameState.setCurrentX(4);
        gameState.setCurrentY(18); // 하단에 배치
        
        // 아이템 타입 설정 (Bomb 아이템)
        gameState.setCurrentItemType(ItemType.BOMB);
        
        // When: Lock 실행
        GameState lockedState = GameEngine.lockTetromino(gameState);
        
        // Then: Grid에 블록이 고정되지 않음 (모든 셀이 비어있어야 함)
        Cell[][] grid = lockedState.getGrid();
        for (int row = 0; row < lockedState.getBoardHeight(); row++) {
            for (int col = 0; col < lockedState.getBoardWidth(); col++) {
                assertFalse(grid[row][col].isOccupied(), 
                    "아이템 블록은 Grid에 고정되지 않아야 합니다. (Row: " + row + ", Col: " + col + ")");
            }
        }
        
        // 아이템 타입도 초기화되어야 함
        assertNull(lockedState.getCurrentItemType(), 
            "Lock 후 currentItemType은 null이어야 합니다");
    }

    @Test
    @DisplayName("일반 블록은 Lock 시 Grid에 정상적으로 고정됨")
    void testNormalBlockLocksToGrid() {
        // Given: 일반 테트로미노 생성 (아이템 아님)
        Tetromino tetromino = new Tetromino(TetrominoType.I);
        gameState.setCurrentTetromino(tetromino);
        gameState.setCurrentX(4);
        gameState.setCurrentY(18);
        
        // 아이템 타입 설정 안함 (null)
        gameState.setCurrentItemType(null);
        
        // When: Lock 실행
        GameState lockedState = GameEngine.lockTetromino(gameState);
        
        // Then: Grid에 블록이 고정되어야 함
        Cell[][] grid = lockedState.getGrid();
        int occupiedCells = 0;
        for (int row = 0; row < lockedState.getBoardHeight(); row++) {
            for (int col = 0; col < lockedState.getBoardWidth(); col++) {
                if (grid[row][col].isOccupied()) {
                    occupiedCells++;
                }
            }
        }
        
        assertEquals(4, occupiedCells, 
            "I 블록은 4개의 셀을 차지해야 합니다");
    }

    @Test
    @DisplayName("아이템 블록은 상단(y<0)에서 Lock되어도 GameOver되지 않음")
    void testItemBlockDoesNotTriggerGameOverAtTop() {
        // Given: 테트로미노를 상단에 배치
        Tetromino tetromino = new Tetromino(TetrominoType.I);
        gameState.setCurrentTetromino(tetromino);
        gameState.setCurrentX(4);
        gameState.setCurrentY(-1); // 보드 위쪽에 위치
        
        // 아이템 타입 설정
        gameState.setCurrentItemType(ItemType.PLUS);
        
        // When: Lock 실행
        GameState lockedState = GameEngine.lockTetromino(gameState);
        
        // Then: GameOver가 발생하지 않아야 함
        assertFalse(lockedState.isGameOver(), 
            "아이템 블록은 상단에 Lock되어도 GameOver되지 않아야 합니다");
    }

    @Test
    @DisplayName("일반 블록은 상단(y<0)에서 Lock되면 GameOver")
    void testNormalBlockTriggersGameOverAtTop() {
        // Given: 일반 테트로미노를 상단에 배치
        Tetromino tetromino = new Tetromino(TetrominoType.I);
        gameState.setCurrentTetromino(tetromino);
        gameState.setCurrentX(4);
        gameState.setCurrentY(-1); // 보드 위쪽에 위치
        
        // 아이템 타입 설정 안함
        gameState.setCurrentItemType(null);
        
        // When: Lock 실행
        GameState lockedState = GameEngine.lockTetromino(gameState);
        
        // Then: GameOver가 발생해야 함
        assertTrue(lockedState.isGameOver(), 
            "일반 블록은 상단에 Lock되면 GameOver되어야 합니다");
    }

    @Test
    @DisplayName("아이템 효과가 Lock 후 정상적으로 적용됨")
    void testItemEffectAppliedAfterLock() {
        // Given: 보드에 일부 블록 배치
        for (int col = 0; col < 10; col++) {
            gameState.getGrid()[19][col].setOccupied(true);
            gameState.getGrid()[19][col].setColor(seoultech.se.core.model.enumType.Color.RED);
        }
        
        // 테트로미노 생성 및 아이템 타입 설정
        Tetromino tetromino = new Tetromino(TetrominoType.O);
        gameState.setCurrentTetromino(tetromino);
        gameState.setCurrentX(4);
        gameState.setCurrentY(17);
        gameState.setCurrentItemType(ItemType.BOMB);
        
        // When: Lock 실행
        GameState lockedState = GameEngine.lockTetromino(gameState);
        
        // Then: Lock 메타데이터 확인
        assertNotNull(lockedState.getLastLockedTetromino(), 
            "Lock 메타데이터가 저장되어야 합니다");
        
        // applyItemEffect를 별도로 호출해야 함 (BoardController에서 처리)
        ItemEffect effect = gameEngine.applyItemEffect(lockedState, ItemType.BOMB);
        
        assertTrue(effect.isSuccess(), 
            "아이템 효과가 성공적으로 적용되어야 합니다");
        assertTrue(effect.getBlocksCleared() > 0, 
            "일부 블록이 제거되어야 합니다");
    }

    @Test
    @DisplayName("아이템 블록 Lock 시 콤보/B2B가 초기화됨")
    void testItemBlockLockResetsComboAndB2B() {
        // Given: 콤보와 B2B 설정
        gameState.setComboCount(5);
        gameState.setBackToBackCount(3);
        gameState.setLastActionClearedLines(true);
        gameState.setLastClearWasDifficult(true);
        
        // 테트로미노 생성 및 아이템 타입 설정
        Tetromino tetromino = new Tetromino(TetrominoType.T);
        gameState.setCurrentTetromino(tetromino);
        gameState.setCurrentX(4);
        gameState.setCurrentY(18);
        gameState.setCurrentItemType(ItemType.SPEED_RESET);
        
        // When: Lock 실행
        GameState lockedState = GameEngine.lockTetromino(gameState);
        
        // Then: 콤보와 B2B가 초기화되어야 함
        assertEquals(0, lockedState.getComboCount(), 
            "아이템 블록 Lock 시 콤보가 초기화되어야 합니다");
        assertEquals(0, lockedState.getBackToBackCount(), 
            "아이템 블록 Lock 시 B2B가 초기화되어야 합니다");
        assertFalse(lockedState.isLastActionClearedLines(), 
            "lastActionClearedLines가 false여야 합니다");
        assertFalse(lockedState.isLastClearWasDifficult(), 
            "lastClearWasDifficult가 false여야 합니다");
    }
}
