package seoultech.se.client.controller;

import java.io.IOException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import seoultech.se.backend.score.ScoreService;
import seoultech.se.client.config.ApplicationContextProvider;
import seoultech.se.client.constants.UIConstants;
import seoultech.se.client.service.KeyMappingService;
import seoultech.se.client.service.NavigationService;
import seoultech.se.client.service.SettingsService;
import seoultech.se.client.ui.BoardRenderer;
import seoultech.se.client.ui.GameInfoManager;
import seoultech.se.client.ui.InputHandler;
import seoultech.se.client.ui.NotificationManager;
import seoultech.se.client.ui.PopupManager;
import seoultech.se.client.util.ColorMapper;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.config.GameModeConfig;

/**
 * 게임 컨트롤러의 기본 추상 클래스 (공통 기능 정의)
 */
public abstract class BaseGameController {

    // FXML UI 요소들
    @FXML protected GridPane boardGridPane;
    @FXML protected GridPane holdGridPane;
    @FXML protected GridPane nextGridPane;
    @FXML protected Label scoreLabel;
    @FXML protected Label levelLabel;
    @FXML protected Label linesLabel;
    @FXML protected Label gameOverLabel;
    @FXML protected HBox topEventLine;
    @FXML protected Label comboLabel;
    @FXML protected Label lineClearTypeLabel;
    @FXML protected Label backToBackLabel;
    @FXML protected Label lineClearNotificationLabel;
    
    // 팝업 오버레이 요소들
    @FXML protected javafx.scene.layout.VBox pauseOverlay;
    @FXML protected javafx.scene.layout.VBox gameOverOverlay;
    
    // 아이템 인벤토리 UI
    @FXML protected javafx.scene.layout.HBox itemInventoryContainer;

    // 상대방 보드 컨테이너 (멀티플레이용, 기본 숨김)
    @FXML protected HBox opponentContainer;

    @Autowired protected KeyMappingService keyMappingService;
    @Autowired protected NavigationService navigationService;
    @Autowired protected SettingsService settingsService;
    @Autowired protected ScoreService scoreService;
    
    protected BoardController boardController;
    protected GameModeConfig gameModeConfig;
    
    // UI 관리 클래스들
    protected BoardRenderer boardRenderer;
    protected NotificationManager notificationManager;
    protected PopupManager popupManager;
    protected InputHandler inputHandler;
    protected GameInfoManager gameInfoManager;
    
    // Rectangle 배열들
    protected Rectangle[][] cellRectangles;
    protected Rectangle[][] holdCellRectangles;
    protected Rectangle[][] nextCellRectangles;

    // Animation state flag to coordinate with game loop
    private volatile boolean isAnimating = false;
    
    protected MediaPlayer mediaPlayer;

    public final boolean isAnimating() { return isAnimating; }
    protected final void setAnimating(boolean animating) { this.isAnimating = animating; }

    @FXML
    public void initialize() {
        System.out.println("🎮 [BaseGameController] Initializing UI components...");
        // SettingsService 수동 주입 (Spring Context가 늦게 로드될 경우 대비)
        if (settingsService == null) {
            settingsService = ApplicationContextProvider.getApplicationContext().getBean(SettingsService.class);
        }
    }

