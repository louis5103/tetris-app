package seoultech.se.core.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.item.ItemManager;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.engine.item.impl.LineClearItem;
import seoultech.se.core.model.enumType.Color;

/**
 * 실제 버그 재현 테스트
 * 
 * 버그 설명:
 * 1. T블록 하드드롭 시 (16,0), (17,0), (17,1) 위치에 블록 생성
 * 2. LINE_CLEAR 마커가 (17,0)에 배치됨
 * 3. Row 17이 삭제되어야 함 → (17,0), (17,1) 모두 삭제
 * 4. 중력 적용 → (16,0)이 (17,0)으로 이동
 * 5. 예상 결과: (17,0)만 존재 [중력으로 내려온 블록]
 * 6. 실제 결과: (17,0), (17,1) 모두 존재 ← 🐛 (17,1)이 삭제되지 않음!
 */
public class LineClearActualBugTest {
    
    private void printBoardState(GameState state) {
        System.out.println("Board state (rows 15-19):");
        for (int row = 15; row < 20; row++) {
            System.out.print("  Row " + row + ": ");
            for (int col = 0; col < 10; col++) {
                System.out.print(state.getGrid()[row][col].isOccupied() ? "█" : "·");
            }
            System.out.println();
        }
    }
    
    @Test
    @DisplayName("🐛 실제 버그: LINE_CLEAR가 row 17을 삭제했는데 (17,1)이 남아있음")
    public void testLineClearBug_Cell17_1_RemainsAfterRowDeletion() {
        // Given: 버그 재현을 위한 초기 상태
        ItemManager itemManager = new ItemManager();
        GameState state = new GameState(10, 20);
        
        GameModeConfig config = GameModeConfig.builder()
            .gameplayType(seoultech.se.core.config.GameplayType.ARCADE)
            .difficulty(seoultech.se.core.model.enumType.Difficulty.NORMAL)
            .itemAutoUse(true)
            .build();
        
        ArcadeGameEngine engine = new ArcadeGameEngine(config);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🐛 버그 재현 시나리오");
        System.out.println("=".repeat(60));
        System.out.println("1. T블록을 X=0 위치에서 하드드롭");
        System.out.println("2. T블록 최종 위치: (16,0), (17,0), (17,1)");
        System.out.println("3. LINE_CLEAR 마커 위치: (17,0)");
        System.out.println("4. 예상 동작: Row 17 삭제 → (17,0), (17,1) 모두 삭제");
        System.out.println("5. 중력 적용: (16,0) → (17,0)으로 이동");
        System.out.println("6. 예상 최종 결과: (17,0)만 존재");
        System.out.println("7. 🐛 실제 결과: (17,0), (17,1) 모두 존재!");
        System.out.println("=".repeat(60) + "\n");
        
        // 하단 블록 배치 (row 19, 18)
        for (int col = 0; col < 10; col++) {
            if (col != 4) {
                state.getGrid()[19][col].setOccupied(true);
                state.getGrid()[19][col].setColor(Color.GRAY);
            }
        }
        state.getGrid()[18][0].setOccupied(true);
        state.getGrid()[18][0].setColor(Color.CYAN);
        
        System.out.println("📋 초기 보드 상태:");
        printBoardState(state);
        
        // When: T블록을 직접 배치하고 LINE_CLEAR 적용 시뮬레이션
        System.out.println("\n🔽 버그 상황 재현...");
        System.out.println("   1. T블록 하드드롭 완료");
        System.out.println("   2. T블록 위치: (16,0), (17,0), (17,1)");
        System.out.println("   3. LINE_CLEAR 마커: (17,0)");
        
        // T블록 직접 배치 (하드드롭 후 상태)
        state.getGrid()[16][0].setOccupied(true);
        state.getGrid()[16][0].setColor(Color.MAGENTA);
        state.getGrid()[17][0].setOccupied(true);
        state.getGrid()[17][0].setColor(Color.MAGENTA);
        state.getGrid()[17][1].setOccupied(true);
        state.getGrid()[17][1].setColor(Color.MAGENTA);
        
        // LINE_CLEAR 마커를 (17,0)에 설정
        state.getGrid()[17][0].setItemMarker(ItemType.LINE_CLEAR);
        
        System.out.println("\n📋 T블록 배치 후 (LINE_CLEAR 마커 포함):");
        printBoardState(state);
        System.out.println("   LINE_CLEAR 마커: (17,0) ✓");
        
        // LINE_CLEAR 아이템 적용 - row 17 삭제
        System.out.println("\n🔥 LINE_CLEAR 적용: Row 17 삭제...");
        LineClearItem lineClearItem = new LineClearItem();
        lineClearItem.apply(state, 0, 17);  // (17,0) 위치의 LINE_CLEAR 마커
        
        System.out.println("\n📋 최종 보드 상태:");
        printBoardState(state);
        
        // Then: 버그 검증
        System.out.println("\n" + "=".repeat(60));
        System.out.println("🔍 검증 결과");
        System.out.println("=".repeat(60));
        
        boolean cell_17_0 = state.getGrid()[17][0].isOccupied();
        boolean cell_17_1 = state.getGrid()[17][1].isOccupied();
        boolean cell_16_0 = state.getGrid()[16][0].isOccupied();
        boolean cell_18_0 = state.getGrid()[18][0].isOccupied();
        
        System.out.println("Row 16:");
        System.out.println("  (16,0): " + (cell_16_0 ? "█ OCCUPIED" : "· EMPTY"));
        System.out.println("\nRow 17:");
        System.out.println("  (17,0): " + (cell_17_0 ? "█ OCCUPIED" : "· EMPTY") + 
                         " ← 중력으로 (16,0)이 내려와야 함");
        System.out.println("  (17,1): " + (cell_17_1 ? "🐛 OCCUPIED (버그!)" : "✅ EMPTY") + 
                         " ← Row 17 삭제로 비어있어야 함!");
        System.out.println("\nRow 18:");
        System.out.println("  (18,0): " + (cell_18_0 ? "█ OCCUPIED" : "· EMPTY") + " ← 기존 블록");
        
        System.out.println("\n분석:");
        if (cell_17_1) {
            System.out.println("❌ 버그 발견!");
            System.out.println("   LINE_CLEAR가 Row 17을 삭제했는데 (17,1)이 남아있습니다.");
            System.out.println("   원인 추정: 중력 적용 시 일부 셀만 처리되고 (17,1)은 누락됨");
        } else {
            System.out.println("✅ 정상 동작");
            System.out.println("   Row 17이 올바르게 삭제되었습니다.");
        }
        
        System.out.println("=".repeat(60));
        
        // 최종 검증
        assertFalse(state.getGrid()[17][1].isOccupied(),
            "🐛 버그 발견! LINE_CLEAR로 Row 17이 삭제되었는데 (17,1)이 남아있습니다!");
        
        assertTrue(state.getGrid()[17][0].isOccupied(),
            "(17,0)은 중력으로 (16,0)이 내려와서 존재해야 합니다");
        
        assertTrue(state.getGrid()[18][0].isOccupied(),
            "(18,0)은 기존 블록이므로 존재해야 합니다");
        
        assertFalse(state.getGrid()[16][0].isOccupied(),
            "(16,0)은 중력으로 (17,0)으로 이동했으므로 비어있어야 합니다");
        
        System.out.println("\n✅ 모든 검증 통과");
    }
}
