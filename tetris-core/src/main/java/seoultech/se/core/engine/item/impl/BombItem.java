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
     * 중력 적용: 빈 공간 위의 블록을 아래로 떨어뜨림
     * 
     * 블록 제거 아이템(BOMB, PLUS) 사용 시 위의 블록이 아래로 떨어지도록 함
     * 자연스러운 게임 경험 제공
     * 
     * @param gameState 게임 상태
     * @return 중력 적용 후 새로 채워진 라인 수
     */
    private int applyGravity(GameState gameState) {
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
            
            // 남은 위쪽 줄들을 빈 칸으로 초기화 (잔상 제거)
            while (writeRow >= 0) {
                grid[writeRow][col].clear();
                writeRow--;
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
            System.out.println("💣 [BombItem] Clearing " + linesToClear.size() + " filled line(s) after gravity");
            
            // 🔥 FIX: 제거할 줄들을 Set으로 변환하여 한번에 처리
            java.util.Set<Integer> rowsToRemove = new java.util.HashSet<>(linesToClear);
            
            // 남아있는 줄들만 수집 (아래에서 위로)
            java.util.List<Cell[]> remainingRows = new java.util.ArrayList<>();
            for (int row = boardHeight - 1; row >= 0; row--) {
                if (!rowsToRemove.contains(row)) {
                    Cell[] rowCopy = new Cell[boardWidth];
                    for (int col = 0; col < boardWidth; col++) {
                        rowCopy[col] = grid[row][col].copy();
                    }
                    remainingRows.add(rowCopy);
                }
            }
            
            // 보드를 아래에서부터 다시 채우기
            int targetRow = boardHeight - 1;
            for (Cell[] rowData : remainingRows) {
                for (int col = 0; col < boardWidth; col++) {
                    grid[targetRow][col].setColor(rowData[col].getColor());
                    grid[targetRow][col].setOccupied(rowData[col].isOccupied());
                    grid[targetRow][col].setItemMarker(rowData[col].getItemMarker());
                }
                targetRow--;
            }
            
            // 남은 위쪽 줄들을 빈 칸으로 초기화
            while (targetRow >= 0) {
                for (int col = 0; col < boardWidth; col++) {
                    grid[targetRow][col].clear();
                }
                targetRow--;
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
