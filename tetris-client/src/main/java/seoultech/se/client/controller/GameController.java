package seoultech.se.client.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import seoultech.se.client.constants.UIConstants;
import seoultech.se.client.service.KeyMappingService;
import seoultech.se.client.service.NavigationService;
import seoultech.se.client.ui.BoardRenderer;
import seoultech.se.client.ui.GameInfoManager;
import seoultech.se.client.ui.GameLoopManager;
import seoultech.se.client.ui.InputHandler;
import seoultech.se.client.ui.NotificationManager;
// import seoultech.se.client.ui.PopupManager; // PopupManager 제거
import seoultech.se.client.util.ColorMapper;
import seoultech.se.client.service.SettingsService;
import seoultech.se.client.config.ApplicationContextProvider;
import seoultech.se.core.GameState;
import seoultech.se.core.command.Direction;
import seoultech.se.core.command.MoveCommand;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * JavaFX UI를 제어하는 컨트롤러
 * 
 * Phase 3: Observer 패턴 제거 완료
 * 
 * 이 클래스의 역할:
 * 1. 사용자 입력을 Command로 변환
 * 2. Command를 BoardController에 전달하고 GameState 수신
 * 3. GameState 비교하여 UI 힌트 추출 및 업데이트
 * 
 * UI 관련 세부 작업은 다음 클래스들에 위임됩니다:
 * - NotificationManager: 알림 메시지 관리
 * - BoardRenderer: 보드 렌더링
 * - GameLoopManager: 게임 루프 관리
 * - InputHandler: 키보드 입력 처리 및 Command 변환
 * - GameInfoManager: 게임 정보 레이블 업데이트
 */
@Component
public class GameController {

    // FXML UI 요소들
    @FXML private GridPane boardGridPane;
    @FXML private GridPane holdGridPane;
    @FXML private GridPane nextGridPane;
    @FXML private Label scoreLabel;
    @FXML private Label levelLabel;
    @FXML private Label linesLabel;
    @FXML private Label gameOverLabel;
    @FXML private HBox topEventLine;
    @FXML private Label comboLabel;
    @FXML private Label lineClearTypeLabel;
    @FXML private Label backToBackLabel;
    @FXML private Label lineClearNotificationLabel;
    
    // 팝업 오버레이 요소들 제거
    // @FXML private javafx.scene.layout.VBox pauseOverlay;
    // @FXML private javafx.scene.layout.VBox gameOverOverlay;
    // @FXML private Label finalScoreLabel;

    @Autowired
    private KeyMappingService keyMappingService;

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private SettingsService settingsService;

    // 게임 로직 컨트롤러
    private BoardController boardController;
    
    // UI 관리 클래스들
    private BoardRenderer boardRenderer;
    private NotificationManager notificationManager;
    private GameLoopManager gameLoopManager;
    // private PopupManager popupManager; // PopupManager 제거
    private InputHandler inputHandler;
    private GameInfoManager gameInfoManager;
    
    // Rectangle 배열들
    private Rectangle[][] cellRectangles;
    private Rectangle[][] holdCellRectangles;
    private Rectangle[][] nextCellRectangles;

    /**
     * FXML이 로드된 후 자동으로 호출됩니다
     */
    @FXML
    public void initialize() {
        System.out.println("🎮 GameController initializing...");

        // SettingsService 확인
        if (settingsService != null) {
            this.settingsService = ApplicationContextProvider.getApplicationContext().getBean(seoultech.se.client.service.SettingsService.class);
            System.out.println("✅ SettingsService is ready");
        } else {
            System.err.println("❌ SettingsService is null!");
        }

        // KeyMappingService 확인
        if (keyMappingService != null) {
            System.out.println("✅ KeyMappingService is ready");
            keyMappingService.printCurrentMappings();
        } else {
            System.err.println("❌ KeyMappingService is null!");
        }

        // BoardController 생성
        boardController = new BoardController();
        
        GameState gameState = boardController.getGameState();
        System.out.println("📊 Board created: " + gameState.getBoardWidth() + "x" + gameState.getBoardHeight());

        // UI 초기화
        initializeGridPane(gameState);
        initializePreviewPanes();
        
        // UI 관리 클래스들 초기화
        initializeManagers();
        
        gameInfoManager.updateAll(gameState);
        setupKeyboardControls();
        startGame();

        System.out.println("✅ GameController initialization complete!");
    }
    
