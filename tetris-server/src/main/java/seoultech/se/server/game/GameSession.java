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

    private final Object lock = new Object(); // 동기화를 위한 락 객체

    /**
     * 생성자 (GameEngine 주입)
     *
     * @param sessionId 세션 ID
     * @param gameEngine 싱글톤 GameEngine (GameEnginePool에서 제공)
     */
    public GameSession(String sessionId, GameEngine gameEngine) {
        this.sessionId = sessionId;
        this.gameEngine = gameEngine;
        System.out.println("✅ [GameSession] Created: " + sessionId +
            ", Engine: " + (gameEngine != null ? gameEngine.getClass().getSimpleName() : "null"));
    }

    /**
     * 플레이어 참여
     * 
     * @param playerId 플레이어 ID
     */
    public void joinPlayer(String playerId) {
        synchronized (lock) {
            if (hostPlayerId == null) {
                // 첫 번째 플레이어가 호스트
                hostPlayerId = playerId;
                System.out.println("👑 [GameSession] Host set: " + playerId);
            }
            
            playerStates.put(playerId, new GameState(10, 20)); // 초기 상태
            lastSequences.put(playerId, 0L); // 초기 시퀀스 번호
            pendingAttackLines.put(playerId, 0); // 대기 중인 공격 라인 초기화
            
            System.out.println("✅ [GameSession] Player joined: " + playerId +
                " (" + playerStates.size() + " players total)");
        }
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
                gameModeConfig = GameModeConfig.classic();
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

    public ServerStateDto processInput(String playerId, PlayerInputDto input){
        synchronized(lock){
            GameState currentState = playerStates.get(playerId);
            
            // 1. 시퀀스 검증 (오래된 패킷 무시)
            long lastSeq = lastSequences.getOrDefault(playerId, 0L);
            if (input.getSequenceId() <= lastSeq) {
                return null; // 이미 처리된 입력은 무시
            }

            // 2. 서버 권한으로 로직 실행
            GameState nextState = gameEngine.executeCommand(input.getCommand(), currentState);
            
            // 3. 상태 업데이트
            playerStates.put(playerId, nextState);
            lastSequences.put(playerId, input.getSequenceId());

            // 4. 상대방 ID 찾기
            String opponentId = playerStates.keySet().stream()
                    .filter(id -> !id.equals(playerId))
                    .findFirst()
                    .orElse(null);

            // 5. 이벤트 감지 및 공격 로직
            List<String> events = new ArrayList<>();
            int linesCleared = nextState.getLastLinesCleared();

            if (linesCleared > 0) {
                events.add("LINE_CLEAR");

                // 6. 상대방에게 공격 라인 추가 (라인 수 - 1)
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

            // 7. 나에게 대기 중인 공격 라인 가져오기 및 초기화
            int attackReceived = pendingAttackLines.getOrDefault(playerId, 0);
            if (attackReceived > 0) {
                pendingAttackLines.put(playerId, 0); // 처리했으므로 초기화
                System.out.println("🛡️ [GameSession] " + playerId + " received " + attackReceived + " attack lines");
            }

            return ServerStateDto.builder()
                    .lastProcessedSequence(input.getSequenceId())
                    .myGameState(nextState)
                    .opponentGameState(opponentId != null ? playerStates.get(opponentId) : null)
                    .events(events)
                    .attackLinesReceived(attackReceived)
                    .build();
        }
    }
}
