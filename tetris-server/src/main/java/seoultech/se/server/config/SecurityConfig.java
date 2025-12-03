package seoultech.se.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import seoultech.se.server.user.UserRepository;


@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Phase 3: CORS 설정 적용
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/tetris/users/login", "/tetris/users/signup", "/tetris/users/reissue").permitAll()
                .requestMatchers("/tetris/users/logout").authenticated()
                // Phase 3: 헬스체크 엔드포인트는 인증 없이 접근 가능
                .requestMatchers("/api/health", "/api/health/**").permitAll()
                // Session API: 개발/테스트용으로 인증 없이 접근 가능
                .requestMatchers("/api/session/**").permitAll()
                // WebSocket: 개발/테스트용으로 인증 없이 접근 가능
                .requestMatchers("/ws-game/**", "/game/**").permitAll()
                // Dashboard: 관리자 대시보드는 인증 필요 (추후 ADMIN 역할 추가 가능)
                .requestMatchers("/api/admin/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userRepository), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
