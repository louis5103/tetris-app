# 싱글/멀티 플레이 UI 분기 처리 방안

## 현재 상태
- **BoardController**: 게임 로직 (UI 독립적) ✅
- **GameController**: JavaFX UI 제어
- **Strategy Pattern**: 로컬/네트워크 실행 분리 완료 ✅

## 문제
싱글플레이와 멀티플레이에서 UI 구조가 다름:
- **싱글**: 내 보드만 표시
- **멀티**: 내 보드 + 상대방 보드 표시

## ❌ 비권장: GameController 분리
```
GameController (싱글용)
MultiGameController (멀티용)
```

**문제점**:
- 중복 코드 대량 발생 (게임 루프, 키 입력, 렌더링, 아이템, 팝업 등)
- 유지보수 비용 2배
- 버그 발생 가능성 증가

## ✅ 권장: 조건부 UI 렌더링

### 방안 1: 동일 FXML + 조건부 Visibility (가장 간단)

**장점**:
- GameController 코드 변경 최소
- FXML 하나로 관리
- 런타임에 PlayType에 따라 UI 토글

**구조**:
```xml
<!-- game-view.fxml -->
<HBox>
    <!-- 내 보드 (항상 표시) -->
    <VBox fx:id="myBoardContainer">
        <GridPane fx:id="boardGridPane"/>
    </VBox>

    <!-- 상대방 보드 (멀티플레이만) -->
    <VBox fx:id="opponentBoardContainer"
          visible="false"
          managed="false">
        <Label text="OPPONENT"/>
        <GridPane fx:id="opponentBoardGridPane"/>
        <Label fx:id="opponentScoreLabel"/>
    </VBox>
</HBox>
```

**GameController 수정**:
```java
@FXML private VBox opponentBoardContainer;
@FXML private GridPane opponentBoardGridPane;
@FXML private Label opponentScoreLabel;

private void initializeExecutionStrategy() {
    if (playType == PlayType.ONLINE_MULTI) {
        // 멀티플레이: 상대방 보드 활성화
        opponentBoardContainer.setVisible(true);
        opponentBoardContainer.setManaged(true);
        initializeOpponentBoard(); // 상대방 GridPane 초기화

        System.out.println("ℹ️ Multiplay mode - Strategy will be set after session creation");
    } else {
        // 싱글플레이: 상대방 보드 숨김
        opponentBoardContainer.setVisible(false);
        opponentBoardContainer.setManaged(false);

        setupSingleplayMode();
    }
}

private void onOpponentStateUpdate(GameState opponentState) {
    Platform.runLater(() -> {
        // 상대방 보드 렌더링 (BoardRenderer 재사용)
        opponentBoardRenderer.drawBoard(opponentState);
        opponentScoreLabel.setText("Score: " + opponentState.getScore());
    });
}
```

---

### 방안 2: FXML 분리 + NavigationService

**장점**:
- UI 레이아웃 완전 분리 (싱글/멀티 레이아웃 독립적)
- FXML 파일 각각 최적화 가능

**단점**:
- FXML 파일 2개 관리
- NavigationService에서 PlayType에 따라 분기 필요

**구조**:
```
game-single-view.fxml  → GameController (playType = LOCAL_SINGLE)
game-multi-view.fxml   → GameController (playType = ONLINE_MULTI)
```

**MainController에서 분기**:
```java
public void startGame(GameModeConfig config, PlayType playType) {
    this.playType = playType;

    String fxmlPath = (playType == PlayType.ONLINE_MULTI)
        ? "/view/game-multi-view.fxml"
        : "/view/game-single-view.fxml";

    navigationService.navigateTo(fxmlPath);
    // GameController.setGameModeConfig(config, playType) 호출
}
```

---

## 🎯 최종 권장사항

**방안 1 (조건부 Visibility) 추천**

**이유**:
1. **코드 중복 최소화**: GameController 하나로 모든 로직 처리
2. **유지보수 용이**: 버그 수정 시 한 곳만 수정
3. **Strategy Pattern 활용**: 이미 로직은 분리되어 있음
4. **간단한 구현**: FXML에 컨테이너 추가만 하면 됨

**구현 단계**:
1. game-view.fxml에 `opponentBoardContainer` 추가 (기본 hidden)
2. GameController에 `opponentBoardRenderer` 추가
3. `initializeExecutionStrategy()`에서 PlayType 체크하여 visibility 설정
4. `onOpponentStateUpdate()`에서 상대방 보드 렌더링

---

## 코드 예시

