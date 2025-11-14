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
        
        // 🎮 GAME UX: 중력 적용 (라인 클리어는 제거)
        // 십자 영역 삭제 후 위의 블록이 아래로 떨어지도록 하여 자연스러운 게임 경험 제공
        // 단, 라인 클리어는 하지 않음 (연쇄 효과 방지, 예측 가능성 확보)
        if (blocksCleared > 0) {
            applyGravity(gameState);
            System.out.println("   - Gravity applied (no line clear)");
        }
        
        return ItemEffect.success(ItemType.PLUS, blocksCleared, bonusScore, message);
    }
    
    /**
     * 중력 적용: 빈 공간 위의 블록을 아래로 떨어뜨림
     * 
     * 블록 제거 아이템(BOMB, PLUS) 사용 시 위의 블록이 아래로 떨어지도록 함
     * 자연스러운 게임 경험 제공
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
    
    /**
     * 라인 클리어 체크 및 처리
     * 
     * ⚠️ 현재 사용하지 않음 (QA 버그 수정으로 제거됨)
     * 기획 팀 결정 후 복원 가능
     * 
     * @param gameState 게임 상태
     * @return 제거된 줄 수
     */
    @SuppressWarnings("unused")
    private int checkAndClearLines(GameState gameState) {
        Cell[][] grid = gameState.getGrid();
        int boardHeight = gameState.getBoardHeight();
        int boardWidth = gameState.getBoardWidth();
        
        java.util.List<Integer> linesToClear = new java.util.ArrayList<>();
        
        // 꽉 찬 줄 찾기
        for (int row = 0; row < boardHeight; row++) {
            boolean isFullLine = true;
            
            for (int col = 0; col < boardWidth; col++) {
                if (grid[row][col] == null || !grid[row][col].isOccupied()) {
                    isFullLine = false;
                    break;
                }
            }
            
            if (isFullLine) {
                linesToClear.add(row);
            }
        }
        
        // 줄 제거 및 위의 블록 내리기
        if (!linesToClear.isEmpty()) {
            // 아래에서 위로 줄 제거
            for (int lineIndex = linesToClear.size() - 1; lineIndex >= 0; lineIndex--) {
                int rowToRemove = linesToClear.get(lineIndex);
                
                // 해당 줄 위의 모든 줄을 한 칸씩 내림
                for (int row = rowToRemove; row > 0; row--) {
                    for (int col = 0; col < boardWidth; col++) {
                        if (grid[row - 1][col] != null) {
                            grid[row][col].setColor(grid[row - 1][col].getColor());
                            grid[row][col].setOccupied(grid[row - 1][col].isOccupied());
                            grid[row][col].setItemMarker(grid[row - 1][col].getItemMarker());
                        }
                    }
                }
                
                // 최상단 줄 초기화
                for (int col = 0; col < boardWidth; col++) {
                    grid[0][col].clear();
                }
            }
        }
        
        return linesToClear.size();
    }
}
