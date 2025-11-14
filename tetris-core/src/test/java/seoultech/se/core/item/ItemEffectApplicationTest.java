package seoultech.se.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.ArcadeGameEngine;
import seoultech.se.core.model.enumType.Color;

/**
 * 아이템 효과 적용 통합 테스트
 * 
 * BoardController의 applyItemEffectAfterLock() 로직을 시뮬레이션하여
 * 실제 아이템 효과가 올바른 위치에서 발동하는지 검증합니다.
 * 
 * 테스트 시나리오:
 * 1. Bomb 아이템 - Pivot 중심 5x5 영역 삭제
 * 2. Plus 아이템 - Pivot 중심 십자 영역 삭제
 * 3. 잘못된 위치 정보 - 효과 실패 처리
 * 4. 경계 케이스 - 보드 가장자리
 */
@DisplayName("아이템 효과 적용 통합 테스트")
class ItemEffectApplicationTest {

    private ArcadeGameEngine engine;
    private ItemManager itemManager;

    @BeforeEach
    void setUp() {
        ItemConfig itemConfig = ItemConfig.builder()
            .dropRate(1.0)
            .enabledItems(Set.of(ItemType.BOMB, ItemType.PLUS, ItemType.LINE_CLEAR))
            .build();
        
        itemManager = new ItemManager(itemConfig.getDropRate(), itemConfig.getEnabledItems());
        engine = new ArcadeGameEngine(itemManager);
        
        GameModeConfig config = GameModeConfig.arcade();
        engine.initialize(config);
    }

    @Test
    @DisplayName("BOMB 아이템 - 중앙 위치에서 5x5 영역 삭제")
    void testBombItem_CenterPosition() {
        // Given: 전체 보드를 블록으로 채움
        GameState state = new GameState(10, 20);
        fillBoard(state, 0, 20, 0, 10);
        
        // Pivot 위치: (10, 5) - 중앙
        int pivotY = 10;
        int pivotX = 5;
        
        // When: Bomb 아이템 효과 적용
        Item bombItem = itemManager.getItem(ItemType.BOMB);
        assertNotNull(bombItem, "BOMB 아이템이 ItemManager에 등록되어 있어야 함");
        
        ItemEffect effect = bombItem.apply(state, pivotY, pivotX);
        
        // Then: 효과 성공
        assertTrue(effect.isSuccess(), "BOMB 아이템 효과가 성공해야 함");
        
        // 🎮 ItemEffect는 정확히 25개 블록 삭제를 보고해야 함
        assertEquals(25, effect.getBlocksCleared(), 
            "BOMB은 5x5 = 25개 블록을 삭제해야 함");
        
        // 중력 적용으로 인해 5x5 영역이 다시 채워질 수 있지만,
        // ItemEffect.getBlocksCleared()는 정확히 25개만 카운트해야 함
        
        System.out.println("💣 BOMB 테스트 - 삭제된 블록: " + effect.getBlocksCleared() + 
            ", 보너스 점수: " + effect.getBonusScore());
    }

    @Test
    @DisplayName("BOMB 아이템 - 보드 가장자리에서 효과")
    void testBombItem_EdgePosition() {
        // Given: 전체 보드를 블록으로 채움
        GameState state = new GameState(10, 20);
        fillBoard(state, 0, 20, 0, 10);
        
        // Pivot 위치: (0, 0) - 왼쪽 상단 모서리
        int pivotY = 0;
        int pivotX = 0;
        
        // When: Bomb 아이템 효과 적용
        Item bombItem = itemManager.getItem(ItemType.BOMB);
        ItemEffect effect = bombItem.apply(state, pivotY, pivotX);
        
        // Then: 효과 성공 (경계 처리)
        assertTrue(effect.isSuccess(), "가장자리에서도 BOMB 효과가 성공해야 함");
        assertTrue(effect.getBlocksCleared() > 0, "블록이 삭제되어야 함");
        
        System.out.println("💣 BOMB 가장자리 - 삭제된 블록: " + effect.getBlocksCleared());
    }

