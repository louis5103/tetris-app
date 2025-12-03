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
 *
 * 레이아웃: 메인 보드와 동일한 구조 (Hold - Board - Next)
 */
public class OpponentBoardView extends javafx.scene.layout.HBox {
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
        super(10); // spacing between Hold, Board, Next
        this.setStyle("-fx-padding: 10; -fx-border-color: #666666; -fx-border-width: 2; -fx-background-color: rgba(30, 30, 30, 0.3); -fx-background-radius: 5; -fx-border-radius: 5;");
        this.setAlignment(javafx.geometry.Pos.CENTER);

        // 타이틀
        titleLabel = new Label("OPPONENT");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFA500;");

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

        // 정보 레이블 (CSS 스타일 클래스 적용)
        scoreLabel = new Label("0");
        scoreLabel.getStyleClass().add("info-label-value");

        levelLabel = new Label("1");
        levelLabel.getStyleClass().add("info-label-value");

        linesLabel = new Label("0");
        linesLabel.getStyleClass().add("info-label-value");

        // 좌측 Hold 영역 (메인 보드와 동일한 구조)
        Label holdLabel = new Label("HOLD");
        holdLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
        VBox holdBox = new VBox(5, holdLabel, holdGrid);
        holdBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        holdBox.setStyle("-fx-padding: 5;");

        // 중앙 보드 영역 (타이틀 + 보드)
        VBox centerBox = new VBox(5, titleLabel, boardGrid);
        centerBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        // 우측 정보 패널 (Next + 점수/레벨/라인)
        Label nextLabel = new Label("NEXT");
        nextLabel.setStyle("-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");

        // 점수/레벨/라인 섹션 (제목 + 값)
        VBox scoreSection = new VBox(5);
        scoreSection.setAlignment(javafx.geometry.Pos.CENTER);
        Label scoreTitleLabel = new Label("SCORE");
        scoreTitleLabel.getStyleClass().add("info-label-title");
        scoreSection.getChildren().addAll(scoreTitleLabel, scoreLabel);

        VBox levelSection = new VBox(5);
        levelSection.setAlignment(javafx.geometry.Pos.CENTER);
        Label levelTitleLabel = new Label("LEVEL");
        levelTitleLabel.getStyleClass().add("info-label-title");
        levelSection.getChildren().addAll(levelTitleLabel, levelLabel);

        VBox linesSection = new VBox(5);
        linesSection.setAlignment(javafx.geometry.Pos.CENTER);
        Label linesTitleLabel = new Label("LINES");
        linesTitleLabel.getStyleClass().add("info-label-title");
        linesSection.getChildren().addAll(linesTitleLabel, linesLabel);

        VBox infoPanel = new VBox(20);
        infoPanel.setAlignment(javafx.geometry.Pos.TOP_CENTER);
        infoPanel.setStyle("-fx-padding: 5;");

        VBox nextSection = new VBox(5);
        nextSection.setAlignment(javafx.geometry.Pos.CENTER);
        nextSection.getChildren().addAll(nextLabel, nextGrid);

        infoPanel.getChildren().addAll(
            nextSection,
            scoreSection,
            levelSection,
            linesSection
        );

        // HBox에 추가 (Hold - Board - Next/Info)
        this.getChildren().addAll(holdBox, centerBox, infoPanel);

        // 초기화
        initializeBoard();

        System.out.println("✅ OpponentBoardView created with main board layout structure");
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

        // 정보 업데이트 (값만 표시, 제목은 별도 레이블로 처리됨)
        scoreLabel.setText(String.valueOf(opponentState.getScore()));
        levelLabel.setText(String.valueOf(opponentState.getLevel()));
        linesLabel.setText(String.valueOf(opponentState.getLinesCleared()));
    }
}
