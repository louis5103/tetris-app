package seoultech.se.client.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import seoultech.se.client.constants.ColorBlindMode;
import seoultech.se.client.constants.UIConstants;
import seoultech.se.client.util.ColorMapper;
import seoultech.se.core.GameState;

/**
 * 상대방 보드 표시 컴포넌트
 *
 * 책임:
 * - 상대방 보드 렌더링만 수행 (입력 처리 없음, 게임 루프 없음)
 * - 상대방 정보 표시 (점수, 레벨, 라인)
 *
 * 사용:
 * - 멀티플레이 모드에서만 활성화
 * - GameController가 GameState를 전달하면 렌더링
 */
public class OpponentBoardView extends VBox {
    private static final double CELL_SIZE = UIConstants.CELL_SIZE; // 메인 보드와 동일한 크기
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;

    private final GridPane boardGrid;
    private final GridPane holdGrid;
    private final GridPane nextGrid;
    private final Label titleLabel;
    private final Label scoreLabel;
    private final Label levelLabel;
    private final Label linesLabel;

    private Rectangle[][] cellRectangles;
    private Rectangle[][] holdCellRectangles;
    private Rectangle[][] nextCellRectangles;
    private BoardRenderer boardRenderer;

    /**
     * 생성자
     */
    public OpponentBoardView() {
        super(5); // spacing
        this.setStyle("-fx-padding: 5; -fx-border-color: #444; -fx-border-width: 2; -fx-background-color: #1a1a1a;");

        // 타이틀
        titleLabel = new Label("OPPONENT");
        titleLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #FFA500;");

        // 보드 GridPane
        boardGrid = new GridPane();
        boardGrid.setHgap(0);
        boardGrid.setVgap(0);
        
        // Hold & Next GridPanes
        holdGrid = new GridPane();
        holdGrid.setHgap(0);
        holdGrid.setVgap(0);
        
        nextGrid = new GridPane();
        nextGrid.setHgap(0);
        nextGrid.setVgap(0);
        
        // 미리보기 영역 레이아웃 (HBox)
        javafx.scene.layout.HBox previewBox = new javafx.scene.layout.HBox(10);
        previewBox.setAlignment(javafx.geometry.Pos.CENTER);
        
        VBox holdBox = new VBox(2, new Label("HOLD"), holdGrid);
        holdBox.setAlignment(javafx.geometry.Pos.CENTER);
        holdBox.getChildren().get(0).setStyle("-fx-text-fill: white; -fx-font-size: 9px;");
        
        VBox nextBox = new VBox(2, new Label("NEXT"), nextGrid);
        nextBox.setAlignment(javafx.geometry.Pos.CENTER);
        nextBox.getChildren().get(0).setStyle("-fx-text-fill: white; -fx-font-size: 9px;");
        
        previewBox.getChildren().addAll(holdBox, nextBox);

        // 정보 레이블
        scoreLabel = new Label("Score: 0");
        scoreLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #FFFFFF;");

        levelLabel = new Label("Level: 1");
        levelLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #FFFFFF;");

        linesLabel = new Label("Lines: 0");
        linesLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #FFFFFF;");

        // VBox에 추가 (Preview -> Board -> Info)
        this.getChildren().addAll(titleLabel, previewBox, boardGrid, scoreLabel, levelLabel, linesLabel);

        // 초기화
        initializeBoard();

        System.out.println("✅ OpponentBoardView created with Hold/Next support");
    }

    /**
     * 보드 GridPane 초기화
     */
    private void initializeBoard() {
        cellRectangles = new Rectangle[BOARD_HEIGHT][BOARD_WIDTH];

        for (int row = 0; row < BOARD_HEIGHT; row++) {
            for (int col = 0; col < BOARD_WIDTH; col++) {
                Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
                rect.setFill(ColorMapper.getEmptyCellColor());
                rect.setStroke(ColorMapper.getCellBorderColor());
                rect.setStrokeWidth(0.5);

                // 픽셀 정렬
                rect.setSmooth(false);
                rect.setCache(true);

                boardGrid.add(rect, col, row);
                cellRectangles[row][col] = rect;
            }
        }
        
        // Hold & Next 초기화 (4x4)
        int PREVIEW_SIZE = 4;
        double PREVIEW_CELL_SIZE = UIConstants.PREVIEW_CELL_SIZE; // 메인 프리뷰와 동일한 크기
        
        holdCellRectangles = new Rectangle[PREVIEW_SIZE][PREVIEW_SIZE];
        initializePreviewGrid(holdGrid, holdCellRectangles, PREVIEW_SIZE, PREVIEW_CELL_SIZE);
        
        nextCellRectangles = new Rectangle[PREVIEW_SIZE][PREVIEW_SIZE];
        initializePreviewGrid(nextGrid, nextCellRectangles, PREVIEW_SIZE, PREVIEW_CELL_SIZE);

        // BoardRenderer 생성 (모든 영역 연결)
        boardRenderer = new BoardRenderer(
            cellRectangles, 
            holdCellRectangles, 
            nextCellRectangles, 
            ColorBlindMode.NORMAL
        );

        System.out.println("✅ OpponentBoardView board initialized (" + BOARD_WIDTH + "x" + BOARD_HEIGHT + ")");
    }
    
    private void initializePreviewGrid(GridPane grid, Rectangle[][] rects, int size, double cellSize) {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                Rectangle rect = new Rectangle(cellSize, cellSize);
                rect.setFill(ColorMapper.getEmptyCellColor());
                rect.setStroke(ColorMapper.getCellBorderColor());
                rect.setStrokeWidth(0.5);
                
                grid.add(rect, col, row);
                rects[row][col] = rect;
            }
        }
    }

    /**
     * 상대방 GameState 업데이트
     *
     * @param opponentState 상대방의 게임 상태
     */
    public void update(GameState opponentState) {
        if (opponentState == null) {
            return;
        }

        // 보드 렌더링
        boardRenderer.drawBoard(opponentState);
        
        // Hold 업데이트
        boardRenderer.drawHoldPiece(opponentState.getHeldPiece(), opponentState.getHeldItemType());
        
        // Next 업데이트
        if (opponentState.getNextQueue() != null && opponentState.getNextQueue().length > 0) {
            boardRenderer.drawNextPiece(opponentState.getNextQueue()[0]);
        }

        // 정보 업데이트
        scoreLabel.setText("Score: " + opponentState.getScore());
        levelLabel.setText("Level: " + opponentState.getLevel());
        linesLabel.setText("Lines: " + opponentState.getLinesCleared());
    }
}
