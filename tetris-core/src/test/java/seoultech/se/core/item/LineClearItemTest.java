package seoultech.se.core.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.item.impl.LineClearItem;

/**
 * LineClearItem 테스트
 * 
 * 명세 검증:
 * - 'L' 마커가 있는 줄을 삭제해야 함
 * - 줄이 꽉 차있지 않아도 삭제해야 함
 * - 삭제된 블록 수만큼 점수 부여
 */
@DisplayName("Ⓛ LINE_CLEAR 아이템 테스트")
class LineClearItemTest {
    
    private GameState gameState;
    private LineClearItem lineClearItem;
    
    @BeforeEach
    void setUp() {
        gameState = new GameState(10, 20);
        lineClearItem = new LineClearItem();
    }
    
    @Test
    @DisplayName("꽉 찬 줄에 'L' 마커가 있으면 삭제해야 함")
    void testFullLineWithMarker() {
        // Given: 19번째 줄을 꽉 채우고 'L' 마커 추가
        int row = 19;
        for (int col = 0; col < 10; col++) {
            gameState.getGrid()[row][col].setOccupied(true);
        }
        gameState.getGrid()[row][5].setItemMarker(ItemType.LINE_CLEAR);
        
        // When: 'L' 마커가 있는 줄 찾기
        List<Integer> markedRows = LineClearItem.findAndClearMarkedLines(gameState);
        
        // Then: row가 찾아져야 함
        assertEquals(1, markedRows.size());
        assertEquals(row, markedRows.get(0));
    }
    
    @Test
    @DisplayName("🎯 명세 검증: 줄이 꽉 차지 않아도 'L' 마커가 있으면 삭제해야 함")
    void testPartiallyFilledLineWithMarker() {
        // Given: 19번째 줄에 3개 블록만 있고 'L' 마커 추가
        int row = 19;
        gameState.getGrid()[row][0].setOccupied(true);
        gameState.getGrid()[row][1].setOccupied(true);
        gameState.getGrid()[row][2].setOccupied(true);
        gameState.getGrid()[row][1].setItemMarker(ItemType.LINE_CLEAR); // 'L' 마커
        
        // 나머지 7칸은 비어있음
        for (int col = 3; col < 10; col++) {
            assertFalse(gameState.getGrid()[row][col].isOccupied(), 
                "Col " + col + " should be empty");
        }
        
        // When: 'L' 마커가 있는 줄 찾기
        List<Integer> markedRows = LineClearItem.findAndClearMarkedLines(gameState);
        
        // Then: 줄이 꽉 차지 않았지만 찾아져야 함
        assertEquals(1, markedRows.size(), "Should find the row with 'L' marker even if not full");
        assertEquals(row, markedRows.get(0));
    }
    
    @Test
    @DisplayName("🎯 명세 검증: 1개 블록만 있어도 'L' 마커가 있으면 삭제해야 함")
    void testSingleBlockLineWithMarker() {
        // Given: 19번째 줄에 블록 1개만 있고 'L' 마커
        int row = 19;
        gameState.getGrid()[row][5].setOccupied(true);
        gameState.getGrid()[row][5].setItemMarker(ItemType.LINE_CLEAR);
        
        // When: 'L' 마커가 있는 줄 찾기
        List<Integer> markedRows = LineClearItem.findAndClearMarkedLines(gameState);
        
        // Then: 1개 블록만 있어도 찾아져야 함
        assertEquals(1, markedRows.size(), "Should find row with only 1 block if it has 'L' marker");
        assertEquals(row, markedRows.get(0));
    }
    
    @Test
    @DisplayName("'L' 마커가 없는 꽉 찬 줄은 찾지 않아야 함")
    void testFullLineWithoutMarker() {
        // Given: 19번째 줄을 꽉 채우되 'L' 마커는 없음
        int row = 19;
        for (int col = 0; col < 10; col++) {
            gameState.getGrid()[row][col].setOccupied(true);
        }
        
        // When: 'L' 마커가 있는 줄 찾기
        List<Integer> markedRows = LineClearItem.findAndClearMarkedLines(gameState);
        
        // Then: 찾지 못해야 함
        assertEquals(0, markedRows.size(), "Should not find full line without 'L' marker");
    }
    
    @Test
    @DisplayName("여러 줄에 'L' 마커가 있으면 모두 찾아야 함")
    void testMultipleLinesWithMarkers() {
        // Given: 17, 18, 19번째 줄에 'L' 마커 (각기 다른 블록 수)
        // 17번째 줄: 2개 블록
        gameState.getGrid()[17][0].setOccupied(true);
        gameState.getGrid()[17][1].setOccupied(true);
        gameState.getGrid()[17][0].setItemMarker(ItemType.LINE_CLEAR);
        
        // 18번째 줄: 5개 블록
        for (int col = 0; col < 5; col++) {
            gameState.getGrid()[18][col].setOccupied(true);
        }
        gameState.getGrid()[18][2].setItemMarker(ItemType.LINE_CLEAR);
        
        // 19번째 줄: 10개 블록 (꽉 참)
        for (int col = 0; col < 10; col++) {
            gameState.getGrid()[19][col].setOccupied(true);
        }
        gameState.getGrid()[19][9].setItemMarker(ItemType.LINE_CLEAR);
        
        // When: 'L' 마커가 있는 줄 찾기
        List<Integer> markedRows = LineClearItem.findAndClearMarkedLines(gameState);
        
        // Then: 3개 줄 모두 찾아져야 함
        assertEquals(3, markedRows.size());
        assertTrue(markedRows.contains(17));
        assertTrue(markedRows.contains(18));
        assertTrue(markedRows.contains(19));
    }
    