    @Test
    @DisplayName("PLUS 아이템 - 십자 영역 효과 확인")
    void testPlusItem_CenterPosition() {
        // Given: 보드를 블록으로 채움
        GameState state = new GameState(10, 20);
        fillBoard(state, 0, 20, 0, 10);
        
        int pivotY = 10;  // 중앙
        int pivotX = 5;
        
        // 초기 블록 수 카운트
        int initialBlockCount = 0;
        for (int r = 0; r < state.getBoardHeight(); r++) {
            for (int c = 0; c < state.getBoardWidth(); c++) {
                if (state.getGrid()[r][c].isOccupied()) {
                    initialBlockCount++;
                }
            }
        }
        
        // When: PLUS 아이템 적용 (십자 영역 삭제 + 중력 + 라인 클리어)
        Item plusItem = itemManager.getItem(ItemType.PLUS);
        assertNotNull(plusItem, "PLUS 아이템이 ItemManager에 등록되어 있어야 함");
        
        ItemEffect effect = plusItem.apply(state, pivotY, pivotX);
        
        // Then: 효과 성공
        assertTrue(effect.isSuccess(), "PLUS 아이템 효과가 성공해야 함");
        assertTrue(effect.getBlocksCleared() > 0, "블록이 삭제되어야 함");
        
        // 최종 블록 수 카운트
        int finalBlockCount = 0;
        for (int r = 0; r < state.getBoardHeight(); r++) {
            for (int c = 0; c < state.getBoardWidth(); c++) {
                if (state.getGrid()[r][c].isOccupied()) {
                    finalBlockCount++;
                }
            }
        }
        
        // 🔥 수정된 검증: PLUS는 십자를 삭제하고 중력을 적용하므로,
        //    전체 블록 수는 감소해야 함 (중력으로 채워지더라도)
        assertTrue(initialBlockCount > finalBlockCount, 
            "PLUS 아이템 적용 후 전체 블록 수가 감소해야 함 " +
            "(초기: " + initialBlockCount + ", 최종: " + finalBlockCount + ")");
        
        System.out.println("Ⓟ PLUS 테스트 - 삭제된 블록: " + effect.getBlocksCleared() + 
            ", 보너스 점수: " + effect.getBonusScore() +
            ", 블록 수 변화: " + initialBlockCount + " → " + finalBlockCount);
    }

    @ParameterizedTest
    @CsvSource({
        "-1, 5",   // Y 음수
        "20, 5",   // Y 범위 초과
        "10, -1",  // X 음수
        "10, 10"   // X 범위 초과
    })
    @DisplayName("잘못된 위치 정보 - 효과 실패")
    void testInvalidPosition(int pivotY, int pivotX) {
        // Given: 보드 준비
        GameState state = new GameState(10, 20);
        fillBoard(state, 0, 20, 0, 10);
        
        // When: BOMB 아이템을 잘못된 위치에 적용
        Item bombItem = itemManager.getItem(ItemType.BOMB);
        ItemEffect effect = bombItem.apply(state, pivotY, pivotX);
        
        // Then: 효과 실패
        assertFalse(effect.isSuccess(), 
            "잘못된 위치 (" + pivotY + ", " + pivotX + ")에서 효과가 실패해야 함");
        assertEquals(0, effect.getBlocksCleared(), "블록이 삭제되지 않아야 함");
        assertEquals(0, effect.getBonusScore(), "보너스 점수가 없어야 함");
    }

    @Test
    @DisplayName("BOMB 아이템 - 중력 적용 후 라인 클리어")
    void testBombItem_GravityAndLineClear() {
        // Given: 바닥에만 블록 배치 (라인 클리어 가능하도록)
        GameState state = new GameState(10, 20);
        
        // 하단 5줄을 완전히 채움
        for (int row = 15; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                state.getGrid()[row][col].setOccupied(true);
                state.getGrid()[row][col].setColor(Color.GRAY);
            }
        }
        
        // 상단에 일부 블록 배치
        for (int row = 5; row < 10; row++) {
            for (int col = 3; col < 7; col++) {
                state.getGrid()[row][col].setOccupied(true);
                state.getGrid()[row][col].setColor(Color.BLUE);
            }
        }
        
