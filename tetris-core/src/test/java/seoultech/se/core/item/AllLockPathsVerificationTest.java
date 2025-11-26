package seoultech.se.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.ArcadeGameEngine;
import seoultech.se.core.engine.ClassicGameEngine;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.engine.item.ItemEffect;
import seoultech.se.core.engine.item.ItemManager;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * 모든 Lock 경로 검증 테스트
 * 
 * 세 가지 Lock 경로:
 * 1. Hard Drop (스페이스바)
 * 2. Soft Drop + Lock (DOWN 키)
 * 3. Auto Lock (GameLoop 자동 낙하)
 * 
 * 각 경로에서 lockTetromino() 호출 여부와 아이템 효과 적용 확인
 */
@DisplayName("🔒 모든 Lock 경로 검증 테스트")
class AllLockPathsVerificationTest {
    
    private GameState gameState;
    private GameEngine classicEngine;
    private ArcadeGameEngine arcadeEngine;
    private ItemManager itemManager;
    
    @BeforeEach
    void setUp() {
        gameState = new GameState(10, 20);

        // Classic Engine (Stateless)
        classicEngine = new ClassicGameEngine(GameModeConfig.classic());

        // Arcade Engine (Stateless 리팩토링)
        arcadeEngine = new ArcadeGameEngine(GameModeConfig.arcade());
        itemManager = new ItemManager(0.1, java.util.EnumSet.allOf(ItemType.class));
    }
    
    // ========== Hard Drop 경로 검증 ==========
    
    @Test
    @DisplayName("경로 1: Hard Drop - lockTetromino() 호출 확인")
    void testHardDrop_CallsLockTetromino() {
        // Given: T 블록을 중앙에 배치
        Tetromino block = new Tetromino(TetrominoType.T);
        gameState.setCurrentTetromino(block);
        gameState.setCurrentX(3);
        gameState.setCurrentY(0);
        
        // When: hardDrop() 호출
        GameState result = classicEngine.hardDrop(gameState);
        
        // Then: 블록이 고정됨 (lastLockedTetromino가 설정됨)
        assertNotNull(result.getLastLockedTetromino(), 
            "Hard Drop should lock the tetromino");
        assertEquals(TetrominoType.T, result.getLastLockedTetromino().getType());
    }
    
    @Test
    @DisplayName("경로 1: Hard Drop - Pivot 위치 저장 확인")
    void testHardDrop_SavesPivotPosition() {
        // Given: T 블록을 중앙에 배치
        Tetromino block = new Tetromino(TetrominoType.T);
        gameState.setCurrentTetromino(block);
        gameState.setCurrentX(3);
        gameState.setCurrentY(0);
        
        // When: hardDrop() 호출
        GameState result = classicEngine.hardDrop(gameState);
        
        // Then: Pivot 위치가 저장됨
        assertNotEquals(-1, result.getLastLockedPivotY(), 
            "Hard Drop should save pivot Y position");
        assertNotEquals(-1, result.getLastLockedPivotX(), 
            "Hard Drop should save pivot X position");
        
        // Pivot 위치가 보드 범위 내에 있는지 확인
        assertTrue(result.getLastLockedPivotX() >= 0 && result.getLastLockedPivotX() < 10, 
            "Pivot X should be within board bounds");
        assertTrue(result.getLastLockedPivotY() >= 0 && result.getLastLockedPivotY() < 20, 
            "Pivot Y should be within board bounds");
    }
    
    @Test
    @DisplayName("경로 1: Hard Drop + BOMB 아이템 - 효과 적용 가능")
    void testHardDrop_WithBombItem() {
        // Given: BOMB 아이템 블록
        Tetromino block = new Tetromino(TetrominoType.T);
        gameState.setCurrentTetromino(block);
        gameState.setCurrentX(5);
        gameState.setCurrentY(0);
        gameState.setCurrentItemType(ItemType.BOMB);
        
        // 보드에 블록 배치 (BOMB 효과 확인용)
        for (int row = 15; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                gameState.getGrid()[row][col].setOccupied(true);
            }
        }
        
        // When: hardDrop() 호출
        GameState result = arcadeEngine.hardDrop(gameState);
        
        // Then: Pivot 위치가 저장되어 있어야 함 (아이템 효과 적용 위치)
        int pivotY = result.getLastLockedPivotY();
        int pivotX = result.getLastLockedPivotX();
        
        assertTrue(pivotY >= 0, "Pivot Y should be valid");
        assertTrue(pivotX >= 0, "Pivot X should be valid");
        
