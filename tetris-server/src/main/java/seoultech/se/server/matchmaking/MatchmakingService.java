package seoultech.se.server.matchmaking;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.model.enumType.Difficulty;
import seoultech.se.server.dto.MatchFoundNotification;
import seoultech.se.server.game.GameSession;
import seoultech.se.server.game.GameSessionManager;
import seoultech.se.server.user.UserEntity;
import seoultech.se.server.user.UserRepository;
import seoultech.se.server.websocket.WebSocketEventListener;

import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Phase 2: 매칭 시스템
 *
 * 기능:
 * - 대기 큐 관리 (GameplayType + Difficulty별 분리)
 * - 자동 매칭 (2명이 대기하면 자동으로 게임 세션 생성)
 * - 매칭 취소
 * - 매칭 상태 조회
 *
 * 매칭 프로세스:
 * 1. 플레이어가 큐에 참여
 * 2. 동일한 게임 모드의 플레이어 2명이 대기 중이면 매칭
 * 3. 새로운 게임 세션 생성
 * 4. 양측에 세션 ID 전달
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchmakingService {

    private final GameSessionManager gameSessionManager;
    private final WebSocketEventListener webSocketEventListener;
    private final seoultech.se.server.admin.AdminDashboardService dashboardService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    /**
     * 매칭 큐: 게임 모드별로 대기 중인 플레이어 관리
     * Key: "GameplayType:Difficulty" (예: "CLASSIC:NORMAL")
     * Value: 플레이어 ID 큐
     */
    private final Map<String, Queue<WaitingPlayer>> matchmakingQueues = new ConcurrentHashMap<>();

    /**
     * 플레이어 ID → 대기 정보 매핑
     * 매칭 취소 및 상태 조회에 사용
     */
    private final Map<String, WaitingPlayer> waitingPlayers = new ConcurrentHashMap<>();

    /**
     * Phase 2: 매칭 큐 참여
     *
     * @param playerId 플레이어 ID
     * @param gameplayType 게임 타입
     * @param difficulty 난이도
     * @return 매칭 결과 (즉시 매칭되면 세션 ID 포함, 대기 중이면 null)
     */
    public MatchmakingResult joinQueue(String playerId, GameplayType gameplayType, Difficulty difficulty) {
        // 이미 대기 중인지 확인
        if (waitingPlayers.containsKey(playerId)) {
            log.warn("⚠️ [Matchmaking] Player {} already in queue", playerId);
            return MatchmakingResult.alreadyInQueue();
        }

        String queueKey = getQueueKey(gameplayType, difficulty);
        Queue<WaitingPlayer> queue = matchmakingQueues.computeIfAbsent(queueKey, k -> new ConcurrentLinkedQueue<>());

        WaitingPlayer waitingPlayer = new WaitingPlayer(playerId, gameplayType, difficulty);
        queue.add(waitingPlayer);
        waitingPlayers.put(playerId, waitingPlayer);

        log.info("🎮 [Matchmaking] Player {} joined queue: {} (queue size: {})",
            playerId, queueKey, queue.size());

        // 자동 매칭 시도
        return tryMatch(queueKey, queue);
    }

    /**
     * Phase 2: 자동 매칭 시도
     *
     * 큐에 2명 이상의 플레이어가 대기 중이면 매칭
     * synchronized로 동시 매칭 시도를 방지하여 경쟁 조건 방지
     *
     * @param queueKey 큐 키
     * @param queue 대기 큐
     * @return 매칭 결과
     */
    private synchronized MatchmakingResult tryMatch(String queueKey, Queue<WaitingPlayer> queue) {
        if (queue.size() >= 2) {
            WaitingPlayer player1 = queue.poll();
            WaitingPlayer player2 = queue.poll();

            if (player1 != null && player2 != null) {
                // 대기 중 정보 제거
                waitingPlayers.remove(player1.getPlayerId());
                waitingPlayers.remove(player2.getPlayerId());

                // 게임 세션 생성
                String sessionId = UUID.randomUUID().toString();
                GameSession session = gameSessionManager.createSession(
                    sessionId,
                    player1.getGameplayType(),
                    player1.getDifficulty()
                );

                // 플레이어 참여
                session.joinPlayer(player1.getPlayerId());
                session.joinPlayer(player2.getPlayerId());

                // WebSocket 이벤트 리스너에 등록
                webSocketEventListener.registerPlayerSession(player1.getPlayerId(), sessionId);
                webSocketEventListener.registerPlayerSession(player2.getPlayerId(), sessionId);

                log.info("✅ [Matchmaking] Match found! Session: {}, Players: {} vs {}",
                    sessionId, player1.getPlayerId(), player2.getPlayerId());

                // 매칭 완료 알림 전송
                sendMatchFoundNotifications(
                    sessionId,
                    player1.getPlayerId(),
                    player2.getPlayerId(),
                    player1.getGameplayType()
                );

                // Dashboard 통계 기록
                dashboardService.recordMatchCreated();

                return MatchmakingResult.matched(sessionId, player1.getPlayerId(), player2.getPlayerId());
            }
        }

        // 매칭 실패 (대기 중)
        return MatchmakingResult.waiting();
    }

    /**
     * Phase 2: 매칭 취소
     *
     * @param playerId 플레이어 ID
     * @return 취소 성공 여부
     */
    public boolean leaveQueue(String playerId) {
        WaitingPlayer waitingPlayer = waitingPlayers.remove(playerId);

        if (waitingPlayer != null) {
            String queueKey = getQueueKey(waitingPlayer.getGameplayType(), waitingPlayer.getDifficulty());
            Queue<WaitingPlayer> queue = matchmakingQueues.get(queueKey);

            if (queue != null) {
                queue.remove(waitingPlayer);
                log.info("👋 [Matchmaking] Player {} left queue: {}", playerId, queueKey);
                return true;
            }
        }

        log.warn("⚠️ [Matchmaking] Player {} not found in any queue", playerId);
        return false;
    }

    /**
     * Phase 2: 매칭 상태 조회
     *
     * @param playerId 플레이어 ID
     * @return 대기 중이면 Optional with WaitingPlayer, 아니면 empty
     */
    public Optional<WaitingPlayer> getWaitingStatus(String playerId) {
        return Optional.ofNullable(waitingPlayers.get(playerId));
    }

    /**
     * Phase 2: 큐 크기 조회
     *
     * @param gameplayType 게임 타입
     * @param difficulty 난이도
     * @return 대기 중인 플레이어 수
     */
    public int getQueueSize(GameplayType gameplayType, Difficulty difficulty) {
        String queueKey = getQueueKey(gameplayType, difficulty);
        Queue<WaitingPlayer> queue = matchmakingQueues.get(queueKey);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Phase 2: 큐 키 생성
     *
     * @param gameplayType 게임 타입
     * @param difficulty 난이도
     * @return 큐 키 (예: "CLASSIC:NORMAL")
     */
    private String getQueueKey(GameplayType gameplayType, Difficulty difficulty) {
        return gameplayType + ":" + difficulty;
    }

    /**
     * 매칭 완료 알림 전송 (두 플레이어에게)
     *
     * @param sessionId 세션 ID
     * @param player1Email 플레이어 1 이메일
     * @param player2Email 플레이어 2 이메일
     * @param gameplayType 게임 모드
     */
    private void sendMatchFoundNotifications(
            String sessionId,
            String player1Email,
            String player2Email,
            GameplayType gameplayType) {

        log.info("🎮 [Matchmaking] Sending match found notifications");
        log.info("   - Session: {}", sessionId);
        log.info("   - Player 1: {}", player1Email);
        log.info("   - Player 2: {}", player2Email);

        // 🕐 동기화를 위해 단일 타임스탬프 생성
        long sharedTimestamp = System.currentTimeMillis();
        log.info("   - Shared timestamp: {}", sharedTimestamp);

        // Player 1과 Player 2의 사용자 정보 조회
        UserEntity player1User = userRepository.findByEmail(player1Email).orElse(null);
        UserEntity player2User = userRepository.findByEmail(player2Email).orElse(null);

        // 상대방 정보 추출
        String player2Name = player2User != null ? player2User.getName() : "알 수 없음";
        String player2EmailFinal = player2User != null ? player2User.getEmail() : player2Email;

        String player1Name = player1User != null ? player1User.getName() : "알 수 없음";
        String player1EmailFinal = player1User != null ? player1User.getEmail() : player1Email;

        // Player 1에게 Player 2 정보 전송 (동일한 타임스탬프 사용)
        MatchFoundNotification notification1 = MatchFoundNotification.create(
            sessionId,
            player2Name,
            player2EmailFinal,
            gameplayType.name(),
            sharedTimestamp
        );

        messagingTemplate.convertAndSendToUser(
            player1Email,
            "/queue/match-found",
            notification1
        );

        log.info("   → Sent to Player 1 ({}): opponent is {}", player1Email, player2Name);

        // Player 2에게 Player 1 정보 전송 (동일한 타임스탬프 사용)
        MatchFoundNotification notification2 = MatchFoundNotification.create(
            sessionId,
            player1Name,
            player1EmailFinal,
            gameplayType.name(),
            sharedTimestamp
        );

        messagingTemplate.convertAndSendToUser(
            player2Email,
            "/queue/match-found",
            notification2
        );

        log.info("   → Sent to Player 2 ({}): opponent is {}", player2Email, player1Name);
        log.info("✅ [Matchmaking] Match found notifications sent to both players with synchronized timestamp");
    }

    /**
     * Phase 2: 대기 중인 플레이어 정보
     */
    public static class WaitingPlayer {
        private final String playerId;
        private final GameplayType gameplayType;
        private final Difficulty difficulty;
        private final long joinedAt;

        public WaitingPlayer(String playerId, GameplayType gameplayType, Difficulty difficulty) {
            this.playerId = playerId;
            this.gameplayType = gameplayType;
            this.difficulty = difficulty;
            this.joinedAt = System.currentTimeMillis();
        }

        public String getPlayerId() {
            return playerId;
        }

        public GameplayType getGameplayType() {
            return gameplayType;
        }

        public Difficulty getDifficulty() {
            return difficulty;
        }

        public long getJoinedAt() {
            return joinedAt;
        }

        public long getWaitingTimeMs() {
            return System.currentTimeMillis() - joinedAt;
        }
    }

    /**
     * Phase 2: 매칭 결과
     */
    public static class MatchmakingResult {
        private final MatchStatus status;
        private final String sessionId;
        private final String player1Id;
        private final String player2Id;

        private MatchmakingResult(MatchStatus status, String sessionId, String player1Id, String player2Id) {
            this.status = status;
            this.sessionId = sessionId;
            this.player1Id = player1Id;
            this.player2Id = player2Id;
        }

        public static MatchmakingResult matched(String sessionId, String player1Id, String player2Id) {
            return new MatchmakingResult(MatchStatus.MATCHED, sessionId, player1Id, player2Id);
        }

        public static MatchmakingResult waiting() {
            return new MatchmakingResult(MatchStatus.WAITING, null, null, null);
        }

        public static MatchmakingResult alreadyInQueue() {
            return new MatchmakingResult(MatchStatus.ALREADY_IN_QUEUE, null, null, null);
        }

        public MatchStatus getStatus() {
            return status;
        }

        public String getSessionId() {
            return sessionId;
        }

        public String getPlayer1Id() {
            return player1Id;
        }

        public String getPlayer2Id() {
            return player2Id;
        }

        public boolean isMatched() {
            return status == MatchStatus.MATCHED;
        }
    }

    /**
     * Phase 2: 매칭 상태
     */
    public enum MatchStatus {
        MATCHED,           // 매칭 완료
        WAITING,           // 대기 중
        ALREADY_IN_QUEUE   // 이미 큐에 참여 중
    }
}
