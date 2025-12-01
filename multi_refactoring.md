# 서버 측 멀티플레이어 로직 단순화 및 구현 완성 계획

## 핵심 원칙

1. **클라이언트**: MOVE, ROTATE, HARD_DROP, HOLD 명령만 서버에 전송
2. **서버**: GameEngine만 사용하여 명령 처리 및 자동 낙하 실행
3. **클라이언트**: 서버로부터 받은 상태만 UI에 반영 (예측 없음)

## 현재 문제점 및 미구현 사항

### 서버 측

1. **GameSession (627줄)**: 과도한 동기화, 중복 로직
2. **GameTickService**: 구조는 있으나 단순화 필요
3. **초기 상태 전달**: 게임 시작 시 클라이언트에 초기 상태를 전달하는 API 없음
4. **게임 오버 처리**: 명확한 게임 오버 플래그 및 알림 부족

### 클라이언트 측

1. **초기 상태 수신**: 서버에서 초기 상태를 받아오는 로직 없음
2. **명령 필터링**: 모든 명령이 전송됨 (MOVE, ROTATE, HARD_DROP, HOLD만 허용해야 함)
3. **게임 오버 처리**: 서버 게임 오버 상태 수신 시 명령 차단 로직 없음
4. **이벤트 처리**: 서버에서 받은 이벤트를 UI에 표시하는 로직 부족
5. **Client-side prediction**: 제거 필요

## 단순화 및 구현 방안

### 1. 서버: 초기 상태 전달 API 추가

- **파일**: `tetris-server/src/main/java/seoultech/se/server/controller/GameSessionController.java`
- **추가**: `GET /api/game/state/{sessionId}` 엔드포인트
- **기능**: 게임 시작 시 클라이언트가 초기 상태를 요청할 수 있도록 함
- **반환**: `ServerStateDto` (myGameState, opponentGameState 포함)

### 2. 클라이언트: 초기 상태 수신 로직

- **파일**: 
- `tetris-client/src/main/java/seoultech/se/client/controller/MultiGameController.java`
- `tetris-client/src/main/java/seoultech/se/client/service/GameApiService.java`
- **추가**: `getInitialState(String sessionId)` 메서드
- **변경**: `initMultiplayer()`에서 서버 초기 상태를 받아와서 설정

### 3. 클라이언트: 명령 필터링

- **파일**: `tetris-client/src/main/java/seoultech/se/client/controller/MultiGameController.java`
- **변경**: `handleCommand()`에서 MOVE, ROTATE, HARD_DROP, HOLD만 허용
- **제외**: PAUSE, RESUME, SOFT_DROP (MOVE_DOWN은 자동 낙하로 서버 처리)

### 4. 게임 오버 처리

- **서버**: `ServerStateDto`에 `gameOver` 플래그 추가
- **클라이언트**: 게임 오버 상태 수신 시 명령 전송 차단
- **파일**: 
- `tetris-core/src/main/java/seoultech/se/core/dto/ServerStateDto.java`
- `tetris-client/src/main/java/seoultech/se/client/controller/MultiGameController.java`
- `tetris-backend/src/main/java/seoultech/se/backend/network/NetworkGameClient.java`

### 5. UI 이벤트 처리

- **파일**: `tetris-client/src/main/java/seoultech/se/client/controller/MultiGameController.java`
- **추가**: `ServerStateDto.events`를 UI 이벤트로 변환하여 표시
- **이벤트 타입**: LINE_CLEAR, ATTACK_SENT, ATTACK_RECEIVED 등

### 6. GameSession 단순화

- **파일**: `tetris-server/src/main/java/seoultech/se/server/game/GameSession.java`
- **변경 사항**:
- `processInput()`: GameEngine.executeCommand()만 호출하고 상태 업데이트
- 시퀀스 번호 검증 단순화
- `synchronized` 블록 최소화
- 공격 라인 처리 로직을 `processAttackLines()`로 추출

### 7. GameTickService 단순화

- **파일**: `tetris-server/src/main/java/seoultech/se/server/service/GameTickService.java`
- **변경 사항**:
- 각 플레이어에 대해 GameEngine.executeCommand(DOWN) 호출
- 복잡한 틱 타이머 관리 단순화
- 상태 변경 시에만 브로드캐스트

### 8. 공격 라인 처리 로직 통합

- **파일**: `tetris-server/src/main/java/seoultech/se/server/game/GameSession.java`
- **추가**: `processAttackLines(GameState state, String playerId, String opponentId)` 공통 메서드
- **효과**: `processInput()`과 `applyGravity()`에서 중복 제거

### 9. WebSocket 토픽 통합

