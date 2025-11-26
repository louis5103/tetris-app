package seoultech.se.client.controller;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
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
import seoultech.se.core.engine.item.Item;
import seoultech.se.core.engine.item.ItemType;
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

    // ✨ 상대방 보드 컨테이너 (멀티플레이)
    @FXML private HBox opponentContainer;

    @Autowired
    private KeyMappingService keyMappingService;

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private ScoreService scoreService;
    
    @Autowired
    private seoultech.se.client.service.GameModeConfigFactory configFactory;

    // 게임 로직 컨트롤러
    private BoardController boardController;

    // 게임 모드 설정
    private GameModeConfig gameModeConfig;

    // ✨ Strategy Pattern: 멀티플레이 여부 및 실행 전략
    private boolean isMultiplayerMode = false;
    private seoultech.se.client.strategy.GameExecutionStrategy executionStrategy;

    // UI 관리 클래스들
    private BoardRenderer boardRenderer;
    private NotificationManager notificationManager;
    private GameLoopManager gameLoopManager;
    private PopupManager popupManager;
    private InputHandler inputHandler;
    private GameInfoManager gameInfoManager;
    private ItemInventoryPanel itemInventoryPanel;

    // ✨ 상대방 보드 뷰 (멀티플레이)
    private seoultech.se.client.ui.OpponentBoardView opponentBoardView;

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
     * 게임 모드 설정 (NEW - ClientSettings + Difficulty 기반)
     * MainController에서 씬 전환 전에 호출됩니다
     * 
     * @param gameplayType 게임플레이 타입 (CLASSIC or ARCADE)
     * @param isMultiplayer 멀티플레이 모드 여부
     */
    public void setGameMode(seoultech.se.core.config.GameplayType gameplayType, boolean isMultiplayer) {
        this.isMultiplayerMode = isMultiplayer;
        
        // 현재 선택된 Difficulty 가져오기
        seoultech.se.core.model.enumType.Difficulty difficulty = settingsService.getCurrentDifficulty();
        
        // GameplayType + Difficulty → GameModeConfig 생성
        this.gameModeConfig = configFactory.create(gameplayType, difficulty);

        System.out.println("⚙️ Game mode set: " +
            gameplayType.getDisplayName() +
            ", Mode: " + (isMultiplayer ? "MULTIPLAYER" : "SINGLEPLAYER") +
            ", Difficulty: " + difficulty +
            ", SRS: " + gameModeConfig.isSrsEnabled() +
            ", Hard Drop: " + gameModeConfig.isHardDropEnabled() +
            ", Drop Speed: " + gameModeConfig.getDropSpeedMultiplier() + "x");
        
        if (gameplayType == seoultech.se.core.config.GameplayType.ARCADE) {
            System.out.println("🎯 [DEBUG] Arcade Item Config:");
            System.out.println("   - linesPerItem: " + gameModeConfig.getLinesPerItem());
            System.out.println("   - maxInventorySize: " + gameModeConfig.getMaxInventorySize());
            System.out.println("   - enabledItems: " + gameModeConfig.getEnabledItemTypes().size());
        }

        // 이제 실제 게임 초기화 수행
        startInitialization();
    }
    
    /**
     * 싱글플레이 게임 시작 (하위 호환성)
     */
    public void setGameMode(seoultech.se.core.config.GameplayType gameplayType) {
        setGameMode(gameplayType, false);
    }
    
    /**
     * 실제 게임 초기화를 수행합니다
     * setGameMode()에서 호출되어 config가 확실히 설정된 후 실행됩니다
     */
    private void startInitialization() {
        System.out.println("🚀 Starting game initialization with config...");
        
        // GameModeConfig 검증 (필수)
        if (gameModeConfig == null) {
            throw new IllegalStateException("GameModeConfig must be set before initialization. Call setGameMode() first.");
        }

        // GameModeConfig에 이미 포함된 Difficulty 사용 (중복 조회 제거)
        seoultech.se.core.model.enumType.Difficulty difficulty = gameModeConfig.getDifficulty();
        
        System.out.println("🎮 Creating BoardController with difficulty: " + difficulty.getDisplayName());
        
        // BoardController 생성 (GameModeConfig + Difficulty 전달)
        boardController = new BoardController(gameModeConfig, difficulty);
        
        GameState gameState = boardController.getGameState();
        System.out.println("📊 Board created: " + gameState.getBoardWidth() + "x" + gameState.getBoardHeight());
        System.out.println("   - Difficulty: " + difficulty.getDisplayName());

        // UI 초기화
        initializeGridPane(gameState);
        initializePreviewPanes();

        // UI 관리 클래스들 초기화
        initializeManagers();

        // ✨ Strategy 초기화 (플레이 타입에 따라)
        initializeExecutionStrategy();

        // 아이템 인벤토리 초기화 (아케이드 모드인 경우)
        initializeItemInventory();

        gameInfoManager.updateAll(gameState);
        setupKeyboardControls();
        startGame();

        System.out.println("✅ GameController initialization complete!");
    }

    /**
     * ✨ 실행 전략 초기화
     *
     * 플레이 타입에 따라 적절한 Strategy를 생성하고 BoardController에 설정합니다.
     * - Singleplay: LocalExecutionStrategy (GameEngine 직접 호출)
     * - Multiplay: NetworkExecutionStrategy (MultiPlayStrategies 사용)
     */
    private void initializeExecutionStrategy() {
        if (isMultiplayerMode) {
            // 멀티플레이: 상대방 보드 활성화
            enableOpponentBoard();
            System.out.println("ℹ️ Multiplay mode - Strategy will be set after session creation");
        } else {
            // 싱글플레이: 상대방 보드 비활성화
            disableOpponentBoard();
            setupSingleplayMode();
        }
    }

    /**
     * ✨ 싱글플레이 모드 설정
     */
    private void setupSingleplayMode() {
        // GameEngine은 GameExecutionStrategy가 관리 - BoardController를 통해 가져오지 않음
        seoultech.se.core.engine.factory.GameEngineFactory factory = 
            new seoultech.se.core.engine.factory.GameEngineFactory();
        seoultech.se.core.engine.GameEngine gameEngine = factory.createGameEngine(gameModeConfig);
        
        executionStrategy = new seoultech.se.client.strategy.LocalExecutionStrategy(gameEngine);
        boardController.setExecutionStrategy(executionStrategy);

        System.out.println("✅ Single-play mode initialized with LocalExecutionStrategy");
    }

    /**
     * ✨ 멀티플레이 모드 설정
     *
     * 세션 생성/매칭 성공 후 외부(매칭 화면 컨트롤러)에서 호출됩니다.
     *
     * @param networkStrategy 설정된 NetworkExecutionStrategy
     * @param sessionId STOMP 세션 ID
     */
    public void setupMultiplayMode(
            seoultech.se.client.strategy.NetworkExecutionStrategy networkStrategy,
            String sessionId) {
        if (networkStrategy == null) {
            throw new IllegalArgumentException("NetworkExecutionStrategy cannot be null");
        }

        if (boardController == null) {
            throw new IllegalStateException(
                "BoardController not initialized. " +
                "Call setGameModeConfig() before setupMultiplayMode()."
            );
        }

        this.executionStrategy = networkStrategy;

        // 초기 GameState 및 콜백과 함께 멀티플레이 모드 설정
        GameState initialState = boardController.getGameState();
        networkStrategy.setupMultiplayMode(
            sessionId,
            initialState,
            this::onOpponentStateUpdate,
            this::onAttackLinesReceived
        );

        // BoardController에 전략 설정
        boardController.setExecutionStrategy(executionStrategy);

        System.out.println("✅ Multi-play mode initialized - Session: " + sessionId);
    }

    /**
     * ✨ 상대방 보드 활성화 (멀티플레이)
     */
    private void enableOpponentBoard() {
        if (opponentContainer != null) {
            // OpponentBoardView 생성
            opponentBoardView = new seoultech.se.client.ui.OpponentBoardView();

            // 컨테이너에 추가
            opponentContainer.getChildren().clear();
            opponentContainer.getChildren().add(opponentBoardView);
            opponentContainer.setVisible(true);
            opponentContainer.setManaged(true);

            System.out.println("✅ Opponent board enabled");
        } else {
            System.out.println("⚠️ opponentContainer is null - cannot enable opponent board");
        }
    }

    /**
     * ✨ 상대방 보드 비활성화 (싱글플레이)
     */
    private void disableOpponentBoard() {
        if (opponentContainer != null) {
            opponentContainer.setVisible(false);
            opponentContainer.setManaged(false);
            opponentContainer.getChildren().clear();
        }
        opponentBoardView = null;
        System.out.println("✅ Opponent board disabled");
    }

    /**
     * ✨ 상대방 상태 업데이트 처리
     *
     * NetworkGameClient가 서버로부터 상대방 GameState를 받으면 호출됩니다.
     *
     * @param opponentState 상대방의 GameState
     */
    private void onOpponentStateUpdate(GameState opponentState) {
        if (opponentBoardView != null) {
            Platform.runLater(() -> {
                opponentBoardView.update(opponentState);
            });
        }
    }

    /**
     * ✨ 공격 라인 수신 처리
     *
     * NetworkGameClient가 서버로부터 공격 라인 정보를 받으면 호출됩니다.
     *
     * @param attackLines 받은 공격 라인 수
     */
    private void onAttackLinesReceived(int attackLines) {
        Platform.runLater(() -> {
            System.out.println("🛡️ [GameController] Received " + attackLines + " attack lines from opponent");

            // 보드에 방해 라인 추가
            GameState currentState = boardController.getGameState();
            boolean gameOver = currentState.addGarbageLines(attackLines);

            if (gameOver) {
                System.out.println("💀 [GameController] Game Over by attack!");
                processGameOver(currentState.getScore());
            } else {
                // 화면 갱신
                boardRenderer.drawBoard(currentState);
                notificationManager.showAttackNotification(attackLines);
            }
        });
    }

    /**
     * 아이템 인벤토리 초기화
     * 아케이드 모드일 때만 활성화됩니다
     */
    private void initializeItemInventory() {
        System.out.println("🔧 [GameController] Initializing item inventory...");
        System.out.println("   - gameModeConfig: " + gameModeConfig);
        System.out.println("   - linesPerItem: " + (gameModeConfig != null ? gameModeConfig.getLinesPerItem() : "null"));
        System.out.println("   - isEnabled: " + (gameModeConfig != null ? gameModeConfig.isItemSystemEnabled() : "N/A"));
        
        if (gameModeConfig != null && gameModeConfig.isItemSystemEnabled()) {
            int maxInventorySize = gameModeConfig.getMaxInventorySize();
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
                System.out.println("⚠️ [GameController] Game is over, stopping loop");
                return false; // 게임 루프 중지
            }
            
            if (gameState.isPaused()) {
                System.out.println("⏸️  [GameController] Game is paused, skipping tick");
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
            
            int oldLines = oldState.getLinesCleared();
            int newLines = newState.getLinesCleared();
            boolean linesWereCleared = newLines > oldLines;

            int width = newState.getBoardWidth();
            int height = newState.getBoardHeight();

            // 기존 UI 업데이트 로직을 Runnable로 캡슐화
            Runnable continueWithUiUpdates = () -> {
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
                
                // 5. 라인 클리어 감지 (이 부분은 알림 표시를 위해 유지)
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
                        case 1: message.append("SINGLE"); break;
                        case 2: message.append("DOUBLE"); break;
                        case 3: message.append("TRIPLE"); break;
                        case 4: message.append("TETRIS"); break;
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
                    seoultech.se.core.engine.item.Item droppedItem = null;
                    if (gameModeConfig != null && gameModeConfig.isItemSystemEnabled()) {
                        // ItemType으로 직접 Item 생성 (GameEngine 접근 불필요)
                        droppedItem = createItemFromType(droppedItemType);
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
            }; // End of continueWithUiUpdates Runnable

            if (linesWereCleared) {
                // 라인 클리어 애니메이션 처리
                System.out.println("DEBUG: Line clear detected. Starting animation logic.");
                gameLoopManager.pause();

                // 클리어된 라인 인덱스 찾기 (GameState에서 직접 가져오기)
                List<Integer> clearedRowIndices = java.util.Arrays.stream(newState.getLastClearedRows())
                                                                    .boxed()
                                                                    .collect(java.util.stream.Collectors.toList());
                System.out.println("DEBUG: Cleared row indices: " + clearedRowIndices);

                // 라인 클리어 시 셀을 흰색으로 변경하여 UI 반응성 확인
                for (int rowIndex : clearedRowIndices) {
                    for (int col = 0; col < width; col++) { 
                        if (cellRectangles[rowIndex][col] != null) {
                             cellRectangles[rowIndex][col].setFill(javafx.scene.paint.Color.WHITE);
                        }
                    }
                }

                // 애니메이션 시간만큼 대기
                CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() -> {
                    Platform.runLater(() -> {
                        System.out.println("DEBUG: Animation delay finished. Cleaning up animation.");
                        // 실제 UI 업데이트 수행 (나머지 기존 로직 실행)
                        continueWithUiUpdates.run();

                        // 게임 루프 재개 (일시정지 상태가 아니라면)
                        if (!boardController.getGameState().isPaused()) {
                            System.out.println("DEBUG: Resuming game loop.");
                            gameLoopManager.resume();
                        }
                    });
                });
            } else {
                // 애니메이션 없는 일반 업데이트 (나머지 기존 로직 실행)
                continueWithUiUpdates.run();
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
        
        // 아케이드 모드에서만 아이템 사용 가능
        if (gameModeConfig == null || !gameModeConfig.isItemSystemEnabled()) {
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
     * 
     * 참고: 아이템 드롭은 ArcadeGameEngine.lockTetromino()에서 자동으로 처리되며,
     * GameState.nextBlockItemType에 저장됩니다. 이 메서드는 현재 사용되지 않습니다.
     */
    private void tryDropItemOnLineClear(int linesCleared) {
        // 아이템 드롭은 GameEngine에서 자동 처리됨
        // showUiHints()에서 nextBlockItemType을 감지하여 인벤토리에 추가
    }
    
    /**
     * ItemType으로부터 Item 객체 생성
     * @param itemType 아이템 타입
     * @return 생성된 Item 객체 또는 null
     */
    private seoultech.se.core.engine.item.Item createItemFromType(seoultech.se.core.engine.item.ItemType itemType) {
        if (itemType == null) return null;
        
        try {
            // ItemType에 해당하는 Item 클래스 이름 가져오기
            String className = "seoultech.se.core.engine.item.concrete." + itemType.name();
            Class<?> itemClass = Class.forName(className);
            
            // 기본 생성자로 인스턴스 생성
            return (seoultech.se.core.engine.item.Item) itemClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("⚠️ Failed to create item from type: " + itemType + " - " + e.getMessage());
            return null;
        }
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

        boolean isItemMode = gameModeConfig != null && gameModeConfig.isItemSystemEnabled();
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
    /**
     * ✨ 게임 재시작
     *
     * 모든 상태를 초기화하고 같은 설정(gameModeConfig, playType)으로 재시작합니다.
     * Strategy도 다시 설정되어 완전히 새로운 게임이 시작됩니다.
     */
    private void restartGame() {
        try {
            System.out.println("🔄 Restarting game...");

            // 1. ✨ Strategy cleanup (네트워크 연결 등 정리)
            cleanupExecutionStrategy();

            // 2. 게임 루프 정리
            if (gameLoopManager != null) {
                gameLoopManager.cleanup();
                System.out.println("   ✓ GameLoopManager cleaned up");
            }

            // 3. 키보드 이벤트 핸들러 제거
            javafx.scene.Scene currentScene = boardGridPane.getScene();
            if (currentScene != null) {
                currentScene.setOnKeyPressed(null);
                System.out.println("   ✓ Keyboard handlers removed");
            }

            // 4. 오버레이 숨기기
            popupManager.hideAllPopups();

            // 5. UI 요소 초기화 (gameOverLabel 숨기기)
            if (gameOverLabel != null) {
                gameOverLabel.setVisible(false);
                gameOverLabel.setManaged(false);
            }

            // 6. ✨ 게임 재초기화 (gameModeConfig, playType 유지, Strategy 재설정)
            System.out.println("🎮 Reinitializing game with current config and playType...");
            startInitialization();

            System.out.println("✅ Game restarted successfully");
        } catch (Exception e) {
            e.printStackTrace();
            showError("재시작 오류", "게임을 재시작하는 데 실패했습니다.");
        }
    }

    /**
     * ✨ 실행 전략 정리
     *
     * 멀티플레이인 경우 네트워크 연결을 정리합니다.
     * Restart나 Quit 시 호출됩니다.
     * 
     * 참고: 네트워크 연결 정리는 매칭 화면 컨트롤러에서 처리해야 합니다.
     */
    private void cleanupExecutionStrategy() {
        executionStrategy = null;
        opponentBoardView = null; // 상대방 보드 뷰 정리
        System.out.println("   ✓ ExecutionStrategy cleaned up");
    }
}

