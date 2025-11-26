package seoultech.se.client.controller;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;

import seoultech.se.backend.score.GameMode;
import seoultech.se.backend.score.ScoreRankDto;
import seoultech.se.backend.score.ScoreRequestDto;
import seoultech.se.backend.score.ScoreService;
import seoultech.se.client.config.ApplicationContextProvider;
import seoultech.se.client.constants.UIConstants;
import seoultech.se.client.service.KeyMappingService;
import seoultech.se.client.service.NavigationService;
import seoultech.se.client.service.SettingsService;
import seoultech.se.client.ui.BoardRenderer;
import seoultech.se.client.ui.GameInfoManager;
import seoultech.se.client.ui.GameLoopManager;
import seoultech.se.client.ui.InputHandler;
import seoultech.se.client.ui.ItemInventoryPanel;
import seoultech.se.client.ui.NotificationManager;
import seoultech.se.client.ui.PopupManager;
import seoultech.se.client.util.ColorMapper;
import seoultech.se.core.GameState;
import seoultech.se.core.command.Direction;
import seoultech.se.core.command.MoveCommand;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.item.Item;
import seoultech.se.core.item.ItemType;
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
    
    // 팝업 오버레이 요소들
    @FXML private javafx.scene.layout.VBox pauseOverlay;
    @FXML private javafx.scene.layout.VBox gameOverOverlay;
    
    // 아이템 인벤토리 UI
    @FXML private javafx.scene.layout.HBox itemInventoryContainer;

    @Autowired
    private KeyMappingService keyMappingService;

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private ScoreService scoreService;

    // 게임 로직 컨트롤러
    private BoardController boardController;
    
    // 게임 모드 설정
    private GameModeConfig gameModeConfig;
    
    // UI 관리 클래스들
    private BoardRenderer boardRenderer;
    private NotificationManager notificationManager;
    private GameLoopManager gameLoopManager;
    private PopupManager popupManager;
    private InputHandler inputHandler;
    private GameInfoManager gameInfoManager;
    private ItemInventoryPanel itemInventoryPanel;
    
    // Rectangle 배열들
    private Rectangle[][] cellRectangles;
    private Rectangle[][] holdCellRectangles;
    private Rectangle[][] nextCellRectangles;

    /**
     * FXML이 로드된 후 자동으로 호출됩니다
     * UI 요소만 준비하고, 실제 게임 초기화는 setGameModeConfig()에서 수행합니다
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

        System.out.println("⏳ Waiting for GameModeConfig to be set...");
    }
    
    /**
     * 게임 모드 설정을 적용하고 게임을 초기화합니다
     * MainController에서 씬 전환 전에 호출됩니다
     * 
     * @param config 게임 모드 설정
     */
    public void setGameModeConfig(GameModeConfig config) {
        this.gameModeConfig = config;
        System.out.println("⚙️ Game mode config set: " + 
            (config.getGameplayType() != null ? config.getGameplayType() : "CLASSIC") +
            ", SRS: " + config.isSrsEnabled() +
            ", Hard Drop: " + config.isHardDropEnabled() +
            ", Hold: " + config.isHoldEnabled() +
            ", Drop Speed: " + config.getDropSpeedMultiplier() + "x");
        
        // 이제 실제 게임 초기화 수행
        startInitialization();
    }
    
    /**
     * 실제 게임 초기화를 수행합니다
     * setGameModeConfig()에서 호출되어 config가 확실히 설정된 후 실행됩니다
     */
    private void startInitialization() {
        System.out.println("🚀 Starting game initialization with config...");
        
        // GameModeConfig 기본값 설정 (혹시 모를 경우 대비)
        if (gameModeConfig == null) {
            gameModeConfig = GameModeConfig.classic();
            System.out.println("⚠️ Config was null, using default CLASSIC");
        }

        // ✨ Phase 5: 설정된 난이도 가져오기
        seoultech.se.core.model.enumType.Difficulty difficulty = 
            settingsService.getCurrentDifficulty();
        
        System.out.println("🎮 Creating BoardController with difficulty: " + difficulty.getDisplayName());
        
        // BoardController 생성 (GameModeConfig + Difficulty 전달)
        boardController = new BoardController(gameModeConfig, difficulty);
        
        GameState gameState = boardController.getGameState();
        System.out.println("📊 Board created: " + gameState.getBoardWidth() + "x" + gameState.getBoardHeight());
        System.out.println("   - Difficulty: " + difficulty.getDisplayName());
        System.out.println("   - I-Block Multiplier: " + difficulty.getIBlockMultiplier() + "x");
        System.out.println("   - Score Multiplier: " + difficulty.getScoreMultiplier() + "x");

        // UI 초기화
        initializeGridPane(gameState);
        initializePreviewPanes();
        
        // UI 관리 클래스들 초기화
        initializeManagers();
        
        // 아이템 인벤토리 초기화 (아케이드 모드인 경우)
        initializeItemInventory();
        
        gameInfoManager.updateAll(gameState);
        setupKeyboardControls();
        startGame();

        System.out.println("✅ GameController initialization complete!");
    }
    
    /**
     * 아이템 인벤토리 초기화
     * 아케이드 모드일 때만 활성화됩니다
     */
    private void initializeItemInventory() {
        System.out.println("🔧 [GameController] Initializing item inventory...");
        System.out.println("   - gameModeConfig: " + gameModeConfig);
        System.out.println("   - itemConfig: " + (gameModeConfig != null ? gameModeConfig.getItemConfig() : "null"));
        System.out.println("   - isEnabled: " + (gameModeConfig != null && gameModeConfig.getItemConfig() != null ? gameModeConfig.getItemConfig().isEnabled() : "N/A"));
        
        if (gameModeConfig != null && 
            gameModeConfig.getItemConfig() != null && 
            gameModeConfig.getItemConfig().isEnabled()) {
            
            int maxInventorySize = gameModeConfig.getItemConfig().getMaxInventorySize();
            System.out.println("   - maxInventorySize: " + maxInventorySize);
            
            itemInventoryPanel = new ItemInventoryPanel(maxInventorySize);
            System.out.println("   - ItemInventoryPanel created: " + itemInventoryPanel);
            
            // 아이템 사용 콜백 설정
            itemInventoryPanel.setOnItemUse((item, slotIndex) -> {
                useItem(item, slotIndex);
            });
            
            // 컨테이너에 추가
            if (itemInventoryContainer != null) {
                itemInventoryContainer.getChildren().clear();
                itemInventoryContainer.getChildren().add(itemInventoryPanel);
                itemInventoryContainer.setVisible(true);
                itemInventoryContainer.setManaged(true);
                System.out.println("✅ [GameController] Item inventory initialized (max: " + maxInventorySize + ")");
                System.out.println("   - Container visible: " + itemInventoryContainer.isVisible());
                System.out.println("   - Container managed: " + itemInventoryContainer.isManaged());
                System.out.println("   - Container children: " + itemInventoryContainer.getChildren().size());
            } else {
                System.out.println("⚠️ [GameController] itemInventoryContainer is null!");
            }
        } else {
            // 아이템 시스템 비활성화
            if (itemInventoryContainer != null) {
                itemInventoryContainer.setVisible(false);
                itemInventoryContainer.setManaged(false);
            }
            System.out.println("ℹ️ [GameController] Item system disabled");
        }
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
        
        // GameLoopManager 초기화 (gameModeConfig의 속도 배율 적용)
        double dropSpeedMultiplier = (gameModeConfig != null) 
            ? gameModeConfig.getDropSpeedMultiplier() 
            : 1.0;
        gameLoopManager = new GameLoopManager(dropSpeedMultiplier);
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
        
        // PopupManager 초기화
        popupManager = new PopupManager(pauseOverlay, gameOverOverlay, scoreService);
        popupManager.setCallback(createPopupCallback());
        
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

    private PopupManager.PopupActionCallback createPopupCallback() {
        return new PopupManager.PopupActionCallback() {
            private void navigateSafely(String fxmlPath) {
                Runnable navigationTask = () -> {
                    try {
                        gameLoopManager.stop();
                        navigationService.navigateTo(fxmlPath);
                    } catch (IOException e) {
                        e.printStackTrace();
                        showError("Navigation Error", "Failed to navigate to " + fxmlPath);
                    }
                };

                popupManager.saveScoreIfPending().thenRun(() -> Platform.runLater(navigationTask))
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            Platform.runLater(navigationTask); // 점수 저장에 실패해도 네비게이션은 실행
                            return null;
                        });
            }

            @Override
            public void onResumeRequested() {
                resumeGame();
            }

            @Override
            public void onQuitRequested() {
                navigateSafely("/view/main-view.fxml");
            }

            @Override
            public void onMainMenuRequested() {
                navigateSafely("/view/main-view.fxml");
            }

            @Override
            public void onRestartRequested() {
                 Runnable restartTask = () -> {
                    try {
                        gameLoopManager.stop();
                        restartGame();
                    } catch (Exception e) {
                        e.printStackTrace();
                        showError("Restart Error", "Failed to restart the game.");
                    }
                };

                popupManager.saveScoreIfPending().thenRun(() -> Platform.runLater(restartTask))
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            Platform.runLater(restartTask); // 점수 저장에 실패해도 재시작은 실행
                            return null;
                        });
            }
        };
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

                // ✨ StackPane으로 감싸서 아이템 마커 오버레이 가능하도록
                javafx.scene.layout.StackPane cellPane = new javafx.scene.layout.StackPane();
                cellPane.getChildren().add(rect);
                cellPane.setAlignment(javafx.geometry.Pos.CENTER);
                cellPane.setPrefSize(UIConstants.CELL_SIZE, UIConstants.CELL_SIZE);
                cellPane.setMaxSize(UIConstants.CELL_SIZE, UIConstants.CELL_SIZE);
                cellPane.setMinSize(UIConstants.CELL_SIZE, UIConstants.CELL_SIZE);

                // GridPane에 StackPane 추가
                boardGridPane.add(cellPane, col, row);
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
        // 아이템 시스템이 활성화된 경우 아이템 키와 게임 키를 함께 처리
        if (itemInventoryPanel != null) {
            // Scene이 준비되면 키 이벤트 설정 (한 번만)
            if (boardGridPane.getScene() != null) {
                boardGridPane.getScene().setOnKeyPressed(this::handleAllKeyPress);
                System.out.println("⌨️  Keyboard controls enabled (with item support)");
            } else {
                boardGridPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null && oldScene == null) {
                        newScene.setOnKeyPressed(this::handleAllKeyPress);
                        System.out.println("⌨️  Keyboard controls enabled (with item support)");
                    }
                });
            }
        } else {
            // 일반 게임 모드는 InputHandler만 사용
            inputHandler.setupKeyboardControls(boardGridPane);
        }
    }
    
    /**
     * 모든 키 입력 처리 (게임 키 + 아이템 키)
     * Arcade 모드 전용
     */
    private void handleAllKeyPress(javafx.scene.input.KeyEvent event) {
        GameState state = boardController.getGameState();
        if (state.isGameOver() || state.isPaused()) {
            return; // 게임 오버 또는 일시정지 중에는 입력 무시
        }
        
        // 먼저 아이템 키 확인
        boolean isItemKey = false;
        switch (event.getCode()) {
            case DIGIT1:
            case NUMPAD1:
                itemInventoryPanel.useItemByKey(1);
                isItemKey = true;
                break;
            case DIGIT2:
            case NUMPAD2:
                itemInventoryPanel.useItemByKey(2);
                isItemKey = true;
                break;
            case DIGIT3:
            case NUMPAD3:
                itemInventoryPanel.useItemByKey(3);
                isItemKey = true;
                break;
            default:
                // 아이템 키가 아님
                break;
        }
        
        // 아이템 키가 아니면 일반 게임 키로 처리
        if (!isItemKey) {
            inputHandler.handleKeyPress(event);
        } else {
            event.consume();
        }
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
            
            // 3. Hold 업데이트 (테트로미노 타입 또는 아이템 타입이 변경된 경우)
            if (oldState.getHeldPiece() != newState.getHeldPiece() ||
                oldState.getHeldItemType() != newState.getHeldItemType()) {
                // 🔥 FIX: Hold된 아이템 정보도 함께 전달
                boardRenderer.drawHoldPiece(newState.getHeldPiece(), newState.getHeldItemType());
            }
            
            // 4. 점수/레벨/라인 업데이트
            gameInfoManager.updateAll(newState);
            gameLoopManager.updateDropSpeed(newState);
            
            // 4.5. 🔥 FIX: SPEED_RESET 아이템 플래그 처리
            if (newState.isSpeedResetRequested()) {
                // GameLoopManager의 dropInterval을 초기값으로 리셋
                gameLoopManager.updateDropSpeed(newState);
                newState.setSpeedResetRequested(false);
                System.out.println("⚡ [GameController] Speed reset processed");
            }
            
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
                
                // 아이템 드롭 시도 (아케이드 모드)
                tryDropItemOnLineClear(linesCleared);
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
            
            // 8. 아이템 드롭 감지 (라인 클리어 시)
            ItemType droppedItemType = newState.getNextBlockItemType();
            if (droppedItemType != null && itemInventoryPanel != null) {
                // 아이템이 드롭되었음 - 인벤토리에 추가
                seoultech.se.core.item.Item droppedItem = null;
                
                if (boardController.getGameEngine() instanceof seoultech.se.core.engine.ArcadeGameEngine) {
                    seoultech.se.core.engine.ArcadeGameEngine arcadeEngine = 
                        (seoultech.se.core.engine.ArcadeGameEngine) boardController.getGameEngine();
                    droppedItem = arcadeEngine.getItemManager().getItem(droppedItemType);
                }
                
                if (droppedItem != null) {
                    boolean added = itemInventoryPanel.addItem(droppedItem);
                    
                    if (added) {
                        // 아이템 획득 알림
                        String message = String.format("🎁 Got item: %s", droppedItem.getName());
                        notificationManager.showLineClearType(message);
                        System.out.println("✅ [GameController] Item dropped and added to inventory: " + droppedItem.getName());
                    } else {
                        // 인벤토리 가득 참
                        notificationManager.showLineClearType("⚠️ Inventory full!");
                        System.out.println("⚠️ [GameController] Item inventory full, item lost: " + droppedItem.getName());
                    }
                    
                    // 아이템을 사용했으므로 GameState에서 제거
                    newState.setNextBlockItemType(null);
                }
            }
            
            // 9. 레벨 업 감지
            int oldLevel = oldState.getLevel();
            int newLevel = newState.getLevel();
            if (newLevel > oldLevel) {
                notificationManager.showLineClearType("📈 LEVEL UP! - Level " + newLevel);
            }
            
            // 10. 일시정지 감지
            boolean wasPaused = oldState.isPaused();
            boolean isPaused = newState.isPaused();
            if (!wasPaused && isPaused) {
                pauseGame();
                popupManager.showPausePopup();
            } else if (wasPaused && !isPaused) {
                resumeGame();
            }
            
            // 11. 게임 오버 감지
            boolean wasGameOver = oldState.isGameOver();
            boolean isGameOver = newState.isGameOver();
            if (!wasGameOver && isGameOver) {
                processGameOver(newState.getScore());
            }
        });
    }

    // ========== UI 업데이트 헬퍼 메서드들 ==========
    // GameInfoManager로 이동됨

    // ========== 아이템 관리 ==========
    
    /**
     * 아이템 사용 처리
     * @param item 사용할 아이템
     * @param slotIndex 인벤토리 슬롯 인덱스
     */
    private void useItem(Item item, int slotIndex) {
        if (item == null) {
            System.out.println("⚠️ [GameController] Cannot use null item");
            return;
        }
        
        GameState currentState = boardController.getGameState();
        
        // 게임 오버 또는 일시정지 상태에서는 아이템 사용 불가
        if (currentState.isGameOver() || currentState.isPaused()) {
            System.out.println("⚠️ [GameController] Cannot use item: game over or paused");
            return;
        }
        
        // ArcadeGameEngine에서만 아이템 사용 가능
        if (!(boardController.getGameEngine() instanceof seoultech.se.core.engine.ArcadeGameEngine)) {
            System.out.println("⚠️ [GameController] Item system not available in this mode");
            notificationManager.showLineClearType("❌ Items not available in this mode");
            return;
        }
        
        // 현재 블록이 있는지 확인
        if (currentState.getCurrentTetromino() == null) {
            System.out.println("⚠️ [GameController] No current tetromino");
            notificationManager.showLineClearType("❌ No block to apply item");
            return;
        }
        
        // 이미 아이템이 적용된 블록인지 확인
        if (currentState.getCurrentItemType() != null) {
            System.out.println("⚠️ [GameController] Current block already has an item");
            notificationManager.showLineClearType("❌ Block already has an item");
            return;
        }
        
        // 아이템을 현재 블록에 적용
        currentState.setCurrentItemType(item.getType());
        
        System.out.println("🎨 [GameController] Before item application:");
        System.out.println("   - Current tetromino type: " + currentState.getCurrentTetromino().getType());
        System.out.println("   - Current item type: " + currentState.getCurrentItemType());
        
        // 인벤토리에서 아이템 제거
        itemInventoryPanel.removeItem(slotIndex);
        
        // 알림 표시
        String message = String.format("✨ %s applied! (Activates on lock)", item.getName());
        notificationManager.showLineClearType(message);
        System.out.println("✅ [GameController] Item applied to current block: " + item.getName());
        
        // 보드 업데이트 (아이템 블록 표시)
        Platform.runLater(() -> {
            boardRenderer.drawBoard(currentState);
        });
    }
    
    /**
     * 라인 클리어 시 아이템 드롭 시도
     * @param linesCleared 클리어된 라인 수
     */
    private void tryDropItemOnLineClear(int linesCleared) {
        if (itemInventoryPanel == null || linesCleared <= 0) {
            return;
        }
        
        // ArcadeGameEngine에서만 아이템 드롭 가능
        if (!(boardController.getGameEngine() instanceof seoultech.se.core.engine.ArcadeGameEngine)) {
            return;
        }
        
        // TODO: tryDropItem 메서드 구현 필요
        // 현재는 기능 비활성화
        /*
        Item droppedItem = ((seoultech.se.core.engine.ArcadeGameEngine)boardController.getGameEngine()).tryDropItem();
        
        if (droppedItem != null) {
            boolean added = itemInventoryPanel.addItem(droppedItem);
            
            if (added) {
                // 아이템 획득 알림
                String message = String.format("🎁 Got item: %s", droppedItem.getName());
                notificationManager.showLineClearType(message);
                System.out.println("✅ [GameController] Item dropped: " + droppedItem.getName());
            } else {
                // 인벤토리 가득 참
                notificationManager.showLineClearType("⚠️ Inventory full!");
                System.out.println("⚠️ [GameController] Item inventory full, item lost: " + droppedItem.getName());
            }
        }
        */
    }
    
    // ========== 게임 제어 ==========
    public void startGame() {
        gameOverLabel.setVisible(false);
        popupManager.hideAllPopups();
        gameLoopManager.start();
        boardGridPane.requestFocus();
        System.out.println("🎮 Game Started!");
    }

    public void pauseGame() {
        gameLoopManager.pause();
        notificationManager.showLineClearType("⏸️ PAUSED - Press P to resume");
    }

    public void resumeGame() {
        if (popupManager.isPausePopupVisible()) {
            popupManager.hidePausePopup();
        }
        gameLoopManager.resume();
        notificationManager.hideAllNotifications();
        // Resume Command 실행하여 게임 상태도 업데이트
        boardController.executeCommand(new seoultech.se.core.command.ResumeCommand());
    }

    // ========== 팝업 창 관리 ==========

    private void processGameOver(long finalScore) {
        gameLoopManager.stop();
        gameOverLabel.setVisible(true);

        boolean isItemMode = gameModeConfig.getItemConfig() != null && gameModeConfig.getItemConfig().isEnabled();
        popupManager.showGameOverPopup(finalScore, isItemMode, settingsService.getCurrentDifficulty());
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
    
    // ========== 오버레이 이벤트 핸들러 ==========
    
    @FXML
    public void handleResumeFromOverlay() {
        popupManager.handleResumeAction();
    }
    
    @FXML
    public void handleQuitFromOverlay() {
        popupManager.handleQuitAction();
    }
    
    @FXML
    public void handleMainFromOverlay() {
        popupManager.handleMainMenuAction();
    }
    
    @FXML
    public void handleRestartFromOverlay() {
        popupManager.handleRestartAction();
    }
    
    /**
     * 게임을 재시작합니다
     */
    private void restartGame() {
        try {
            System.out.println("🔄 Restarting game...");
            
            // 1. 게임 루프 정리
            if (gameLoopManager != null) {
                gameLoopManager.cleanup();
                System.out.println("   ✓ GameLoopManager cleaned up");
            }
            
            // 2. 키보드 이벤트 핸들러 제거
            javafx.scene.Scene currentScene = boardGridPane.getScene();
            if (currentScene != null) {
                currentScene.setOnKeyPressed(null);
                System.out.println("   ✓ Keyboard handlers removed");
            }
            
            // 3. 오버레이 숨기기
            popupManager.hideAllPopups();
            
            // 4. UI 요소 초기화 (gameOverLabel 숨기기)
            if (gameOverLabel != null) {
                gameOverLabel.setVisible(false);
                gameOverLabel.setManaged(false);
            }
            
            // 5. 게임 재초기화 (현재 gameModeConfig 유지)
            System.out.println("🎮 Reinitializing game with current config...");
            startInitialization();
            
            System.out.println("✅ Game restarted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            showError("재시작 오류", "게임을 재시작하는 데 실패했습니다.");
        }
    }
}

