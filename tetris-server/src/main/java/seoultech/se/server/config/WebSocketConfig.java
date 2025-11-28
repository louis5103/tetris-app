package seoultech.se.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import lombok.RequiredArgsConstructor;
import seoultech.se.server.config.JwtUtil;


@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // 구독 경로 (server -> client)
        config.setApplicationDestinationPrefixes("/app"); // 발행 경로 (client -> server)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-game")
        .setAllowedOrigins("*"); // 개발용 로컬호스트 허용
        // SockJS 제거 - JavaFX 데스크톱 앱은 순수 WebSocket 사용
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // WebSocket 메시지 크기 제한 설정
        registration.setMessageSizeLimit(512 * 1024);        // 512KB (기본값: 64KB)
        registration.setSendBufferSizeLimit(1024 * 1024);    // 1MB (기본값: 512KB)
        registration.setSendTimeLimit(20000);                // 20초 (기본값: 10초)
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    System.out.println("🔐 [WebSocket] CONNECT with token: " + (token != null ? "present" : "null"));

                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                        try {
                            String email = jwtUtil.extractEmail(token);
                            accessor.setUser(() -> email);
                            System.out.println("✅ [WebSocket] User set: " + email);
                        } catch (Exception e) {
                            System.err.println("❌ [WebSocket] Invalid JWT: " + e.getMessage());
                            throw new IllegalArgumentException("Invalid JWT Token");
                        }
                    }
                } else {
                    // CONNECT 이후의 메시지에서는 세션의 User 정보 확인
                    if (accessor.getUser() == null) {
                        System.out.println("⚠️ [WebSocket] " + accessor.getCommand() + " - Principal is null");
                    }
                }

                return message;
            }
        });
    }
}
