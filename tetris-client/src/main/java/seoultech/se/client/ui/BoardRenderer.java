package seoultech.se.client.ui;

import javafx.application.Platform;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import seoultech.se.client.constants.ColorBlindMode;
import seoultech.se.client.constants.UIConstants;
import seoultech.se.client.util.ColorMapper;
import seoultech.se.core.GameState;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * 테트리스 게임 보드의 렌더링을 담당하는 클래스
 * 
 * 이 클래스는 다음과 같은 렌더링 작업을 수행합니다:
 * - 보드 셀 업데이트
 * - 현재 테트로미노 그리기
 * - Hold 영역 테트로미노 그리기
 * - Next 영역 테트로미노 그리기
 * 
 * GameController에서 렌더링 책임을 분리하여
 * 단일 책임 원칙(SRP)을 준수합니다.
 */
public class BoardRenderer {
    
    private final Rectangle[][] cellRectangles;
    private final Rectangle[][] holdCellRectangles;
    private final Rectangle[][] nextCellRectangles;
    private ColorBlindMode currentColorBlindMode = ColorBlindMode.NORMAL;
    
    /**
     * BoardRenderer 생성자
     * 
     * @param cellRectangles 메인 보드의 Rectangle 배열
     * @param holdCellRectangles Hold 영역의 Rectangle 배열
     * @param nextCellRectangles Next 영역의 Rectangle 배열
     */
    public BoardRenderer(
            Rectangle[][] cellRectangles,
            Rectangle[][] holdCellRectangles,
            Rectangle[][] nextCellRectangles) {
        
        this.cellRectangles = cellRectangles;
        this.holdCellRectangles = holdCellRectangles;
        this.nextCellRectangles = nextCellRectangles;
    }

    public BoardRenderer(
        Rectangle[][] cellRectangles,
        Rectangle[][] holdCellRectangles,
        Rectangle[][] nextCellRectangles,
        ColorBlindMode initialMode) {
            
        this.cellRectangles = cellRectangles;
        this.holdCellRectangles = holdCellRectangles;
        this.nextCellRectangles = nextCellRectangles;
        this.currentColorBlindMode = initialMode;
    }

    public void setColorBlindMode(ColorBlindMode mode) {
        this.currentColorBlindMode = mode;
    }
    
    /**
     * 특정 셀의 Rectangle을 업데이트합니다
     * 
     * @param row 행 인덱스
     * @param col 열 인덱스
     * @param cell 셀 데이터
     */
    public void updateCell(int row, int col, Cell cell) {
        Platform.runLater(() -> {
            Rectangle rect = cellRectangles[row][col];
            
            if (cell.isOccupied()) {
                rect.setFill(ColorMapper.toJavaFXColor(cell.getColor()));
                String colorClass = ColorMapper.toCssClass(cell.getColor(), currentColorBlindMode);
                rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
                if (colorClass != null) {
                    rect.getStyleClass().add(colorClass);
                }
            } else {
                rect.setFill(ColorMapper.getEmptyCellColor());
                rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
            }
        });
    }
    
    /**
     * 현재 테트로미노를 포함한 전체 보드를 다시 그립니다
     * 
     * @param gameState 현재 게임 상태
     */
    public void drawBoard(GameState gameState) {
        Platform.runLater(() -> {
            // 전체 보드를 먼저 그립니다
            Cell[][] grid = gameState.getGrid();
            for (int row = 0; row < gameState.getBoardHeight(); row++) {
                for (int col = 0; col < gameState.getBoardWidth(); col++) {
                    updateCellInternal(row, col, grid[row][col]);
                }
            }
            
            // 현재 테트로미노가 있으면 그 위에 그립니다
            if (gameState.getCurrentTetromino() != null) {
                drawCurrentTetromino(gameState);
            }
        });
    }
    
