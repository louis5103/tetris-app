package seoultech.se.server.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 3: Rate Limiting 인터셉터
 *
 * 기능:
 * - IP 기반 요청 횟수 제한
 * - 시간 윈도우 방식 (1분 단위)
 * - 매칭 API 보호 (DDoS 방지)
 *
 * 동작 방식:
 * 1. IP 주소별 요청 카운터 관리
 * 2. 1분당 최대 요청 횟수 제한
 * 3. 제한 초과 시 429 (Too Many Requests) 반환
 * 4. 매 분마다 카운터 초기화
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    /**
     * IP별 요청 카운터
     * Key: IP 주소
     * Value: RequestCounter (요청 횟수 및 마지막 리셋 시간)
     */
    private final Map<String, RequestCounter> requestCounters = new ConcurrentHashMap<>();

    /**
     * 1분당 최대 요청 횟수 (application.yml에서 설정 가능)
     */
    @Value("${rate-limit.max-requests-per-minute:60}")
    private int maxRequestsPerMinute;

    /**
     * Rate limit 시간 윈도우 (밀리초)
     * 기본값: 60000ms = 1분
     */
    private static final long WINDOW_SIZE_MS = 60000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();

        // 매칭 API에만 Rate Limiting 적용
        if (!requestUri.startsWith("/api/matchmaking")) {
            return true; // 다른 API는 통과
        }

        RequestCounter counter = requestCounters.computeIfAbsent(clientIp, k -> new RequestCounter());

        long currentTime = System.currentTimeMillis();
        long timeSinceReset = currentTime - counter.getLastResetTime();

        // 시간 윈도우가 지났으면 카운터 리셋
        if (timeSinceReset >= WINDOW_SIZE_MS) {
            counter.reset(currentTime);
        }

        // 요청 횟수 증가
        int currentCount = counter.incrementAndGet();

        // 제한 초과 확인
        if (currentCount > maxRequestsPerMinute) {
            log.warn("⚠️ [RateLimit] IP {} exceeded rate limit: {}/{} requests in {} seconds",
                clientIp, currentCount, maxRequestsPerMinute, timeSinceReset / 1000);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Max %d requests per minute.\",\"retryAfter\":%d}",
                maxRequestsPerMinute,
                (WINDOW_SIZE_MS - timeSinceReset) / 1000
            ));
            return false; // 요청 차단
        }

        log.debug("✅ [RateLimit] IP {} request count: {}/{}", clientIp, currentCount, maxRequestsPerMinute);
        return true; // 요청 허용
    }

    /**
     * 클라이언트 IP 주소 추출
     *
     * X-Forwarded-For 헤더를 우선 확인 (프록시 환경 대응)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // 프록시를 거쳤을 경우 첫 번째 IP가 실제 클라이언트 IP
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 요청 카운터 내부 클래스
     */
    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long lastResetTime = System.currentTimeMillis();

        public int incrementAndGet() {
            return count.incrementAndGet();
        }

        public long getLastResetTime() {
            return lastResetTime;
        }

        public void reset(long currentTime) {
            count.set(0);
            lastResetTime = currentTime;
        }
    }

    /**
     * Phase 3: 정리 작업 (선택적)
     *
     * 오래된 IP 엔트리 제거 (메모리 누수 방지)
     * 스케줄러를 통해 주기적으로 호출 가능
     */
    public void cleanupOldEntries() {
        long currentTime = System.currentTimeMillis();
        requestCounters.entrySet().removeIf(entry -> {
            long timeSinceReset = currentTime - entry.getValue().getLastResetTime();
            return timeSinceReset > WINDOW_SIZE_MS * 2; // 2분 이상 비활성 IP 제거
        });
        log.debug("🧹 [RateLimit] Cleaned up old entries. Active IPs: {}", requestCounters.size());
    }
}
