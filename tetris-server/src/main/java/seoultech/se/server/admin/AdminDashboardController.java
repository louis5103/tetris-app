package seoultech.se.server.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import seoultech.se.server.admin.AdminDashboardService.DashboardOverview;
import seoultech.se.server.admin.AdminDashboardService.HourlyStats;
import seoultech.se.server.game.GameSessionManager;

/**
 * 관리자 대시보드 REST API
 *
 * 엔드포인트:
 * - GET /api/admin/dashboard/overview: 대시보드 개요
 * - GET /api/admin/dashboard/stats/hourly: 시간대별 통계
 * - GET /api/admin/sessions: 활성 세션 목록
 * - DELETE /api/admin/sessions/{sessionId}: 세션 강제 종료
 * - GET /api/admin/system/status: 시스템 상태
 * - POST /api/admin/system/gc: 가비지 컬렉션 실행
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;
    private final GameSessionManager gameSessionManager;

    /**
     * 대시보드 개요 조회
     *
     * @return 서버 전체 통계
     */
    @GetMapping("/dashboard/overview")
    public ResponseEntity<DashboardOverview> getDashboardOverview() {
        log.info("📊 [Admin] Dashboard overview requested");
        DashboardOverview overview = dashboardService.getOverview();
        return ResponseEntity.ok(overview);
    }

    /**
     * 시간대별 통계 조회
     *
     * @param hours 조회할 시간 수 (기본: 24시간)
     * @return 시간대별 통계 리스트
     */
    @GetMapping("/dashboard/stats/hourly")
    public ResponseEntity<List<HourlyStats>> getHourlyStats(
        @RequestParam(defaultValue = "24") int hours
    ) {
        log.info("📊 [Admin] Hourly stats requested for {} hours", hours);
        List<HourlyStats> stats = dashboardService.getHourlyStats(hours);
        return ResponseEntity.ok(stats);
    }

    /**
     * 활성 세션 목록 조회
     *
     * @return 활성 세션 정보
     */
    @GetMapping("/sessions")
    public ResponseEntity<Map<String, Object>> getActiveSessions() {
        log.info("📊 [Admin] Active sessions list requested");

        Map<String, Object> response = new HashMap<>();
        response.put("activeSessionCount", gameSessionManager.getActiveSessionCount());
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * 세션 강제 종료
     *
     * @param sessionId 종료할 세션 ID
     * @return 종료 결과
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> terminateSession(@PathVariable String sessionId) {
        log.warn("⚠️ [Admin] Force terminating session: {}", sessionId);

        gameSessionManager.removeSession(sessionId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Session terminated successfully");
        response.put("sessionId", sessionId);

        return ResponseEntity.ok(response);
    }

    /**
     * 시스템 상태 조회
     *
     * @return 시스템 상태 정보
     */
    @GetMapping("/system/status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        log.info("📊 [Admin] System status requested");

        Map<String, Object> status = new HashMap<>();
        status.put("status", dashboardService.getSystemStatus());
        status.put("memoryUsage", dashboardService.getMemoryUsagePercentage());
        status.put("activeSessionCount", gameSessionManager.getActiveSessionCount());
        status.put("timestamp", System.currentTimeMillis());

        // JVM 정보
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvmInfo = new HashMap<>();
        jvmInfo.put("totalMemory", runtime.totalMemory());
        jvmInfo.put("freeMemory", runtime.freeMemory());
        jvmInfo.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        jvmInfo.put("maxMemory", runtime.maxMemory());
        jvmInfo.put("processors", runtime.availableProcessors());
        status.put("jvm", jvmInfo);

        return ResponseEntity.ok(status);
    }

    /**
     * 가비지 컬렉션 수동 실행
     *
     * 주의: 프로덕션 환경에서는 신중하게 사용
     *
     * @return GC 실행 결과
     */
    @GetMapping("/system/gc")
    public ResponseEntity<Map<String, Object>> runGarbageCollection() {
        log.warn("⚠️ [Admin] Manual GC requested");

        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();

        System.gc();

        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryFreed = memoryBefore - memoryAfter;

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("memoryBefore", memoryBefore);
        response.put("memoryAfter", memoryAfter);
        response.put("memoryFreed", memoryFreed);
        response.put("timestamp", System.currentTimeMillis());

        log.info("🧹 [Admin] GC completed. Memory freed: {} bytes", memoryFreed);

        return ResponseEntity.ok(response);
    }

    /**
     * 모든 세션 강제 종료 (긴급 상황용)
     *
     * @return 종료 결과
     */
    @DeleteMapping("/sessions/all")
    public ResponseEntity<Map<String, Object>> terminateAllSessions() {
        log.error("🚨 [Admin] EMERGENCY: Terminating all sessions");

        int sessionCount = gameSessionManager.getActiveSessionCount();
        gameSessionManager.clearAllSessions();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "All sessions terminated");
        response.put("terminatedCount", sessionCount);
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    /**
     * 대시보드 실시간 메트릭스 (WebSocket 대신 폴링용)
     *
     * @return 실시간 메트릭스
     */
    @GetMapping("/dashboard/metrics/realtime")
    public ResponseEntity<Map<String, Object>> getRealtimeMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // 현재 상태
        metrics.put("activeSessionCount", gameSessionManager.getActiveSessionCount());
        metrics.put("memoryUsage", dashboardService.getMemoryUsagePercentage());
        metrics.put("systemStatus", dashboardService.getSystemStatus());
        metrics.put("timestamp", System.currentTimeMillis());

        // 메모리 정보
        Runtime runtime = Runtime.getRuntime();
        metrics.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        metrics.put("totalMemory", runtime.totalMemory());

        return ResponseEntity.ok(metrics);
    }
}
