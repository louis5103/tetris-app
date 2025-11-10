# 🔧 게임 모드 선택 시스템 - 개선 제안서

> **기준 문서**: GAME_MODE_IMPLEMENTATION_PLAN.md v3.0  
> **작성일**: 2025-10-29

---

## 📋 개선 제안 요약

### ⭐ 우선순위 높음 (필수)
1. **설정 타입 안전성 강화** - `@ConfigurationProperties` 도입
2. **에러 처리 및 검증 로직** - 설정 유효성 검사
3. **게임 시작 흐름 개선** - 모드 미선택 시 처리

### 🔸 우선순위 중간 (권장)
4. **로깅 체계화** - SLF4J 활용
5. **애니메이션 효과** - 팝업 전환 부드럽게
6. **키보드 단축키** - 모드 선택 팝업 조작

### 🔹 우선순위 낮음 (선택)
7. **설정 프리셋 시스템** - 즐겨찾기 모드
8. **통계 및 분석** - 모드별 플레이 기록
9. **다국어 지원 준비** - i18n 구조

---

## 1️⃣ 설정 타입 안전성 강화 ⭐ **필수**

### 문제점
현재 계획에서 `SettingsService`는 Properties 파일을 직접 다룸:
```java
// 문제: 문자열 기반, 타입 안전하지 않음
String playTypeStr = props.getProperty("playType", defaultPlayType);
PlayType playType = PlayType.valueOf(playTypeStr); // 런타임 에러 가능
```

### 해결책: Spring `@ConfigurationProperties` 활용

**장점**:
- ✅ **타입 안전성**: String → Enum 자동 변환
- ✅ **유효성 검증**: `@Validated` + `@NotNull` 등 사용 가능
- ✅ **IDE 지원**: 자동완성, 리팩토링 안전
- ✅ **테스트 용이**: Mock 객체 생성 쉬움
- ✅ **환경 변수 통합**: 자동으로 `${ENV_VAR}` 처리

**구현 파일**: `tetris-client/src/main/java/seoultech/se/client/config/GameModeProperties.java`
*(이미 생성됨)*

**SettingsService 수정**:
```java
@Service
public class SettingsService {
    
    @Autowired
    private GameModeProperties gameModeProperties; // ⭐ 주입
    
    // Properties 파일은 UI 설정만 (soundVolume, colorMode 등)
    // 게임 모드 설정은 GameModeProperties 사용
    
    public GameModeConfig buildGameModeConfig() {
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
    }
}
```

**application.properties 예시**:
```properties
# 타입 안전하게 자동 매핑됨
tetris.mode.play-type=LOCAL_SINGLE
tetris.mode.gameplay-type=CLASSIC
tetris.mode.srs-enabled=true

# 환경 변수도 자동 처리
tetris.mode.play-type=${GAME_MODE_PLAY_TYPE:LOCAL_SINGLE}
```

---

## 2️⃣ 에러 처리 및 검증 로직 ⭐ **필수**

### 문제점
- 잘못된 설정 값 처리 안 됨
- 네트워크 오류 시 멀티모드 처리 없음
- 모드 전환 실패 시 복구 로직 없음

### 해결책 1: 설정 유효성 검증

**GameModeProperties에 추가**:
```java
@Configuration
@ConfigurationProperties(prefix = "tetris.mode")
@Validated // ⭐ 추가
@Getter
@Setter
public class GameModeProperties {
    
    @NotNull(message = "Play type must not be null")
    private PlayType playType = PlayType.LOCAL_SINGLE;
    
    @NotNull(message = "Gameplay type must not be null")
    private GameplayType gameplayType = GameplayType.CLASSIC;
    
    /**
     * Bean 생성 후 검증
     */
    @PostConstruct
    public void validate() {
        if (playType == null || gameplayType == null) {
            throw new IllegalStateException("Invalid game mode configuration");
        }
        
        System.out.println("✅ GameModeProperties validated successfully");
        System.out.println("  - Play Type: " + playType);
        System.out.println("  - Gameplay Type: " + gameplayType);
    }
}
```

### 해결책 2: 모드 선택 실패 처리

