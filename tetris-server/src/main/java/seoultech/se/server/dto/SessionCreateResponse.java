package seoultech.se.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 세션 생성 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateResponse {
    /**
     * 생성된 세션 ID
     */
    private String sessionId;

    /**
     * WebSocket 연결 URL
     */
    private String websocketUrl;
    
    /**
     * 게임 모드 설정 (세션 조인 시 동기화)
     * 호스트가 설정한 Config를 클라이언트에게 전달
     */
    private SessionConfigDto config;
    
    /**
     * 호스트 플레이어 ID
     */
    private String hostPlayerId;

    /**
     * 성공 여부
     */
    private boolean success;

    /**
     * 에러 메시지 (실패 시)
     */
    private String errorMessage;

    /**
     * 성공 응답 생성
     */
    public static SessionCreateResponse success(String sessionId, String websocketUrl, 
                                                  SessionConfigDto config, String hostPlayerId) {
        return new SessionCreateResponse(sessionId, websocketUrl, config, hostPlayerId, true, null);
    }

    /**
     * 실패 응답 생성
     */
    public static SessionCreateResponse failure(String errorMessage) {
        return new SessionCreateResponse(null, null, null, null, false, errorMessage);
    }
}