    @Test
    @DisplayName("빈 줄에 'L' 마커만 있어도 찾아야 함")
    void testEmptyLineWithMarkerOnly() {
        // Given: 19번째 줄은 비어있지만 마커는 있음 (점유되지 않음)
        int row = 19;
        gameState.getGrid()[row][5].setItemMarker(ItemType.LINE_CLEAR);
        // occupied는 false
        assertFalse(gameState.getGrid()[row][5].isOccupied());
        
        // When: 'L' 마커가 있는 줄 찾기
        List<Integer> markedRows = LineClearItem.findAndClearMarkedLines(gameState);
        
        // Then: 마커가 있으면 찾아져야 함 (occupied 여부와 무관)
        assertEquals(1, markedRows.size());
        assertEquals(row, markedRows.get(0));
    }
    
    @Test
    @DisplayName("clearLines() - 줄 삭제 후 위 블록들이 내려와야 함")
    void testClearLinesWithGravity() {
        // Given: 18번, 19번 줄에 블록 배치
        // 18번째 줄 (위): XXXX......
        for (int col = 0; col < 4; col++) {
            gameState.getGrid()[18][col].setOccupied(true);
        }
        
        // 19번째 줄 (아래): XX........
        gameState.getGrid()[19][0].setOccupied(true);
        gameState.getGrid()[19][1].setOccupied(true);
        
        // When: 19번째 줄 삭제
        List<Integer> rowsToRemove = List.of(19);
        int blocksCleared = LineClearItem.clearLines(gameState, rowsToRemove);
        
        // Then: 2개 블록 삭제됨
        assertEquals(2, blocksCleared);
        
        // 18번째 줄이 19번째로 내려옴
        assertTrue(gameState.getGrid()[19][0].isOccupied());
        assertTrue(gameState.getGrid()[19][1].isOccupied());
        assertTrue(gameState.getGrid()[19][2].isOccupied());
        assertTrue(gameState.getGrid()[19][3].isOccupied());
        
        // 18번째 줄은 비어있음
        for (int col = 0; col < 10; col++) {
            assertFalse(gameState.getGrid()[18][col].isOccupied(), 
                "Row 18 col " + col + " should be empty after gravity");
        }
    }
    
    @Test
    @DisplayName("clearLines() - 여러 줄 삭제 후 중력 적용")
    void testClearMultipleLinesWithGravity() {
        // Given: 17, 18, 19번 줄 모두 채움
        for (int row = 17; row <= 19; row++) {
            for (int col = 0; col < 10; col++) {
                gameState.getGrid()[row][col].setOccupied(true);
            }
        }
        
        // 16번째 줄에 블록 3개
        gameState.getGrid()[16][0].setOccupied(true);
        gameState.getGrid()[16][1].setOccupied(true);
        gameState.getGrid()[16][2].setOccupied(true);
        
        // When: 18, 19번 줄 삭제
        List<Integer> rowsToRemove = List.of(18, 19);
        int blocksCleared = LineClearItem.clearLines(gameState, rowsToRemove);
        
        // Then: 20개 블록 삭제 (2줄 × 10블록)
        assertEquals(20, blocksCleared);
        
        // 17번째 줄이 19번째로 내려옴 (꽉 참)
        for (int col = 0; col < 10; col++) {
            assertTrue(gameState.getGrid()[19][col].isOccupied());
        }
        
        // 16번째 줄이 18번째로 내려옴 (3개)
        assertTrue(gameState.getGrid()[18][0].isOccupied());
        assertTrue(gameState.getGrid()[18][1].isOccupied());
        assertTrue(gameState.getGrid()[18][2].isOccupied());
        for (int col = 3; col < 10; col++) {
            assertFalse(gameState.getGrid()[18][col].isOccupied());
        }
        
        // 16, 17번 줄은 비어있음
        for (int row = 16; row <= 17; row++) {
            for (int col = 0; col < 10; col++) {
                assertFalse(gameState.getGrid()[row][col].isOccupied());
            }
        }
    }
    
    @Test
    @DisplayName("apply() - 지정된 줄의 블록 삭제 및 점수 계산")
    void testApplyMethod() {
        // Given: 19번째 줄에 5개 블록
        int row = 19;
        for (int col = 0; col < 5; col++) {
            gameState.getGrid()[row][col].setOccupied(true);
        }
        
        // When: apply() 호출
        ItemEffect effect = lineClearItem.apply(gameState, row, 0);
        
        // Then: 성공, 5개 블록 삭제, 50점 (10점 × 5)
        assertTrue(effect.isSuccess());
        assertEquals(ItemType.LINE_CLEAR, effect.getItemType());
        assertEquals(5, effect.getBlocksCleared());
        assertEquals(50, effect.getBonusScore());
        
        // 해당 줄이 비어있음
        for (int col = 0; col < 10; col++) {
            assertFalse(gameState.getGrid()[row][col].isOccupied());
        }
    }
    