    /**
     * 게임 모드 및 설정 초기화 (자식 클래스에서 호출)
     */
    public void initGame(GameModeConfig config) {
        this.gameModeConfig = config;
        
        // BoardController 생성
        this.boardController = new BoardController(config, config.getDifficulty());
        GameState gameState = boardController.getGameState();
        
        // UI 초기화
        initializeGridPane(gameState);
        initializePreviewPanes();
        
        // 매니저 초기화
        this.notificationManager = new NotificationManager(topEventLine, comboLabel, lineClearTypeLabel, backToBackLabel, lineClearNotificationLabel);
        this.boardRenderer = new BoardRenderer(cellRectangles, holdCellRectangles, nextCellRectangles, settingsService.getColorBlindMode());
        this.gameInfoManager = new GameInfoManager(scoreLabel, levelLabel, linesLabel);
        
        // 팝업 매니저
        this.popupManager = new PopupManager(pauseOverlay, gameOverOverlay, scoreService);
        this.popupManager.setCallback(createPopupCallback());
        
        // 입력 핸들러
        this.inputHandler = new InputHandler(keyMappingService);
        this.inputHandler.setCallback(this::handleCommand);
        this.inputHandler.setGameStateProvider(new InputHandler.GameStateProvider() {
            @Override
            public boolean isGameOver() { return boardController.getGameState().isGameOver(); }
            @Override
            public boolean isPaused() { return boardController.getGameState().isPaused(); }
        });
        
        // 초기 렌더링
        gameInfoManager.updateAll(gameState);
        setupKeyboardControls();
        
        startMusic();

        onInitComplete();
    }

