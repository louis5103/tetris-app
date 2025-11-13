# 아이템 시스템 검증 보고서

날짜: 2024년 (현재)
검증자: GitHub Copilot
목적: 사용자가 제기한 4가지 아이템 시스템 이슈 검증

---

## 📋 검증 항목

1. **SPEED_RESET 아이템**: 속도 초기화 기능이 실제로 작동하는가?
2. **LINE_CLEAR 아이템**: 줄이 꽉 차지 않아도 삭제하는가? (명세 확인)
3. **모든 Lock 경로**: Hard Drop, Soft Drop, Auto Lock의 검증 로직이 빠짐없이 작동하는가?
4. **중력 시스템**: 모든 아이템에 일관되게 적용되는가?

---

## 🔍 검증 결과

### 1. SPEED_RESET 아이템 검증 ❌

**파일**: `tetris-core/src/main/java/seoultech/se/core/item/impl/SpeedResetItem.java`

**현재 구현** (Line 50-65):
```java
@Override
public ItemEffect apply(GameState gameState, int row, int col) {
    if (!isEnabled()) {
        return ItemEffect.none();
    }
    
    // 🎮 GAME UX: 소프트 드롭 속도 초기화
    // GameEngine에 속도 초기화 메서드가 있어야 합니다
    // 현재는 GameState에 플래그만 설정
    gameState.setLastActionWasRotation(false); // 임시로 플래그 활용
    
    // 실제로는 BoardController의 속도를 초기화해야 함
    // TODO: GameEngine에 resetSoftDropSpeed() 메서드 추가
    
    String message = "⚡ Speed Reset! 속도가 초기값으로 돌아갑니다.";
    System.out.println(message);
    
    return ItemEffect.success(ItemType.SPEED_RESET, 0, 0, message);
}
```

**문제점**:
- ❌ **임시 플래그만 설정**: `gameState.setLastActionWasRotation(false)` - T-Spin 관련 플래그를 임시로 사용
- ❌ **실제 속도 변경 없음**: GameState에 속도 필드가 존재하지 않음
- ❌ **TODO 주석**: "GameEngine에 resetSoftDropSpeed() 메서드 추가" - 미구현 상태
- ❌ **BoardController 연동 없음**: 실제 게임 루프의 속도는 BoardController에서 관리되는데, 아무 연결도 없음

**GameState 확인**:
- `tetris-core/src/main/java/seoultech/se/core/GameState.java` 확인 결과
- 속도 관련 필드 없음: `speed`, `softDropSpeed`, `dropInterval` 등 존재하지 않음

**결론**: ❌ **SPEED_RESET 아이템은 실제로 속도를 초기화하지 않습니다.**

**권장 해결 방안**:
1. **Option A (GameState 확장)**:
   - GameState에 `private long dropInterval` 필드 추가
   - SpeedResetItem에서 `gameState.setDropInterval(initialDropInterval)` 호출
   - BoardController에서 `gameState.getDropInterval()` 값 사용

2. **Option B (Event-based)**:
   - SpeedResetItem이 ItemEffect에 "SPEED_RESET" 플래그 설정
   - BoardController가 ItemEffect를 감지하고 타이머 속도 리셋

3. **Option C (Direct Callback)**:
   - SpeedResetItem이 callback 함수를 통해 BoardController에 직접 알림
   - BoardController가 타이머 속도 조정

---

### 2. LINE_CLEAR 아이템 검증 ✅

**파일**: `tetris-core/src/main/java/seoultech/se/core/item/impl/LineClearItem.java`

**명세 확인** (Line 14-20):
```java
/**
 * 줄 삭제 아이템 ('L')
 * 
 * Req2 필수 아이템 #1
 * 
 * 효과:
 * - 블록이 고정되면 'L'이 위치한 줄을 즉시 삭제
 * - 해당 줄이 꽉 차있지 않아도 삭제됨  ✅
 * - 삭제된 줄에 대해서도 기존 방식대로 점수 계산
 */
```

**실제 구현 확인** (Line 104-149):
```java
public static java.util.List<Integer> findAndClearMarkedLines(GameState gameState) {
    // 'L' 마커가 있는 줄 찾기
    for (int row = 0; row < boardHeight; row++) {
        boolean hasMarker = false;
        int occupiedCount = 0;
        
        for (int col = 0; col < boardWidth; col++) {
            if (grid[row][col].isOccupied()) {
                occupiedCount++;
            }
            if (grid[row][col].hasItemMarker() && 
                grid[row][col].getItemMarker() == ItemType.LINE_CLEAR) {
                hasMarker = true;
            }
        }
        
        if (hasMarker) {
            clearedRows.add(row);
            // occupiedCount는 로그만 출력, 조건으로 사용하지 않음
            System.out.println("Ⓛ [LineClearItem] Found 'L' marker at row " + row + 
                " (" + occupiedCount + "/" + boardWidth + " occupied)");
        }
    }
    // ...
}
```

