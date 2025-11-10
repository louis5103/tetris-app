# 🚀 아케이드 모드 아이템 시스템 - 빠른 시작 가이드

## ✅ 이미 완료된 작업

### 1. Core 모듈 (tetris-core)
- ✅ `ItemType` enum - 4가지 아이템 타입 정의
- ✅ `Item` 인터페이스 - 아이템 기본 계약
- ✅ `ItemEffect` - 아이템 효과 결과 객체
- ✅ `ItemConfig` - 아이템 설정 객체
- ✅ `ItemManager` - 아이템 팩토리 및 관리자
- ✅ `AbstractItem` - 아이템 추상 클래스
- ✅ **4가지 아이템 구현체**:
  - `BombItem` (💣 폭탄 - 5x5 영역 제거)
  - `PlusItem` (➕ 십자 - 행/열 제거)
  - `SpeedResetItem` (⚡ 속도 초기화)
  - `BonusScoreItem` (⭐ 보너스 점수)

### 2. Client 모듈 (tetris-client)
- ✅ `GameModeProperties` - 아이템 설정 필드 추가
- ✅ `SettingsService.buildArcadeConfig()` - 아이템 설정 빌더
- ✅ `application.properties` - 아이템 설정 추가

### 3. 문서 & 테스트
- ✅ `ARCADE_ITEM_SYSTEM_DESIGN.md` - 상세 설계 문서
- ✅ `ItemSystemTest.java` - 단위 테스트

---

## 🔲 다음 단계: GameEngine 통합

### Step 1: GameEngine에 ItemManager 추가

```java
public class GameEngine {
    private ItemManager itemManager;
    
    public void initialize(GameState gameState, GameModeConfig config) {
        // ... 기존 초기화 코드 ...
        
        // ✨ 아이템 시스템 초기화
        if (config.getItemConfig() != null && config.getItemConfig().isEnabled()) {
            ItemConfig itemConfig = config.getItemConfig();
            itemManager = new ItemManager(
                itemConfig.getDropRate(),
                itemConfig.getEnabledItems()
            );
            System.out.println("✅ Item system initialized");
        }
    }
}
```

### Step 2: 라인 클리어 시 아이템 드롭

```java
public void clearFullLines() {
    int linesCleared = /* 라인 클리어 로직 */;
    
    if (linesCleared > 0) {
        // 점수 계산 등...
        
        // ✨ 아이템 드롭 체크
        if (itemManager != null && itemManager.shouldDropItem()) {
            Item item = itemManager.generateRandomItem();
            if (item != null) {
                System.out.println("🎁 Item dropped: " + item.getType());
                // TODO: 인벤토리에 추가하거나 즉시 사용
            }
        }
    }
}
```

### Step 3: 아이템 사용 메서드 추가

```java
/**
 * 아이템 사용
 * 
 * @param item 사용할 아이템
 * @param row 대상 행 (아이템에 따라 의미가 다름)
 * @param col 대상 열 (아이템에 따라 의미가 다름)
 * @return 아이템 효과
 */
public ItemEffect useItem(Item item, int row, int col) {
    if (itemManager == null) {
        return ItemEffect.none();
    }
    
    ItemEffect effect = itemManager.useItem(item, gameState, row, col);
    
    if (effect.isSuccess()) {
        // 점수 추가
        gameState.setScore(gameState.getScore() + effect.getBonusScore());
        
        // UI 업데이트 이벤트 발생
        notifyItemUsed(effect);
    }
    
    return effect;
}
```

---

## 🔲 다음 단계: UI 구현

### 아이템 인벤토리 UI (JavaFX)

```java
public class ItemInventoryPanel extends HBox {
    
    private final List<Item> inventory = new ArrayList<>();
    private final int maxSize = 3;
    
    public void addItem(Item item) {
        if (inventory.size() < maxSize) {
            inventory.add(item);
            updateUI();
        }
    }
    
    private void updateUI() {
        getChildren().clear();
        
        for (int i = 0; i < inventory.size(); i++) {
            Item item = inventory.get(i);
            
            VBox itemSlot = new VBox(5);
            itemSlot.setAlignment(Pos.CENTER);
            
            Label icon = new Label(item.getIcon());
            icon.setStyle("-fx-font-size: 24px;");
            
            Label name = new Label(item.getType().getDisplayName());
            name.setStyle("-fx-font-size: 10px;");
            
            Button useBtn = new Button("사용");
            final int index = i;
            useBtn.setOnAction(e -> useItem(index));
            
            itemSlot.getChildren().addAll(icon, name, useBtn);
            getChildren().add(itemSlot);
        }
    }
    
    private void useItem(int index) {
        if (index < inventory.size()) {
            Item item = inventory.remove(index);
            // GameEngine에 아이템 사용 요청
            gameController.useItem(item);
            updateUI();
        }
    }
}
```

