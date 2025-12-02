package seoultech.se.server.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import seoultech.se.server.game.GameSessionManager;
import seoultech.se.server.matchmaking.MatchmakingService;

/**
 * Phase 3: 연결 상태 및 헬스체크 API
 *
 * 기능:
 * - 서버 상태 확인
 * - 활성 세션 수 조회
 * - 매칭 큐 상태 조회
 * - 클라이언트 연결 상태 모니터링용
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final GameSessionManager gameSessionManager;

    /**
     * Phase 3: 서버 헬스체크
     *
     * @return 서버 상태 정보
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("activeSessions", gameSessionManager.getActiveSessionCount());

        return ResponseEntity.ok(health);
    }

    /**
     * Phase 3: 서버 상세 상태
     *
     * @return 상세 서버 상태 정보
     */
    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealth() {
        Map<String, Object> health = new HashMap<>();

        // 기본 정보
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        // 게임 세션 정보
        Map<String, Object> gameInfo = new HashMap<>();
        gameInfo.put("activeSessions", gameSessionManager.getActiveSessionCount());
        health.put("game", gameInfo);

        // 시스템 정보
        Map<String, Object> systemInfo = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        systemInfo.put("totalMemory", runtime.totalMemory());
        systemInfo.put("freeMemory", runtime.freeMemory());
        systemInfo.put("maxMemory", runtime.maxMemory());
        systemInfo.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        systemInfo.put("processors", runtime.availableProcessors());
        health.put("system", systemInfo);

        return ResponseEntity.ok(health);
    }

    /**
     * Phase 3: WebSocket 연결 상태 확인
     *
     * @return WebSocket 연결 가능 여부
     */
    @GetMapping("/websocket")
    public ResponseEntity<Map<String, Object>> websocketStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("available", true);
        status.put("endpoint", "/ws-game");
        status.put("protocols", new String[]{"v10.stomp", "v11.stomp", "v12.stomp"});

        return ResponseEntity.ok(status);
    }
}