**동작 분석**:
- ✅ **'L' 마커만 체크**: `hasMarker` 플래그만으로 삭제 여부 결정
- ✅ **occupiedCount 미사용**: 줄이 꽉 찬지 여부를 체크하지 않음
- ✅ **명세 준수**: "해당 줄이 꽉 차있지 않아도 삭제됨"을 정확히 구현

**clearLines() 메서드** (Line 151-209):
```java
public static int clearLines(GameState gameState, java.util.List<Integer> rowsToRemove) {
    // 지정된 줄들을 무조건 삭제
    for (int row : rowsToRemove) {
        int rowBlockCount = 0;
        for (int col = 0; col < boardWidth; col++) {
            if (grid[row][col].isOccupied()) {
                totalBlocksCleared++;
                rowBlockCount++;
            }
        }
        System.out.println("Ⓛ [LineClearItem] Row " + row + " has " + rowBlockCount + 
            " occupied blocks (will clear entire row)");  // ✅ 꽉 차지 않아도 삭제
    }
    // ...
}
```

**결론**: ✅ **LINE_CLEAR 아이템은 명세대로 작동합니다. 줄이 꽉 차지 않아도 'L' 마커가 있으면 해당 줄을 삭제합니다.**

---

### 3. 모든 Lock 경로 검증 로직 확인 ⏳ (추가 조사 필요)

**세 가지 Lock 경로**:
1. **Hard Drop**: 스페이스바로 즉시 낙하 + 고정
2. **Soft Drop**: DOWN 키 누른 채로 아래 이동 + 바닥 접촉 시 고정
3. **Auto Lock (Game Loop)**: 타이머로 자동 낙하 + 바닥 접촉 시 고정

**확인 필요 사항**:
- GameEngine의 `hardDrop()` 메서드가 `lockTetromino()` 호출하는가?
- BoardController에서 Soft Drop 감지 시 Lock 처리하는가?
- GameLoop의 타이머에서 자동 낙하 + Lock 처리하는가?
- 모든 경로에서 아이템 효과 적용 (`applyItemEffectAfterLock()`)이 호출되는가?

**현재 파악된 정보**:
- `GameEngine` 인터페이스에 `lockTetromino()` 메서드 존재 (Line 113-126)
- `ArcadeGameEngine`이 `ClassicGameEngine`을 확장
- tetris-client의 `BoardController`에서 Lock 처리하는 것으로 추정

**필요한 추가 조사**:
1. `ArcadeGameEngine.hardDrop()` 구현 확인
2. `ArcadeGameEngine.tryMoveDown()` 구현 확인 (Auto Lock 포함)
3. `BoardController`의 키보드 입력 처리 확인
4. `BoardController`의 GameLoop 타이머 확인

**임시 결론**: ⚠️ **추가 코드 확인 필요 - 각 Lock 경로의 구현을 직접 읽어야 정확히 판단 가능**

---

### 4. 중력 시스템 일관성 확인 ⚠️

**현재 중력 적용 현황**:

| 아이템 | 중력 적용 여부 | 파일 위치 | 코멘트 |
|--------|--------------|----------|--------|
| BOMB | ✅ 적용됨 | `BombItem.java` Line 90-106 | 라인 클리어는 제거됨 (Phase 12 수정) |
| PLUS | ✅ 적용됨 | `PlusItem.java` Line 87-93 | 라인 클리어는 제거됨 (Phase 12 수정) |
| LINE_CLEAR | ❓ 불명 | `LineClearItem.java` | 코드에서 applyGravity() 호출 확인 안 됨 |
| SPEED_RESET | ❓ 불명 | `SpeedResetItem.java` | 블록 삭제 없음 → 중력 불필요? |
| BONUS_SCORE | ❓ 불명 | `BonusScoreItem.java` | 블록 삭제 없음 → 중력 불필요? |
| WEIGHT_BOMB | ❓ 불명 | `WeightBombItem.java` | 확인 필요 |

**BOMB 아이템 중력 적용** (`BombItem.java` Line 90-106):
```java
// 🎮 GAME UX: 중력 적용 (라인 클리어는 제거)
if (blocksCleared > 0) {
    applyGravity(gameState);
    System.out.println("   - Gravity applied (no line clear)");
}
```

**PLUS 아이템 중력 적용** (`PlusItem.java` Line 87-93):
```java
// 🎮 GAME UX: 중력 적용 (라인 클리어는 제거)
if (blocksCleared > 0) {
    applyGravity(gameState);
    System.out.println("   - Gravity applied (no line clear)");
}
```

**LINE_CLEAR 아이템 중력 미확인**:
- `LineClearItem.apply()` 메서드 (Line 52-97)에서 `applyGravity()` 호출 확인 안 됨
- 대신 `findAndClearMarkedLines()` + `clearLines()` 사용
- `clearLines()` 메서드에서 블록을 아래로 내리는 로직은 있음 (Line 178-196)

