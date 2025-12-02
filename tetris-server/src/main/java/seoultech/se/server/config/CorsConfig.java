package seoultech.se.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Phase 3: CORS 보안 설정
 *
 * 기능:
 * - 허용된 Origin만 접근 가능
 * - 개발 환경과 프로덕션 환경 분리
 * - 안전한 HTTP 메서드 및 헤더 설정
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Value("${cors.allowed-headers:*}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials:true}")
    private boolean allowCredentials;

    @Value("${cors.max-age:3600}")
    private long maxAge;

    /**
     * Phase 3: CORS 설정
     *
     * application.yml에서 다음 설정 가능:
     * - cors.allowed-origins: 허용할 Origin 목록 (쉼표로 구분)
     * - cors.allowed-methods: 허용할 HTTP 메서드
     * - cors.allowed-headers: 허용할 헤더
     * - cors.allow-credentials: 인증 정보 포함 여부
     * - cors.max-age: preflight 요청 캐시 시간 (초)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin 설정
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        configuration.setAllowedOrigins(origins);

        // 허용할 HTTP 메서드
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        configuration.setAllowedMethods(methods);

        // 허용할 헤더
        if ("*".equals(allowedHeaders)) {
            configuration.addAllowedHeader("*");
        } else {
            List<String> headers = Arrays.asList(allowedHeaders.split(","));
            configuration.setAllowedHeaders(headers);
        }

        // 인증 정보 포함 여부
        configuration.setAllowCredentials(allowCredentials);

        // preflight 요청 캐시 시간
        configuration.setMaxAge(maxAge);

        // 노출할 헤더 (클라이언트가 읽을 수 있는 헤더)
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        System.out.println("✅ [CORS] Configured with origins: " + origins);

        return source;
    }
}
