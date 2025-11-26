# 🔍 라인 클리어 로직 감사 보고서

> **작성일**: 2025년 11월 27일  
> **대상**: ArcadeGameEngine, ClassicGameEngine, BOMB/PLUS 아이템  
> **목적**: 라인 클리어 및 락 로직의 일관성 검증

---

## 📊 조사 개요

ArcadeGameEngine의 라인 클리어 및 락 로직과 아이템들(BOMB, PLUS, LINE_CLEAR)의 라인 클리어 호출 사이의 일관성을 전수 조사했습니다.

---

## ✅ 조사 결과 요약

### 발견된 문제
1. ✅ **Cell 복사 방식 불일치** - **수정 완료**
2. ✅ **LINE_CLEAR 중복 처리 가능성** - **이미 해결됨**
3. ✅ **BOMB/PLUS 라인 클리어 GameState 반영** - **이미 해결됨**

### 전체 평가
**🟢 양호**: 모든 주요 문제가 수정되었거나 이미 해결되어 있음

---

## 🔍 상세 조사 내용

### 1. ClassicGameEngine의 라인 클리어 로직

**파일**: `ClassicGameEngine.java`  
**메서드**: `checkAndClearLines(GameState state, boolean isTSpin, boolean isTSpinMini)`

**처리 흐름**:
```
1. 꽉 찬 줄 찾기 (isOccupied() 체크)
2. Set 기반으로 제거할 줄 수집
3. 남아있는 줄들만 수집 (Cell.copy() 사용)
4. 보드를 아래에서부터 다시 채우기
5. GameState 업데이트:
   - lastLinesCleared
   - lastClearedRows
   - lastScoreEarned
   - lastIsPerfectClear
```

**발견된 문제**:
- ❌ **Cell 참조 복사**: `state.getGrid()[targetRow][col] = rowData[col]`
- **영향**: 렌더링 불일치, 메모리 참조 문제 발생 가능
- ✅ **수정**: Cell 값 복사로 변경 (`setColor`, `setOccupied`, `setItemMarker`)

**수정 후 코드**:
```java
// 🔥 FIX: Cell 값을 복사 (참조가 아닌 값 복사)
state.getGrid()[targetRow][col].setColor(rowData[col].getColor());
state.getGrid()[targetRow][col].setOccupied(rowData[col].isOccupied());
state.getGrid()[targetRow][col].setItemMarker(rowData[col].getItemMarker());
```

---

### 2. BOMB/PLUS 아이템의 라인 클리어 로직

**파일**: `BombItem.java`, `PlusItem.java`  
**메서드**: `checkAndClearLines(GameState gameState)`

**처리 흐름**:
```
1. applyGravity() - 블록을 아래로 이동
2. checkAndClearLines() 호출
3. 꽉 찬 줄 찾기
4. Set 기반으로 제거할 줄 수집
5. 남아있는 줄들만 수집 (Cell.copy() 사용)
6. Cell 값 복사로 보드 재구성
7. 제거된 줄 수 반환
8. ItemEffect.successWithLines()로 반환
```

**검증 결과**:
- ✅ **Cell 값 복사**: 이미 올바르게 구현됨
- ✅ **Set 기반 처리**: 다중 라인 제거 시 인덱스 오류 방지
- ✅ **라인 수 반환**: ItemEffect.linesCleared에 포함

**코드 일관성**:
```java
// BombItem & PlusItem - 동일한 구현
grid[targetRow][col].setColor(rowData[col].getColor());
grid[targetRow][col].setOccupied(rowData[col].isOccupied());
grid[targetRow][col].setItemMarker(rowData[col].getItemMarker());
```

---

### 3. ArcadeGameEngine의 처리 순서

**파일**: `ArcadeGameEngine.java`  
**메서드**: `lockTetromino(GameState state)`

**처리 순서**:
```
1. Pivot 위치 미리 저장 (originalPivotX, originalPivotY, originalItemType)
2. WEIGHT_BOMB 처리 (Lock 전)
   - clearVerticalPath()
   - 바닥까지 재낙하
3. super.lockTetromino() 호출
   → ClassicGameEngine.lockTetrominoInternal()
   → 블록 고정, 마커 추가, 일반 라인 클리어
4. LINE_CLEAR 마커 처리 (Lock 후)
   🔥 CRITICAL: 마커가 Grid에 추가된 후 처리
   - findAndClearMarkedLines()
   - clearLines()
   - addScore(lineClearScore)
   - addLinesCleared(lineClearMarkerLines)
5. BOMB/PLUS/SPEED_RESET/BONUS_SCORE 효과 적용 (Lock 후)
   - item.apply()
   - addScore(effect.getBonusScore())
   - addLinesCleared(effect.getLinesCleared())
6. 무게추 점수 추가
7. 아이템 생성 체크
   totalLinesCleared = lastLinesCleared + lineClearMarkerLines + itemEffectLinesCleared
```