**ModeSelectionPopup에 추가**:
```java
private void handleStart() {
    if (callback != null) {
        try {
            PlayType selectedPlayType = getSelectedPlayType();
            GameplayType selectedGameplayType = getSelectedGameplayType();
            
            // ⭐ 온라인 멀티 검증 (아직 미구현)
            if (selectedPlayType == PlayType.ONLINE_MULTI) {
                // TODO: 네트워크 연결 확인
                // if (!NetworkClient.isAvailable()) {
                //     showAlert("온라인 멀티플레이를 사용할 수 없습니다.\n네트워크 연결을 확인해주세요.");
                //     return;
                // }
                showAlert("온라인 멀티플레이는 향후 업데이트 예정입니다.");
                return;
            }
            
            callback.onStartGame(selectedPlayType, selectedGameplayType, srsCheckBox.isSelected());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to start game: " + e.getMessage());
            showAlert("게임 시작 중 오류가 발생했습니다.\n" + e.getMessage());
        }
    }
}
```

### 해결책 3: GameController 에러 처리

**GameController에 추가**:
```java
private void handleModeSelected(PlayType playType, GameplayType gameplayType, boolean srsEnabled) {
    try {
        System.out.println("🎮 모드 선택 완료:");
        System.out.println("  - PlayType: " + playType.getDisplayName());
        System.out.println("  - GameplayType: " + gameplayType.getDisplayName());
        System.out.println("  - SRS: " + (srsEnabled ? "ON" : "OFF"));
        
        // 설정 저장
        gameModeProperties.setPlayType(playType);
        gameModeProperties.setGameplayType(gameplayType);
        gameModeProperties.setSrsEnabled(srsEnabled);
        
        // ⭐ 검증
        if (!gameModeProperties.isValid()) {
            throw new IllegalStateException("Invalid game mode configuration");
        }
        
        // GameModeConfig 생성
        GameModeConfig config = settingsService.buildGameModeConfig();
        
        // GameMode 선택
        GameMode gameMode = selectGameMode(playType, config);
        
        // ⭐ null 체크
        if (gameMode == null) {
            throw new IllegalStateException("Failed to create game mode");
        }
        
        // BoardController에 주입
        boardController.setGameMode(gameMode);
        
        // 팝업 숨기고 게임 시작
        popupManager.hideModeSelectionPopup();
        startGame();
        
    } catch (Exception e) {
        System.err.println("❌ Failed to initialize game mode: " + e.getMessage());
        e.printStackTrace();
        
        // ⭐ 사용자에게 알림
        showErrorDialog("게임 모드 초기화 실패", 
                       "게임을 시작할 수 없습니다.\n" + e.getMessage());
        
        // ⭐ 기본 설정으로 복구
        resetToDefaultMode();
    }
}

private GameMode selectGameMode(PlayType playType, GameModeConfig config) {
    GameMode gameMode;
    
    if (playType == PlayType.LOCAL_SINGLE) {
        singleMode.setConfig(config);
        gameMode = singleMode;
    } else if (playType == PlayType.ONLINE_MULTI) {
        multiMode.setConfig(config);
        gameMode = multiMode;
    } else {
        throw new IllegalArgumentException("Unknown play type: " + playType);
    }
    
    return gameMode;
}

private void resetToDefaultMode() {
    try {
        GameModeConfig defaultConfig = GameModeConfig.classic(true);
        singleMode.setConfig(defaultConfig);
        boardController.setGameMode(singleMode);
        
        popupManager.hideModeSelectionPopup();
        startGame();
        
        System.out.println("✅ Reset to default mode (Classic + SRS)");
    } catch (Exception e) {
        System.err.println("❌ Critical: Failed to reset to default mode");
        // 메인 메뉴로 돌아가기
        handleModeSelectionCancel();
    }
}
```

---

## 3️⃣ 게임 시작 흐름 개선 ⭐ **필수**

### 문제점
- 모드를 선택하지 않고 ESC를 눌렀을 때 처리 불명확
- 게임 재시작 시 이전 모드 유지 여부 불분명

### 해결책 1: 기본 모드 자동 적용

