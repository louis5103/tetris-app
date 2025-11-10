# Settings System Refactoring Plan

## 📊 현재 상황 분석

### 🔴 발견된 문제점

1. **설정 적용 안 됨**
   - 설정은 저장/로드되지만, 게임 시작 시 반영되지 않음
   - 원인: `GameController.initialize()`가 FXML 로드 시 자동 호출되는데, 그때 `gameModeConfig`가 `null`
   - `setGameModeConfig()`는 나중에 호출되지만, 이미 `BoardController`는 기본값으로 생성된 후

2. **책임 분산 (Separation of Concerns 위반)**
   - `SettingsService`: 전역 설정 + 게임 모드 설정 혼재 (400+ lines)
   - `MainController`: 설정 다이얼로그 UI 로직 포함 (300+ lines)
   - `GameController`: 일부 설정 적용
   - `BoardController`: Hard Drop, Hold 체크
   - → **단일 책임 원칙(SRP) 위반**

3. **설정 흐름 복잡도**
   ```
   MainController.showModeSettingsDialog()
       ↓
   SettingsService.loadCustomGameModeConfig()
       ↓
   [사용자가 설정 변경]
       ↓
   SettingsService.saveCustomGameModeConfig()
       ↓
   SettingsService.saveGameModeSettings()
       ↓
   SettingsService.saveSettings() ← 여기서 custom.* 키 손실 위험
   ```

### ✅ 잘된 점

- `GameModeConfig`는 불변(immutable) 객체로 잘 설계됨
- Builder 패턴 적용으로 유연성 확보
- `SettingsService`의 `saveCustomGameModeConfig()`, `loadCustomGameModeConfig()` 메서드는 정상 작동
- `GameLoopManager`에 Drop Speed Multiplier 이미 적용됨

---

## 🎯 리팩토링 목표

### 1. **SOLID 원칙 준수**
- **Single Responsibility Principle (SRP)**: 각 클래스는 하나의 책임만
- **Open/Closed Principle (OCP)**: 확장에는 열려있고 수정에는 닫혀있게
- **Dependency Inversion Principle (DIP)**: 추상화에 의존

### 2. **모던 아키텍처 패턴**
- **Service Layer**: 비즈니스 로직 분리
- **Facade Pattern**: 복잡한 설정 시스템을 간단한 인터페이스로 제공
- **Strategy Pattern**: 게임 모드별 설정 전략 분리

### 3. **테스트 가능성**
- 각 컴포넌트 독립적으로 테스트 가능
- Mock 객체 활용 가능한 구조

---

## 📋 실행 계획 (Phase별)

### **Phase 1: 즉시 수정 - 설정 적용 문제 해결** ✅ **완료**

#### 1.1 GameController 초기화 순서 수정
```java
// 변경 전
initialize() {
    boardController = new BoardController(gameModeConfig); // null!
}

// 변경 후
initialize() {
    // UI 준비만
}

setGameModeConfig(config) {
    this.gameModeConfig = config;
    startInitialization(); // 실제 초기화
}

private startInitialization() {
    boardController = new BoardController(gameModeConfig); // config 확정 후 생성
    // 나머지 초기화...
}
```

**상태**: ✅ 완료  
**커밋**: 준비 중  
**검증 방법**: 
1. Hard Drop OFF 설정 → 게임에서 스페이스바 눌러도 작동 안 함
2. Drop Speed 0.5x 설정 → 블록이 천천히 떨어짐
3. Hold OFF 설정 → C키 눌러도 작동 안 함

---

### **Phase 2: 아키텍처 개선 - 책임 분리**

#### 2.1 GameModeConfigManager 서비스 생성

**파일**: `tetris-client/src/main/java/seoultech/se/client/service/GameModeConfigManager.java`

**책임**:
- GameModeConfig의 라이프사이클 관리
- 모드별 설정 저장/로드
- 기본값 제공
- 유효성 검증

