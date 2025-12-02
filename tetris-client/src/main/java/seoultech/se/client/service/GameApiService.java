package seoultech.se.client.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import seoultech.se.core.dto.ServerStateDto;

/**
 * 게임 API 서비스
 *
 * 책임:
 * - 게임 세션 관련 HTTP 통신
 * - 게임 시작/종료 트리거
 */
@Service
public class GameApiService {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Autowired
    private AuthService authService;

    public GameApiService(@Value("${tetris.auth.base-url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }

    /**
     * 게임 시작 트리거 (멀티플레이용)
     *
     * 서버에 게임 세션 시작을 알려 GameTickService가 동작하도록 함
     *
     * @param sessionId 게임 세션 ID
     */
    public void startGame(String sessionId) {
        String url = baseUrl + "/api/game/start/" + sessionId;

        try {
            HttpHeaders headers = new HttpHeaders();
            String token = authService.getCurrentToken();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            System.out.println("📡 [GameApiService] Calling start game API: " + url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );

            System.out.println("✅ [GameApiService] Game started successfully: " + response.getBody());

        } catch (Exception e) {
            System.err.println("❌ [GameApiService] Failed to start game: " + e.getMessage());
            e.printStackTrace();
            // 게임 시작은 실패해도 클라이언트 로직은 계속 진행
            // 서버는 입력을 받으면서도 동작할 수 있음
        }
    }

    /**
     * 초기 게임 상태 조회 (멀티플레이용)
     *
     * 게임 시작 시 서버에서 초기 상태를 요청
     *
     * @param sessionId 게임 세션 ID
     * @return ServerStateDto (myGameState, opponentGameState 포함)
     */
    public ServerStateDto getInitialState(String sessionId) {
        String url = baseUrl + "/api/game/state/" + sessionId;

        try {
            HttpHeaders headers = new HttpHeaders();
            String token = authService.getCurrentToken();
            if (token != null && !token.isEmpty()) {
                headers.set("Authorization", "Bearer " + token);
            }

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            System.out.println("📡 [GameApiService] Requesting initial state: " + url);

            ResponseEntity<ServerStateDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ServerStateDto.class
            );

            System.out.println("✅ [GameApiService] Initial state received successfully");
            return response.getBody();

        } catch (Exception e) {
            System.err.println("❌ [GameApiService] Failed to get initial state: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}