### FXML 수정 (game-view.fxml)
```xml
<HBox styleClass="game-main-container" alignment="CENTER">
    <!-- Hold 영역 (기존) -->
    <VBox styleClass="hold-container">...</VBox>

    <!-- 내 보드 (기존) -->
    <VBox fx:id="myBoardContainer">
        <StackPane fx:id="gameStackPane">
            <GridPane fx:id="boardGridPane"/>
            <!-- ... 기존 요소들 ... -->
        </StackPane>
        <HBox fx:id="itemInventoryContainer"/>
    </VBox>

    <!-- 상대방 보드 (신규, 멀티플레이만) -->
    <VBox fx:id="opponentBoardContainer"
          styleClass="opponent-container"
          alignment="TOP_CENTER"
          visible="false"
          managed="false">
        <Label text="OPPONENT" styleClass="info-label-title"/>
        <GridPane fx:id="opponentBoardGridPane" styleClass="game-board"/>
        <VBox styleClass="opponent-info">
            <Label fx:id="opponentScoreLabel"/>
            <Label fx:id="opponentLevelLabel"/>
            <Label fx:id="opponentLinesLabel"/>
        </VBox>
    </VBox>

    <!-- Next 영역 (기존) -->
    <VBox styleClass="next-container">...</VBox>
</HBox>
```

### GameController 수정
```java
// FXML 요소 추가
@FXML private VBox opponentBoardContainer;
@FXML private GridPane opponentBoardGridPane;
@FXML private Label opponentScoreLabel;
@FXML private Label opponentLevelLabel;
@FXML private Label opponentLinesLabel;

// 필드 추가
private Rectangle[][] opponentCellRectangles;
private BoardRenderer opponentBoardRenderer;

private void initializeExecutionStrategy() {
    if (playType == null) {
        playType = PlayType.LOCAL_SINGLE;
    }

    if (playType == PlayType.ONLINE_MULTI) {
        // 상대방 보드 UI 활성화
        showOpponentBoard();
        System.out.println("ℹ️ Multiplay mode - waiting for session");
    } else {
        // 싱글플레이
        hideOpponentBoard();
        setupSingleplayMode();
    }
}

private void showOpponentBoard() {
    if (opponentBoardContainer != null) {
        opponentBoardContainer.setVisible(true);
        opponentBoardContainer.setManaged(true);

        // 상대방 보드 GridPane 초기화
        initializeOpponentGridPane();

        System.out.println("✅ Opponent board UI enabled");
    }
}

private void hideOpponentBoard() {
    if (opponentBoardContainer != null) {
        opponentBoardContainer.setVisible(false);
        opponentBoardContainer.setManaged(false);
    }
}

private void initializeOpponentGridPane() {
    int width = 10;  // 표준 테트리스 보드
    int height = 20;

    opponentBoardGridPane.setHgap(0);
    opponentBoardGridPane.setVgap(0);

    opponentCellRectangles = new Rectangle[height][width];

    for (int row = 0; row < height; row++) {
        for (int col = 0; col < width; col++) {
            Rectangle rect = new Rectangle(20, 20); // 작은 크기
            rect.setFill(ColorMapper.getEmptyCellColor());
            rect.setStroke(ColorMapper.getCellBorderColor());

            opponentBoardGridPane.add(rect, col, row);
            opponentCellRectangles[row][col] = rect;
        }
    }

    // 상대방 전용 BoardRenderer 생성
    opponentBoardRenderer = new BoardRenderer(
        opponentCellRectangles,
        null, // hold 없음
        null, // next 없음
        settingsService.getColorBlindMode()
    );
}

private void onOpponentStateUpdate(GameState opponentState) {
    Platform.runLater(() -> {
        // 상대방 보드 렌더링
        opponentBoardRenderer.drawBoard(opponentState);

        // 상대방 정보 업데이트
        opponentScoreLabel.setText("Score: " + opponentState.getScore());
        opponentLevelLabel.setText("Level: " + opponentState.getLevel());
        opponentLinesLabel.setText("Lines: " + opponentState.getLinesCleared());

        System.out.println("👥 [GameController] Opponent board rendered");
    });
}
```

---

## 결론

**GameController와 BoardController를 분리하지 마세요!**

대신:
1. ✅ **FXML에 조건부 UI 컨테이너 추가**
2. ✅ **GameController에서 PlayType에 따라 visibility 제어**
3. ✅ **기존 Strategy Pattern 활용** (이미 로직은 분리됨)

이 방식이 가장 깔끔하고 유지보수하기 쉬운 구조입니다.