**NavigationService 개선**:
```java
/**
 * 모드 선택과 함께 게임 화면으로 이동
 * 
 * @param showModeSelection true: 모드 선택 팝업 표시, false: 이전 설정 사용
 */
public void navigateToGameWithModeSelection(boolean showModeSelection) throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/game-view.fxml"));
    loader.setControllerFactory(applicationContext::getBean);
    Parent root = loader.load();
    
    GameController controller = loader.getController();
    
    // 게임 화면 표시
    Stage stage = getCurrentStage();
    stage.setScene(new Scene(root));
    stage.show();
    
    if (showModeSelection) {
        // 모드 선택 팝업 표시
        controller.showModeSelectionPopup();
    } else {
        // ⭐ 이전 설정으로 바로 시작
        controller.startWithPreviousMode();
    }
}
```

**GameController에 추가**:
```java
/**
 * ⭐ 이전 모드로 게임 시작 (팝업 없이)
 */
public void startWithPreviousMode() {
    try {
        // 마지막 설정 로드
        PlayType lastPlayType = gameModeProperties.getLastPlayType();
        GameplayType lastGameplayType = gameModeProperties.getLastGameplayType();
        boolean lastSrsEnabled = gameModeProperties.isLastSrsEnabled();
        
        System.out.println("🔄 Starting with previous mode:");
        System.out.println("  - Play Type: " + lastPlayType.getDisplayName());
        System.out.println("  - Gameplay Type: " + lastGameplayType.getDisplayName());
        System.out.println("  - SRS: " + (lastSrsEnabled ? "ON" : "OFF"));
        
        // 모드 초기화
        handleModeSelected(lastPlayType, lastGameplayType, lastSrsEnabled);
        
    } catch (Exception e) {
        System.err.println("❌ Failed to start with previous mode, showing mode selection");
        // 실패 시 모드 선택 팝업 표시
        showModeSelectionPopup();
    }
}
```

### 해결책 2: 재시작 시 옵션 제공

**게임 오버 팝업 수정** (over-pop.fxml):
```xml
<VBox styleClass="center-box">
    <Button text="Same Mode Restart" styleClass="menu-button-middle" 
            onAction="#handleRestartSameMode"/>
    <Button text="Change Mode" styleClass="menu-button-middle" 
            onAction="#handleRestartWithModeSelect"/>
    <Button text="Main Menu" styleClass="menu-button-middle" 
            onAction="#handleMain"/>
</VBox>
```

---

## 4️⃣ 로깅 체계화 🔸 **권장**

### 문제점
- `System.out.println` 산재
- 로그 레벨 구분 없음
- 프로덕션 환경에서 디버그 로그 제거 어려움

### 해결책: SLF4J + Logback 활용

**build.gradle.kts 의존성 추가**:
```kotlin
dependencies {
    // 로깅
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")
}
```

**GameController 수정**:
```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class GameController {
    
    private static final Logger log = LoggerFactory.getLogger(GameController.class);
    
    private void handleModeSelected(PlayType playType, GameplayType gameplayType, boolean srsEnabled) {
        log.info("🎮 Game mode selected: playType={}, gameplayType={}, srs={}", 
                 playType, gameplayType, srsEnabled);
        
        try {
            // ...
        } catch (Exception e) {
            log.error("Failed to initialize game mode", e);
            showErrorDialog("게임 모드 초기화 실패", e.getMessage());
        }
    }
}
```

**logback.xml 설정**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <logger name="seoultech.se" level="DEBUG"/>
    <logger name="org.springframework" level="INFO"/>
    
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>
</configuration>
```

---

## 5️⃣ 애니메이션 효과 🔸 **권장**

### 문제점
- 팝업 전환이 즉시 발생 (부드럽지 않음)
- 모드 선택 시 시각적 피드백 부족

### 해결책: JavaFX Transition 활용

**PopupManager에 애니메이션 추가**:
```java
import javafx.animation.FadeTransition;
import javafx.util.Duration;

public class PopupManager {
    
