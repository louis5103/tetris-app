package seoultech.se.core.item.impl;

import seoultech.se.core.GameState;
import seoultech.se.core.item.AbstractItem;
import seoultech.se.core.item.ItemEffect;
import seoultech.se.core.item.ItemType;
import seoultech.se.core.model.Cell;

/**
 * 십자(Plus) 아이템
 * 
 * 아이템 위치의 행(row)과 열(column) 전체를 제거합니다.
 * 
 * 효과:
 * - 지정된 행의 모든 블록 제거
 * - 지정된 열의 모든 블록 제거
 * - 중복되는 교차점은 한 번만 계산
 * 
 * 사용 예시:
 * - 한 줄이 거의 채워진 상황에서 라인 클리어 대신 사용
 * - 특정 열이 높게 쌓인 경우 정리용
 */
public class PlusItem extends AbstractItem {
    
    /**
     * 블록당 점수
     */
    private static final int SCORE_PER_BLOCK = 5;
    
    /**
     * 생성자
     */
    public PlusItem() {
        super(ItemType.PLUS);
    }
    
    /**
     * 십자 효과 적용
     * 
     * @param gameState 게임 상태
     * @param row 제거할 행
     * @param col 제거할 열
     * @return 아이템 효과
     */
    @Override
    public ItemEffect apply(GameState gameState, int row, int col) {
        if (!isEnabled()) {
            return ItemEffect.none();
        }
        
        Cell[][] grid = gameState.getGrid();
        int boardHeight = gameState.getBoardHeight();
        int boardWidth = gameState.getBoardWidth();
        
        // 경계 체크
        if (row < 0 || row >= boardHeight || col < 0 || col >= boardWidth) {
            System.err.println("⚠️ [PlusItem] Invalid position: (" + row + ", " + col + ")");
            System.err.println("   - Board size: " + boardHeight + "x" + boardWidth);
            return ItemEffect.none();
        }
        
        int blocksCleared = 0;
        
        System.out.println("➕ [PlusItem] Applying PLUS effect at (" + row + ", " + col + ")");
        System.out.println("   - Board size: " + boardHeight + "x" + boardWidth);
        
        // 행 제거
        System.out.println("   - Clearing row " + row);
        for (int c = 0; c < boardWidth; c++) {
            if (grid[row][c] != null && grid[row][c].isOccupied()) {
                System.out.println("     * Clearing block at (" + row + ", " + c + ")");
                grid[row][c].clear();
                blocksCleared++;
            }
        }
        
        // 열 제거 (교차점 제외)
        System.out.println("   - Clearing column " + col);
        for (int r = 0; r < boardHeight; r++) {
            if (r != row && grid[r][col] != null && grid[r][col].isOccupied()) {
                System.out.println("     * Clearing block at (" + r + ", " + col + ")");
                grid[r][col].clear();
                blocksCleared++;
            }
        }
        
        int bonusScore = blocksCleared * SCORE_PER_BLOCK;
        
        String message = String.format("➕ Plus cleared! Row %d and Column %d - %d blocks cleared", 
            row, col, blocksCleared);
        
        System.out.println("✅ [PlusItem] " + message);
        
        return ItemEffect.success(ItemType.PLUS, blocksCleared, bonusScore, message);
    }
}
