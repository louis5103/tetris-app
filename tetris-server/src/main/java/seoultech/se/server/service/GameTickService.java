package seoultech.se.server.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import seoultech.se.backend.mapper.GameStateMapper;
import seoultech.se.core.dto.ServerStateDto;
import seoultech.se.server.game.GameSession;
import seoultech.se.server.game.GameSessionManager;

/**
 * 멀티플레이 게임 틱 서비스
 *
 * 책임:
 * - 멀티플레이 세션에 대해 주기적으로 자동 중력 적용
 * - 서버 권위 있는 게임 루프 실행
 * - 클라이언트는 사용자 입력만 전송, 서버가 모든 로직 처리
 *
 * 작동 원리:
 * 1. @Scheduled로 50ms마다 틱 메서드 실행
 * 2. GameSessionManager에서 모든 MULTI 세션 조회
 * 3. 각 세션의 모든 플레이어에 대해 applyGravity() 호출
 * 4. 상태가 변경된 경우 WebSocket으로 클라이언트에 브로드캐스트
 *
 * 설계 특징:
 * - 각 플레이어는 독립적인 틱 타이머 보유 (lastTickTime)
 * - 레벨에 따라 중력 적용 간격이 자동 조절됨
 * - 게임 오버 상태에서는 자동 중력 적용 안됨
 */
@Service
public class GameTickService {

    private final GameSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final GameStateMapper gameStateMapper;

    /**
     * 생성자
     *
     * @param sessionManager 게임 세션 관리자
     * @param messagingTemplate WebSocket 메시지 전송 템플릿
     * @param gameStateMapper GameState를 GameStateDto로 변환하는 매퍼
     */
    @Autowired
    public GameTickService(GameSessionManager sessionManager, SimpMessagingTemplate messagingTemplate, GameStateMapper gameStateMapper) {
        this.sessionManager = sessionManager;
        this.messagingTemplate = messagingTemplate;
        this.gameStateMapper = gameStateMapper;
        System.out.println("✅ [GameTickService] Created - Server game loop enabled for multiplayer");
    }

    private long tickCount = 0;
    private static final long LOG_INTERVAL = 20; // 1초마다 로그 (20 ticks = 1초)

    /**
     * 멀티플레이 게임 틱 (100ms마다 실행)
     *
     * 모든 멀티플레이 세션을 순회하며 자동 중력 적용
     * GameSession.applyGravity()가 내부적으로 틱 간격을 체크하므로
     * 이 메서드는 단순히 모든 플레이어를 순회하면 됨
     * 
     * 부하 감소를 위해 50ms -> 100ms로 변경 (10 TPS)
     */
    @Scheduled(fixedRate = 100) // 100ms = 10 ticks/sec
    public void tick() {
        long currentTime = System.currentTimeMillis();
        tickCount++;
        boolean shouldLog = (tickCount % LOG_INTERVAL == 0);

        // 1. 모든 멀티플레이 세션 조회
        Map<String, GameSession> multiSessions = sessionManager.getMultiplayerSessions();

        if (multiSessions.isEmpty()) {
            return; // 멀티플레이 세션이 없으면 스킵
        }

        // 2. 각 세션의 모든 플레이어에 대해 중력 적용
        for (Map.Entry<String, GameSession> entry : multiSessions.entrySet()) {
            String sessionId = entry.getKey();
            GameSession session = entry.getValue();

            // 게임이 시작되지 않았으면 스킵
            if (!session.isGameStarted()) {
                if (shouldLog) {
                    System.out.println("⏸️ [GameTickService] Session not started yet: " + sessionId);
                }
                continue;
            }
            
            // 활성 플레이어가 없으면 스킵 (모두 연결 끊김)
            if (!session.hasActivePlayers()) {
                // 로그 스팸 방지: 10초마다 한 번씩만 출력 (20 ticks/sec * 10 sec = 200 ticks)
                if (tickCount % 200 == 0) {
                    System.out.println("⏸️ [GameTickService] Session paused (no active players): " + sessionId);
                }
                continue;
            }

            // 세션의 모든 플레이어 조회
            List<String> playerIds = session.getPlayerIds();

            if (shouldLog) {
                System.out.println("🔄 [GameTickService] Processing session: " + sessionId +
                    " with " + playerIds.size() + " players");
            }

            for (String playerId : playerIds) {
                try {
                    // 자동 중력 적용
                    ServerStateDto stateUpdate = session.applyGravity(playerId, currentTime, gameStateMapper);

                    // 상태가 변경된 경우에만 브로드캐스트
                    if (stateUpdate != null) {
                        // 1. 해당 플레이어(Active)에게 업데이트 전송 (통합된 토픽 사용)
                        messagingTemplate.convertAndSendToUser(
                            playerId,
                            "/topic/game/state",
                            stateUpdate
                        );

                        if (shouldLog) {
                            System.out.println("⏬ [GameTickService] Gravity update sent: Session=" + sessionId +
                                ", Player=" + playerId);
                        }

                        // 2. 상대방(Passive)에게도 업데이트 전송 (부드러운 움직임을 위해)
                        String opponentId = playerIds.stream()
                            .filter(id -> !id.equals(playerId))
                            .findFirst()
                            .orElse(null);

                        if (opponentId != null) {
                            // Opponent 기준 DTO 생성 (GameStateDto Swap)
                            ServerStateDto opponentUpdate = ServerStateDto.builder()
                                .lastProcessedSequence(0)
                                .myGameState(stateUpdate.getOpponentGameState()) // 상대 입장에서의 나 = 원래 상대
                                .opponentGameState(stateUpdate.getMyGameState()) // 상대 입장에서의 상대 = 원래 나 (움직인 사람)
                                .events(stateUpdate.getEvents())
                                .attackLinesReceived(0)
                                .gameOver(stateUpdate.isGameOver()) // 게임 오버 상태도 전달
                                .build();

                            messagingTemplate.convertAndSendToUser(
                                opponentId,
                                "/topic/game/state",
                                opponentUpdate
                            );
                        }
                    }
                } catch (Exception e) {
                    System.err.println("❌ [GameTickService] Error applying gravity for player " + playerId + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
}
