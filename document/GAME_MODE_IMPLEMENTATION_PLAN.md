# 게임 모드 선택 시스템 구현 계획 (v3.0)

> **최적화 버전**: PopupManager 재사용 + application.properties 통합  
> **작성일**: 2025-10-29  
> **예상 기간**: 3주

---

## 🎯 개선 사항

### ✅ 기존 시스템 재사용
1. ❌ ~~새로운 FXML 화면 생성~~ → ✅ **PopupManager에 모드 선택 팝업 추가**
2. ❌ ~~별도 Properties 파일~~ → ✅ **application.properties + 환경 변수 활용**
3. ❌ ~~새로운 Service 클래스~~ → ✅ **기존 SettingsService 확장**

### 📈 효율성 향상
- **개발 시간**: 5주 → **3주** (40% 단축)
- **코드 재사용성**: 높음
- **일관성**: 기존 팝업 스타일과 동일

---

## 📋 Phase 1: Core 모듈 확장

### 1.1 GameModeConfig 확장 ⭐

**파일**: `tetris-core/src/main/java/seoultech/se/core/config/GameModeConfig.java`

**추가 필드**:
```java
// ========== SRS 회전 시스템 ==========
/**
 * SRS(Super Rotation System) 활성화 여부
 * true: 벽 킥 적용, false: 기본 회전만
 */
@Builder.Default
private final boolean srsEnabled = true;

// ========== 게임플레이 타입 ==========
/**
 * 게임플레이 종류 (클래식/아케이드)
 */
@Builder.Default
private final GameplayType gameplayType = GameplayType.CLASSIC;
```

**추가 프리셋**:
```java
/**
 * 아케이드 모드 설정
 * - 빠른 낙하 속도 (1.5배)
 * - 짧은 락 딜레이 (300ms)
 * - SRS 활성화
 */
public static GameModeConfig arcade() {
    return GameModeConfig.builder()
        .gameplayType(GameplayType.ARCADE)
        .dropSpeedMultiplier(1.5)
        .lockDelay(300)
        .srsEnabled(true)
        .build();
}

/**
 * 클래식 모드 (SRS 옵션)
 */
public static GameModeConfig classic(boolean srsEnabled) {
    return GameModeConfig.builder()
        .gameplayType(GameplayType.CLASSIC)
        .srsEnabled(srsEnabled)
        .build();
}
```

---

### 1.2 GameplayType Enum 생성 ⭐

**파일**: `tetris-core/src/main/java/seoultech/se/core/config/GameplayType.java` *(새 파일)*

```java
package seoultech.se.core.config;

/**
 * 게임플레이 타입
 * 
 * PlayType(싱글/멀티)과 독립적으로 선택 가능
 */
public enum GameplayType {
    /**
     * 클래식 모드 - 전통적인 테트리스
     */
    CLASSIC("클래식", "전통적인 테트리스"),
    
    /**
     * 아케이드 모드 - 빠르고 박진감 넘치는
     */
    ARCADE("아케이드", "빠르고 박진감 넘치는 모드");
    
    private final String displayName;
    private final String description;
    
    GameplayType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
```

---

### 1.3 PlayType Enum 생성 ⭐

**파일**: `tetris-core/src/main/java/seoultech/se/core/mode/PlayType.java` *(새 파일)*

```java
package seoultech.se.core.mode;

/**
 * 플레이 타입
 */
public enum PlayType {
    LOCAL_SINGLE("로컬 싱글", "혼자서 플레이"),
    ONLINE_MULTI("온라인 멀티", "다른 플레이어와 대전");
    
    private final String displayName;
    private final String description;
    
    PlayType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
}
```

---

### 1.4 GameEngine SRS 토글 지원 ⭐

**파일**: `tetris-core/src/main/java/seoultech/se/core/GameEngine.java`

