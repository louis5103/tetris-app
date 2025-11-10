# 🎮 아케이드 모드 아이템 시스템 설계 문서

## 📋 목차
1. [개요](#개요)
2. [시스템 아키텍처](#시스템-아키텍처)
3. [아이템 종류](#아이템-종류)
4. [설계 원칙](#설계-원칙)
5. [구현 세부사항](#구현-세부사항)
6. [확장 가이드](#확장-가이드)
7. [통합 가이드](#통합-가이드)

---

## 개요

### 목적
테트리스 아케이드 모드에 **확장 가능한 아이템 시스템**을 구현하여 게임플레이에 다양성과 전략성을 부여합니다.

### 핵심 요구사항
- ✅ 라인 클리어 시 10% 확률로 아이템 드롭
- ✅ 4가지 기본 아이템 (Bomb, Plus, Speed Reset, Bonus Score)
- ✅ 사용자 설정에서 아이템별 활성화/비활성화 가능
- ✅ 새로운 아이템 추가가 용이한 확장 가능한 구조

### 기술 스택
- **언어**: Java 17
- **프레임워크**: Spring Boot, JavaFX
- **패턴**: Strategy Pattern, Factory Pattern, Builder Pattern
- **모듈**: tetris-core (비즈니스 로직), tetris-client (UI 및 설정)

---

## 시스템 아키텍처

### 전체 구조도

```
┌─────────────────────────────────────────────────────────────┐
│                     tetris-client                           │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  GameModeProperties (Spring Configuration)           │  │
│  │  - itemDropRate: 0.1                                 │  │
│  │  - itemEnabled: Map<String, Boolean>                 │  │
│  │  - maxInventorySize: 3                               │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  SettingsService                                      │  │
│  │  - buildArcadeConfig()                               │  │
│  │  - 아이템 설정을 ItemConfig로 변환                    │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                      tetris-core                            │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  GameModeConfig                                       │  │
│  │  - itemConfig: ItemConfig                            │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  ItemConfig (불변 객체)                               │  │
│  │  - dropRate: double                                   │  │
│  │  - enabledItems: Set<ItemType>                       │  │
│  │  - maxInventorySize: int                             │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  ItemManager (Factory & Manager)                     │  │
│  │  - registerItem(Item)                                │  │
│  │  - generateRandomItem(): Item                        │  │
│  │  - useItem(Item, GameState, row, col): ItemEffect   │  │
│  └───────────────────────────────────────────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Item (Interface) ← Strategy Pattern                 │  │
│  │  - apply(GameState, row, col): ItemEffect           │  │
│  └───────────────────────────────────────────────────────┘  │
│           ▲         ▲         ▲          ▲                  │
│      ┌────┴────┬────┴────┬────┴────┬────┴────┐             │
│      │BombItem │PlusItem │SpeedRst │BonusScr │             │
│      └─────────┴─────────┴─────────┴─────────┘             │
└─────────────────────────────────────────────────────────────┘
```

### 모듈별 책임

#### tetris-core (비즈니스 로직)
- 아이템 인터페이스 및 구현
- 아이템 효과 로직
- 아이템 관리 및 생성

#### tetris-client (UI & 설정)
- 사용자 설정 관리
- 아이템 활성화/비활성화
- 설정을 Core 모듈로 전달

---

## 아이템 종류

### 1. 💣 Bomb (폭탄)
**효과**: 5x5 영역 제거

```java
public class BombItem extends AbstractItem {
    - 중심점 기준 반경 2칸 (상하좌우 각 2칸)
    - 제거된 블록 수 × 5점
    - 사용 예: 긴급 상황에서 공간 확보
}
```

**파라미터**:
- `row`, `col`: 아이템 중심 좌표
- **반환**: `ItemEffect` (blocksCleared, bonusScore)

---

### 2. ➕ Plus (십자)
**효과**: 행과 열 전체 제거

```java
public class PlusItem extends AbstractItem {
    - 지정된 행의 모든 블록 제거
    - 지정된 열의 모든 블록 제거
    - 교차점은 중복 계산 안 함
    - 제거된 블록 수 × 5점
}
```

**파라미터**:
- `row`: 제거할 행
- `col`: 제거할 열

---

### 3. ⚡ Speed Reset (속도 초기화)
**효과**: 소프트 드롭 속도 초기화

```java
public class SpeedResetItem extends AbstractItem {
    - 누적된 소프트 드롭 속도를 초기 값으로 복원
    - 고레벨에서 유용
    - 보너스 점수: 100점
}
```

**참고**: GameEngine과의 연동 필요 (향후 구현)

---

### 4. ⭐ Bonus Score (보너스 점수)
**효과**: 즉시 점수 획득

```java
public class BonusScoreItem extends AbstractItem {
    - 기본 점수: 500점
    - 레벨 보너스: 현재 레벨 × 50점
    - 총 점수 = 500 + (level × 50)
}
```

**예시**:
- 레벨 1: 550점
- 레벨 5: 750점
- 레벨 10: 1000점

---

## 설계 원칙

### 1. SOLID 원칙 적용

#### 단일 책임 원칙 (SRP)
- `Item` 인터페이스: 아이템 효과만 정의
- `ItemManager`: 아이템 생성 및 관리
- `ItemConfig`: 설정 관리

#### 개방-폐쇄 원칙 (OCP)
- 새 아이템 추가 시 기존 코드 수정 불필요
- `ItemType` enum에 값 추가
- 새 클래스 생성 (AbstractItem 상속)

#### 의존성 역전 원칙 (DIP)
- `Item` 인터페이스에 의존
- 구체적인 구현체는 런타임에 결정

### 2. 디자인 패턴

#### Strategy Pattern
```java
public interface Item {
    ItemEffect apply(GameState gameState, int row, int col);
}
```
- 각 아이템의 효과를 독립적인 전략으로 캡슐화
- 런타임에 아이템 교체 가능

#### Factory Pattern
```java
public class ItemManager {
    private Map<ItemType, Item> itemPrototypes;
    
    public Item generateRandomItem() {
        // 활성화된 아이템 중 무작위 선택
    }
}
```

#### Builder Pattern
```java
ItemConfig config = ItemConfig.builder()
    .dropRate(0.1)
    .enabledItems(EnumSet.of(BOMB, PLUS))
    .build();
```

### 3. 불변성 (Immutability)
- `ItemConfig`: 설정 객체는 생성 후 수정 불가
- `ItemEffect`: 효과 결과는 불변
- Thread-Safe 보장

---

## 구현 세부사항

### 파일 구조

```
tetris-core/src/main/java/seoultech/se/core/
├── item/
│   ├── Item.java                    # 인터페이스
│   ├── ItemType.java                # Enum
│   ├── ItemEffect.java              # 효과 결과 VO
│   ├── ItemConfig.java              # 설정 객체
│   ├── ItemManager.java             # 매니저
│   ├── AbstractItem.java            # 추상 클래스
│   └── impl/
│       ├── BombItem.java            # 폭탄
│       ├── PlusItem.java            # 십자
│       ├── SpeedResetItem.java      # 속도 초기화
│       └── BonusScoreItem.java      # 보너스 점수
└── config/
    └── GameModeConfig.java          # itemConfig 필드 추가

tetris-client/src/main/java/seoultech/se/client/
├── config/
│   └── GameModeProperties.java      # 아이템 설정 추가
└── service/
    └── SettingsService.java         # buildArcadeConfig() 추가
```

### 핵심 코드 예시

#### 1. 아이템 사용

```java
// ItemManager에서 아이템 사용
ItemManager itemManager = new ItemManager(0.1, enabledItems);
Item item = itemManager.generateRandomItem();

if (item != null) {
    ItemEffect effect = itemManager.useItem(item, gameState, row, col);
    
    if (effect.isSuccess()) {
        // 점수 업데이트
        gameState.setScore(gameState.getScore() + effect.getBonusScore());
        
        // UI 업데이트
        System.out.println(effect.getMessage());
    }
}
```

#### 2. 설정에서 아이템 활성화

```java
// application.properties
tetris.mode.item-enabled.BOMB=true
tetris.mode.item-enabled.PLUS=true
tetris.mode.item-enabled.SPEED_RESET=false
tetris.mode.item-enabled.BONUS_SCORE=true

// SettingsService에서 로드
GameModeConfig config = settingsService.buildGameModeConfig();
ItemConfig itemConfig = config.getItemConfig();

// SPEED_RESET은 비활성화됨
boolean enabled = itemConfig.isItemEnabled(ItemType.SPEED_RESET); // false
```

---

## 확장 가이드

### 새로운 아이템 추가 방법

#### Step 1: ItemType에 추가

```java
public enum ItemType {
    BOMB("Bomb", "💣", "Clears a 5x5 area"),
    PLUS("Plus", "➕", "Clears row and column"),
    SPEED_RESET("Speed Reset", "⚡", "Resets speed"),
    BONUS_SCORE("Bonus Score", "⭐", "Bonus points"),
    // ✨ 새 아이템 추가
    FREEZE("Freeze", "❄️", "Freezes falling speed for 10 seconds");
}
```

#### Step 2: 구현 클래스 생성

```java
package seoultech.se.core.item.impl;

public class FreezeItem extends AbstractItem {
    
    public FreezeItem() {
        super(ItemType.FREEZE);
    }
    
    @Override
    public ItemEffect apply(GameState gameState, int row, int col) {
        if (!isEnabled()) {
            return ItemEffect.none();
        }
        
        // 효과 로직 구현
        // (예: GameState에 freeze 플래그 설정)
        
        return ItemEffect.success(
            ItemType.FREEZE, 
            0, 
            100, 
            "❄️ Freeze activated!"
        );
    }
}
```

#### Step 3: ItemManager에 등록

```java
private void registerPrototypes() {
    registerItem(new BombItem());
    registerItem(new PlusItem());
    registerItem(new SpeedResetItem());
    registerItem(new BonusScoreItem());
    registerItem(new FreezeItem()); // ✨ 추가
}
```

#### Step 4: application.properties에 추가

```properties
tetris.mode.item-enabled.FREEZE=${ITEM_ENABLED_FREEZE:true}
```

**끝! 기존 코드 수정 없이 새 아이템 추가 완료** ✅

---

## 통합 가이드

### GameEngine에 통합 예시

```java
public class GameEngine {
    private ItemManager itemManager;
    
    public void initialize(GameModeConfig config) {
        // 아이템 시스템 초기화
        if (config.getItemConfig() != null && config.getItemConfig().isEnabled()) {
            ItemConfig itemConfig = config.getItemConfig();
            itemManager = new ItemManager(
                itemConfig.getDropRate(),
                itemConfig.getEnabledItems()
            );
            System.out.println("✅ Item system initialized");
        }
    }
    
    public void onLineClear(int linesCleared) {
        // 라인 클리어 시 아이템 드롭 체크
        if (itemManager != null && itemManager.shouldDropItem()) {
            Item item = itemManager.generateRandomItem();
            if (item != null) {
                // 아이템을 인벤토리에 추가하거나
                // 즉시 사용 (설정에 따라)
                addItemToInventory(item);
            }
        }
    }
    
    public void useItem(Item item, int row, int col) {
        if (itemManager != null) {
            ItemEffect effect = itemManager.useItem(item, gameState, row, col);
            
            if (effect.isSuccess()) {
                // 효과 적용 후 처리
                handleItemEffect(effect);
            }
        }
    }
}
```

### UI 통합 예시 (JavaFX)

```java
public class ItemInventoryView extends VBox {
    
    public void displayItems(List<Item> items) {
        getChildren().clear();
        
        for (Item item : items) {
            HBox itemBox = new HBox(10);
            
            Label icon = new Label(item.getIcon());
            Label name = new Label(item.getType().getDisplayName());
            Button useButton = new Button("사용");
            
            useButton.setOnAction(e -> onItemUsed(item));
            
            itemBox.getChildren().addAll(icon, name, useButton);
            getChildren().add(itemBox);
        }
    }
    
    private void onItemUsed(Item item) {
        // GameEngine에 아이템 사용 요청
        gameEngine.useItem(item, targetRow, targetCol);
    }
}
```

---

## 테스트 전략

### 단위 테스트

```java
@Test
void testBombItem() {
    GameState gameState = new GameState(10, 20);
    // 테스트용 블록 배치
    
    BombItem bomb = new BombItem();
    ItemEffect effect = bomb.apply(gameState, 5, 5);
    
    assertTrue(effect.isSuccess());
    assertEquals(25, effect.getBlocksCleared()); // 5x5
    assertEquals(125, effect.getBonusScore());   // 25 × 5
}

@Test
void testItemManager() {
    Set<ItemType> enabledItems = EnumSet.of(ItemType.BOMB, ItemType.PLUS);
    ItemManager manager = new ItemManager(0.1, enabledItems);
    
    Item item = manager.generateRandomItem();
    
    assertNotNull(item);
    assertTrue(enabledItems.contains(item.getType()));
}
```

---

## 설정 예시

### application.properties (완전한 예시)

```properties
# ========== Item System Configuration ==========
# 드롭 확률 (10%)
tetris.mode.item-drop-rate=0.1

# 아이템 활성화 설정
tetris.mode.item-enabled.BOMB=true
tetris.mode.item-enabled.PLUS=true
tetris.mode.item-enabled.SPEED_RESET=true
tetris.mode.item-enabled.BONUS_SCORE=true

# 인벤토리 설정
tetris.mode.max-inventory-size=3
tetris.mode.item-auto-use=false
```

---

## 요약

### ✅ 완료된 작업
1. ✅ Core 모듈에 아이템 시스템 구현
2. ✅ 4가지 기본 아이템 구현
3. ✅ ItemManager (팩토리 & 관리자) 구현
4. ✅ GameModeConfig에 ItemConfig 통합
5. ✅ SettingsService에 아이템 설정 빌더 추가
6. ✅ application.properties에 설정 추가

### 🔲 향후 작업
1. 🔲 GameEngine에 아이템 시스템 통합
2. 🔲 UI에서 아이템 인벤토리 표시
3. 🔲 UI에서 아이템 활성화/비활성화 설정
4. 🔲 단위 테스트 작성

### 🎯 주요 장점
- ✨ **확장성**: 새 아이템 추가가 매우 쉬움
- ✨ **유연성**: 설정으로 아이템 활성화/비활성화
- ✨ **타입 안전성**: Enum과 인터페이스로 타입 보장
- ✨ **테스트 용이성**: 각 아이템이 독립적으로 테스트 가능
- ✨ **현대적 설계**: SOLID 원칙 및 디자인 패턴 적용

---

**작성일**: 2025-10-29  
**버전**: 1.0  
**작성자**: GitHub Copilot
