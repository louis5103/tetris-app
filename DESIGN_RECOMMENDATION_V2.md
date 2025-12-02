# 싱글/멀티 플레이 Controller 분리 설계 (수정안)

## 문제점 재인식

**기존 제안의 문제**:
```java
// GameController에서 모든 것을 관리 (SRP 위반!)
private void onOpponentStateUpdate(GameState opponentState) {
    // 내 보드 로직 + 상대방 보드 로직 모두 관리
    opponentBoardRenderer.drawBoard(opponentState);  // 추가 책임!
}
```

**현재 GameController의 책임**:
1. 내 게임 상태 관리
2. 내 보드 렌더링 위임 (BoardRenderer)
3. 게임 루프 관리 (GameLoopManager)
4. 입력 처리 (InputHandler)
5. 알림 관리 (NotificationManager)
6. 팝업 관리 (PopupManager)

➡️ **상대방 보드 관리까지 추가하면 책임 과다!**

---

## ✅ 개선안: 공통 로직 추상화 + Controller 분리

### 아키텍처

```
BaseGameController (추상 클래스)
├── 공통 로직: 게임 루프, 입력 처리, 알림, 팝업, 점수 저장
├── 추상 메서드: initializeGameBoard(), setupExecutionStrategy()
│
├── SingleGameController (구체 클래스)
│   └── 내 보드 하나만 관리
│
└── MultiGameController (구체 클래스)
    ├── 내 보드 관리 (PlayerBoardPanel)
    └── 상대방 보드 관리 (OpponentBoardPanel)
```

**핵심 원칙**:
- ✅ **공통 로직은 Base에 위치** (중복 제거)
- ✅ **차이점만 하위 클래스에서 구현** (Template Method Pattern)
- ✅ **각 Controller는 하나의 책임만** (SRP 준수)

---

## 구현 설계

### 1. BaseGameController (추상 클래스)

```java
@Component
public abstract class BaseGameController {
    // 공통 서비스
    @Autowired protected KeyMappingService keyMappingService;
    @Autowired protected NavigationService navigationService;
    @Autowired protected SettingsService settingsService;
    @Autowired protected ScoreService scoreService;

    // 공통 UI 매니저들
    protected NotificationManager notificationManager;
    protected GameLoopManager gameLoopManager;
    protected PopupManager popupManager;
    protected InputHandler inputHandler;

    // 게임 로직
    protected BoardController boardController;
    protected GameModeConfig gameModeConfig;
    protected PlayType playType;
    protected GameExecutionStrategy executionStrategy;

    // 공통 FXML 요소
    @FXML protected Label scoreLabel;
    @FXML protected Label levelLabel;
    @FXML protected Label linesLabel;
    @FXML protected HBox topEventLine;
    @FXML protected Label comboLabel;
    // ... 기타 공통 요소들

    /**
     * FXML 로드 후 호출 (공통 로직)
     */
    @FXML
    public void initialize() {
        System.out.println("🎮 BaseGameController initializing...");
        // 공통 초기화 로직
    }

    /**
     * 게임 모드 설정 (Template Method)
     */
    public final void setGameModeConfig(GameModeConfig config, PlayType playType) {
        this.gameModeConfig = config;
        this.playType = playType;

        // 1. BoardController 생성 (공통)
        createBoardController();

        // 2. 게임 보드 UI 초기화 (하위 클래스마다 다름)
        initializeGameBoard();

        // 3. 공통 매니저 초기화
        initializeCommonManagers();

        // 4. Strategy 초기화 (하위 클래스마다 다름)
        setupExecutionStrategy();

        // 5. 게임 시작
        startGame();
    }

    /**
     * BoardController 생성 (공통 로직)
     */
    private void createBoardController() {
        Difficulty difficulty = settingsService.getCurrentDifficulty();
        boardController = new BoardController(gameModeConfig, difficulty);
    }

    /**
     * 공통 매니저 초기화 (공통 로직)
     */
    private void initializeCommonManagers() {
        notificationManager = new NotificationManager(
            topEventLine, comboLabel, lineClearTypeLabel, backToBackLabel, lineClearNotificationLabel
        );

        gameLoopManager = new GameLoopManager(gameModeConfig.getDropSpeedMultiplier());
        gameLoopManager.setCallback(this::onGameTick);

        popupManager = new PopupManager(pauseOverlay, gameOverOverlay, finalScoreLabel);
        popupManager.setCallback(createPopupCallback());

        inputHandler = new InputHandler(keyMappingService);
        inputHandler.setCallback(this::onCommandExecuted);
    }

    /**
     * 게임 틱 처리 (공통 로직)
     */
    private boolean onGameTick() {
        GameState gameState = boardController.getGameState();

        if (gameState.isGameOver()) return false;
        if (gameState.isPaused()) return true;

        GameState oldState = gameState.deepCopy();
        GameState newState = boardController.executeCommand(new MoveCommand(Direction.DOWN));

        showUiHints(oldState, newState);
        return true;
    }

    /**
     * 명령 실행 후 처리 (공통 로직)
     */
    private void onCommandExecuted(GameCommand command) {
        GameState oldState = boardController.getGameState().deepCopy();
        GameState newState = boardController.executeCommand(command);
        showUiHints(oldState, newState);
    }

    /**
     * UI 힌트 업데이트 (공통 로직)
     */
    protected void showUiHints(GameState oldState, GameState newState) {
        Platform.runLater(() -> {
            // 보드 렌더링 (하위 클래스에서 구현)
            renderBoard(newState);

            // 점수/레벨/라인 업데이트 (공통)
            updateGameInfo(newState);

            // 라인 클리어 감지 (공통)
            detectLineClear(oldState, newState);

            // 콤보 감지 (공통)
            detectCombo(oldState, newState);

            // ... 기타 공통 감지 로직
        });
    }

    /**
     * 점수 저장 (공통 로직)
     */
    protected void saveScore(long finalScore) {
        // ScoreService를 사용한 점수 저장 로직
    }

    /**
     * 재시작 (공통 로직)
     */
    protected void restartGame() {
        cleanupExecutionStrategy();
        gameLoopManager.cleanup();
        // ... 공통 정리 로직

        // 재초기화
        setGameModeConfig(gameModeConfig, playType);
    }

    // ========== 추상 메서드 (하위 클래스에서 구현) ==========

    /**
     * 게임 보드 UI 초기화 (하위 클래스마다 다름)
     */
    protected abstract void initializeGameBoard();

    /**
     * 실행 전략 설정 (하위 클래스마다 다름)
     */
    protected abstract void setupExecutionStrategy();

    /**
     * 보드 렌더링 (하위 클래스마다 다름)
     */
    protected abstract void renderBoard(GameState newState);

    /**
     * Strategy 정리 (하위 클래스마다 다름)
     */
    protected abstract void cleanupExecutionStrategy();
}
```