**수정 메서드**:
```java
/**
 * 회전을 시도합니다 (SRS 옵션 지원)
 * 
 * @param state 현재 게임 상태
 * @param direction 회전 방향
 * @param srsEnabled SRS 활성화 여부
 * @return 새로운 게임 상태
 */
public static GameState tryRotate(GameState state, 
                                  RotationDirection direction,
                                  boolean srsEnabled) {
    Tetromino currentTetromino = state.getCurrentTetromino();
    RotationState newRotation = direction.apply(currentTetromino.getRotationState());
    Tetromino rotatedTetromino = currentTetromino.rotate(newRotation);
    
    if (srsEnabled) {
        // SRS Wall Kick 적용
        return tryRotateWithWallKick(state, rotatedTetromino, 
                                     currentTetromino.getRotationState(), 
                                     newRotation);
    } else {
        // 기본 회전만
        if (isValidPosition(state, rotatedTetromino, 
                           state.getCurrentX(), state.getCurrentY())) {
            GameState newState = state.deepCopy();
            newState.setCurrentTetromino(rotatedTetromino);
            newState.setLastActionWasRotation(true);
            return newState;
        }
        return state;
    }
}

// 하위 호환성 유지
public static GameState tryRotate(GameState state, RotationDirection direction) {
    return tryRotate(state, direction, true);
}
```

---

## 📋 Phase 2: SettingsService 확장 ⭐ **핵심 변경**

### 2.1 application.properties 게임 모드 설정 추가

**파일**: `tetris-client/application.properties`

**추가 설정**:
```properties
# ===============================================================================
# Game Mode Configuration
# ===============================================================================

# Play Type: LOCAL_SINGLE, ONLINE_MULTI
tetris.mode.play-type=${TETRIS_MODE_PLAY_TYPE:LOCAL_SINGLE}

# Gameplay Type: CLASSIC, ARCADE
tetris.mode.gameplay-type=${TETRIS_MODE_GAMEPLAY_TYPE:CLASSIC}

# SRS (Super Rotation System) Enabled
tetris.mode.srs-enabled=${TETRIS_MODE_SRS_ENABLED:true}

# Last Selected Mode (Auto-saved)
tetris.mode.last-play-type=${TETRIS_MODE_LAST_PLAY_TYPE:LOCAL_SINGLE}
tetris.mode.last-gameplay-type=${TETRIS_MODE_LAST_GAMEPLAY_TYPE:CLASSIC}
tetris.mode.last-srs-enabled=${TETRIS_MODE_LAST_SRS_ENABLED:true}
```

---

### 2.2 SettingsService 확장

**파일**: `tetris-client/src/main/java/seoultech/se/client/service/SettingsService.java`

