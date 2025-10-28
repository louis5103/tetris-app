package seoultech.se.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.mode.PlayType;

/**
 * 게임 모드 설정을 위한 Configuration Properties
 * 
 * application.properties의 tetris.mode.* 값을 자동으로 매핑합니다.
 * 
 * 장점:
 * - 타입 안전성 (String → Enum 자동 변환)
 * - IDE 자동완성 지원
 * - 유효성 검증 가능
 * - 테스트 용이
 * 
 * 사용 예시:
 * @Autowired
 * private GameModeProperties gameModeProperties;
 * 
 * PlayType playType = gameModeProperties.getPlayType();
 */
@Configuration
@ConfigurationProperties(prefix = "tetris.mode")
@Getter
@Setter
public class GameModeProperties {
    
    /**
     * 플레이 타입 (LOCAL_SINGLE, ONLINE_MULTI)
     * 런타임 검증은 SettingsService.validateGameModeSettings()에서 수행
     */
    private PlayType playType = PlayType.LOCAL_SINGLE;
    
    /**
     * 게임플레이 타입 (CLASSIC, ARCADE)
     * 런타임 검증은 SettingsService.validateGameModeSettings()에서 수행
     */
    private GameplayType gameplayType = GameplayType.CLASSIC;
    
    /**
     * SRS 활성화 여부
     */
    private boolean srsEnabled = true;
    
    /**
     * 마지막 선택 - 플레이 타입
     */
    private PlayType lastPlayType = PlayType.LOCAL_SINGLE;
    
    /**
     * 마지막 선택 - 게임플레이 타입
     */
    private GameplayType lastGameplayType = GameplayType.CLASSIC;
    
    /**
     * 마지막 선택 - SRS 활성화 여부
     */
    private boolean lastSrsEnabled = true;
    
    /**
     * 설정 유효성 검증
     */
    public boolean isValid() {
        return playType != null && gameplayType != null;
    }
}