    /**
     * ⭐ 페이드 인 애니메이션과 함께 팝업 표시
     */
    public void showModeSelectionPopup(...) {
        if (modeSelectionPopup != null) {
            modeSelectionPopup.loadSelection(...);
            modeSelectionPopup.setCallback(callback);
            
            modeSelectionOverlay.setVisible(true);
            modeSelectionOverlay.setManaged(true);
            
            // ⭐ 페이드 인 효과
            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), modeSelectionOverlay);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
        }
    }
    
    /**
     * ⭐ 페이드 아웃 애니메이션과 함께 팝업 숨김
     */
    public void hideModeSelectionPopup() {
        if (modeSelectionOverlay != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), modeSelectionOverlay);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                modeSelectionOverlay.setVisible(false);
                modeSelectionOverlay.setManaged(false);
            });
            fadeOut.play();
        }
    }
}
```

**ModeSelectionPopup 버튼 호버 효과**:
```java
private void addHoverEffect(ToggleButton button) {
    button.setOnMouseEntered(e -> {
        if (!button.isSelected()) {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        }
    });
    
    button.setOnMouseExited(e -> {
        if (!button.isSelected()) {
            ScaleTransition scale = new ScaleTransition(Duration.millis(100), button);
            scale.setToX(1.0);
            scale.setToY(1.0);
            scale.play();
        }
    });
}
```

---

## 6️⃣ 키보드 단축키 🔸 **권장**

### 문제점
- 모드 선택 팝업에서 마우스만 사용 가능
- 키보드로 조작 불가능

### 해결책: 키보드 네비게이션 추가

**ModeSelectionPopup에 추가**:
```java
public ModeSelectionPopup() {
    // ... 기존 UI 구성
    
    // ⭐ 키보드 이벤트 처리
    this.setOnKeyPressed(event -> {
        switch (event.getCode()) {
            case DIGIT1:
                localSingleButton.setSelected(true);
                break;
            case DIGIT2:
                onlineMultiButton.setSelected(true);
                break;
            case C:
                classicButton.setSelected(true);
                break;
            case A:
                arcadeButton.setSelected(true);
                break;
            case S:
                srsCheckBox.setSelected(!srsCheckBox.isSelected());
                break;
            case ENTER:
                handleStart();
                break;
            case ESCAPE:
                handleCancel();
                break;
        }
    });
    
    // ⭐ 포커스 요청 (키보드 입력 받기 위해)
    this.setFocusTraversable(true);
    Platform.runLater(() -> this.requestFocus());
}
```

**UI에 힌트 표시**:
```java
localSingleButton.setText("로컬 싱글 (1)");
onlineMultiButton.setText("온라인 멀티 (2)");
classicButton.setText("클래식 (C)");
arcadeButton.setText("아케이드 (A)");
srsCheckBox.setText("SRS 활성화 (S)");
startButton.setText("게임 시작 (Enter)");
cancelButton.setText("취소 (ESC)");
```

---

## 7️⃣ 설정 프리셋 시스템 🔹 **선택**

### 제안
사용자가 자주 사용하는 모드 조합을 저장

**GameModePreset 클래스**:
```java
@Data
@Builder
public class GameModePreset {
    private String name;
    private PlayType playType;
    private GameplayType gameplayType;
    private boolean srsEnabled;
    
