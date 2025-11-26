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
    public static SessionCreateResponse success(String sessionId, String websocketUrl) {
        return new SessionCreateResponse(sessionId, websocketUrl, true, null);
    }

    /**
     * 실패 응답 생성
     */
    public static SessionCreateResponse failure(String errorMessage) {
        return new SessionCreateResponse(null, null, false, errorMessage);
    }
}
