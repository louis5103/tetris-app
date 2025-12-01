package seoultech.se.server.config;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
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
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

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
        // WebSocket 메시지 크기 제한 설정 (GameState JSON 전송용)
        // 대용량 데이터 전송을 위해 넉넉하게 설정 (10MB)
        registration.setMessageSizeLimit(10 * 1024 * 1024);       // 10MB (수신 제한)
        registration.setSendBufferSizeLimit(10 * 1024 * 1024);    // 10MB (송신 버퍼)
        registration.setSendTimeLimit(60 * 1000);                 // 60초 (전송 타임아웃)
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

    /**
     * Tomcat WebSocket 컨테이너 커스터마이저
     *
     * Embedded Tomcat에서 WebSocket 버퍼 크기를 설정하는 올바른 방법입니다.
     * ServletServerContainerFactoryBean은 런타임에 적용되지 않으므로
     * WebServerFactoryCustomizer를 사용해야 합니다.
     *
     * CloseStatus code=1009 (message too big) 오류 해결
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers((TomcatConnectorCustomizer) connector -> {
            connector.setProperty("org.apache.tomcat.websocket.textBufferSize", "1048576");  // 1MB
            connector.setProperty("org.apache.tomcat.websocket.binaryBufferSize", "1048576"); // 1MB

            System.out.println("✅ [WebSocket] Tomcat connector configured with 1MB buffer size");
        });
    }
}
