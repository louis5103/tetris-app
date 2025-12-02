package seoultech.se.server.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.dto.PlayerInputDto;
import seoultech.se.core.dto.ServerStateDto;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.TetrominoType;
import seoultech.se.core.random.TetrominoGenerator;

/**
 * 게임 세션
 *
 * Stateless 리팩토링: 싱글톤 GameEngine을 공유하여 사용
 *
 * 변경 사항:
 * - GameEngine을 외부에서 주입받음 (GameEnginePool의 싱글톤)
 * - GameEngineFactory.createGameEngine() 제거
 * - 여러 세션이 동일한 GameEngine 인스턴스를 공유
 * - GameModeConfig 추가: 멀티플레이어 세션의 권위 있는 설정 저장
 *
 * Thread-safety:
 * - GameEngine은 Stateless이므로 동시 접근 안전
 * - playerStates는 ConcurrentHashMap으로 보호
 * - processInput은 synchronized로 보호
 * - gameModeConfig 설정은 동기화 블록에서 보호
 *
 * 멀티플레이어 설정 동기화:
 * - 호스트가 게임 시작 전 GameModeConfig 설정
 * - 모든 클라이언트는 서버의 Config를 사용 (로컬 설정 무시)
 * - 세션 조인 시 Config를 클라이언트에게 전송
 */
public class GameSession {

    private final String sessionId;
    private final Map<String, GameState> playerStates = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSequences = new ConcurrentHashMap<>();
    private final Map<String, Integer> pendingAttackLines = new ConcurrentHashMap<>(); // 대기 중인 공격 라인
    private final Map<String, TetrominoGenerator> playerGenerators = new ConcurrentHashMap<>(); // 플레이어별 블록 생성기
    private final GameEngine gameEngine; // 싱글톤 공유

    /**
     * 게임 모드 설정 (멀티플레이어 세션의 권위 있는 Config)
     * - 호스트만 설정 가능
     * - 게임 시작 전에만 변경 가능
     * - 모든 클라이언트가 이 Config를 공유
     */
    private GameModeConfig gameModeConfig;

    private String hostPlayerId; // 호스트 플레이어 ID (Config 설정 권한)
    private boolean isGameStarted = false; // 게임 시작 여부

    /**
     * Phase 1: 세션 타임아웃 추적
     * 마지막 활동 시간 (밀리초)
     */
    private volatile long lastActivityTime;

    /**
     * 세션 타입 (SINGLE/MULTI)
     * - SINGLE: 클라이언트가 모든 로직 처리, 서버는 상태만 저장
     * - MULTI: 서버가 자동 게임 루프 실행, 클라이언트는 입력만 전송
     */
    private final SessionType sessionType;

    /**
     * 마지막 틱 시간 (멀티플레이용)
     * - 서버 게임 루프에서 자동 중력 적용 시 사용
     * - 각 플레이어마다 독립적인 틱 시간 관리
     */
    private final Map<String, Long> lastTickTimes = new ConcurrentHashMap<>();
    private final java.util.Set<String> offlinePlayers = ConcurrentHashMap.newKeySet(); // 연결 끊긴 플레이어 추적

    private final Object lock = new Object(); // 동기화를 위한 락 객체

    /**
     * 생성자 (GameEngine 주입)
     *
     * @param sessionId 세션 ID
     * @param gameEngine 싱글톤 GameEngine (GameEnginePool에서 제공)
     * @param sessionType 세션 타입 (SINGLE/MULTI)
     */
    public GameSession(String sessionId, GameEngine gameEngine, SessionType sessionType) {
        this.sessionId = sessionId;
        this.gameEngine = gameEngine;
        this.sessionType = sessionType;
        this.lastActivityTime = System.currentTimeMillis(); // 생성 시점을 마지막 활동 시간으로 초기화
        System.out.println("✅ [GameSession] Created: " + sessionId +
            ", Type: " + sessionType +
            ", Engine: " + (gameEngine != null ? gameEngine.getClass().getSimpleName() : "null"));
    }