**추가 필드 및 메서드**:
```java
@Service
public class SettingsService {
    
    // 기존 필드들...
    private final DoubleProperty soundVolume = new SimpleDoubleProperty(80);
    private final StringProperty colorMode = new SimpleStringProperty("colorModeDefault");
    private final StringProperty screenSize = new SimpleStringProperty("screenSizeM");
    
    // ⭐ 새로 추가: 게임 모드 설정
    private final ObjectProperty<PlayType> playType = new SimpleObjectProperty<>(PlayType.LOCAL_SINGLE);
    private final ObjectProperty<GameplayType> gameplayType = new SimpleObjectProperty<>(GameplayType.CLASSIC);
    private final BooleanProperty srsEnabled = new SimpleBooleanProperty(true);
    
    @Value("${tetris.mode.play-type:LOCAL_SINGLE}")
    private String defaultPlayType;
    
    @Value("${tetris.mode.gameplay-type:CLASSIC}")
    private String defaultGameplayType;
    
    @Value("${tetris.mode.srs-enabled:true}")
    private boolean defaultSrsEnabled;
    
    private static final String SETTINGS_FILE = "tetris_settings";
    
    @PostConstruct
    public void init() {
        loadSettings();
    }
    
    /**
     * 설정 로드 (기존 + 게임 모드)
     */
    public void loadSettings() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(new File(SETTINGS_FILE))) {
            props.load(in);
            
            // 기존 설정
            soundVolume.set(Double.parseDouble(props.getProperty("soundVolume", "80")));
            colorMode.set(props.getProperty("colorMode", "colorModeDefault"));
            screenSize.set(props.getProperty("screenSize", "screenSizeM"));
            double width = Double.parseDouble(props.getProperty("stageWidth", "500"));
            double height = Double.parseDouble(props.getProperty("stageHeight", "600"));
            applyResolution(width, height);
            
            // ⭐ 게임 모드 설정
            String playTypeStr = props.getProperty("playType", defaultPlayType);
            String gameplayTypeStr = props.getProperty("gameplayType", defaultGameplayType);
            boolean srsEnabledVal = Boolean.parseBoolean(props.getProperty("srsEnabled", String.valueOf(defaultSrsEnabled)));
            
            playType.set(PlayType.valueOf(playTypeStr));
            gameplayType.set(GameplayType.valueOf(gameplayTypeStr));
            srsEnabled.set(srsEnabledVal);
            
            System.out.println("✅ Settings loaded successfully.");
            System.out.println("  - Play Type: " + playType.get());
            System.out.println("  - Gameplay Type: " + gameplayType.get());
            System.out.println("  - SRS: " + (srsEnabled.get() ? "ON" : "OFF"));
            
        } catch (Exception e) {
            System.out.println("❗ Failed to load settings, using defaults.");
            restoreDefaults();
        }
    }
    
    /**
     * 설정 저장 (기존 + 게임 모드)
     */
    public void saveSettings() {
        Properties props = new Properties();
        
        // 기존 설정
        props.setProperty("soundVolume", String.valueOf(soundVolume.get()));
        props.setProperty("colorMode", colorMode.get());
        props.setProperty("screenSize", screenSize.get());
        props.setProperty("stageWidth", String.valueOf(stageWidth.get()));
        props.setProperty("stageHeight", String.valueOf(stageHeight.get()));
        
        // ⭐ 게임 모드 설정
        props.setProperty("playType", playType.get().name());
        props.setProperty("gameplayType", gameplayType.get().name());
        props.setProperty("srsEnabled", String.valueOf(srsEnabled.get()));
        
        try {
            props.store(new java.io.FileOutputStream(new File(SETTINGS_FILE)), null);
            System.out.println("✅ Settings saved successfully.");
        } catch (Exception e) {
            System.out.println("❗ Failed to save settings.");
        }
    }
    
    /**
     * 기본값 복원
     */
    public void restoreDefaults() {
        // 기존
        soundVolume.set(80);
        colorMode.set("colorModeDefault");
        screenSize.set("screenSizeM");
        applyResolution(500, 700);
        
        // ⭐ 게임 모드
        playType.set(PlayType.valueOf(defaultPlayType));
        gameplayType.set(GameplayType.valueOf(defaultGameplayType));
        srsEnabled.set(defaultSrsEnabled);
        
        saveSettings();
    }
    
    // ⭐ 게임 모드 Property 접근자
    public ObjectProperty<PlayType> playTypeProperty() {
        return playType;
    }
    
    public ObjectProperty<GameplayType> gameplayTypeProperty() {
        return gameplayType;
    }
    
    public BooleanProperty srsEnabledProperty() {
        return srsEnabled;
    }
    
    /**
     * GameModeConfig 생성
     */
    public GameModeConfig buildGameModeConfig() {
        if (gameplayType.get() == GameplayType.ARCADE) {
            return GameModeConfig.arcade()
                .toBuilder()
                .srsEnabled(srsEnabled.get())
                .build();
        } else {
            return GameModeConfig.classic(srsEnabled.get());
        }
    }
}
```

**체크리스트**:
- [ ] `@Value`로 application.properties 값 주입
- [ ] 게임 모드 Property 추가
- [ ] `loadSettings()` / `saveSettings()` 확장
- [ ] `buildGameModeConfig()` 메서드 추가

---

## 📋 Phase 3: PopupManager 확장 및 모드 선택 팝업 ⭐ **핵심 변경**

### 3.1 모드 선택 팝업 UI 컴포넌트

**파일**: `tetris-client/src/main/java/seoultech/se/client/ui/ModeSelectionPopup.java` *(새 파일)*