    protected void startMusic() {
        try {
            if (mediaPlayer == null) {
                URL resource = getClass().getResource("/Tetris - Bradinsky.mp3");
                if (resource != null) {
                    Media media = new Media(resource.toString());
                    mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                } else {
                    System.err.println("❌ Could not find music file: /Tetris - Bradinsky.mp3");
                }
            }
            
            if (mediaPlayer != null) {
                mediaPlayer.play();
                System.out.println("🎵 Game background music started");
            }
        } catch (Exception e) {
            System.err.println("❌ Error playing game music: " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            System.out.println("🔇 Game background music stopped");
        }
    }

    protected abstract void onInitComplete(); // 초기화 완료 후크
    protected abstract void handleCommand(GameCommand command); // 입력 처리
    public abstract void startGame();
    public abstract void cleanup();

    /**
     * GameState 변경에 따른 UI 업데이트 (Template Method)
     */
    protected void updateUI(GameState oldState, GameState newState) {
        // ✅ 성능 최적화: AnimationTimer가 이미 UI 스레드에서 실행되므로 Platform.runLater() 제거
        // GameLoop tick이나 사용자 입력 모두 UI 스레드에서 처리되므로 직접 실행
        Runnable updateTask = () -> {
            // 🔥 FIX: 애니메이션 데이터를 복사한 후 즉시 원본에서 클리어 (반복 방지)
            int[] clearedRowsCopy = newState.getLastClearedRows() != null ? newState.getLastClearedRows().clone() : new int[0];
            java.util.List<int[]> itemEffectCellsCopy = new java.util.ArrayList<>();
            if (newState.getItemEffectClearedCells() != null) {
                for (int[] cell : newState.getItemEffectClearedCells()) {
                    itemEffectCellsCopy.add(cell.clone());
                }
            }
            
            // 라인이 제거되었는지 확인
            boolean hasLineClearRows = clearedRowsCopy.length > 0;
            
            // 아이템 효과로 셀이 제거되었는지 확인
            boolean hasItemEffectCells = !itemEffectCellsCopy.isEmpty();
            
            // 라인 클리어 또는 아이템 효과가 있으면 애니메이션 실행
            boolean shouldAnimate = hasLineClearRows || hasItemEffectCells;
            
            // ✅ 원본 GameState에서 애니메이션 데이터 즉시 클리어 (다음 입력 시 재트리거 방지)
            if (shouldAnimate) {
                newState.setLastClearedRows(new int[0]);
                newState.setItemEffectClearedCells(new java.util.ArrayList<>());
                newState.setLastClearedCells(new java.util.ArrayList<>());
            }

            // ✅ 애니메이션 중에도 Next/Hold만 즉시 업데이트 (보드 전체는 최종 시점에 반영)
            Runnable immediateUIUpdate = () -> {
                if (newState.getNextQueue() != null && newState.getNextQueue().length > 0) {
                    boardRenderer.drawNextPiece(newState.getNextQueue()[0]);
                }
                if (oldState.getHeldPiece() != newState.getHeldPiece() || oldState.getHeldItemType() != newState.getHeldItemType()) {
                    boardRenderer.drawHoldPiece(newState.getHeldPiece(), newState.getHeldItemType());
                }
            };
            
            Runnable uiUpdateTask = () -> {
                // 1. 보드 전체 렌더링 (라인 클리어 후 최종 상태)
                // ✅ drawBoardSync()가 updateCellInternal()을 호출하여 인라인 스타일 자동 제거
                boardRenderer.drawBoardSync(newState);
                if (newState.getNextQueue() != null && newState.getNextQueue().length > 0) {
                    boardRenderer.drawNextPiece(newState.getNextQueue()[0]);
                }
                // 2. Hold 업데이트
                if (oldState.getHeldPiece() != newState.getHeldPiece() || oldState.getHeldItemType() != newState.getHeldItemType()) {
                    boardRenderer.drawHoldPiece(newState.getHeldPiece(), newState.getHeldItemType());
                }
                // 3. 정보 업데이트
                gameInfoManager.updateAll(newState);
                
                // 4. 이벤트 알림 (콤보, B2B, 레벨업 등)
                checkEvents(oldState, newState);
                
                // 5. 게임 상태 체크 (오버, 일시정지)
                checkGameState(oldState, newState);
            };

            // 라인 클리어 또는 아이템 효과 애니메이션 처리 (UI 전용)
            if (shouldAnimate) {
                // Flag on: signal game loop to skip gravity ticks during animation
                setAnimating(true);
                System.out.println("🎞️ [UI] Animation start (line/item). Gravity paused");
                // Performance optimized: silent animation execution
                
                // ✅ 딜레이 제거: 새 테트로미노를 애니메이션 시작과 동시에 즉시 표시
                immediateUIUpdate.run();
                
                // ✅ PauseTransition 사용: 일회성 타이머, AnimationTimer 매 프레임 체크 오버헤드 제거
                // Step 1: 아이템 효과로 제거된 셀 하이라이트 (BOMB, PLUS)
                if (hasItemEffectCells) {
                    boardRenderer.highlightClearedCellsSync(itemEffectCellsCopy);
                    
                    // 300ms 후 Step 1b로 진행
                    PauseTransition step1bDelay = new PauseTransition(Duration.millis(UIConstants.LINE_CLEAR_ANIMATION_MS));
                    step1bDelay.setOnFinished(event -> {
                        // Step 1b: 라인 클리어 하이라이트 (LINE_CLEAR 아이템 포함)
                        if (hasLineClearRows) {
                            java.util.List<int[]> cells = new java.util.ArrayList<>();
                            for (int row : clearedRowsCopy) {
                                for (int col = 0; col < newState.getBoardWidth(); col++) {
                                    cells.add(new int[]{row, col});
                                }
                            }
                            boardRenderer.highlightClearedCellsSync(cells);
                        }
                        
                        // Step 2: 300ms 후 최종 UI 업데이트
                        PauseTransition step2Delay = new PauseTransition(Duration.millis(UIConstants.LINE_CLEAR_ANIMATION_MS));
                        step2Delay.setOnFinished(event2 -> {
                            uiUpdateTask.run();
                            setAnimating(false);
                            System.out.println("✅ [UI] Animation end. Gravity resumed");
                        });
                        step2Delay.play();
                    });
                    step1bDelay.play();
                } else {
                    // 아이템 효과 없음 - 바로 라인 클리어 하이라이트
                    
                    if (hasLineClearRows) {
                        java.util.List<int[]> cells = new java.util.ArrayList<>();
                        for (int row : clearedRowsCopy) {
                            for (int col = 0; col < newState.getBoardWidth(); col++) {
                                cells.add(new int[]{row, col});
                            }
                        }
                        boardRenderer.highlightClearedCellsSync(cells);
                    }
                    
                    // Step 2: 300ms 후 최종 UI 업데이트
                    PauseTransition step2Delay = new PauseTransition(Duration.millis(UIConstants.LINE_CLEAR_ANIMATION_MS));
                    step2Delay.setOnFinished(event -> {
                        uiUpdateTask.run();
                        setAnimating(false);
                        System.out.println("✅ [UI] Animation end. Gravity resumed");
                    });
                    step2Delay.play();
                }
            } else {
                uiUpdateTask.run();
            }
        };
        
        // AnimationTimer와 InputHandler 모두 UI 스레드에서 실행되므로 직접 호출
        if (Platform.isFxApplicationThread()) {
            updateTask.run();
        } else {
            // 혹시 백그라운드 스레드에서 호출된 경우만 Platform.runLater 사용
            Platform.runLater(updateTask);
        }
    }
    
    // 자식 클래스에서 오버라이드 가능한 훅
    protected void onLineClearAnimationStart() {}
    protected void onLineClearAnimationEnd() {}

    private void checkEvents(GameState oldState, GameState newState) {
        // 콤보
        if (newState.getComboCount() > oldState.getComboCount()) {
            notificationManager.showCombo("🔥 COMBO x" + newState.getComboCount());
        }
        // B2B
        if (newState.getBackToBackCount() > oldState.getBackToBackCount()) {
            notificationManager.showBackToBack("⚡ B2B x" + newState.getBackToBackCount());
        }
        // 레벨업
        if (newState.getLevel() > oldState.getLevel()) {
            notificationManager.showLineClearType("📈 LEVEL UP! - Level " + newState.getLevel());
        }
        // 라인 클리어 텍스트
        if (newState.getLastLinesCleared() > 0) {
             // ... (기존 로직 동일)
             notificationManager.showLineClearCount(newState.getLastLinesCleared(), newState.getLinesCleared());
        }
    }
    
    private void checkGameState(GameState oldState, GameState newState) {
        // 일시정지 상태 변경
        if (!oldState.isPaused() && newState.isPaused()) {
            popupManager.showPausePopup();
            onPause();
        } else if (oldState.isPaused() && !newState.isPaused()) {
            popupManager.hidePausePopup();
            onResume();
        }
        
        // 게임 오버
        if (!oldState.isGameOver() && newState.isGameOver()) {
            processGameOver(newState.getScore());
        }
    }
    
    protected void onPause() {}
    protected void onResume() {}

    protected void processGameOver(long finalScore) {
        System.out.println("💥 [BaseGameController] Game Over");
        // ✅ 입력 차단 제거: 게임 오버 시 InputHandler의 isGameOver() 체크로 자동 차단됨
        if (gameOverLabel != null) gameOverLabel.setVisible(true);
        
        boolean isItemMode = gameModeConfig != null && gameModeConfig.isItemSystemEnabled();
        popupManager.showGameOverPopup(finalScore, isItemMode, settingsService.getCurrentDifficulty());
        
        cleanup(); // 리소스 정리
    }

    private void initializeGridPane(GameState gameState) {
        int width = gameState.getBoardWidth();
        int height = gameState.getBoardHeight();
        
        boardGridPane.getChildren().clear();
        cellRectangles = new Rectangle[height][width];
        
        double boardWidth = width * UIConstants.CELL_SIZE;
        double boardHeight = height * UIConstants.CELL_SIZE;
        boardGridPane.setPrefSize(boardWidth, boardHeight);
        boardGridPane.setMinSize(boardWidth, boardHeight);
        boardGridPane.setMaxSize(boardWidth, boardHeight);

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Rectangle rect = new Rectangle(UIConstants.CELL_SIZE, UIConstants.CELL_SIZE);
                rect.setFill(ColorMapper.getEmptyCellColor());
                rect.setStroke(ColorMapper.getCellBorderColor());
                rect.setStrokeWidth(UIConstants.CELL_BORDER_WIDTH);
                rect.setSmooth(false);
                rect.setCache(true);
                rect.getStyleClass().add(UIConstants.BOARD_CELL_CLASS);

                javafx.scene.layout.StackPane cellPane = new javafx.scene.layout.StackPane();
                cellPane.getChildren().add(rect);
                cellPane.setAlignment(javafx.geometry.Pos.CENTER);
                cellPane.setPrefSize(UIConstants.CELL_SIZE, UIConstants.CELL_SIZE);
                
                boardGridPane.add(cellPane, col, row);
                cellRectangles[row][col] = rect;
            }
        }
    }

    private void initializePreviewPanes() {
        holdCellRectangles = new Rectangle[UIConstants.PREVIEW_GRID_ROWS][UIConstants.PREVIEW_GRID_COLS];
        initializePreviewGrid(holdGridPane, holdCellRectangles);
        
        nextCellRectangles = new Rectangle[UIConstants.PREVIEW_GRID_ROWS][UIConstants.PREVIEW_GRID_COLS];
        initializePreviewGrid(nextGridPane, nextCellRectangles);
    }

    private void initializePreviewGrid(GridPane gridPane, Rectangle[][] rectangles) {
        gridPane.getChildren().clear();
        for (int row = 0; row < UIConstants.PREVIEW_GRID_ROWS; row++) {
            for (int col = 0; col < UIConstants.PREVIEW_GRID_COLS; col++) {
                Rectangle rect = new Rectangle(UIConstants.PREVIEW_CELL_SIZE, UIConstants.PREVIEW_CELL_SIZE);
                rect.setFill(ColorMapper.getEmptyCellColor());
                rect.setStroke(ColorMapper.getCellBorderColor());
                rect.setStrokeWidth(UIConstants.CELL_BORDER_WIDTH);
                rect.getStyleClass().add(UIConstants.PREVIEW_CELL_CLASS);
                
                gridPane.add(rect, col, row);
                rectangles[row][col] = rect;
            }
        }
    }

    private void setupKeyboardControls() {
        if (boardGridPane.getScene() != null) {
            boardGridPane.getScene().setOnKeyPressed(inputHandler::handleKeyPress);
        } else {
            boardGridPane.sceneProperty().addListener((obs, old, newScene) -> {
                if (newScene != null) newScene.setOnKeyPressed(inputHandler::handleKeyPress);
            });
        }
    }
    
    protected void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // Popup Callback
    private PopupManager.PopupActionCallback createPopupCallback() {
        return new PopupManager.PopupActionCallback() {
            @Override
            public void onResumeRequested() { 
                // Resume Command 실행
                if (inputHandler != null) inputHandler.handleKeyPress(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", javafx.scene.input.KeyCode.P, false, false, false, false));
            }
            @Override
            public void onQuitRequested() { 
                cleanup();
                try { navigationService.navigateTo("/view/main-view.fxml"); } catch (IOException e) { e.printStackTrace(); }
            }
            @Override
            public void onMainMenuRequested() {
                cleanup();
                try { navigationService.navigateTo("/view/main-view.fxml"); } catch (IOException e) { e.printStackTrace(); }
            }
            @Override
            public void onRestartRequested() {
                // 재시작 로직은 구현체에서 처리하거나 여기서 공통 처리
                // 하지만 재시작은 Controller 재생성이 깔끔하므로 네비게이션 추천
                cleanup();
                // TODO: 같은 모드로 재시작하는 로직 필요 (여기선 간단히 메인으로)
                try { navigationService.navigateTo("/view/main-view.fxml"); } catch (IOException e) { e.printStackTrace(); }
            }
        };
    }
    
    // 오버레이 이벤트 핸들러 (FXML 연결용)
    @FXML public void handleResumeFromOverlay() { popupManager.handleResumeAction(); }
    @FXML public void handleQuitFromOverlay() { popupManager.handleQuitAction(); }
    @FXML public void handleMainFromOverlay() { popupManager.handleMainMenuAction(); }
    @FXML public void handleRestartFromOverlay() { popupManager.handleRestartAction(); }
    @FXML public void handleExitFromOverlay() { Platform.exit(); }
    
    // Public getters (P2P support)
    public BoardRenderer getBoardRenderer() {
        return boardRenderer;
    }
    
    public javafx.scene.layout.GridPane getBoardGridPane() {
        return boardGridPane;
    }
}