    /**
     * 플레이어 참여
     *
     * @param playerId 플레이어 ID
     * @return 매칭 완료 여부 (두 번째 플레이어가 참여하면 true)
     */
    public boolean joinPlayer(String playerId) {
        synchronized (lock) {
            if (hostPlayerId == null) {
                // 첫 번째 플레이어가 호스트
                hostPlayerId = playerId;
                System.out.println("👑 [GameSession] Host set: " + playerId);
            }

            // 플레이어 전용 블록 생성기 생성
            seoultech.se.core.random.RandomGenerator randomGen = new seoultech.se.core.random.RandomGenerator();
            seoultech.se.core.model.enumType.Difficulty difficulty = gameModeConfig != null ?
                gameModeConfig.getDifficulty() : seoultech.se.core.model.enumType.Difficulty.NORMAL;
            TetrominoGenerator generator = new TetrominoGenerator(randomGen, difficulty);
            playerGenerators.put(playerId, generator);

            // 초기 상태 생성 및 첫 블록 스폰
            GameState initialState = new GameState(10, 20);
            spawnNextBlock(initialState, playerId); // 첫 블록 생성 및 Next Queue 업데이트

            playerStates.put(playerId, initialState);
            lastSequences.put(playerId, 0L); // 초기 시퀀스 번호
            pendingAttackLines.put(playerId, 0); // 대기 중인 공격 라인 초기화
            offlinePlayers.remove(playerId); // 온라인 상태로 전환

            // 멀티플레이 세션인 경우 틱 시간 초기화
            if (sessionType == SessionType.MULTI) {
                lastTickTimes.put(playerId, System.currentTimeMillis());
            }

            // Phase 1: 활동 시간 갱신
            updateLastActivityTime();

            int playerCount = playerStates.size();
            System.out.println("✅ [GameSession] Player joined: " + playerId +
                " (" + playerCount + " players total)");

            // 두 번째 플레이어가 참여하면 매칭 완료
            return playerCount == 2;
        }
    }

    /**
     * Phase 1: 마지막 활동 시간 갱신
     */
    private void updateLastActivityTime() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    /**
     * Phase 1: 마지막 활동 시간 조회
     *
     * @return 마지막 활동 시간 (밀리초)
     */
    public long getLastActivityTime() {
        return lastActivityTime;
    }

    /**
     * Phase 1: 플레이어 제거
     *
     * @param playerId 제거할 플레이어 ID
     * @return 제거 성공 여부
     */
    public boolean removePlayer(String playerId) {
        synchronized (lock) {
            boolean removed = playerStates.remove(playerId) != null;

            if (removed) {
                lastSequences.remove(playerId);
                pendingAttackLines.remove(playerId);
                playerGenerators.remove(playerId); // 블록 생성기도 제거
                offlinePlayers.remove(playerId); // 오프라인 목록에서도 제거

                System.out.println("👋 [GameSession] Player removed: " + playerId +
                    " (" + playerStates.size() + " players remaining)");

                // 호스트가 나간 경우 새로운 호스트 지정
                if (playerId.equals(hostPlayerId)) {
                    hostPlayerId = playerStates.keySet().stream().findFirst().orElse(null);
                    if (hostPlayerId != null) {
                        System.out.println("👑 [GameSession] New host: " + hostPlayerId);
                    }
                }
            }

            return removed;
        }
    }
    
    /**
     * 플레이어 온라인 상태 설정
     * 
     * @param playerId 플레이어 ID
     * @param isOnline 온라인 여부
     */
    public void setPlayerOnline(String playerId, boolean isOnline) {
        if (isOnline) {
            offlinePlayers.remove(playerId);
        } else {
            offlinePlayers.add(playerId);
        }
    }
    
    /**
     * 플레이어 온라인 여부 확인
     * 
     * @param playerId 플레이어 ID
     * @return 온라인이면 true
     */
    public boolean isPlayerOnline(String playerId) {
        return !offlinePlayers.contains(playerId);
    }
    
    /**
     * 활성 플레이어가 있는지 확인
     * 
     * @return 최소 1명의 플레이어가 온라인이면 true
     */
    public boolean hasActivePlayers() {
        // 등록된 플레이어 중 오프라인이 아닌 플레이어가 1명이라도 있으면 true
        return playerStates.keySet().stream()
            .anyMatch(id -> !offlinePlayers.contains(id));
    }

    /**
     * Phase 1: 현재 플레이어 수 조회
     *
     * @return 플레이어 수
     */
    public int getPlayerCount() {
        return playerStates.size();
    }

    /**
     * 플레이어 ID 목록 반환
     *
     * @return 플레이어 ID 리스트
     */
    public List<String> getPlayerIds() {
        return new ArrayList<>(playerStates.keySet());
    }