```java
package seoultech.se.client.ui;

import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.mode.PlayType;

/**
 * 게임 모드 선택 팝업 UI
 * 
 * PopupManager와 통합하여 사용
 * VBox 기반으로 구성
 */
public class ModeSelectionPopup extends VBox {
    
    // UI 컴포넌트
    private final ToggleButton localSingleButton;
    private final ToggleButton onlineMultiButton;
    private final ToggleButton classicButton;
    private final ToggleButton arcadeButton;
    private final CheckBox srsCheckBox;
    private final Button startButton;
    private final Button cancelButton;
    
    private final ToggleGroup playTypeGroup;
    private final ToggleGroup gameplayTypeGroup;
    
    // 콜백
    private ModeSelectionCallback callback;
    
    public interface ModeSelectionCallback {
        void onStartGame(PlayType playType, GameplayType gameplayType, boolean srsEnabled);
        void onCancel();
    }
    
    public ModeSelectionPopup() {
        // 스타일 클래스 설정
        this.getStyleClass().add("mode-selection-popup");
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setMaxWidth(500);
        
        // 제목
        Label titleLabel = new Label("게임 모드 선택");
        titleLabel.getStyleClass().add("popup-title");
        
        // ===== 플레이 타입 =====
        Label playTypeLabel = new Label("플레이 타입");
        playTypeLabel.getStyleClass().add("section-label");
        
        playTypeGroup = new ToggleGroup();
        localSingleButton = new ToggleButton("로컬 싱글");
        localSingleButton.setToggleGroup(playTypeGroup);
        localSingleButton.setSelected(true);
        localSingleButton.getStyleClass().add("mode-toggle-button");
        
        onlineMultiButton = new ToggleButton("온라인 멀티");
        onlineMultiButton.setToggleGroup(playTypeGroup);
        onlineMultiButton.getStyleClass().add("mode-toggle-button");
        
        HBox playTypeBox = new HBox(15, localSingleButton, onlineMultiButton);
        playTypeBox.setAlignment(Pos.CENTER);
        
        // ===== 게임플레이 타입 =====
        Label gameplayTypeLabel = new Label("게임 모드");
        gameplayTypeLabel.getStyleClass().add("section-label");
        
        gameplayTypeGroup = new ToggleGroup();
        classicButton = new ToggleButton("클래식");
        classicButton.setToggleGroup(gameplayTypeGroup);
        classicButton.setSelected(true);
        classicButton.getStyleClass().add("mode-toggle-button");
        
        arcadeButton = new ToggleButton("아케이드");
        arcadeButton.setToggleGroup(gameplayTypeGroup);
        arcadeButton.getStyleClass().add("mode-toggle-button");
        
        HBox gameplayTypeBox = new HBox(15, classicButton, arcadeButton);
        gameplayTypeBox.setAlignment(Pos.CENTER);
        
        // ===== SRS 설정 =====
        srsCheckBox = new CheckBox("SRS (Super Rotation System) 활성화");
        srsCheckBox.setSelected(true);
        srsCheckBox.getStyleClass().add("srs-checkbox");
        
        Label srsDescription = new Label("벽 킥을 사용하여 더 유연한 회전 가능");
        srsDescription.getStyleClass().add("description-label");
        
        VBox srsBox = new VBox(5, srsCheckBox, srsDescription);
        srsBox.setAlignment(Pos.CENTER);
        
        // ===== 버튼 =====
        startButton = new Button("게임 시작");
        startButton.getStyleClass().add("start-button");
        startButton.setOnAction(e -> handleStart());
        
        cancelButton = new Button("취소");
        cancelButton.getStyleClass().add("cancel-button");
        cancelButton.setOnAction(e -> handleCancel());
        
        HBox buttonBox = new HBox(15, startButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        // ===== 전체 레이아웃 =====
        this.getChildren().addAll(
            titleLabel,
            playTypeLabel,
            playTypeBox,
            gameplayTypeLabel,
            gameplayTypeBox,
            srsBox,
            buttonBox
        );
        
        // 온라인 멀티 버튼 비활성화 (향후 구현)
        onlineMultiButton.setOnAction(e -> {
            if (onlineMultiButton.isSelected()) {
                showAlert("온라인 멀티플레이는 향후 업데이트 예정입니다.");
                localSingleButton.setSelected(true);
            }
        });
    }
    
    public void setCallback(ModeSelectionCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 마지막 선택 값 로드
     */
    public void loadSelection(PlayType playType, GameplayType gameplayType, boolean srsEnabled) {
        if (playType == PlayType.ONLINE_MULTI) {
            onlineMultiButton.setSelected(true);
        } else {
            localSingleButton.setSelected(true);
        }
        
        if (gameplayType == GameplayType.ARCADE) {
            arcadeButton.setSelected(true);
        } else {
            classicButton.setSelected(true);
        }
        
        srsCheckBox.setSelected(srsEnabled);
    }
    
    private void handleStart() {
        if (callback != null) {
            PlayType selectedPlayType = localSingleButton.isSelected() ? 
                PlayType.LOCAL_SINGLE : PlayType.ONLINE_MULTI;
            GameplayType selectedGameplayType = classicButton.isSelected() ? 
                GameplayType.CLASSIC : GameplayType.ARCADE;
            boolean selectedSrsEnabled = srsCheckBox.isSelected();
            
            callback.onStartGame(selectedPlayType, selectedGameplayType, selectedSrsEnabled);
        }
    }
    
    private void handleCancel() {
        if (callback != null) {
            callback.onCancel();
        }
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("안내");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
```