**검증 결과**:
- ✅ **LINE_CLEAR 처리 순서**: Lock 후 처리로 마커 손실 방지
- ✅ **라인 카운트 집계**: 모든 소스의 라인 클리어가 합산됨
  - `lastLinesCleared`: ClassicGameEngine의 일반 라인 클리어
  - `lineClearMarkerLines`: LINE_CLEAR 아이템
  - `itemEffectLinesCleared`: BOMB/PLUS 아이템
- ✅ **GameState 동기화**: addLinesCleared()로 레벨업 진행
- ✅ **아이템 생성 카운터**: totalLinesCleared에 모든 라인 포함

---

## 🎯 중복 처리 가능성 검토

### ❓ LINE_CLEAR 마커가 있는 줄이 꽉 찬 경우?

**시나리오**:
```
줄 19: [OOOO OOOO OL]  ← 'L' 마커 있음, 꽉 참
```

**처리 과정**:
1. `super.lockTetromino()` → ClassicGameEngine
   - 줄 19가 꽉 찼으므로 일반 라인 클리어
   - `lastLinesCleared = 1`
2. LINE_CLEAR 마커 처리
   - `findAndClearMarkedLines()` → 줄 19는 이미 제거됨
   - 마커가 없으므로 `markedLines.isEmpty()` → 처리 안 함

**결론**: ✅ **중복 처리 없음**
- LINE_CLEAR 마커가 있는 줄이 꽉 차면 일반 라인 클리어로 제거
- 이후 LINE_CLEAR 처리에서는 해당 줄이 없으므로 건너뜀
- 라인 카운트는 1번만 집계됨

### ❓ BOMB/PLUS로 제거된 블록이 다시 일반 라인 클리어로 제거되는 경우?

**시나리오**:
```
BOMB 효과로 블록 제거 → 중력 적용 → 새로 꽉 찬 줄 생김
```

**처리 과정**:
1. `super.lockTetromino()` → 일반 라인 클리어 (BOMB 효과 전)
   - `lastLinesCleared = N`
2. BOMB 아이템 효과
   - 블록 제거
   - applyGravity() → 중력 적용
   - checkAndClearLines() → **새로 생긴** 꽉 찬 줄 제거
   - `itemEffectLinesCleared = M`
3. 총 라인: `N + M`

**결론**: ✅ **중복 처리 없음**
- 일반 라인 클리어는 BOMB 효과 전에 처리
- BOMB의 라인 클리어는 중력 적용 후 새로 생긴 줄만 처리
- 서로 다른 시점의 라인 클리어이므로 중복 없음

---

## 📝 Cell 복사 방식 통일

### 수정 전 (불일치)

**ClassicGameEngine**:
```java
// ❌ 참조 복사 - 문제 있음
state.getGrid()[targetRow][col] = rowData[col];
```

**BombItem/PlusItem**:
```java
// ✅ 값 복사 - 올바름
grid[targetRow][col].setColor(rowData[col].getColor());
grid[targetRow][col].setOccupied(rowData[col].isOccupied());
grid[targetRow][col].setItemMarker(rowData[col].getItemMarker());
```

**LineClearItem**:
```java
// ✅ 값 복사 - 올바름
grid[targetRow][col].setColor(rowData[col].getColor());
grid[targetRow][col].setOccupied(rowData[col].isOccupied());
grid[targetRow][col].setItemMarker(rowData[col].getItemMarker());
```

### 수정 후 (통일)

모든 라인 클리어 로직이 **Cell 값 복사** 방식으로 통일되었습니다:
- ✅ ClassicGameEngine
- ✅ BombItem
- ✅ PlusItem
- ✅ LineClearItem

---

## 🔧 수정 사항 요약

### 수정 완료
1. ✅ **ClassicGameEngine.checkAndClearLines()** - Cell 참조 복사 → 값 복사
   - 파일: `ClassicGameEngine.java`
   - 라인: ~760
   - 변경: `state.getGrid()[targetRow][col] = rowData[col]` → `setColor/setOccupied/setItemMarker`

