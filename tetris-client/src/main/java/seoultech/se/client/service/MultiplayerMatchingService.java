package seoultech.se.client.service;

import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private Consumer<seoultech.se.backend.dto.MatchFoundNotification> onMatchSuccessCallback;
    private Consumer<String> onMatchFailCallback;
    private boolean isWaitingForMatch = false;

    /**
     * 매칭 시작
     *
     * @param serverBaseUrl 서버 기본 URL (예: "http://localhost:8080")
     * @param jwtToken JWT 인증 토큰
     * @param onSuccess 매칭 성공 시 콜백 (MatchFoundNotification 전달)
     * @param onFail 매칭 실패 시 콜백 (에러 메시지 전달)
     */
    public void startMatching(
            String serverBaseUrl,
            String jwtToken,
            Consumer<seoultech.se.backend.dto.MatchFoundNotification> onSuccess,
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

            // 1. WebSocket 연결 먼저 (매칭 알림을 받기 위해)
            String websocketUrl = serverBaseUrl.replace("http://", "ws://")
                .replace("https://", "wss://") + "/ws-game";

            System.out.println("🔌 [MatchingService] Connecting to WebSocket: " + websocketUrl);
            networkTemplate.connect(websocketUrl, jwtToken);
            System.out.println("✅ [MatchingService] WebSocket connected");

            // 2. 매칭 완료 알림 구독
            isWaitingForMatch = true;
            networkTemplate.subscribeToMatchFound(matchNotification -> {
                System.out.println("🎮 [MatchingService] Match found notification received!");
                System.out.println("   - Session ID: " + matchNotification.getSessionId());
                System.out.println("   - Opponent: " + matchNotification.getOpponentName());
                System.out.println("   - Opponent Email: " + matchNotification.getOpponentEmail());

                if (isWaitingForMatch) {
                    isWaitingForMatch = false;
                    currentSessionId = matchNotification.getSessionId();
                    notifySuccess(matchNotification);
                }
            });

            // 3. 매칭 큐 참여 API 호출
            MatchmakingRequest request = new MatchmakingRequest();
            request.setGameplayType(GameplayType.CLASSIC);
            request.setDifficulty(seoultech.se.core.model.enumType.Difficulty.NORMAL);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (jwtToken != null && !jwtToken.isEmpty()) {
                headers.set("Authorization", "Bearer " + jwtToken);
            }

            HttpEntity<MatchmakingRequest> httpRequest = new HttpEntity<>(request, headers);

            String matchmakingApiUrl = serverBaseUrl + "/api/matchmaking/join";
            System.out.println("📡 [MatchingService] Calling matchmaking API: " + matchmakingApiUrl);

            MatchmakingResponse response = restTemplate.postForObject(
                matchmakingApiUrl,
                httpRequest,
                MatchmakingResponse.class
            );

            if (response == null) {
                notifyFailure("No response from matchmaking server");
                return;
            }

            System.out.println("✅ [MatchingService] Matchmaking response: " + response.getStatus());

            // 4. 즉시 매칭된 경우 (큐에 이미 대기자가 있었던 경우)
            if ("MATCHED".equals(response.getStatus())) {
                currentSessionId = response.getSessionId();
                System.out.println("🎮 [MatchingService] Immediately matched! Session: " + currentSessionId);
                // WebSocket 알림도 올 것이므로 여기서는 처리하지 않음
            } else if ("WAITING".equals(response.getStatus())) {
                System.out.println("⏳ [MatchingService] Waiting for match...");
            } else if ("ALREADY_IN_QUEUE".equals(response.getStatus())) {
                notifyFailure("Already in matchmaking queue");
                return;
            }

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
            isWaitingForMatch = false;
            System.out.println("🛑 [MatchingService] Matching cancelled");
        }
    }

    /**
     * 매칭 성공 알림
     */
    private void notifySuccess(seoultech.se.backend.dto.MatchFoundNotification notification) {
        if (onMatchSuccessCallback != null) {
            onMatchSuccessCallback.accept(notification);
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
     * NetworkExecutionStrategy 생성
     * 
     * @return 새로운 NetworkExecutionStrategy 인스턴스
     */
    public seoultech.se.client.strategy.NetworkExecutionStrategy createNetworkExecutionStrategy() {
        if (networkTemplate == null || networkGameClient == null) {
            throw new IllegalStateException(
                "Network components not available. " +
                "Ensure backend module dependencies are correctly configured."
            );
        }
        return new seoultech.se.client.strategy.NetworkExecutionStrategy(
            networkTemplate,
            networkGameClient
        );
    }

    /**
     * 연결 종료
     */
    public void disconnect() {
        if (networkGameClient != null) {
            networkGameClient.cleanup();
        }
        currentSessionId = null;
        System.out.println("🔌 [MatchingService] Disconnected");
    }

    /**
     * 매칭 요청 DTO
     */
    private static class MatchmakingRequest {
        private GameplayType gameplayType;
        private seoultech.se.core.model.enumType.Difficulty difficulty;

        public GameplayType getGameplayType() {
            return gameplayType;
        }

        public void setGameplayType(GameplayType gameplayType) {
            this.gameplayType = gameplayType;
        }

        public seoultech.se.core.model.enumType.Difficulty getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(seoultech.se.core.model.enumType.Difficulty difficulty) {
            this.difficulty = difficulty;
        }
    }

    /**
     * 매칭 응답 DTO
     */
    private static class MatchmakingResponse {
        private String status;
        private String sessionId;
        private String player1Id;
        private String player2Id;

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getPlayer1Id() {
            return player1Id;
        }

        public void setPlayer1Id(String player1Id) {
            this.player1Id = player1Id;
        }

        public String getPlayer2Id() {
            return player2Id;
        }

        public void setPlayer2Id(String player2Id) {
            this.player2Id = player2Id;
        }
    }
}