**주요 메서드**:
```java
@Service
public class GameModeConfigManager {
    
    @Autowired
    private SettingsService settingsService; // 파일 I/O 위임
    
    /**
     * 특정 게임플레이 타입의 설정 로드
     * 커스텀 설정이 없으면 기본값 반환
     */
    public GameModeConfig loadConfigForGameplay(GameplayType gameplayType) {
        GameModeConfig custom = settingsService.loadCustomGameModeConfig(gameplayType);
        return custom != null ? custom : getDefaultConfig(gameplayType);
    }
    
    /**
     * 설정 저장
     */
    public void saveConfigForGameplay(GameplayType gameplayType, GameModeConfig config) {
        validateConfig(config);
        settingsService.saveCustomGameModeConfig(gameplayType, config);
        settingsService.saveGameModeSettings(gameplayType, config.getPlayType(), config.isSrsEnabled());
    }
    
    /**
     * 기본 설정 제공
     */
    public GameModeConfig getDefaultConfig(GameplayType gameplayType) {
        return switch (gameplayType) {
            case CLASSIC -> GameModeConfig.classic();
            case ARCADE -> GameModeConfig.arcade();
            default -> GameModeConfig.classic();
        };
    }
    
    /**
     * 설정 유효성 검증
     */
    public void validateConfig(GameModeConfig config) throws IllegalArgumentException {
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        if (config.getDropSpeedMultiplier() < 0.1 || config.getDropSpeedMultiplier() > 10.0) {
            throw new IllegalArgumentException("Drop speed multiplier must be between 0.1 and 10.0");
        }
        // 추가 검증...
    }
    
    /**
     * 설정 병합 (기본값 + 커스텀)
     */
    public GameModeConfig mergeWithDefaults(GameplayType gameplayType, GameModeConfig partial) {
        GameModeConfig defaults = getDefaultConfig(gameplayType);
        return GameModeConfig.builder()
            .hardDropEnabled(partial.isHardDropEnabled() != defaults.isHardDropEnabled() 
                ? partial.isHardDropEnabled() 
                : defaults.isHardDropEnabled())
            // ... 나머지 필드
            .build();
    }
}
```

**우선순위**: HIGH  
**예상 작업 시간**: 2시간  
**의존성**: SettingsService

---

#### 2.2 GameSettingsDialogService 생성

**파일**: `tetris-client/src/main/java/seoultech/se/client/service/GameSettingsDialogService.java`

**책임**:
- 설정 다이얼로그 UI 생성 및 표시
- 사용자 입력 수집
- 설정 객체 반환

