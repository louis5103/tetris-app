# Arcade 모드 아이템 생성 오류 수정 보고서

## 문제 상황
Arcade 모드에서 아이템이 생성되지 않는 오류가 발생했습니다.

## 원인 분석

### 근본 원인
`GameModeConfig` 클래스의 `enabledItemTypes` 필드에 `@Builder.Default` 어노테이션과 함께 `Collections.emptySet()`이 기본값으로 설정되어 있었습니다.

```java
// 문제가 있는 코드 (수정 전)
@Builder.Default
private final Set<ItemType> enabledItemTypes = Collections.emptySet();
```

Lombok의 `@Builder.Default` 어노테이션은 빌더 패턴 사용 시 명시적으로 값을 설정하지 않으면 기본값을 사용합니다. 그런데 이 경우, `Collections.emptySet()`이 불변 빈 Set을 반환하므로, 빌더에서 `enabledItemTypes`를 설정해도 제대로 동작하지 않을 수 있습니다.

### 영향 범위
- `GameModeConfigFactory.createArcadeConfig()`: YML에서 읽은 아이템 타입들이 Config에 설정되지만, `enabledItemTypes`가 빈 Set으로 유지됨
- `ArcadeGameEngine`: `config.isItemSystemEnabled()`가 false를 반환하여 ItemManager가 생성되지 않음
- 결과: Arcade 모드에서 아이템이 전혀 생성되지 않음

## 수정 내용

### 1. GameModeConfig.java 수정
**파일**: `/tetris-core/src/main/java/seoultech/se/core/config/GameModeConfig.java`

#### 변경사항 1: `@Builder.Default` 제거
```java
// 수정 후
/**
 * 활성화된 아이템 타입 목록
 * arcade.yml의 enabledItems와 매핑
 * Note: Builder에서 null을 전달하면 빈 Set으로 초기화됨
 */
private final Set<ItemType> enabledItemTypes;
```

#### 변경사항 2: Null-safe getter 추가
```java
/**
 * 활성화된 아이템 타입 목록 반환 (Null-safe)
 * Lombok의 기본 getter를 오버라이드하여 null 체크 추가
 * 
 * @return 활성화된 아이템 타입 집합 (null인 경우 빈 집합 반환)
 */
public Set<ItemType> getEnabledItemTypes() {
    return enabledItemTypes != null ? enabledItemTypes : Collections.emptySet();
}
```

#### 변경사항 3: `isItemSystemEnabled()` 메서드 개선
```java
/**
 * 아이템 시스템 활성화 여부 확인
 * 
 * @return 아이템 시스템이 활성화되어 있으면 true
 */
public boolean isItemSystemEnabled() {
    return linesPerItem > 0 && getEnabledItemTypes() != null && !getEnabledItemTypes().isEmpty();
}
```

### 2. 테스트 코드 추가
**파일**: `/tetris-core/src/test/java/seoultech/se/core/config/GameModeConfigItemTest.java`

새로운 테스트 파일을 생성하여 다음 케이스들을 검증:
- ✅ Arcade 모드에서 아이템 타입이 정상적으로 설정됨
- ✅ null enabledItemTypes는 빈 Set으로 처리됨
- ✅ 빈 enabledItemTypes는 아이템 시스템 비활성화
- ✅ linesPerItem이 0이면 아이템 시스템 비활성화
- ✅ `createDefaultArcade()` 헬퍼 메서드의 아이템 시스템 활성화 확인

## 검증 결과

### 1. 단위 테스트
```bash
./gradlew :tetris-core:test --tests "GameModeConfigItemTest"
```
**결과**: ✅ 모든 테스트 통과 (5/5)

### 2. 기존 테스트 호환성
```bash
./gradlew :tetris-core:test
```
**결과**: ✅ 모든 기존 테스트 통과

### 3. 빌드 검증
```bash
./gradlew :tetris-core:build -x test
./gradlew :tetris-client:build -x test
```
**결과**: ✅ 빌드 성공

## 동작 확인

수정 후 Arcade 모드에서 다음과 같이 동작합니다:

1. **설정 로드**:
   - `game-modes.yml`에서 `linesPerItem: 1` 및 `enabledTypes` 읽기
   - `GameModeConfigFactory.createArcadeConfig()`에서 Config 생성
   - `enabledItemTypes`가 올바르게 설정됨

2. **엔진 초기화**:
   - `ArcadeGameEngine` 생성자에서 `config.isItemSystemEnabled()` 체크
   - true 반환 → `ItemManager` 정상 생성
   - 로그: "✅ [Engine] ArcadeGameEngine initialized - Items enabled (6 types, 1 lines per item)"

3. **아이템 생성**:
   - 1줄 클리어 시마다 아이템 생성
   - 활성화된 6개 아이템 타입 중 랜덤 선택
   - 로그: "[Item] Generated: {아이템명} (after 1 lines)"

## 기술적 세부사항

### Lombok @Builder.Default 이슈
Lombok의 `@Builder.Default`는 컬렉션 필드에 사용할 때 주의가 필요합니다:
- 불변 컬렉션(`Collections.emptySet()`)을 기본값으로 사용하면 빌더에서 설정한 값이 무시될 수 있음
- 해결책: `@Builder.Default` 제거하고 null-safe getter로 처리

### Null Safety 전략
- 필드는 nullable로 유지 (`enabledItemTypes`)
- Getter에서 null을 빈 Set으로 변환 (`getEnabledItemTypes()`)
- 비즈니스 로직에서 안전하게 처리 (`isItemSystemEnabled()`)

## 영향받는 컴포넌트
- ✅ `GameModeConfig`: 수정됨
- ✅ `GameModeConfigFactory`: 변경 없음 (기존 로직 정상 동작)
- ✅ `ArcadeGameEngine`: 변경 없음 (Config 수정으로 문제 해결)
- ✅ `ItemManager`: 변경 없음

## 결론
`GameModeConfig`의 `enabledItemTypes` 필드에서 `@Builder.Default` 어노테이션을 제거하고, null-safe getter를 추가함으로써 Arcade 모드에서 아이템이 정상적으로 생성되도록 수정했습니다. 모든 테스트가 통과하며, 기존 코드와의 호환성도 유지됩니다.

---
**수정일**: 2025년 11월 30일  
**수정자**: GitHub Copilot  
**검증**: 단위 테스트 + 통합 테스트 + 빌드 검증
