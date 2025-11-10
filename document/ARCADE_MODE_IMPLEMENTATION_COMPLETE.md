# 아케이드 모드 아이템 시스템 구현 완료

## 📋 개요

테트리스 게임에 아케이드 모드를 위한 아이템 시스템을 완전히 구현했습니다. 라인 클리어 시 10% 확률로 4가지 아이템 중 하나가 드롭되며, 아이템 사용 시 특별한 효과가 발생합니다.

## ✅ 구현 완료 항목

### 1. Core 모듈 (tetris-core)

#### 아이템 시스템 아키텍처
- ✅ `ItemType` enum: 4가지 아이템 타입 정의 (BOMB, PLUS, SPEED_RESET, BONUS_SCORE)
- ✅ `Item` 인터페이스: 아이템 공통 인터페이스
- ✅ `ItemEffect` 클래스: 아이템 효과 결과 (불변 객체)
- ✅ `ItemConfig` 클래스: 아이템 설정 (Builder 패턴)
- ✅ `ItemManager` 클래스: 아이템 생성 및 관리 (Factory 패턴)
- ✅ `AbstractItem` 추상 클래스: 공통 로직 (Template Method 패턴)

#### 아이템 구현체
- ✅ `BombItem`: 5x5 영역 제거 (💣)
- ✅ `PlusItem`: 행과 열 전체 제거 (➕)
- ✅ `SpeedResetItem`: 낙하 속도 초기화 (⚡)
- ✅ `BonusScoreItem`: 레벨별 보너스 점수 (⭐)

#### GameEngine 통합
- ✅ `useItem(Item, GameState)`: 아이템 사용 → 테트로미노를 아이템 블록으로 변환
- ✅ `applyItemEffect(GameState, ItemType)`: Lock 시 아이템 효과 적용
- ✅ `tryDropItem()`: 라인 클리어 시 아이템 드롭 확률 계산

#### GameState 확장
- ✅ `currentItemType`: 현재 테트로미노가 아이템 블록인지 추적
- ✅ Cell과 Tetromino는 수정하지 않음 (간결한 설계)

#### Lock 로직 개선
- ✅ **아이템 블록 Lock 시 Grid에 고정되지 않음** (블록 사라짐)
- ✅ **GameOver 임계값 예외 처리** (상단에 Lock되어도 GameOver 안됨)
- ✅ **콤보/B2B 초기화** (아이템 블록은 콤보 연장 안됨)

### 2. Client 모듈 (tetris-client)

#### 설정 시스템
- ✅ `GameModeProperties`: Spring 설정 속성
  - `itemDropRate`: 드롭 확률 (0.1 = 10%)
  - `itemEnabled`: 개별 아이템 활성화/비활성화
  - `maxInventorySize`: 최대 인벤토리 크기 (3)
- ✅ `application.properties`: 아이템 설정 추가
- ✅ `SettingsService.buildArcadeConfig()`: 아케이드 모드 설정 빌드

#### UI 컴포넌트
- ✅ `ItemInventoryPanel`: 아이템 인벤토리 UI
  - 3슬롯 인벤토리
  - 아이템 아이콘 및 이름 표시
  - "Use" 버튼 및 키보드 단축키 (1, 2, 3)
  - ItemUseCallback 인터페이스
- ✅ `GameController`: 아이템 사용 로직 통합
  - `useItem()`: 아이템을 블록으로 변환
  - `tryDropItemOnLineClear()`: 라인 클리어 시 드롭
  - 키보드 단축키 (1, 2, 3)

#### BoardController 통합
- ✅ `lockAndSpawnNext()`: Lock 전 아이템 타입 저장, Lock 후 효과 적용
- ✅ `GameEngine` 인스턴스 생성 및 초기화

#### 렌더링
- ✅ `BoardRenderer.applyItemBlockStyle()`: 아이템 블록 시각화
  - BOMB → `range-bomb-block` (폭탄 아이콘)
  - PLUS → `cross-bomb-block` (십자가 아이콘)
  - SPEED_RESET/BONUS_SCORE → `selectable-block` (무지개 효과)
- ✅ CSS 스타일: `item.css`의 기존 스타일 활용

#### FXML 및 CSS
- ✅ `game-view.fxml`: 아이템 인벤토리 컨테이너 추가
- ✅ `game-view.css`: 아이템 인벤토리 스타일 추가

### 3. 테스트

#### `ItemSystemTest.java`
- ✅ 아이템 생성 및 활성화 테스트
- ✅ 아이템 드롭 확률 테스트
- ✅ 4가지 아이템 효과 테스트