**주요 메서드**:
```java
@Service
public class GameSettingsDialogService {
    
    @Autowired
    private GameModeConfigManager configManager;
    
    /**
     * 설정 다이얼로그 표시 및 결과 반환
     * 
     * @param gameplayType 게임플레이 타입
     * @return 사용자가 설정한 GameModeConfig, 취소 시 null
     */
    public Optional<GameModeConfig> showSettingsDialog(GameplayType gameplayType) {
        GameModeConfig currentConfig = configManager.loadConfigForGameplay(gameplayType);
        
        Dialog<GameModeConfig> dialog = new Dialog<>();
        dialog.setTitle("Game Settings - " + gameplayType.name());
        dialog.setHeaderText("Customize your game experience");
        
        // UI 생성
        GridPane grid = createDialogContent(currentConfig);
        dialog.getDialogPane().setContent(grid);
        
        // 버튼 설정
        ButtonType applyButton = new ButtonType("Apply", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButton, ButtonType.CANCEL);
        
        // 결과 변환
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == applyButton) {
                return buildConfigFromDialog(grid, gameplayType);
            }
            return null;
        });
        
        return dialog.showAndWait();
    }
    
    private GridPane createDialogContent(GameModeConfig config) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // SRS 체크박스
        CheckBox srsCheckBox = new CheckBox();
        srsCheckBox.setSelected(config.isSrsEnabled());
        srsCheckBox.setId("srsCheckBox");
        grid.add(new Label("Super Rotation System (SRS):"), 0, 0);
        grid.add(srsCheckBox, 1, 0);
        
        // 180° 회전
        CheckBox rotation180CheckBox = new CheckBox();
        rotation180CheckBox.setSelected(config.isRotation180Enabled());
        rotation180CheckBox.setId("rotation180CheckBox");
        grid.add(new Label("180° Rotation:"), 0, 1);
        grid.add(rotation180CheckBox, 1, 1);
        
        // Hard Drop
        CheckBox hardDropCheckBox = new CheckBox();
        hardDropCheckBox.setSelected(config.isHardDropEnabled());
        hardDropCheckBox.setId("hardDropCheckBox");
        grid.add(new Label("Hard Drop (Space):"), 0, 2);
        grid.add(hardDropCheckBox, 1, 2);
        
        // Hold
        CheckBox holdCheckBox = new CheckBox();
        holdCheckBox.setSelected(config.isHoldEnabled());
        holdCheckBox.setId("holdCheckBox");
        grid.add(new Label("Hold (C):"), 0, 3);
        grid.add(holdCheckBox, 1, 3);
        
        // Ghost Piece
        CheckBox ghostCheckBox = new CheckBox();
        ghostCheckBox.setSelected(config.isGhostPieceEnabled());
        ghostCheckBox.setId("ghostCheckBox");
        grid.add(new Label("Ghost Piece:"), 0, 4);
        grid.add(ghostCheckBox, 1, 4);
        
        // Drop Speed Slider
        Slider dropSpeedSlider = new Slider(0.5, 2.0, config.getDropSpeedMultiplier());
        dropSpeedSlider.setShowTickLabels(true);
        dropSpeedSlider.setShowTickMarks(true);
        dropSpeedSlider.setMajorTickUnit(0.5);
        dropSpeedSlider.setBlockIncrement(0.1);
        dropSpeedSlider.setId("dropSpeedSlider");
        Label dropSpeedValueLabel = new Label(String.format("%.1fx", config.getDropSpeedMultiplier()));
        dropSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            dropSpeedValueLabel.setText(String.format("%.1fx", newVal.doubleValue()));
        });
        grid.add(new Label("Drop Speed:"), 0, 5);
        grid.add(dropSpeedSlider, 1, 5);
        grid.add(dropSpeedValueLabel, 2, 5);
        
        // Soft Drop Speed Slider
        Slider softDropSlider = new Slider(10.0, 30.0, config.getSoftDropSpeed());
        softDropSlider.setShowTickLabels(true);
        softDropSlider.setShowTickMarks(true);
        softDropSlider.setMajorTickUnit(5.0);
        softDropSlider.setId("softDropSlider");
        Label softDropValueLabel = new Label(String.format("%.0fx", config.getSoftDropSpeed()));
        softDropSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            softDropValueLabel.setText(String.format("%.0fx", newVal.doubleValue()));
        });
        grid.add(new Label("Soft Drop Speed:"), 0, 6);
        grid.add(softDropSlider, 1, 6);
        grid.add(softDropValueLabel, 2, 6);
        
        // Lock Delay Slider
        Slider lockDelaySlider = new Slider(0, 1000, config.getLockDelay());
        lockDelaySlider.setShowTickLabels(true);
        lockDelaySlider.setShowTickMarks(true);
        lockDelaySlider.setMajorTickUnit(250);
        lockDelaySlider.setBlockIncrement(50);
        lockDelaySlider.setId("lockDelaySlider");
        Label lockDelayValueLabel = new Label(config.getLockDelay() + "ms");
        lockDelaySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lockDelayValueLabel.setText(newVal.intValue() + "ms");
        });
        grid.add(new Label("Lock Delay:"), 0, 7);
        grid.add(lockDelaySlider, 1, 7);
        grid.add(lockDelayValueLabel, 2, 7);
        
        return grid;
    }
    
    private GameModeConfig buildConfigFromDialog(GridPane grid, GameplayType gameplayType) {
        CheckBox srsCheckBox = (CheckBox) grid.lookup("#srsCheckBox");
        CheckBox rotation180CheckBox = (CheckBox) grid.lookup("#rotation180CheckBox");
        CheckBox hardDropCheckBox = (CheckBox) grid.lookup("#hardDropCheckBox");
        CheckBox holdCheckBox = (CheckBox) grid.lookup("#holdCheckBox");
        CheckBox ghostCheckBox = (CheckBox) grid.lookup("#ghostCheckBox");
        Slider dropSpeedSlider = (Slider) grid.lookup("#dropSpeedSlider");
        Slider softDropSlider = (Slider) grid.lookup("#softDropSlider");
        Slider lockDelaySlider = (Slider) grid.lookup("#lockDelaySlider");
        
        return GameModeConfig.builder()
            .gameplayType(gameplayType)
            .srsEnabled(srsCheckBox.isSelected())
            .rotation180Enabled(rotation180CheckBox.isSelected())
            .hardDropEnabled(hardDropCheckBox.isSelected())
            .holdEnabled(holdCheckBox.isSelected())
            .ghostPieceEnabled(ghostCheckBox.isSelected())
            .dropSpeedMultiplier(dropSpeedSlider.getValue())
            .softDropSpeed(softDropSlider.getValue())
            .lockDelay((int) lockDelaySlider.getValue())
            .build();
    }
}
```

