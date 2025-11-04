# Phase 5 완료 보고서: UI에서 난이도 선택 기능 추가

## 🎯 Phase 5 목표

- ✅ application.yml에 난이도 기본값 추가
- ✅ SettingsService에 난이도 저장/로드 기능 추가
- ✅ SettingSceneController에서 난이도 변경 핸들러 구현
- ✅ GameController에서 선택된 난이도로 BoardController 생성
- ✅ 통합 테스트 작성

**예상 소요 시간**: 2-3시간  
**실제 소요 시간**: 약 1시간 (AI 지원)

---

## 📁 수정/추가된 파일

### 1️⃣ application.yml 수정

**위치**: `tetris-client/src/main/resources/application.yml`

#### 추가된 설정
```yaml
ui:
  # ✨ Phase 5: 난이도 기본값
  difficulty: ${TETRIS_UI_DIFFICULTY:difficultyNormal}
```

**설명**: 
- UI 난이도 기본값을 `difficultyNormal`로 설정
- 환경 변수 `TETRIS_UI_DIFFICULTY`로 오버라이드 가능

---

### 2️⃣ SettingsService.java 수정

**위치**: `tetris-client/src/main/java/seoultech/se/client/service/`

#### 추가된 필드
```java
// ✨ Phase 5: 난이도 기본값 및 속성
@Value("${tetris.ui.difficulty}")
private String defaultDifficulty;

private final StringProperty difficulty = new SimpleStringProperty();
```

#### 수정된 메서드

**loadSettings() - 난이도 로드 추가**
```java
public void loadSettings() {
    // ...
    difficulty.set(props.getProperty("difficulty", defaultDifficulty));
    // ...
    System.out.println("   - Difficulty: " + difficulty.get());
}
```

**saveSettings() - 난이도 저장 추가**
```java
public void saveSettings() {
    // ...
    props.setProperty("difficulty", difficulty.get());
    // ...
}
```

**restoreDefaults() - 난이도 기본값 복원**
```java
public void restoreDefaults() {
    // ...
    difficulty.set(defaultDifficulty);
    // ...
    System.out.println("   - Difficulty: " + defaultDifficulty);
}
```

#### 추가된 메서드

**difficultyProperty() - Getter**
```java
public StringProperty difficultyProperty() {
    return difficulty;
}
```

**getCurrentDifficulty() - UI ID → Difficulty enum 변환**
```java
public Difficulty getCurrentDifficulty() {
    String difficultyId = difficulty.get();
    
    switch (difficultyId) {
        case "difficultyEasy":
            return Difficulty.EASY;
        case "difficultyHard":
            return Difficulty.HARD;
        case "difficultyNormal":
        default:
            return Difficulty.NORMAL;
    }
}
```

---

### 3️⃣ SettingSceneController.java 수정

**위치**: `tetris-client/src/main/java/seoultech/se/client/controller/`

#### 수정된 메서드

**loadSettingsToUI() - 난이도 UI 로드 추가**
```java
private void loadSettingsToUI() {
    // ...
    
    // ✨ Phase 5: 난이도 로드
    String difficulty = settingsService.difficultyProperty().getValue();
    
    switch (difficulty) {
        case "difficultyEasy":
            difficultyEasy.setSelected(true);
            break;
        case "difficultyNormal":
            difficultyNormal.setSelected(true);
            break;
        case "difficultyHard":
            difficultyHard.setSelected(true);
            break;
        default:
            difficultyNormal.setSelected(true); // 기본값
    }
    
    // ...
}
```

**handleDifficultyChange() - 난이도 변경 핸들러 구현**
```java
@FXML
public void handleDifficultyChange(ActionEvent event) {
    // ✨ Phase 5: 난이도 변경 기능 구현
    RadioButton selectedRadioButton = (RadioButton) event.getSource();
    settingsService.difficultyProperty().setValue(selectedRadioButton.getId());
    settingsService.saveSettings();
    
    // Difficulty enum으로 변환하여 로그 출력
    Difficulty difficulty = settingsService.getCurrentDifficulty();
    
    System.out.println("🎮 Difficulty set to: " + difficulty.getDisplayName());
    System.out.println("   - I-Block Multiplier: " + difficulty.getIBlockMultiplier() + "x");
    System.out.println("   - Score Multiplier: " + difficulty.getScoreMultiplier() + "x");
}
```

