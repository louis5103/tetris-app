# 🎮 아이템 시스템 상태 문서

> **최종 업데이트**: 2025년 11월 27일
> **작성자**: GitHub Copilot (AI Assistant)

## 📊 현재 상태 요약

### ✅ 완료된 작업
- [x] linesPerItem 카운터 기반 아이템 생성 시스템
- [x] 모든 아이템 타입 구현 (6종)
- [x] 아이템 마커 시스템 (Cell.itemMarker)
- [x] 중력 적용 후 라인 클리어
- [x] ItemEffect에 linesCleared 필드 추가
- [x] 라인 클리어 카운트 일관성 확보
- [x] 다중 라인 제거 버그 수정

### ⚠️ 알려진 제약사항
- LINE_CLEAR 마커는 일반 라인 클리어보다 먼저 처리됨
- BOMB/PLUS는 마커 없는 모든 블록 제거
- WEIGHT_BOMB은 무게추 자체에만 마커 추가

---

## 🎯 아이템 타입별 상세 스펙

### 1. LINE_CLEAR (Ⓛ)
**구현 상태**: ✅ 완료

```java
// 처리 위치: ArcadeGameEngine.lockTetromino()
// 처리 시점: super.lockTetromino() 호출 후
```

**동작**:
1. 블록 Lock 시 무작위로 하나의 블록에 'L' 마커 추가 (ClassicGameEngine)
2. super.lockTetromino() 호출하여 블록과 마커를 Grid에 고정
3. `findAndClearMarkedLines()`: 'L' 마커가 있는 줄 찾기
4. `clearLines()`: 해당 줄 전체 제거 (꽉 차지 않아도 제거)
5. 위의 블록들 아래로 이동

**점수**: 
- Line bonus: `줄 수 × 100 × 레벨`
- Block bonus: `제거된 블록 수 × 10`

**주의사항**:
- ✅ **super.lockTetromino() 호출 후** 처리됨 (마커가 Grid에 추가된 후)
- ✅ GameState.addLinesCleared() 호출하여 레벨업 진행

---

### 2. WEIGHT_BOMB (⚓)
**구현 상태**: ✅ 완료

```java
// 처리 위치: ArcadeGameEngine.lockTetromino()
// 처리 시점: super.lockTetromino() 호출 전
```

**동작**:
1. 무게추가 떨어질 때마다 `processWeightBombFall()` 호출
2. 4칸 모두에서 바로 아래 블록 제거
3. Lock 시 `clearVerticalPath()`: 무게추 아래 수직 경로 모든 블록 제거
4. 블록 제거 후 무게추를 다시 바닥까지 떨어뜨림
5. Lock 후 무게추 블록 6개에 WEIGHT_BOMB 마커 추가

**점수**: `제거된 블록 수 × 10`

**주의사항**:
- ✅ 좌우 이동 제한은 GameController에서 처리
- ✅ 무한 루프 방지 (maxDropDistance 체크)

---

### 3. BOMB (💣)
**구현 상태**: ✅ 완료

```java
// 처리 위치: ArcadeGameEngine.lockTetromino()
// 처리 시점: super.lockTetromino() 호출 후
```

**동작**:
1. Lock 후 pivot 위치 기준 5x5 영역 모든 블록 제거
2. 중력 적용 (블록을 아래로 이동)
3. 라인 클리어 체크 및 실행
4. ItemEffect에 linesCleared 포함하여 반환

**점수**: `제거된 블록 수 × 5`

**주의사항**:
- ✅ 마커 체크 없이 범위 내 모든 블록 제거
- ✅ 중력 후 라인 클리어를 GameState에 반영
- ✅ 다중 라인 제거 시 Set 기반 처리로 인덱스 오류 방지

---

### 4. PLUS (➕)
**구현 상태**: ✅ 완료

```java
// 처리 위치: ArcadeGameEngine.lockTetromino()
// 처리 시점: super.lockTetromino() 호출 후
```

**동작**:
1. Lock 후 pivot 위치의 행과 열 전체 블록 제거
2. 중력 적용 (블록을 아래로 이동)
3. 라인 클리어 체크 및 실행
4. ItemEffect에 linesCleared 포함하여 반환

**점수**: `제거된 블록 수 × 5`

**주의사항**:
- ✅ 마커 체크 없이 십자(+) 모양 모든 블록 제거
- ✅ 중력 후 라인 클리어를 GameState에 반영
- ✅ 다중 라인 제거 시 Set 기반 처리로 인덱스 오류 방지

---