- **파일**: 
- `tetris-server/src/main/java/seoultech/se/server/controller/GameSessionController.java`
- `tetris-backend/src/main/java/seoultech/se/backend/network/NetworkTemplate.java`
- `tetris-backend/src/main/java/seoultech/se/backend/network/NetworkGameClient.java`
- **변경**: `/user/topic/game/sync`와 `/user/queue/game-state`를 `/user/topic/game/state` 하나로 통합

### 10. 블록 생성 로직 단순화

- **파일**: `tetris-server/src/main/java/seoultech/se/server/game/GameSession.java`
- **변경**: `spawnNewTetromino()`와 `updateNextQueue()`를 `spawnNextBlock()` 하나로 통합

### 11. 에러 처리 개선

- **파일**: `tetris-server/src/main/java/seoultech/se/server/game/GameSession.java`
- **변경**: 명령 실행 실패 시 `null` 대신 에러 정보 포함 DTO 반환

### 12. 클라이언트: Client-side prediction 제거

- **파일**: `tetris-client/src/main/java/seoultech/se/client/controller/MultiGameController.java`
- **변경**: `handleCommand()`에서 예측 로직 제거, 서버 상태만 UI 반영

### 13. NetworkGameClient 단순화

- **파일**: `tetris-backend/src/main/java/seoultech/se/backend/network/NetworkGameClient.java`
- **변경**:
- 예측 로직 제거
- 통합된 토픽만 구독
- 서버 상태 수신 후 즉시 UI 업데이트

### 14. 시퀀스 번호 관리 단순화

- **파일**: `tetris-backend/src/main/java/seoultech/se/backend/network/NetworkGameClient.java`
- **변경**: 시퀀스 번호는 선택적으로 관리 (서버에서 중복 검증만 수행)

## 구현 우선순위

### Phase 1: 기본 동작 구현 (필수)

1. **GameState 크기 최적화** (최우선): ServerStateDto에서 GameStateDto 사용
2. 초기 상태 전달 API 및 수신 로직
3. 명령 필터링 (MOVE, ROTATE, HARD_DROP, HOLD만)
4. 게임 오버 처리
5. Client-side prediction 제거

### Phase 2: 단순화 (중요)

5. GameSession 단순화
6. GameTickService 단순화
7. 공격 라인 처리 로직 통합
8. WebSocket 토픽 통합

### Phase 3: 개선 (선택)

9. 블록 생성 로직 단순화
10. 에러 처리 개선
11. UI 이벤트 처리
12. 시퀀스 번호 관리 단순화

## GameState 크기 최적화 (중요)

### 문제점

- **GameState JSON 크기**: 약 12-30KB (보드 상태에 따라)
- **ServerStateDto**: myGameState + opponentGameState = 약 30-60KB
- **전송 빈도**: 100ms마다 (자동 낙하) + 사용자 입력마다
- **예상 대역폭**: 초당 300-600KB 이상
- **JSON 직렬화/역직렬화 오버헤드**: 높음

### 해결 방안

#### Option 1: 경량 DTO 사용 (권장)

- **파일**: `tetris-core/src/main/java/seoultech/se/core/dto/ServerStateDto.java`
- **변경**: GameState 전체 대신 경량 DTO 사용
- **구현**: 
- Grid를 `int[][]`로 변환 (Cell 객체 대신)
- 필요한 필드만 포함
- 예상 크기: 5-10KB (50-70% 감소)

#### Option 2: Delta 전송 (고급)

- 변경된 셀만 전송
- 초기 상태는 한 번만 전송, 이후 변경사항만
- 구현 복잡도 높음

#### Option 3: 압축 (간단)

- WebSocket 메시지 gzip 압축
- Spring에서 자동 지원 가능
- 예상 압축률: 30-50%

#### Option 4: 이진 프로토콜 (최적)

- JSON 대신 Protocol Buffers 또는 MessagePack
- 크기: 50-70% 감소
- 구현 복잡도 높음

### 권장 사항

1. **Phase 1**: 경량 DTO 사용 (GameStateDto 활용)
2. **Phase 2**: 필요시 압축 추가
3. **Phase 3**: 성능 문제 시 Delta 전송 고려

## 예상 효과

1. **코드 라인 수 감소**: 약 200-250줄 감소
2. **기능 완성**: 멀티플레이어 게임이 실제로 동작
3. **복잡도 감소**: 단순한 명령-처리-브로드캐스트 모델
4. **유지보수성 향상**: 역할 명확화, 중복 코드 제거
5. **안정성 향상**: 에러 처리 개선, 게임 오버 처리 명확화
6. **성능 향상**: GameState 크기 최적화로 네트워크 부하 감소
