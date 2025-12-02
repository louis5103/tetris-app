package seoultech.se.core.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.Color;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * 아이템 위치 계산 버그 수정 검증 테스트
 * 
 * 버그 상황:
 * - Plus 아이템을 rotate한 테트로미노로 사용 시
 * - 중심점 계산(center)이 잘못된 위치를 가리킴
 * - Pivot 위치 (16, 9)이지만 center는 (16, 8)로 계산되어
 * - 사용자가 기대한 위치가 아닌 곳에서 Plus 효과가 발동함
 * 
 * 수정 내용:
 * - Center position 계산 대신 item marker가 설정된 블록의 위치 사용
 * - 회전된 테트로미노에서도 정확한 위치에서 아이템 효과 발동
 */
@DisplayName("🐛 아이템 위치 계산 버그 수정 검증")
class ItemPositionCalculationFixTest {
    
    private ArcadeGameEngine engine;
    
    @BeforeEach
    void setUp() {
        GameModeConfig config = GameModeConfig.builder()
            .itemAutoUse(true)
            .linesPerItem(1)
            .enabledItemTypes(java.util.EnumSet.of(
                ItemType.PLUS,
                ItemType.BOMB
            ))
            .build();
        
        engine = new ArcadeGameEngine(config);
    }
    
    @Test
    @DisplayName("🔴 Plus 아이템: Item marker 위치에서 효과 발동 (center 아님)")
    void testPlusItem_AppliesAtMarkerPosition_NotCenter() {
        // Given: 간단한 보드 상태
        GameState state = new GameState(10, 20);
        
        // 하단에 블록 배치
        for (int y = 18; y < 20; y++) {
            for (int x = 0; x < 10; x++) {
                state.getGrid()[y][x].setOccupied(true);
                state.getGrid()[y][x].setColor(Color.GRAY);
            }
        }
        
        // T-block at (16, 5)  
        // T 모양:
        //  XXX
        //   X
        // Blocks at: (15, 4), (15, 5), (15, 6), (16, 5)
        Tetromino tBlock = new Tetromino(TetrominoType.T);
        state.setCurrentTetromino(tBlock);
        state.setCurrentX(5);
        state.setCurrentY(16);
        state.setCurrentItemType(ItemType.PLUS);
        
        // Place blocks
        state.getGrid()[15][4].setOccupied(true);
        state.getGrid()[15][4].setColor(Color.MAGENTA);
        state.getGrid()[15][5].setOccupied(true);
        state.getGrid()[15][5].setColor(Color.MAGENTA);
        state.getGrid()[15][6].setOccupied(true);
        state.getGrid()[15][6].setColor(Color.MAGENTA);
        state.getGrid()[16][5].setOccupied(true);
        state.getGrid()[16][5].setColor(Color.MAGENTA);
        
        // Set marker at pivot (16, 5) 
        state.getGrid()[16][5].setItemMarker(ItemType.PLUS);
        
        System.out.println("\n=== Before Plus (Marker at 16, 5) ===");
        printBoard(state);
        
        // When: lockTetromino()
        GameState result = engine.lockTetromino(state);
        
        System.out.println("\n=== After Plus (should clear row 16 & column 5) ===");
        printBoard(result);
        
        // Then: Plus should apply at marker (16, 5)
        // Row 16 and Column 5 should be cleared
        
        // Verify column 5 is cleared
        for (int y = 0; y < 18; y++) {  // Check above the base blocks
            assertFalse(result.getGrid()[y][5].isOccupied(),
                "Column 5 at Y=" + y + " should be empty after Plus at (16, 5)");
        }
        
        System.out.println("✅ Plus applied at marker position (16, 5)");
    }

    
    private void printBoard(GameState state) {
        System.out.println("   0 1 2 3 4 5 6 7 8 9");
        for (int y = 0; y < 20; y++) {
            System.out.printf("%2d ", y);
            for (int x = 0; x < 10; x++) {
                if (state.getGrid()[y][x].isOccupied()) {
                    if (state.getGrid()[y][x].getItemMarker() != null) {
                        System.out.print("⭐");
                    } else {
                        System.out.print("█ ");
                    }
                } else {
                    System.out.print("· ");
                }
            }
            System.out.println();
        }
    }
    
}

