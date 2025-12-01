package seoultech.se.core.engine.item.impl;

import seoultech.se.core.GameState;
import seoultech.se.core.engine.item.AbstractItem;
import seoultech.se.core.engine.item.ItemEffect;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.Cell;

/**
 * 줄 삭제 아이템 ('L')
 * 
 * Req2 필수 아이템 #1
 * 
 * 명세:
 * - 표시: 문자 'L'로 표시
 * - 형태: 기본 블록 내에 포함 (예: I형 → 'OOOL', 'OOLO', 'OLOO', 'LOOO')
 * - 위치: 블록 내 무작위
 * 
 * 효과:
 * - 블록이 고정되면 'L'이 위치한 줄을 즉시 삭제
 * - 해당 줄이 꽉 차있지 않아도 삭제됨
 * - 삭제된 줄에 대해서도 기존 방식대로 점수 계산
 * 
 * 구현 방식:
 * - Cell에 itemMarker로 저장
 * - 블록 고정 시 ArcadeGameEngine에서 'L' 마커가 있는 줄 감지 후 삭제
 * 
 * Phase 3 구현
 */
public class LineClearItem extends AbstractItem {
    
    /**
     * 블록당 점수
     */
    private static final int SCORE_PER_BLOCK = 10;
    
    /**
     * 생성자
     */
    public LineClearItem() {
        super(ItemType.LINE_CLEAR);
    }
    
    /**
     * 줄 삭제 효과 적용
     * 
     * 'L' 마커가 있는 모든 줄을 찾아서 삭제합니다.
     * 
     * @param gameState 게임 상태
     * @param row 사용하지 않음 (모든 'L' 마커 줄을 찾음)
     * @param col 사용하지 않음
     * @return 아이템 효과
     */
    @Override
    public ItemEffect apply(GameState gameState, int row, int col) {
        if (!isEnabled()) {
            return ItemEffect.none();
        }
        
        System.out.println("Ⓛ [LineClearItem] Applying LINE_CLEAR effect - searching for 'L' markers");
        
        // 'L' 마커가 있는 줄 찾기
        java.util.List<Integer> markedLines = findAndClearMarkedLines(gameState);
        
        if (markedLines.isEmpty()) {
            System.out.println("⚠️ [LineClearItem] No 'L' markers found!");
            return ItemEffect.none();
        }
        
        // 줄 삭제 및 중력 적용
        int blocksCleared = clearLines(gameState, markedLines);
        
        // 점수 계산
        int bonusScore = blocksCleared * SCORE_PER_BLOCK;
        
        String message = String.format("Ⓛ %d line(s) cleared by 'L' markers! %d blocks removed", 
            markedLines.size(), blocksCleared);
        
        System.out.println("✅ [LineClearItem] " + message);
        
        return ItemEffect.success(ItemType.LINE_CLEAR, blocksCleared, bonusScore, message);
    }
    
    /**
     * 'L' 마커가 있는 줄을 찾아서 삭제하는 유틸리티 메서드
     * 
     * ArcadeGameEngine에서 사용됩니다.
     * 
     * @param gameState 게임 상태
     * @return 삭제된 줄 번호들
     */
    public static java.util.List<Integer> findAndClearMarkedLines(GameState gameState) {
        java.util.List<Integer> clearedRows = new java.util.ArrayList<>();
        Cell[][] grid = gameState.getGrid();
        int boardHeight = gameState.getBoardHeight();
        int boardWidth = gameState.getBoardWidth();
        
        // 'L' 마커가 있는 줄 찾기
        for (int row = 0; row < boardHeight; row++) {
            boolean hasMarker = false;
            int occupiedCount = 0;
            
            for (int col = 0; col < boardWidth; col++) {
                if (grid[row][col].isOccupied()) {
                    occupiedCount++;
                }
                if (grid[row][col].hasItemMarker() && 
                    grid[row][col].getItemMarker() == ItemType.LINE_CLEAR) {
                    hasMarker = true;
                }
            }
            
            if (hasMarker) {
                clearedRows.add(row);
                System.out.println("Ⓛ [LineClearItem] Found 'L' marker at row " + row + 
                    " (" + occupiedCount + "/" + boardWidth + " occupied)");
            }
        }
        
        if (!clearedRows.isEmpty()) {
            System.out.println("Ⓛ [LineClearItem] Total rows with 'L' markers: " + clearedRows);
        } else {
            System.out.println("Ⓛ [LineClearItem] No 'L' markers found in any row");
        }
        
        return clearedRows;
    }
    
