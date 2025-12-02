package seoultech.se.backend.dto;

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
     * 상대방 이름 (매칭 완료 시)
     */
    private String opponentName;

    /**
     * 상대방 이메일 (매칭 완료 시)
     */
    private String opponentEmail;

    /**
     * 매칭 상태 (WAITING, MATCHED)
     */
    private String matchingStatus;

    /**
     * 성공 응답 생성 (매칭 대기 중)
     */
    public static SessionCreateResponse success(String sessionId, String websocketUrl) {
        SessionCreateResponse response = new SessionCreateResponse();
        response.setSessionId(sessionId);
        response.setWebsocketUrl(websocketUrl);
        response.setSuccess(true);
        response.setMatchingStatus("WAITING");
        return response;
    }

    /**
     * 성공 응답 생성 (매칭 완료)
     */
    public static SessionCreateResponse matched(String sessionId, String websocketUrl, String opponentName, String opponentEmail) {
        SessionCreateResponse response = new SessionCreateResponse();
        response.setSessionId(sessionId);
        response.setWebsocketUrl(websocketUrl);
        response.setSuccess(true);
        response.setOpponentName(opponentName);
        response.setOpponentEmail(opponentEmail);
        response.setMatchingStatus("MATCHED");
        return response;
    }

    /**
     * 실패 응답 생성
     */
    public static SessionCreateResponse failure(String errorMessage) {
        SessionCreateResponse response = new SessionCreateResponse();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        return response;
    }
}
