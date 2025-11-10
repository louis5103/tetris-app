# 아이템 시스템 버그 수정

## 🐛 문제 상황

아케이드 모드에서 게임을 시작하고 끝날 때까지 **아이템이 한 번도 드롭되지 않는** 문제가 발생했습니다.

## 🔍 원인 분석

### 1. 증상
- ARCADE 모드 선택 후 게임 시작
- 라인 클리어 시 아이템이 전혀 드롭되지 않음
- `tryDropItem()` 메서드는 정상적으로 호출되고 있음

### 2. 근본 원인 발견

**`SettingsService.loadCustomGameModeConfig(GameplayType.ARCADE)`** 메서드가 ARCADE 모드 설정을 로드할 때 **`itemConfig`를 포함하지 않는** 문제였습니다.

#### 문제가 된 코드:
```java
// SettingsService.loadCustomGameModeConfig() - 수정 전
GameModeConfig config = GameModeConfig.builder()
    .gameplayType(gameplayType)
    .srsEnabled(...)
    .hardDropEnabled(...)
    // ... 기타 설정들
    .build(); // ❌ itemConfig가 없음!
```

#### 실행 흐름:
1. `MainController.handleArcadeModeAction()` 호출
2. `settingsService.loadCustomGameModeConfig(GameplayType.ARCADE)` 호출
3. **커스텀 설정이 존재하면** → `itemConfig` 없는 설정 반환
4. **커스텀 설정이 없으면** → `GameModeConfig.arcade()` 사용 (itemConfig 포함) ✅
5. `BoardController` 생성 시 설정 전달
6. `GameEngine.initialize(config)` 호출
7. **`config.getItemConfig() == null`** → itemManager 초기화 안됨 ❌
8. `tryDropItem()` 호출 시 `itemManager == null` → 아이템 드롭 안됨 ❌

#### 조건문 체크:
```java
// GameEngine.initialize()
if (config != null && 
    config.getItemConfig() != null &&  // ❌ 여기서 false!
    config.getItemConfig().isEnabled()) {
    // 아이템 시스템 초기화
}
```

## ✅ 해결 방법

### 1. `buildItemConfig()` 메서드 추가

`buildArcadeConfig()`에서 중복되던 아이템 설정 로직을 별도 메서드로 분리:

```java
/**
 * ItemConfig 생성
 * GameModeProperties 설정을 기반으로 ItemConfig를 빌드합니다.
 */
private seoultech.se.core.item.ItemConfig buildItemConfig() {
    // 활성화된 아이템 타입 수집
    java.util.Set<seoultech.se.core.item.ItemType> enabledItems = 
        new java.util.HashSet<>();
    
    for (seoultech.se.core.item.ItemType itemType : 
         seoultech.se.core.item.ItemType.values()) {
        if (gameModeProperties.isItemEnabled(itemType.name())) {
            enabledItems.add(itemType);
        }
    }
    
    System.out.println("📊 Item drop rate: " + (int)(gameModeProperties.getItemDropRate() * 100) + "%");
    System.out.println("📊 Enabled items: " + enabledItems);
    
    return seoultech.se.core.item.ItemConfig.builder()
        .dropRate(gameModeProperties.getItemDropRate())
        .enabledItems(enabledItems)
        .maxInventorySize(gameModeProperties.getMaxInventorySize())
        .autoUse(gameModeProperties.isItemAutoUse())
        .build();
}
```

### 2. `loadCustomGameModeConfig()` 수정

ARCADE 모드 로드 시 `itemConfig` 자동 추가:

```java
// GameModeConfig 빌더 시작
GameModeConfig.GameModeConfigBuilder builder = GameModeConfig.builder()
    .gameplayType(gameplayType)
    .srsEnabled(...)
    .hardDropEnabled(...)
    // ... 기타 설정들
    .lockDelay(...);

// ✅ ARCADE 모드인 경우 아이템 설정 추가
if (gameplayType == GameplayType.ARCADE) {
    builder.itemConfig(buildItemConfig());
    System.out.println("   - itemConfig added for ARCADE mode");
}

GameModeConfig config = builder.build();
```