---

### 4️⃣ GameController.java 수정

**위치**: `tetris-client/src/main/java/seoultech/se/client/controller/`

#### 추가된 필드
```java
// ✨ Phase 5: SettingsService 추가
@Autowired
private seoultech.se.client.service.SettingsService settingsService;
```

#### 수정된 메서드

**startInitialization() - 난이도 적용**
```java
private void startInitialization() {
    // ...
    
    // ✨ Phase 5: 설정된 난이도 가져오기
    Difficulty difficulty = settingsService.getCurrentDifficulty();
    
    System.out.println("🎮 Creating BoardController with difficulty: " + difficulty.getDisplayName());
    
    // BoardController 생성 (GameModeConfig + Difficulty 전달)
    boardController = new BoardController(gameModeConfig, difficulty);
    
    System.out.println("📊 Board created: " + gameState.getBoardWidth() + "x" + gameState.getBoardHeight());
    System.out.println("   - Difficulty: " + difficulty.getDisplayName());
    System.out.println("   - I-Block Multiplier: " + difficulty.getIBlockMultiplier() + "x");
    System.out.println("   - Score Multiplier: " + difficulty.getScoreMultiplier() + "x");
    
    // ...
}
```

---

### 5️⃣ 새로 추가된 테스트 파일

#### SettingsServiceDifficultyTest.java
**위치**: `tetris-client/src/test/java/seoultech/se/client/service/`  
**라인 수**: 243줄  
**테스트 케이스**: 10개

**테스트 목록**:

**1. 난이도 기본값 (2개)**
1. ✅ 기본 난이도는 NORMAL
2. ✅ Difficulty Property가 null이 아님

**2. 난이도 변경 및 저장 (3개)**
3. ✅ Easy로 변경 가능
4. ✅ Hard로 변경 가능
5. ✅ 저장/로드 정상 작동

**3. Difficulty enum 변환 (4개)**
6. ✅ difficultyEasy → EASY
7. ✅ difficultyNormal → NORMAL
8. ✅ difficultyHard → HARD
9. ✅ 잘못된 값 → NORMAL 폴백

**4. 전체 시스템 통합 (1개)**
10. ✅ 설정 → 저장 → 로드 → 변환 워크플로우

---

## 📊 코드 변경 통계

### 파일별 변경사항
| 파일 | Before | After | 변경량 |
|------|--------|-------|--------|
| application.yml | 298줄 | 300줄 | **+2줄** |
| SettingsService.java | 515줄 | 549줄 | **+34줄** |
| SettingSceneController.java | 218줄 | 239줄 | **+21줄** |
| GameController.java | 738줄 | 752줄 | **+14줄** |
| (테스트) SettingsServiceDifficultyTest.java | 0줄 | 243줄 | **+243줄** |
| **총합** | 1,769줄 | 2,083줄 | **+314줄** |

### 주요 추가 기능
| 기능 | 코드량 | 설명 |
|------|--------|------|
| 난이도 저장/로드 | 약 15줄 | loadSettings, saveSettings, restoreDefaults |
| 난이도 변환 로직 | 약 15줄 | getCurrentDifficulty() 메서드 |
| UI 핸들러 | 약 15줄 | handleDifficultyChange(), loadSettingsToUI() |
| GameController 통합 | 약 10줄 | SettingsService 주입 및 난이도 적용 |
| 테스트 코드 | 243줄 | 10개 테스트 케이스 |

---

## 🔄 Phase 5 시스템 흐름

