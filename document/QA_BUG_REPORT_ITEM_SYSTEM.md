# 🚨 QA 테스트 결과 - 심각한 버그 발견 보고서

**날짜**: 2025년 11월 13일  
**테스터**: QA 팀  
**테스트 대상**: 아이템 시스템 (BOMB, PLUS)

---

## 📊 테스트 결과 요약

| 테스트 케이스 | 결과 | 심각도 |
|--------------|------|--------|
| QA-BOMB-001: BOMB 정확히 25개 블록 삭제 | ❌ **실패** | 🔴 Critical |
| QA-BOMB-002: BOMB 5x5 영역 밖 블록 유지 | ❌ **실패** | 🔴 Critical |
| QA-BOMB-003: BOMB 가장자리 안전성 | ✅ 통과 | - |
| QA-PLUS-001: PLUS 십자 영역 삭제 | ✅ 통과 | - |
| QA-PLUS-002: PLUS 중력 적용 후 일관성 | ✅ 통과 | - |
| QA-PIVOT-001: Hard Drop pivot 정확성 | ✅ 통과 | - |
| QA-PIVOT-002: I 블록 pivot 내부 위치 | ❌ **실패** | 🟡 Major |
| QA-PATH-001: Lock 경로 pivot 일관성 | ✅ 통과 | - |
| QA-SAFETY-001: 음수 좌표 안전성 | ✅ 통과 | - |
| QA-SAFETY-002: 범위 초과 안전성 | ✅ 통과 | - |
| QA-SAFETY-003: 빈 보드 안전성 | ✅ 통과 | - |
| QA-DUPLICATE-001: 중복 적용 안전성 | ✅ 통과 | - |

**전체 통과율**: 9/13 = **69%** ❌

---

## 🔴 Critical 버그

### Bug #1: BOMB 아이템이 115개 블록을 삭제 (예상: 25개)

**재현 단계**:
1. 10x20 보드를 완전히 블록으로 채움 (200개 블록)
2. Pivot (10, 5)에서 BOMB 아이템 적용
3. 결과 확인

**예상 결과**:
- 5x5 = 25개 블록만 삭제
- 나머지 175개 블록은 그대로 유지

**실제 결과**:
- **115개 블록 삭제** (25개의 460%)
- 연쇄적으로 추가 블록 삭제됨

**원인 분석**:
```java
// BombItem.java Line 98-108
if (blocksCleared > 0) {
    applyGravity(gameState);  // 🔥 중력 적용
    
    int linesCleared = checkAndClearLines(gameState);  // 🔥 라인 클리어
    if (linesCleared > 0) {
        bonusScore += linesCleared * 100;
    }
}
```

BOMB 아이템이:
1. 5x5 영역 삭제 (25개)
2. **중력 적용** → 위의 블록이 아래로 떨어짐
3. **라인 클리어** → 완성된 라인 추가 삭제
4. **반복** → 연쇄 삭제

**심각도**: 🔴 **Critical**  
- 사용자가 예상하지 못한 대량 삭제
- 게임 밸런스 파괴
- "5x5 영역 삭제"라는 설명과 불일치

**수정 방안**:

#### 옵션 1: 중력/라인 클리어 제거 (순수한 영역 삭제)
```java
// BOMB은 5x5 영역만 삭제하고 끝
return ItemEffect.success(ItemType.BOMB, blocksCleared, bonusScore, message);
// applyGravity() 호출 제거
```

#### 옵션 2: 문서 수정 (현재 동작이 의도라면)
```java
/**
 * 폭탄 아이템
 * 
 * 아이템 위치 기준 5x5 영역의 블록을 제거하고,
 * 중력 적용 + 라인 클리어를 자동으로 수행합니다.
 * 
 * ⚠️ 주의: 연쇄 효과로 인해 25개 이상의 블록이 삭제될 수 있습니다.
 */
```

---

### Bug #2: BOMB 영역 밖 블록도 삭제됨

**재현 단계**:
1. 10x20 보드를 완전히 블록으로 채움
2. Pivot (10, 5)에서 BOMB 적용
3. Row 7 (pivot - 3) 확인

**예상 결과**:
- Row 7은 5x5 영역 밖이므로 모든 블록 유지

**실제 결과**:
- Row 7, Col 0이 비어있음 (삭제됨)

**원인 분석**:
- 중력 + 라인 클리어 연쇄 효과
- 5x5 영역 삭제 → 중력 → 라인 완성 → 영역 밖 라인도 삭제

