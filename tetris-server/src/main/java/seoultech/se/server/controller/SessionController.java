package seoultech.se.server.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import seoultech.se.core.config.GameplayType;
import seoultech.se.server.dto.SessionCreateRequest;
import seoultech.se.server.dto.SessionCreateResponse;
import seoultech.se.server.game.GameSession;
import seoultech.se.server.game.GameSessionManager;

/**
 * 세션 관리 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
public class SessionController {

    private final GameSessionManager gameSessionManager;

    /**
     * 새로운 게임 세션 생성
     *
     * POST /api/session/create
     *
     * @param request 세션 생성 요청 (gameplayType 포함)
     * @return 세션 ID와 WebSocket URL
     */
    @PostMapping("/create")
    public ResponseEntity<SessionCreateResponse> createSession(@RequestBody SessionCreateRequest request) {
        try {
            // 1. 세션 ID 생성 (UUID 기반)
            String sessionId = "session-" + UUID.randomUUID().toString();

            // 2. GameplayType 설정 (기본값: CLASSIC)
            GameplayType gameplayType = request.getGameplayType();
            if (gameplayType == null) {
                gameplayType = GameplayType.CLASSIC;
            }

            // 3. 세션 생성
            GameSession session = gameSessionManager.createSession(sessionId, gameplayType);

            // 4. WebSocket URL 생성
            String websocketUrl = "/game"; // STOMP endpoint

            // 5. 응답 생성
            SessionCreateResponse response = SessionCreateResponse.success(sessionId, websocketUrl);

            System.out.println("✅ [SessionController] Session created: " + sessionId +
                ", GameplayType: " + gameplayType);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ [SessionController] Failed to create session: " + e.getMessage());
            e.printStackTrace();

            SessionCreateResponse response = SessionCreateResponse.failure(
                "Failed to create session: " + e.getMessage()
            );
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 세션 삭제 (매칭 취소)
     *
     * DELETE /api/session/{sessionId}
     *
     * @param sessionId 삭제할 세션 ID
     * @return 성공 여부
     */
    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        try {
            gameSessionManager.removeSession(sessionId);
            System.out.println("✅ [SessionController] Session deleted: " + sessionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("❌ [SessionController] Failed to delete session: " + e.getMessage());
            return ResponseEntity.status(500).build();
        }
    }
}