**우선순위**: HIGH  
**예상 작업 시간**: 3시간  
**의존성**: GameModeConfigManager

---

#### 2.3 MainController 슬림화

**현재**: 413 lines  
**목표**: ~200 lines

**변경사항**:
```java
@FXML
private void handleSettingsIconClick(ActionEvent event) {
    // 변경 전: 300+ lines의 다이얼로그 생성 코드
    
    // 변경 후: Service에 위임
    GameplayType gameplayType = determineGameplayType(event);
    
    Optional<GameModeConfig> newConfig = gameSettingsDialogService.showSettingsDialog(gameplayType);
    
    newConfig.ifPresent(config -> {
        configManager.saveConfigForGameplay(gameplayType, config);
        showSuccessNotification("Settings saved successfully!");
    });
}
```

**우선순위**: MEDIUM  
**예상 작업 시간**: 1시간  
**의존성**: GameSettingsDialogService

---

#### 2.4 SettingsService 슬림화

**현재**: 400+ lines  
**목표**: ~200 lines

**제거할 메서드**:
- `saveCustomGameModeConfig()` → GameModeConfigManager로 이동
- `loadCustomGameModeConfig()` → GameModeConfigManager로 이동
- `buildGameModeConfig()` → 더 이상 불필요

**유지할 메서드**:
- `saveSettings()` (전역 설정)
- `loadSettings()` (전역 설정)
- Sound, Color, ScreenSize 관련 메서드

**우선순위**: MEDIUM  
**예상 작업 시간**: 2시간  
**의존성**: GameModeConfigManager 완성 후

---

### **Phase 3: 기능 완성 - 미구현 설정 적용**

#### 3.1 180° Rotation 구현

**파일**: `tetris-core/src/main/java/seoultech/se/core/GameEngine.java`

```java
public static boolean tryRotate180(Tetromino piece, int[][] board, GameModeConfig config) {
    if (!config.isRotation180Enabled()) {
        return false;
    }
    
    Tetromino rotated = piece.rotate(RotationDirection.CLOCKWISE)
                             .rotate(RotationDirection.CLOCKWISE);
    
    if (isValidPosition(rotated, board)) {
        return true;
    }
    
    // SRS 적용 (활성화된 경우)
    if (config.isSrsEnabled()) {
        // SRS 180° 킥 테이블 적용
    }
    
    return false;
}
```

**우선순위**: LOW  
**예상 작업 시간**: 2시간

---

#### 3.2 Ghost Piece 렌더링

**파일**: `tetris-client/src/main/java/seoultech/se/client/ui/BoardRenderer.java`

```java
public void renderGhostPiece(GameState state, GameModeConfig config) {
    if (!config.isGhostPieceEnabled()) {
        return;
    }
    
    Tetromino currentPiece = state.getCurrentPiece();
    if (currentPiece == null) return;
    
    // 가장 아래 위치 계산
    Tetromino ghost = calculateGhostPosition(currentPiece, state.getBoard());
    
    // 투명하게 렌더링
    int[][] shape = ghost.getShape();
    for (int i = 0; i < shape.length; i++) {
        for (int j = 0; j < shape[i].length; j++) {
            if (shape[i][j] != 0) {
                int x = ghost.getX() + j;
                int y = ghost.getY() + i;
                
                if (isWithinBounds(x, y)) {
                    Rectangle rect = cellRectangles[y][x];
                    rect.setFill(Color.LIGHTGRAY);
                    rect.setOpacity(0.3); // 투명도
                }
            }
        }
    }
}
```

**우선순위**: MEDIUM  
**예상 작업 시간**: 3시간

---

#### 3.3 Lock Delay 타이밍

**파일**: `tetris-client/src/main/java/seoultech/se/client/ui/GameLoopManager.java`

```java
private long lockStartTime = 0;
private int lockResetCount = 0;

private void checkLockDelay(GameState state, GameModeConfig config) {
    Tetromino piece = state.getCurrentPiece();
    
    // 블록이 바닥에 닿았는지 확인
    if (isOnGround(piece, state.getBoard())) {
        if (lockStartTime == 0) {
            lockStartTime = System.currentTimeMillis();
        }
        
        long elapsed = System.currentTimeMillis() - lockStartTime;
        
        if (elapsed >= config.getLockDelay()) {
            // 블록 고정
            lockPiece();
            resetLockDelay();
        }
    } else {
        // 바닥에서 떠있으면 락 딜레이 리셋
        if (lockResetCount < config.getMaxLockResets()) {
            resetLockDelay();
        }
    }
}

private void resetLockDelay() {
    lockStartTime = 0;
    lockResetCount++;
}
```