---

### 2. SingleGameController (싱글플레이)

```java
@Component
public class SingleGameController extends BaseGameController {
    // 싱글플레이 전용 FXML 요소
    @FXML private GridPane boardGridPane;
    @FXML private GridPane holdGridPane;
    @FXML private GridPane nextGridPane;

    // 싱글플레이 전용 UI 매니저
    private BoardRenderer boardRenderer;
    private GameInfoManager gameInfoManager;
    private Rectangle[][] cellRectangles;
    private Rectangle[][] holdCellRectangles;
    private Rectangle[][] nextCellRectangles;

    @Override
    protected void initializeGameBoard() {
        GameState gameState = boardController.getGameState();
        int width = gameState.getBoardWidth();
        int height = gameState.getBoardHeight();

        // GridPane 초기화
        cellRectangles = new Rectangle[height][width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Rectangle rect = new Rectangle(UIConstants.CELL_SIZE, UIConstants.CELL_SIZE);
                rect.setFill(ColorMapper.getEmptyCellColor());
                boardGridPane.add(rect, col, row);
                cellRectangles[row][col] = rect;
            }
        }

        // Hold, Next 초기화
        initializePreviewPanes();

        // BoardRenderer 생성
        boardRenderer = new BoardRenderer(
            cellRectangles,
            holdCellRectangles,
            nextCellRectangles,
            settingsService.getColorBlindMode()
        );

        // GameInfoManager 생성
        gameInfoManager = new GameInfoManager(scoreLabel, levelLabel, linesLabel);
    }

    @Override
    protected void setupExecutionStrategy() {
        // 싱글플레이는 항상 LocalExecutionStrategy
        GameEngine gameEngine = boardController.getGameEngine();
        executionStrategy = new LocalExecutionStrategy(gameEngine);
        boardController.setExecutionStrategy(executionStrategy);

        System.out.println("✅ Single-play mode initialized");
    }

    @Override
    protected void renderBoard(GameState newState) {
        // 내 보드만 렌더링
        boardRenderer.drawBoard(newState);

        // Next Queue
        TetrominoType[] nextQueue = newState.getNextQueue();
        if (nextQueue != null && nextQueue.length > 0) {
            boardRenderer.drawNextPiece(nextQueue[0]);
        }

        // Hold
        if (needsHoldUpdate(newState)) {
            boardRenderer.drawHoldPiece(newState.getHeldPiece(), newState.getHeldItemType());
        }
    }

    @Override
    protected void cleanupExecutionStrategy() {
        executionStrategy = null;
        System.out.println("   ✓ ExecutionStrategy cleaned up");
    }

    private void initializePreviewPanes() {
        // Hold, Next 영역 초기화 로직
    }

    private boolean needsHoldUpdate(GameState newState) {
        // Hold 업데이트 필요 여부 판단
        return true; // 간단히 구현
    }
}
```

