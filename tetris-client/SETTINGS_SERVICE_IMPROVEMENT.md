# SettingsService 권장 개선 적용 보고서

## 🎯 개선 목표

기존에 하드코딩되어 있던 기본값들을 `application.yml`에서 관리하도록 변경하여, 설정의 일관성과 유지보수성을 향상시킵니다.

---

## 📊 변경 전후 비교

### ❌ 개선 전 (하드코딩)

```java
// SettingsService.java
private final DoubleProperty soundVolume = new SimpleDoubleProperty(80);
private final StringProperty colorMode = new SimpleStringProperty("colorModeDefault");
private final StringProperty screenSize = new SimpleStringProperty("screenSizeM");

public void loadSettings() {
    soundVolume.set(Double.parseDouble(props.getProperty("soundVolume", "80")));
    colorMode.set(props.getProperty("colorMode", "colorModeDefault"));
    // ...
}

public void restoreDefaults() {
    soundVolume.set(80);
    colorMode.set("colorModeDefault");
    screenSize.set("screenSizeM");
    applyResolution(500, 700);
}
```

**문제점:**
- 기본값이 코드에 하드코딩됨
- 기본값 변경 시 코드 수정 및 재컴파일 필요
- application.yml과 코드 간 일관성 유지 어려움

### ✅ 개선 후 (application.yml 주입)

```java
// SettingsService.java
@Value("${tetris.sound.volume}")
private double defaultSoundVolume;

@Value("${tetris.ui.color-mode}")
private String defaultColorMode;

@Value("${tetris.ui.screen-size}")
private String defaultScreenSize;

@Value("${tetris.ui.stage-width}")
private double defaultStageWidth;

@Value("${tetris.ui.stage-height}")
private double defaultStageHeight;

public void loadSettings() {
    soundVolume.set(Double.parseDouble(
        props.getProperty("soundVolume", String.valueOf(defaultSoundVolume))));
    colorMode.set(props.getProperty("colorMode", defaultColorMode));
    // ...
    System.out.println("✅ Settings loaded from tetris_settings.");
    System.out.println("   - Sound Volume: " + soundVolume.get() + 
                       " (default: " + defaultSoundVolume + ")");
}

public void restoreDefaults() {
    soundVolume.set(defaultSoundVolume);
    colorMode.set(defaultColorMode);
    screenSize.set(defaultScreenSize);
    applyResolution(defaultStageWidth, defaultStageHeight);
    
    System.out.println("✅ Settings restored to defaults from application.yml.");
}
```

**장점:**
- 기본값이 `application.yml`에서 중앙 관리됨
- 환경 변수로 오버라이드 가능
- 프로파일별 기본값 설정 가능
- 코드 재컴파일 없이 기본값 변경 가능

---

## 🔧 변경 세부사항

### 1. application.yml 수정

#### UI 설정 추가

```yaml
tetris:
  sound:
    enabled: true
    # 사운드 볼륨 (0 ~ 100, SettingsService 스케일)
    volume: ${TETRIS_SOUND_VOLUME:80}
  
  ui:
    theme: dark
    # 색상 모드 (colorModeDefault, rg_blind, yb_blind)
    color-mode: ${TETRIS_UI_COLOR_MODE:colorModeDefault}
    # 화면 크기 (screenSizeXS, screenSizeS, screenSizeM, screenSizeL, screenSizeXL)
    screen-size: ${TETRIS_UI_SCREEN_SIZE:screenSizeM}
    # 스테이지 기본 너비
    stage-width: ${TETRIS_UI_STAGE_WIDTH:500}
    # 스테이지 기본 높이
    stage-height: ${TETRIS_UI_STAGE_HEIGHT:700}
```

#### 환경 변수 지원

모든 설정은 환경 변수로 오버라이드 가능:
```bash
export TETRIS_SOUND_VOLUME=50
export TETRIS_UI_COLOR_MODE=rg_blind
export TETRIS_UI_SCREEN_SIZE=screenSizeL
```

### 2. SettingsService.java 수정

#### @Value 어노테이션 추가

```java
@Value("${tetris.sound.volume}")
private double defaultSoundVolume;

@Value("${tetris.ui.color-mode}")
private String defaultColorMode;

@Value("${tetris.ui.screen-size}")
private String defaultScreenSize;

@Value("${tetris.ui.stage-width}")
private double defaultStageWidth;

@Value("${tetris.ui.stage-height}")
private double defaultStageHeight;
```