---

### 3.2 PopupManager 확장

**파일**: `tetris-client/src/main/java/seoultech/se/client/ui/PopupManager.java`

**추가 필드 및 메서드**:
```java
public class PopupManager {
    
    // 기존 필드
    private final VBox pauseOverlay;
    private final VBox gameOverOverlay;
    private final Label finalScoreLabel;
    
    // ⭐ 새로 추가: 모드 선택 팝업
    private ModeSelectionPopup modeSelectionPopup;
    private VBox modeSelectionOverlay; // 오버레이 컨테이너
    
    private PopupActionCallback callback;
    
    /**
     * 생성자 (기존 유지)
     */
    public PopupManager(VBox pauseOverlay, VBox gameOverOverlay, Label finalScoreLabel) {
        this.pauseOverlay = pauseOverlay;
        this.gameOverOverlay = gameOverOverlay;
        this.finalScoreLabel = finalScoreLabel;
    }
    
    /**
     * ⭐ 모드 선택 팝업 초기화
     */
    public void initModeSelectionPopup(VBox modeSelectionOverlay) {
        this.modeSelectionOverlay = modeSelectionOverlay;
        
        // ModeSelectionPopup 생성 및 추가
        modeSelectionPopup = new ModeSelectionPopup();
        modeSelectionOverlay.getChildren().add(modeSelectionPopup);
        
        // 초기 숨김
        hideModeSelectionPopup();
    }
    
    /**
     * ⭐ 모드 선택 팝업 표시
     */
    public void showModeSelectionPopup(PlayType currentPlayType, 
                                       GameplayType currentGameplayType, 
                                       boolean currentSrsEnabled,
                                       ModeSelectionPopup.ModeSelectionCallback callback) {
        if (modeSelectionPopup != null) {
            modeSelectionPopup.loadSelection(currentPlayType, currentGameplayType, currentSrsEnabled);
            modeSelectionPopup.setCallback(callback);
            setOverlayVisibility(modeSelectionOverlay, true);
        }
    }
    
    /**
     * ⭐ 모드 선택 팝업 숨김
     */
    public void hideModeSelectionPopup() {
        if (modeSelectionOverlay != null) {
            setOverlayVisibility(modeSelectionOverlay, false);
        }
    }
    
    // 기존 메서드들...
    public void init() {
        hideAllPopups();
    }
    
    public void hideAllPopups() {
        hidePausePopup();
        hideGameOverPopup();
        hideModeSelectionPopup();
    }
    
    // ... 나머지 기존 메서드 유지
}
```

---

### 3.3 CSS 스타일 추가

**파일**: `tetris-client/src/main/resources/css/application.css` (기존 파일에 추가)

