package seoultech.se.server.websocket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import seoultech.se.server.game.GameSessionManager;

/**
 * Phase 1: WebSocket 연결/끊김 이벤트 리스너
 *
 * 기능:
 * - 플레이어 연결/끊김 감지
 * - 30초 재연결 유예 기간 제공
 * - 유예 기간 후 세션에서 플레이어 제거
 *
 * 구현:
 * - SessionConnectEvent: 연결 시 플레이어 ID 저장
 * - SessionDisconnectEvent: 끊김 시 유예 시간 스케줄링
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final GameSessionManager gameSessionManager;

    /**
     * WebSocket 세션 ID → 플레이어 ID 매핑
     * 연결 끊김 시 플레이어를 식별하기 위해 사용
     */
    private final Map<String, String> sessionPlayerMap = new ConcurrentHashMap<>();

    /**
     * 플레이어 ID → 게임 세션 ID 매핑
     * 연결 끊김 시 어느 게임 세션에서 제거할지 알기 위해 사용
     */
    private final Map<String, String> playerSessionMap = new ConcurrentHashMap<>();

    /**
     * 연결 끊김 유예 시간 (밀리초)
     * 30초 내에 재연결하면 게임 세션 유지
     */
    private static final long DISCONNECT_GRACE_PERIOD_MS = 30000; // 30 seconds

    /**
     * Phase 1: WebSocket 연결 이벤트 처리
     *
     * @param event 연결 이벤트
     */
    @EventListener
    public void handleWebSocketConnect(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String playerId = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;

        if (playerId != null && sessionId != null) {
            sessionPlayerMap.put(sessionId, playerId);
            
            // 플레이어 재연결 처리 (오프라인 상태 해제)
            String gameSessionId = playerSessionMap.get(playerId);
            if (gameSessionId != null) {
                gameSessionManager.setPlayerOnline(gameSessionId, playerId, true);
                log.info("✅ [WebSocket] Player reconnected: {} (session: {}, game session: {})", 
                    playerId, sessionId, gameSessionId);
            } else {
                log.info("✅ [WebSocket] Player connected: {} (session: {})", playerId, sessionId);
            }
        }
    }

    /**
     * Phase 1: WebSocket 연결 끊김 이벤트 처리
     *
     * 30초 유예 기간 제공:
     * - 30초 내 재연결 → 게임 계속
     * - 30초 초과 → 세션에서 플레이어 제거
     *
     * @param event 연결 끊김 이벤트
     */
    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String playerId = sessionPlayerMap.get(sessionId);

        if (playerId != null) {
            log.warn("⚠️ [WebSocket] Player disconnected: {} (session: {}). Grace period: {} seconds",
                playerId, sessionId, DISCONNECT_GRACE_PERIOD_MS / 1000);
            
            // 플레이어를 오프라인으로 표시 (게임 틱 일시정지용)
            String gameSessionId = playerSessionMap.get(playerId);
            if (gameSessionId != null) {
                gameSessionManager.setPlayerOnline(gameSessionId, playerId, false);
            }

            // 30초 유예 기간 스케줄링
            schedulePlayerRemoval(sessionId, playerId);
        }
    }

    /**
     * Phase 1: 플레이어 제거 스케줄링
     *
     * 30초 후에 플레이어가 여전히 연결되지 않았으면 세션에서 제거
     *
     * @param sessionId WebSocket 세션 ID
     * @param playerId 플레이어 ID
     */
    private void schedulePlayerRemoval(String sessionId, String playerId) {
        new Thread(() -> {
            try {
                Thread.sleep(DISCONNECT_GRACE_PERIOD_MS);

                // 유예 기간 후에도 여전히 끊겨있으면 제거
                if (!sessionPlayerMap.containsKey(sessionId)) {
                    String gameSessionId = playerSessionMap.remove(playerId);

                    if (gameSessionId != null) {
                        // GameSessionManager를 통해 플레이어 제거
                        boolean removed = gameSessionManager.removePlayerFromSession(gameSessionId, playerId);

                        if (removed) {
                            log.info("⏰ [WebSocket] Grace period expired. Player {} removed from game session {}",
                                playerId, gameSessionId);
                        } else {
                            log.warn("⚠️ [WebSocket] Failed to remove player {} from game session {} (session may not exist)",
                                playerId, gameSessionId);
                        }
                    }
                }
            } catch (InterruptedException e) {
                log.error("❌ [WebSocket] Error in grace period scheduler", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Phase 1: 플레이어와 게임 세션 연결
     *
     * 게임 세션 참여 시 호출하여 끊김 처리에 사용
     *
     * @param playerId 플레이어 ID
     * @param gameSessionId 게임 세션 ID
     */
    public void registerPlayerSession(String playerId, String gameSessionId) {
        playerSessionMap.put(playerId, gameSessionId);
        log.debug("🔗 [WebSocket] Player {} linked to game session {}", playerId, gameSessionId);
    }

    /**
     * Phase 1: 플레이어와 게임 세션 연결 해제
     *
     * 게임 세션 종료 시 호출
     *
     * @param playerId 플레이어 ID
     */
    public void unregisterPlayerSession(String playerId) {
        String gameSessionId = playerSessionMap.remove(playerId);
        if (gameSessionId != null) {
            log.debug("🔓 [WebSocket] Player {} unlinked from game session {}", playerId, gameSessionId);
        }
    }
}