#### loadSettings() 개선

```java
public void loadSettings() {
    Properties props = new Properties();
    try (FileInputStream in = new FileInputStream(new File(SETTINGS_FILE))) {
        props.load(in);
        
        // tetris_settings 파일에서 값을 읽되, 없으면 application.yml 기본값 사용
        soundVolume.set(Double.parseDouble(
            props.getProperty("soundVolume", String.valueOf(defaultSoundVolume))));
        colorMode.set(props.getProperty("colorMode", defaultColorMode));
        screenSize.set(props.getProperty("screenSize", defaultScreenSize));
        
        // ... 로깅 추가
        System.out.println("✅ Settings loaded successfully from tetris_settings.");
        System.out.println("   - Sound Volume: " + soundVolume.get() + 
                           " (default: " + defaultSoundVolume + ")");
    } catch (Exception e) {
        System.out.println("❗ Failed to load settings, using defaults from application.yml.");
        restoreDefaults();
    }
}
```

#### restoreDefaults() 개선

```java
public void restoreDefaults() {
    // application.yml의 기본값 사용
    soundVolume.set(defaultSoundVolume);
    colorMode.set(defaultColorMode);
    screenSize.set(defaultScreenSize);
    applyResolution(defaultStageWidth, defaultStageHeight);
    saveSettings();
    
    System.out.println("✅ Settings restored to defaults from application.yml.");
    System.out.println("   - Sound Volume: " + defaultSoundVolume);
    System.out.println("   - Color Mode: " + defaultColorMode);
    System.out.println("   - Screen Size: " + defaultScreenSize);
    System.out.println("   - Stage Size: " + defaultStageWidth + "x" + defaultStageHeight);
}
```

---

## 🔄 설정 우선순위 (최종)

```
높음 ↑  1. tetris_settings 파일 (사용자 런타임 설정)
        2. 환경 변수 (${TETRIS_SOUND_VOLUME:80})
        3. application.yml (개발자 기본값)
낮음 ↓
```

### 동작 흐름

```
1. 애플리케이션 시작
   ↓
2. @Value로 application.yml 기본값 주입
   ↓
3. @PostConstruct init() 호출
   ↓
4. loadSettings() 실행
   ↓
5. tetris_settings 파일 읽기 시도
   ├─ 성공: 파일 값 사용
   │  └─ 파일에 키 없음: yml 기본값 사용 ✨
   └─ 실패: restoreDefaults() 호출
      └─ yml 기본값으로 초기화 ✨
```

---

## 📝 사용 예시

### 예시 1: 사용자가 처음 게임 실행

```
1. tetris_settings 파일 없음
2. loadSettings() → Exception 발생
3. restoreDefaults() 호출
4. application.yml의 기본값으로 설정:
   - soundVolume: 80
   - colorMode: colorModeDefault
   - screenSize: screenSizeM
5. tetris_settings 파일 생성 (saveSettings())
```

### 예시 2: 사용자가 일부 설정만 변경

```
tetris_settings 내용:
soundVolume=50
screenSize=screenSizeL
# colorMode는 저장 안 됨

loadSettings() 실행 시:
- soundVolume: 50 (파일에서 읽음)
- screenSize: screenSizeL (파일에서 읽음)
- colorMode: colorModeDefault (yml 기본값 사용) ✨
```

### 예시 3: 개발 환경에서 다른 기본값 사용

```yaml
# application.yml - dev profile
spring:
  config:
    activate:
      on-profile: dev

tetris:
  sound:
    volume: 100  # 개발 환경에서는 최대 볼륨
  ui:
    screen-size: screenSizeXL  # 개발 환경에서는 큰 화면
```

```bash
# 개발 프로파일로 실행
./gradlew :tetris-client:bootRun --args='--spring.profiles.active=dev'
```

---

## ✅ 개선 효과

### 1. 유지보수성 향상
- 기본값이 한 곳(application.yml)에서 관리됨
- 코드 수정 없이 기본값 변경 가능

### 2. 환경별 설정 지원
```yaml
---
# 개발 환경
spring.config.activate.on-profile: dev
tetris.ui.screen-size: screenSizeXL

---
# 테스트 환경
spring.config.activate.on-profile: test
tetris.sound.volume: 0  # 테스트 시 무음
```

