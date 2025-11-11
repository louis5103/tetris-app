# Phase 3: 줄 삭제 아이템 ('L') 구현 완료

## ✅ 구현된 기능

### 1. LineClearItem 클래스
- 위치: `tetris-core/src/main/java/seoultech/se/core/item/impl/LineClearItem.java`
- 기능:
  - 'L' 마커가 있는 줄 찾기: `findAndClearMarkedLines()`
  - 해당 줄 삭제 및 블록 낙하: `clearLines()`
  - 점수 계산 및 효과 적용

### 2. Cell 확장 (Phase 2에서 완료)
- `itemMarker` 필드: ItemType 저장
- `hasItemMarker()`, `clearItemMarker()` 메서드

### 3. ClassicGameEngine 수정
- `lockTetrominoInternal()` 메서드에서:
  - 블록 고정 시 `currentItemType` 확인
  - LINE_CLEAR 타입이면 무작위 셀에 'L' 마커 추가

### 4. ArcadeGameEngine 확장
- `lockTetromino()` 메서드에서:
  - 블록 고정 후 'L' 마커가 있는 줄 검색
  - 해당 줄 삭제 및 점수 계산
  - 10줄 카운터 업데이트

### 5. ItemManager 업데이트
- LineClearItem 프로토타입 등록

---

## 🔄 전체 흐름

```
1. 라인 클리어 (10줄 달성)
   ↓
2. ItemManager.checkAndGenerateItem() 호출
   ↓
3. ItemType.LINE_CLEAR 반환
   ↓
4. GameState.nextBlockItemType = LINE_CLEAR 설정
   ↓
5. 새 블록 스폰 시 (BoardController/TetrominoSpawner)
   currentItemType = nextBlockItemType
   nextBlockItemType = null
   ↓
6. 블록 고정 시 (ClassicGameEngine.lockTetrominoInternal)
   currentItemType == LINE_CLEAR 확인
   → 무작위 셀에 'L' 마커 추가
   ↓
7. ArcadeGameEngine.lockTetromino()
   LineClearItem.findAndClearMarkedLines() 호출
   → 'L' 마커가 있는 줄 삭제
   ↓
8. 점수 추가 및 라인 카운트 업데이트
```

---

## 🎮 BoardController 통합 가이드

BoardController (또는 TetrominoSpawner)에서 새 블록을 생성할 때 다음 로직을 추가해야 합니다:

```java
// BoardController의 spawnNewTetromino() 또는 유사 메서드에서

public void spawnNewTetromino() {
    // 1. Next Queue에서 다음 블록 타입 가져오기
    TetrominoType nextType = gameState.getNextQueue()[0];
    
    // 2. 새 Tetromino 생성
    Tetromino newTetromino = new Tetromino(nextType);
    
    // 3. 아이템 타입 설정 (Phase 3)
    if (gameState.getNextBlockItemType() != null) {
        gameState.setCurrentItemType(gameState.getNextBlockItemType());
        gameState.setNextBlockItemType(null);  // 사용 후 리셋
        
        System.out.println("📦 [BoardController] Spawning item block: " + 
            gameState.getCurrentItemType());
    } else {
        gameState.setCurrentItemType(null);  // 일반 블록
    }
    
    // 4. 스폰 위치 설정
    int spawnX = gameState.getBoardWidth() / 2 - 1;
    int spawnY = 0;
    
    // 5. GameState 업데이트
    gameState.setCurrentTetromino(newTetromino);
    gameState.setCurrentX(spawnX);
    gameState.setCurrentY(spawnY);
    
    // 6. Next Queue 업데이트...
}
```

---

## 📊 점수 계산

### LINE_CLEAR 아이템 점수
- 줄당 기본 점수: `100 × Level`
- 블록당 추가 점수: `10 × 블록 수`
- 예시:
  - 1줄 삭제 (10블록): `100 × Level + 10 × 10 = (100 + 100) × Level`
  - 2줄 삭제 (20블록): `200 × Level + 10 × 20 = (200 + 200) × Level`

