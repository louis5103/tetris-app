package seoultech.se.server.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import seoultech.se.server.game.GameSessionManager;
import seoultech.se.server.user.UserRepository;

/**
 * 관리자 대시보드 서비스
 *
 * 기능:
 * - 실시간 서버 통계
 * - 게임 세션 모니터링
 * - 사용자 통계
 * - 시스템 메트릭스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final GameSessionManager gameSessionManager;
    private final UserRepository userRepository;

    /**
     * 실시간 통계 데이터
     */
    private final AtomicLong totalMatchesCreated = new AtomicLong(0);
    private final AtomicLong totalGamesPlayed = new AtomicLong(0);
    private final Map<String, Long> hourlyStats = new ConcurrentHashMap<>();

    /**
     * 서버 시작 시간
     */
    private final long serverStartTime = System.currentTimeMillis();

    /**
     * 대시보드 개요 통계
     *
     * @return 서버 전체 통계
     */
    public DashboardOverview getOverview() {
        DashboardOverview overview = new DashboardOverview();

        // 기본 정보
        overview.setServerUptime(System.currentTimeMillis() - serverStartTime);
        overview.setActiveSessionCount(gameSessionManager.getActiveSessionCount());
        overview.setTotalUsersRegistered(userRepository.count());

        // 게임 통계
        overview.setTotalMatchesCreated(totalMatchesCreated.get());
        overview.setTotalGamesPlayed(totalGamesPlayed.get());

        // 시스템 메트릭스
        Runtime runtime = Runtime.getRuntime();
        overview.setTotalMemory(runtime.totalMemory());
        overview.setFreeMemory(runtime.freeMemory());
        overview.setUsedMemory(runtime.totalMemory() - runtime.freeMemory());
        overview.setMaxMemory(runtime.maxMemory());
        overview.setCpuCores(runtime.availableProcessors());

        return overview;
    }

    /**
     * 시간대별 통계
     *
     * @param hours 조회할 시간 수 (기본: 24시간)
     * @return 시간대별 통계 리스트
     */
    public List<HourlyStats> getHourlyStats(int hours) {
        List<HourlyStats> stats = new ArrayList<>();
        long currentHour = System.currentTimeMillis() / (1000 * 60 * 60);

        for (int i = hours - 1; i >= 0; i--) {
            long hourKey = currentHour - i;
            String hourLabel = String.valueOf(hourKey);

            HourlyStats stat = new HourlyStats();
            stat.setHour(hourLabel);
            stat.setMatchCount(hourlyStats.getOrDefault(hourLabel, 0L));
            stats.add(stat);
        }

        return stats;
    }

    /**
     * 매칭 생성 기록
     */
    public void recordMatchCreated() {
        totalMatchesCreated.incrementAndGet();
        String currentHour = String.valueOf(System.currentTimeMillis() / (1000 * 60 * 60));
        hourlyStats.merge(currentHour, 1L, Long::sum);
        log.debug("📊 [Dashboard] Match created. Total: {}", totalMatchesCreated.get());
    }

    /**
     * 게임 완료 기록
     */
    public void recordGameCompleted() {
        totalGamesPlayed.incrementAndGet();
        log.debug("📊 [Dashboard] Game completed. Total: {}", totalGamesPlayed.get());
    }

    /**
     * 메모리 사용률 계산
     *
     * @return 메모리 사용률 (0.0 ~ 1.0)
     */
    public double getMemoryUsagePercentage() {
        Runtime runtime = Runtime.getRuntime();
        long used = runtime.totalMemory() - runtime.freeMemory();
        long max = runtime.maxMemory();
        return (double) used / max;
    }

    /**
     * 시스템 상태 확인
     *
     * @return 시스템 상태 (HEALTHY, WARNING, CRITICAL)
     */
    public String getSystemStatus() {
        double memoryUsage = getMemoryUsagePercentage();

        if (memoryUsage > 0.9) {
            return "CRITICAL";
        } else if (memoryUsage > 0.7) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }

    /**
     * 오래된 통계 데이터 정리 (매일 자정)
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void cleanupOldStats() {
        long currentHour = System.currentTimeMillis() / (1000 * 60 * 60);
        long oneDayAgo = currentHour - 24;

        hourlyStats.entrySet().removeIf(entry -> {
            long hour = Long.parseLong(entry.getKey());
            return hour < oneDayAgo;
        });

        log.info("🧹 [Dashboard] Cleaned up old hourly statistics");
    }

    /**
     * 대시보드 개요 DTO
     */
    public static class DashboardOverview {
        private long serverUptime;
        private int activeSessionCount;
        private long totalUsersRegistered;
        private long totalMatchesCreated;
        private long totalGamesPlayed;
        private long totalMemory;
        private long freeMemory;
        private long usedMemory;
        private long maxMemory;
        private int cpuCores;

        // Getters and Setters
        public long getServerUptime() { return serverUptime; }
        public void setServerUptime(long serverUptime) { this.serverUptime = serverUptime; }

        public int getActiveSessionCount() { return activeSessionCount; }
        public void setActiveSessionCount(int activeSessionCount) { this.activeSessionCount = activeSessionCount; }

        public long getTotalUsersRegistered() { return totalUsersRegistered; }
        public void setTotalUsersRegistered(long totalUsersRegistered) { this.totalUsersRegistered = totalUsersRegistered; }

        public long getTotalMatchesCreated() { return totalMatchesCreated; }
        public void setTotalMatchesCreated(long totalMatchesCreated) { this.totalMatchesCreated = totalMatchesCreated; }

        public long getTotalGamesPlayed() { return totalGamesPlayed; }
        public void setTotalGamesPlayed(long totalGamesPlayed) { this.totalGamesPlayed = totalGamesPlayed; }

        public long getTotalMemory() { return totalMemory; }
        public void setTotalMemory(long totalMemory) { this.totalMemory = totalMemory; }

        public long getFreeMemory() { return freeMemory; }
        public void setFreeMemory(long freeMemory) { this.freeMemory = freeMemory; }

        public long getUsedMemory() { return usedMemory; }
        public void setUsedMemory(long usedMemory) { this.usedMemory = usedMemory; }

        public long getMaxMemory() { return maxMemory; }
        public void setMaxMemory(long maxMemory) { this.maxMemory = maxMemory; }

        public int getCpuCores() { return cpuCores; }
        public void setCpuCores(int cpuCores) { this.cpuCores = cpuCores; }
    }

    /**
     * 시간대별 통계 DTO
     */
    public static class HourlyStats {
        private String hour;
        private long matchCount;

        public String getHour() { return hour; }
        public void setHour(String hour) { this.hour = hour; }

        public long getMatchCount() { return matchCount; }
        public void setMatchCount(long matchCount) { this.matchCount = matchCount; }
    }
}
