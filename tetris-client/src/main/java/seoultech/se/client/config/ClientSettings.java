package seoultech.se.client.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 클라이언트 사용자 설정
 * 
 * 게임 모드 설정(Classic/Arcade)은 game-modes.yml에서 관리되며,
 * 이 클래스는 사용자별 환경 설정만 담당합니다.
 */
@Component
@ConfigurationProperties(prefix = "client")
@Getter
@Setter
public class ClientSettings {
    private GeneralSettings setting;
}