### 3. `buildArcadeConfig()` 리팩토링

중복 제거:

```java
private GameModeConfig buildArcadeConfig(boolean srsEnabled) {
    System.out.println("🎮 [SettingsService] Building ARCADE config...");
    
    // ItemConfig 생성 (재사용)
    seoultech.se.core.item.ItemConfig itemConfig = buildItemConfig();
    
    System.out.println("✅ ItemConfig created - isEnabled: " + itemConfig.isEnabled());
    
    return GameModeConfig.builder()
        .gameplayType(GameplayType.ARCADE)
        .dropSpeedMultiplier(1.5)
        .lockDelay(300)
        .srsEnabled(srsEnabled)
        .itemConfig(itemConfig)
        .build();
}
```

## 🧪 테스트 방법

### 1. 디버깅용 100% 드롭률 설정

빠른 테스트를 위해 일시적으로 드롭 확률을 올립니다:

```properties
# application.properties
tetris.mode.item-drop-rate=${ITEM_DROP_RATE:1.0}  # 100%
```

### 2. 게임 실행 및 로그 확인

```bash
./gradlew :tetris-client:bootRun
```

콘솔에서 다음 로그 확인:
```
🕹️ ARCADE mode selected
🎮 [SettingsService] Building ARCADE config...
📊 Item drop rate: 100%
📊 Enabled items: [BOMB, PLUS, SPEED_RESET, BONUS_SCORE]
✅ ItemConfig created - isEnabled: true
   - itemConfig added for ARCADE mode
✅ [GameEngine] Item system initialized - Drop rate: 100%
✅ ItemManager initialized - Drop Rate: 100%
```

### 3. 아이템 드롭 확인

- 라인을 1개 클리어하면 **즉시 아이템 획득**
- 우측 인벤토리 UI에 아이템 표시
- 콘솔에 `🎁 [GameEngine] Item dropped: BOMB` 등의 로그

### 4. 정상 설정으로 복구

```properties
# application.properties
tetris.mode.item-drop-rate=${ITEM_DROP_RATE:0.1}  # 10% (기본값)
```

## 📊 수정된 파일

### `tetris-client/src/main/java/seoultech/se/client/service/SettingsService.java`

1. ✅ `buildItemConfig()` 메서드 추가
2. ✅ `buildArcadeConfig()` 메서드 리팩토링 (buildItemConfig() 재사용)
3. ✅ `loadCustomGameModeConfig()` 메서드 수정 (ARCADE 모드 시 itemConfig 자동 추가)

### `tetris-client/application.properties`

- 아이템 드롭 확률 설정 확인: `tetris.mode.item-drop-rate=0.1` (10%)

## 🎯 결과

- ✅ ARCADE 모드 선택 시 **항상** `itemConfig` 포함
- ✅ `GameEngine` 초기화 시 `itemManager` 정상 생성
- ✅ 라인 클리어 시 설정된 확률(10%)로 아이템 드롭
- ✅ 커스텀 설정 저장 후에도 정상 작동
- ✅ 기본 프리셋 사용 시에도 정상 작동

## 🔑 핵심 교훈

1. **설정 로드 시 모든 필수 데이터를 포함해야 함**
   - ARCADE 모드는 `itemConfig`가 필수
   - 로드 로직에서 빠뜨리면 시스템이 작동하지 않음

2. **코드 중복 제거로 일관성 유지**
   - `buildItemConfig()` 분리로 중복 제거
   - 한 곳에서만 수정하면 모든 곳에 적용

3. **디버깅 로그의 중요성**
   - 각 단계별 로그로 문제 위치 빠르게 파악
   - 설정 로드, 초기화, 드롭 체크 모두 로그 출력

4. **테스트 전략**
   - 100% 드롭률로 빠른 검증
   - 정상 설정으로 최종 확인

## 🚀 후속 작업

- [ ] 아이템 드롭 확률 밸런스 조정 (현재 10%)
- [ ] 아이템 획득 시 효과음 추가
- [ ] 아이템 드롭 애니메이션 추가
- [ ] 아이템 사용 튜토리얼 추가