### 아이템 설정 UI

```java
public class ItemSettingsPanel extends VBox {
    
    @Autowired
    private GameModeProperties gameModeProperties;
    
    @FXML
    private CheckBox bombCheckBox;
    @FXML
    private CheckBox plusCheckBox;
    @FXML
    private CheckBox speedResetCheckBox;
    @FXML
    private CheckBox bonusScoreCheckBox;
    @FXML
    private Slider dropRateSlider;
    
    @FXML
    public void initialize() {
        // 현재 설정 로드
        bombCheckBox.setSelected(gameModeProperties.isItemEnabled("BOMB"));
        plusCheckBox.setSelected(gameModeProperties.isItemEnabled("PLUS"));
        speedResetCheckBox.setSelected(gameModeProperties.isItemEnabled("SPEED_RESET"));
        bonusScoreCheckBox.setSelected(gameModeProperties.isItemEnabled("BONUS_SCORE"));
        
        dropRateSlider.setValue(gameModeProperties.getItemDropRate() * 100);
    }
    
    @FXML
    public void saveSettings() {
        // 설정 저장
        gameModeProperties.setItemEnabled("BOMB", bombCheckBox.isSelected());
        gameModeProperties.setItemEnabled("PLUS", plusCheckBox.isSelected());
        gameModeProperties.setItemEnabled("SPEED_RESET", speedResetCheckBox.isSelected());
        gameModeProperties.setItemEnabled("BONUS_SCORE", bonusScoreCheckBox.isSelected());
        
        gameModeProperties.setItemDropRate(dropRateSlider.getValue() / 100.0);
        
        System.out.println("✅ Item settings saved");
    }
}
```

---

## 📝 설정 예시

### application.properties에서 아이템 커스터마이징

```properties
# 드롭 확률을 20%로 증가
tetris.mode.item-drop-rate=0.2

# BOMB와 PLUS만 활성화
tetris.mode.item-enabled.BOMB=true
tetris.mode.item-enabled.PLUS=true
tetris.mode.item-enabled.SPEED_RESET=false
tetris.mode.item-enabled.BONUS_SCORE=false

# 인벤토리 크기를 5개로 증가
tetris.mode.max-inventory-size=5

# 아이템 즉시 사용 모드
tetris.mode.item-auto-use=true
```

---

## 🎯 테스트 실행

```bash
# Core 모듈 테스트
cd tetris-core
./gradlew test --tests ItemSystemTest

# 전체 프로젝트 테스트
cd ..
./gradlew test
```

**기대 결과**:
- ✅ 모든 테스트 통과
- ✅ 아이템 생성/사용 정상 작동
- ✅ 설정 로드/저장 정상 작동

---

## 🐛 트러블슈팅

### 문제 1: 아이템이 드롭되지 않음
**원인**: ItemManager가 초기화되지 않음  
**해결**: GameEngine.initialize()에서 ItemConfig가 null이 아닌지 확인

### 문제 2: 설정이 저장되지 않음
**원인**: application.properties가 로드되지 않음  
**해결**: GameModeProperties가 @Configuration으로 등록되었는지 확인

### 문제 3: 특정 아이템이 생성되지 않음
**원인**: 해당 아이템이 비활성화됨  
**해결**: application.properties에서 `tetris.mode.item-enabled.{ITEM_NAME}=true` 확인

---

## 📚 추가 참고 자료

- [상세 설계 문서](./ARCADE_ITEM_SYSTEM_DESIGN.md)
- [게임 모드 구현 계획](./GAME_MODE_IMPLEMENTATION_PLAN.md)
- [테스트 코드](../tetris-core/src/test/java/seoultech/se/core/item/ItemSystemTest.java)

---

## 🎉 완료 체크리스트

- [x] Core 모듈 아이템 시스템 구현
- [x] 4가지 기본 아이템 구현
- [x] 설정 시스템 통합
- [x] 테스트 코드 작성
- [x] 설계 문서 작성
- [ ] GameEngine 통합
- [ ] UI 구현 (인벤토리)
- [ ] UI 구현 (설정 패널)
- [ ] 통합 테스트

**현재 진행률: 60% 완료** 🎯

---

**다음 작업**: GameEngine에 아이템 시스템 통합하기