    @Test
    @DisplayName("apply() - 빈 줄 삭제 시 0점")
    void testApplyOnEmptyLine() {
        // Given: 19번째 줄이 비어있음
        int row = 19;
        
        // When: apply() 호출
        ItemEffect effect = lineClearItem.apply(gameState, row, 0);
        
        // Then: 성공하지만 0개 블록, 0점
        assertTrue(effect.isSuccess());
        assertEquals(0, effect.getBlocksCleared());
        assertEquals(0, effect.getBonusScore());
    }
    
    @Test
    @DisplayName("apply() - 범위 초과 row는 실패")
    void testApplyInvalidRow() {
        // When: 범위 초과
        ItemEffect effect1 = lineClearItem.apply(gameState, -1, 0);
        ItemEffect effect2 = lineClearItem.apply(gameState, 20, 0);
        
        // Then: 실패
        assertFalse(effect1.isSuccess());
        assertFalse(effect2.isSuccess());
    }
    
    @Test
    @DisplayName("아이템 비활성화 시 효과 없음")
    void testDisabledItem() {
        // Given: 아이템 비활성화
        lineClearItem.setEnabled(false);
        
        // 19번째 줄에 블록 배치
        int row = 19;
        for (int col = 0; col < 10; col++) {
            gameState.getGrid()[row][col].setOccupied(true);
        }
        
        // When: apply() 호출
        ItemEffect effect = lineClearItem.apply(gameState, row, 0);
        
        // Then: 효과 없음, 블록 그대로
        assertFalse(effect.isSuccess());
        for (int col = 0; col < 10; col++) {
            assertTrue(gameState.getGrid()[row][col].isOccupied(), 
                "Block should remain when item is disabled");
        }
    }
    
    @Test
    @DisplayName("🎯 통합 시나리오: 부분 채움 + 마커 찾기 + 삭제 + 중력")
    void testIntegratedScenario() {
        // Given: 복잡한 보드 상태
        // 15번째 줄: 3개 블록
        for (int col = 0; col < 3; col++) {
            gameState.getGrid()[15][col].setOccupied(true);
        }
        
        // 17번째 줄: 7개 블록 + 'L' 마커 (꽉 차지 않음!)
        for (int col = 0; col < 7; col++) {
            gameState.getGrid()[17][col].setOccupied(true);
        }
        gameState.getGrid()[17][3].setItemMarker(ItemType.LINE_CLEAR);
        
        // 19번째 줄: 10개 블록 + 'L' 마커 (꽉 참)
        for (int col = 0; col < 10; col++) {
            gameState.getGrid()[19][col].setOccupied(true);
        }
        gameState.getGrid()[19][5].setItemMarker(ItemType.LINE_CLEAR);
        
        // When: 'L' 마커 찾기
        List<Integer> markedRows = LineClearItem.findAndClearMarkedLines(gameState);
        
        // Then: 17, 19번 줄 찾아짐 (17번은 꽉 차지 않았지만 마커 있음)
        assertEquals(2, markedRows.size());
        assertTrue(markedRows.contains(17));
        assertTrue(markedRows.contains(19));
        
        // When: 찾아진 줄 삭제
        int blocksCleared = LineClearItem.clearLines(gameState, markedRows);
        
        // Then: 17개 블록 삭제 (7 + 10)
        assertEquals(17, blocksCleared);
        
        // 중력 적용 후 보드 상태:
        // 원래: [15: XXX.......], 16: 비움, [17: 삭제됨], 18: 비움, [19: 삭제됨]
        // 결과: 0~17: 비움, 18: 비움, [19: XXX.......]
        // 15번째 줄이 2칸 내려와서 17번째로 이동
        // (17번과 19번이 삭제되어 2개 줄이 사라짐)
        
        // 15번째 줄(3개 블록)이 17번째로 내려옴
        assertTrue(gameState.getGrid()[17][0].isOccupied());
        assertTrue(gameState.getGrid()[17][1].isOccupied());
        assertTrue(gameState.getGrid()[17][2].isOccupied());
        for (int col = 3; col < 10; col++) {
            assertFalse(gameState.getGrid()[17][col].isOccupied());
        }
        
        // 15~16번, 18~19번 줄은 비어있음
        for (int row = 15; row <= 16; row++) {
            for (int col = 0; col < 10; col++) {
                assertFalse(gameState.getGrid()[row][col].isOccupied(), 
                    "Row " + row + " col " + col + " should be empty");
            }
        }
        for (int row = 18; row <= 19; row++) {
            for (int col = 0; col < 10; col++) {
                assertFalse(gameState.getGrid()[row][col].isOccupied(), 
                    "Row " + row + " col " + col + " should be empty");
            }
        }
    }
}