    public static List<GameModePreset> getDefaultPresets() {
        return List.of(
            GameModePreset.builder()
                .name("빠른 시작 (클래식)")
                .playType(PlayType.LOCAL_SINGLE)
                .gameplayType(GameplayType.CLASSIC)
                .srsEnabled(true)
                .build(),
            GameModePreset.builder()
                .name("도전 모드 (아케이드)")
                .playType(PlayType.LOCAL_SINGLE)
                .gameplayType(GameplayType.ARCADE)
                .srsEnabled(true)
                .build(),
            GameModePreset.builder()
                .name("클래식 (SRS 없음)")
                .playType(PlayType.LOCAL_SINGLE)
                .gameplayType(GameplayType.CLASSIC)
                .srsEnabled(false)
                .build()
        );
    }
}
```

**ModeSelectionPopup에 프리셋 버튼 추가**:
```java
private void addPresetButtons() {
    Label presetLabel = new Label("빠른 선택");
    presetLabel.getStyleClass().add("section-label");
    
    HBox presetBox = new HBox(10);
    presetBox.setAlignment(Pos.CENTER);
    
    for (GameModePreset preset : GameModePreset.getDefaultPresets()) {
        Button presetButton = new Button(preset.getName());
        presetButton.getStyleClass().add("preset-button");
        presetButton.setOnAction(e -> applyPreset(preset));
        presetBox.getChildren().add(presetButton);
    }
    
    this.getChildren().add(1, presetLabel);
    this.getChildren().add(2, presetBox);
}
```

---

## 8️⃣ 통계 및 분석 🔹 **선택**

### 제안
모드별 플레이 통계 수집

**GameModeStatistics 클래스**:
```java
@Entity
@Table(name = "game_mode_statistics")
public class GameModeStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private PlayType playType;
    
    @Enumerated(EnumType.STRING)
    private GameplayType gameplayType;
    
    private boolean srsEnabled;
    
    private int playCount;
    private long totalScore;
    private int highestScore;
    private LocalDateTime lastPlayed;
    
    // 통계 업데이트 메서드
    public void recordGame(int score) {
        this.playCount++;
        this.totalScore += score;
        this.highestScore = Math.max(this.highestScore, score);
        this.lastPlayed = LocalDateTime.now();
    }
}
```

**ModeSelectionPopup에 통계 표시**:
```java
private void showStatistics() {
    Label statsLabel = new Label(
        String.format("이 모드 플레이 횟수: %d회, 최고 점수: %d",
                     statistics.getPlayCount(), 
                     statistics.getHighestScore())
    );
    statsLabel.getStyleClass().add("stats-label");
}
```

---

## 9️⃣ 다국어 지원 준비 🔹 **선택**

### 제안
향후 다국어 지원을 위한 구조 준비

**messages.properties**:
```properties
# 모드 선택
mode.selection.title=게임 모드 선택
mode.selection.playtype=플레이 타입
mode.selection.gameplay=게임 모드
mode.selection.srs=SRS 활성화
mode.selection.srs.description=벽 킥을 사용하여 더 유연한 회전 가능

# PlayType
playtype.local.single=로컬 싱글
playtype.online.multi=온라인 멀티

# GameplayType
gameplay.classic=클래식
gameplay.arcade=아케이드
```

**ResourceBundle 활용**:
```java
private ResourceBundle messages = ResourceBundle.getBundle("messages");

titleLabel.setText(messages.getString("mode.selection.title"));
```

---

## ✅ 우선순위별 적용 가이드

### 🚀 Phase 1 구현 시 필수 적용
- ✅ **1. @ConfigurationProperties 도입**
- ✅ **2. 에러 처리 (검증 로직)**
- ✅ **3. 게임 시작 흐름 개선**

### 📅 Phase 3-4 구현 시 권장
- 🔸 **4. SLF4J 로깅**
- 🔸 **5. 페이드 애니메이션**
- 🔸 **6. 키보드 단축키**

### 🎁 배포 전 선택 적용
- 🔹 **7. 프리셋 시스템**
- 🔹 **8. 통계 기능**
- 🔹 **9. 다국어 준비**

---

## 📝 수정된 체크리스트

### Phase 1 (기존 + 개선)
- [ ] `GameModeConfig`에 `srsEnabled`, `gameplayType` 추가
- [ ] `GameplayType` enum 생성
- [ ] `PlayType` enum 생성
- [ ] `GameEngine.tryRotate()` SRS 토글 구현
- [ ] **⭐ `GameModeProperties` 클래스 생성** *(새로 추가)*
- [ ] **⭐ 설정 검증 로직 추가** *(새로 추가)*

### Phase 2 (기존 + 개선)
- [ ] `application.properties` 게임 모드 설정 추가
- [ ] **⭐ `SettingsService`에서 `GameModeProperties` 주입** *(변경)*
- [ ] `buildGameModeConfig()` 메서드 추가
- [ ] **⭐ 에러 처리 추가** *(새로 추가)*

### Phase 5 (기존 + 개선)
- [ ] `MainController` 수정
- [ ] `NavigationService` 확장
- [ ] `GameController` 통합
- [ ] `BoardController` SRS 설정 반영
- [ ] **⭐ 에러 복구 로직 추가** *(새로 추가)*
- [ ] **⭐ 로깅 적용 (선택)** *(새로 추가)*
- [ ] **⭐ 애니메이션 적용 (선택)** *(새로 추가)*
- [ ] 전체 테스트

---

**이 개선안들을 적용하시겠습니까?** 🚀