    /**
     * UI 관리 클래스들을 초기화합니다
     */
    private void initializeManagers() {
        // NotificationManager 초기화
        notificationManager = new NotificationManager(
            topEventLine,
            comboLabel,
            lineClearTypeLabel,
            backToBackLabel,
            lineClearNotificationLabel
        );
        
        // BoardRenderer 초기화
        boardRenderer = new BoardRenderer(
            cellRectangles,
            holdCellRectangles,
            nextCellRectangles,
            settingsService.getColorBlindMode()
        );
        
        // GameLoopManager 초기화
        gameLoopManager = new GameLoopManager();
        gameLoopManager.setCallback(() -> {
            GameState gameState = boardController.getGameState();
            
            if (gameState.isGameOver()) {
                return false; // 게임 루프 중지
            }
            
            if (gameState.isPaused()) {
                return true; // 일시정지 중이면 블록 낙하 안 함, 루프는 계속
            }
            
            // 블록 자동 낙하
            GameState oldState = gameState.deepCopy();
            GameState newState = boardController.executeCommand(new MoveCommand(Direction.DOWN));
            
            // GameState 비교하여 UI 힌트 추출 및 업데이트
            showUiHints(oldState, newState);
            
            return true; // 게임 루프 계속
        });
        
        // PopupManager 관련 코드 제거
        
        // InputHandler 초기화
        inputHandler = new InputHandler(keyMappingService);
        inputHandler.setCallback(command -> {
            GameState oldState = boardController.getGameState().deepCopy();
            GameState newState = boardController.executeCommand(command);
            
            // GameState 비교하여 UI 힌트 추출 및 업데이트
            showUiHints(oldState, newState);
        });
        inputHandler.setGameStateProvider(new InputHandler.GameStateProvider() {
            @Override
            public boolean isGameOver() {
                return boardController.getGameState().isGameOver();
            }
            
            @Override
            public boolean isPaused() {
                return boardController.getGameState().isPaused();
            }
        });
        
        // GameInfoManager 초기화
        gameInfoManager = new GameInfoManager(
            scoreLabel,
            levelLabel,
            linesLabel
        );
    }

    /**
     * GridPane을 초기화하고 모든 셀의 Rectangle을 생성합니다
     */
    private void initializeGridPane(GameState gameState) {
        int width = gameState.getBoardWidth();
        int height = gameState.getBoardHeight();

        System.out.println("🎨 Initializing GridPane with " + width + "x" + height + " cells...");

        // GridPane 기본 설정
        boardGridPane.setHgap(0);
        boardGridPane.setVgap(0);
        
        // GridPane 크기 고정
        double boardWidth = width * UIConstants.CELL_SIZE;
        double boardHeight = height * UIConstants.CELL_SIZE;
      
        boardGridPane.setPrefSize(boardWidth, boardHeight);
        boardGridPane.setMinSize(boardWidth, boardHeight);
        boardGridPane.setMaxSize(boardWidth, boardHeight);
        
        cellRectangles = new Rectangle[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Rectangle rect = new Rectangle(UIConstants.CELL_SIZE, UIConstants.CELL_SIZE);

                // 기본 색상 설정
                rect.setFill(ColorMapper.getEmptyCellColor());
                rect.setStroke(ColorMapper.getCellBorderColor());
                rect.setStrokeWidth(UIConstants.CELL_BORDER_WIDTH);
                
                // 픽셀 정렬로 떨림 방지
                rect.setSmooth(false);
                rect.setCache(true);

                // CSS 클래스 추가
                rect.getStyleClass().add(UIConstants.BOARD_CELL_CLASS);

                // GridPane에 추가
                boardGridPane.add(rect, col, row);
                cellRectangles[row][col] = rect;
            }
        }