```java
// clearLines() 메서드 내부 (Line 178-196)
// 남아있는 줄들만 수집 (아래에서 위로)
java.util.List<Cell[]> remainingRows = new java.util.ArrayList<>();
for (int row = boardHeight - 1; row >= 0; row--) {
    if (!rowsSet.contains(row)) {
        Cell[] rowCopy = new Cell[boardWidth];
        for (int col = 0; col < boardWidth; col++) {
            rowCopy[col] = grid[row][col].copy();
        }
        remainingRows.add(rowCopy);
    }
}

// 보드를 아래에서부터 다시 채우기
int targetRow = boardHeight - 1;
for (Cell[] rowData : remainingRows) {
    for (int col = 0; col < boardWidth; col++) {
        grid[targetRow][col] = rowData[col];
    }
    targetRow--;
}
```

**분석**:
- LINE_CLEAR는 자체적으로 줄 삭제 + 위 블록 내리기를 구현
- BOMB/PLUS는 블록 삭제 후 `applyGravity()` 호출
- 두 가지 다른 중력 적용 방식 사용 → 일관성 부족 가능성

**권장 사항**:
1. **LINE_CLEAR**: 이미 자체 중력 로직 있음 → 문제 없음
2. **SPEED_RESET, BONUS_SCORE**: 블록 삭제 없음 → 중력 불필요
3. **WEIGHT_BOMB**: 확인 필요 (코드 읽어야 함)
4. **일관성 개선**: 
   - `applyGravity()` 공통 메서드를 모든 아이템이 사용하도록 통일
   - 또는 각 아이템의 특성에 맞게 명시적으로 중력 적용/미적용 결정

**임시 결론**: ⚠️ **BOMB/PLUS는 중력 적용 확인. LINE_CLEAR는 자체 로직. 나머지 아이템은 추가 확인 필요.**

---

## 📊 종합 평가

| 검증 항목 | 상태 | 평가 |
|----------|-----|-----|
| 1. SPEED_RESET 속도 초기화 | ❌ 실패 | 임시 플래그만 설정, 실제 속도 변경 없음 |
| 2. LINE_CLEAR 동작 | ✅ 통과 | 명세대로 꽉 차지 않아도 삭제 |
| 3. 모든 Lock 경로 검증 | ⏳ 보류 | 추가 코드 확인 필요 |
| 4. 중력 시스템 일관성 | ⚠️ 부분적 | BOMB/PLUS 확인, 나머지 확인 필요 |

---

## 🛠️ 권장 조치 사항

### 우선순위 1: SPEED_RESET 기능 구현 ❌

**현재 상태**: 완전히 작동하지 않음

**해결 방안**:
1. GameState에 `dropInterval` 필드 추가
2. SpeedResetItem에서 `gameState.setDropInterval(초기값)` 호출
3. BoardController에서 `gameState.getDropInterval()` 읽어서 타이머 속도 조정

**예상 작업량**: 2-3개 파일 수정

---

### 우선순위 2: Lock 경로 검증 로직 확인 ⏳

**현재 상태**: 코드 확인 필요

**필요한 작업**:
1. `ArcadeGameEngine.hardDrop()` 전체 읽기
2. `ArcadeGameEngine.tryMoveDown()` 전체 읽기
3. `BoardController` 키보드 입력 처리 확인
4. `BoardController` GameLoop 타이머 확인

**예상 작업량**: 4개 메서드 읽기 + 분석

---

### 우선순위 3: 중력 시스템 일관성 검증 ⚠️

**현재 상태**: 부분적으로 확인됨

**필요한 작업**:
1. `WeightBombItem.java` 전체 읽기
2. 모든 아이템의 중력 적용 방식 통일 고려
3. `applyGravity()` 공통 메서드 사용 권장

**예상 작업량**: 1개 파일 확인 + 설계 검토

---

## 📝 참고: 관련 파일 목록

### 아이템 관련
- `tetris-core/src/main/java/seoultech/se/core/item/impl/SpeedResetItem.java`
- `tetris-core/src/main/java/seoultech/se/core/item/impl/LineClearItem.java`
- `tetris-core/src/main/java/seoultech/se/core/item/impl/BombItem.java`
- `tetris-core/src/main/java/seoultech/se/core/item/impl/PlusItem.java`
- `tetris-core/src/main/java/seoultech/se/core/item/impl/BonusScoreItem.java`
- `tetris-core/src/main/java/seoultech/se/core/item/impl/WeightBombItem.java`

### 게임 엔진 관련
- `tetris-core/src/main/java/seoultech/se/core/engine/GameEngine.java` (인터페이스)
- `tetris-core/src/main/java/seoultech/se/core/engine/ClassicGameEngine.java`
- `tetris-core/src/main/java/seoultech/se/core/engine/ArcadeGameEngine.java`

### 게임 상태 관련
- `tetris-core/src/main/java/seoultech/se/core/GameState.java`

### 클라이언트 컨트롤러 관련
- `tetris-client/src/main/java/seoultech/se/client/controller/BoardController.java`

---

## 🎯 다음 단계

1. **SPEED_RESET 아이템 수정 구현** (최우선)
2. **Lock 경로 코드 정밀 분석** (높음)
3. **중력 시스템 통일성 검토** (중간)
4. **QA 테스트 추가** (추가 검증용)

---

**보고서 작성**: GitHub Copilot
**검증 날짜**: 2024년 현재
**검증 방법**: 정적 코드 분석 + 명세 비교
