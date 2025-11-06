# FINAL_SYSTEM_REQUIREMENTS

**프로젝트**: Tetris Multi-Module Architecture  
**버전**: 5.0 (Final - 분석 결과 반영)  
**작성일**: 2025-11-06  
**최종 업데이트**: 2025-11-06  
**목적**: Spring Boot 기반 아키텍처 구축 및 디자인 패턴 적용

---

## 📋 목차

1. [시스템 요구사항 (Requirements)](#1-시스템-요구사항-requirements)
2. [변경 파일 목록 (Change List)](#2-변경-파일-목록-change-list)
3. [아키텍처 설계 (Architecture)](#3-아키텍처-설계-architecture)
4. [디자인 패턴 적용 (Design Patterns)](#4-디자인-패턴-적용-design-patterns)
5. [멀티플레이어 통신 (Multiplayer)](#5-멀티플레이어-통신-multiplayer)
6. [UI 이벤트 시스템 (UI Events)](#6-ui-이벤트-시스템-ui-events)
7. [모듈별 상세 구현 (Implementation)](#7-모듈별-상세-구현-implementation)
8. [Spring Boot 설정 (Configuration)](#8-spring-boot-설정-configuration)
9. [검증 체크리스트 (Verification)](#9-검증-체크리스트-verification)

---

## 1. 시스템 요구사항 (Requirements)

### 1.1 기술 스택
- **언어**: Java 21
- **프레임워크**: Spring Boot 3.x (Client/Backend)
- **빌드 도구**: Gradle 8.x
- **의존성 주입**: Spring DI (@Autowired, @Configuration)
- **아키텍처**: Multi-Module (tetris-core, tetris-client, tetris-backend)
- **통신**: REST API (Command/Response), WebSocket (Server Push)
- **동시성**: java.util.concurrent (AtomicBoolean, AtomicInteger, ConcurrentHashMap)
- **보안**: Spring Security, JWT
- **모니터링**: Micrometer, Prometheus, Grafana

### 1.2 모듈 구조 원칙

#### M-1: tetris-core (Core Game Logic)
- **M-1.1**: Spring 의존성 포함 가능 (설정 로드 제외)
- **M-1.2**: 게임 로직만 포함 (UI, 네트워크 제외)
- **M-1.3**: Spring Bean으로 등록 가능
- **M-1.4**: application.yml은 Client에서만 로드
- **M-1.5**: ✅ **GameEngine은 Interface로 구현** (다형성 지원)
- **M-1.6**: ✅ **GameState는 불변 객체** (@Value 사용)

#### M-2: tetris-client (Spring Boot + JavaFX)
- **M-2.1**: Spring Boot DI 컨테이너 사용
- **M-2.2**: @ConfigurationProperties로 설정 로드
- **M-2.3**: tetris-core를 의존성으로 사용
- **M-2.4**: @Configuration으로 Bean 등록
- **M-2.5**: ✅ **모든 동시성 컴포넌트는 Thread-safe** (AtomicBoolean/Integer 사용)
- **M-2.6**: ✅ **전역 예외 처리기 구현 필수** (@ControllerAdvice)

#### M-3: tetris-backend (Spring Boot REST API)
- **M-3.1**: REST API 서버
- **M-3.2**: MySQL 연동
- **M-3.3**: Server Authoritative 게임 로직
- **M-3.4**: WebSocket으로 실시간 Push
- **M-3.5**: ✅ **JWT 인증 필수**
- **M-3.6**: ✅ **Rate Limiting 구현 필수**
- **M-3.7**: ✅ **Cheating Detection 구현**

---

### 1.3 기능 요구사항

#### FR-1: 모드 조합 지원 (두 축 분리)
- **FR-1.1**: **Axis 1 - PlayType** (플레이 방식)
  - LOCAL_SINGLE: 싱글 플레이
  - ONLINE_MULTI: 대전 플레이
- **FR-1.2**: **Axis 2 - GameplayType** (게임 규칙)
  - CLASSIC: 기본 테트리스
  - ARCADE: 아이템 테트리스
- **FR-1.3**: 4가지 조합 지원
  - Single + Classic (기본 테트리스)
  - Single + Arcade (아이템 싱글)
  - Multi + Classic (대전 테트리스)
  - Multi + Arcade (아이템 대전)

#### FR-2: 멀티플레이어 Command 전송
- **FR-2.1**: 모든 사용자 입력을 서버에 전송
  - MOVE_LEFT, MOVE_RIGHT
  - ROTATE_CW, ROTATE_CCW
  - HARD_DROP
  - HOLD
  - USE_ITEM (Arcade)
- **FR-2.2**: Client-Side Prediction (즉시 반응)
- **FR-2.3**: Server Authoritative (서버 검증)
- **FR-2.4**: State Reconciliation (서버 상태로 보정)
- **FR-2.5**: ✅ **Command Throttling** (16ms 간격, 60 FPS)
  - **제약사항**: 동일 타입 Command는 16ms 간격으로만 전송
  - **목적**: 서버 부하 방지 (100명 접속 시 6,000 req/s → 375 req/s)

#### FR-3: UI 이벤트 시스템 (Hybrid 방식)
- **FR-3.1**: Critical Events - 서버 생성
  - LINE_CLEAR, T_SPIN, COMBO
  - LEVEL_UP, PERFECT_CLEAR
  - ATTACK_SENT, ATTACK_RECEIVED
  - GAME_OVER
- **FR-3.2**: Local Events - 클라이언트 생성
  - BLOCK_MOVE, BLOCK_ROTATE
  - BLOCK_LOCK
  - GHOST_PIECE_UPDATE
- **FR-3.3**: 우선순위 기반 순차 표시
- **FR-3.4**: GameState + Events 함께 전송
- **FR-3.5**: ✅ **UIEventHandler는 Thread-safe 필수**
  - **구현 조건**: AtomicBoolean + synchronized block 사용
  - **제약사항**: Race Condition 방지

#### FR-4: 아이템 시스템 (Arcade 모드)
- **FR-4.1**: 아이템 드롭 확률 설정 가능
- **FR-4.2**: 활성화 아이템 선택 가능
- **FR-4.3**: 아이템 효과 적용
  - Bomb: 주변 블록 제거
  - Plus: 1줄 추가
  - SpeedReset: 낙하 속도 초기화
  - BonusScore: 보너스 점수
- **FR-4.4**: 아이템 블록은 1칸짜리 특수 테트로미노
- **FR-4.5**: 아이템 블록은 Hold 불가

#### FR-5: 난이도 시스템
- **FR-5.1**: 난이도별 테트로미노 생성 확률 변경
- **FR-5.2**: 난이도별 점수 배율 적용
- **FR-5.3**: 난이도: EASY, NORMAL, HARD, EXPERT

#### FR-6: 네트워크 시스템 (Multi 모드)
- **FR-6.1**: 공격 전송 (2줄 이상 클리어 시)
- **FR-6.2**: 공격 수신 및 적용
- **FR-6.3**: 네트워크 장애 처리 (Proxy 패턴)
- **FR-6.4**: 오프라인 큐잉 및 자동 재전송
- **FR-6.5**: ✅ **자동 재연결 로직 필수**
  - **구현 조건**: 5초 간격으로 Ping 테스트
  - **제약사항**: 최대 재연결 시도 횟수 없음 (계속 시도)
  - **목적**: 일시적 네트워크 장애 대응
- **FR-6.6**: ✅ **오프라인 큐 크기 제한**
  - **제약사항**: 최대 1000개 항목
  - **초과 시**: 가장 오래된 항목 제거 (FIFO)
  - **목적**: Memory Leak 방지

#### FR-7: 동시성 처리 ⭐ 신규
- **FR-7.1**: ✅ **모든 공유 변수는 Thread-safe 구현**
  - UIEventHandler.isProcessing → AtomicBoolean
  - MultiPlayStrategy.sequenceNumber → AtomicInteger
  - CriticalEventGenerator.eventSequenceId → AtomicInteger
- **FR-7.2**: ✅ **Queue 접근 시 synchronized 사용**
  - UIEventHandler.eventQueue 접근 시 lock 사용
  - NetworkServiceProxy.offlineQueue 접근 시 동기화
- **FR-7.3**: ✅ **CAS (Compare-And-Swap) 패턴 사용**
  - isProcessing 상태 변경 시 compareAndSet() 사용
  - **목적**: Race Condition 방지

#### FR-8: 예외 처리 전략 ⭐ 신규
- **FR-8.1**: ✅ **전역 예외 처리기 구현 필수**
  - @ControllerAdvice 사용
  - 모든 예외를 일관된 형식으로 변환
- **FR-8.2**: ✅ **에러 코드 표준화**
  ```
  - 400: INVALID_COMMAND (잘못된 Command)
  - 408: NETWORK_TIMEOUT (네트워크 타임아웃)
  - 409: STATE_CONFLICT (상태 불일치)
  - 429: TOO_MANY_REQUESTS (Rate Limit 초과)
  - 500: INTERNAL_ERROR (내부 오류)
  - 503: SERVICE_UNAVAILABLE (서비스 불가)
  ```
- **FR-8.3**: ✅ **예외 계층 구조**
  - TetrisException (최상위)
    - NetworkException
    - ValidationException
    - StateConflictException
    - CheatDetectedException
- **FR-8.4**: ✅ **Graceful Degradation**
  - NetworkException → 오프라인 모드 전환
  - StateConflictException → 서버 상태로 강제 동기화
  - ValidationException → 사용자에게 에러 메시지 표시

#### FR-9: 보안 시스템 ⭐ 신규
- **FR-9.1**: ✅ **JWT 인증 필수** (멀티플레이어)
  - 모든 API 요청에 JWT 토큰 필요
  - 토큰 만료 시간: 1시간
  - Refresh Token 지원
- **FR-9.2**: ✅ **Rate Limiting**
  - 플레이어당 최대 100 req/min
  - 초과 시 429 에러 반환
  - Sliding Window 방식
- **FR-9.3**: ✅ **Cheating Detection**
  - **검증 항목**:
    - 점수 증가율: 최대 1000점/초
    - 라인 클리어: 최대 10줄/초
    - Command 간격: 최소 5ms
  - **탐지 시**: Command 거부 + 경고 로그
  - **3회 탐지 시**: 게임 강제 종료

---

### 1.4 비기능 요구사항

#### NFR-1: 확장성
- **NFR-1.1**: Strategy 패턴으로 PlayType 확장 용이
- **NFR-1.2**: Composition 패턴으로 GameplayType 독립 구성
- **NFR-1.3**: 새 모드 추가 시 기존 코드 수정 최소화

#### NFR-2: 반응성
- **NFR-2.1**: Client-Side Prediction으로 즉시 피드백
- **NFR-2.2**: Local Events 즉시 표시 (<50ms)
- **NFR-2.3**: 네트워크 지연 허용 범위 (100-200ms)
- **NFR-2.4**: ✅ **Command 처리 시간 제한**
  - **목표**: 평균 <50ms, 최대 <100ms
  - **측정**: @Measured 어노테이션으로 성능 로깅

#### NFR-3: 일관성
- **NFR-3.1**: Server Authoritative로 치팅 방지
- **NFR-3.2**: State Reconciliation으로 동기화
- **NFR-3.3**: Critical Events 서버 생성으로 일관성 보장
- **NFR-3.4**: ✅ **State Mismatch 감지**
  - **검증 항목**: currentTetromino, score, grid
  - **불일치 시**: 서버 상태로 강제 동기화 + 로그
- **NFR-3.5**: ✅ **Pending Commands 타임아웃**
  - **제약사항**: 5초 내 서버 응답 없으면 타임아웃
  - **타임아웃 시**: Command 제거 + 재전송 또는 취소

#### NFR-4: 유지보수성
- **NFR-4.1**: 단일 책임 원칙 준수
- **NFR-4.2**: 모듈 경계 명확
- **NFR-4.3**: Spring Boot 컨벤션 준수
- **NFR-4.4**: ✅ **BoardController 책임 분리**
  - CommandHandler: Command 처리
  - GameStateManager: 상태 관리
  - ServerCommunicator: 서버 통신
  - UIRenderer: 렌더링

#### NFR-5: 테스트 가능성
- **NFR-5.1**: 각 컴포넌트 독립적 테스트 가능
- **NFR-5.2**: Mock 주입 용이
- **NFR-5.3**: Spring Test 활용

#### NFR-6: 동시성 안정성 ⭐ 신규
- **NFR-6.1**: ✅ **Race Condition 제거**
  - 모든 공유 변수는 AtomicBoolean/AtomicInteger 사용
  - Queue 접근 시 synchronized block 사용
  - CAS 패턴으로 상태 변경
- **NFR-6.2**: ✅ **Deadlock 방지**
  - Lock 순서 일관성 유지
  - 중첩 Lock 최소화
  - Timeout 설정
- **NFR-6.3**: ✅ **Memory Visibility 보장**
  - volatile 키워드 사용 (단순 플래그)
  - AtomicReference 사용 (객체 참조)

#### NFR-7: 오류 복구 능력 ⭐ 신규
- **NFR-7.1**: ✅ **네트워크 장애 자동 복구**
  - 5초 간격 자동 재연결
  - 오프라인 큐 최대 1000개
  - 재연결 시 자동 Flush
- **NFR-7.2**: ✅ **상태 불일치 자동 복구**
  - Mismatch 감지 → 서버 상태로 동기화
  - Pending Commands 재실행
- **NFR-7.3**: ✅ **Graceful Degradation**
  - 서버 오류 시 싱글 플레이 모드로 전환 제안
  - 일부 기능 오류 시 나머지 기능 정상 동작

#### NFR-8: 보안성 ⭐ 신규
- **NFR-8.1**: ✅ **인증 필수**
  - 모든 API 요청에 JWT 검증
  - 토큰 없으면 401 Unauthorized
- **NFR-8.2**: ✅ **입력 검증**
  - 모든 Command 서버에서 검증
  - 범위 체크, 타입 체크
- **NFR-8.3**: ✅ **치팅 방지**
  - 점수/라인 클리어 속도 검증
  - 의심 행위 로그 기록
  - 3회 탐지 시 게임 종료

#### NFR-9: 성능 ⭐ 신규
- **NFR-9.1**: ✅ **응답 시간**
  - Command 처리: 평균 <50ms, 최대 <100ms
  - State Update: 평균 <100ms, 최대 <200ms
  - Local Event 표시: <50ms
- **NFR-9.2**: ✅ **처리량**
  - 동시 접속: 1000명
  - 서버 처리량: 1000 req/s (Throttling 적용 시)
- **NFR-9.3**: ✅ **메모리**
  - 클라이언트: 최대 512MB
  - 서버 (플레이어당): 최대 10MB
  - 오프라인 큐: 최대 100KB

#### NFR-10: 테스트 전략 ⭐ 신규
- **NFR-10.1**: ✅ **단위 테스트**
  - 커버리지: 최소 80%
  - 도구: JUnit 5, Mockito
  - 대상: GameEngine, PlayTypeStrategy, UIEventHandler
- **NFR-10.2**: ✅ **통합 테스트**
  - 커버리지: 주요 흐름 100%
  - 도구: Spring Boot Test, TestContainers
  - 시나리오: Command → 서버 → 응답 → Reconciliation
- **NFR-10.3**: ✅ **성능 테스트**
  - 도구: JMeter, Gatling
  - 목표: 1000명 동시 접속 시 응답 시간 <100ms
- **NFR-10.4**: ✅ **E2E 테스트**
  - 도구: TestFX (JavaFX 테스트)
  - 시나리오: 전체 게임 플레이

#### NFR-11: 모니터링 ⭐ 신규
- **NFR-11.1**: ✅ **메트릭 수집**
  - game.commands.total (Counter)
  - game.commands.duration (Timer)
  - game.active.players (Gauge)
  - game.state.conflicts (Counter)
- **NFR-11.2**: ✅ **알림 설정**
  - 에러율 > 5%: 경고
  - 응답 시간 > 200ms: 경고
  - 동시 접속 > 900명: 주의
- **NFR-11.3**: ✅ **대시보드**
  - Grafana 대시보드 구성
  - 실시간 메트릭 표시
  - 히스토리 데이터 7일 보관

#### NFR-12: 로깅 전략 ⭐ 신규
- **NFR-12.1**: ✅ **로그 레벨**
  - TRACE: 상세 디버깅 (개발 환경)
  - DEBUG: 일반 디버깅 (개발 환경)
  - INFO: 중요 이벤트 (운영 환경)
  - WARN: 경고 (100ms 이상 처리)
  - ERROR: 오류 (예외 발생)
- **NFR-12.2**: ✅ **로그 포맷**
  - 구조화된 로그 (JSON)
  - 타임스탬프, 스레드명, 레벨, 메시지 포함
- **NFR-12.3**: ✅ **로그 보관**
  - 파일 로그: 최대 10MB, 30일 보관
  - 에러 로그: 별도 파일 저장
- **NFR-12.4**: ✅ **성능 로깅**
  - @Measured 어노테이션으로 자동 로깅
  - 100ms 이상 걸리는 메서드 경고

---

## 2. 변경 파일 목록 (Change List)

### 2.1 tetris-core 모듈

#### 수정 파일
```
tetris-core/src/main/java/seoultech/se/core/
├── GameEngine.java                          [REFACTOR] Static → Interface
├── GameState.java                           [REFACTOR] Mutable → Immutable (@Value)
├── config/
│   ├── GameConfig.java                      [NEW] @ConfigurationProperties
│   └── ItemConfig.java                      [MODIFY] @ConfigurationProperties
├── exception/                               [NEW] 예외 계층 구조
│   ├── TetrisException.java                [NEW] 최상위 예외
│   ├── ValidationException.java            [NEW] 검증 예외
│   └── StateConflictException.java         [NEW] 상태 불일치 예외
└── item/
    ├── ItemManager.java                     [NO CHANGE]
    └── ItemSystem.java                      [OPTIONAL] ItemManager 래퍼
```

#### 중요 변경사항
```java
// ✅ GameEngine을 Interface로 변경
public interface GameEngine {
    GameState lockTetromino(GameState state);
    GameState tryMoveLeft(GameState state);
    // ...
}

@Component
public class ClassicGameEngine implements GameEngine {
    // 구현
}

@Component
@Primary
public class ArcadeGameEngine implements GameEngine {
    // 구현
}

// ✅ GameState를 불변 객체로 변경
@Value
@Builder(toBuilder = true)
public class GameState {
    private final int score;
    private final int level;
    // ...
    
    public GameState withScore(int newScore) {
        return this.toBuilder().score(newScore).build();
    }
}
```

### 2.2 tetris-client 모듈

#### 신규 생성 파일
```
tetris-client/src/main/java/seoultech/se/client/
├── config/
│   ├── GameEngineConfig.java               [NEW] GameEngine Bean 등록
│   ├── GameModeConfig.java                 [NEW] PlayTypeStrategy Bean 등록
│   ├── NetworkConfig.java                  [NEW] NetworkService + Proxy
│   └── TetrisGameConfig.java               [NEW] @ConfigurationProperties
├── strategy/
│   ├── PlayTypeStrategy.java               [NEW] Strategy 인터페이스 (확장)
│   ├── SinglePlayStrategy.java             [NEW] Single 구현
│   └── MultiPlayStrategy.java              [CRITICAL] AtomicInteger 사용
├── proxy/
│   └── NetworkServiceProxy.java            [CRITICAL] 재연결 + 큐 제한
├── service/
│   ├── NetworkService.java                 [NEW] ping() 메서드 추가
│   └── NetworkServiceImpl.java             [NEW] 실제 네트워크 통신
├── event/
│   ├── UIEvent.java                        [NEW] UI 이벤트 DTO
│   ├── UIEventHandler.java                 [CRITICAL] AtomicBoolean + synchronized
│   └── LocalUIEventGenerator.java          [NEW] Local Event 생성
├── exception/
│   ├── NetworkException.java               [NEW] 네트워크 예외
│   └── GlobalExceptionHandler.java         [NEW] @ControllerAdvice
├── security/                               [NEW] 보안 컴포넌트
│   ├── JwtAuthenticationFilter.java        [NEW] JWT 필터
│   └── RateLimitingInterceptor.java        [NEW] Rate Limiting
├── monitoring/                             [NEW] 모니터링
│   ├── GameMetrics.java                    [NEW] Micrometer 메트릭
│   └── PerformanceLoggingAspect.java       [NEW] @Measured AOP
└── dto/
    ├── GameCommand.java                    [NEW] Command DTO
    ├── GameUpdateResponse.java             [NEW] Response DTO
    ├── ErrorResponse.java                  [NEW] 에러 응답 DTO
    └── AttackEvent.java                    [NEW] 공격 이벤트 DTO
```

#### 수정 파일 (CRITICAL)
```
tetris-client/src/main/java/seoultech/se/client/
├── controller/
│   └── BoardController.java                [MAJOR REFACTOR] 
│       - Command 전송 + 이벤트 처리
│       - forceStateUpdate() 메서드 추가
│       - 예외 처리 추가
└── mode/
    ├── PlayType.java                        [MOVE] tetris-core에서 이동
    └── GameplayType.java                    [NEW] Axis 2 정의
```

### 2.3 tetris-backend 모듈

#### 신규 생성 파일
```
tetris-backend/src/main/java/seoultech/se/backend/
├── game/
│   ├── GameService.java                    [NEW] 게임 로직 서비스
│   ├── CriticalEventGenerator.java         [CRITICAL] AtomicInteger 사용
│   ├── GameStateStore.java                 [NEW] 게임 상태 저장소
│   └── CheatDetectionService.java          [NEW] 치팅 검증
├── controller/
│   └── GameController.java                 [NEW] REST API 엔드포인트
├── security/
│   ├── JwtUtil.java                        [NEW] JWT 유틸리티
│   ├── JwtAuthenticationFilter.java        [NEW] JWT 필터
│   └── SecurityConfig.java                 [NEW] Spring Security 설정
├── exception/
│   ├── GlobalExceptionHandler.java         [NEW] @RestControllerAdvice
│   └── ErrorCode.java                      [NEW] 에러 코드 Enum
└── websocket/
    └── GameWebSocketHandler.java           [NEW] WebSocket 핸들러
```

---

## 3. 아키텍처 설계 (Architecture)

### 3.1 핵심 설계 원칙

#### 원칙 1: 두 축 명확히 분리 (Composition)

```
┌────────────────────────────────────────────────────────┐
│                  BoardController                        │
│                   (Orchestrator)                        │
│                                                         │
│  executeCommand() {                                    │
│    try {                                               │
│      1. playTypeStrategy.beforeCommand()              │ ← Axis 1 (서버 전송)
│      2. gameEngine.execute()                          │ ← Axis 2 (로컬 예측)
│      3. playTypeStrategy.afterCommand()               │ ← Axis 1 (예측 저장)
│    } catch (NetworkException e) {                     │
│      handleNetworkError(e);                           │
│    }                                                    │
│  }                                                      │
│                                                         │
│  onServerUpdate() {                                    │
│    1. playTypeStrategy.onServerStateUpdate()          │ ← Axis 1 (보정)
│    2. uiEventHandler.handleEvents()                   │ ← UI Events
│  }                                                      │
└───────────┬────────────────────────┬───────────────────┘
            │                        │
            │ Axis 2                 │ Axis 1
            │ (Gameplay)             │ (PlayType)
            ▼                        ▼
   ┌─────────────────┐      ┌─────────────────┐
   │   GameEngine    │      │ PlayTypeStrategy│
   │  (Interface)    │      │  (Interface)    │
   └────────┬────────┘      └────────┬────────┘
            │                        │
     ┌──────┴──────┐        ┌────────┴────────┐
     │             │        │                 │
     ▼             ▼        ▼                 ▼
┌─────────┐  ┌─────────┐  ┌──────┐     ┌──────┐
│Classic  │  │ Arcade  │  │Single│     │Multi │
│Engine   │  │ Engine  │  │Play  │     │Play  │
└─────────┘  └─────────┘  └──────┘     └──────┘
```

#### 원칙 2: Server Authoritative + Cheating Detection

```
┌─────────────┐                    ┌─────────────┐
│   Client    │                    │   Server    │
│             │                    │             │
│  1. Input   │──sendCommand()────→│ 2. Validate │
│     ↓       │     + JWT Token    │  & Execute  │
│  3. Local   │                    │     ↓       │
│  Prediction │                    │ 3. Cheat    │
│     ↓       │                    │  Detection  │
│  4. Render  │←──GameState +──────│     ↓       │
│     +       │   Events[]         │ 4. Generate │
│  Reconcile  │                    │  Events     │
└─────────────┘                    └─────────────┘
```

#### 원칙 3: 동시성 안정성

```
UIEventHandler:
┌──────────────────────────────────────────┐
│ private final AtomicBoolean isProcessing │ ← Thread-safe
│                                           │
│ public void handleEvents(...) {          │
│   synchronized (lock) {                  │ ← Race Condition 방지
│     eventQueue.addAll(events);           │
│   }                                       │
│                                           │
│   if (isProcessing.compareAndSet(        │ ← CAS 패턴
│       false, true)) {                    │
│     processNextEvent();                  │
│   }                                       │
│ }                                         │
└──────────────────────────────────────────┘
```

---

## 8. Spring Boot 설정 (Configuration)

### 8.1 application.yml (완전한 설정)

```yaml
# ========================================
# Tetris Game Configuration
# ========================================

tetris:
  # ========== Axis 1: PlayType (플레이 방식) ==========
  play-type: LOCAL_SINGLE  # LOCAL_SINGLE | ONLINE_MULTI
  
  # ========== Network (Multi 모드) ==========
  network:
    enabled: false
    server-url: http://localhost:8080
    timeout: 5000
    reconnect-interval: 5000    # ✅ 재연결 간격 (ms)
    max-queue-size: 1000        # ✅ 오프라인 큐 크기
  
  # ========== Game Settings ==========
  game:
    board-width: 10
    board-height: 20
    srs-enabled: true
    difficulty: NORMAL
    
    item:
      enabled: false
      drop-rate: 0.15
      enabled-items:
        - BOMB
        - PLUS_ONE_LINE
        - SPEED_RESET
        - BONUS_SCORE

# ========================================
# 동시성 설정 ⭐ 신규
# ========================================
concurrency:
  thread-pool-size: 10
  scheduler-pool-size: 1
  command-timeout-ms: 5000      # ✅ Command 타임아웃

# ========================================
# 보안 설정 ⭐ 신규
# ========================================
security:
  jwt:
    secret: ${JWT_SECRET:tetris-secret-key-change-in-production}
    expiration: 3600000  # 1 hour
  
  rate-limiting:
    enabled: true
    max-requests: 100
    window-seconds: 60
  
  cheating-detection:
    enabled: true
    max-score-per-second: 1000
    max-lines-per-second: 10
    max-violation-count: 3

# ========================================
# 성능 설정 ⭐ 신규
# ========================================
performance:
  command-throttle-ms: 16       # ✅ Command Throttling (60 FPS)
  max-command-duration-ms: 100  # ✅ Command 처리 시간 제한
  max-state-update-ms: 200      # ✅ State Update 시간 제한

# ========================================
# 예외 처리 설정 ⭐ 신규
# ========================================
error-handling:
  strategy: GRACEFUL_DEGRADATION
  include-stacktrace: true      # 개발 환경에서만 true
  network-error-fallback: OFFLINE_MODE

# ========================================
# 모니터링 설정 ⭐ 신규
# ========================================
management:
  endpoints:
    web:
      exposure:
        include: health, metrics, prometheus
  
  metrics:
    export:
      prometheus:
        enabled: true
    
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:dev}

monitoring:
  custom-metrics:
    - name: game.commands.total
      type: COUNTER
    - name: game.commands.duration
      type: TIMER
    - name: game.active.players
      type: GAUGE
    - name: game.state.conflicts
      type: COUNTER

# ========================================
# 로깅 설정 ⭐ 신규
# ========================================
logging:
  level:
    root: INFO
    seoultech.se.client: DEBUG
    seoultech.se.backend: INFO
    org.springframework.security: DEBUG
  
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  
  file:
    name: logs/tetris-client.log
    max-size: 10MB
    max-history: 30
  
  # 성능 로깅
  performance:
    enabled: true
    threshold-ms: 100  # 100ms 이상 걸리면 경고

# ========================================
# Spring Boot 기본 설정
# ========================================
spring:
  application:
    name: tetris-client
  
  jackson:
    serialization:
      indent-output: true
      write-dates-as-timestamps: false
```

---

## 9. 검증 체크리스트 (Verification)

### 9.1 기능 검증

#### V-F1: 멀티플레이어 Command 전송
- [ ] MOVE_LEFT, ROTATE 등 모든 입력이 서버로 전송
- [ ] beforeCommand() 호출 확인
- [ ] Command Throttling 동작 (16ms 간격)

#### V-F2: Client-Side Prediction
- [ ] 로컬에서 즉시 실행
- [ ] 지연 없는 반응 (<50ms)

#### V-F3: State Reconciliation
- [ ] 서버 상태로 보정
- [ ] onServerStateUpdate() 호출 확인
- [ ] State Mismatch 감지 및 동기화

#### V-F4: UI 이벤트 시스템
- [ ] Local Events 즉시 표시
- [ ] Critical Events 순차 표시
- [ ] Priority Queue 정렬 확인
- [ ] 복잡한 케이스 (7개 이벤트 동시 발생)

#### V-F5: 네트워크 장애 처리
- [ ] 오프라인 큐잉 동작
- [ ] 자동 재연결 (5초 간격)
- [ ] 재연결 시 큐 Flush
- [ ] 큐 크기 제한 (1000개)

---

### 9.2 동시성 검증 ⭐ 신규

#### V-C1: UIEventHandler Thread-safety
- [ ] AtomicBoolean 사용 확인
- [ ] synchronized block 사용 확인
- [ ] CAS 패턴 동작 확인
- [ ] 멀티스레드 환경 테스트 (100 스레드)

#### V-C2: MultiPlayStrategy Thread-safety
- [ ] sequenceNumber가 AtomicInteger 확인
- [ ] pendingCommands가 ConcurrentHashMap 확인
- [ ] 동시 Command 전송 테스트 (1000 요청)

#### V-C3: CriticalEventGenerator Thread-safety
- [ ] eventSequenceId가 AtomicInteger 확인
- [ ] 동시 이벤트 생성 테스트 (100 요청)

---

### 9.3 예외 처리 검증 ⭐ 신규

#### V-E1: 전역 예외 처리
- [ ] @ControllerAdvice 동작 확인
- [ ] NetworkException → 오프라인 모드 전환
- [ ] StateConflictException → 강제 동기화
- [ ] ValidationException → 사용자 메시지 표시

#### V-E2: 에러 코드 표준화
- [ ] 400 INVALID_COMMAND
- [ ] 408 NETWORK_TIMEOUT
- [ ] 409 STATE_CONFLICT
- [ ] 429 TOO_MANY_REQUESTS
- [ ] 500 INTERNAL_ERROR

#### V-E3: Graceful Degradation
- [ ] 서버 오류 시 싱글 플레이 제안
- [ ] 부분 오류 시 나머지 기능 정상 동작

---

### 9.4 보안 검증 ⭐ 신규

#### V-S1: JWT 인증
- [ ] 토큰 없이 요청 시 401 반환
- [ ] 만료된 토큰 시 401 반환
- [ ] 정상 토큰 시 요청 성공

#### V-S2: Rate Limiting
- [ ] 60초 내 100 요청 시 정상
- [ ] 60초 내 101 요청 시 429 반환
- [ ] Sliding Window 동작 확인

#### V-S3: Cheating Detection
- [ ] 점수 1000점/초 초과 시 거부
- [ ] 라인 10줄/초 초과 시 거부
- [ ] 3회 탐지 시 게임 종료

---

### 9.5 성능 검증 ⭐ 신규

#### V-P1: 응답 시간
- [ ] Command 처리: 평균 <50ms, 최대 <100ms
- [ ] State Update: 평균 <100ms, 최대 <200ms
- [ ] Local Event 표시: <50ms

#### V-P2: 처리량
- [ ] 동시 접속 1000명 테스트
- [ ] 서버 처리량 1000 req/s 유지

#### V-P3: 메모리
- [ ] 클라이언트 메모리 <512MB
- [ ] 서버 메모리 (플레이어당) <10MB
- [ ] 메모리 누수 테스트 (장시간 실행)

---

### 9.6 테스트 검증 ⭐ 신규

#### V-T1: 단위 테스트
- [ ] 테스트 커버리지 >80%
- [ ] GameEngine 모든 메서드 테스트
- [ ] PlayTypeStrategy 모든 구현체 테스트

#### V-T2: 통합 테스트
- [ ] Command → 서버 → 응답 흐름
- [ ] State Reconciliation 시나리오
- [ ] 네트워크 장애 시나리오

#### V-T3: 성능 테스트
- [ ] JMeter: 1000명 동시 접속
- [ ] 응답 시간 <100ms 유지
- [ ] 에러율 <1%

---

### 9.7 모니터링 검증 ⭐ 신규

#### V-M1: 메트릭 수집
- [ ] game.commands.total 증가 확인
- [ ] game.commands.duration 기록 확인
- [ ] game.active.players 정확성 확인
- [ ] game.state.conflicts 기록 확인

#### V-M2: 알림
- [ ] 에러율 >5% 시 알림 발생
- [ ] 응답 시간 >200ms 시 알림 발생
- [ ] 동시 접속 >900명 시 알림 발생

#### V-M3: 대시보드
- [ ] Grafana 대시보드 접속
- [ ] 실시간 메트릭 표시 확인
- [ ] 히스토리 데이터 조회 확인

---

## 부록 A: 구현 우선순위 (수정)

### 🔴 Phase 1: CRITICAL 수정 (1-3일)
```
1. ✅ 동시성 이슈 수정
   - UIEventHandler: AtomicBoolean + synchronized
   - MultiPlayStrategy: AtomicInteger
   - CriticalEventGenerator: AtomicInteger
   
2. ✅ 전역 예외 처리
   - @ControllerAdvice 구현
   - ErrorCode enum 정의
   - 예외 계층 구조
   
3. ✅ 보안 기본 구현
   - JWT 인증 필터
   - Rate Limiting 인터셉터
```

### 🟡 Phase 2: HIGH 개선 (1-2주)
```
4. ✅ 네트워크 재연결
   - 자동 재연결 로직
   - ping() 메서드 추가
   - 큐 크기 제한
   
5. ✅ State Reconciliation 강화
   - Mismatch 감지
   - 강제 동기화
   - Pending Command 타임아웃
   
6. ✅ 로깅 전략
   - 구조화된 로깅
   - 성능 로깅 (@Measured AOP)
   - 파일 로그 설정
```

### 🟢 Phase 3: MEDIUM 개선 (1-2개월)
```
7. ✅ 아키텍처 리팩토링
   - GameEngine Interface
   - BoardController 책임 분리
   - GameState 불변성
   
8. ✅ 성능 최적화
   - Command Throttling
   - Batch Processing (선택)
   
9. ✅ 모니터링 구축
   - Prometheus 메트릭
   - Grafana 대시보드
   - 알림 설정
   
10. ✅ 테스트 작성
    - 단위 테스트 (80% 커버리지)
    - 통합 테스트
    - 성능 테스트
```

**총 예상 시간**: 2-4주

---

## 부록 B: 체크리스트

### B.1 즉시 수정 체크리스트 🔴

- [ ] UIEventHandler에 AtomicBoolean 적용
- [ ] UIEventHandler에 synchronized block 추가
- [ ] MultiPlayStrategy.sequenceNumber를 AtomicInteger로
- [ ] CriticalEventGenerator.eventSequenceId를 AtomicInteger로
- [ ] 전역 예외 처리기 구현 (@ControllerAdvice)
- [ ] ErrorCode enum 정의
- [ ] 예외 계층 구조 구현 (TetrisException 등)
- [ ] JWT 인증 필터 추가
- [ ] Rate Limiting 인터셉터 추가

### B.2 단기 개선 체크리스트 🟡

- [ ] NetworkServiceProxy 재연결 로직 구현
- [ ] 오프라인 큐 크기 제한 (1000개)
- [ ] NetworkService.ping() 메서드 추가
- [ ] State Reconciliation mismatch 감지
- [ ] Pending Commands 타임아웃 (5초)
- [ ] 로깅 설정 (application.yml)
- [ ] 성능 로깅 Aspect (@Measured)
- [ ] Cheating Detection 구현

### B.3 중기 개선 체크리스트 🟢

- [ ] GameEngine을 Interface로 리팩토링
- [ ] BoardController 책임 분리
- [ ] GameState를 @Value로 불변화
- [ ] Command Throttling 구현 (16ms)
- [ ] Prometheus 메트릭 추가
- [ ] Grafana 대시보드 구성
- [ ] 단위 테스트 80% 커버리지
- [ ] 통합 테스트 작성
- [ ] 성능 테스트 (1000명)

---

**변경 이력**:
- v5.0 (2025-11-06): 분석 결과 반영 - 동시성, 예외 처리, 보안, 성능, 테스트, 모니터링 요구사항 추가
- v4.0 (2025-11-06): 논의 반영 - Hybrid UI Events, Command 전송
- v3.0: 초기 작성

**END OF DOCUMENT**