```css
/* ========== 모드 선택 팝업 ========== */
.mode-selection-popup {
    -fx-background-color: rgba(26, 26, 46, 0.95);
    -fx-padding: 30px;
    -fx-border-color: #e94560;
    -fx-border-width: 3px;
    -fx-border-radius: 10px;
    -fx-background-radius: 10px;
}

.popup-title {
    -fx-font-size: 28px;
    -fx-font-weight: bold;
    -fx-text-fill: #eee;
}

.section-label {
    -fx-font-size: 16px;
    -fx-text-fill: #bbb;
    -fx-font-weight: bold;
}

.mode-toggle-button {
    -fx-min-width: 140px;
    -fx-min-height: 50px;
    -fx-font-size: 15px;
    -fx-background-color: #16213e;
    -fx-text-fill: #eee;
    -fx-border-color: #0f3460;
    -fx-border-width: 2px;
    -fx-border-radius: 5px;
    -fx-background-radius: 5px;
    -fx-cursor: hand;
}

.mode-toggle-button:selected {
    -fx-background-color: #e94560;
    -fx-border-color: #e94560;
    -fx-text-fill: white;
}

.mode-toggle-button:hover {
    -fx-background-color: #0f3460;
}

.srs-checkbox {
    -fx-font-size: 14px;
    -fx-text-fill: #eee;
}

.description-label {
    -fx-font-size: 11px;
    -fx-text-fill: #888;
    -fx-font-style: italic;
}

.start-button {
    -fx-min-width: 150px;
    -fx-min-height: 45px;
    -fx-font-size: 18px;
    -fx-font-weight: bold;
    -fx-background-color: #e94560;
    -fx-text-fill: white;
    -fx-cursor: hand;
    -fx-border-radius: 5px;
    -fx-background-radius: 5px;
}

.start-button:hover {
    -fx-background-color: #ff6b81;
}

.cancel-button {
    -fx-min-width: 100px;
    -fx-min-height: 45px;
    -fx-font-size: 16px;
    -fx-background-color: #444;
    -fx-text-fill: #eee;
    -fx-cursor: hand;
    -fx-border-radius: 5px;
    -fx-background-radius: 5px;
}

.cancel-button:hover {
    -fx-background-color: #666;
}
```

---

## 📋 Phase 4: Mode 클래스 구현

### 4.1 SingleMode 수정

**파일**: `tetris-client/src/main/java/seoultech/se/client/mode/SingleMode.java`

**추가 메서드**:
```java
@Component
@Getter
@Setter
public class SingleMode implements GameMode {
    
    private GameModeConfig config = GameModeConfig.classic(true);
    private GameState gameState;
    
    @Override
    public GameModeType getType() {
        return GameModeType.SINGLE;
    }
    
    /**
     * ⭐ PlayType 반환
     */
    public PlayType getPlayType() {
        return PlayType.LOCAL_SINGLE;
    }
    
    /**
     * ⭐ GameplayType 반환
     */
    public GameplayType getGameplayType() {
        return config.getGameplayType();
    }
    
    @Override
    public GameModeConfig getConfig() {
        return config;
    }
    
    @Override
    public void initialize(GameState initialState) {
        this.gameState = initialState;
        System.out.println("[SingleMode] 초기화 완료");
        System.out.println("  - PlayType: " + getPlayType().getDisplayName());
        System.out.println("  - GameplayType: " + getGameplayType().getDisplayName());
        System.out.println("  - SRS: " + (config.isSrsEnabled() ? "ON" : "OFF"));
    }
    
    // 기존 메서드들 유지...
}
```

---

### 4.2 MultiMode 구현 (기본 구조)

**파일**: `tetris-client/src/main/java/seoultech/se/client/mode/MultiMode.java` *(새 파일)*

```java
package seoultech.se.client.mode;

import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;
import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.mode.GameMode;
import seoultech.se.core.mode.GameModeType;
import seoultech.se.core.mode.PlayType;

/**
 * 멀티플레이어 모드 (향후 구현)
 */
@Component
@Getter
@Setter
public class MultiMode implements GameMode {
    
    private GameModeConfig config = GameModeConfig.classic(true);
    private GameState gameState;
    
    @Override
    public GameModeType getType() {
        return GameModeType.MULTI;
    }
    
    public PlayType getPlayType() {
        return PlayType.ONLINE_MULTI;
    }
    
    public GameplayType getGameplayType() {
        return config.getGameplayType();
    }
    
    @Override
    public GameModeConfig getConfig() {
        return config;
    }
    
    @Override
    public void initialize(GameState initialState) {
        this.gameState = initialState;
        System.out.println("[MultiMode] 초기화 (네트워크 기능은 향후 추가)");
    }
    
    @Override
    public void cleanup() {
        // TODO: 네트워크 연결 해제
    }
}
```

---

## 📋 Phase 5: 통합 및 테스트

### 5.1 MainController 수정

**파일**: `tetris-client/src/main/java/seoultech/se/client/controller/MainController.java`

**수정 내용**:
```java
@Component
public class MainController extends BaseController {
    
    @Autowired
    private NavigationService navigationService;
    
    @Autowired
    private SettingsService settingsService;
    
    /**
     * START 버튼 - 모드 선택 팝업 표시
     */
    public void handleStartButtonAction(ActionEvent event) {
        System.out.println("▶️ Start button clicked");
        
        try {
            // ⭐ 모드 선택 팝업을 띄운 후 게임 시작
            navigationService.navigateToGameWithModeSelection();
        } catch (Exception e) {
            System.err.println("❌ Failed to start game: " + e.getMessage());
        }
    }
    
    // 기존 메서드들...
}
```