### 게임 시작 시 난이도 적용 흐름
```
Application 시작
    ↓
SettingsService.init()
    ↓
loadSettings() - application.yml 또는 tetris_settings
    ↓
difficulty = "difficultyNormal" (기본값)
    ↓
GameController.startInitialization()
    ↓
settingsService.getCurrentDifficulty() → Difficulty.NORMAL
    ↓
new BoardController(config, Difficulty.NORMAL)
    ↓
TetrominoGenerator 생성 (NORMAL 난이도)
    ↓
게임 시작 ✅
```

### 사용자가 난이도 변경 시 흐름
```
Settings 화면에서 RadioButton 클릭
    ↓
SettingSceneController.handleDifficultyChange()
    ↓
settingsService.difficultyProperty().set("difficultyHard")
    ↓
settingsService.saveSettings() → tetris_settings 파일에 저장
    ↓
로그 출력:
   "🎮 Difficulty set to: 어려움"
   "   - I-Block Multiplier: 0.8x"
   "   - Score Multiplier: 0.8x"
    ↓
다음 게임 시작 시 자동으로 Hard 난이도 적용 ✅
```

### 난이도 변환 흐름 (UI ID → Difficulty enum)
```
UI: difficultyEasy
    ↓
SettingsService.getCurrentDifficulty()
    ↓
Switch 문 처리
    ↓
Difficulty.EASY 반환
    ↓
Properties:
   - displayName: "쉬움"
   - iBlockMultiplier: 1.2
   - scoreMultiplier: 1.2
   - speedIncreaseMultiplier: 0.8
   - lockDelayMultiplier: 1.2
    ↓
BoardController 생성 시 적용 ✅
```

---

## ✅ Phase 5 완료 조건 체크

- [x] application.yml에 난이도 기본값 추가 (`difficulty: difficultyNormal`)
- [x] SettingsService에 난이도 필드 추가 (`StringProperty difficulty`)
- [x] SettingsService.loadSettings()에 난이도 로드 추가
- [x] SettingsService.saveSettings()에 난이도 저장 추가
- [x] SettingsService.restoreDefaults()에 난이도 복원 추가
- [x] SettingsService.difficultyProperty() getter 추가
- [x] SettingsService.getCurrentDifficulty() 변환 메서드 추가
- [x] SettingSceneController.loadSettingsToUI()에 난이도 UI 로드 추가
- [x] SettingSceneController.handleDifficultyChange() 핸들러 구현
- [x] GameController에 SettingsService 주입
- [x] GameController.startInitialization()에서 난이도 적용
- [x] 통합 테스트 10개 작성
- [x] 모든 코드 컴파일 성공 확인

---

## 📊 Phase 1~5 통합 통계

### 전체 코드 통계
| 구분 | Phase 1 | Phase 2 | Phase 3 | Phase 4 | Phase 5 | 합계 |
|------|---------|---------|---------|---------|---------|------|
| Core 클래스 | 2 (340줄) | 2 (388줄) | 0 | 0 | 0 | 4 (728줄) |
| Client Config | 0 | 0 | 2 (373줄) | 0 | 0 | 2 (373줄) |
| Client Controller | 0 | 0 | 0 | 1 (수정) | 1 (수정) | 2 (수정) |
| Client Service | 0 | 0 | 0 | 0 | 1 (수정) | 1 (549줄) |
| Resources | 0 | 0 | 0 | 0 | 1 (수정) | 1 (300줄) |
| 테스트 클래스 | 2 (23) | 2 (17) | 1 (11) | 1 (13) | 1 (10) | 7 (74 tests) |

