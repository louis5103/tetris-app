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
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.model.enumType.Difficulty;
import seoultech.se.server.dto.SessionConfigDto;
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
     * @param request 세션 생성 요청 (gameplayType, difficulty 포함)
     * @return 세션 ID, WebSocket URL, GameModeConfig, 호스트 ID
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
            
            // 3. Difficulty 설정 (기본값: NORMAL)
            Difficulty difficulty = request.getDifficulty();
            if (difficulty == null) {
                difficulty = Difficulty.NORMAL;
            }

            // 4. 세션 생성 (GameplayType + Difficulty → GameModeConfig)
            GameSession session = gameSessionManager.createSession(sessionId, gameplayType, difficulty);

            // 5. 플레이어 참여 (첫 플레이어가 호스트)
            String playerId = request.getPlayerId();
            if (playerId != null && !playerId.isEmpty()) {
                session.joinPlayer(playerId);
            }

            // 6. WebSocket URL 생성
            String websocketUrl = "/ws-game"; // STOMP endpoint

            // 7. Config를 DTO로 변환
            GameModeConfig config = session.getGameModeConfig();
            SessionConfigDto configDto = SessionConfigDto.fromGameModeConfig(config);

            // 8. 응답 생성
            SessionCreateResponse response = SessionCreateResponse.success(
                sessionId, 
                websocketUrl, 
                configDto,
                session.getHostPlayerId()
            );

            System.out.println("✅ [SessionController] Session created: " + sessionId +
                ", GameplayType: " + gameplayType + ", Difficulty: " + difficulty +
                ", Host: " + session.getHostPlayerId());

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
     * 세션 Config 업데이트 (호스트만 가능)
     *
     * POST /api/session/{sessionId}/config
     *
     * @param sessionId 세션 ID
     * @param request Config DTO (호스트의 playerId 포함)
     * @return 업데이트된 Config
     */
    @PostMapping("/{sessionId}/config")
    public ResponseEntity<SessionConfigDto> updateSessionConfig(
            @PathVariable String sessionId,
            @RequestBody ConfigUpdateRequest request) {
        try {
            // 1. 세션 조회
            GameSession session = gameSessionManager.getSession(sessionId);
            if (session == null) {
                return ResponseEntity.notFound().build();
            }

            // 2. DTO → GameModeConfig 변환
            GameModeConfig newConfig = request.getConfig().toGameModeConfig();

            // 3. Config 업데이트 (호스트 검증 포함)
            session.setGameModeConfig(request.getPlayerId(), newConfig);

            // 4. 업데이트된 Config 반환
            SessionConfigDto updatedDto = SessionConfigDto.fromGameModeConfig(session.getGameModeConfig());

            System.out.println("✅ [SessionController] Config updated for session: " + sessionId +
                " by player: " + request.getPlayerId());

            return ResponseEntity.ok(updatedDto);

        } catch (IllegalStateException e) {
            System.err.println("❌ [SessionController] Config update failed: " + e.getMessage());
            return ResponseEntity.status(403).build(); // Forbidden
        } catch (Exception e) {
            System.err.println("❌ [SessionController] Unexpected error: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
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
    
    /**
     * Config 업데이트 요청 DTO (내부 클래스)
     */
    @lombok.Data
    public static class ConfigUpdateRequest {
        private String playerId;
        private SessionConfigDto config;
    }
}
