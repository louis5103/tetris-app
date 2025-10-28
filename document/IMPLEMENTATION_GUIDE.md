# 🚀 Tetris 프로젝트 전체 구현 가이드

> **총 기간**: 약 30일 (6주)  
> **난이도**: ⭐⭐⭐⭐ (중상급)  
> **목표**: 게임 모드 선택 시스템 + UI 테마 상점 완성  
> **작성일**: 2025-10-29

---

## 📋 전체 로드맵 한눈에 보기

```
┌─────────────────────────────────────────────────────────┐
│                    전체 6주 계획                         │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Week 1-3: 게임 모드 선택 시스템 (핵심 기능)            │
│  ├─ Phase 0: 문서 숙지 및 준비 (1일)                    │
│  ├─ Phase 1: Core 모듈 확장 (3일)                       │
│  ├─ Phase 2: Settings & Properties (3일)               │
│  ├─ Phase 3: UI 모드 선택 팝업 (4일)                    │
│  ├─ Phase 4: Mode 클래스 구현 (3일)                     │
│  └─ Phase 5: 통합 및 테스트 (4일)                       │
│                                                         │
│  Week 4-6: UI 테마 상점 시스템 (확장 기능)              │
│  ├─ Phase 6: 데이터베이스 설계 (2일)                    │
│  ├─ Phase 7: 테마 서비스 구현 (3일)                     │
│  ├─ Phase 8: 테마 UI 구현 (4일)                         │
│  ├─ Phase 9: 최종 통합 테스트 (3일)                     │
│  └─ Phase 10: 문서화 및 배포 (2일)                      │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 Phase 0: 문서 숙지 및 준비 (1일)

### 목표
프로젝트 전체 구조를 이해하고 개발 환경을 확인합니다.

### 체크리스트

#### 1. 문서 읽기 (필수)
```bash
# 순서대로 읽기
□ GAME_MODE_IMPLEMENTATION_PLAN.md (1시간)
  → 3주 계획 전체 파악
  
□ GAME_MODE_IMPROVEMENTS.md 섹션 1-3 (30분)
  → @ConfigurationProperties 개념 이해
  
□ ARCHITECTURE_QUALITY_ASSESSMENT.md (30분)
  → 설계 표준 확인
  
□ FRAMEWORK_ARCHITECTURE_DESIGN.md 섹션 1-2 (1시간)
  → 테마 시스템 구조 파악
```

#### 2. 개발 환경 확인
```bash
# Java 21 확인
□ java -version
  # 출력: openjdk version "21"

# Gradle 빌드 테스트
□ cd /path/to/tetris-app
□ ./gradlew clean build
  # 빌드 성공 확인

# MySQL 연결 확인
□ mysql -u team9 -p
  # 접속 성공 확인
  
# Spring Boot 애플리케이션 실행 테스트
□ ./gradlew :tetris-client:run
  # JavaFX 창 뜨는지 확인
```

#### 3. Git 브랜치 전략 설정
```bash
# develop 브랜치에서 시작
□ git checkout develop
□ git pull origin develop

# 작업 브랜치 생성
□ git checkout -b feature/game-mode-selection
```

### 📝 완료 기준
- ✅ 4개 문서 모두 읽음
- ✅ 빌드 성공
- ✅ MySQL 연결 확인
- ✅ 작업 브랜치 생성 완료

---

## 🎯 Phase 1: Core 모듈 확장 (3일)

### 목표
게임 모드의 핵심 타입과 설정 객체를 만듭니다.

### Day 1: Enum 생성

#### 1.1 GameplayType Enum 생성
```bash
□ 파일 생성: tetris-core/src/main/java/seoultech/se/core/config/GameplayType.java
```

```java
package seoultech.se.core.config;

/**
 * 게임플레이 타입
 */