#### `ItemBlockLockTest.java` (새로 작성)
- ✅ 아이템 블록은 Grid에 고정되지 않음
- ✅ 일반 블록은 Grid에 정상 고정됨
- ✅ 아이템 블록은 상단에서 Lock되어도 GameOver 안됨
- ✅ 일반 블록은 상단에서 Lock되면 GameOver
- ✅ Lock 후 아이템 효과 정상 적용
- ✅ 아이템 블록 Lock 시 콤보/B2B 초기화

**모든 테스트 통과 ✅**

## 🎮 사용 방법

### 1. 아이템 획득
- 라인을 클리어하면 **10% 확률**로 아이템 드롭
- 최대 3개까지 인벤토리에 보관 가능
- 인벤토리가 가득 차면 새 아이템은 버려짐

### 2. 아이템 사용
- **키보드**: `1`, `2`, `3` 키로 슬롯의 아이템 사용
- **마우스**: "Use" 버튼 클릭
- 아이템 사용 시 현재 테트로미노가 **아이템 블록**으로 변환됨
- 아이템 블록이 Lock되면 효과 발생

### 3. 아이템 효과

| 아이템 | 아이콘 | 효과 | 시각적 표시 |
|--------|--------|------|------------|
| 💣 Bomb | 폭탄 | 5x5 영역 제거 | 폭탄 아이콘 |
| ➕ Plus | 십자가 | 행과 열 전체 제거 | 십자가 아이콘 |
| ⚡ Speed Reset | 번개 | 낙하 속도 초기화 | 무지개 효과 |
| ⭐ Bonus Score | 별 | 레벨별 보너스 점수 | 무지개 효과 |

### 4. 특별 규칙
- ✅ **아이템 블록은 사라짐**: Grid에 고정되지 않음
- ✅ **GameOver 예외**: 상단에 Lock되어도 GameOver 안됨
- ✅ **콤보 중단**: 아이템 블록은 콤보를 연장하지 않음

## 🔧 설정

### application.properties
```properties
# 아이템 드롭 확률 (0.0 ~ 1.0)
tetris.mode.item-drop-rate=0.1

# 개별 아이템 활성화/비활성화
tetris.mode.item-enabled.BOMB=true
tetris.mode.item-enabled.PLUS=true
tetris.mode.item-enabled.SPEED_RESET=true
tetris.mode.item-enabled.BONUS_SCORE=true

# 인벤토리 크기
tetris.mode.max-inventory-size=3
```

## 📊 아키텍처 특징

### 디자인 패턴
- **Strategy Pattern**: 각 아이템의 효과를 독립적으로 캡슐화
- **Factory Pattern**: ItemManager를 통한 아이템 생성
- **Builder Pattern**: ItemConfig, ItemEffect 구성
- **Template Method**: AbstractItem의 공통 로직

### SOLID 원칙
- **Single Responsibility**: 각 클래스는 단일 책임
- **Open-Closed**: 새 아이템 추가 시 기존 코드 수정 불필요
- **Liskov Substitution**: Item 인터페이스를 통한 다형성
- **Interface Segregation**: 필요한 인터페이스만 정의
- **Dependency Inversion**: 추상화에 의존

### 확장성
- 새 아이템 추가: `ItemType` enum에 추가 + 구현 클래스 작성
- 설정 변경: `application.properties`에서 간편하게 조정
- 시각적 커스터마이징: CSS만 수정하면 됨

## 🚀 빌드 및 실행

```bash
# 전체 빌드
./gradlew build

# 테스트 실행
./gradlew :tetris-core:test --tests "seoultech.se.core.item.*"

# 게임 실행
./gradlew :tetris-client:bootRun
```

## 📝 주요 파일

### Core
- `tetris-core/src/main/java/seoultech/se/core/item/`
  - `ItemType.java`, `Item.java`, `ItemEffect.java`
  - `ItemConfig.java`, `ItemManager.java`
  - `AbstractItem.java`
  - `impl/BombItem.java`, `PlusItem.java`, `SpeedResetItem.java`, `BonusScoreItem.java`
- `GameEngine.java`: Lock 로직, 아이템 효과 적용
- `GameState.java`: currentItemType 필드

### Client
- `GameController.java`: 아이템 사용 UI 로직
- `BoardController.java`: Lock 후 효과 적용
- `BoardRenderer.java`: 아이템 블록 시각화
- `ItemInventoryPanel.java`: 인벤토리 UI
- `application.properties`: 설정
- `game-view.fxml`, `game-view.css`: UI 레이아웃

### Tests
- `ItemSystemTest.java`: 기본 아이템 시스템 테스트
- `ItemBlockLockTest.java`: Lock 메커니즘 테스트

## ✨ 완료!

모든 기능이 정상적으로 구현되고 테스트되었습니다. 게임을 즐겨보세요! 🎮
