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
    
    // 🚀 이미지 캐시 (정적 필드)
    private static final java.util.Map<String, javafx.scene.image.Image> IMAGE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    
    // ⚡ 성능 최적화: 이전 테트로미노 위치 저장 (차분 업데이트용)
    private Tetromino previousTetromino = null;
    private int previousX = -1;
    private int previousY = -1;
    
    // 🔒 락 감지: 이전 그리드 상태 저장 (변경된 셀만 업데이트)
    private Cell[][] previousGrid = null;
    
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
     * 특정 셀의 Rectangle을 업데이트합니다 (동기 버전 - 애니메이션용)
     * 
     * ⚠️ UI 스레드에서만 호출해야 합니다!
     * 
     * @param row 행 인덱스
     * @param col 열 인덱스
     * @param cell 셀 데이터
     */
    public void updateCellSync(int row, int col, Cell cell) {
        updateCellInternal(row, col, cell);
    }
    
    /**
     * 특정 셀의 Rectangle을 업데이트합니다
     * 
     * ⚠️ Thread-safe: UI 스레드가 아니면 Platform.runLater()로 감싸서 실행
     * 
     * @param row 행 인덱스
     * @param col 열 인덱스
     * @param cell 셀 데이터
     */
    public void updateCell(int row, int col, Cell cell) {
        Runnable updateTask = () -> {
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
        };
        
        if (Platform.isFxApplicationThread()) {
            updateTask.run();
        } else {
            Platform.runLater(updateTask);
        }
    }
    
    /**
     * 현재 테트로미노를 포함한 전체 보드를 다시 그립니다
     * 
     * ⚠️ Thread-safe: UI 스레드가 아니면 Platform.runLater()로 감싸서 실행
     * 
     * @param gameState 현재 게임 상태
     */
    public void drawBoard(GameState gameState) {
        Runnable drawTask = () -> {
            drawBoardInternal(gameState);
        };
        
        if (Platform.isFxApplicationThread()) {
            drawTask.run();
        } else {
            Platform.runLater(drawTask);
        }
    }
    
    /**
     * 보드를 동기적으로 그립니다 (이미 UI 스레드에 있을 때 사용)
     * ✅ 성능 최적화: Platform.runLater() 체크 없이 즉시 실행
     * 
     * @param gameState 현재 게임 상태
     */
    public void drawBoardSync(GameState gameState) {
        drawBoardInternal(gameState);
    }
    
    /**
     * 내부 보드 렌더링 메서드
     */
    private void drawBoardInternal(GameState gameState) {
        drawBoardInternal(gameState, true);
    }
    
    /**
     * 내부 보드 렌더링 메서드
     * ⚡ 성능 최적화: 변경된 셀만 업데이트 (락 시 ~4개, 이동 시 ~8개)
     * 🔥 애니메이션 처리: 강제 전체 렌더링 플래그 지원
     * 
     * @param gameState 현재 게임 상태
     * @param includeCurrentTetromino 현재 테트로미노를 포함할지 여부
     */
    private void drawBoardInternal(GameState gameState, boolean includeCurrentTetromino) {
        System.out.println("🖌️ [BoardRenderer] drawBoardInternal. Tetromino: " + (gameState.getCurrentTetromino() != null)); // Debug log
        Cell[][] currentGrid = gameState.getGrid();
        
        // 🔒 락 감지: 이전 그리드와 비교하여 변경된 셀만 업데이트
        if (previousGrid != null) {
            // 변경된 셀만 업데이트 (락된 블록만)
            for (int row = 0; row < gameState.getBoardHeight(); row++) {
                for (int col = 0; col < gameState.getBoardWidth(); col++) {
                    Cell prev = previousGrid[row][col];
                    Cell curr = currentGrid[row][col];
                    Rectangle rect = cellRectangles[row][col];
                    
                    // 셀이 변경되었거나 애니메이션 스타일이 있으면 업데이트
                    // 🔥 인라인 스타일 체크: 애니메이션으로 흰색이 된 셀 감지
                    boolean hasAnimationStyle = rect.getStyle() != null && 
                                               !rect.getStyle().isEmpty() && 
                                               rect.getStyle().contains("-fx-fill: white");
                    boolean gridChanged = prev.isOccupied() != curr.isOccupied() || 
                                         (prev.isOccupied() && prev.getColor() != curr.getColor());
                    
                    if (hasAnimationStyle || gridChanged) {
                        updateCellInternal(row, col, curr);
                    }
                }
            }
        } else {
            // 첫 렌더링 - 전체 보드 그리기
            for (int row = 0; row < gameState.getBoardHeight(); row++) {
                for (int col = 0; col < gameState.getBoardWidth(); col++) {
                    updateCellInternal(row, col, currentGrid[row][col]);
                }
            }
        }
        
        // 이전 그리드 저장 (얕은 복사로 충분 - Cell은 불변)
        previousGrid = new Cell[currentGrid.length][currentGrid[0].length];
        for (int row = 0; row < currentGrid.length; row++) {
            System.arraycopy(currentGrid[row], 0, previousGrid[row], 0, currentGrid[row].length);
        }
        
        // ⚡ 최적화: 이전 테트로미노 위치 지우기 (그리드 셀로 복원)
        if (previousTetromino != null) {
            clearPreviousTetromino(gameState);
        }
        
        // 현재 테트로미노가 있으면 그립니다
        if (includeCurrentTetromino && gameState.getCurrentTetromino() != null) {
            drawCurrentTetromino(gameState);
            
            // 상태 저장
            previousTetromino = gameState.getCurrentTetromino();
            previousX = gameState.getCurrentX();
            previousY = gameState.getCurrentY();
        } else {
            // 테트로미노가 없으면 이전 상태 초기화
            previousTetromino = null;
            previousX = -1;
            previousY = -1;
        }
    }
    
    /**
     * 이전 테트로미노 위치를 현재 그리드 셀로 복원합니다
     * ⚡ 성능 최적화: 변경된 위치만 업데이트
     */
    private void clearPreviousTetromino(GameState currentState) {
        if (previousTetromino == null) return;
        
        int[][] shape = previousTetromino.getCurrentShape();
        int pivotX = previousTetromino.getPivotX();
        int pivotY = previousTetromino.getPivotY();
        Cell[][] grid = currentState.getGrid();
        
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[0].length; col++) {
                if (shape[row][col] == 1) {
                    int absoluteX = previousX + (col - pivotX);
                    int absoluteY = previousY + (row - pivotY);
                    
                    if (absoluteY >= 0 && absoluteY < currentState.getBoardHeight() &&
                        absoluteX >= 0 && absoluteX < currentState.getBoardWidth()) {
                        // 현재 그리드의 셀로 복원 (락된 블록 표시)
                        updateCellInternal(absoluteY, absoluteX, grid[absoluteY][absoluteX]);
                    }
                }
            }
        }
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
        seoultech.se.core.engine.item.ItemType itemType = gameState.getCurrentItemType();

        int markerIndex = -1;
        if (isItemBlock) {
            markerIndex = tetromino.getItemMarkerBlockIndex();
            if (tetromino.getType() == TetrominoType.O) {
                int rotations = tetromino.getRotationState().ordinal();
                int initialRow = markerIndex / 2;
                int initialCol = markerIndex % 2;
                
                int rotatedRow = initialRow;
                int rotatedCol = initialCol;
        
                for (int i = 0; i < rotations; i++) {
                    int temp = rotatedRow;
                    rotatedRow = rotatedCol;
                    rotatedCol = 1 - temp;
                }
                markerIndex = rotatedRow * 2 + rotatedCol;
            }
        }
        int blockCount = 0;
        
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[0].length; col++) {
                if (shape[row][col] == 1) {
                    int absoluteX = gameState.getCurrentX() + (col - pivotX);
                    int absoluteY = gameState.getCurrentY() + (row - pivotY);
                    
                    if (absoluteY >= 0 && absoluteY < gameState.getBoardHeight() &&
                        absoluteX >= 0 && absoluteX < gameState.getBoardWidth()) {
                        
                        Rectangle rect = cellRectangles[absoluteY][absoluteX];
                        
                        // 아이템이 있는 경우 올바른 블록에 아이템 마커 표시
                        boolean isWeightBomb = (tetromino.getType() == TetrominoType.WEIGHT_BOMB);
                        boolean shouldShowItemMarker = isItemBlock && (blockCount == markerIndex) && !isWeightBomb;
                        
                        if (shouldShowItemMarker) {
                            // ✨ 수정: 마커 블록에는 배경색 + 아이템 마커 오버레이
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
                    blockCount++;
                }
            }
        }
    }
    
    /**
     * 🎨 아이템 마커를 Rectangle 위에 오버레이로 표시
     * 
     * Rectangle의 parent가 StackPane인 경우, ImageView를 추가하여
     * 배경색 위에 아이템 아이콘을 겹쳐서 표시합니다.
    /**
     * ✨ 핵심 개선:
     * 1. 배경색이 보이도록 반투명 이미지 사용
     * 2. 회전해도 아이콘은 항상 정방향 유지 (rotate=0)
     * 
     * 🔒 PRIORITY 5: synchronized로 중복 방지
     * 
     * @param rect 대상 Rectangle
     * @param itemType 아이템 타입
     */
    private synchronized void applyItemMarkerOverlay(Rectangle rect, seoultech.se.core.engine.item.ItemType itemType) {
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
        
        // StackPane의 자식 노드 중 ImageView/Text가 있고, 같은 itemType이면 스킵
        for (javafx.scene.Node node : parentPane.getChildren()) {
            if (node instanceof javafx.scene.image.ImageView) {
                javafx.scene.image.ImageView existingView = (javafx.scene.image.ImageView) node;
                if (existingView.getId() != null && existingView.getId().equals(itemType.name())) {
                    // 이미 동일한 아이템 마커가 있으므로 스킵 (로그 없음)
                    return;
                }
            } else if (node instanceof javafx.scene.text.Text) {
                javafx.scene.text.Text existingText = (javafx.scene.text.Text) node;
                if (existingText.getId() != null && existingText.getId().equals(itemType.name())) {
                    // 이미 동일한 텍스트 마커가 있으므로 스킵 (로그 없음)
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
                // 🚀 이미지 캐싱 적용 (메모리/IO 최적화)
                javafx.scene.image.Image image = IMAGE_CACHE.computeIfAbsent(imagePath, path -> {
                    try {
                        String imageUrl = getClass().getResource(path).toExternalForm();
                        return new javafx.scene.image.Image(imageUrl);
                    } catch (Exception e) {
                        System.err.println("⚠️ [BoardRenderer] Failed to load image: " + path);
                        return null;
                    }
                });
                
                if (image == null) return; // 로드 실패 시 중단

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
            
            // 🔥 FIX: StackPane에서 Rectangle(배경)을 제외한 모든 노드 제거 (확실한 청소)
            // ImageView, Text 등 모든 오버레이를 제거하여 잔상을 방지함
            parentPane.getChildren().removeIf(node -> node != rect);
            
            rect.setUserData(null);
        }
    }
    
    /**
     * 내부용 셀 업데이트 메서드 (Platform.runLater 없음)
     * 
     * 🔍 동기화 확인:
     * - GameState.grid의 Cell 객체를 직접 읽음
     * - Cell의 isOccupied, color, itemMarker 상태를 Rectangle에 반영
     * - itemMarker는 Lock된 셀에서만 의미 있음 (현재 테트로미노는 drawCurrentTetromino에서 처리)
     */
    private void updateCellInternal(int row, int col, Cell cell) {
        Rectangle rect = cellRectangles[row][col];
        
        // 애니메이션에서 설정한 인라인 스타일과 불투명도를 초기화
        rect.setStyle("");
        rect.setOpacity(1.0);
        
        // 🔍 Cell에 아이템 마커가 있으면 오버레이 표시, 없으면 제거
        if (cell.hasItemMarker()) {
            applyItemMarkerOverlay(rect, cell.getItemMarker());
        } else {
            removeItemMarkerOverlay(rect);
        }
        
        if (cell.isOccupied()) {
            // 🔍 Cell이 점유 상태 → 블록 색상으로 렌더링
            rect.setFill(ColorMapper.toJavaFXColor(cell.getColor()));
            String colorClass = ColorMapper.toCssClass(cell.getColor(), currentColorBlindMode);
            rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
            if (colorClass != null) {
                rect.getStyleClass().add(colorClass);
            }
        } else {
            // 🔍 Cell이 비어있음 → 빈 셀 색상으로 렌더링
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
     * ⚠️ Thread-safe: UI 스레드가 아니면 Platform.runLater()로 감싸서 실행
     * 
     * @param type 테트로미노 타입 (null이면 비움)
     * @param itemType 아이템 타입 (null이면 일반 블록)
     */
    public void drawHoldPiece(TetrominoType type, seoultech.se.core.engine.item.ItemType itemType) {
        Runnable drawTask = () -> {
            // 모든 셀 초기화
            clearPreviewGrid(holdCellRectangles);
            
            if (type != null) {
                drawPreviewPiece(holdCellRectangles, type, itemType);
            }
        };
        
        if (Platform.isFxApplicationThread()) {
            drawTask.run();
        } else {
            Platform.runLater(drawTask);
        }
    }
    
    /**
     * Next 영역에 테트로미노를 그립니다
     * 
     * ⚠️ Thread-safe: UI 스레드가 아니면 Platform.runLater()로 감싸서 실행
     * 
     * @param type 테트로미노 타입 (null이면 비움)
     */
    public void drawNextPiece(TetrominoType type) {
        Runnable drawTask = () -> {
            // 모든 셀 초기화
            clearPreviewGrid(nextCellRectangles);
            
            if (type != null) {
                drawPreviewPiece(nextCellRectangles, type);
            }
        };
        
        if (Platform.isFxApplicationThread()) {
            drawTask.run();
        } else {
            Platform.runLater(drawTask);
        }
    }
    
    /**
     * 미리보기 그리드를 비웁니다
     * 
     * @param grid 비울 Rectangle 배열
     */
    private void clearPreviewGrid(Rectangle[][] grid) {
        for (int row = 0; row < UIConstants.PREVIEW_GRID_ROWS; row++) {
            for (int col = 0; col < UIConstants.PREVIEW_GRID_COLS; col++) {
                Rectangle rect = grid[row][col];
                rect.setFill(ColorMapper.getEmptyCellColor());
                rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
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
    private void drawPreviewPiece(Rectangle[][] grid, TetrominoType type, seoultech.se.core.engine.item.ItemType itemType) {
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
            itemType != seoultech.se.core.engine.item.ItemType.WEIGHT_BOMB) {
            Rectangle pivotRect = grid[pivotGridRow][pivotGridCol];
            applyItemMarkerOverlay(pivotRect, itemType);
        }
    }

    /**
     * 상대방 보드를 그립니다 (멀티플레이 모드)
     *
     * 상대방의 게임 상태를 받아서 별도의 영역에 렌더링합니다.
     * 현재는 기본 구현으로, 추후 별도의 opponent용 Rectangle 배열이 필요할 수 있습니다.
     *
     * @param opponentState 상대방의 게임 상태
     */
    public void drawOpponent(GameState opponentState) {
        // TODO: 상대방 보드를 그리기 위한 별도의 UI 영역이 필요합니다
        // 현재는 로그만 출력하는 기본 구현
        Platform.runLater(() -> {
            System.out.println("👥 [BoardRenderer] Opponent board update - Score: " +
                opponentState.getScore() + ", Lines: " + opponentState.getLinesCleared());

            // 추후 구현:
            // 1. 별도의 Rectangle[][] opponentCellRectangles 필드 추가
            // 2. 상대방 보드 전용 UI 영역에 렌더링
            // 3. 상대방의 현재 테트로미노도 표시
        });
    }
    
    /**
     * 락된 테트로미노만 그립니다 (동기 버전 - 애니메이션용)
     * 
     * ⚠️ UI 스레드에서만 호출해야 합니다!
     * ⚠️ 전체 보드를 다시 그리지 않고 락된 테트로미노 셀들만 업데이트합니다.
     * 
     * @param oldState 락 직전 상태 (사용 안 함, 호환성 유지)
     * @param newState 라인 제거 후 상태 (lastLockedTetromino 정보 포함)
     */
    public void drawBoardWithLockedPieceSync(GameState oldState, GameState newState) {
        System.out.println("🎨 [BoardRenderer] drawBoardWithLockedPieceSync called");
        
        // 락된 테트로미노만 그립니다 (나머지 보드는 건드리지 않음)
        Tetromino lockedTetromino = newState.getLastLockedTetromino();
        
        if (lockedTetromino == null) {
            System.out.println("   ⚠️ lastLockedTetromino is NULL! Cannot draw locked piece.");
            return;
        }
        
        int lockedX = newState.getLastLockedX();
        int lockedY = newState.getLastLockedY();
        int[][] shape = lockedTetromino.getCurrentShape();
        int pivotX = lockedTetromino.getPivotX();
        int pivotY = lockedTetromino.getPivotY();
        seoultech.se.core.model.enumType.Color color = lockedTetromino.getColor();
        
        System.out.println("   ✅ Drawing locked tetromino at (" + lockedY + ", " + lockedX + ") with color " + color);
        
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[0].length; col++) {
                if (shape[row][col] == 1) {
                    int absoluteX = lockedX + (col - pivotX);
                    int absoluteY = lockedY + (row - pivotY);
                    
                    if (absoluteY >= 0 && absoluteY < newState.getBoardHeight() &&
                        absoluteX >= 0 && absoluteX < newState.getBoardWidth()) {
                        Rectangle rect = cellRectangles[absoluteY][absoluteX];
                        rect.setFill(ColorMapper.toJavaFXColor(color));
                        rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
                        String colorClass = ColorMapper.toCssClass(color, currentColorBlindMode);
                        if (colorClass != null) {
                            rect.getStyleClass().add(colorClass);
                        }
                    }
                }
            }
        }
        
        System.out.println("   ✅ Locked tetromino drawing completed");
    }
    
    /**
     * 셀을 직접 업데이트 (내부 헬퍼 메서드)
     */
    private void updateCellDirect(int row, int col, Cell cell) {
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
     * 제거될 셀들을 흰색으로 하이라이트 표시 (동기 버전 - 애니메이션용)
     * 
     * ⚠️ UI 스레드에서만 호출해야 합니다!
     * 
     * @param clearedCells 제거될 셀들의 좌표 [[row1, col1], [row2, col2], ...]
     */
    public void highlightClearedCellsSync(java.util.List<int[]> clearedCells) {
        if (clearedCells == null || clearedCells.isEmpty()) {
            return;
        }
        
        for (int[] cell : clearedCells) {
            int row = cell[0];
            int col = cell[1];
            
            if (row >= 0 && row < cellRectangles.length && 
                col >= 0 && col < cellRectangles[0].length) {
                Rectangle rect = cellRectangles[row][col];
                
                // 모든 스타일 클래스 제거
                rect.getStyleClass().removeAll(UIConstants.ALL_TETROMINO_COLOR_CLASSES);
                rect.getStyleClass().removeAll("range-bomb-block", "cross-bomb-block", "line-clear-block", "selectable-block");
                
                // 아이템 마커 오버레이 제거
                removeItemMarkerOverlay(rect);
                
                // 흰색으로 변경 (불투명도 1.0으로 명시)
                rect.setFill(Color.WHITE);
                rect.setOpacity(1.0);
                
                // 🔥 인라인 스타일을 빈 문자열로 설정 (추후 감지 가능하도록)
                rect.setStyle("-fx-fill: white; -fx-opacity: 1.0;");
            }
        }
    }
    
    /**
     * 제거될 셀들을 흰색으로 하이라이트 표시 (비동기 버전)
     * 
     * @param clearedCells 제거될 셀들의 좌표 [[row1, col1], [row2, col2], ...]
     */
    public void highlightClearedCells(java.util.List<int[]> clearedCells) {
        if (clearedCells == null || clearedCells.isEmpty()) {
            return;
        }
        
        Runnable highlightTask = () -> {
            highlightClearedCellsSync(clearedCells);
        };
        
        if (Platform.isFxApplicationThread()) {
            highlightTask.run();
        } else {
            Platform.runLater(highlightTask);
        }
    }

}