public enum GameplayType {
    CLASSIC("클래식", "전통적인 테트리스"),
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

#### 1.2 PlayType Enum 생성
```bash
□ 파일 생성: tetris-core/src/main/java/seoultech/se/core/mode/PlayType.java
```

```java
package seoultech.se.core.mode;

/**
 * 플레이 타입
 */
public enum PlayType {
    LOCAL_SINGLE("로컬 싱글", "혼자 플레이"),
    ONLINE_MULTI("온라인 멀티", "다른 플레이어와 대결");
    
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

#### 1.3 컴파일 테스트
```bash
□ ./gradlew :tetris-core:compileJava
  # 컴파일 성공 확인
```

### Day 2: GameModeConfig 확장

#### 2.1 GameModeConfig 수정
```bash
□ 파일 수정: tetris-core/src/main/java/seoultech/se/core/config/GameModeConfig.java
```

**추가할 필드:**
```java
// 기존 코드 유지 + 아래 추가

/**
 * SRS(Super Rotation System) 활성화 여부
 */
@Builder.Default
private final boolean srsEnabled = true;

/**
 * 게임플레이 타입
 */
@Builder.Default
private final GameplayType gameplayType = GameplayType.CLASSIC;
```

**추가할 프리셋 메서드:**
```java
/**
 * 클래식 모드 (SRS 옵션)
 */
public static GameModeConfig classic(boolean srsEnabled) {
    return GameModeConfig.builder()
        .gameplayType(GameplayType.CLASSIC)
        .srsEnabled(srsEnabled)
        .build();
}

/**
 * 아케이드 모드
 */
public static GameModeConfig arcade() {
    return GameModeConfig.builder()
        .gameplayType(GameplayType.ARCADE)
        .dropSpeedMultiplier(1.5)
        .lockDelay(300)
        .srsEnabled(true)
        .build();
}
```

### Day 3: 단위 테스트 작성

#### 3.1 GameModeConfig 테스트
```bash
□ 파일 생성: tetris-core/src/test/java/seoultech/se/core/config/GameModeConfigTest.java
```

```java
package seoultech.se.core.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameModeConfigTest {
    
    @Test
    void testClassicPreset() {
        GameModeConfig config = GameModeConfig.classic(true);
        
        assertEquals(GameplayType.CLASSIC, config.getGameplayType());
        assertTrue(config.isSrsEnabled());
    }
    
    @Test
    void testArcadePreset() {
        GameModeConfig config = GameModeConfig.arcade();
        
        assertEquals(GameplayType.ARCADE, config.getGameplayType());
        assertEquals(1.5, config.getDropSpeedMultiplier());
        assertTrue(config.isSrsEnabled());
    }
    
    @Test
    void testBuilder() {
        GameModeConfig config = GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .srsEnabled(false)
            .build();
        
        assertFalse(config.isSrsEnabled());
    }
}
```

#### 3.2 테스트 실행
```bash
□ ./gradlew :tetris-core:test
  # 모든 테스트 통과 확인
```

### 📝 Phase 1 완료 기준
- ✅ GameplayType.java 컴파일 성공
- ✅ PlayType.java 컴파일 성공
- ✅ GameModeConfig 확장 완료
- ✅ 단위 테스트 3개 이상 통과
- ✅ `./gradlew :tetris-core:build` 성공

---

## ⚙️ Phase 2: Settings & Properties 통합 (3일)

### 목표
타입 안전한 설정 관리 시스템을 구축합니다.

### Day 1: GameModeProperties 생성

#### 1.1 GameModeProperties 클래스
```bash
□ 파일 확인: tetris-client/src/main/java/seoultech/se/client/config/GameModeProperties.java
  # 이미 생성되어 있음 (확인만)
```

**내용 검증:**
```java
@Configuration
@ConfigurationProperties(prefix = "tetris.mode")
@Validated // ← 추가
@Getter
@Setter
public class GameModeProperties {
    
    @NotNull(message = "Play type must be specified") // ← 추가
    private PlayType playType = PlayType.LOCAL_SINGLE;
    
    @NotNull(message = "Gameplay type must be specified") // ← 추가
    private GameplayType gameplayType = GameplayType.CLASSIC;
    
    private boolean srsEnabled = true;
    
    // ... (나머지 필드)
}
```

#### 1.2 application.properties 설정
```bash
□ 파일 수정: tetris-client/application.properties
```

**추가할 내용:**
```properties
# ========== 게임 모드 설정 ==========
tetris.mode.play-type=${GAME_MODE_PLAY_TYPE:LOCAL_SINGLE}
tetris.mode.gameplay-type=${GAME_MODE_GAMEPLAY_TYPE:CLASSIC}
tetris.mode.srs-enabled=${GAME_MODE_SRS_ENABLED:true}

# 마지막 선택 (자동 저장)
tetris.mode.last-play-type=LOCAL_SINGLE
tetris.mode.last-gameplay-type=CLASSIC
tetris.mode.last-srs-enabled=true
```

### Day 2: SettingsService 확장

#### 2.1 SettingsService 수정
```bash
□ 파일 수정: tetris-client/src/main/java/seoultech/se/client/service/SettingsService.java
```

**추가할 필드:**
```java
@Autowired
private GameModeProperties gameModeProperties;
```

**추가할 메서드:**
```java
/**
 * 게임 모드 설정 빌드
 */
public GameModeConfig buildGameModeConfig() {
    try {
        GameplayType gameplayType = gameModeProperties.getGameplayType();
        boolean srsEnabled = gameModeProperties.isSrsEnabled();
        
        if (gameplayType == GameplayType.ARCADE) {
            return GameModeConfig.arcade()
                .toBuilder()
                .srsEnabled(srsEnabled)
                .build();
        } else {
            return GameModeConfig.classic(srsEnabled);
        }
    } catch (Exception e) {
        System.err.println("❗ Failed to build game mode config: " + e.getMessage());
        return GameModeConfig.classic(true); // 기본값 반환
    }
}

/**
 * 게임 모드 설정 저장
 */
public void saveGameModeSettings(PlayType playType, 
                                  GameplayType gameplayType, 
                                  boolean srsEnabled) {
    gameModeProperties.setLastPlayType(playType);
    gameModeProperties.setLastGameplayType(gameplayType);
    gameModeProperties.setLastSrsEnabled(srsEnabled);
    
    saveSettings(); // 기존 메서드 호출
}
```

### Day 3: 에러 처리 및 테스트

#### 3.1 유효성 검증 추가
```java
/**
 * 설정 유효성 검증
 */
public boolean validateGameModeSettings() {
    if (gameModeProperties.getPlayType() == null) {
        System.err.println("❗ PlayType is null, using default");
        gameModeProperties.setPlayType(PlayType.LOCAL_SINGLE);
        return false;
    }
    
    if (gameModeProperties.getGameplayType() == null) {
        System.err.println("❗ GameplayType is null, using default");
        gameModeProperties.setGameplayType(GameplayType.CLASSIC);
        return false;
    }
    
    return true;
}
```

#### 3.2 통합 테스트
```bash
□ ./gradlew :tetris-client:bootRun
  # 애플리케이션 실행 확인
  
□ 로그 확인:
  # "✅ Settings loaded successfully." 출력 확인
```

### 📝 Phase 2 완료 기준
- ✅ GameModeProperties @Validated 적용
- ✅ application.properties 설정 추가
- ✅ SettingsService.buildGameModeConfig() 구현
- ✅ 에러 처리 로직 3개 이상
- ✅ 애플리케이션 정상 실행

---

## 🎨 Phase 3: UI - 모드 선택 팝업 (4일)

### 목표
사용자가 게임 모드를 선택할 수 있는 팝업을 만듭니다.

### Day 1-2: ModeSelectionPopup 컴포넌트

#### 1.1 클래스 생성
```bash
□ 파일 생성: tetris-client/src/main/java/seoultech/se/client/ui/ModeSelectionPopup.java
```

```java
package seoultech.se.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import lombok.Getter;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.mode.PlayType;

/**
 * 게임 모드 선택 팝업 컴포넌트
 */
public class ModeSelectionPopup extends VBox {
    