    /**
     * 게임 모드 설정 (호스트만 가능, 또는 초기 설정)
     * 
     * @param playerId 설정을 요청한 플레이어 ID (null이면 초기 설정)
     * @param config 설정할 GameModeConfig
     * @throws IllegalStateException 게임이 이미 시작되었거나 호스트가 아닌 경우
     */
    public void setGameModeConfig(String playerId, GameModeConfig config) {
        synchronized (lock) {
            // playerId가 null이면 초기 설정 (GameSessionManager가 호출)
            if (playerId != null) {
                // 검증 1: 호스트만 설정 가능
                if (!playerId.equals(hostPlayerId)) {
                    throw new IllegalStateException("Only host can set game config. Host: " + hostPlayerId);
                }
                
                // 검증 2: 게임 시작 전에만 설정 가능
                if (isGameStarted) {
                    throw new IllegalStateException("Cannot change config after game has started");
                }
            }
            
            this.gameModeConfig = config;
            
            if (playerId != null) {
                System.out.println("⚙️ [GameSession] Config set by host " + playerId +
                    ": " + config.getGameplayType() + " / " + config.getDifficulty());
            } else {
                System.out.println("⚙️ [GameSession] Initial config set: " +
                    config.getGameplayType() + " / " + config.getDifficulty());
            }
        }
    }
    
    /**
     * 게임 모드 설정 조회 (클라이언트 동기화용)
     * 
     * @return 현재 설정된 GameModeConfig (null일 수 있음)
     */
    public GameModeConfig getGameModeConfig() {
        return gameModeConfig;
    }
    
    /**
     * 호스트 플레이어 ID 조회
     * 
     * @return 호스트 플레이어 ID
     */
    public String getHostPlayerId() {
        return hostPlayerId;
    }
    
    /**
     * 게임 시작 마킹
     * Config가 설정되지 않았으면 기본 Config 사용
     */
    public void startGame() {
        synchronized (lock) {
            if (isGameStarted) {
                System.out.println("⚠️ [GameSession] Game already started");
                return;
            }
            
            // Config가 없으면 기본값 사용
            if (gameModeConfig == null) {
                gameModeConfig = GameModeConfig.createDefaultClassic();
                System.out.println("⚙️ [GameSession] No config set, using default: " +
                    gameModeConfig.getGameplayType() + " / " + gameModeConfig.getDifficulty());
            }
            
            isGameStarted = true;
            System.out.println("🎮 [GameSession] Game started with " + playerStates.size() + " players");
        }
    }
    
    /**
     * 게임 시작 여부 조회
     * 
     * @return 게임이 시작되었는지 여부
     */
    public boolean isGameStarted() {
        return isGameStarted;
    }

    /**
     * 다음 블록 생성 및 스폰 (통합 메서드)
     *
     * @param state 게임 상태 (변경됨)
     * @param playerId 플레이어 ID
     */
    private void spawnNextBlock(GameState state, String playerId) {
        TetrominoGenerator generator = playerGenerators.get(playerId);
        if (generator == null) {
            System.err.println("❌ [GameSession] No generator for player: " + playerId);
            return;
        }

        // 새 테트로미노 생성
        TetrominoType nextType = generator.next();
        Tetromino newTetromino = new Tetromino(nextType);

        // 초기 위치 설정
        int startX = (state.getBoardWidth() - newTetromino.getCurrentShape()[0].length) / 2;
        int startY = 0;

        state.setCurrentTetromino(newTetromino);
        state.setCurrentX(startX);
        state.setCurrentY(startY);
        state.setHoldUsedThisTurn(false); // 새 블록이므로 Hold 재사용 가능

        // 아이템 타입 설정 (있다면)
        state.setCurrentItemType(state.getNextBlockItemType());
        state.setNextBlockItemType(null);
        state.setWeightBombLocked(false); // 무게추 초기화

        // Next Queue 업데이트 (표시용)
        TetrominoType[] queue = state.getNextQueue();
        // TetrominoGenerator는 peekNext 메서드가 없으므로 간단히 기본값으로 설정
        // 클라이언트에서 표시용으로만 사용
        for (int i = 0; i < queue.length; i++) {
            queue[i] = TetrominoType.I; // 기본값
        }
    }

    /**
     * 공격 라인 처리 결과를 담는 내부 클래스
     */
    private static class AttackResult {
        private final List<String> events;
        private final int attackLinesReceived;
        private final boolean gameOver;

        public AttackResult(List<String> events, int attackLinesReceived, boolean gameOver) {
            this.events = events;
            this.attackLinesReceived = attackLinesReceived;
            this.gameOver = gameOver;
        }

        public List<String> getEvents() {
            return events;
        }

        public int getAttackLinesReceived() {
            return attackLinesReceived;
        }

        public boolean isGameOver() {
            return gameOver;
        }
    }

