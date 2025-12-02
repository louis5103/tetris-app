# 싱글/멀티 플레이 UI 최적 설계 (최종안)

## 핵심 인사이트

### 이미 완성된 것 ✅
- **Strategy Pattern**: 로컬/네트워크 실행 로직 완전 분리
- **BoardController**: UI 독립적, Strategy 사용
- **GameController**: UI 제어 (입력 → Command → 렌더링)

### 추가 필요한 것
- **상대방 보드 렌더링만** (입력 처리 없음, 게임 루프 없음)

### 잘못된 접근들 ❌
1. ~~GameController에 상대방 로직 추가~~ → SRP 위반
2. ~~BaseController + Single/Multi 분리~~ → 과도한 설계, Strategy 중복

---

## ✅ 최적 해법: OpponentBoardView 컴포넌트 추가

### 아키텍처

```
GameController (하나, 기존 유지)
├── 내 게임 로직 (기존)
│   ├── BoardController (Strategy 사용)
│   ├── InputHandler
│   ├── GameLoopManager
│   └── BoardRenderer (내 보드)
│
└── 상대방 표시 (NEW, 간단)
    └── OpponentBoardView
        ├── GridPane (상대방 보드)
        └── BoardRenderer (상대방 보드)
```

**핵심**:
- GameController는 **그대로 유지** (책임 추가 없음)
- OpponentBoardView는 **독립 컴포넌트** (렌더링만)
- Callback으로 연결: `MultiPlayStrategies` → `GameController` → `OpponentBoardView`

---

## 구현

### 1. OpponentBoardView (간단한 UI 컴포넌트)

```java
package seoultech.se.client.ui;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Label;
import seoultech.se.core.GameState;
import seoultech.se.client.constants.UIConstants;
import seoultech.se.client.util.ColorMapper;

/**
 * 상대방 보드 표시 컴포넌트
 *
 * 책임:
 * - 상대방 보드 렌더링만 수행 (입력 처리 없음)
 * - 상대방 정보 표시 (점수, 레벨)
 *
 * 사용:
 * - 멀티플레이 모드에서만 활성화
 * - GameController가 GameState를 전달하면 렌더링
 */
public class OpponentBoardView extends VBox {
    private static final int CELL_SIZE = 15; // 작은 크기

    private final GridPane boardGrid;
    private final Label titleLabel;
    private final Label scoreLabel;
    private final Label levelLabel;

    private Rectangle[][] cellRectangles;
    private BoardRenderer boardRenderer;

    /**
     * 생성자
     */
    public OpponentBoardView() {
        super(10); // spacing
        this.setStyle("-fx-padding: 10; -fx-border-color: #444; -fx-border-width: 2;");

        // 타이틀
        titleLabel = new Label("OPPONENT");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        // 보드 GridPane
        boardGrid = new GridPane();
        boardGrid.setHgap(0);
        boardGrid.setVgap(0);

        // 정보 레이블
        scoreLabel = new Label("Score: 0");
        scoreLabel.setStyle("-fx-font-size: 12px;");

        levelLabel = new Label("Level: 1");
        levelLabel.setStyle("-fx-font-size: 12px;");

        // VBox에 추가
        this.getChildren().addAll(titleLabel, boardGrid, scoreLabel, levelLabel);

        // 초기화
        initializeBoard();
    }

    /**
     * 보드 GridPane 초기화
     */
    private void initializeBoard() {
        int width = 10;
        int height = 20;

        cellRectangles = new Rectangle[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Rectangle rect = new Rectangle(CELL_SIZE, CELL_SIZE);
                rect.setFill(ColorMapper.getEmptyCellColor());
                rect.setStroke(ColorMapper.getCellBorderColor());
                rect.setStrokeWidth(0.5);

                boardGrid.add(rect, col, row);
                cellRectangles[row][col] = rect;
            }
        }

        // BoardRenderer 생성 (hold, next 없음)
        boardRenderer = new BoardRenderer(cellRectangles, null, null, false);
    }

    /**
     * 상대방 GameState 업데이트
     *
     * @param opponentState 상대방의 게임 상태
     */
    public void update(GameState opponentState) {
        if (opponentState == null) return;

        // 보드 렌더링
        boardRenderer.drawBoard(opponentState);

        // 정보 업데이트
        scoreLabel.setText("Score: " + opponentState.getScore());
        levelLabel.setText("Level: " + opponentState.getLevel());
    }
}
```