    @Getter
    private PlayType selectedPlayType = PlayType.LOCAL_SINGLE;
    
    @Getter
    private GameplayType selectedGameplayType = GameplayType.CLASSIC;
    
    @Getter
    private boolean srsEnabled = true;
    
    private final ToggleGroup playTypeGroup;
    private final ToggleGroup gameplayTypeGroup;
    private final CheckBox srsCheckBox;
    
    public ModeSelectionPopup() {
        super(20);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(40));
        getStyleClass().add("mode-selection-popup");
        
        // 제목
        Label title = new Label("게임 모드 선택");
        title.getStyleClass().add("popup-title");
        
        // ========== 플레이 타입 선택 ==========
        Label playTypeLabel = new Label("플레이 타입:");
        playTypeLabel.getStyleClass().add("section-label");
        
        playTypeGroup = new ToggleGroup();
        
        RadioButton singleRadio = new RadioButton(PlayType.LOCAL_SINGLE.getDisplayName());
        singleRadio.setToggleGroup(playTypeGroup);
        singleRadio.setSelected(true);
        singleRadio.setUserData(PlayType.LOCAL_SINGLE);
        
        RadioButton multiRadio = new RadioButton(PlayType.ONLINE_MULTI.getDisplayName());
        multiRadio.setToggleGroup(playTypeGroup);
        multiRadio.setUserData(PlayType.ONLINE_MULTI);
        
        VBox playTypeBox = new VBox(10, playTypeLabel, singleRadio, multiRadio);
        
        // ========== 게임플레이 타입 선택 ==========
        Label gameplayLabel = new Label("게임플레이 타입:");
        gameplayLabel.getStyleClass().add("section-label");
        
        gameplayTypeGroup = new ToggleGroup();
        
        RadioButton classicRadio = new RadioButton(GameplayType.CLASSIC.getDisplayName());
        classicRadio.setToggleGroup(gameplayTypeGroup);
        classicRadio.setSelected(true);
        classicRadio.setUserData(GameplayType.CLASSIC);
        
        RadioButton arcadeRadio = new RadioButton(GameplayType.ARCADE.getDisplayName());
        arcadeRadio.setToggleGroup(gameplayTypeGroup);
        arcadeRadio.setUserData(GameplayType.ARCADE);
        
        VBox gameplayBox = new VBox(10, gameplayLabel, classicRadio, arcadeRadio);
        
        // ========== SRS 옵션 ==========
        srsCheckBox = new CheckBox("SRS (Super Rotation System) 활성화");
        srsCheckBox.setSelected(true);
        
        // ========== 버튼 ==========
        Button startButton = new Button("게임 시작");
        startButton.getStyleClass().add("primary-button");
        startButton.setOnAction(e -> handleStart());
        
        Button cancelButton = new Button("취소");
        cancelButton.setOnAction(e -> handleCancel());
        
        HBox buttonBox = new HBox(10, startButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);
        
        // ========== 레이아웃 ==========
        getChildren().addAll(
            title,
            new Separator(),
            playTypeBox,
            new Separator(),
            gameplayBox,
            new Separator(),
            srsCheckBox,
            buttonBox
        );
    }
    
    private void handleStart() {
        // 선택 값 저장
        selectedPlayType = (PlayType) playTypeGroup.getSelectedToggle().getUserData();
        selectedGameplayType = (GameplayType) gameplayTypeGroup.getSelectedToggle().getUserData();
        srsEnabled = srsCheckBox.isSelected();
        
        // 콜백 호출 (PopupManager에서 설정)
        if (onStartCallback != null) {
            onStartCallback.run();
        }
    }
    