---

### 5.2 NavigationService 확장

**파일**: `tetris-client/src/main/java/seoultech/se/client/service/NavigationService.java`

**추가 메서드**:
```java
@Service
public class NavigationService {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private SettingsService settingsService;
    
    /**
     * ⭐ 모드 선택과 함께 게임 화면으로 이동
     */
    public void navigateToGameWithModeSelection() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/game-view.fxml"));
        loader.setControllerFactory(applicationContext::getBean);
        Parent root = loader.load();
        
        GameController controller = loader.getController();
        
        // 게임 화면 표시
        Stage stage = getCurrentStage();
        stage.setScene(new Scene(root));
        stage.show();
        
        // ⭐ 모드 선택 팝업 표시 (게임 화면 위에)
        controller.showModeSelectionPopup();
    }
    
    // 기존 메서드들...
}
```

---

### 5.3 GameController 수정 ⭐ **핵심 통합**

**파일**: `tetris-client/src/main/java/seoultech/se/client/controller/GameController.java`

**추가 필드 및 메서드**:
```java
@Component
public class GameController {
    
    @Autowired
    private SettingsService settingsService;
    
    @Autowired
    private SingleMode singleMode;
    
    @Autowired
    private MultiMode multiMode;
    
    @FXML private VBox pauseOverlay;
    @FXML private VBox gameOverOverlay;
    @FXML private Label finalScoreLabel;
    
    // ⭐ 모드 선택 팝업 오버레이
    @FXML private VBox modeSelectionOverlay;
    
    private PopupManager popupManager;
    private BoardController boardController;
    
    @FXML
    public void initialize() {
        // ... 기존 초기화
        
        // PopupManager 초기화
        popupManager = new PopupManager(pauseOverlay, gameOverOverlay, finalScoreLabel);
        popupManager.init();
        
        // ⭐ 모드 선택 팝업 초기화
        popupManager.initModeSelectionPopup(modeSelectionOverlay);
        
        // ... 나머지 초기화
    }
    
    /**
     * ⭐ 모드 선택 팝업 표시 (외부에서 호출)
     */
    public void showModeSelectionPopup() {
        popupManager.showModeSelectionPopup(
            settingsService.playTypeProperty().get(),
            settingsService.gameplayTypeProperty().get(),
            settingsService.srsEnabledProperty().get(),
            new ModeSelectionPopup.ModeSelectionCallback() {
                @Override
                public void onStartGame(PlayType playType, GameplayType gameplayType, boolean srsEnabled) {
                    handleModeSelected(playType, gameplayType, srsEnabled);
                }
                
                @Override
                public void onCancel() {
                    handleModeSelectionCancel();
                }
            }
        );
    }
    
    /**
     * ⭐ 모드 선택 완료 핸들러
     */
    private void handleModeSelected(PlayType playType, GameplayType gameplayType, boolean srsEnabled) {
        System.out.println("🎮 모드 선택 완료:");
        System.out.println("  - PlayType: " + playType.getDisplayName());
        System.out.println("  - GameplayType: " + gameplayType.getDisplayName());
        System.out.println("  - SRS: " + (srsEnabled ? "ON" : "OFF"));
        
        // ⭐ 설정 저장
        settingsService.playTypeProperty().set(playType);
        settingsService.gameplayTypeProperty().set(gameplayType);
        settingsService.srsEnabledProperty().set(srsEnabled);
        settingsService.saveSettings();
        
        // ⭐ GameModeConfig 생성
        GameModeConfig config = settingsService.buildGameModeConfig();
        
        // ⭐ GameMode 선택 및 설정
        GameMode gameMode;
        if (playType == PlayType.LOCAL_SINGLE) {
            singleMode.setConfig(config);
            gameMode = singleMode;
        } else {
            multiMode.setConfig(config);
            gameMode = multiMode;
        }
        
        // BoardController에 주입
        boardController.setGameMode(gameMode);
        
        // 팝업 숨기고 게임 시작
        popupManager.hideModeSelectionPopup();
        startGame();
    }
    
    /**
     * ⭐ 모드 선택 취소 핸들러
     */
    private void handleModeSelectionCancel() {
        popupManager.hideModeSelectionPopup();
        
        // 메인 메뉴로 돌아가기
        try {
            navigationService.navigateTo("/view/main-view.fxml");
        } catch (Exception e) {
            System.err.println("❌ Failed to navigate: " + e.getMessage());
        }
    }
    
    // 기존 메서드들...
}
```