        // BOMB 아이템 효과 적용 가능
        seoultech.se.core.engine.item.impl.BombItem bombItem = new seoultech.se.core.engine.item.impl.BombItem();
        ItemEffect effect = bombItem.apply(result, pivotY, pivotX);
        
        assertTrue(effect.isSuccess(), "BOMB effect should apply successfully at pivot position");
    }
    
    // ========== Soft Drop + Lock 경로 검증 ==========
    
    @Test
    @DisplayName("경로 2: tryMoveDown 실패 시 - 원본 상태 반환 (Lock 신호)")
    void testSoftDrop_FailureSignalsLock() {
        // Given: 블록을 바닥 위치에 배치
        Tetromino block = new Tetromino(TetrominoType.I);
        gameState.setCurrentTetromino(block);
        gameState.setCurrentX(0);
        gameState.setCurrentY(19);  // 바닥
        
        // When: tryMoveDown 호출 (이동 불가능)
        GameState result = classicEngine.tryMoveDown(gameState, true);
        
        // Then: 원본 상태가 반환됨 (이동 실패 신호)
        assertSame(gameState, result, 
            "tryMoveDown should return original state when movement fails (Lock signal)");
    }
    
    @Test
    @DisplayName("경로 2: tryMoveDown 실패 후 lockTetromino() 호출 - 정상 Lock")
    void testSoftDrop_ThenLock() {
        // Given: 블록을 바닥 바로 위에 배치
        Tetromino block = new Tetromino(TetrominoType.O);  // O 블록은 2x2
        gameState.setCurrentTetromino(block);
        gameState.setCurrentX(4);
        gameState.setCurrentY(18);  // O 블록은 Y=18에서 바닥(Y=19)에 닿음
        
        // When: tryMoveDown 실패 확인
        GameState afterMove = classicEngine.tryMoveDown(gameState, true);
        assertSame(gameState, afterMove, "tryMoveDown should fail at bottom");
        
        // When: lockTetromino() 호출 (BoardController에서 수행)
        GameState result = classicEngine.lockTetromino(gameState);
        
        // Then: 블록이 고정됨
        assertNotNull(result.getLastLockedTetromino());
        assertEquals(TetrominoType.O, result.getLastLockedTetromino().getType());
        
        // Pivot 위치 저장 확인
        assertNotEquals(-1, result.getLastLockedPivotY());
        assertNotEquals(-1, result.getLastLockedPivotX());
    }
    
    @Test
    @DisplayName("경로 2: Soft Drop + LINE_CLEAR 아이템 - Pivot 위치 저장")
    void testSoftDrop_WithLineClearItem() {
        // Given: LINE_CLEAR 아이템 블록 (O 블록 사용)
        Tetromino block = new Tetromino(TetrominoType.O);
        gameState.setCurrentTetromino(block);
        gameState.setCurrentX(4);
        gameState.setCurrentY(18);  // 바닥 바로 위
        gameState.setCurrentItemType(ItemType.LINE_CLEAR);
        
        // When: tryMoveDown 실패 후 lockTetromino() 호출
        GameState afterMove = arcadeEngine.tryMoveDown(gameState, true);
        assertSame(gameState, afterMove, "tryMoveDown should fail at bottom");
        
        GameState result = arcadeEngine.lockTetromino(gameState);
        
        // Then: Pivot 위치 저장됨
        int pivotY = result.getLastLockedPivotY();
        int pivotX = result.getLastLockedPivotX();
        
        assertTrue(pivotY >= 0, "Pivot Y should be valid for LINE_CLEAR");
        assertTrue(pivotX >= 0, "Pivot X should be valid for LINE_CLEAR");
        
        // LINE_CLEAR 아이템 타입 확인
        assertEquals(ItemType.LINE_CLEAR, gameState.getCurrentItemType(), 
            "LINE_CLEAR item should be set on gameState");
        
        // 블록이 고정되었는지 확인
        assertNotNull(result.getLastLockedTetromino(), 
            "Block should be locked");
    }
    
    // ========== Auto Lock (GameLoop) 경로 검증 ==========
    
    @Test
    @DisplayName("경로 3: Auto Lock (GameLoop) - tryMoveDown(false) 실패 후 Lock")
    void testAutoLock_GameLoop() {
        // Given: 블록을 바닥 위치에 배치
        Tetromino block = new Tetromino(TetrominoType.O);
        gameState.setCurrentTetromino(block);
        gameState.setCurrentX(4);
        gameState.setCurrentY(18);
        
        // When: tryMoveDown(false) - 자동 낙하 (GameLoop 시뮬레이션)
        GameState afterMove = classicEngine.tryMoveDown(gameState, false);
        
        // Then: 이동 실패 신호
        assertSame(gameState, afterMove, 
            "Auto drop should fail at bottom");
        
        // When: lockTetromino() 호출 (BoardController에서 수행)
        GameState result = classicEngine.lockTetromino(gameState);
        
        // Then: 블록이 고정됨
        assertNotNull(result.getLastLockedTetromino());
        assertEquals(TetrominoType.O, result.getLastLockedTetromino().getType());
    }
    
    @Test
    @DisplayName("경로 3: Auto Lock + PLUS 아이템 - Pivot 위치 저장")
    void testAutoLock_WithPlusItem() {
        // Given: PLUS 아이템 블록
        Tetromino block = new Tetromino(TetrominoType.T);
        gameState.setCurrentTetromino(block);
        gameState.setCurrentX(5);
        gameState.setCurrentY(17);
        gameState.setCurrentItemType(ItemType.PLUS);
        
        // 보드에 블록 배치 (PLUS 효과 확인용)
        for (int col = 0; col < 10; col++) {
            gameState.getGrid()[18][col].setOccupied(true);
        }
        for (int row = 10; row < 18; row++) {
            gameState.getGrid()[row][5].setOccupied(true);
        }
        
        // When: tryMoveDown(false) 실패 후 lockTetromino()
        GameState afterMove = arcadeEngine.tryMoveDown(gameState, false);
        assertSame(gameState, afterMove);
        
        GameState result = arcadeEngine.lockTetromino(gameState);
        
        // Then: Pivot 위치 저장됨
        int pivotY = result.getLastLockedPivotY();
        int pivotX = result.getLastLockedPivotX();
        
        assertTrue(pivotY >= 0, "Pivot Y should be valid for PLUS");
        assertTrue(pivotX >= 0, "Pivot X should be valid for PLUS");
        
        // PLUS 아이템 효과 적용 가능
        seoultech.se.core.engine.item.impl.PlusItem plusItem = new seoultech.se.core.engine.item.impl.PlusItem();
        ItemEffect effect = plusItem.apply(result, pivotY, pivotX);
        
        assertTrue(effect.isSuccess(), "PLUS effect should apply successfully at pivot position");
    }
    
    @Test
    @DisplayName("경로 3: Auto Lock + 점수 확인 - Soft Drop과 다름")
    void testAutoLock_NoScoreForAutoDrop() {
        // Given: 블록 배치
        Tetromino block = new Tetromino(TetrominoType.T);
        gameState.setCurrentTetromino(block);
        gameState.setCurrentX(3);
        gameState.setCurrentY(10);
        
        long initialScore = gameState.getScore();
        
        // When: tryMoveDown(false) - 자동 낙하 (점수 없음)
        GameState result = classicEngine.tryMoveDown(gameState, false);
        
        // Then: 점수 변화 없음 (자동 낙하는 점수를 주지 않음)
        if (result != gameState) {
            // 이동 성공 시
            assertEquals(initialScore, result.getScore(), 
                "Auto drop should not add score");
        }
    }
    
    @Test
    @DisplayName("경로 비교: Soft Drop(true) vs Auto Lock(false) - 점수 차이")
    void testSoftDropVsAutoLock_ScoreDifference() {
        // Given: 동일한 블록 위치
        Tetromino block1 = new Tetromino(TetrominoType.T);
        gameState.setCurrentTetromino(block1);
        gameState.setCurrentX(3);
        gameState.setCurrentY(10);
        
        long initialScore = gameState.getScore();
        
        // When: Soft Drop (isSoftDrop=true)
        GameState softDropResult = classicEngine.tryMoveDown(gameState, true);
        
        // Then: 점수 +1
        if (softDropResult != gameState) {
            assertEquals(initialScore + 1, softDropResult.getScore(), 
                "Soft drop should add 1 point");
        }
        
        // Given: 같은 위치 다시 설정
        gameState.setScore(initialScore);
        
        // When: Auto Lock (isSoftDrop=false)
        GameState autoLockResult = classicEngine.tryMoveDown(gameState, false);
        
        // Then: 점수 변화 없음
        if (autoLockResult != gameState) {
            assertEquals(initialScore, autoLockResult.getScore(), 
                "Auto lock should not add score");
        }
    }
    
    // ========== 통합 검증 ==========
    
    @Test
    @DisplayName("통합: 세 가지 Lock 경로 모두 Pivot 위치 저장")
    void testAllPaths_SavePivotPosition() {
        // Path 1: Hard Drop
        GameState state1 = new GameState(10, 20);
        state1.setCurrentTetromino(new Tetromino(TetrominoType.T));
        state1.setCurrentX(3);
        state1.setCurrentY(0);
        
        GameState result1 = classicEngine.hardDrop(state1);
        assertNotEquals(-1, result1.getLastLockedPivotY(), "Hard Drop should save pivot");
        assertNotEquals(-1, result1.getLastLockedPivotX(), "Hard Drop should save pivot");
        
        // Path 2: Soft Drop + Lock
        GameState state2 = new GameState(10, 20);
        state2.setCurrentTetromino(new Tetromino(TetrominoType.T));
        state2.setCurrentX(3);
        state2.setCurrentY(18);
        
        GameState result2 = classicEngine.lockTetromino(state2);
        assertNotEquals(-1, result2.getLastLockedPivotY(), "Soft Drop Lock should save pivot");
        assertNotEquals(-1, result2.getLastLockedPivotX(), "Soft Drop Lock should save pivot");
        
        // Path 3: Auto Lock
        GameState state3 = new GameState(10, 20);
        state3.setCurrentTetromino(new Tetromino(TetrominoType.T));
        state3.setCurrentX(3);
        state3.setCurrentY(18);
        
        GameState result3 = classicEngine.lockTetromino(state3);
        assertNotEquals(-1, result3.getLastLockedPivotY(), "Auto Lock should save pivot");
        assertNotEquals(-1, result3.getLastLockedPivotX(), "Auto Lock should save pivot");
    }
    
    @Test
    @DisplayName("통합: 모든 경로에서 아이템 효과 적용 가능")
    void testAllPaths_ItemEffectApplicable() {
        // Path 1: Hard Drop + BOMB
        GameState state1 = new GameState(10, 20);
        state1.setCurrentTetromino(new Tetromino(TetrominoType.T));
        state1.setCurrentX(5);
        state1.setCurrentY(0);
        state1.setCurrentItemType(ItemType.BOMB);
        
        // 보드 채우기
        for (int row = 15; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                state1.getGrid()[row][col].setOccupied(true);
            }
        }
        
        GameState result1 = arcadeEngine.hardDrop(state1);
        seoultech.se.core.engine.item.impl.BombItem bombItem = new seoultech.se.core.engine.item.impl.BombItem();
        ItemEffect effect1 = bombItem.apply(result1, result1.getLastLockedPivotY(), result1.getLastLockedPivotX());
        assertTrue(effect1.isSuccess(), "BOMB should work after Hard Drop");
        
        // Path 2: Soft Drop Lock + PLUS
        GameState state2 = new GameState(10, 20);
        state2.setCurrentTetromino(new Tetromino(TetrominoType.T));
        state2.setCurrentX(5);
        state2.setCurrentY(17);
        state2.setCurrentItemType(ItemType.PLUS);
        
        for (int col = 0; col < 10; col++) {
            state2.getGrid()[18][col].setOccupied(true);
        }
        
        GameState result2 = arcadeEngine.lockTetromino(state2);
        seoultech.se.core.engine.item.impl.PlusItem plusItem = new seoultech.se.core.engine.item.impl.PlusItem();
        ItemEffect effect2 = plusItem.apply(result2, result2.getLastLockedPivotY(), result2.getLastLockedPivotX());
        assertTrue(effect2.isSuccess(), "PLUS should work after Soft Drop Lock");
        
        // Path 3: Auto Lock + SPEED_RESET
        GameState state3 = new GameState(10, 20);
        state3.setCurrentTetromino(new Tetromino(TetrominoType.T));
        state3.setCurrentX(5);
        state3.setCurrentY(17);
        state3.setCurrentItemType(ItemType.SPEED_RESET);
        
        GameState result3 = arcadeEngine.lockTetromino(state3);
        seoultech.se.core.engine.item.impl.SpeedResetItem speedResetItem = new seoultech.se.core.engine.item.impl.SpeedResetItem();
        ItemEffect effect3 = speedResetItem.apply(result3, result3.getLastLockedPivotY(), result3.getLastLockedPivotX());
        assertTrue(effect3.isSuccess(), "SPEED_RESET should work after Auto Lock");
    }
}
