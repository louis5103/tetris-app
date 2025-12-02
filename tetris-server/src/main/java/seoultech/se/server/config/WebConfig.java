package seoultech.se.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import seoultech.se.server.ratelimit.RateLimitInterceptor;

/**
 * Phase 3: Web MVC 설정
 *
 * 기능:
 * - Rate Limiting 인터셉터 등록
 * - 매칭 API 보호
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private RateLimitInterceptor rateLimitInterceptor;

    /**
     * Phase 3: Rate Limiting 인터셉터 등록
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
            .addPathPatterns("/api/matchmaking/**"); // 매칭 API에만 적용

        System.out.println("✅ [WebConfig] Rate Limiting interceptor registered for /api/matchmaking/**");
    }

    /**
     * Phase 3: 오래된 Rate Limit 엔트리 정리 (매 5분마다)
     */
    @Scheduled(fixedRate = 300000) // 5분
    public void cleanupRateLimitEntries() {
        rateLimitInterceptor.cleanupOldEntries();
    }
}