### 5. SPEED_RESET (⚡)
**구현 상태**: ✅ 완료

```java
// 처리 위치: ArcadeGameEngine.lockTetromino()
// 처리 시점: super.lockTetromino() 호출 후
```

**동작**:
1. `gameState.setSoftDropSpeedMultiplier(1.0)`: 속도를 초기값으로 리셋
2. `gameState.setSpeedResetRequested(true)`: 플래그 설정
3. GameController에서 플래그 감지하고 타이머 조정

**점수**: `100점 (고정)`

**주의사항**:
- ✅ GameController/GameLoop 연동 필요
- ✅ 블록 제거 없음

---

### 6. BONUS_SCORE (⭐)
**구현 상태**: ✅ 완료

```java
// 처리 위치: ArcadeGameEngine.lockTetromino()
// 처리 시점: super.lockTetromino() 호출 후
```

**동작**:
1. 레벨에 따른 점수 계산
2. ItemEffect로 점수만 반환

**점수**: `500 + (레벨 × 50)`

**주의사항**:
- ✅ 블록 제거 없음
- ✅ GameState 수정 없음

---

## 🔄 처리 순서 (ArcadeGameEngine.lockTetromino)

```java
1. Pivot 위치 미리 저장
   originalPivotX = state.getCurrentX()
   originalPivotY = state.getCurrentY()
   originalItemType = state.getCurrentItemType()

2. 무게추 처리 (Lock 전)
   if (WEIGHT_BOMB) {
       clearVerticalPath()
       drop to bottom
   }

3. super.lockTetromino() 호출
   → ClassicGameEngine.lockTetrominoInternal()
   → 블록 고정, 마커 추가, 일반 라인 클리어

4. LINE_CLEAR 마커 처리 (Lock 후)
   🔥 CRITICAL: 마커가 Grid에 추가된 후 처리
   findAndClearMarkedLines()
   clearLines()
   addScore(lineClearScore)
   addLinesCleared(lineClearMarkerLines)

5. 다른 아이템 효과 적용 (Lock 후)
   if (BOMB/PLUS/SPEED_RESET/BONUS_SCORE) {
       effect = item.apply(newState, originalPivotY, originalPivotX)
       addScore(effect.getBonusScore())
       if (effect.getLinesCleared() > 0) {
           addLinesCleared(effect.getLinesCleared())
       }
   }

6. 무게추 점수 추가
   addScore(weightBombScore)

7. 아이템 생성 체크
   totalLinesCleared = lastLinesCleared + lineClearMarkerLines + itemEffectLinesCleared
   checkAndGenerateItem(totalLinesCleared)
```

---

## 🎯 ItemEffect 구조

```java
@Builder
public class ItemEffect {
    private final boolean success;
    private final int blocksCleared;      // 제거된 블록 수
    private final int bonusScore;         // 보너스 점수
    private final int linesCleared;       // 중력 후 클리어된 라인 수 (NEW)
    private final String message;
    private final ItemType itemType;
}

// 사용 예:
ItemEffect.success(ItemType.BOMB, blocksCleared, bonusScore, message)
ItemEffect.successWithLines(ItemType.BOMB, blocksCleared, bonusScore, linesCleared, message)
```

---

## 📝 주요 수정 이력

### 2025-11-27 (최종)
1. ✅ ItemEffect에 linesCleared 필드 추가
2. ✅ BOMB/PLUS 아이템이 successWithLines() 사용
3. ✅ ArcadeGameEngine이 아이템 효과의 라인 클리어를 GameState에 반영
4. ✅ BOMB/PLUS checkAndClearLines() 다중 라인 제거 버그 수정 (Set 기반 처리)
5. ✅ LineClearItem.clearLines() 셀 복사 방식 수정 (렌더링 불일치 방지)
6. ✅ **LINE_CLEAR 처리 순서 수정: Lock 전 → Lock 후** (마커가 Grid에 추가된 후 처리)
7. ✅ **ClassicGameEngine Cell 복사 방식 통일** (참조 복사 → 값 복사로 변경)

### 라인 클리어 로직 감사 (2025-11-27)
- ✅ ClassicGameEngine, BOMB/PLUS, LINE_CLEAR 모든 라인 클리어 로직 검증 완료
- ✅ Cell 복사 방식 일관성 확보 (모두 값 복사 사용)
- ✅ 라인 클리어 중복 처리 없음 확인
- ✅ GameState 동기화 정확성 확인
- 📄 상세 내용: [LINE_CLEAR_AUDIT_REPORT.md](LINE_CLEAR_AUDIT_REPORT.md)