    /**
     * 공격 라인 처리 로직 (공통 메서드)
     * 
     * @param state 게임 상태 (라인 클리어 정보 포함)
     * @param playerId 플레이어 ID
     * @param opponentId 상대방 ID
     * @param currentState 현재 상태 (공격 라인 적용용)
     * @return 공격 처리 결과
     */
    private AttackResult processAttackLines(GameState state, String playerId, String opponentId, GameState currentState) {
        List<String> events = new ArrayList<>();
        int linesCleared = state.getLastLinesCleared();

        // 라인 클리어 이벤트
        if (linesCleared > 0) {
            events.add("LINE_CLEAR");

            // 상대방에게 공격 라인 추가 (라인 수 - 1)
            if (opponentId != null && linesCleared > 1) {
                int attackLines = linesCleared - 1; // 2줄 → 1줄, 3줄 → 2줄, 4줄 → 3줄

                // 상대방의 대기 중인 공격 라인에 누적
                int currentPending = pendingAttackLines.getOrDefault(opponentId, 0);
                pendingAttackLines.put(opponentId, currentPending + attackLines);

                events.add("ATTACK_SENT:" + attackLines);
                System.out.println("⚔️ [GameSession] Attack: " + playerId +
                    " → " + opponentId + " (" + attackLines + " lines, total pending: " +
                    (currentPending + attackLines) + ")");
            }
        }

        // 나에게 대기 중인 공격 라인 가져오기 및 초기화
        int attackReceived = pendingAttackLines.getOrDefault(playerId, 0);
        boolean gameOver = false;
        if (attackReceived > 0) {
            pendingAttackLines.put(playerId, 0); // 처리했으므로 초기화
            
            // ✨ 중요: 서버 상태에 실제로 방해 라인 적용 (Server Authoritative)
            gameOver = currentState.addGarbageLines(attackReceived);
            if (gameOver) {
                System.out.println("💀 [GameSession] Player " + playerId + " Game Over by attack");
            }
            
            System.out.println("🛡️ [GameSession] " + playerId + " received and APPLIED " + attackReceived + " attack lines");
        }

        return new AttackResult(events, attackReceived, gameOver);
    }

    public ServerStateDto processInput(String playerId, PlayerInputDto input, seoultech.se.backend.mapper.GameStateMapper gameStateMapper){
        synchronized(lock){
            GameState currentState = playerStates.get(playerId);

            // Phase 1: 활동 시간 갱신
            updateLastActivityTime();

            // 플레이어 상태가 없으면 자동으로 join 처리
            if (currentState == null) {
                System.out.println("⚠️ [GameSession] No state for player: " + playerId + ", auto-joining...");
                joinPlayer(playerId);
                currentState = playerStates.get(playerId);

                if (currentState == null) {
                    System.err.println("❌ [GameSession] Failed to initialize player state");
                    return null;
                }
            }

            // 1. 시퀀스 검증 (오래된 패킷 무시)
            long lastSeq = lastSequences.getOrDefault(playerId, 0L);
            if (input.getSequenceId() <= lastSeq) {
                return null; // 이미 처리된 입력은 무시
            }

            // 2. 서버 권한으로 로직 실행
            GameState nextState = gameEngine.executeCommand(input.getCommand(), currentState);

            // nextState가 null이면 명령 실행 실패
            if (nextState == null) {
                System.err.println("❌ [GameSession] Command execution failed, command: " + input.getCommand());
                return null;
            }

            // 블록이 잠긴 경우 (currentTetromino가 null) 새 블록 생성
            if (nextState.getCurrentTetromino() == null && !nextState.isGameOver()) {
                spawnNextBlock(nextState, playerId);
            }

            // 3. 상태 업데이트
            playerStates.put(playerId, nextState);
            lastSequences.put(playerId, input.getSequenceId());

            // 4. 상대방 ID 찾기
            String opponentId = playerStates.keySet().stream()
                    .filter(id -> !id.equals(playerId))
                    .findFirst()
                    .orElse(null);

            // 5. 공격 라인 처리 (공통 메서드 사용)
            AttackResult attackResult = processAttackLines(nextState, playerId, opponentId, currentState);
            
            // 게임 오버 체크 (명령 실행으로 인한 게임 오버도 확인)
            boolean gameOver = nextState.isGameOver() || attackResult.isGameOver();

            // GameState를 GameStateDto로 변환
            return ServerStateDto.builder()
                    .lastProcessedSequence(input.getSequenceId())
                    .myGameState(gameStateMapper.toDto(nextState, (int)input.getSequenceId()))
                    .opponentGameState(opponentId != null ? gameStateMapper.toDto(playerStates.get(opponentId), 0) : null)
                    .events(attackResult.getEvents())
                    .attackLinesReceived(attackResult.getAttackLinesReceived())
                    .gameOver(gameOver)
                    .build();
        }
    }