    private void handleCancel() {
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
    }
    
    private Runnable onStartCallback;
    private Runnable onCancelCallback;
    
    public void setOnStart(Runnable callback) {
        this.onStartCallback = callback;
    }
    
    public void setOnCancel(Runnable callback) {
        this.onCancelCallback = callback;
    }
}
```

### Day 3: PopupManager 확장

#### 3.1 PopupManager 수정
```bash
□ 파일 수정: tetris-client/src/main/java/seoultech/se/client/ui/PopupManager.java
```

**추가할 필드:**
```java
private VBox modeSelectionOverlay;
private ModeSelectionPopup modeSelectionPopup;
```

**추가할 메서드:**
```java
/**
 * 모드 선택 팝업 초기화
 */
public void initModeSelectionPopup(VBox overlay) {
    this.modeSelectionOverlay = overlay;
    this.modeSelectionPopup = new ModeSelectionPopup();
    
    modeSelectionOverlay.getChildren().clear();
    modeSelectionOverlay.getChildren().add(modeSelectionPopup);
    modeSelectionOverlay.setVisible(false);
}

/**
 * 모드 선택 팝업 표시
 */
public void showModeSelectionPopup(Runnable onStart, Runnable onCancel) {
    if (modeSelectionOverlay != null && modeSelectionPopup != null) {
        Platform.runLater(() -> {
            modeSelectionPopup.setOnStart(() -> {
                hideModeSelectionPopup();
                onStart.run();
            });
            
            modeSelectionPopup.setOnCancel(() -> {
                hideModeSelectionPopup();
                if (onCancel != null) {
                    onCancel.run();
                }
            });
            
            modeSelectionOverlay.setVisible(true);
        });
    }
}

/**
 * 모드 선택 팝업 숨기기
 */
public void hideModeSelectionPopup() {
    if (modeSelectionOverlay != null) {
        Platform.runLater(() -> {
            modeSelectionOverlay.setVisible(false);
        });
    }
}

/**
 * 선택된 모드 정보 가져오기
 */
public ModeSelectionPopup getModeSelectionPopup() {
    return modeSelectionPopup;
}
```

### Day 4: FXML 및 CSS

#### 4.1 game-view.fxml 수정
```bash
□ 파일 수정: tetris-client/src/main/resources/view/game-view.fxml
```

**추가할 VBox:**
```xml
<!-- 기존 pauseOverlay, gameOverOverlay 아래에 추가 -->

<!-- 모드 선택 오버레이 -->
<VBox fx:id="modeSelectionOverlay" 
      styleClass="overlay" 
      visible="false"
      StackPane.alignment="CENTER">
    <!-- ModeSelectionPopup이 여기에 동적으로 추가됨 -->
</VBox>
```

#### 4.2 CSS 스타일 추가
```bash
□ 파일 수정: tetris-client/src/main/resources/css/application.css
```

**추가할 스타일:**
```css
/* ========== 모드 선택 팝업 ========== */
.mode-selection-popup {
    -fx-background-color: rgba(44, 62, 80, 0.95);
    -fx-background-radius: 15px;
    -fx-border-color: #3498db;
    -fx-border-width: 2px;
    -fx-border-radius: 15px;
    -fx-padding: 40px;
    -fx-min-width: 400px;
    -fx-max-width: 500px;
}

.mode-selection-popup .popup-title {
    -fx-font-size: 28px;
    -fx-font-weight: bold;
    -fx-text-fill: white;
}

.mode-selection-popup .section-label {
    -fx-font-size: 16px;
    -fx-font-weight: bold;
    -fx-text-fill: #3498db;
}

.mode-selection-popup .radio-button {
    -fx-text-fill: white;
    -fx-font-size: 14px;
}

.mode-selection-popup .check-box {
    -fx-text-fill: white;
    -fx-font-size: 14px;
}

.mode-selection-popup .primary-button {
    -fx-background-color: #27ae60;
    -fx-text-fill: white;
    -fx-font-size: 16px;
    -fx-font-weight: bold;
    -fx-padding: 10px 30px;
    -fx-background-radius: 8px;
}

.mode-selection-popup .primary-button:hover {
    -fx-background-color: #2ecc71;
    -fx-cursor: hand;
}
```

### 📝 Phase 3 완료 기준
- ✅ ModeSelectionPopup 컴파일 성공
- ✅ PopupManager 확장 완료
- ✅ game-view.fxml에 overlay 추가
- ✅ CSS 스타일 적용
- ✅ 팝업 표시/숨기기 동작 확인

---

## 🎮 Phase 4: Mode 클래스 구현 (3일)

### 목표
SingleMode, MultiMode에 새로운 모드 정보를 통합합니다.

### Day 1: GameMode 인터페이스 확장

#### 1.1 GameMode 인터페이스 수정
```bash
□ 파일 확인: tetris-client/src/main/java/seoultech/se/client/mode/GameMode.java
```

**추가할 메서드:**
```java
/**
 * 플레이 타입 반환
 */
PlayType getPlayType();

/**
 * 게임플레이 타입 반환
 */
