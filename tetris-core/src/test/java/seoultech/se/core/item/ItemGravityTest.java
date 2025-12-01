package seoultech.se.core.item;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.engine.item.impl.BombItem;
import seoultech.se.core.engine.item.impl.PlusItem;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.enumType.Color;

/**
 * 아이템 중력 및 잔상 제거 테스트
 */
class ItemGravityTest {

    private GameState gameState;
    private PlusItem plusItem;
    private BombItem bombItem;
    private final int WIDTH = 10;
    private final int HEIGHT = 20;

    @BeforeEach
    void setUp() {
        gameState = new GameState(WIDTH, HEIGHT);
        plusItem = new PlusItem();
        bombItem = new BombItem();
    }

    @Test
    void testPlusItemGravity() {
        // 1. 보드 설정: 18행, 19행을 채우고 15행에 블록 배치
        fillRow(18);
        fillRow(19);
        setBlock(15, 4); // (15, 4) 위치에 블록 (5열은 PlusItem이 지우므로 피함)

        // 2. PlusItem 사용 (18, 5) -> 18행과 5열 삭제
        plusItem.apply(gameState, 18, 5);

        // 3. 검증
        Cell[][] grid = gameState.getGrid();

        // 잔상 확인: 원래 15행은 비어있어야 함
        assertFalse(grid[15][4].isOccupied(), "Original block at (15, 4) should be moved or cleared");
        
        // 내려온 블록 확인 (어딘가에는 있어야 함)
        boolean found = false;
        for (int r = 16; r < HEIGHT; r++) {
            if (grid[r][4].isOccupied()) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Block from (15, 4) should have fallen down");
    }

    @Test
    void testBombItemGravityAndGhosts() {
        // 1. 상황: 바닥(19행)에 블록들이 있고, 그 위에 붕 뜬 블록(15행)이 있음
        // 17, 18, 19행 채움
        fillRow(17);
        fillRow(18);
        fillRow(19);
        
        // 15행 4열에 블록
        setBlock(15, 4);
        
        // 2. BombItem 투하 (18, 5)
        // 범위: 16~20행(보드끝), 3~7열
        // 17, 18, 19행의 3~7열이 지워짐 -> 큰 구멍 발생
        bombItem.apply(gameState, 18, 5);
        
        // 3. 검증
        Cell[][] grid = gameState.getGrid();
        
        // 잔상 확인: 15행 4열은 폭발 범위 밖이지만, 아래가 비었으므로 떨어져야 함
        
        assertFalse(grid[15][4].isOccupied(), "Block at (15, 4) should fall (no ghost)");
        
        // 바닥 근처에 떨어졌는지 확인
        assertTrue(grid[19][4].isOccupied() || grid[18][4].isOccupied(), "Block should accumulate at the bottom");
    }

    private void fillRow(int row) {
        for (int c = 0; c < WIDTH; c++) {
            gameState.getGrid()[row][c] = Cell.of(Color.GRAY, true);
        }
    }

    private void setBlock(int row, int col) {
        gameState.getGrid()[row][col] = Cell.of(Color.RED, true);
    }
}