    /**
     * 현재 테트로미노를 보드 위에 그립니다
     * 
     * @param gameState 현재 게임 상태
     */
    private void drawCurrentTetromino(GameState gameState) {
        Tetromino tetromino = gameState.getCurrentTetromino();
        if (tetromino == null) {
            return;
        }
        
        int[][] shape = tetromino.getCurrentShape();
        int pivotX = tetromino.getPivotX();
        int pivotY = tetromino.getPivotY();
        seoultech.se.core.model.enumType.Color color = tetromino.getColor();
        
        // 아이템 블록 여부 확인
        boolean isItemBlock = gameState.getCurrentItemType() != null;
        seoultech.se.core.item.ItemType itemType = gameState.getCurrentItemType();
        
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[0].length; col++) {
                if (shape[row][col] == 1) {
                    int absoluteX = gameState.getCurrentX() + (col - pivotX);
                    int absoluteY = gameState.getCurrentY() + (row - pivotY);
                    
                    if (absoluteY >= 0 && absoluteY < gameState.getBoardHeight() &&
                        absoluteX >= 0 && absoluteX < gameState.getBoardWidth()) {
                        
                        Rectangle rect = cellRectangles[absoluteY][absoluteX];
                        
                        String colorClass = ColorMapper.toCssClass(color, currentColorBlindMode);
                        rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
                        if (colorClass != null) {
                            rect.getStyleClass().add(colorClass);
                        // 아이템 블록인 경우 특별한 스타일 적용
                        if (isItemBlock) {
                            applyItemBlockStyle(rect, itemType);
                        } else {
                            // 일반 블록
                            rect.setFill(ColorMapper.toJavaFXColor(color));
                            
                            String colorClass = ColorMapper.toCssClass(color);
                            rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
                            rect.getStyleClass().removeAll("range-bomb-block", "cross-bomb-block", "line-clear-block", "selectable-block");
                            if (colorClass != null) {
                                rect.getStyleClass().add(colorClass);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 아이템 블록에 특별한 스타일 적용
     * 
     * @param rect 대상 Rectangle
     * @param itemType 아이템 타입
     */
    private void applyItemBlockStyle(Rectangle rect, seoultech.se.core.item.ItemType itemType) {
        // 모든 기존 스타일 제거
        rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
        rect.getStyleClass().removeAll("range-bomb-block", "cross-bomb-block", "line-clear-block", "selectable-block");
        
        // 아이템 타입에 따라 다른 스타일 적용
        if (itemType != null) {
            switch (itemType) {
                case BOMB:
                    rect.getStyleClass().add("range-bomb-block");
                    break;
                case PLUS:
                    rect.getStyleClass().add("cross-bomb-block");
                    break;
                case SPEED_RESET:
                case BONUS_SCORE:
                    // 무지개 효과
                    rect.getStyleClass().add("selectable-block");
                    break;
                default:
                    rect.setFill(Color.GOLD);
                    break;
            }
        }
    }
    
    /**
     * 내부용 셀 업데이트 메서드 (Platform.runLater 없음)
     */
    private void updateCellInternal(int row, int col, Cell cell) {
        Rectangle rect = cellRectangles[row][col];
        
        if (cell.isOccupied()) {
            rect.setFill(ColorMapper.toJavaFXColor(cell.getColor()));
            String colorClass = ColorMapper.toCssClass(cell.getColor(), currentColorBlindMode);
            rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
            if (colorClass != null) {
                rect.getStyleClass().add(colorClass);
            }
        } else {
            rect.setFill(ColorMapper.getEmptyCellColor());
            rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
        }
    }
    
    /**
     * Hold 영역에 테트로미노를 그립니다
     * 
     * @param type 테트로미노 타입 (null이면 비움)
     */
    public void drawHoldPiece(TetrominoType type) {
        Platform.runLater(() -> {
            // 모든 셀 초기화
            clearPreviewGrid(holdCellRectangles);
            
            if (type != null) {
                drawPreviewPiece(holdCellRectangles, type);
            }
        });
    }
    
    /**
     * Next 영역에 테트로미노를 그립니다
     * 
     * @param type 테트로미노 타입 (null이면 비움)
     */
    public void drawNextPiece(TetrominoType type) {
        Platform.runLater(() -> {
            // 모든 셀 초기화
            clearPreviewGrid(nextCellRectangles);
            
            if (type != null) {
                drawPreviewPiece(nextCellRectangles, type);
            }
        });
    }
    
    /**
     * 미리보기 그리드를 비웁니다
     * 
     * @param grid 비울 Rectangle 배열
     */
    private void clearPreviewGrid(Rectangle[][] grid) {
        for (int row = 0; row < UIConstants.PREVIEW_GRID_ROWS; row++) {
            for (int col = 0; col < UIConstants.PREVIEW_GRID_COLS; col++) {
                grid[row][col].setFill(ColorMapper.getEmptyCellColor());
            }
        }
    }
    
    /**
     * 미리보기 그리드에 테트로미노를 그립니다
     * 
     * @param grid 그릴 Rectangle 배열
     * @param type 테트로미노 타입
     */
    private void drawPreviewPiece(Rectangle[][] grid, TetrominoType type) {
        int[][] shape = type.shape;
        Color color = ColorMapper.toJavaFXColor(type.color);
        
        int offsetX = (UIConstants.PREVIEW_GRID_COLS - shape[0].length) / 2;
        int offsetY = (UIConstants.PREVIEW_GRID_ROWS - shape.length) / 2;
        
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    int gridRow = row + offsetY;
                    int gridCol = col + offsetX;
                    if (gridRow >= 0 && gridRow < UIConstants.PREVIEW_GRID_ROWS && 
                        gridCol >= 0 && gridCol < UIConstants.PREVIEW_GRID_COLS) {
                    
                        grid[gridRow][gridCol].setFill(color);
                        
                        String colorClass = ColorMapper.toCssClass(type.color, currentColorBlindMode);
                        grid[gridRow][gridCol].getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
                        if (colorClass != null) {
                            grid[gridRow][gridCol].getStyleClass().add(colorClass);
                        }
                    }
                }
            }
        }
    }
    

}