GameplayType getGameplayType();
```

### Day 2: SingleMode 업데이트

#### 2.1 SingleMode 수정
```bash
□ 파일 수정: tetris-client/src/main/java/seoultech/se/client/mode/SingleMode.java
```

**추가/수정할 내용:**
```java
import seoultech.se.core.mode.PlayType;
import seoultech.se.core.config.GameplayType;

public class SingleMode implements GameMode {
    
    private final GameModeConfig config;
    
    public SingleMode(GameModeConfig config) {
        this.config = config;
    }
    
    @Override
    public PlayType getPlayType() {
        return PlayType.LOCAL_SINGLE;
    }
    
    @Override
    public GameplayType getGameplayType() {
        return config.getGameplayType();
    }
    
    @Override
    public void start() {
        System.out.println("🎮 Starting Single Mode");
        System.out.println("  - Gameplay: " + getGameplayType().getDisplayName());
        System.out.println("  - SRS: " + (config.isSrsEnabled() ? "ON" : "OFF"));
        
        // 기존 게임 시작 로직...
    }
}
```

### Day 3: MultiMode 생성

#### 3.1 MultiMode 클래스
```bash
□ 파일 생성: tetris-client/src/main/java/seoultech/se/client/mode/MultiMode.java
```

```java
package seoultech.se.client.mode;

import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.mode.PlayType;

/**
 * 온라인 멀티플레이 모드
 */
public class MultiMode implements GameMode {
    
    private final GameModeConfig config;
    
    public MultiMode(GameModeConfig config) {
        this.config = config;
    }
    
    @Override
    public PlayType getPlayType() {
        return PlayType.ONLINE_MULTI;
    }
    
    @Override
    public GameplayType getGameplayType() {
        return config.getGameplayType();
    }
    
    @Override
    public void start() {
        System.out.println("🌐 Starting Multi Mode");
        System.out.println("  - Gameplay: " + getGameplayType().getDisplayName());
        System.out.println("  - SRS: " + (config.isSrsEnabled() ? "ON" : "OFF"));
        
        // TODO: 멀티플레이 로직 구현
        System.out.println("⚠️ Multi mode is not implemented yet");
    }
}
```

### 📝 Phase 4 완료 기준
- ✅ GameMode 인터페이스 확장
- ✅ SingleMode 업데이트 완료
- ✅ MultiMode 클래스 생성
- ✅ 컴파일 에러 없음

---

## 🔗 Phase 5: 통합 및 테스트 (4일)

### 목표
모든 컴포넌트를 연결하고 전체 흐름을 테스트합니다.

### Day 1-2: Controller 통합

#### 1.1 GameController 수정
```bash
□ 파일 수정: tetris-client/src/main/java/seoultech/se/client/controller/GameController.java
```

**추가할 필드:**
```java
@Autowired
private SettingsService settingsService;

@Autowired
private PopupManager popupManager;

private GameMode currentMode;
```

**추가할 메서드:**
```java
/**
 * 게임 초기화
 */
@PostConstruct
public void initialize() {
    // 모드 선택 팝업 초기화
    popupManager.initModeSelectionPopup(modeSelectionOverlay);
}

/**
 * 모드 선택 완료 처리
 */
public void handleModeSelected() {
    ModeSelectionPopup popup = popupManager.getModeSelectionPopup();
    
    PlayType playType = popup.getSelectedPlayType();
    GameplayType gameplayType = popup.getSelectedGameplayType();
    boolean srsEnabled = popup.isSrsEnabled();
    
    // 설정 저장
    settingsService.saveGameModeSettings(playType, gameplayType, srsEnabled);
    
    // GameModeConfig 빌드
    GameModeConfig config = settingsService.buildGameModeConfig();
    
    // 모드 인스턴스 생성
    if (playType == PlayType.LOCAL_SINGLE) {
        currentMode = new SingleMode(config);
    } else {
        currentMode = new MultiMode(config);
    }
    
    // 게임 시작
    try {
        currentMode.start();
        startGame();
    } catch (Exception e) {
        System.err.println("❗ Failed to start game: " + e.getMessage());
        resetToDefaultMode();
    }
}

/**
 * 기본 모드로 복구
 */
private void resetToDefaultMode() {
    System.out.println("⚠️ Resetting to default mode...");
    GameModeConfig defaultConfig = GameModeConfig.classic(true);
    currentMode = new SingleMode(defaultConfig);
    currentMode.start();
    startGame();
}

/**
 * 게임 시작 (기존 로직)
 */
private void startGame() {
    // 기존 게임 시작 코드...
    System.out.println("✅ Game started successfully");
}
```

#### 1.2 MainController 수정
```bash
□ 파일 수정: tetris-client/src/main/java/seoultech/se/client/controller/MainController.java
```

**게임 시작 버튼 이벤트:**
```java
@FXML
private void handleStartGame() {
    // 모드 선택 팝업 표시
    popupManager.showModeSelectionPopup(
        () -> {
            // 게임 시작
            gameController.handleModeSelected();
        },
        () -> {
            // 취소
            System.out.println("Game start cancelled");
        }
    );
}
```

### Day 3: 통합 테스트

#### 3.1 수동 테스트 시나리오
```bash
□ ./gradlew :tetris-client:bootRun
```

**테스트 체크리스트:**
```
□ 시나리오 1: 클래식 싱글 모드
  1. "게임 시작" 버튼 클릭
  2. 모드 선택 팝업 표시 확인
  3. "로컬 싱글" + "클래식" 선택
  4. SRS 체크박스 ON
  5. "게임 시작" 클릭
  6. 콘솔 출력 확인:
     "🎮 Starting Single Mode"
     "  - Gameplay: 클래식"
     "  - SRS: ON"
     "✅ Game started successfully"