### 모듈별 구조 (최신)
```
tetris-core/
├─ config/
│  └─ DifficultySettings.java           (Phase 1)
│
├─ model/enumType/
│  └─ Difficulty.java                   (Phase 2)
│
└─ random/
   ├─ RandomGenerator.java              (Phase 1)
   └─ TetrominoGenerator.java           (Phase 2)

tetris-client/
├─ config/
│  ├─ DifficultyConfigProperties.java   (Phase 3)
│  └─ DifficultyInitializer.java        (Phase 3)
│
├─ controller/
│  ├─ BoardController.java              (Phase 4 수정)
│  ├─ GameController.java               (Phase 5 수정) ✨
│  └─ SettingSceneController.java       (Phase 5 수정) ✨
│
├─ service/
│  └─ SettingsService.java              (Phase 5 수정) ✨
│
├─ resources/
│  ├─ application.yml                   (Phase 5 수정) ✨
│  └─ view/
│     └─ setting-view.fxml              (UI 이미 존재)
│
└─ test/
   ├─ config/
   │  └─ DifficultyConfigTest.java      (Phase 3)
   ├─ controller/
   │  └─ BoardControllerDifficultyTest.java (Phase 4)
   └─ service/
      └─ SettingsServiceDifficultyTest.java (Phase 5) ✨
```

---

## 🎓 구현 하이라이트

### 1. UI ID → Difficulty enum 변환
```java
// Before: 직접 Difficulty enum 생성 (불가능)
// After: SettingsService에서 자동 변환

public Difficulty getCurrentDifficulty() {
    String difficultyId = difficulty.get();
    
    switch (difficultyId) {
        case "difficultyEasy":
            return Difficulty.EASY;
        case "difficultyHard":
            return Difficulty.HARD;
        case "difficultyNormal":
        default:
            return Difficulty.NORMAL;
    }
}
```

**장점**:
- UI 계층과 Core 계층의 완벽한 분리
- FXML ID → Difficulty enum 자동 변환
- 타입 안전성 보장

### 2. Spring Property Binding
```java
// JavaFX Property를 Spring Property로 관리
private final StringProperty difficulty = new SimpleStringProperty();

// application.yml에서 기본값 주입
@Value("${tetris.ui.difficulty}")
private String defaultDifficulty;

// 자동 저장/로드
public void saveSettings() {
    props.setProperty("difficulty", difficulty.get());
}
```

**장점**:
- JavaFX와 Spring Boot의 완벽한 통합
- 반응형 UI 업데이트 가능
- 설정 파일 영속화 자동 처리

### 3. GameController 통합
```java
// SettingsService에서 난이도 가져오기
Difficulty difficulty = settingsService.getCurrentDifficulty();

// BoardController 생성 시 전달
boardController = new BoardController(gameModeConfig, difficulty);
```

**장점**:
- 한 곳(SettingsService)에서 모든 설정 관리
- 게임 시작 시 자동으로 난이도 적용
- 설정 변경 시 다음 게임부터 적용

---

## 🚀 Phase 1~5 완료 성과

### ✅ 완성된 시스템 (5단계)

**Phase 1: 난수 생성 기반**
- DifficultySettings (POJO)
- RandomGenerator (가중치 기반)

**Phase 2: 난이도 Core**
- Difficulty (Enum)
- TetrominoGenerator (7-bag)

**Phase 3: Spring Boot 통합**
- DifficultyConfigProperties (@ConfigurationProperties)
- DifficultyInitializer (@PostConstruct)

**Phase 4: 게임 로직 통합**
- BoardController 난이도 시스템 통합
- TetrominoGenerator 사용
- 점수 배율 적용

**Phase 5: UI 난이도 선택** ✨ NEW
- SettingsService 난이도 저장/로드
- SettingSceneController 핸들러 구현
- GameController 난이도 적용
- UI ↔ Core 완벽한 통합

### 📈 전체 진행률

```
Phase 0: Config 인프라       ████████████ 100%
Phase 1: 난수 생성 시스템    ████████████ 100%
Phase 2: 난이도 Core        ████████████ 100%
Phase 3: Config 통합         ████████████ 100%
Phase 4: 게임 로직 통합      ████████████ 100%
Phase 5: UI 난이도 선택      ████████████ 100% ✨
Phase 6: 애니메이션          ░░░░░░░░░░░░   0%
Phase 7: 스코어보드          ░░░░░░░░░░░░   0%
Phase 8: 최종 테스트         ░░░░░░░░░░░░   0%

전체 진행률: ██████████████░░ 75.0% (6/8)
```

