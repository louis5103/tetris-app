package seoultech.se.core.engine.item.impl;

import seoultech.se.core.GameState;
import seoultech.se.core.engine.item.AbstractItem;
import seoultech.se.core.engine.item.ItemEffect;
import seoultech.se.core.engine.item.ItemType;
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
        
        int areaBlocks = 0;
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                if (grid[r][c] != null && grid[r][c].isOccupied()) {
                    areaBlocks++;
                }
            }
        }
        
        System.out.println("   - Explosion area: rows " + startRow + "-" + endRow + 
            ", cols " + startCol + "-" + endCol + " (" + areaBlocks + " blocks)");
        
        // ✨ 제거될 셀들의 좌표 수집 (애니메이션용)
        java.util.List<int[]> clearedCells = new java.util.ArrayList<>();
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                if (grid[r][c] != null && grid[r][c].isOccupied()) {
                    clearedCells.add(new int[]{r, c});
                }
            }
        }
        gameState.setItemEffectClearedCells(clearedCells);
        
        // 블록 제거 - 폭발 범위 내의 모든 블록 제거
        for (int r = startRow; r <= endRow; r++) {
            for (int c = startCol; c <= endCol; c++) {
                if (grid[r][c] != null && grid[r][c].isOccupied()) {
                    grid[r][c].clear();
                    blocksCleared++;
                }
            }
        }
        
        int linesCleared = 0;
        
        // 🎮 중력 적용 및 라인 클리어
        if (blocksCleared > 0) {
            linesCleared = applyGravity(gameState);
            System.out.println("   - Gravity applied, " + linesCleared + " line(s) cleared");
        }
        
        int bonusScore = blocksCleared * SCORE_PER_BLOCK;
        
        String message = String.format("💣 Bomb exploded! %d blocks cleared at (%d, %d)", 
            blocksCleared, row, col);
        
        System.out.println("✅ [BombItem] " + message);
        
        // 🔥 FIX: 라인 클리어 수를 ItemEffect에 포함
        return ItemEffect.successWithLines(ItemType.BOMB, blocksCleared, bonusScore, linesCleared, message);
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
            System.out.println("💣 [BombItem] Clearing " + linesToClear.size() + " filled line(s) after BOMB effect");
            
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