□ 시나리오 2: 아케이드 싱글 모드
  1-2. 동일
  3. "로컬 싱글" + "아케이드" 선택
  4. SRS 체크박스 ON
  5-6. 게임 시작 확인

□ 시나리오 3: 멀티 모드 (미구현)
  1-2. 동일
  3. "온라인 멀티" 선택
  4-5. 게임 시작
  6. 콘솔 출력 확인:
     "🌐 Starting Multi Mode"
     "⚠️ Multi mode is not implemented yet"

□ 시나리오 4: 취소 버튼
  1-2. 동일
  3. "취소" 버튼 클릭
  4. 팝업 닫힘 확인
  5. 메인 화면 유지 확인
```

### Day 4: 버그 수정 및 최적화

#### 4.1 발견된 버그 수정
```bash
□ 컴파일 에러 모두 해결
□ NPE (NullPointerException) 방지 코드 추가
□ 로그 메시지 정리
```

#### 4.2 코드 리뷰 체크리스트
```
□ 모든 public 메서드에 JavaDoc 주석
□ @Autowired 누락 없는지 확인
□ Exception handling 적절한지 확인
□ 콘솔 출력 System.out.println → Logger 변경 (선택)
```

### 📝 Phase 5 완료 기준
- ✅ GameController 통합 완료
- ✅ MainController 통합 완료
- ✅ 4개 시나리오 모두 통과
- ✅ 버그 0개
- ✅ 코드 리뷰 완료

---

## 🗄️ Phase 6: 데이터베이스 설계 (테마 준비, 2일)

### 목표
UI 테마 시스템을 위한 데이터베이스 스키마를 설계합니다.

### Day 1: JPA 엔티티 생성

#### 6.1 Theme 엔티티
```bash
□ 파일 생성: tetris-backend/src/main/java/seoultech/se/backend/entity/Theme.java
```

**전체 코드는 FRAMEWORK_ARCHITECTURE_DESIGN.md 섹션 2.2.1 참조**

핵심 필드:
- id (PK)
- themeCode (고유 코드)
- displayName
- cssFilePath
- price
- type (FREE/PREMIUM/EXCLUSIVE)

#### 6.2 UserTheme 엔티티
```bash
□ 파일 생성: tetris-backend/src/main/java/seoultech/se/backend/entity/UserTheme.java
```

핵심 필드:
- userId
- theme (FK)
- purchasedAt
- paidPoints

#### 6.3 UserSettings 엔티티 (확장)
```bash
□ 파일 생성: tetris-backend/src/main/java/seoultech/se/backend/entity/UserSettings.java
```

새로 추가할 필드:
- selectedTheme (FK)
- lastPlayType
- lastGameplayType
- lastSrsEnabled

### Day 2: Repository 인터페이스

#### 6.4 Repository 생성
```bash
□ tetris-backend/src/main/java/seoultech/se/backend/repository/ThemeRepository.java
□ tetris-backend/src/main/java/seoultech/se/backend/repository/UserThemeRepository.java
□ tetris-backend/src/main/java/seoultech/se/backend/repository/UserSettingsRepository.java
```

**전체 코드는 FRAMEWORK_ARCHITECTURE_DESIGN.md 섹션 2.3 참조**

#### 6.5 data.sql 초기화 스크립트
```bash
□ 파일 수정: tetris-backend/src/main/resources/data.sql
```

```sql
-- 기본 테마 초기화
INSERT INTO themes (theme_code, display_name, description, css_file_path, preview_image_path, price, type, active, created_at)
VALUES 
('classic', '클래식', '전통적인 테트리스 스타일', '/css/themes/classic.css', '/image/themes/classic.png', 0, 'FREE', true, NOW()),
('minimalist', '미니멀', '심플하고 깔끔한 디자인', '/css/themes/minimalist.css', '/image/themes/minimalist.png', 0, 'FREE', true, NOW()),
('neon_glow', '네온 글로우', '화려한 네온 효과', '/css/themes/neon-glow.css', '/image/themes/neon.png', 100, 'PREMIUM', true, NOW());
```

### 📝 Phase 6 완료 기준
- ✅ 3개 엔티티 모두 생성
- ✅ 3개 Repository 생성
- ✅ data.sql 스크립트 작성
- ✅ `./gradlew :tetris-backend:build` 성공

---

## 🛠️ Phase 7: 테마 서비스 구현 (3일)

### 목표
테마 관리 비즈니스 로직을 구현합니다.

### Day 1: ThemeService

```bash
□ 파일 생성: tetris-backend/src/main/java/seoultech/se/backend/service/ThemeService.java
```

**전체 코드는 FRAMEWORK_ARCHITECTURE_DESIGN.md 섹션 2.4.1 참조**

핵심 메서드:
- getAllActiveThemes()
- getThemesByType()
- purchaseTheme()
- grantFreeThemes()

### Day 2: UserSettingsService

```bash
□ 파일 생성: tetris-backend/src/main/java/seoultech/se/backend/service/UserSettingsService.java
```

핵심 메서드:
- getUserSettings()
- applyTheme()
- saveGameModeSettings()

### Day 3: Client-Side Properties

#### 7.1 ThemeProperties
```bash
□ 파일 생성: tetris-client/src/main/java/seoultech/se/client/config/ThemeProperties.java
```

#### 7.2 application.properties 추가
```properties
# ========== 테마 설정 ==========
tetris.theme.selected-theme-code=${THEME_CODE:classic}
tetris.theme.css-base-path=/css/themes/
tetris.theme.image-base-path=/image/themes/
tetris.theme.auto-apply=true
```

### 📝 Phase 7 완료 기준
- ✅ ThemeService 구현
- ✅ UserSettingsService 확장
- ✅ ThemeProperties 생성
- ✅ 단위 테스트 5개 이상

---

## 🎨 Phase 8: 테마 UI 구현 (4일)

### 목표
사용자가 테마를 선택하고 적용할 수 있는 UI를 만듭니다.

### Day 1-2: ThemeManager

```bash
□ 파일 생성: tetris-client/src/main/java/seoultech/se/client/service/ThemeManager.java
```

**전체 코드는 FRAMEWORK_ARCHITECTURE_DESIGN.md 섹션 2.5.2 참조**

핵심 메서드:
- registerScene()
- applyTheme()
- applyThemeToScene()

### Day 3: ThemeStoreController

```bash
□ 파일 생성: tetris-client/src/main/java/seoultech/se/client/controller/ThemeStoreController.java
```

핵심 메서드:
- loadThemes()
- selectTheme()
- handlePurchase()

### Day 4: CSS 테마 파일

```bash
□ tetris-client/src/main/resources/css/themes/classic.css
□ tetris-client/src/main/resources/css/themes/neon-glow.css
□ tetris-client/src/main/resources/css/themes/retro.css
```

**기본 템플릿:**
```css
/* classic.css */
.root {
    -fx-base: #2c3e50;
    -fx-accent: #3498db;
    -fx-background: #ecf0f1;
}

