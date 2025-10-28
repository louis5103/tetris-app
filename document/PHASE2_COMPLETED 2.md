# ✅ Phase 2 완료 보고서

## 📋 완료된 작업

### 1. GameEngine.java 수정
**파일**: `tetris-core/src/main/java/seoultech/se/core/GameEngine.java`

#### 변경 내용:

**1) LineClearInfo 내부 클래스 제거**
- 기존: `LineClearInfo` 내부 클래스로 라인 클리어 정보 반환
- 변경: GameState에 직접 저장 (반환값 없음)

```java
// 제거됨
private static class LineClearInfo {
    final int linesCleared;
    final long scoreEarned;
    final boolean isTSpin;
    final boolean isPerfectClear;
}
```

**2) checkAndClearLines() 메서드 시그니처 변경**
```java
// 변경 전
private static LineClearInfo checkAndClearLines(GameState state, boolean isTSpin, boolean isTSpinMini)

// 변경 후
private static void checkAndClearLines(GameState state, boolean isTSpin, boolean isTSpinMini)
```

**3) checkAndClearLines() 메서드 구현 변경**
- 라인 클리어 정보를 GameState에 직접 저장:
  - `state.setLastLinesCleared(linesCleared)`
  - `state.setLastClearedRows(clearedRowsArray)`
  - `state.setLastScoreEarned(score)`
  - `state.setLastIsPerfectClear(isPerfectClear)`

```java
private static void checkAndClearLines(GameState state, boolean isTSpin, boolean isTSpinMini) {
    // ... 라인 클리어 로직 ...
    
    if (clearedRowsList.isEmpty()){
        // 라인 클리어 없음 - GameState에 기본값 저장
        state.setLastLinesCleared(0);
        state.setLastClearedRows(new int[0]);
        state.setLastScoreEarned(0);
        state.setLastIsPerfectClear(false);
        return;
    }
    
    // ... 라인 클리어 실행 ...
    
    // Phase 2: GameState에 라인 클리어 정보 직접 저장
    state.setLastLinesCleared(linesCleared);
    state.setLastClearedRows(clearedRowsArray);
    state.setLastScoreEarned(score);
    state.setLastIsPerfectClear(isPerfectClear);
}
```

**4) lockTetrominoInternal() 메서드 수정**
- LineClearResult 대신 GameState에서 직접 정보 읽기

```java
// 변경 전
LineClearResult clearResult = checkAndClearLines(newState, isTSpin, isTSpinMini);
if(clearResult.getLinesCleared() > 0) {
    newState.addScore(clearResult.getScoreEarned());
    leveledUp = newState.addLinesCleared(clearResult.getLinesCleared());
    // ...
}

// 변경 후
checkAndClearLines(newState, isTSpin, isTSpinMini);
if(newState.getLastLinesCleared() > 0) {
    newState.addScore(newState.getLastScoreEarned());
    leveledUp = newState.addLinesCleared(newState.getLastLinesCleared());
    // ...
}
```

---

## 🎯 핵심 개선 사항

### Before (Phase 1)
```
checkAndClearLines() 
    ↓
LineClearInfo 객체 반환
    ↓
clearResult.getLinesCleared()
clearResult.getScoreEarned()
    ↓
GameState에 저장
```

### After (Phase 2)
```
checkAndClearLines()
    ↓
GameState에 직접 저장
    ↓
newState.getLastLinesCleared()
newState.getLastScoreEarned()
```

**장점:**
- ✅ 중간 객체 제거 → 메모리 효율 향상
- ✅ 단순한 데이터 흐름 → 디버깅 용이
- ✅ GameState 중심 아키텍처 강화

---

## 💬 사용자 질문에 대한 답변

### Q: "result를 GameState로 반환하면, 중요 UI 이벤트는 어떻게 전송되나요?"

### A: GameState 비교를 통한 UI 힌트 추출

Phase 3에서 구현될 `GameController.showUiHints()` 메서드가 이전/이후 GameState를 비교하여 UI 알림을 추출합니다.

