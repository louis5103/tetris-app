package seoultech.se.client.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import seoultech.se.client.constants.ColorBlindMode;
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
    private static final int CELL_SIZE = 15; // 작은 크기
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;

    private final GridPane boardGrid;
    private final Label titleLabel;
    private final Label scoreLabel;
    private final Label levelLabel;
    private final Label linesLabel;

    private Rectangle[][] cellRectangles;
    private BoardRenderer boardRenderer;

    /**
     * 생성자
     */
    public OpponentBoardView() {
        super(10); // spacing
        this.setStyle("-fx-padding: 10; -fx-border-color: #444; -fx-border-width: 2; -fx-background-color: #1a1a1a;");

        // 타이틀
        titleLabel = new Label("OPPONENT");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFA500;");

        // 보드 GridPane
        boardGrid = new GridPane();
        boardGrid.setHgap(0);
        boardGrid.setVgap(0);

        // 정보 레이블
        scoreLabel = new Label("Score: 0");
        scoreLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #FFFFFF;");

        levelLabel = new Label("Level: 1");
        levelLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #FFFFFF;");

        linesLabel = new Label("Lines: 0");
        linesLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #FFFFFF;");

        // VBox에 추가
        this.getChildren().addAll(titleLabel, boardGrid, scoreLabel, levelLabel, linesLabel);

        // 초기화
        initializeBoard();

        System.out.println("✅ OpponentBoardView created");
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

        // BoardRenderer 생성 (hold, next 없음, 색맹 모드 NORMAL)
        boardRenderer = new BoardRenderer(cellRectangles, null, null, ColorBlindMode.NORMAL);

        System.out.println("✅ OpponentBoardView board initialized (" + BOARD_WIDTH + "x" + BOARD_HEIGHT + ")");
    }

    /**
     * 상대방 GameState 업데이트
     *
     * @param opponentState 상대방의 게임 상태
     */
    public void update(GameState opponentState) {
        if (opponentState == null) {
            System.out.println("⚠️ OpponentBoardView.update() called with null state");
            return;
        }

        // 보드 렌더링
        boardRenderer.drawBoard(opponentState);

        // 정보 업데이트
        scoreLabel.setText("Score: " + opponentState.getScore());
        levelLabel.setText("Level: " + opponentState.getLevel());
        linesLabel.setText("Lines: " + opponentState.getLinesCleared());

        System.out.println("👥 OpponentBoardView updated - Score: " + opponentState.getScore());
    }
}
