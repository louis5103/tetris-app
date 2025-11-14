package seoultech.se.client.config;

import org.springframework.boot.test.context.TestConfiguration;

/**
 * 테스트용 설정 클래스
 * 
 * JavaFX 컴포넌트는 @ConditionalOnProperty로 제외됩니다.
 * (javafx.enabled=false 설정 사용)
 */
@TestConfiguration
public class TestConfig {
    // JavaFX 컴포넌트는 메인 코드의 @ConditionalOnProperty로 제어
    // 테스트에서는 javafx.enabled=false 속성을 사용
}