### 3. 환경 변수 지원
```bash
# CI/CD 파이프라인에서
export TETRIS_UI_SCREEN_SIZE=screenSizeS
export TETRIS_SOUND_VOLUME=0
./gradlew test
```

### 4. 로깅 개선
```
✅ Settings loaded successfully from tetris_settings.
   - Sound Volume: 50.0 (default: 80.0)
   - Color Mode: rg_blind (default: colorModeDefault)
   - Screen Size: screenSizeL (default: screenSizeM)
```

---

## 🔍 테스트 시나리오

### 시나리오 1: 첫 실행
```bash
# tetris_settings 파일 없음
./gradlew :tetris-client:bootRun

# 예상 로그:
# ❗ Failed to load settings, using defaults from application.yml.
# ✅ Settings restored to defaults from application.yml.
#    - Sound Volume: 80.0
#    - Color Mode: colorModeDefault
#    - Screen Size: screenSizeM
```

### 시나리오 2: 설정 변경 후 재시작
```bash
# 1. 게임 실행 → 설정 변경 → 종료
# 2. 재시작

# 예상 로그:
# ✅ Settings loaded successfully from tetris_settings.
#    - Sound Volume: 50.0 (default: 80.0)
#    - Color Mode: colorModeDefault (default: colorModeDefault)
#    - Screen Size: screenSizeL (default: screenSizeM)
```

### 시나리오 3: 환경 변수 오버라이드
```bash
export TETRIS_SOUND_VOLUME=100
./gradlew :tetris-client:bootRun

# 예상 로그:
# ✅ Settings restored to defaults from application.yml.
#    - Sound Volume: 100.0  # 환경 변수 적용됨
```

---

## 📦 변경된 파일

### 수정된 파일 (2개)
1. **application.yml** (tetris-client/src/main/resources/)
   - UI 설정 추가 (color-mode, screen-size, stage-width, stage-height)
   - sound.volume 스케일 변경 (0.5 → 80)

2. **SettingsService.java** (tetris-client/src/main/java/seoultech/se/client/service/)
   - @Value 어노테이션 추가 (5개 필드)
   - loadSettings() 개선 (yml 기본값 사용)
   - restoreDefaults() 개선 (yml 기본값 사용)
   - 로깅 개선

### 유지된 파일
- **tetris_settings** (tetris-client/)
  - Legacy 호환성 유지
  - 사용자 런타임 설정 저장

---

## 🚀 다음 단계

### 1. Java 버전 전환 (필수)
```bash
sdk use java 21.0.5-tem
```

### 2. 빌드 검증
```bash
./gradlew clean build -x test
```

### 3. 동작 확인
```bash
./gradlew :tetris-client:bootRun

# 로그 확인:
# - @Value 주입 성공 확인
# - loadSettings() 로그 확인
# - 기본값이 yml에서 주입되었는지 확인
```

### 4. Git 커밋
```bash
git add .
git commit -m "[Phase-0] Apply recommended improvements to SettingsService

- Add @Value injection for default values from application.yml
- Improve loadSettings() to use yml defaults as fallback
- Improve restoreDefaults() to use yml defaults
- Add UI settings to application.yml (color-mode, screen-size, stage dimensions)
- Add detailed logging for settings loading
- Keep tetris_settings for backward compatibility"
```

---

## 💡 추가 개선 아이디어 (선택)

### 1. 난이도 설정도 tetris_settings에 저장
```java
// SettingsService.java
private final StringProperty difficulty = new SimpleStringProperty();

@Value("${tetris.difficulty.default:NORMAL}")
private String defaultDifficulty;

public void loadSettings() {
    // ...
    difficulty.set(props.getProperty("difficulty", defaultDifficulty));
}
```

### 2. 설정 검증 로직 추가
```java
@Value("${tetris.sound.volume}")
private double defaultSoundVolume;

public void loadSettings() {
    double volume = Double.parseDouble(
        props.getProperty("soundVolume", String.valueOf(defaultSoundVolume)));
    
    // 검증: 0~100 범위
    if (volume < 0 || volume > 100) {
        System.err.println("⚠️ Invalid sound volume: " + volume + 
                           ", using default: " + defaultSoundVolume);
        volume = defaultSoundVolume;
    }
    
    soundVolume.set(volume);
}
```

---

**개선 완료일**: Phase 0 (2025-11-03)
**개선 유형**: 설정 관리 개선
**문서 버전**: 1.0
