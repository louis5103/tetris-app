package seoultech.se.client.config;

import lombok.Getter;
import lombok.Setter;

/**
 * 멀티플레이 서버 설정
 */
@Getter
@Setter
public class ServerSettings {
    /**
     * 서버 기본 URL (예: "http://localhost:8090")
     */
    private String baseUrl = "http://localhost:8090";

    /**
     * WebSocket 엔드포인트 (예: "/ws-game")
     */
    private String websocketEndpoint = "/ws-game";
}