2. ✅ **ClassicGameEngine.checkAndClearLines()** - Cell.empty() → clear() 사용 (2025-11-27 추가)
   - 파일: `ClassicGameEngine.java`
   - 라인: ~768
   - 문제: `Cell.empty()`가 새 객체 생성하여 렌더링 시스템과 참조 불일치
   - 변경: `state.getGrid()[targetRow][col] = Cell.empty()` → `state.getGrid()[targetRow][col].clear()`
   - 효과: 기존 Cell 객체 재사용으로 렌더링 일관성 확보

### 이미 해결됨
2. ✅ **LINE_CLEAR 처리 순서** - Lock 후 처리로 마커 손실 방지
   - 파일: `ArcadeGameEngine.java`
   - 변경: Lock 전 → Lock 후
   - 이유: super.lockTetromino() 후에 마커가 Grid에 추가됨

3. ✅ **BOMB/PLUS 라인 클리어 GameState 반영**
   - 파일: `ArcadeGameEngine.java`
   - 구현: `effect.getLinesCleared()` → `addLinesCleared()`
   - 효과: 레벨업 진행 및 아이템 생성 카운터 반영

---

## ⚠️ 알려진 제약사항

### 1. BOMB/PLUS 라인 클리어는 lastLinesCleared에 포함되지 않음

**현재 동작**:
- ClassicGameEngine의 일반 라인 클리어 → `lastLinesCleared`에 저장
- BOMB/PLUS의 라인 클리어 → `addLinesCleared()`로 총 카운트에만 반영

**영향**:
- UI에서 "마지막 라인 클리어 수"를 표시할 때 BOMB/PLUS 효과는 표시 안 됨
- 통계에는 정확히 반영됨 (총 라인 카운트)

**해결 방법** (필요시):
```java
// ArcadeGameEngine.lockTetromino()에서
if (itemEffectLinesCleared > 0) {
    // lastLinesCleared에도 합산
    newState.setLastLinesCleared(
        newState.getLastLinesCleared() + itemEffectLinesCleared
    );
}
```

### 2. 아이템 효과의 라인 클리어는 T-Spin 보너스 없음

**현재 동작**:
- 일반 라인 클리어: T-Spin 감지 → 보너스 점수
- BOMB/PLUS 라인 클리어: T-Spin 보너스 없음

**의도된 설계**:
- 아이템 효과는 별도 보너스 체계 (blocksCleared × 5점)
- T-Spin 보너스는 일반 라인 클리어에만 적용

---

## 🧪 테스트 권장사항

### 필수 테스트
1. ✅ ClassicGameEngine의 라인 클리어 - Cell 값이 올바르게 복사되는가?
2. ✅ LINE_CLEAR 마커 - Lock 후 정상 처리되는가?
3. ✅ BOMB/PLUS 중력 - 블록이 아래로 정상 이동하는가?
4. ✅ BOMB/PLUS 라인 클리어 - 새로 생긴 줄이 정상 제거되는가?
5. ✅ 라인 카운트 집계 - 모든 소스가 합산되는가?

### 통합 테스트
- [ ] LINE_CLEAR 마커가 있는 줄이 꽉 찬 경우 - 중복 제거 없는가?
- [ ] BOMB 효과 후 중력으로 새 줄 생성 - 정상 클리어되는가?
- [ ] 여러 아이템이 동시에 라인을 클리어하는 경우 - 모든 카운트 정확한가?
- [ ] 렌더링 일관성 - Cell 값 복사 후 화면 표시 정확한가?

### 회귀 테스트
- [ ] 기존 테스트 케이스 모두 통과하는가?
- [ ] Perfect Clear 감지 정상 작동하는가?
- [ ] 레벨업 진행 정확한가?
- [ ] 점수 계산 중복 없는가?

---

## 📚 관련 문서

- [ITEM_SYSTEM_STATUS.md](ITEM_SYSTEM_STATUS.md) - 아이템 시스템 전체 상태
- [QA_BUG_REPORT_ITEM_SYSTEM.md](document/QA_BUG_REPORT_ITEM_SYSTEM.md) - QA 버그 리포트

---

## 👥 검토자

- **코드 분석**: AI Assistant (GitHub Copilot)
- **수정 사항**: ClassicGameEngine Cell 복사 방식 변경
- **최종 검수**: 개발팀 검토 필요

---

**⚠️ 중요**: 이 보고서는 2025년 11월 27일 기준 코드 상태를 반영합니다.  
코드 변경 시 이 문서도 함께 업데이트하세요!