### 라인 카운트
- 'L' 마커로 삭제된 줄도 라인 카운트에 포함
- 레벨업 진행에 기여
- 10줄 카운터에도 포함 (연쇄 아이템 가능)

---

## 🧪 테스트 방법

### 1. 단위 테스트
```java
@Test
public void testLineClearItem() {
    // GameState 생성
    GameState state = new GameState(10, 20);
    
    // 아이템 블록 고정 시뮬레이션
    state.setCurrentItemType(ItemType.LINE_CLEAR);
    
    // ClassicGameEngine으로 블록 고정
    GameEngine engine = new ClassicGameEngine();
    GameState newState = engine.lockTetromino(state);
    
    // 'L' 마커 확인
    boolean hasMarker = false;
    for (int row = 0; row < 20; row++) {
        for (int col = 0; col < 10; col++) {
            if (newState.getGrid()[row][col].hasItemMarker()) {
                hasMarker = true;
                break;
            }
        }
    }
    
    assertTrue(hasMarker, "'L' marker should be added");
}
```

### 2. 통합 테스트
1. Arcade 모드로 게임 시작
2. 10줄 클리어
3. 다음 블록에 'L' 표시 확인 (UI)
4. 'L' 블록 고정
5. 해당 줄이 삭제되는지 확인
6. 점수가 올바르게 계산되는지 확인

---

## ⚠️ 주의사항

### 1. currentItemType vs nextBlockItemType
- `currentItemType`: 현재 떨어지고 있는 블록의 아이템 타입
- `nextBlockItemType`: 다음 블록에 적용될 아이템 타입 (10줄 달성 시 설정)

### 2. 'L' 마커 위치
- 블록 내 무작위 하나의 셀에만 추가
- 같은 블록에 여러 개의 'L' 마커가 있으면 안 됨

### 3. 줄 삭제 타이밍
- 일반 라인 클리어 후에 'L' 마커 줄 삭제
- 'L' 마커 줄도 라인 카운트에 포함
- 점수는 별도로 계산 (일반 라인 클리어 점수 + 'L' 마커 점수)

### 4. Hold 기능과의 상호작용
- Hold 시 currentItemType도 함께 보관되어야 함
- Hold에서 꺼낼 때 itemType도 함께 복원
- **TODO**: GameState에 heldItemType 필드 추가 필요 (Phase 5)

---

## 🎯 다음 단계: Phase 4

Phase 4에서는 **무게추 아이템** 구현이 예정되어 있습니다.

무게추 아이템은 더 복잡한 로직이 필요합니다:
- 4칸 너비의 특수 블록 형태
- 초기: 좌우 이동 가능
- 바닥 접촉 후: 좌우 이동 불가, 아래로만 이동
- 떨어지면서 아래 블록 제거

---

## 📝 완료 체크리스트

- [x] LineClearItem 클래스 생성
- [x] ItemManager에 등록
- [x] ClassicGameEngine에 'L' 마커 추가 로직
- [x] ArcadeGameEngine에 줄 삭제 로직
- [x] Cell itemMarker 필드 활용
- [x] 점수 계산 로직
- [ ] BoardController 통합 (클라이언트 측 작업)
- [ ] Hold 기능과의 통합 (Phase 5)
- [ ] UI에서 'L' 마커 표시 (클라이언트 측 작업)
- [ ] 단위 테스트 작성
- [ ] 통합 테스트

---

## 🔧 문제 해결

### Q: 'L' 마커가 추가되지 않아요
A: `currentItemType`이 제대로 설정되었는지 확인하세요. BoardController에서 `nextBlockItemType`을 `currentItemType`으로 복사하는 로직이 필요합니다.

### Q: 줄이 삭제되지 않아요
A: Arcade 모드인지 확인하세요. Classic 모드에서는 아이템이 작동하지 않습니다.

### Q: 점수가 이상해요
A: 일반 라인 클리어 점수와 'L' 마커 점수가 별도로 계산됩니다. 로그를 확인해보세요.

---

생성일: 2025-01-10
작성자: Claude (Anthropic)
버전: Phase 3