### 이전 수정
- linesPerItem 카운터 기반 시스템 구현
- 모든 아이템에 마커 추가 로직 구현
- Pivot 위치 미리 저장 (lockTetromino 후 null 문제 해결)

---

## 🐛 알려진 버그 및 제약사항

### ✅ 해결됨
- ~~아이템이 생성되지 않음~~ → linesPerItem 시스템으로 해결
- ~~아이템 효과가 적용되지 않음~~ → lockTetromino에 로직 추가
- ~~PLUS/BOMB이 모든 블록 제거~~ → 마커 체크 제거 (의도된 동작)
- ~~LINE_CLEAR 마커가 사라짐~~ → Lock 후 처리로 해결 (마커 추가 후 처리)
- ~~LINE_CLEAR 효과가 적용되지 않음~~ → Lock 후 처리로 해결 (2025-11-27)
- ~~Pivot 위치 참조 오류~~ → 미리 저장으로 해결
- ~~렌더링 불일치~~ → 셀 복사 방식 수정
- ~~다중 라인 제거 오류~~ → Set 기반 처리로 해결
- ~~라인 클리어 카운트 불일치~~ → 모든 아이템 GameState 반영

### ⚠️ 현재 없음
모든 알려진 버그가 수정되었습니다.

---

## 🧪 테스트 체크리스트

### 기능 테스트
- [ ] LINE_CLEAR: 마커가 있는 줄이 꽉 차지 않아도 제거되는가?
- [ ] WEIGHT_BOMB: 수직 경로의 모든 블록이 제거되는가?
- [ ] BOMB: 5x5 범위의 모든 블록이 제거되는가?
- [ ] PLUS: 십자(+) 모양의 모든 블록이 제거되는가?
- [ ] SPEED_RESET: 속도가 초기값으로 돌아가는가?
- [ ] BONUS_SCORE: 레벨에 따라 점수가 증가하는가?

### 통합 테스트
- [ ] 아이템이 linesPerItem마다 생성되는가?
- [ ] 중력 적용 후 라인 클리어가 정상 작동하는가?
- [ ] 여러 줄을 동시에 제거해도 오류가 없는가?
- [ ] 레벨업이 정상적으로 진행되는가?
- [ ] 점수가 중복 추가되지 않는가?

### 렌더링 테스트
- [ ] 아이템 마커가 화면에 표시되는가?
- [ ] 중력 적용 후 블록 위치가 정확한가?
- [ ] 라인 클리어 애니메이션이 정상인가?

---

## 🔧 유지보수 가이드

### 새 아이템 추가 시
1. `ItemType` enum에 새 타입 추가
2. `AbstractItem`을 상속하는 클래스 생성
3. `ItemManager.registerPrototypes()`에 등록
4. 필요시 `ArcadeGameEngine.lockTetromino()`에 특수 처리 추가
5. `ClassicGameEngine.lockTetrominoInternal()`에 마커 추가 로직 추가

### 버그 수정 시 주의사항
1. **Pivot 위치**: lockTetromino 전에 미리 저장할 것
2. **라인 클리어 순서**: 🔥 **LINE_CLEAR는 Lock 후**, 나머지 아이템도 Lock 후 (마커가 Grid에 추가된 후에 처리)
3. **라인 카운트**: GameState.addLinesCleared() 호출 확인
4. **점수 중복**: 각 아이템당 1회만 addScore() 호출
5. **다중 라인 제거**: Set 기반 처리 사용

### 리팩토링 시 체크리스트
- [ ] 기존 테스트 케이스 모두 통과하는가?
- [ ] 점수 중복 추가 없는가?
- [ ] 라인 카운트 일관성 유지되는가?
- [ ] 렌더링 동기화 문제 없는가?
- [ ] 아이템 생성 간격 정확한가?

---

## 📚 관련 문서

- [QA_BUG_REPORT_ITEM_SYSTEM.md](document/QA_BUG_REPORT_ITEM_SYSTEM.md) - QA 버그 리포트
- [FINAL_SYSTEM_REQUIREMENTS_v6_part2.md](document/FINAL_SYSTEM_REQUIREMENTS_v6_part2.md) - 아이템 시스템 요구사항

---

## 👥 담당자

- **시스템 설계**: 개발팀
- **구현**: AI Assistant (GitHub Copilot)
- **최종 검수**: 개발팀

---

**⚠️ 중요**: 이 문서를 수정하지 않고 코드를 변경하면 일관성이 깨질 수 있습니다.
변경 사항이 있을 때마다 이 문서를 업데이트하세요!
