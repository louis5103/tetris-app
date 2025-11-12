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
                        
                        // 아이템이 있는 경우 pivot 블록에만 아이템 마커 표시
                        // ✅ WEIGHT_BOMB는 테트로미노 전체가 아이템이므로 마커 표시 제외
                        boolean isPivotBlock = (row == pivotY && col == pivotX);
                        boolean isWeightBomb = (tetromino.getType() == TetrominoType.WEIGHT_BOMB);
                        boolean shouldShowItemMarker = isItemBlock && isPivotBlock && !isWeightBomb;
                        
                        if (shouldShowItemMarker) {
                            // ✨ 수정: pivot 블록에는 배경색 + 아이템 마커 오버레이
                            // 배경색 먼저 적용
                            rect.setFill(ColorMapper.toJavaFXColor(color));
                            rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
                            rect.getStyleClass().removeAll("range-bomb-block", "cross-bomb-block", "line-clear-block", "selectable-block");
                            
                            String colorClass = ColorMapper.toCssClass(color, currentColorBlindMode);
                            if (colorClass != null) {
                                rect.getStyleClass().add(colorClass);
                            }
                            
                            // 아이템 마커는 투명 오버레이로 표시 (별도 처리)
                            applyItemMarkerOverlay(rect, itemType);
                        } else {
                            // 일반 블록 - 기본 색상만 적용
                            rect.setFill(ColorMapper.toJavaFXColor(color));
                            rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
                            rect.getStyleClass().removeAll("range-bomb-block", "cross-bomb-block", "line-clear-block", "selectable-block");
                            
                            String colorClass = ColorMapper.toCssClass(color, currentColorBlindMode);
                            if (colorClass != null) {
                                rect.getStyleClass().add(colorClass);
                            }
                            
                            // 기존 마커 제거
                            removeItemMarkerOverlay(rect);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 🎨 아이템 마커를 Rectangle 위에 오버레이로 표시
     * 
     * Rectangle의 parent가 StackPane인 경우, ImageView를 추가하여
     * 배경색 위에 아이템 아이콘을 겹쳐서 표시합니다.
     * 
     * ✨ 핵심 개선:
     * 1. 배경색이 보이도록 반투명 이미지 사용
     * 2. 회전해도 아이콘은 항상 정방향 유지 (rotate=0)
     * 
     * @param rect 대상 Rectangle
     * @param itemType 아이템 타입
     */
    private void applyItemMarkerOverlay(Rectangle rect, seoultech.se.core.item.ItemType itemType) {
        if (itemType == null) {
            System.err.println("⚠️ [BoardRenderer] applyItemMarkerOverlay called with null itemType");
            return;
        }
        
        // Rectangle의 부모가 StackPane인지 확인
        if (!(rect.getParent() instanceof javafx.scene.layout.StackPane)) {
            System.err.println("⚠️ [BoardRenderer] Rectangle parent is not StackPane, cannot add ImageView overlay");
            return;
        }
        
        javafx.scene.layout.StackPane parentPane = (javafx.scene.layout.StackPane) rect.getParent();
        
        // StackPane의 자식 노드 중 ImageView가 있고, 같은 itemType이면 스킵
        for (javafx.scene.Node node : parentPane.getChildren()) {
            if (node instanceof javafx.scene.image.ImageView) {
                javafx.scene.image.ImageView existingView = (javafx.scene.image.ImageView) node;
                if (existingView.getId() != null && existingView.getId().equals(itemType.name())) {
                    // 이미 동일한 아이템 마커가 있으므로 스킵 (로그 없음)
                    return;
                }
            }
        }
        
        // 기존 마커 제거 (다른 타입의 마커인 경우)
        removeItemMarkerOverlay(rect);
        
        // 아이템 타입에 따라 이미지 또는 텍스트 선택
        String imagePath = null;
        String textOverlay = null;
        
        switch (itemType) {
            case WEIGHT_BOMB:
            case BOMB:
                imagePath = "/image/bomb.png";
                break;
            case PLUS:
                imagePath = "/image/cross.png";
                break;
            case LINE_CLEAR:
                imagePath = "/image/L.png";
                break;
            case SPEED_RESET:
                // ⚡ SPEED_RESET은 텍스트로 표시 (전용 아이콘 없음)
                textOverlay = "⚡";
                break;
            case BONUS_SCORE:
                // ⭐ BONUS_SCORE는 텍스트로 표시 (전용 아이콘 없음)
                textOverlay = "⭐";
                break;
            default:
                System.err.println("⚠️ [BoardRenderer] Unknown item type: " + itemType);
                return;
        }
        
        // ImageView 또는 Text 생성 및 추가
        if (imagePath != null) {
            try {
                String imageUrl = getClass().getResource(imagePath).toExternalForm();
                javafx.scene.image.Image image = new javafx.scene.image.Image(imageUrl);
                javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
                
                // 🔥 FIX: 이미지를 정확히 정사각형으로 만들어 대각선 문제 해결
                double size = rect.getWidth() * 0.8;  // 80% 크기
                imageView.setFitWidth(size);
                imageView.setFitHeight(size);
                imageView.setPreserveRatio(false);  // 🔥 비율 유지 끄기 - 정사각형으로 강제
                imageView.setSmooth(true);
                
                // ✨ 핵심: 항상 회전 0도로 고정
                imageView.setRotate(0);
                
                // 마우스 이벤트 무시 (Rectangle이 클릭 받도록)
                imageView.setMouseTransparent(true);
                
                // 🔥 FIX: ImageView에 itemType ID 설정 (중복 체크용)
                imageView.setId(itemType.name());
                
                // userData에 저장하여 나중에 제거 가능하도록
                rect.setUserData(imageView);
                
                // StackPane에 추가 (StackPane의 alignment가 CENTER이므로 자동 중앙 정렬)
                parentPane.getChildren().add(imageView);
                
                // 🔥 FIX: 로그를 실제 추가 시에만 출력 (중복 방지)
                System.out.println("🎨 [BoardRenderer] Item marker overlay added: " + itemType);
            } catch (Exception e) {
                System.err.println("⚠️ [BoardRenderer] Failed to load item image: " + imagePath + " - " + e.getMessage());
            }
        } else if (textOverlay != null) {
            // 텍스트 오버레이 생성 (SPEED_RESET, BONUS_SCORE)
            javafx.scene.text.Text text = new javafx.scene.text.Text(textOverlay);
            text.setStyle("-fx-font-size: " + (rect.getWidth() * 0.7) + "px; " +
                         "-fx-font-weight: bold; " +
                         "-fx-fill: white; " +
                         "-fx-stroke: black; " +
                         "-fx-stroke-width: 2;");
            
            // 마우스 이벤트 무시
            text.setMouseTransparent(true);
            
            // ID 설정 (중복 체크용)
            text.setId(itemType.name());
            
            // userData에 저장
            rect.setUserData(text);
            
            // StackPane에 추가
            parentPane.getChildren().add(text);
            
            System.out.println("🎨 [BoardRenderer] Item marker text overlay added: " + itemType + " (" + textOverlay + ")");
        }
    }
    
    /**
     * 아이템 마커 오버레이 제거
     * 
     * @param rect 대상 Rectangle
     */
    private void removeItemMarkerOverlay(Rectangle rect) {
        if (rect.getParent() instanceof javafx.scene.layout.StackPane) {
            javafx.scene.layout.StackPane parentPane = (javafx.scene.layout.StackPane) rect.getParent();
            
            // 🔥 FIX: StackPane에서 Rectangle을 제외한 모든 노드(ImageView, Text) 제거
            parentPane.getChildren().removeIf(node -> 
                node instanceof javafx.scene.image.ImageView || 
                node instanceof javafx.scene.text.Text
            );
            
            rect.setUserData(null);
        }
    }
    
    /**
     * 내부용 셀 업데이트 메서드 (Platform.runLater 없음)
     */
    private void updateCellInternal(int row, int col, Cell cell) {
        Rectangle rect = cellRectangles[row][col];
        
        // 🔥 FIX: Lock된 셀에 남아있는 아이템 마커 제거 (메모리 누수 방지)
        removeItemMarkerOverlay(rect);
        
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
        drawHoldPiece(type, null);
    }
    
    /**
     * Hold 영역에 테트로미노를 그립니다 (아이템 정보 포함)
     * 
     * @param type 테트로미노 타입 (null이면 비움)
     * @param itemType 아이템 타입 (null이면 일반 블록)
     */
    public void drawHoldPiece(TetrominoType type, seoultech.se.core.item.ItemType itemType) {
        Platform.runLater(() -> {
            // 모든 셀 초기화
            clearPreviewGrid(holdCellRectangles);
            
            if (type != null) {
                drawPreviewPiece(holdCellRectangles, type, itemType);
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
        drawPreviewPiece(grid, type, null);
    }
    
    /**
     * 미리보기 그리드에 테트로미노를 그립니다 (아이템 정보 포함)
     * 
     * @param grid 그릴 Rectangle 배열
     * @param type 테트로미노 타입
     * @param itemType 아이템 타입 (null이면 일반 블록)
     */
    private void drawPreviewPiece(Rectangle[][] grid, TetrominoType type, seoultech.se.core.item.ItemType itemType) {
        int[][] shape = type.shape;
        Color color = ColorMapper.toJavaFXColor(type.color);
        
        int offsetX = (UIConstants.PREVIEW_GRID_COLS - shape[0].length) / 2;
        int offsetY = (UIConstants.PREVIEW_GRID_ROWS - shape.length) / 2;
        
        boolean isItemBlock = (itemType != null);
        
        // 🔥 CRITICAL FIX: 실제 pivot 위치를 사용 (테트로미노의 중심)
        // TetrominoType에서 pivotX, pivotY를 가져올 수 없으므로, Tetromino 객체 생성
        seoultech.se.core.model.Tetromino tempTetromino = new seoultech.se.core.model.Tetromino(type);
        int pivotInShape = tempTetromino.getPivotX();  // shape 배열 내 pivot 열
        int pivotRowInShape = tempTetromino.getPivotY();  // shape 배열 내 pivot 행
        
        int pivotGridRow = -1;
        int pivotGridCol = -1;
        
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
                        
                        // 🔥 pivot 블록인지 확인
                        if (row == pivotRowInShape && col == pivotInShape) {
                            pivotGridRow = gridRow;
                            pivotGridCol = gridCol;
                        }
                    }
                }
            }
        }
        
        // 🔥 아이템 마커 표시 (pivot 블록에만, WEIGHT_BOMB 제외)
        if (isItemBlock && pivotGridRow != -1 && 
            itemType != seoultech.se.core.item.ItemType.WEIGHT_BOMB) {
            Rectangle pivotRect = grid[pivotGridRow][pivotGridCol];
            applyItemMarkerOverlay(pivotRect, itemType);
        }
    }
    

}
