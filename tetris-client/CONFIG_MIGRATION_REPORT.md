# 설정 파일 통합 완료 보고서

## 📋 통합 전 상황

### 기존 설정 파일 (3개)
1. **tetris_settings** (tetris-client/)
   - 런타임 사용자 설정 (색상, 화면 크기, 볼륨 등)
   - Properties 형식

2. **application.properties** (tetris-client/)
   - Spring Boot 정적 설정
   - 잘못된 위치 (src/main/resources에 있어야 함)

3. **application.yml** (src/main/resources/)
   - 새로운 난이도 시스템 설정

## ✅ 통합 후 구조 (2-Tier)

### 1️⃣ 정적 설정: application.yml
**위치**: `tetris-client/src/main/resources/application.yml`
**용도**: 개발자가 정의하는 애플리케이션 기본값

```yaml
tetris:
  difficulty:           # ✨ 난이도 시스템 (NEW)
  bag-system:          # ✨ 7-bag 시스템 (NEW)
  animation:           # ✨ 애니메이션 설정 (NEW)
  game:                # 게임 기본 설정
  sound:               # 사운드 설정
  ui:                  # UI 설정
  score:               # 점수 설정
  save:                # 저장 설정
  mode:                # 게임 모드 설정
```

### 2️⃣ 동적 설정: tetris_settings
**위치**: `tetris-client/tetris_settings`
**용도**: 사용자가 게임 플레이하면서 변경하는 런타임 값
**Legacy**: 기존 SettingsService 로직 그대로 유지

```properties
colorMode=colorModeDefault
custom.classic.*=...
screenSize=screenSizeM
soundVolume=80.0
stageHeight=700.0
stageWidth=500.0
```

## 🔄 통합 세부사항

### ✅ 완료된 작업

1. **application.properties 내용 → application.yml 통합**
   - 모든 설정을 YAML 형식으로 변환
   - 구조화된 계층으로 재구성
   - 환경 변수 지원 유지 (${VAR:default})

2. **난이도 시스템 설정 추가**
   - difficulty (Easy/Normal/Hard)
   - bag-system (7-bag 알고리즘)
   - animation (라인 클리어 애니메이션)

3. **프로파일 구성**
   - default: 프로덕션 설정
   - dev: 개발 환경 (극단적 값)
   - test: 테스트 환경 (애니메이션 비활성화)

4. **파일 정리**
   - application.properties → application.properties.backup (백업)
   - 올바른 위치에 yml 배치 (src/main/resources)

### 🔧 Legacy 호환성

#### tetris_settings 사용 패턴 (유지)
```java
// SettingsService.java
// 기존 로직 그대로 유지
public void loadSettings() {
    Properties props = new Properties();
    // tetris_settings 파일 읽기
    ...
}

public void saveSettings() {
    Properties props = new Properties();
    // tetris_settings 파일 쓰기
    ...
}
```

#### 권장 개선 사항 (선택)
```java
// 초기값은 application.yml에서 가져오기
@Value("${tetris.sound.volume}")
private double defaultSoundVolume;

@Value("${tetris.ui.theme}")
private String defaultTheme;

public void loadSettings() {
    Properties props = new Properties();
    // tetris_settings 파일 읽기
    
    // 파일에 값이 없으면 yml 기본값 사용
    String volume = props.getProperty("soundVolume", 
                     String.valueOf(defaultSoundVolume));
}
```

## 📊 설정 우선순위

```
1. 환경 변수 (${TETRIS_GAME_INITIAL_LEVEL:1})
   ↓
2. application.yml (프로파일별)
   ↓
3. tetris_settings (사용자 런타임 설정)
```

## 🎯 설정 사용 가이드

### 개발자가 변경하는 설정
→ **application.yml** 수정

예시:
- 난이도 밸런스 조정
- 기본 점수 체계 변경
- 아이템 드롭 확률 변경

### 사용자가 변경하는 설정
→ **tetris_settings** (게임 UI에서 자동 저장)

예시:
- 색상 모드
- 화면 크기
- 사운드 볼륨
- 커스텀 게임 모드 설정

## 📁 파일 구조

```
tetris-client/
├─ tetris_settings                    # 사용자 런타임 설정
├─ application.properties.backup      # 백업 (삭제 가능)
└─ src/main/resources/
   └─ application.yml                 # ✅ 통합된 정적 설정
```

## ✅ 검증 체크리스트

- [x] application.yml 생성 완료
- [x] application.properties → yml 통합
- [x] 난이도 시스템 설정 추가
- [x] 프로파일 구성 (dev/test)
- [x] tetris_settings legacy 유지
- [x] 올바른 위치에 파일 배치
- [x] 기존 properties 백업

## 🚀 다음 단계

1. **Java 21로 전환** (현재 Java 25 이슈)
2. **빌드 테스트**
   ```bash
   ./gradlew clean build -x test
   ```
3. **설정 로딩 확인**
   ```bash
   ./gradlew :tetris-client:bootRun
   # 로그에서 설정 로딩 확인
   ```

## 📝 주의사항

### ⚠️ 중요: tetris_settings 파일 삭제 금지
- 사용자 설정이 저장되어 있음
- SettingsService가 이 파일을 사용
- 삭제 시 사용자 설정 초기화됨

### ⚠️ application.properties.backup
- 필요시 참고용으로 보관
- 통합 완료 후 삭제 가능

---

**통합 완료일**: Phase 0 (2025-11-03)
**담당**: Claude AI Assistant
**문서 버전**: 1.0