```java
// GameController.java (Phase 3에서 구현)
private void showUiHints(GameState oldState, GameState newState) {
    // 라인 클리어 감지
    if (newState.getLinesCleared() > oldState.getLinesCleared()) {
        int cleared = newState.getLinesCleared() - oldState.getLinesCleared();
        
        // GameState에 저장된 T-Spin 메타데이터 사용
        if (newState.isLastLockWasTSpin()) {
            if (newState.isLastLockWasTSpinMini()) {
                notificationManager.showLineClearType("T-SPIN MINI!");
            } else {
                notificationManager.showLineClearType("T-SPIN!");
            }
        } else if (cleared == 4) {
            notificationManager.showLineClearType("TETRIS!");
        }
    }
    
    // 콤보 감지
    if (newState.getComboCount() > oldState.getComboCount()) {
        notificationManager.showCombo(newState.getComboCount());
    }
    
    // B2B 감지
    if (newState.getBackToBackCount() > oldState.getBackToBackCount()) {
        notificationManager.showBackToBack(newState.getBackToBackCount());
    }
    
    // 레벨업 감지
    if (newState.getLevel() > oldState.getLevel()) {
        notificationManager.showLevelUp(newState.getLevel());
    }
    
    // Perfect Clear 감지
    if (newState.getLastIsPerfectClear()) {
        notificationManager.showPerfectClear();
    }
}
```

**핵심 원리:**
1. **이벤트 전송 없음** - UI 이벤트를 별도로 전송하지 않음
2. **상태 비교** - oldState vs newState 비교로 변화 감지
3. **자동 추출** - GameState의 메타데이터만으로 모든 UI 힌트 추출 가능

**GameState에 저장된 메타데이터:**
- `lastLinesCleared` - 마지막 클리어된 라인 수
- `lastScoreEarned` - 마지막 획득 점수
- `lastIsPerfectClear` - Perfect Clear 여부
- `lastLockWasTSpin` - T-Spin 여부
- `lastLockWasTSpinMini` - T-Spin Mini 여부
- `comboCount` - 현재 콤보 수
- `backToBackCount` - 현재 B2B 수
- `level` - 현재 레벨

---

## ⚠️ 주의사항

### 현재 상태
- ✅ GameEngine.java 수정 완료
- ⏳ BoardController.java는 아직 LockResult 사용 중
- ⏳ SingleMode.java는 아직 LineClearResult 사용 중

### 다음 단계 (Phase 3)
Phase 3에서 다음 작업을 수행하여 컴파일 완료:
1. BoardController에서 LockResult 제거
2. Observer 패턴 제거
3. GameController 리팩토링
4. EventMapper 삭제

**Phase 2는 GameEngine만 수정하는 것이 목표이며, 다른 파일은 Phase 3에서 수정합니다.**

---

## 📊 변경 통계

### 삭제된 코드
- LineClearInfo 내부 클래스: 약 20줄

### 수정된 메서드
- `checkAndClearLines()`: 반환 타입 변경 및 구현 수정
- `lockTetrominoInternal()`: LineClearResult → GameState 직접 접근

### 코드 라인 수 변화
- 삭제: ~20줄
- 수정: ~10줄
- **순감소: ~10줄**

---

## ✅ 완료 체크리스트

- [x] LineClearInfo 내부 클래스 제거
- [x] checkAndClearLines() 반환 타입을 void로 변경
- [x] checkAndClearLines()가 GameState에 직접 저장하도록 구현
- [x] lockTetrominoInternal()에서 GameState 직접 접근으로 변경
- [x] 사용자 질문에 대한 답변 작성

---

## 🎯 다음 단계

Phase 3으로 진행하여:
1. Observer 패턴 제거
2. BoardController 간소화
3. GameController 리팩토링
4. EventMapper 삭제

이를 통해 완전한 GameState 중심 아키텍처가 완성됩니다!
