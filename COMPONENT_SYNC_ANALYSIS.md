# 컴포넌트 동기화 및 데이터 흐름 분석 보고서

**분석 일시**: 2025년 11월 27일  
**분석 범위**: GameController, BoardController 및 하위 모든 컴포넌트  
**목적**: 이벤트 시점 전후 호출 순서, 동기화, 데이터 참조/복사 검증

---

## 📋 목차

1. [초기화 흐름 분석](#1-초기화-흐름-분석)
2. [게임 루프 틱 이벤트 체인](#2-게임-루프-틱-이벤트-체인)
3. [입력 처리 체인](#3-입력-처리-체인)
4. [UI 업데이트 동기화](#4-ui-업데이트-동기화)
5. [아이템 시스템 이벤트 흐름](#5-아이템-시스템-이벤트-흐름)
6. [애니메이션 동기화](#6-애니메이션-동기화)
7. [게임 오버/재시작 체인](#7-게임-오버재시작-체인)
8. [데이터 참조/복사 패턴](#8-데이터-참조복사-패턴)
9. [발견된 문제점 및 권장사항](#9-발견된-문제점-및-권장사항)

---

## 1. 초기화 흐름 분석

### 1.1 전체 초기화 순서

```
MainController.onGameStart()
    ↓
GameController.setGameMode(GameplayType, isMultiplayer)
    ↓ [gameModeConfig 생성]
GameController.startInitialization()
    ↓ [gameModeConfig null 검증 ✅]
BoardController 생성 (GameModeConfig + Difficulty 주입)
    ↓
GameController.initializeGridPane()
    ↓
GameController.initializePreviewPanes()
    ↓
GameController.initializeManagers()
    ├─ NotificationManager 생성
    ├─ BoardRenderer 생성
    ├─ GameLoopManager 생성
    │   └─ setCallback() ← 게임 틱 콜백 등록 ✅
    ├─ PopupManager 생성
    ├─ InputHandler 생성
    │   ├─ setCallback() ← 입력 처리 콜백 등록 ✅
    │   └─ setGameStateProvider() ← 상태 제공자 등록 ✅
    └─ GameInfoManager 생성
    ↓
GameController.initializeExecutionStrategy()
    ├─ [Singleplay] LocalExecutionStrategy 생성
    │   └─ BoardController.setExecutionStrategy()
    └─ [Multiplay] NetworkExecutionStrategy (세션 생성 후)
    ↓
GameController.initializeItemInventory()
    └─ [Arcade] ItemInventoryPanel 생성
        └─ setOnItemUse() ← 아이템 사용 콜백 등록 ✅
    ↓
GameController.setupKeyboardControls()
    └─ InputHandler.setupKeyboardControls()
    ↓
GameController.startGame()
    └─ GameLoopManager.start()
```

### 1.2 검증 결과

#### ✅ 올바른 점
1. **초기화 순서 보장**: `gameModeConfig` 검증 후 컴포넌트 생성
2. **Dependency Injection**: 모든 컴포넌트가 생성자 주입 방식
3. **콜백 등록**: 모든 이벤트 핸들러가 초기화 시점에 명확히 등록됨
4. **Fail-Fast**: `gameModeConfig` null 검증 (Line 201)

```java
if (gameModeConfig == null) {
    throw new IllegalStateException("GameModeConfig must be set before initialization.");
}
```

#### ⚠️ 주의 사항
1. **GameLoopManager 이중 검증**: `initializeManagers()`에서 `gameModeConfig` 재검증 (방어적이지만 중복)
2. **ExecutionStrategy 지연 설정**: Multiplay 모드에서는 세션 생성 후 설정 필요

---

## 2. 게임 루프 틱 이벤트 체인

### 2.1 자동 낙하 이벤트 흐름

```
AnimationTimer.handle(now)  [60fps, JavaFX Application Thread]
    ↓ [dropInterval 경과 체크]
GameLoopManager.callback.onTick()  ──────┐
    ↓                                      │
GameController (람다 콜백)                  │ 데이터 흐름
    ├─ GameState oldState = gameState.deepCopy()  ← 🔵 복사본
    ├─ new MoveCommand(Direction.DOWN) 생성
    ├─ BoardController.executeCommand(command)
    │   ├─ executionStrategy.execute(command, gameState)
    │   │   └─ LocalExecutionStrategy
    │   │       └─ GameEngine.executeCommand(command, currentState)
    │   │           └─ GameEngine.tryMoveDown(state, false)
    │   │               └─ 새로운 GameState 반환 ← 🟢 새 인스턴스
    │   └─ this.gameState = newState  ← 🟡 참조 업데이트
    └─ GameController.showUiHints(oldState, newState)
        └─ Platform.runLater(() -> { UI 업데이트 })
```

### 2.2 데이터 흐름 검증

| 단계 | 변수 | 타입 | 용도 |
|-----|------|------|------|
| 1 | `oldState` | 복사본 (deepCopy) | UI 변경 감지용 |
| 2 | `gameState` (BoardController) | 참조 | 현재 게임 상태 |
| 3 | `newState` (GameEngine 반환) | 새 인스턴스 | 업데이트된 상태 |
| 4 | `gameState` (업데이트 후) | 참조 업데이트 | newState를 가리킴 |

#### ✅ 올바른 점
1. **불변성 유지**: `oldState`는 `deepCopy()`로 분리 → UI 비교 시 안전
2. **GameEngine stateless**: 항상 새로운 `GameState` 반환
3. **동기화 보장**: `Platform.runLater()`로 UI 업데이트는 JavaFX 스레드에서 실행

#### ⚠️ 잠재적 문제
1. **deepCopy() 비용**: 60fps로 실행되는 게임 루프에서 매 틱마다 복사 → 성능 부담
   - **권장**: 변경 감지가 필요한 필드만 선택적 복사 또는 변경 플래그 사용

---

## 3. 입력 처리 체인

### 3.1 키보드 입력 이벤트 흐름

```
Scene.onKeyPressed(KeyEvent)  [JavaFX Event Thread]
    ↓
InputHandler.handleKeyPress(event)
    ├─ 🔒 inputEnabled 체크 (애니메이션 중 차단)  ← ✅ PRIORITY 3
    ├─ GameStateProvider.isGameOver() 체크
    ├─ GameStateProvider.isPaused() 체크 (PAUSE_RESUME만 허용)
    ├─ KeyMappingService.getAction(keyCode)
    └─ createCommandFromAction(action)
        ↓
InputHandler.callback.onCommandGenerated(command)  ────┐
    ↓                                                   │
GameController (람다 콜백)                               │ 데이터 흐름
    ├─ GameState oldState = gameState.deepCopy()  ← 🔵 복사본
    ├─ BoardController.executeCommand(command)
    │   └─ [게임 루프와 동일한 흐름]
    └─ GameController.showUiHints(oldState, newState)
```

### 3.2 입력 차단 메커니즘 (PRIORITY 3 수정 적용)

```
라인 클리어 애니메이션 시작
    ↓
InputHandler.setInputEnabled(false)  ← 🔒 입력 차단
    ↓
GameLoopManager.pause()  ← ⏸️ 게임 루프 일시정지
    ↓
CompletableFuture.delayedExecutor(300ms)
    ↓
애니메이션 종료
    ↓
InputHandler.setInputEnabled(true)  ← ✅ 입력 재활성화
    ↓
GameLoopManager.resume()  ← ▶️ 게임 루프 재개
```

#### ✅ 올바른 점
1. **이중 차단**: 게임 루프 + 입력 핸들러 모두 차단
2. **스레드 안전**: `volatile boolean inputEnabled`로 동시성 제어
3. **순서 보장**: 입력 차단 → 게임 루프 일시정지 → 애니메이션 → 입력 재활성화 → 게임 루프 재개

#### 🔍 검증 필요
1. **300ms 타이밍**: 실제 애니메이션 시간과 일치하는지 확인 필요
2. **재개 순서**: 입력 재활성화와 게임 루프 재개의 순서가 명확한지 (현재는 동일 `Platform.runLater` 블록)

---

## 4. UI 업데이트 동기화

### 4.1 showUiHints() 실행 흐름

```
GameController.showUiHints(oldState, newState)
    ↓
Platform.runLater(() -> {  ← JavaFX Application Thread로 전환
    │
    ├─ continueWithUiUpdates (Runnable)
    │   ├─ BoardRenderer.drawBoard(newState)  ← 🎨 보드 렌더링
    │   ├─ BoardRenderer.drawNextPiece(nextQueue[0])
    │   ├─ BoardRenderer.drawHoldPiece(heldPiece, heldItemType)
    │   ├─ GameInfoManager.updateAll(newState)  ← 📊 점수/레벨/라인
    │   ├─ GameLoopManager.updateDropSpeed(newState)  ← ⏱️ 낙하 속도
    │   ├─ NotificationManager.showLineClearType()  ← 🔔 알림
    │   ├─ NotificationManager.showCombo()
    │   ├─ NotificationManager.showBackToBack()
    │   ├─ [아이템 드롭 감지] ItemInventoryPanel.addItem()
    │   ├─ [레벨 업 감지] NotificationManager.showLineClearType()
    │   ├─ [일시정지 감지] pauseGame() / resumeGame()
    │   └─ [게임 오버 감지] processGameOver()
    │
    └─ [라인 클리어 시]
        ├─ 🔒 InputHandler.setInputEnabled(false)
        ├─ GameLoopManager.pause()
        ├─ 흰색 애니메이션 (300ms)
        └─ CompletableFuture → continueWithUiUpdates.run()
})
```

### 4.2 BoardRenderer 동기화

```java
BoardRenderer.drawBoard(GameState)
    ↓
    for (row, col) in grid:
        ├─ Cell cell = grid.getCell(row, col)  ← 🔵 참조
        └─ updateCellInternal(row, col, cell)
            ├─ Rectangle rect = cellRectangles[row][col]  ← 🟡 UI 참조
            ├─ rect.setFill(color)  ← 직접 수정
            ├─ clearItemMarkerOverlay(rect)
            └─ [아이템 마커] applyItemMarkerOverlay(rect, itemType)
```

#### ✅ 올바른 점
1. **UI 스레드 보장**: `Platform.runLater()`로 모든 UI 업데이트 래핑
2. **Cell 직접 참조**: `GameState.grid.getCell()`의 Cell을 직접 사용 → 복사 불필요
3. **아이템 오버레이 동기화**: `synchronized` 키워드 적용 (PRIORITY 5)

```java
private synchronized void applyItemMarkerOverlay(Rectangle rect, ItemType itemType) {
    // 중복 오버레이 방지 로직
}
```

#### ⚠️ 주의 사항
1. **Platform.runLater() 지연**: UI 업데이트가 실제로는 다음 프레임에 적용됨
   - 게임 로직(GameEngine)과 UI 렌더링 사이에 1프레임 지연 가능
   - 현재 게임 속도에서는 무시 가능한 수준

---

## 5. 아이템 시스템 이벤트 흐름

### 5.1 아이템 획득 → 사용 → 효과 적용 전체 체인

```
[1단계: 아이템 드롭 (GameEngine)]
ArcadeGameEngine.lockTetromino()
    ├─ itemManager.tryDropItem(linesCleared)
    ├─ newState.setNextBlockItemType(droppedItem)  ← 🎁 다음 블록에 아이템 설정
    └─ return newState

[2단계: 아이템 획득 감지 (GameController)]
GameController.showUiHints(oldState, newState)
    ├─ ItemType droppedItemType = newState.getNextBlockItemType()
    ├─ [droppedItemType != null]
    ├─ Item droppedItem = createItemFromType(droppedItemType)
    ├─ itemInventoryPanel.addItem(droppedItem)  ← 📦 인벤토리 추가
    └─ newState.setNextBlockItemType(null)  ← 🧹 클리어

[3단계: 아이템 사용 (사용자 입력)]
ItemInventoryPanel.onItemClick(slotIndex)
    ↓
ItemInventoryPanel.onItemUse.accept(item, slotIndex)
    ↓
GameController.useItem(item, slotIndex)
    ├─ GameState currentState = boardController.getGameState()  ← 🔵 참조
    ├─ currentState.setCurrentItemType(item.getType())  ← 🎨 현재 블록에 아이템 적용
    ├─ itemInventoryPanel.removeItem(slotIndex)  ← 🗑️ 인벤토리에서 제거
    ├─ notificationManager.showLineClearType("✨ Item applied!")
    └─ Platform.runLater(() -> boardRenderer.drawBoard(currentState))

[4단계: 아이템 효과 발동 (다음 락 시)]
BoardController.executeCommand(HardDropCommand)
    ↓
LocalExecutionStrategy.execute()
    ↓
ArcadeGameEngine.lockTetromino(state)
    ├─ ItemType currentItemType = state.getCurrentItemType()
    ├─ [WEIGHT_BOMB] → WeightBombItem.apply(state.deepCopy())  ← 🔵 복사본
    ├─ [LINE_CLEAR] → LineClearItem.clearLines(state)  ← 🔴 원본 수정
    ├─ [BOMB] → BombItem.apply(state)  ← 🔴 원본 수정
    ├─ [PLUS] → PlusItem.apply(state)  ← 🔴 원본 수정
    └─ return newState
```

### 5.2 데이터 참조/복사 패턴

| 아이템 타입 | GameState 처리 | 이유 |
|-----------|---------------|------|
| **WEIGHT_BOMB** | `deepCopy()` 사용 | 중력 적용 시뮬레이션이 복잡하여 원본 보호 |
| **LINE_CLEAR** | 원본 직접 수정 | 간단한 행 삭제 로직 |
| **BOMB** | 원본 직접 수정 | 폭발 범위 간단 |
| **PLUS** | 원본 직접 수정 | 블록 추가 로직 간단 |

#### ✅ 올바른 점
1. **아이템 상태 추적**: `currentItemType`, `nextBlockItemType` 명확히 분리
2. **인벤토리 동기화**: 아이템 추가/제거 즉시 UI 반영
3. **효과 발동 타이밍**: 블록 락 시점에 일괄 처리

#### ⚠️ 주의 사항
1. **혼합된 데이터 처리 방식**: WEIGHT_BOMB만 `deepCopy()`, 나머지는 원본 수정
   - **권장**: 모든 아이템을 동일한 방식으로 처리 (일관성)
   - 또는 명확한 주석으로 이유 문서화

---

## 6. 애니메이션 동기화

### 6.1 라인 클리어 애니메이션 타이밍

```
라인 클리어 감지 (linesWereCleared = true)
    ↓ [t=0ms]
🔒 InputHandler.setInputEnabled(false)
    ↓
⏸️ GameLoopManager.pause()
    ↓
🎨 흰색 애니메이션 표시 (cellRectangles[row][col].setFill(WHITE))
    ↓ [t=300ms]
CompletableFuture.delayedExecutor(300ms).execute(() -> {
    Platform.runLater(() -> {
        ✅ continueWithUiUpdates.run()
        ▶️ GameLoopManager.resume()
        🔓 InputHandler.setInputEnabled(true)
    })
})
```

### 6.2 검증 결과

#### ✅ 올바른 점
1. **입력 차단 완료**: 애니메이션 중 키 입력 완전 무시 (PRIORITY 3)
2. **게임 루프 정지**: 블록 낙하 중단
3. **타이밍 보장**: `CompletableFuture.delayedExecutor`로 300ms 정확히 대기

#### ⚠️ 개선 가능 사항
1. **하드코딩된 300ms**: 상수화 권장
   ```java
   private static final long ANIMATION_DURATION_MS = 300;
   ```
2. **애니메이션 취소 불가**: 중간에 취소할 수 있는 메커니즘 없음
3. **여러 애니메이션 동시 실행 시**: 입력 차단 카운터 필요할 수 있음

---

## 7. 게임 오버/재시작 체인

### 7.1 게임 오버 처리

```
GameState.isGameOver() = true 감지
    ↓
GameController.processGameOver(finalScore)
    ├─ 1. GameLoopManager.stop()  ← ⏹️ 게임 루프 중지
    ├─ 2. 🔒 InputHandler.setInputEnabled(false)
    ├─ 3. 🗑️ ItemInventoryPanel.clear()  ← 인벤토리 초기화
    ├─ 4. 📝 로그: "BoardController state will reset on restart"
    ├─ 5. gameOverLabel.setVisible(true)
    └─ 6. PopupManager.showGameOverPopup(finalScore, isItemMode, difficulty)
```

### 7.2 재시작 처리

```
PopupManager.onRestartRequested()
    ↓
GameController.restartGame()
    ├─ 1. 🧹 cleanupExecutionStrategy()
    │   ├─ executionStrategy = null
    │   ├─ opponentBoardView = null
    │   └─ isMultiplayerMode = false
    ├─ 2. 🗑️ GameLoopManager.cleanup()
    │   ├─ stop()
    │   ├─ gameLoop.stop() + gameLoop = null
    │   ├─ callback = null
    │   ├─ isRunning = false
    │   └─ isInitialized = false
    ├─ 3. 🔓 InputHandler.setInputEnabled(true)  ← ✅ 입력 재활성화
    ├─ 4. 🔌 키보드 핸들러 제거 (scene.setOnKeyPressed(null))
    ├─ 5. 🚫 PopupManager.hideAllPopups()
    ├─ 6. 👁️ gameOverLabel.setVisible(false)
    └─ 7. 🔄 startInitialization()  ← 전체 재초기화
```

### 7.3 검증 결과

#### ✅ 올바른 점
1. **완전한 리소스 정리**: GameLoopManager cleanup() 개선 (PRIORITY 1)
2. **입력 상태 리셋**: 게임 오버 시 차단 → 재시작 시 재활성화
3. **인벤토리 초기화**: 아이템 상태 깨끗이 정리
4. **재초기화 순서**: cleanup → 재활성화 → startInitialization

#### ⚠️ 주의 사항
1. **BoardController 재생성**: `startInitialization()`에서 새로운 인스턴스 생성
   - 기존 GameState는 GC 대상
   - 모든 참조가 끊어졌는지 확인 필요

---

## 8. 데이터 참조/복사 패턴

### 8.1 전체 패턴 요약

| 컴포넌트 | 메서드 | GameState 처리 | 목적 |
|---------|--------|---------------|------|
| **GameController** | `gameLoopManager callback` | `oldState = gameState.deepCopy()` | UI 변경 감지 |
| **GameController** | `inputHandler callback` | `oldState = gameState.deepCopy()` | UI 변경 감지 |
| **BoardController** | `executeCommand()` | `this.gameState = newState` (참조 업데이트) | 상태 동기화 |
| **LocalExecutionStrategy** | `execute()` | 참조 전달 | GameEngine 호출 |
| **GameEngine** | `executeCommand()` | 새 인스턴스 반환 | 불변성 유지 |
| **WeightBombItem** | `apply()` | `state.deepCopy()` | 시뮬레이션용 |
| **LineClearItem** | `clearLines()` | 원본 직접 수정 | 간단한 행 삭제 |
| **BoardRenderer** | `drawBoard()` | `grid.getCell()` 참조 | UI 동기화 |
| **GameController** | `useItem()` | `boardController.getGameState()` 참조 | 현재 상태 접근 |

### 8.2 복사 vs 참조 결정 기준

#### 🔵 deepCopy() 사용 시점
1. **변경 감지 필요**: UI 업데이트를 위한 oldState vs newState 비교
2. **시뮬레이션**: WeightBombItem의 중력 계산 (원본 보호)
3. **롤백 가능성**: 명령 실패 시 원본 복구

#### 🔴 참조 사용 시점
1. **읽기 전용 접근**: `boardController.getGameState()` 조회
2. **단방향 전달**: GameEngine으로 전달 (GameEngine이 새 인스턴스 반환)
3. **UI 렌더링**: Cell 참조를 Rectangle에 반영

#### ✅ 올바른 점
- **불변성 원칙**: GameEngine은 항상 새로운 GameState 반환
- **참조 투명성**: oldState는 deepCopy로 분리되어 안전

#### ⚠️ 개선 가능 사항
- **deepCopy() 성능**: 매 틱마다 실행 → 필드 레벨 복사로 최적화 고려

---

## 9. 발견된 문제점 및 권장사항

### 9.1 🟢 우수한 점

1. **명확한 책임 분리**
   - GameController: UI 조율
   - BoardController: 게임 로직 중개
   - GameEngine: 순수 게임 로직
   - 각 Manager: 특정 UI 영역 전담

2. **이벤트 체인 추적 가능**
   - 모든 콜백이 명확히 등록됨
   - 로깅이 잘 되어 있어 디버깅 용이

3. **동기화 보장**
   - Platform.runLater()로 UI 스레드 보장
   - synchronized로 Race condition 방지

4. **방어적 프로그래밍 적용**
   - Null 체크, 상태 검증 철저
   - Fail-fast 원칙 준수

### 9.2 🟡 개선 필요 사항

#### 문제 1: 혼합된 데이터 처리 방식
**현상**: WEIGHT_BOMB만 deepCopy, 나머지는 원본 수정
**영향**: 코드 이해도 저하, 유지보수 어려움
**권장**:
```java
// 모든 아이템을 동일한 방식으로 처리
public interface Item {
    GameState apply(GameState state); // 항상 새 인스턴스 반환
}
```

#### 문제 2: deepCopy() 성능 부담
**현상**: 게임 루프/입력 핸들러에서 매번 전체 복사
**영향**: 60fps 게임에서 불필요한 GC 압력
**권장**:
```java
// 변경 감지만 필요한 필드만 복사
class GameStateSnapshot {
    int score;
    int level;
    int linesCleared;
    // ... 비교 필요한 필드만
}
```

#### 문제 3: 애니메이션 타이밍 하드코딩
**현상**: 300ms가 여러 곳에 하드코딩
**영향**: 변경 시 누락 가능성
**권장**:
```java
public class UIConstants {
    public static final long LINE_CLEAR_ANIMATION_MS = 300;
}
```

### 9.3 🔴 수정 필요 (긴급도 낮음)

#### 문제 4: 초기화 순서 중복 검증
**현상**: gameModeConfig null 체크가 2번 (startInitialization, initializeManagers)
**영향**: 코드 중복, 약간의 성능 손실
**권장**: 첫 번째 검증만 유지하고 주석으로 보장 명시

#### 문제 5: NetworkExecutionStrategy cleanup 미구현
**현상**: TODO 주석만 있고 실제 구현 없음
**영향**: 멀티플레이 종료 시 WebSocket 연결 누수 가능
**권장**: NetworkExecutionStrategy에 cleanup() 메서드 구현

### 9.4 ✅ 최근 수정으로 해결된 문제

1. **GameLoopManager 리소스 누수** (PRIORITY 1) ✅
2. **애니메이션 중 입력 스택** (PRIORITY 3) ✅
3. **게임 오버 상태 불완전 리셋** (PRIORITY 6) ✅
4. **아이템 오버레이 race condition** (PRIORITY 5) ✅

---

## 10. 결론

### 전체 평가: ⭐⭐⭐⭐☆ (4/5)

**강점**:
- 컴포넌트 간 데이터 흐름이 명확하고 추적 가능
- 이벤트 체인이 잘 정의되어 있음
- UI 스레드 동기화가 철저함
- 최근 수정으로 주요 버그 해결됨

**약점**:
- 일부 데이터 처리 방식이 혼재 (deepCopy vs 원본 수정)
- 성능 최적화 여지 있음 (deepCopy 빈도)
- 애니메이션 타이밍 관리 개선 필요

**종합**:
전반적으로 **안정적이고 유지보수 가능한 구조**입니다. 발견된 문제들은 대부분 **성능 최적화와 코드 일관성** 측면이며, 기능적 버그는 거의 없습니다. 

**권장 조치**:
1. 혼합된 데이터 처리 방식 통일 (중기)
2. deepCopy() 최적화 고려 (장기)
3. 애니메이션 타이밍 상수화 (단기)
4. NetworkExecutionStrategy cleanup 구현 (멀티플레이 출시 전 필수)

---

**분석 완료 일시**: 2025년 11월 27일  
**분석자**: GitHub Copilot (Claude Sonnet 4.5)  
**문서 버전**: 1.0
