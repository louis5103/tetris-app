package seoultech.se.backend.network;

import java.lang.reflect.Type;
import java.util.function.Consumer;

import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import seoultech.se.core.dto.PlayerInputDto;
import seoultech.se.core.dto.ServerStateDto;


@Component
public class NetworkTemplate {
    private StompSession session;

    /**
     * Phase 1: 자동 재연결 설정
     */
    private String lastUrl;
    private String lastJwtToken;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long INITIAL_RECONNECT_DELAY_MS = 1000; // 1초
    private volatile boolean isReconnecting = false;

    public void connect(String url, String jwtToken) {
        // Phase 1: 연결 정보 저장 (재연결용)
        this.lastUrl = url;
        this.lastJwtToken = jwtToken;

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        try{
            this.session = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {

                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    // Phase 1: 재연결 성공 시 카운터 리셋
                    reconnectAttempts = 0;
                    isReconnecting = false;
                    System.out.println("✅ Connected to server: " + url);
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    System.err.println("❌ Transport error: " + exception.getMessage());

                    // Phase 1: 연결 끊김 시 자동 재연결 시도
                    attemptReconnect();
                }
            }, "Authorization", "Bearer " + jwtToken).get();
        } catch(Exception e){
            System.err.println("❌ Connection failed: " + e.getMessage());

            // Phase 1: 초기 연결 실패 시에도 재연결 시도
            attemptReconnect();
        }
    }
    public void sendInput(PlayerInputDto input) {
        if (session != null && session.isConnected()) {
            session.send("/app/game/input", input);
        } else {
            System.out.println("Not connected to server");
        }
    }

    public void subscribeToSync(Consumer<ServerStateDto> callback) {
        if (session != null && session.isConnected()) {
            session.subscribe("/user/topic/game/sync", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return ServerStateDto.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    callback.accept((ServerStateDto) payload);
                }
            });
        } else {
            System.out.println("Not connected to server");
        }
    }

    /**
     * Phase 1: 자동 재연결 시도 (Exponential Backoff)
     *
     * 재연결 간격:
     * - 1회: 1초
     * - 2회: 2초
     * - 3회: 4초
     * - 4회: 8초
     * - 5회: 16초
     * - 최대 5회 시도 후 포기
     */
    private void attemptReconnect() {
        // 이미 재연결 중이거나 최대 시도 횟수 초과
        if (isReconnecting || reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
                System.err.println("❌ Max reconnection attempts reached (" + MAX_RECONNECT_ATTEMPTS + "). Giving up.");
            }
            return;
        }

        isReconnecting = true;

        new Thread(() -> {
            reconnectAttempts++;

            // Exponential backoff: 1s → 2s → 4s → 8s → 16s
            long delay = INITIAL_RECONNECT_DELAY_MS * (1L << (reconnectAttempts - 1));

            System.out.println("🔄 Reconnection attempt " + reconnectAttempts + "/" + MAX_RECONNECT_ATTEMPTS +
                " in " + (delay / 1000) + " seconds...");

            try {
                Thread.sleep(delay);

                // 재연결 시도
                if (lastUrl != null && lastJwtToken != null) {
                    connect(lastUrl, lastJwtToken);
                }
            } catch (InterruptedException e) {
                System.err.println("❌ Reconnection interrupted: " + e.getMessage());
                Thread.currentThread().interrupt();
                isReconnecting = false;
            }
        }).start();
    }

    /**
     * Phase 1: 연결 상태 확인
     *
     * @return 연결 여부
     */
    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    /**
     * Phase 1: 재연결 수동 트리거
     */
    public void reconnect() {
        if (lastUrl != null && lastJwtToken != null) {
            reconnectAttempts = 0;
            isReconnecting = false;
            connect(lastUrl, lastJwtToken);
        } else {
            System.err.println("❌ Cannot reconnect: No previous connection info");
        }
    }
    
    /**
     * 연결 종료 및 리소스 정리
     * 
     * 게임 종료 또는 재시작 시 호출되어 WebSocket 연결을 정리합니다.
     */
    public void disconnect() {
        if (session != null) {
            try {
                if (session.isConnected()) {
                    session.disconnect();
                    System.out.println("✅ [NetworkTemplate] Disconnected from server");
                }
            } catch (Exception e) {
                System.err.println("⚠️ [NetworkTemplate] Error during disconnect: " + e.getMessage());
            } finally {
                session = null;
            }
        }
        
        // 재연결 정보도 초기화
        lastUrl = null;
        lastJwtToken = null;
        reconnectAttempts = 0;
        isReconnecting = false;
    }
}