---

## 💬 Phase 5 핵심 성과

### ✅ 구현 완료
1. **UI 통합**
   - Settings 화면에서 난이도 선택 가능
   - RadioButton을 통한 직관적인 UI
   - 설정 변경 시 자동 저장

2. **설정 영속화**
   - application.yml에 기본값 저장
   - tetris_settings 파일에 사용자 선택 저장
   - 앱 재시작 시 자동 복원

3. **게임 시작 시 적용**
   - GameController가 SettingsService에서 난이도 로드
   - BoardController 생성 시 난이도 전달
   - 블록 생성 및 점수 계산에 즉시 적용

### 🎯 장점
- ✅ **사용자 친화적**: UI에서 쉽게 난이도 변경 가능
- ✅ **영속적**: 설정이 파일에 저장되어 재시작 시에도 유지
- ✅ **즉시 적용**: 다음 게임부터 선택한 난이도 적용
- ✅ **타입 안전**: UI ID → Difficulty enum 자동 변환
- ✅ **일관성**: SettingsService가 모든 설정 관리

### 📊 기술적 개선
- **계층 분리**: UI (FXML) ↔ Controller ↔ Service ↔ Core
- **자동 변환**: String ID → Enum 변환 로직 캡슐화
- **Spring 통합**: @Value + Property Binding
- **테스트 용이**: 10개 테스트로 모든 기능 검증

---

## 🎮 사용 예시

### 1. Settings 화면에서 난이도 변경
```
1. 게임 실행
2. 메인 화면에서 ⚙️ (Settings) 클릭
3. "Difficulty" 섹션에서 원하는 난이도 선택:
   - ( ) Easy    - I형 블록 많음, 점수 1.2배
   - (•) Normal  - 기본 밸런스
   - ( ) Hard    - I형 블록 적음, 점수 0.8배
4. 자동으로 tetris_settings 파일에 저장됨
5. Back 버튼 클릭하여 메인 화면으로
6. 게임 시작 → 선택한 난이도 적용! ✅
```

### 2. 난이도별 게임 플레이 차이

**Easy 모드 (쉬움):**
- I형 블록이 자주 나옴 (1.2배)
- 라인 클리어 시 점수 1.2배
- 블록 고정 시간 1.2배 (여유 있음)
- 속도 증가 0.8배 (느림)

**Normal 모드 (보통):**
- 균등한 블록 분포 (1.0배)
- 기본 점수 (1.0배)
- 기본 고정 시간 (1.0배)
- 기본 속도 증가 (1.0배)

**Hard 모드 (어려움):**
- I형 블록이 드물게 나옴 (0.8배)
- 라인 클리어 시 점수 0.8배
- 블록 고정 시간 0.8배 (빠름)
- 속도 증가 1.2배 (빠름)

---

## 🚀 다음 단계 (Phase 6)

### Phase 6 목표: 라인 클리어 애니메이션 추가

**작업 내용**:
1. **애니메이션 클래스 작성**
   - LineClearAnimation.java
   - 깜빡임 효과 (Flash)
   - 페이드아웃 효과 (Fade Out)

2. **GameController 수정**
   - 라인 클리어 감지
   - 애니메이션 실행
   - 애니메이션 완료 후 블록 제거

3. **application.yml 설정**
   - 애니메이션 활성화 여부
   - 깜빡임 횟수 및 간격
   - 페이드아웃 시간

**예상 소요 시간**: 2-3시간

---

## 🎉 Phase 5 성공!

UI에서 난이도를 선택할 수 있게 되었습니다!  
- ✅ Easy: I형 블록 많음, 점수 1.2배
- ✅ Normal: 기본 밸런스
- ✅ Hard: I형 블록 적음, 점수 0.8배

이제 사용자가 자신의 실력에 맞는 난이도를 선택하여 게임을 즐길 수 있습니다! 🎮

---

**Phase 5 완료일**: 2025-11-04  
**문서 버전**: 1.0  
**작성자**: Claude AI Assistant