---

### 3. MultiGameController (멀티플레이)

```java
@Component
public class MultiGameController extends BaseGameController {
    @Autowired(required = false)
    private MultiPlayStrategies multiPlayStrategies;

    // 멀티플레이 전용 FXML 요소
    @FXML private VBox myBoardContainer;
    @FXML private VBox opponentBoardContainer;
    @FXML private GridPane myBoardGridPane;
    @FXML private GridPane opponentBoardGridPane;

    // 내 보드 UI
    private PlayerBoardPanel myBoardPanel;

    // 상대방 보드 UI
    private OpponentBoardPanel opponentBoardPanel;

    @Override
    protected void initializeGameBoard() {
        GameState gameState = boardController.getGameState();

        // 내 보드 패널 초기화 (책임 위임)
        myBoardPanel = new PlayerBoardPanel(
            myBoardGridPane,
            scoreLabel,
            levelLabel,
            linesLabel,
            settingsService.getColorBlindMode()
        );
        myBoardPanel.initialize(gameState);

        // 상대방 보드 패널 초기화 (책임 위임)
        opponentBoardPanel = new OpponentBoardPanel(
            opponentBoardGridPane,
            settingsService.getColorBlindMode()
        );
        opponentBoardPanel.initialize();
    }

    @Override
    protected void setupExecutionStrategy() {
        // 멀티플레이는 세션 생성 후 setupMultiplayMode() 호출 필요
        System.out.println("ℹ️ Multiplay mode - waiting for session");
    }

    /**
     * 멀티플레이 세션 설정 (외부 호출)
     */
    public void setupMultiplayMode(String sessionId) {
        if (multiPlayStrategies == null) {
            throw new IllegalStateException("MultiPlayStrategies not available");
        }

        // 1. 세션 초기화
        GameState initialState = boardController.getGameState();
        multiPlayStrategies.init(sessionId, initialState);

        // 2. 상대방 상태 콜백 설정
        multiPlayStrategies.setOpponentStateCallback(this::onOpponentStateUpdate);

        // 3. NetworkExecutionStrategy 설정
        executionStrategy = new NetworkExecutionStrategy(multiPlayStrategies);
        boardController.setExecutionStrategy(executionStrategy);

        System.out.println("✅ Multi-play mode initialized");
    }

    @Override
    protected void renderBoard(GameState newState) {
        // 내 보드만 렌더링 (책임 위임)
        myBoardPanel.render(newState);
    }

    /**
     * 상대방 상태 업데이트
     */
    private void onOpponentStateUpdate(GameState opponentState) {
        Platform.runLater(() -> {
            // 상대방 보드 렌더링 (책임 위임)
            opponentBoardPanel.render(opponentState);
        });
    }

    @Override
    protected void cleanupExecutionStrategy() {
        if (executionStrategy instanceof NetworkExecutionStrategy) {
            if (multiPlayStrategies != null) {
                multiPlayStrategies.disconnect();
            }
        }
        executionStrategy = null;
        System.out.println("   ✓ ExecutionStrategy cleaned up");
    }
}
```

---

### 4. 새로운 UI 컴포넌트: PlayerBoardPanel