**우선순위**: MEDIUM  
**예상 작업 시간**: 2시간

---

#### 3.4 Soft Drop Speed 적용

**파일**: `tetris-client/src/main/java/seoultech/se/client/controller/BoardController.java`

```java
private GameState handleMoveCommand(MoveCommand command) {
    Direction direction = command.getDirection();
    
    // Soft Drop 감지
    if (direction == Direction.DOWN) {
        GameModeConfig config = getConfig();
        double softDropSpeed = config.getSoftDropSpeed();
        
        // 속도 배율 적용 (GameLoopManager에 전달)
        // 또는 점수 가산만 수행
        int points = (int) (1 * softDropSpeed);
        gameState.addScore(points);
    }
    
    boolean moved = GameEngine.tryMove(gameState.getCurrentPiece(), direction, gameState.getBoard());
    
    if (moved) {
        gameState.getCurrentPiece().move(direction);
    }
    
    return gameState;
}
```

**우선순위**: LOW  
**예상 작업 시간**: 1시간

---

### **Phase 4: 테스트 및 문서화**

#### 4.1 단위 테스트 작성

**파일**: `tetris-client/src/test/java/seoultech/se/client/service/GameModeConfigManagerTest.java`

```java
@SpringBootTest
class GameModeConfigManagerTest {
    
    @Autowired
    private GameModeConfigManager configManager;
    
    @Test
    void testLoadConfigForGameplay_Classic() {
        GameModeConfig config = configManager.loadConfigForGameplay(GameplayType.CLASSIC);
        assertNotNull(config);
        assertEquals(GameplayType.CLASSIC, config.getGameplayType());
    }
    
    @Test
    void testSaveAndLoadCustomConfig() {
        GameModeConfig custom = GameModeConfig.builder()
            .gameplayType(GameplayType.CLASSIC)
            .hardDropEnabled(false)
            .dropSpeedMultiplier(0.5)
            .build();
        
        configManager.saveConfigForGameplay(GameplayType.CLASSIC, custom);
        
        GameModeConfig loaded = configManager.loadConfigForGameplay(GameplayType.CLASSIC);
        
        assertEquals(false, loaded.isHardDropEnabled());
        assertEquals(0.5, loaded.getDropSpeedMultiplier(), 0.01);
    }
    
    @Test
    void testValidateConfig_InvalidDropSpeed() {
        GameModeConfig invalid = GameModeConfig.builder()
            .dropSpeedMultiplier(100.0) // 유효 범위 초과
            .build();
        
        assertThrows(IllegalArgumentException.class, () -> {
            configManager.validateConfig(invalid);
        });
    }
}
```

**우선순위**: MEDIUM  
**예상 작업 시간**: 4시간

---

#### 4.2 통합 테스트

**시나리오**:
1. Classic 모드 설정 변경 → 게임 시작 → 설정 적용 확인
2. Arcade 모드 기본값 → 게임 시작 → 빠른 속도 확인
3. 설정 저장 → 앱 재시작 → 설정 유지 확인

**우선순위**: HIGH  
**예상 작업 시간**: 2시간

---

#### 4.3 문서 업데이트

**파일**: `document/ARCHITECTURE.md`

```markdown
## Settings System Architecture

### Overview
설정 시스템은 3계층으로 구성됩니다:
1. **GameModeConfigManager**: 게임 모드 설정 비즈니스 로직
2. **GameSettingsDialogService**: 설정 UI 관리
3. **SettingsService**: 전역 설정 및 파일 I/O

### Component Diagram
```
┌─────────────────────────────────────┐
│      MainController                 │
│  (UI Event Handlers)                │
└──────────┬──────────────────────────┘
           │
           ├─────────────────────────┐
           ▼                         ▼
┌──────────────────────┐  ┌──────────────────────┐
│ GameSettingsDialog   │  │ GameModeConfig       │
│ Service              │  │ Manager              │
│ (UI Logic)           │  │ (Business Logic)     │
└──────────┬───────────┘  └──────────┬───────────┘
           │                         │
           │                         ▼
           │              ┌──────────────────────┐
           │              │ SettingsService      │
           └─────────────►│ (File I/O)           │
                          └──────────────────────┘