    /**
     * 자동 중력 적용 (멀티플레이 서버 게임 루프용)
     *
     * @param playerId 플레이어 ID
     * @param currentTime 현재 시간 (밀리초)
     * @param gameStateMapper GameState를 GameStateDto로 변환하는 매퍼
     * @return 업데이트된 ServerStateDto (상태가 변경된 경우) 또는 null (틱 간격 미도달)
     */
    public ServerStateDto applyGravity(String playerId, long currentTime, seoultech.se.backend.mapper.GameStateMapper gameStateMapper) {
        synchronized (lock) {
            // 1. 세션 타입 검증
            if (sessionType != SessionType.MULTI) {
                System.err.println("⚠️ [GameSession] applyGravity called on non-MULTI session");
                return null;
            }

            // 2. 플레이어 상태 확인
            GameState currentState = playerStates.get(playerId);
            if (currentState == null) {
                System.err.println("⚠️ [GameSession] No state for player: " + playerId);
                return null;
            }

            // 3. 게임 오버 체크
            if (currentState.isGameOver()) {
                return null; // 게임 오버 상태에서는 중력 적용 안함
            }

            // 4. 틱 간격 계산 (레벨에 따른 낙하 속도)
            long lastTickTime = lastTickTimes.getOrDefault(playerId, currentTime);
            int level = currentState.getLevel();
            long tickInterval = calculateTickInterval(level); // 레벨에 따른 간격

            // 5. 틱 간격이 아직 도달하지 않았으면 스킵
            if (currentTime - lastTickTime < tickInterval) {
                return null;
            }

            // 6. 자동 중력 적용 (DOWN 명령 실행)
            seoultech.se.core.command.MoveCommand downCommand =
                new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.DOWN);

            GameState nextState = gameEngine.executeCommand(downCommand, currentState);

            // 7. 명령 실행 실패 시
            if (nextState == null) {
                System.err.println("❌ [GameSession] Gravity application failed for player: " + playerId);
                return null;
            }

            // 8. 블록이 잠긴 경우 새 블록 생성
            // 블록이 없고 게임 오버가 아니면 새 블록 생성
            if (nextState.getCurrentTetromino() == null && !nextState.isGameOver()) {
                spawnNextBlock(nextState, playerId);
            }

            // 9. 상태 업데이트
            playerStates.put(playerId, nextState);
            lastTickTimes.put(playerId, currentTime); // 틱 시간 갱신
            updateLastActivityTime();

            // 10. 상대방 ID 찾기
            String opponentId = playerStates.keySet().stream()
                    .filter(id -> !id.equals(playerId))
                    .findFirst()
                    .orElse(null);

            // 11. 공격 라인 처리 (공통 메서드 사용)
            AttackResult attackResult = processAttackLines(nextState, playerId, opponentId, currentState);
            
            // 게임 오버 체크 (중력 적용으로 인한 게임 오버도 확인)
            boolean gameOver = nextState.isGameOver() || attackResult.isGameOver();

            // 12. 응답 생성 (GameState를 GameStateDto로 변환)
            return ServerStateDto.builder()
                    .lastProcessedSequence(0L) // 자동 틱이므로 시퀀스 없음
                    .myGameState(gameStateMapper.toDto(nextState, 0))
                    .opponentGameState(opponentId != null ? gameStateMapper.toDto(playerStates.get(opponentId), 0) : null)
                    .events(attackResult.getEvents())
                    .attackLinesReceived(attackResult.getAttackLinesReceived())
                    .gameOver(gameOver)
                    .build();
        }
    }

    /**
     * 레벨에 따른 틱 간격 계산
     *
     * @param level 현재 레벨
     * @return 틱 간격 (밀리초)
     */
    private long calculateTickInterval(int level) {
        // 레벨에 따라 블록 낙하 속도 조절
        // 레벨 1: 1000ms, 레벨 10: 100ms
        long baseInterval = 1000L; // 1초
        long minInterval = 100L;   // 0.1초
        long decrement = 100L;     // 레벨당 100ms 감소

        long interval = baseInterval - ((level - 1) * decrement);
        return Math.max(interval, minInterval);
    }

    /**
     * 세션 타입 조회
     *
     * @return 세션 타입
     */
    public SessionType getSessionType() {
        return sessionType;
    }

    /**
     * 특정 플레이어의 게임 상태 조회
     *
     * @param playerId 플레이어 ID
     * @return 게임 상태 (없으면 null)
     */
    public GameState getStateForPlayer(String playerId) {
        return playerStates.get(playerId);
    }

    /**
     * 세션 ID 조회
     *
     * @return 세션 ID
     */
    public String getSessionId() {
        return sessionId;
    }
}