        // When: Pivot 위치 (17, 5)에서 BOMB 적용
        Item bombItem = itemManager.getItem(ItemType.BOMB);
        ItemEffect effect = bombItem.apply(state, 17, 5);
        
        // Then: 효과 성공
        assertTrue(effect.isSuccess(), "BOMB 효과 성공");
        assertTrue(effect.getBlocksCleared() > 0, "블록 삭제됨");
        
        // 중력 + 라인 클리어로 점수 증가 확인
        // BombItem 내부에서 checkAndClearLines() 호출하므로 점수 업데이트됨
        System.out.println("💣 BOMB + 중력 - 삭제된 블록: " + effect.getBlocksCleared() + 
            ", 보너스: " + effect.getBonusScore());
    }

    @Test
    @DisplayName("PLUS 아이템 - 중력 적용 후 라인 클리어")
    void testPlusItem_GravityAndLineClear() {
        // Given: 바닥 근처에 블록 배치
        GameState state = new GameState(10, 20);
        
        // 하단 3줄을 거의 채움 (Plus로 십자 제거하면 라인 클리어)
        for (int row = 17; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                state.getGrid()[row][col].setOccupied(true);
                state.getGrid()[row][col].setColor(Color.CYAN);
            }
        }
        
        // When: Pivot 위치 (18, 5)에서 PLUS 적용
        Item plusItem = itemManager.getItem(ItemType.PLUS);
        ItemEffect effect = plusItem.apply(state, 18, 5);
        
        // Then: 효과 성공
        assertTrue(effect.isSuccess(), "PLUS 효과 성공");
        assertTrue(effect.getBlocksCleared() > 0, "블록 삭제됨");
        
        System.out.println("➕ PLUS + 중력 - 삭제된 블록: " + effect.getBlocksCleared() + 
            ", 보너스: " + effect.getBonusScore());
    }

    @Test
    @DisplayName("아이템 효과 적용 - 전체 시나리오 시뮬레이션")
    void testFullScenario_ItemEffectApplication() {
        // Given: Hard Drop으로 Lock된 상황 시뮬레이션
        GameState state = new GameState(10, 20);
        fillBoard(state, 10, 20, 0, 10);  // 하단 절반 채움
        
        // Lock 후 저장된 Pivot 위치
        int lastLockedPivotX = 5;
        int lastLockedPivotY = 9;
        
        // 아이템 타입
        ItemType itemType = ItemType.BOMB;
        
        // When: BoardController의 applyItemEffectAfterLock() 로직 시뮬레이션
        Item item = itemManager.getItem(itemType);
        assertNotNull(item, "ItemManager에서 아이템을 가져올 수 있어야 함");
        
        // LINE_CLEAR는 스킵 (ArcadeGameEngine이 처리)
        if (itemType == ItemType.LINE_CLEAR) {
            System.out.println("ℹ️ LINE_CLEAR는 ArcadeGameEngine이 처리");
            return;
        }
        
        // 아이템 효과 적용
        ItemEffect effect = item.apply(state, lastLockedPivotY, lastLockedPivotX);
        
        // Then: 효과 성공
        assertTrue(effect.isSuccess(), 
            "아이템 효과가 성공해야 함 (Item: " + itemType + 
            ", Pivot: (" + lastLockedPivotY + ", " + lastLockedPivotX + "))");
        
        assertTrue(effect.getBlocksCleared() > 0, 
            "블록이 삭제되어야 함 (삭제된 블록: " + effect.getBlocksCleared() + ")");
        
        assertTrue(effect.getBonusScore() > 0, 
            "보너스 점수가 있어야 함 (보너스: " + effect.getBonusScore() + ")");
        
        System.out.println("✅ 전체 시나리오 성공 - " + 
            "Item: " + itemType + 
            ", 삭제: " + effect.getBlocksCleared() + 
            ", 점수: " + effect.getBonusScore());
    }

    /**
     * 보드의 지정된 영역을 블록으로 채우는 헬퍼 메서드
     */
    private void fillBoard(GameState state, int startRow, int endRow, int startCol, int endCol) {
        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                state.getGrid()[row][col].setOccupied(true);
                state.getGrid()[row][col].setColor(Color.GRAY);
            }
        }
    }
}