```

### Settings Flow
1. 사용자가 설정 아이콘(⚙) 클릭
2. `GameSettingsDialogService.showSettingsDialog()` 호출
3. 다이얼로그에서 설정 변경
4. "Apply" 클릭 시 `GameModeConfigManager.saveConfigForGameplay()` 호출
5. `SettingsService`를 통해 `tetris_settings` 파일에 저장
6. 게임 시작 시 `GameModeConfigManager.loadConfigForGameplay()` 호출
7. `GameController.setGameModeConfig()` → `BoardController` 생성

### File Format
```properties
# tetris_settings
soundVolume=80.0
colorMode=colorModeDefault
screenSize=screenSizeL
game.mode.playType=LOCAL_SINGLE
game.mode.gameplayType=CLASSIC
game.mode.srsEnabled=true

# Custom Settings (per GameplayType)
custom.classic.hardDropEnabled=false
custom.classic.holdEnabled=true
custom.classic.dropSpeedMultiplier=0.5
custom.classic.lockDelay=500
custom.classic.rotation180Enabled=false
custom.classic.ghostPieceEnabled=true
custom.classic.softDropSpeed=20.0

custom.arcade.hardDropEnabled=true
custom.arcade.dropSpeedMultiplier=2.0
...
```
```

**우선순위**: MEDIUM  
**예상 작업 시간**: 2시간

---

## 📊 타임라인 및 우선순위

### Week 1: 핵심 기능 완성
- ✅ **Day 1**: Phase 1 완료 (설정 적용 문제 해결)
- **Day 2-3**: Phase 2.1-2.2 (GameModeConfigManager, GameSettingsDialogService)
- **Day 4-5**: Phase 2.3-2.4 (MainController, SettingsService 슬림화)

### Week 2: 기능 완성 및 테스트
- **Day 6-7**: Phase 3.2, 3.3 (Ghost Piece, Lock Delay)
- **Day 8-9**: Phase 4.1-4.2 (단위 테스트, 통합 테스트)
- **Day 10**: Phase 4.3 (문서화)

### Optional (Low Priority)
- Phase 3.1: 180° Rotation
- Phase 3.4: Soft Drop Speed

---

## ✅ 완료 체크리스트

### Phase 1: 즉시 수정
- [x] GameController 초기화 순서 변경
- [x] 컴파일 성공
- [ ] 실제 테스트: Hard Drop OFF, Hold OFF, Drop Speed 0.5x

### Phase 2: 아키텍처 개선
- [ ] GameModeConfigManager 서비스 생성
- [ ] GameSettingsDialogService 분리
- [ ] MainController 슬림화 (413 → ~200 lines)
- [ ] SettingsService 슬림화 (400 → ~200 lines)

### Phase 3: 기능 완성
- [ ] Ghost Piece 렌더링
- [ ] Lock Delay 타이밍
- [ ] 180° Rotation (Optional)
- [ ] Soft Drop Speed (Optional)

### Phase 4: 테스트 및 문서화
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 수행
- [ ] ARCHITECTURE.md 업데이트
- [ ] DEVELOPMENT.md 업데이트

---

## 🎯 기대 효과

### 코드 품질
- **라인 수 감소**: MainController 413 → 200 lines (-50%)
- **복잡도 감소**: Cyclomatic Complexity 감소
- **테스트 커버리지**: 30% → 80% 목표

### 유지보수성
- **단일 책임 원칙**: 각 클래스가 하나의 책임만
- **변경 용이성**: 설정 UI 변경 시 Service만 수정
- **확장성**: 새로운 게임 모드 추가 용이

### 개발자 경험
- **가독성**: 코드 의도가 명확
- **디버깅**: 각 레이어별로 독립적으로 테스트 가능
- **협업**: 명확한 책임 분리로 충돌 최소화

---

## 📚 참고 자료

- [SOLID Principles](https://en.wikipedia.org/wiki/SOLID)
- [Service Layer Pattern](https://martinfowler.com/eaaCatalog/serviceLayer.html)
- [Facade Pattern](https://refactoring.guru/design-patterns/facade)
- [Spring Best Practices](https://spring.io/guides)

---

**작성일**: 2025-01-29  
**작성자**: GitHub Copilot  
**버전**: 1.0  
**상태**: 진행 중 (Phase 1 완료)