```java
/**
 * 플레이어 보드 패널
 *
 * 책임:
 * - 내 보드의 GridPane 관리
 * - 내 보드 렌더링 위임 (BoardRenderer 사용)
 * - 점수/레벨/라인 정보 표시
 */
public class PlayerBoardPanel {
    private final GridPane boardGridPane;
    private final Label scoreLabel;
    private final Label levelLabel;
    private final Label linesLabel;

    private BoardRenderer boardRenderer;
    private Rectangle[][] cellRectangles;

    public PlayerBoardPanel(GridPane gridPane, Label scoreLabel,
                           Label levelLabel, Label linesLabel, boolean colorBlindMode) {
        this.boardGridPane = gridPane;
        this.scoreLabel = scoreLabel;
        this.levelLabel = levelLabel;
        this.linesLabel = linesLabel;
    }

    public void initialize(GameState initialState) {
        int width = initialState.getBoardWidth();
        int height = initialState.getBoardHeight();

        cellRectangles = new Rectangle[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Rectangle rect = new Rectangle(UIConstants.CELL_SIZE, UIConstants.CELL_SIZE);
                rect.setFill(ColorMapper.getEmptyCellColor());
                boardGridPane.add(rect, col, row);
                cellRectangles[row][col] = rect;
            }
        }

        boardRenderer = new BoardRenderer(cellRectangles, null, null, colorBlindMode);
    }

    public void render(GameState gameState) {
        boardRenderer.drawBoard(gameState);
        updateInfo(gameState);
    }

    private void updateInfo(GameState gameState) {
        scoreLabel.setText("Score: " + gameState.getScore());
        levelLabel.setText("Level: " + gameState.getLevel());
        linesLabel.setText("Lines: " + gameState.getLinesCleared());
    }
}
```

---

### 5. 새로운 UI 컴포넌트: OpponentBoardPanel

```java
/**
 * 상대방 보드 패널
 *
 * 책임:
 * - 상대방 보드의 GridPane 관리
 * - 상대방 보드 렌더링 (BoardRenderer 사용)
 * - 상대방 정보 표시 (선택적)
 */
public class OpponentBoardPanel {
    private final GridPane opponentGridPane;
    private BoardRenderer opponentRenderer;
    private Rectangle[][] opponentCellRectangles;

    public OpponentBoardPanel(GridPane gridPane, boolean colorBlindMode) {
        this.opponentGridPane = gridPane;
    }

    public void initialize() {
        int width = 10;
        int height = 20;

        opponentCellRectangles = new Rectangle[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                // 작은 크기로 렌더링
                Rectangle rect = new Rectangle(15, 15);
                rect.setFill(ColorMapper.getEmptyCellColor());
                opponentGridPane.add(rect, col, row);
                opponentCellRectangles[row][col] = rect;
            }
        }

        opponentRenderer = new BoardRenderer(opponentCellRectangles, null, null, false);
    }

    public void render(GameState opponentState) {
        opponentRenderer.drawBoard(opponentState);
    }
}
```

---

## FXML 구조

### game-single-view.fxml (싱글플레이)
```xml
<BorderPane fx:controller="seoultech.se.client.controller.SingleGameController">
    <center>
        <HBox>
            <!-- Hold -->
            <VBox>
                <GridPane fx:id="holdGridPane"/>
            </VBox>

            <!-- 내 보드 -->
            <GridPane fx:id="boardGridPane"/>

            <!-- Next -->
            <VBox>
                <GridPane fx:id="nextGridPane"/>
            </VBox>
        </HBox>
    </center>
</BorderPane>
```

### game-multi-view.fxml (멀티플레이)
```xml
<BorderPane fx:controller="seoultech.se.client.controller.MultiGameController">
    <center>
        <HBox>
            <!-- 내 보드 컨테이너 -->
            <VBox fx:id="myBoardContainer">
                <Label text="YOU"/>
                <GridPane fx:id="myBoardGridPane"/>
                <Label fx:id="scoreLabel"/>
            </VBox>

            <!-- 상대방 보드 컨테이너 -->
            <VBox fx:id="opponentBoardContainer">
                <Label text="OPPONENT"/>
                <GridPane fx:id="opponentBoardGridPane"/>
            </VBox>
        </HBox>
    </center>
</BorderPane>
```

---

## 장점

### ✅ 단일 책임 원칙 (SRP) 준수
- `SingleGameController`: 싱글플레이 UI만 관리
- `MultiGameController`: 멀티플레이 UI만 관리 (내 보드 + 상대방 보드)
- `PlayerBoardPanel`: 플레이어 보드만 관리
- `OpponentBoardPanel`: 상대방 보드만 관리

### ✅ 중복 코드 최소화
- `BaseGameController`에 공통 로직 집중
- Template Method Pattern으로 확장 포인트만 제공

### ✅ 유지보수성 향상
- 공통 로직 수정 시 Base만 수정
- 싱글/멀티 고유 로직은 각 Controller에서만 수정

### ✅ 테스트 용이성
- 각 Controller를 독립적으로 테스트 가능
- Panel 컴포넌트는 단위 테스트 가능

---

## 결론

**Controller 분리 + 공통 로직 추상화가 올바른 설계입니다.**

이 방식이:
1. ✅ 단일 책임 원칙 준수
2. ✅ 코드 중복 최소화
3. ✅ 확장성 확보
4. ✅ 유지보수성 향상

을 모두 달성합니다.