    /**
     * 지정된 줄들을 삭제하고 위의 블록들을 내립니다
     * 
     * @param gameState 게임 상태
     * @param rowsToRemove 삭제할 줄 번호들 (정렬 필요 없음)
     * @return 삭제된 블록 수
     */
    public static int clearLines(GameState gameState, java.util.List<Integer> rowsToRemove) {
        if (rowsToRemove == null || rowsToRemove.isEmpty()) {
            return 0;
        }
        
        Cell[][] grid = gameState.getGrid();
        int boardHeight = gameState.getBoardHeight();
        int boardWidth = gameState.getBoardWidth();
        int totalBlocksCleared = 0;
        
        // 삭제할 줄들을 Set으로 변환 (O(1) 조회)
        java.util.Set<Integer> rowsSet = new java.util.HashSet<>(rowsToRemove);
        
        // 디버그: 삭제 전 보드 상태 출력
        System.out.println("Ⓛ [LineClearItem] 🔍 BEFORE CLEAR - Board state (rows with blocks):");
        for (int row = 0; row < boardHeight; row++) {
            int rowBlockCount = 0;
            StringBuilder rowStr = new StringBuilder();
            for (int col = 0; col < boardWidth; col++) {
                if (grid[row][col].isOccupied()) {
                    rowBlockCount++;
                    rowStr.append("█");
                } else {
                    rowStr.append("·");
                }
            }
            if (rowBlockCount > 0 || rowsSet.contains(row)) {
                String marker = rowsSet.contains(row) ? " ← TO BE CLEARED" : "";
                System.out.println("Ⓛ   Row " + String.format("%2d", row) + ": " + rowStr + " (" + rowBlockCount + " blocks)" + marker);
            }
        }
        
        // 블록 수 계산 및 디버그 로그
        for (int row : rowsToRemove) {
            int rowBlockCount = 0;
            for (int col = 0; col < boardWidth; col++) {
                if (grid[row][col].isOccupied()) {
                    totalBlocksCleared++;
                    rowBlockCount++;
                }
            }
            System.out.println("Ⓛ [LineClearItem] Row " + row + " has " + rowBlockCount + 
                " occupied blocks (will clear entire row)");
        }
        
        // 남아있는 줄들만 수집 (아래에서 위로)
        java.util.List<Cell[]> remainingRows = new java.util.ArrayList<>();
        for (int row = boardHeight - 1; row >= 0; row--) {
            if (!rowsSet.contains(row)) {
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
                // 🔥 FIX: 셀 값을 복사 (참조가 아닌 값 복사)
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
        
        System.out.println("✅ [LineClearItem] Cleared " + rowsToRemove.size() + 
            " line(s), removed " + totalBlocksCleared + " blocks");
        
        // 디버그: 삭제 후 보드 상태 출력
        System.out.println("Ⓛ [LineClearItem] 🔍 AFTER CLEAR - Board state (rows with blocks):");
        for (int row = 0; row < boardHeight; row++) {
            int rowBlockCount = 0;
            StringBuilder rowStr = new StringBuilder();
            for (int col = 0; col < boardWidth; col++) {
                if (grid[row][col].isOccupied()) {
                    rowBlockCount++;
                    rowStr.append("█");
                } else {
                    rowStr.append("·");
                }
            }
            if (rowBlockCount > 0) {
                System.out.println("Ⓛ   Row " + String.format("%2d", row) + ": " + rowStr + " (" + rowBlockCount + " blocks)");
            }
        }
        
        return totalBlocksCleared;
    }
}
