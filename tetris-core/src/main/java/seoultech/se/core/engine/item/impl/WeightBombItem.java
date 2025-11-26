package seoultech.se.core.engine.item.impl;

import seoultech.se.core.GameState;
import seoultech.se.core.engine.item.AbstractItem;
import seoultech.se.core.engine.item.ItemEffect;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * 무게추 아이템 ('W')
 * 
 * Req2 필수 아이템 #2
 * 
 * 명세:
 * - 표시: 문자 'W'로 표시
 * - 형태: 4칸 너비의 특수 블록 (WEIGHT_BOMB TetrominoType)
 * - 초기: 좌우 이동 가능
 * - 바닥/블록 접촉 후: 좌우 이동 불가, 아래로만 이동
 * 
 * 효과:
 * - 떨어지면서 아래에 있는 모든 블록 제거
 * - 무게추가 지나간 경로의 모든 블록이 사라짐
 * - 점수: 제거된 블록당 10점
 * 
 * 구현 방식:
 * - TetrominoType.WEIGHT_BOMB 사용
 * - GameState.isWeightBombLocked 플래그로 상태 관리
 * - 매 프레임마다 processWeightBombFall() 호출하여 블록 제거
 * 
 * Phase 4 구현
 */
public class WeightBombItem extends AbstractItem {
    
    /**
     * 블록당 점수
     */
    private static final int SCORE_PER_BLOCK = 10;
    
    /**
     * 생성자
     */
    public WeightBombItem() {
        super(ItemType.WEIGHT_BOMB);
    }
    
    /**
     * 무게추 효과 적용
     * 
     * 주의: 이 메서드는 일반적으로 직접 호출되지 않습니다.
     * 무게추는 ArcadeGameEngine에서 자동으로 처리됩니다.
     * 
     * 이 메서드는 일관성과 테스트 목적으로 구현되었습니다.
     * 
     * @param gameState 게임 상태
     * @param row 무게추의 Y 좌표
     * @param col 무게추의 X 좌표
     * @return 아이템 효과
     */
    @Override
    public ItemEffect apply(GameState gameState, int row, int col) {
        if (!isEnabled()) {
            return ItemEffect.none();
        }
        
        // 무게추는 실시간으로 처리되므로 이 메서드는 사용되지 않음
        System.out.println("⚓ [WeightBombItem] apply() called - use processWeightBombFall() instead");
        return ItemEffect.none();
    }
    
    /**
     * 무게추가 떨어지면서 블록을 제거하는 메인 로직
     * 
     * 무게추의 4칸 모두에서 아래에 있는 블록을 제거합니다.
     * ArcadeGameEngine에서 매 프레임마다 호출됩니다.
     * 
     * @param gameState 게임 상태
     * @return 제거된 블록 수
     */
    public static int processWeightBombFall(GameState gameState) {
        // 현재 블록이 무게추가 아니면 무시
        if (gameState.getCurrentTetromino() == null || 
            gameState.getCurrentTetromino().getType() != TetrominoType.WEIGHT_BOMB) {
            return 0;
        }
        
        Cell[][] grid = gameState.getGrid();
        int[][] shape = gameState.getCurrentTetromino().getCurrentShape();
        int pivotX = gameState.getCurrentTetromino().getPivotX();
        int pivotY = gameState.getCurrentTetromino().getPivotY();
        int currentX = gameState.getCurrentX();
        int currentY = gameState.getCurrentY();
        int boardHeight = gameState.getBoardHeight();
        int boardWidth = gameState.getBoardWidth();
        
        int blocksCleared = 0;
        
        // 무게추의 각 칸에 대해 아래에 있는 블록 제거
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    // 절대 좌표 계산
                    int absX = currentX + (col - pivotX);
                    int belowY = currentY + (row - pivotY) + 1;  // 바로 아래 칸
                    
                    // 경계 체크
                    if (absX < 0 || absX >= boardWidth || belowY < 0 || belowY >= boardHeight) {
                        continue;
                    }
                    
                    // 아래에 블록이 있으면 제거
                    if (grid[belowY][absX].isOccupied()) {
                        grid[belowY][absX].clear();
                        blocksCleared++;
                    }
                }
            }
        }
        
        if (blocksCleared > 0) {
            System.out.println("⚓ [WeightBombItem] Cleared " + blocksCleared + " block(s) during fall");
        }
        
        return blocksCleared;
    }
    
    /**
     * 무게추가 고정될 때 최종 블록 제거
     * 
     * 무게추가 고정되는 위치의 아래에 있는 모든 블록을 수직으로 제거합니다.
     * 
     * @param gameState 게임 상태
     * @param weightBombX 무게추의 X 좌표 배열 (4칸)
     * @param weightBombY 무게추의 Y 좌표
     * @return 제거된 블록 수
     */
    public static int clearVerticalPath(GameState gameState, int[] weightBombX, int weightBombY) {
        Cell[][] grid = gameState.getGrid();
        int boardHeight = gameState.getBoardHeight();
        int boardWidth = gameState.getBoardWidth();
        int blocksCleared = 0;
        
        System.out.println("⚓ [WeightBombItem] Clearing vertical path at Y=" + weightBombY);
        
        // 무게추의 각 X 좌표에 대해 아래의 모든 블록 제거
        for (int x : weightBombX) {
            if (x < 0 || x >= boardWidth) {
                continue;
            }
            
            // 무게추 위치부터 바닥까지 모든 블록 제거
            for (int y = weightBombY + 1; y < boardHeight; y++) {
                if (grid[y][x].isOccupied()) {
                    grid[y][x].clear();
                    blocksCleared++;
                }
            }
        }
        
        System.out.println("⚓ [WeightBombItem] Cleared " + blocksCleared + " block(s) in vertical path");
        
        return blocksCleared;
    }
    
    /**
     * 무게추의 X 좌표 배열 계산
     * 
     * @param gameState 게임 상태
     * @return X 좌표 배열 (4칸)
     */
    public static int[] getWeightBombXPositions(GameState gameState) {
        if (gameState.getCurrentTetromino() == null || 
            gameState.getCurrentTetromino().getType() != TetrominoType.WEIGHT_BOMB) {
            return new int[0];
        }
        
        int[][] shape = gameState.getCurrentTetromino().getCurrentShape();
        int pivotX = gameState.getCurrentTetromino().getPivotX();
        int currentX = gameState.getCurrentX();
        
        java.util.List<Integer> xPositions = new java.util.ArrayList<>();
        
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    int absX = currentX + (col - pivotX);
                    xPositions.add(absX);
                }
            }
        }
        
        return xPositions.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * 무게추가 현재 활성화되어 있는지 확인
     * 
     * @param gameState 게임 상태
     * @return 무게추가 활성화되어 있으면 true
     */
    public static boolean isWeightBombActive(GameState gameState) {
        return gameState.getCurrentTetromino() != null && 
               gameState.getCurrentTetromino().getType() == TetrominoType.WEIGHT_BOMB &&
               gameState.getCurrentItemType() == ItemType.WEIGHT_BOMB;
    }
}