        System.out.println("✅ GridPane initialized with " + (width * height) + " cells");
    }
    
    /**
     * Hold와 Next 미리보기 영역 초기화
     */
    private void initializePreviewPanes() {
        // Hold 영역 초기화
        holdCellRectangles = new Rectangle[UIConstants.PREVIEW_GRID_ROWS][UIConstants.PREVIEW_GRID_COLS];
        initializePreviewGrid(holdGridPane, holdCellRectangles, 
                            UIConstants.PREVIEW_GRID_ROWS, UIConstants.PREVIEW_GRID_COLS);
        
        // Next 영역 초기화
        nextCellRectangles = new Rectangle[UIConstants.PREVIEW_GRID_ROWS][UIConstants.PREVIEW_GRID_COLS];
        initializePreviewGrid(nextGridPane, nextCellRectangles, 
                            UIConstants.PREVIEW_GRID_ROWS, UIConstants.PREVIEW_GRID_COLS);
    }
    
    /**
     * 미리보기 그리드 초기화 헬퍼 메서드
     */
    private void initializePreviewGrid(GridPane gridPane, Rectangle[][] rectangles, int rows, int cols) {
        // GridPane 기본 설정
        gridPane.setHgap(0);
        gridPane.setVgap(0);
        
        // GridPane 크기 고정
        double gridWidth = cols * UIConstants.PREVIEW_CELL_SIZE;
        double gridHeight = rows * UIConstants.PREVIEW_CELL_SIZE;
        gridPane.setPrefSize(gridWidth, gridHeight);
        gridPane.setMinSize(gridWidth, gridHeight);
        gridPane.setMaxSize(gridWidth, gridHeight);
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                Rectangle rect = new Rectangle(UIConstants.PREVIEW_CELL_SIZE, UIConstants.PREVIEW_CELL_SIZE);
                rect.setFill(ColorMapper.getEmptyCellColor());
                rect.setStroke(ColorMapper.getCellBorderColor());
                rect.setStrokeWidth(UIConstants.CELL_BORDER_WIDTH);
                
                // 픽셀 정렬로 떨림 방지
                rect.setSmooth(false);
                rect.setCache(true);
                
                // CSS 클래스 추가
                rect.getStyleClass().add(UIConstants.PREVIEW_CELL_CLASS);
                
                gridPane.add(rect, col, row);
                rectangles[row][col] = rect;
            }
        }
    }

    /**
     * 키보드 입력을 처리합니다
     */
    private void setupKeyboardControls() {
        inputHandler.setupKeyboardControls(boardGridPane);
    }

    // ========== GameState 비교하여 UI 힌트 추출 ==========
    
    /**
     * GameState를 비교하여 필요한 UI 힌트를 추출하고 업데이트합니다
     * 
     * Phase 3: Observer 패턴 대체 메서드
     * 
     * @param oldState 이전 GameState
     * @param newState 새로운 GameState
     */
    private void showUiHints(GameState oldState, GameState newState) {
        Platform.runLater(() -> {
            // 1. 보드 전체 렌더링
            boardRenderer.drawBoard(newState);
            
            // 2. Next Queue 업데이트
            TetrominoType[] nextQueue = newState.getNextQueue();
            if (nextQueue != null && nextQueue.length > 0) {
                boardRenderer.drawNextPiece(nextQueue[0]);
            }
            
            // 3. Hold 업데이트
            if (oldState.getHeldPiece() != newState.getHeldPiece()) {
                boardRenderer.drawHoldPiece(newState.getHeldPiece());
            }
            
            // 4. 점수/레벨/라인 업데이트
            gameInfoManager.updateAll(newState);
            gameLoopManager.updateDropSpeed(newState);
            
            // 5. 라인 클리어 감지
            int oldLines = oldState.getLinesCleared();
            int newLines = newState.getLinesCleared();
            if (newLines > oldLines) {
                int linesCleared = newState.getLastLinesCleared();
                boolean isTSpin = newState.isLastLockWasTSpin();
                boolean isTSpinMini = newState.isLastLockWasTSpinMini();
                
                StringBuilder message = new StringBuilder();
                
                // T-Spin 표시
                if (isTSpin) {
                    message.append(isTSpinMini ? "T-SPIN MINI " : "T-SPIN ");
                }
                
                // 라인 타입 표시
                switch (linesCleared) {
                    case 1:
                        message.append("SINGLE");
                        break;
                    case 2:
                        message.append("DOUBLE");
                        break;
                    case 3:
                        message.append("TRIPLE");
                        break;
                    case 4:
                        message.append("TETRIS");
                        break;
                }
                
                // 중앙에 라인 클리어 타입 표시
                if (message.length() > 0) {
                    notificationManager.showLineClearType(message.toString());
                }
                
                // 우측에 라인 클리어 수 표시
                notificationManager.showLineClearCount(linesCleared, newLines);
            }
            
            // 6. 콤보 감지
            int oldCombo = oldState.getComboCount();
            int newCombo = newState.getComboCount();
            if (newCombo > oldCombo) {
                notificationManager.showCombo("🔥 COMBO x" + newCombo);
            }
            
            // 7. Back-to-Back 감지
            int oldB2B = oldState.getBackToBackCount();
            int newB2B = newState.getBackToBackCount();
            if (newB2B > oldB2B) {
                notificationManager.showBackToBack("⚡ B2B x" + newB2B);
            }
            
            // 8. 레벨 업 감지
            int oldLevel = oldState.getLevel();
            int newLevel = newState.getLevel();
            if (newLevel > oldLevel) {
                notificationManager.showLineClearType("📈 LEVEL UP! - Level " + newLevel);
            }
            
            // 9. 일시정지 감지
            boolean wasPaused = oldState.isPaused();
            boolean isPaused = newState.isPaused();
            if (!wasPaused && isPaused) {
                pauseGame();
                showPausePopup(); // 팝업 띄우는 메서드 호출
            } else if (wasPaused && !isPaused) {
                gameLoopManager.resume();
                notificationManager.hideAllNotifications();
            }
            
            // 10. 게임 오버 감지
            boolean wasGameOver = oldState.isGameOver();
            boolean isGameOver = newState.isGameOver();
            if (!wasGameOver && isGameOver) {
                gameOverLabel.setVisible(true);
                System.out.println("💀 GAME OVER");
                System.out.println("   Final Score: " + newState.getScore());
                System.out.println("   Lines Cleared: " + newState.getLinesCleared());
                showGameOverPopup(newState.getScore()); // 팝업 띄우는 메서드 호출
            }
        });
    }

    // ========== UI 업데이트 헬퍼 메서드들 ==========
    // GameInfoManager로 이동됨

    // ========== 게임 제어 ==========
    public void startGame() {
        gameOverLabel.setVisible(false);
        gameLoopManager.start();
        boardGridPane.requestFocus();
        System.out.println("🎮 Game Started!");
    }

    public void pauseGame() {
        gameLoopManager.pause();
        notificationManager.showLineClearType("⏸️ PAUSED - Press P to resume");
    }

    public void resumeGame() {
        gameLoopManager.resume();
        notificationManager.hideAllNotifications();
        // Resume Command 실행하여 게임 상태도 업데이트
        boardController.executeCommand(new seoultech.se.core.command.ResumeCommand());
    }

    // ========== 오버레이 버튼 핸들러 ==========
    // FXML 핸들러 메서드 제거
    // @FXML private void handleResumeFromOverlay() { ... }
    // @FXML private void handleQuitFromOverlay() { ... }
    // @FXML private void handleMainFromOverlay() { ... }
    // @FXML private void handleRestartFromOverlay() { ... }
    
    // ========== 팝업 창 관리 ==========

    /**
     * 일시정지 팝업을 띄웁니다.
     */
    private void showPausePopup() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/pause-pop.fxml"));
            
            // Spring 컨텍스트에서 컨트롤러 인스턴스를 가져오도록 설정
            loader.setControllerFactory(ApplicationContextProvider.getApplicationContext()::getBean);
            
            Parent root = loader.load();
            
            PausePopController controller = loader.getController();
            controller.setResumeCallback(this::resumeGame); // Resume 콜백 설정

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(boardGridPane.getScene().getWindow());
            popupStage.initStyle(StageStyle.TRANSPARENT);

            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);
            
            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showError("팝업 오류", "일시정지 팝업을 불러오는 데 실패했습니다.");
        }
    }

    /**
     * 게임 오버 팝업을 띄웁니다.
     * @param score 최종 점수
     */
    private void showGameOverPopup(long score) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/over-pop.fxml"));
            
            // Spring 컨텍스트에서 컨트롤러 인스턴스를 가져오도록 설정
            loader.setControllerFactory(ApplicationContextProvider.getApplicationContext()::getBean);

            Parent root = loader.load();

            OverPopController controller = loader.getController();
            controller.setScore(score);

            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(boardGridPane.getScene().getWindow());
            popupStage.initStyle(StageStyle.TRANSPARENT);
            
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            popupStage.setScene(scene);

            popupStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            showError("팝업 오류", "게임 오버 팝업을 불러오는 데 실패했습니다.");
        }
    }
    
    // ========== UI 알림 메서드 ==========
    
    /**
     * 오류 알림 표시
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}

