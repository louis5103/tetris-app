package seoultech.se.client.service;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import seoultech.se.backend.dto.SessionCreateRequest;
import seoultech.se.backend.dto.SessionCreateResponse;
import seoultech.se.backend.network.NetworkGameClient;
import seoultech.se.backend.network.NetworkTemplate;
import seoultech.se.core.config.GameplayType;

/**
 * 멀티플레이 매칭 서비스
 *
 * 책임:
 * - 서버 연결 관리
 * - 세션 생성 및 매칭
 * - GameController에 멀티플레이 모드 설정
 *
 * 사용 흐름:
 * 1. startMatching() 호출 → 서버 연결 시도
 * 2. 매칭 성공 → onMatchSuccess 콜백 호출
 * 3. GameController.setupMultiplayMode(sessionId) 호출
 */
@Service
public class MultiplayerMatchingService {

    @Autowired(required = false)
    private NetworkTemplate networkTemplate;

    @Autowired(required = false)
    private NetworkGameClient networkGameClient;

    private final RestTemplate restTemplate = new RestTemplate();

    private String currentSessionId;
    private Consumer<String> onMatchSuccessCallback;
    private Consumer<String> onMatchFailCallback;

    /**
     * 매칭 시작
     *
     * @param serverBaseUrl 서버 기본 URL (예: "http://localhost:8080")
     * @param jwtToken JWT 인증 토큰
     * @param onSuccess 매칭 성공 시 콜백 (sessionId 전달)
     * @param onFail 매칭 실패 시 콜백 (에러 메시지 전달)
     */
    public void startMatching(
            String serverBaseUrl,
            String jwtToken,
            Consumer<String> onSuccess,
            Consumer<String> onFail) {

        this.onMatchSuccessCallback = onSuccess;
        this.onMatchFailCallback = onFail;

        if (networkTemplate == null) {
            notifyFailure("NetworkTemplate not available. Check backend dependencies.");
            return;
        }

        try {
            System.out.println("🔍 [MatchingService] Starting matching...");
            System.out.println("   - Server URL: " + serverBaseUrl);

            // 1. 세션 생성 API 호출
            SessionCreateRequest request = new SessionCreateRequest();
            request.setGameplayType(GameplayType.CLASSIC);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (jwtToken != null && !jwtToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + jwtToken);
            }

            HttpEntity<SessionCreateRequest> httpRequest = new HttpEntity<>(request, headers);

            String sessionApiUrl = serverBaseUrl + "/api/session/create";
            System.out.println("📡 [MatchingService] Calling session API: " + sessionApiUrl);

            SessionCreateResponse response = restTemplate.postForObject(
                sessionApiUrl,
                httpRequest,
                SessionCreateResponse.class
            );

            if (response == null || !response.isSuccess()) {
                String errorMsg = response != null ? response.getErrorMessage() : "No response from server";
                notifyFailure("Session creation failed: " + errorMsg);
                return;
            }

            currentSessionId = response.getSessionId();
            System.out.println("✅ [MatchingService] Session created: " + currentSessionId);

            // 2. WebSocket 연결
            String websocketUrl = serverBaseUrl.replace("http://", "ws://")
                .replace("https://", "wss://") + response.getWebsocketUrl();

            System.out.println("🔌 [MatchingService] Connecting to WebSocket: " + websocketUrl);
            networkTemplate.connect(websocketUrl, jwtToken);

            System.out.println("✅ [MatchingService] WebSocket connected");

            // 3. 매칭 성공 콜백 호출
            notifySuccess(currentSessionId);

        } catch (Exception e) {
            System.err.println("❌ [MatchingService] Matching failed: " + e.getMessage());
            e.printStackTrace();
            notifyFailure("Failed to connect to server: " + e.getMessage());
        }
    }

    /**
     * 매칭 취소
     *
     * @param serverBaseUrl 서버 기본 URL
     */
    public void cancelMatching(String serverBaseUrl) {
        if (currentSessionId == null) {
            System.out.println("⚠️ [MatchingService] No active session to cancel");
            return;
        }

        try {
            System.out.println("🛑 [MatchingService] Cancelling matching...");
            System.out.println("   - Session ID: " + currentSessionId);

            // 서버에 세션 삭제 요청
            String deleteUrl = serverBaseUrl + "/api/session/" + currentSessionId;
            restTemplate.delete(deleteUrl);

            System.out.println("✅ [MatchingService] Session deleted on server");

        } catch (Exception e) {
            System.err.println("❌ [MatchingService] Failed to cancel on server: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 로컬 상태 정리
            currentSessionId = null;
            System.out.println("🛑 [MatchingService] Matching cancelled");
        }
    }

    /**
     * 매칭 성공 알림
     */
    private void notifySuccess(String sessionId) {
        if (onMatchSuccessCallback != null) {
            onMatchSuccessCallback.accept(sessionId);
        }
    }

    /**
     * 매칭 실패 알림
     */
    private void notifyFailure(String errorMessage) {
        if (onMatchFailCallback != null) {
            onMatchFailCallback.accept(errorMessage);
        }
    }

    /**
     * 현재 세션 ID 반환
     */
    public String getCurrentSessionId() {
        return currentSessionId;
    }

    /**
     * 연결 종료
     */
    public void disconnect() {
        if (networkGameClient != null) {
            networkGameClient.disconnect();
        }
        currentSessionId = null;
        System.out.println("🔌 [MatchingService] Disconnected");
    }
}