---

### 2. GameController 수정 (최소한의 변경)

```java
// FXML 요소 추가 (하나만)
@FXML private HBox opponentContainer; // 멀티플레이에서만 보임

// 필드 추가 (하나만)
private OpponentBoardView opponentBoardView;

/**
 * 실행 전략 초기화
 */
private void initializeExecutionStrategy() {
    if (playType == null) {
        playType = PlayType.LOCAL_SINGLE;
    }

    if (playType == PlayType.ONLINE_MULTI) {
        // 멀티플레이: 상대방 보드 활성화
        enableOpponentBoard();
        System.out.println("ℹ️ Multiplay mode - waiting for session");
    } else {
        // 싱글플레이
        disableOpponentBoard();
        setupSingleplayMode();
    }
}

/**
 * 상대방 보드 활성화 (멀티플레이)
 */
private void enableOpponentBoard() {
    if (opponentContainer != null) {
        // OpponentBoardView 생성
        opponentBoardView = new OpponentBoardView();

        // 컨테이너에 추가
        opponentContainer.getChildren().clear();
        opponentContainer.getChildren().add(opponentBoardView);
        opponentContainer.setVisible(true);
        opponentContainer.setManaged(true);

        System.out.println("✅ Opponent board enabled");
    }
}

/**
 * 상대방 보드 비활성화 (싱글플레이)
 */
private void disableOpponentBoard() {
    if (opponentContainer != null) {
        opponentContainer.setVisible(false);
        opponentContainer.setManaged(false);
    }
    opponentBoardView = null;
}

/**
 * 상대방 상태 업데이트 (콜백)
 */
private void onOpponentStateUpdate(GameState opponentState) {
    if (opponentBoardView != null) {
        Platform.runLater(() -> {
            opponentBoardView.update(opponentState);
        });
    }
}

/**
 * Strategy 정리
 */
private void cleanupExecutionStrategy() {
    if (executionStrategy instanceof NetworkExecutionStrategy) {
        if (multiPlayStrategies != null) {
            multiPlayStrategies.disconnect();
        }
    }
    executionStrategy = null;
    opponentBoardView = null; // 정리
    System.out.println("   ✓ ExecutionStrategy cleaned up");
}
```

---

### 3. FXML 수정 (최소한의 변경)

```xml
<!-- game-view.fxml (하나만 사용) -->
<BorderPane>
    <center>
        <HBox styleClass="game-main-container">
            <!-- Hold 영역 (기존) -->
            <VBox styleClass="hold-container">
                <GridPane fx:id="holdGridPane"/>
            </VBox>

            <!-- 내 보드 (기존) -->
            <VBox>
                <GridPane fx:id="boardGridPane"/>
            </VBox>

            <!-- Next 영역 (기존) -->
            <VBox styleClass="next-container">
                <GridPane fx:id="nextGridPane"/>
            </VBox>

            <!-- 상대방 보드 컨테이너 (NEW, 기본 숨김) -->
            <HBox fx:id="opponentContainer"
                  visible="false"
                  managed="false">
                <!-- OpponentBoardView가 여기 동적으로 추가됨 -->
            </HBox>
        </HBox>
    </center>
</BorderPane>
```

---

## 비교: 제안들

### ❌ 제안 1: GameController에 직접 추가
```java
// GameController에서
private void onOpponentStateUpdate(GameState opponentState) {
    opponentBoardRenderer.drawBoard(opponentState);  // 책임 추가
    opponentScoreLabel.setText(...);                // 책임 추가
}
```
**문제**: GameController가 **내 보드 + 상대방 보드** 둘 다 관리 (SRP 위반)

