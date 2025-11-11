package seoultech.se.core.item.impl;

import seoultech.se.core.GameState;
import seoultech.se.core.item.AbstractItem;
import seoultech.se.core.item.ItemEffect;
import seoultech.se.core.item.ItemType;
import seoultech.se.core.model.Cell;

/**
 * 폭탄 아이템
 * 
 * 아이템 위치 기준 반경 2칸 (5x5 영역)의 블록을 제거합니다.
 * 
 * 효과:
 * - 중심점 (row, col)을 기준으로 상하좌우 각 2칸씩 총 5x5 영역 제거
 * - 제거된 블록 수만큼 점수 부여
 * 
 * 사용 예시:
 * - 블록이 쌓여 위험한 상황에서 긴급 탈출용
 * - 보드 중앙 정리에 효과적
 */
public class BombItem extends AbstractItem {
    
    /**
     * 폭발 반경 (기본: 2)
     */
    private static final int EXPLOSION_RADIUS = 2;
    
    /**
     * 블록당 점수
     */
    private static final int SCORE_PER_BLOCK = 5;
    
    /**
     * 생성자
     */
    public BombItem() {
        super(ItemType.BOMB);
    }
    
    /**
     * 폭탄 효과 적용
     * 
     * @param gameState 게임 상태
     * @param row 중심 행
     * @param col 중심 열
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
            System.err.println("⚠️ [BombItem] Invalid position: (" + row + ", " + col + ")");
            System.err.println("   - Board size: " + boardHeight + "x" + boardWidth);
            return ItemEffect.none();
        }
        
        int blocksCleared = 0;
        
        System.out.println("💣 [BombItem] Applying BOMB effect at (" + row + ", " + col + ")");
        System.out.println("   - Board size: " + boardHeight + "x" + boardWidth);
        
        // 5x5 영역 제거 (중심 기준 상하좌우 각 2칸)
        int startRow = Math.max(0, row - EXPLOSION_RADIUS);
        int endRow = Math.min(boardHeight - 1, row + EXPLOSION_RADIUS);
        int startCol = Math.max(0, col - EXPLOSION_RADIUS);
        int endCol = Math.min(boardWidth - 1, col + EXPLOSION_RADIUS);
        
        System.out.println("   - Explosion area: rows " + startRow + "-" + endRow + 
            ", cols " + startCol + "-" + endCol);
        
        // 블록 제거
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                if (grid[r][c] != null && grid[r][c].isOccupied()) {
                    grid[r][c].clear();
                    blocksCleared++;
                }
            }
        }
        
        int bonusScore = blocksCleared * SCORE_PER_BLOCK;
        
        String message = String.format("💣 Bomb exploded! %d blocks cleared at (%d, %d)", 
            blocksCleared, row, col);
        
        System.out.println("✅ [BombItem] " + message);
        
        // 블록 제거 후 중력 적용
        if (blocksCleared > 0) {
            applyGravity(gameState);
            System.out.println("   - Gravity applied after explosion");
        }
        
        return ItemEffect.success(ItemType.BOMB, blocksCleared, bonusScore, message);
    }
    
    /**
     * 중력 적용: 빈 공간 위의 블록을 아래로 떨어뜨림
     * 
     * @param gameState 게임 상태
     */
    private void applyGravity(GameState gameState) {
        Cell[][] grid = gameState.getGrid();
        int boardHeight = gameState.getBoardHeight();
        int boardWidth = gameState.getBoardWidth();
        
        // 각 열에 대해 아래에서 위로 스캔하여 블록을 아래로 이동
        for (int col = 0; col < boardWidth; col++) {
            int writeRow = boardHeight - 1;  // 쓰기 위치 (아래에서 시작)
            
            // 아래에서 위로 스캔
            for (int readRow = boardHeight - 1; readRow >= 0; readRow--) {
                if (grid[readRow][col] != null && grid[readRow][col].isOccupied()) {
                    // 블록을 발견하면 쓰기 위치로 이동
                    if (readRow != writeRow) {
                        // 블록 복사
                        grid[writeRow][col].setColor(grid[readRow][col].getColor());
                        grid[writeRow][col].setOccupied(true);
                        grid[writeRow][col].setItemMarker(grid[readRow][col].getItemMarker());
                        
                        // 원래 위치 비우기
                        grid[readRow][col].clear();
                    }
                    writeRow--;  // 다음 쓰기 위치는 한 칸 위로
                }
            }
        }
    }
}
