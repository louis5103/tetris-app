package seoultech.se.core.engine.item.impl;

import seoultech.se.core.GameState;
import seoultech.se.core.engine.item.AbstractItem;
import seoultech.se.core.engine.item.ItemEffect;
import seoultech.se.core.engine.item.ItemType;
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
        
        // ✨ 제거될 셀들의 좌표 수집 (애니메이션용)
        java.util.List<int[]> clearedCells = new java.util.ArrayList<>();
        
        // 행 제거 - 지정된 행의 모든 블록 제거
        int rowBlocks = 0;
        for (int c = 0; c < boardWidth; c++) {
            if (grid[row][c] != null && grid[row][c].isOccupied()) {
                rowBlocks++;
                clearedCells.add(new int[]{row, c});
            }
        }
        System.out.println("   - Clearing row " + row + " (" + rowBlocks + " blocks)");
        for (int c = 0; c < boardWidth; c++) {
            if (grid[row][c] != null && grid[row][c].isOccupied()) {
                grid[row][c].clear();
                blocksCleared++;
            }
        }
        
        // 열 제거 (교차점 제외) - 지정된 열의 모든 블록 제거
        int colBlocks = 0;
        for (int r = 0; r < boardHeight; r++) {
            if (r != row && grid[r][col] != null && grid[r][col].isOccupied()) {
                colBlocks++;
                clearedCells.add(new int[]{r, col});
            }
        }
        System.out.println("   - Clearing column " + col + " (" + colBlocks + " blocks, excluding intersection)");
        for (int r = 0; r < boardHeight; r++) {
            if (r != row && grid[r][col] != null && grid[r][col].isOccupied()) {
                grid[r][col].clear();
                blocksCleared++;
            }
        }
        
        // 애니메이션용 좌표 저장
        gameState.setLastClearedCells(clearedCells);
        
        int linesCleared = 0;
        
        // 🎮 중력 적용 및 라인 클리어
        if (blocksCleared > 0) {
            linesCleared = applyGravity(gameState);
            System.out.println("   - Gravity applied, " + linesCleared + " line(s) cleared");
        }
        
        int bonusScore = blocksCleared * SCORE_PER_BLOCK;
        
        String message = String.format("➕ Plus cleared! Row %d and Column %d - %d blocks cleared", 
            row, col, blocksCleared);
        
        System.out.println("✅ [PlusItem] " + message);
        
        // 🔥 FIX: 라인 클리어 수를 ItemEffect에 포함
        return ItemEffect.successWithLines(ItemType.PLUS, blocksCleared, bonusScore, linesCleared, message);
    }
    
    /**
     * 중력 적용: 행 단위로 중력 적용
     * 
     * 1. 빈 행(완전히 비어있는 행)을 찾음
     * 2. 빈 행 위의 모든 행들을 아래로 이동
     * 3. 꽉 찬 행이 있으면 라인 클리어
     * 
     * @param gameState 게임 상태
     * @return 제거된 라인 수
     */
    private int applyGravity(GameState gameState) {
        Cell[][] grid = gameState.getGrid();
        int boardHeight = gameState.getBoardHeight();
        int boardWidth = gameState.getBoardWidth();
        
        boolean changed = true;
        
        // 빈 행이 없을 때까지 반복
        while (changed) {
            changed = false;
            
            // 아래에서 위로 스캔하여 빈 행 찾기
            for (int row = boardHeight - 1; row > 0; row--) {
                // 현재 행이 완전히 비어있는지 확인
                boolean isEmptyRow = true;
                for (int col = 0; col < boardWidth; col++) {
                    if (grid[row][col].isOccupied()) {
                        isEmptyRow = false;
                        break;
                    }
                }
                
                // 현재 행이 비어있고, 위에 블록이 있으면 내림
                if (isEmptyRow) {
                    boolean hasBlockAbove = false;
                    for (int aboveRow = row - 1; aboveRow >= 0; aboveRow--) {
                        for (int col = 0; col < boardWidth; col++) {
                            if (grid[aboveRow][col].isOccupied()) {
                                hasBlockAbove = true;
                                break;
                            }
                        }
                        if (hasBlockAbove) break;
                    }
                    
                    if (hasBlockAbove) {
                        // 위의 모든 행을 한 칸씩 아래로 이동
                        for (int moveRow = row; moveRow > 0; moveRow--) {
                            for (int col = 0; col < boardWidth; col++) {
                                grid[moveRow][col].setColor(grid[moveRow - 1][col].getColor());
                                grid[moveRow][col].setOccupied(grid[moveRow - 1][col].isOccupied());
                                grid[moveRow][col].setItemMarker(grid[moveRow - 1][col].getItemMarker());
                            }
                        }
                        // 맨 위 행 비우기
                        for (int col = 0; col < boardWidth; col++) {
                            grid[0][col].clear();
                        }
                        changed = true;
                        break;  // 다시 처음부터 검사
                    }
                }
            }
        }
        
        // 중력 적용 후 라인 클리어 체크
        return checkAndClearLines(gameState);
    }
    
    /**
     * 라인 클리어 체크 및 처리
     * 
     * @param gameState 게임 상태
     * @return 제거된 줄 수
     */
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
            System.out.println("➕ [PlusItem] Clearing " + linesToClear.size() + " filled line(s) after PLUS effect");
            
            java.util.Set<Integer> rowsToRemove = new java.util.HashSet<>(linesToClear);
            
            // 남아있는 줄들만 수집 (위에서 아래로 순서대로)
            java.util.List<Cell[]> remainingRows = new java.util.ArrayList<>();
            for (int row = 0; row < boardHeight; row++) {
                if (!rowsToRemove.contains(row)) {
                    Cell[] rowCopy = new Cell[boardWidth];
                    for (int col = 0; col < boardWidth; col++) {
                        rowCopy[col] = grid[row][col].copy();
                    }
                    remainingRows.add(rowCopy);
                }
            }
            
            // 보드를 위에서부터 다시 채우기 (빈 줄이 위로 가도록)
            int srcIndex = 0;
            for (int targetRow = linesToClear.size(); targetRow < boardHeight; targetRow++) {
                Cell[] rowData = remainingRows.get(srcIndex++);
                for (int col = 0; col < boardWidth; col++) {
                    grid[targetRow][col].setColor(rowData[col].getColor());
                    grid[targetRow][col].setOccupied(rowData[col].isOccupied());
                    grid[targetRow][col].setItemMarker(rowData[col].getItemMarker());
                }
            }
            
            // 위쪽 줄들을 빈 칸으로 초기화
            for (int row = 0; row < linesToClear.size(); row++) {
                for (int col = 0; col < boardWidth; col++) {
                    grid[row][col].clear();
                }
            }
        }
        
        return linesToClear.size();
    }
    
    /**
     * 라인 클리어 체크 및 처리 (OLD - DEPRECATED)
     * 
     * @deprecated 위의 새로운 checkAndClearLines() 메서드 사용
     * @param gameState 게임 상태
     * @return 제거된 줄 수
     */
    @Deprecated
    @SuppressWarnings("unused")
    private int checkAndClearLinesOld(GameState gameState) {
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