---

### ❌ 제안 2: BaseController + Single/Multi 분리
```
BaseGameController (추상)
├── SingleGameController
└── MultiGameController
```
**문제**:
- Strategy Pattern과 **책임 중복**
- 불필요한 **복잡도 증가**
- 게임 루프, 입력 처리 등 공통 로직이 너무 많음

---

### ✅ 최종안: OpponentBoardView 컴포넌트
```java
// GameController는 그대로
private OpponentBoardView opponentBoardView; // 컴포넌트 하나 추가

private void onOpponentStateUpdate(GameState opponentState) {
    opponentBoardView.update(opponentState); // 위임만
}
```
**장점**:
- ✅ GameController 책임 변화 없음 (위임만)
- ✅ OpponentBoardView가 상대방 렌더링 전담 (SRP)
- ✅ Strategy Pattern과 보완적
- ✅ 간단하고 명확한 구조

---

## 책임 분리 확인

### GameController 책임 (변화 없음)
1. ✅ 내 게임 로직 제어 (BoardController)
2. ✅ 입력 처리 (InputHandler)
3. ✅ 게임 루프 (GameLoopManager)
4. ✅ 알림 (NotificationManager)
5. ✅ 내 보드 렌더링 위임 (BoardRenderer)
6. ✅ 상대방 보드 위임 (OpponentBoardView) ← 위임만 추가

### OpponentBoardView 책임 (NEW)
1. ✅ 상대방 보드 GridPane 관리
2. ✅ 상대방 보드 렌더링 (BoardRenderer 사용)
3. ✅ 상대방 정보 표시

### Strategy Pattern 책임 (기존 유지)
1. ✅ 로컬/네트워크 실행 분기
2. ✅ Client-side prediction
3. ✅ Server reconciliation
4. ✅ 상대방 상태 콜백

---

## 실행 흐름

### 싱글플레이
```
1. setGameModeConfig(config, LOCAL_SINGLE)
2. initializeExecutionStrategy()
   → disableOpponentBoard() // opponentContainer 숨김
   → setupSingleplayMode()
3. 게임 실행 (상대방 보드 없음)
```

### 멀티플레이
```
1. setGameModeConfig(config, ONLINE_MULTI)
2. initializeExecutionStrategy()
   → enableOpponentBoard() // OpponentBoardView 생성 및 추가
3. setupMultiplayMode(sessionId)
   → setOpponentStateCallback(this::onOpponentStateUpdate)
4. 게임 실행
5. 서버에서 상대방 상태 수신
   → onOpponentStateUpdate()
   → opponentBoardView.update(opponentState) // 렌더링만
```

---

## 결론

### 왜 이 방식이 최적인가?

1. **Strategy Pattern 존중**
   - 이미 실행 로직은 Strategy로 완벽 분리됨
   - Controller까지 분리는 과도한 설계

2. **단일 책임 원칙 (SRP)**
   - GameController: 내 게임 제어 + 위임
   - OpponentBoardView: 상대방 표시만
   - 명확한 책임 분리

3. **간결함**
   - 기존 GameController 거의 변경 없음
   - OpponentBoardView 하나만 추가
   - FXML도 최소 변경

4. **확장성**
   - 향후 관전 모드 등 추가 시에도 같은 패턴 사용 가능
   - 컴포넌트만 추가하면 됨

### 코드 변경 요약

**추가**:
- `OpponentBoardView.java` (100줄)

**수정**:
- `GameController.java` (30줄 추가)
- `game-view.fxml` (5줄 추가)

**변경 없음**:
- `BoardController.java` ✅
- Strategy 관련 클래스들 ✅
- 기존 게임 로직 ✅

---

## 최종 권장사항

**OpponentBoardView 컴포넌트 방식을 사용하세요!**

이유:
1. ✅ 간단하고 명확
2. ✅ SRP 준수
3. ✅ Strategy Pattern과 보완적
4. ✅ 최소한의 변경
5. ✅ 확장성 확보
