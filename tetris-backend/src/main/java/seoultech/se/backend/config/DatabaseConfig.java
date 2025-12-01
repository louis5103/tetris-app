package seoultech.se.backend.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA/Database 설정
 * 
 * Backend 서버 실행시 JPA 활성화:
 * - JPA Repository 스캔
 * - Entity 스캔
 * - 데이터베이스 연결
 */
@Configuration
@EnableJpaRepositories(basePackages = "seoultech.se")
@EntityScan(basePackages = "seoultech.se")
public class DatabaseConfig {
    // JPA 관련 설정이 필요한 경우 여기에 추가
}
