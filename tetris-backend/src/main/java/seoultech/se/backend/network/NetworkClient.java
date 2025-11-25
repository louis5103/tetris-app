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
public class NetworkClient {
    private StompSession session;

    public void connect(String url, String jwtToken) {
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketStompClient stompClient = new WebSocketStompClient(client);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        try{
            this.session = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
                
                @Override
                public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                    System.out.println("Connected to server");
                }

                @Override
                public void handleTransportError(StompSession session, Throwable exception) {
                    System.err.println("Transport error: " + exception.getMessage());
                }
            }, "Authorization", "Bearer " + jwtToken).get();
        } catch(Exception e){
            e.printStackTrace();
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
}
