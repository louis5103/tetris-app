package seoultech.se.server.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import seoultech.se.server.matchmaking.MatchmakingService;
import seoultech.se.server.matchmaking.MatchmakingService.MatchmakingResult;
import seoultech.se.server.matchmaking.MatchmakingService.WaitingPlayer;

/**
 * Phase 2: 매칭 시스템 REST API
 *
 * 엔드포인트:
 * - POST /api/matchmaking/join: 매칭 큐 참여
 * - DELETE /api/matchmaking/leave: 매칭 취소
 * - GET /api/matchmaking/status: 매칭 상태 조회
 * - GET /api/matchmaking/queue-size: 큐 크기 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/matchmaking")
@RequiredArgsConstructor
public class MatchmakingController {

    private final MatchmakingService matchmakingService;

    /**
     * Phase 2: 매칭 큐 참여
     *
     * @param request 매칭 요청
     * @param principal 인증된 사용자 정보
     * @return 매칭 결과
     */
    @PostMapping("/join")
    public ResponseEntity<MatchmakingResponse> joinQueue(
        @Valid @RequestBody MatchmakingRequest request,
        Principal principal
    ) {
        String playerId = principal.getName();

        log.info("📝 [Matchmaking API] Join request from {}: {} / {}",
            playerId, request.getGameplayType(), request.getDifficulty());

        MatchmakingResult result = matchmakingService.joinQueue(
            playerId,
            request.getGameplayType(),
            request.getDifficulty()
        );

        return ResponseEntity.ok(MatchmakingResponse.from(result));
    }

    /**
     * Phase 2: 매칭 취소
     *
     * @param principal 인증된 사용자 정보
     * @return 취소 결과
     */
    @DeleteMapping("/leave")
    public ResponseEntity<LeaveQueueResponse> leaveQueue(Principal principal) {
        String playerId = principal.getName();

        log.info("🚪 [Matchmaking API] Leave request from {}", playerId);

        boolean success = matchmakingService.leaveQueue(playerId);

        return ResponseEntity.ok(new LeaveQueueResponse(success));
    }

    /**
     * Phase 2: 매칭 상태 조회
     *
     * @param principal 인증된 사용자 정보
     * @return 매칭 상태
     */
    @GetMapping("/status")
    public ResponseEntity<MatchmakingStatusResponse> getStatus(Principal principal) {
        String playerId = principal.getName();

        return matchmakingService.getWaitingStatus(playerId)
            .map(waitingPlayer -> ResponseEntity.ok(MatchmakingStatusResponse.waiting(
                waitingPlayer.getGameplayType().name(),
                waitingPlayer.getDifficulty().name(),
                waitingPlayer.getWaitingTimeMs()
            )))
            .orElse(ResponseEntity.ok(MatchmakingStatusResponse.notInQueue()));
    }

    /**
     * Phase 2: 큐 크기 조회
     *
     * @param request 큐 크기 조회 요청
     * @return 큐 크기
     */
    @GetMapping("/queue-size")
    public ResponseEntity<QueueSizeResponse> getQueueSize(@Valid MatchmakingRequest request) {
        int size = matchmakingService.getQueueSize(
            request.getGameplayType(),
            request.getDifficulty()
        );

        return ResponseEntity.ok(new QueueSizeResponse(size));
    }

    // ===== DTOs =====

    /**
     * 매칭 요청 DTO
     */
    public static class MatchmakingRequest {
        private seoultech.se.core.config.GameplayType gameplayType;
        private seoultech.se.core.model.enumType.Difficulty difficulty;

        public seoultech.se.core.config.GameplayType getGameplayType() {
            return gameplayType;
        }

        public void setGameplayType(seoultech.se.core.config.GameplayType gameplayType) {
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
    public static class MatchmakingResponse {
        private String status;
        private String sessionId;
        private String player1Id;
        private String player2Id;

        public static MatchmakingResponse from(MatchmakingResult result) {
            MatchmakingResponse response = new MatchmakingResponse();
            response.status = result.getStatus().name();
            response.sessionId = result.getSessionId();
            response.player1Id = result.getPlayer1Id();
            response.player2Id = result.getPlayer2Id();
            return response;
        }

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

    /**
     * 큐 탈퇴 응답 DTO
     */
    public static class LeaveQueueResponse {
        private boolean success;

        public LeaveQueueResponse(boolean success) {
            this.success = success;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }

    /**
     * 매칭 상태 응답 DTO
     */
    public static class MatchmakingStatusResponse {
        private boolean inQueue;
        private String gameplayType;
        private String difficulty;
        private Long waitingTimeMs;

        public static MatchmakingStatusResponse waiting(String gameplayType, String difficulty, long waitingTimeMs) {
            MatchmakingStatusResponse response = new MatchmakingStatusResponse();
            response.inQueue = true;
            response.gameplayType = gameplayType;
            response.difficulty = difficulty;
            response.waitingTimeMs = waitingTimeMs;
            return response;
        }

        public static MatchmakingStatusResponse notInQueue() {
            MatchmakingStatusResponse response = new MatchmakingStatusResponse();
            response.inQueue = false;
            return response;
        }

        public boolean isInQueue() {
            return inQueue;
        }

        public void setInQueue(boolean inQueue) {
            this.inQueue = inQueue;
        }

        public String getGameplayType() {
            return gameplayType;
        }

        public void setGameplayType(String gameplayType) {
            this.gameplayType = gameplayType;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }

        public Long getWaitingTimeMs() {
            return waitingTimeMs;
        }

        public void setWaitingTimeMs(Long waitingTimeMs) {
            this.waitingTimeMs = waitingTimeMs;
        }
    }

    /**
     * 큐 크기 응답 DTO
     */
    public static class QueueSizeResponse {
        private int queueSize;

        public QueueSizeResponse(int queueSize) {
            this.queueSize = queueSize;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public void setQueueSize(int queueSize) {
            this.queueSize = queueSize;
        }
    }
}