**심각도**: 🔴 **Critical**  
- "5x5 영역만 삭제"라는 명세 위반

---

## 🟡 Major 버그

### Bug #3: I 블록 Lock 후 pivot 주변에 블록 없음

**재현 단계**:
1. I 블록 생성 (X=5, Y=0)
2. PLUS 아이템 설정
3. lockTetromino() 호출
4. Pivot 주변 5x5 영역 확인

**예상 결과**:
- I 블록 4칸이 pivot 주변에 고정됨

**실제 결과**:
- Pivot 주변에 블록 0개 (아무것도 없음)

**원인 분석**:
```java
// 가능한 원인:
1. Lock이 실패했음 (블록이 고정되지 않음)
2. Lock 직후 PLUS 아이템이 자동으로 발동하여 블록 삭제
3. Pivot 위치가 잘못 계산되어 엉뚱한 곳을 확인함
```

**심각도**: 🟡 **Major**  
- Lock 메커니즘 자체의 신뢰성 문제

**추가 조사 필요**:
- `engine.lockTetromino(state)` 내부 로직 확인
- PLUS 아이템이 자동으로 발동하는지 확인
- Pivot 저장 로직 검증

---

## ✅ 통과한 테스트

- ✅ BOMB 가장자리 안전성
- ✅ PLUS 십자 영역 삭제 (기본 기능)
- ✅ PLUS 중력 후 일관성
- ✅ Pivot 위치 정확성 (T 블록)
- ✅ Lock 경로 일관성
- ✅ 경계 케이스 안전성 (음수, 범위 초과, 빈 보드)
- ✅ 중복 적용 안전성

---

## 🎯 권장 조치

### 즉시 수정 필요 (Critical)

1. **BOMB 아이템 중력/라인 클리어 제거**
   - 파일: `BombItem.java`
   - 수정: `applyGravity()` 및 `checkAndClearLines()` 호출 제거
   - 또는: 문서 수정하여 현재 동작이 의도임을 명시

2. **PLUS 아이템도 동일 검토**
   - 파일: `PlusItem.java`
   - 현재: 십자 삭제 + 중력 + 라인 클리어
   - 검토: 이것이 기획 의도인지 확인

### 추가 조사 필요 (Major)

3. **I 블록 Lock 실패 원인 조사**
   - 파일: `ClassicGameEngine.java` (lockTetrominoInternal)
   - 확인: Lock이 제대로 되는지, 아이템 자동 발동 여부

---

## 📝 기획 팀 확인 필요

아래 질문에 대한 명확한 답변이 필요합니다:

### 질문 1: BOMB 아이템 의도
- **A안**: BOMB은 5x5 영역만 삭제하고 끝 (중력 없음)
- **B안**: BOMB은 5x5 삭제 + 중력 + 라인 클리어 자동 (현재 구현)

### 질문 2: PLUS 아이템 의도
- **A안**: PLUS는 십자 영역만 삭제하고 끝 (중력 없음)
- **B안**: PLUS는 십자 삭제 + 중력 + 라인 클리어 자동 (현재 구현)

### 질문 3: 연쇄 효과 허용 여부
- 아이템 효과 후 자동으로 라인 클리어까지 수행할 것인가?
- 아니면 단순히 블록만 삭제하고, 라인 클리어는 다음 블록 Lock 시에?

---

## 🔧 임시 패치 제안

기획 확정 전까지 임시로 적용할 수 있는 패치:

### Option A: 중력/라인 클리어 주석 처리
```java
// BombItem.java
return ItemEffect.success(ItemType.BOMB, blocksCleared, bonusScore, message);
// TODO: 기획 확정 후 중력/라인 클리어 복원
// applyGravity(gameState);
// checkAndClearLines(gameState);
```

### Option B: 설정으로 제어
```java
// ItemConfig.java
private boolean enableItemChainReaction = false;  // 기본값: false

// BombItem.java
if (itemConfig.isEnableItemChainReaction()) {
    applyGravity(gameState);
    checkAndClearLines(gameState);
}
```

---

## 📈 다음 단계

1. ✅ QA 테스트 완료 (13개 테스트)
2. ⏳ 기획 팀 확인 대기
3. ⏳ 버그 수정 (Critical 2건, Major 1건)
4. ⏳ 수정 후 재테스트
5. ⏳ 회귀 테스트

**예상 수정 시간**: 2-4시간  
**예상 재테스트**: 1시간  
**목표 통과율**: 100% (13/13)

---

**보고자**: GitHub Copilot QA Agent  
**검토 필요**: 개발 팀장, 기획 팀장