---

### 5.4 game-view.fxml 수정

**파일**: `tetris-client/src/main/resources/view/game-view.fxml`

**추가 오버레이**:
```xml
<!-- 기존 pauseOverlay, gameOverOverlay 아래에 추가 -->

<!-- ⭐ 모드 선택 오버레이 -->
<VBox fx:id="modeSelectionOverlay" 
      styleClass="overlay" 
      alignment="CENTER"
      visible="false" 
      managed="false">
    <!-- ModeSelectionPopup이 동적으로 추가됨 -->
</VBox>
```

---

### 5.5 BoardController 수정

**파일**: `tetris-client/src/main/java/seoultech/se/client/controller/BoardController.java`

**회전 처리 수정**:
```java
private GameState handleRotateCommand(RotateCommand command) {
    if (gameState.isPaused() || gameState.isGameOver()) {
        return gameState;
    }
    
    // ⭐ SRS 설정 가져오기
    boolean srsEnabled = getConfig().isSrsEnabled();
    
    // GameEngine 호출 (SRS 옵션 전달)
    GameState newState = GameEngine.tryRotate(
        gameState, 
        command.getDirection(),
        srsEnabled  // ⭐
    );
    
    if (newState != gameState) {
        this.gameState = newState;
        return gameState;
    }
    
    return gameState;
}
```

---

## 🎯 구현 순서

```
Week 1: Phase 1 (Core 확장)
├─ Day 1-2: GameModeConfig, GameplayType, PlayType 추가
├─ Day 3-4: GameEngine SRS 토글 구현
└─ Day 5: 테스트

Week 2: Phase 2-3 (SettingsService + PopupManager)
├─ Day 1-2: SettingsService 확장 (properties 연동)
├─ Day 3-4: ModeSelectionPopup UI 구현
├─ Day 5: PopupManager 통합

Week 3: Phase 4-5 (Mode 클래스 + 통합)
├─ Day 1-2: SingleMode, MultiMode 구현
├─ Day 3-4: GameController, MainController 통합
└─ Day 5: 전체 테스트 및 디버깅
```

---

## ✅ 체크리스트

### Phase 1
- [ ] `GameModeConfig`에 `srsEnabled`, `gameplayType` 추가
- [ ] `GameplayType` enum 생성
- [ ] `PlayType` enum 생성
- [ ] `GameEngine.tryRotate()` SRS 토글 구현

### Phase 2
- [ ] `application.properties` 게임 모드 설정 추가
- [ ] `SettingsService`에 게임 모드 Property 추가
- [ ] `loadSettings()` / `saveSettings()` 확장
- [ ] `buildGameModeConfig()` 메서드 추가

### Phase 3
- [ ] `ModeSelectionPopup` UI 컴포넌트 생성
- [ ] `PopupManager`에 모드 선택 기능 추가
- [ ] CSS 스타일 추가
- [ ] `game-view.fxml`에 오버레이 추가

### Phase 4
- [ ] `SingleMode` 수정 (PlayType, GameplayType 메서드)
- [ ] `MultiMode` 기본 구현

### Phase 5
- [ ] `MainController` 수정
- [ ] `NavigationService` 확장
- [ ] `GameController` 통합
- [ ] `BoardController` SRS 설정 반영
- [ ] 전체 테스트

---

## 🎊 완료 후 기대 효과

✅ **기존 시스템과 완벽 통합**
- PopupManager 일관성 유지
- SettingsService 단일 진입점
- application.properties 중앙 관리

✅ **개발 효율성**
- 새로운 화면 불필요
- CSS 재사용
- 3주 만에 완성

✅ **확장성**
- 새로운 모드 추가 용이
- 설정 항목 확장 간단
- 향후 온라인 멀티 대비

---

**이제 시작하시겠습니까? 어느 Phase부터 진행할까요?** 🚀