.block {
    -fx-border-color: #34495e;
    -fx-border-width: 1px;
}

/* 블록별 색상 */
.block-I { -fx-background-color: #00ffff; }
.block-O { -fx-background-color: #ffff00; }
.block-T { -fx-background-color: #ff00ff; }
/* ... */
```

### 📝 Phase 8 완료 기준
- ✅ ThemeManager 구현
- ✅ ThemeStoreController 구현
- ✅ 3개 테마 CSS 작성
- ✅ 테마 전환 동작 확인

---

## ✅ Phase 9: 최종 통합 테스트 (3일)

### 목표
전체 시스템이 완벽하게 동작하는지 검증합니다.

### Day 1: 통합 테스트 시나리오

```bash
□ 시나리오 1: 게임 모드 + 테마 통합
  1. 애플리케이션 시작
  2. 테마 상점에서 "네온 글로우" 선택
  3. 테마 적용 확인
  4. "게임 시작" → 모드 선택 팝업
  5. "아케이드" 선택
  6. 게임 시작 확인
  7. 네온 테마로 게임 진행

□ 시나리오 2: 영속성 테스트
  1. 설정 변경 (테마 + 모드)
  2. 애플리케이션 종료
  3. 애플리케이션 재시작
  4. 이전 설정 복원 확인

□ 시나리오 3: 에러 복구
  1. DB 연결 끊기
  2. 애플리케이션 시작
  3. 기본값으로 동작 확인
  4. DB 재연결
  5. 정상 동작 확인
```

### Day 2: 성능 테스트

```bash
□ 테마 전환 속도 측정 (< 500ms 목표)
□ 메모리 누수 확인 (VisualVM 사용)
□ 게임 FPS 확인 (60fps 유지)
```

### Day 3: 버그 수정

```bash
□ 발견된 모든 버그 수정
□ 회귀 테스트 실행
□ 최종 코드 리뷰
```

### 📝 Phase 9 완료 기준
- ✅ 모든 시나리오 통과
- ✅ 성능 목표 달성
- ✅ 버그 0개
- ✅ 코드 커버리지 > 80%

---

## 📝 Phase 10: 문서화 및 배포 준비 (2일)

### 목표
프로젝트를 완료하고 배포 준비를 합니다.

### Day 1: 문서 작성

#### 10.1 README.md 업데이트
```bash
□ 파일 수정: /README.md
```

**추가할 섹션:**
```markdown
## 🎮 새로운 기능

### 게임 모드 선택
- **플레이 타입**: 로컬 싱글 / 온라인 멀티
- **게임플레이**: 클래식 / 아케이드
- **SRS 옵션**: Super Rotation System On/Off

### UI 테마 시스템
- **무료 테마**: 클래식, 미니멀
- **프리미엄 테마**: 네온 글로우, 레트로 아케이드 (포인트 구매)
- **테마 상점**: 미리보기, 구매, 적용

## 🛠️ 기술 스택
- Java 21 LTS
- Spring Boot 3.x
- JavaFX 21
- MySQL 8.x
- JPA / Hibernate
- Lombok
```

#### 10.2 API 문서
```bash
□ 파일 생성: /document/API_DOCUMENTATION.md
```

주요 API:
- ThemeService
- UserSettingsService
- GameModeProperties

### Day 2: 배포 준비

#### 10.3 JAR 빌드
```bash
□ ./gradlew clean build
□ ./gradlew :tetris-client:bootJar
```

#### 10.4 배포 스크립트
```bash
□ 파일 생성: /script_files/deploy.sh
```

```bash
#!/bin/bash

echo "🚀 Deploying Tetris Application..."

# 1. 빌드
./gradlew clean build

# 2. JAR 복사
cp tetris-client/build/libs/tetris-client-*.jar ./deploy/

# 3. 설정 파일 복사
cp tetris-client/application.properties ./deploy/

# 4. 실행 스크립트 생성
cat > ./deploy/run.sh << 'EOF'
#!/bin/bash
java -jar tetris-client-*.jar
EOF

chmod +x ./deploy/run.sh

echo "✅ Deployment complete!"
```

### 📝 Phase 10 완료 기준
- ✅ README.md 업데이트
- ✅ API 문서 작성
- ✅ JAR 빌드 성공
- ✅ 배포 스크립트 작성
- ✅ 최종 Git 커밋

---

## 🎯 전체 완료 체크리스트

### 필수 항목 (Must Have)
```
□ Phase 0: 문서 숙지 ✅
□ Phase 1: Core 모듈 확장 ✅
□ Phase 2: Settings & Properties ✅
□ Phase 3: UI 모드 선택 팝업 ✅
□ Phase 4: Mode 클래스 구현 ✅
□ Phase 5: 통합 및 테스트 ✅
□ 모든 단위 테스트 통과 ✅
□ 통합 테스트 시나리오 통과 ✅
```

### 권장 항목 (Should Have)
```
□ Phase 6: 데이터베이스 설계 ✅
□ Phase 7: 테마 서비스 구현 ✅
□ Phase 8: 테마 UI 구현 ✅
□ Phase 9: 최종 통합 테스트 ✅
□ SLF4J 로깅 추가
□ 애니메이션 효과 추가
```

### 선택 항목 (Nice to Have)
```
□ Phase 10: 문서화 및 배포
□ 키보드 단축키
□ 설정 프리셋
□ 통계 시스템
```

---

## 🚀 빠른 시작 가이드

### 지금 당장 시작하려면?

```bash
# 1. 문서 읽기 (1시간)
□ GAME_MODE_IMPLEMENTATION_PLAN.md 읽기

# 2. 환경 확인 (10분)
□ java -version  # Java 21 확인
□ ./gradlew clean build  # 빌드 테스트

# 3. 브랜치 생성 (1분)
□ git checkout -b feature/game-mode-selection

# 4. Phase 1 시작 (30분)
□ GameplayType.java 생성
□ PlayType.java 생성

# 5. 첫 커밋
□ git add .
□ git commit -m "feat: Add GameplayType and PlayType enums"
```

### 일정이 촉박하다면?

**최소 구현 (Week 1-3만):**
```
✅ Phase 0-5만 완료 → 게임 모드 선택 동작
⏭️ Phase 6-10 스킵 → 테마는 추후 추가
```

**완전 구현 (Week 1-6):**
```
✅ Phase 0-10 모두 완료 → 풀 시스템
```

---

## 💡 트러블슈팅

### 자주 발생하는 문제

#### 1. 컴파일 에러: "Cannot find symbol GameplayType"
```bash
해결:
□ tetris-core 모듈 빌드 확인
□ ./gradlew :tetris-core:build
□ IntelliJ: Gradle Refresh
```

#### 2. Spring Boot 실행 실패
```bash
해결:
□ application.properties 설정 확인
□ MySQL 연결 확인
□ @Autowired 누락 확인
```

#### 3. JavaFX 팝업이 안 보임
```bash
해결:
□ FXML에 fx:id 설정 확인
□ @FXML 어노테이션 확인
□ PopupManager.initModeSelectionPopup() 호출 확인
```

---

## 📞 도움이 필요하면?

### 참고 문서
1. `GAME_MODE_IMPLEMENTATION_PLAN.md` - 상세 구현 가이드
2. `GAME_MODE_IMPROVEMENTS.md` - 개선 팁
3. `ARCHITECTURE_QUALITY_ASSESSMENT.md` - 설계 검증
4. `FRAMEWORK_ARCHITECTURE_DESIGN.md` - 테마 시스템

### 외부 리소스
- [Spring Boot 공식 문서](https://spring.io/projects/spring-boot)
- [JavaFX 공식 문서](https://openjfx.io/)
- [JPA 가이드](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/)

---

**작성자**: GitHub Copilot  
**최종 수정**: 2025-10-29  
**버전**: 1.0

**행운을 빕니다! 🍀**
