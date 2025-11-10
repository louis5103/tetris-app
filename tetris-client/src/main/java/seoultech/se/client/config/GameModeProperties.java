package seoultech.se.client.config;

import java.util.HashMap;
import java.util.Map;

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
    
    // ========== 아이템 설정 (아케이드 모드용) ==========
    
    /**
     * 아이템 드롭 확률 (0.0 ~ 1.0)
     */
    private double itemDropRate = 0.1;
    
    /**
     * 아이템 활성화 맵
     * 각 아이템 타입별로 활성화 여부를 저장
     * 예: {"BOMB": true, "PLUS": true, "SPEED_RESET": false, "BONUS_SCORE": true}
     */
    private Map<String, Boolean> itemEnabled = new HashMap<String, Boolean>() {{
        put("BOMB", true);
        put("PLUS", true);
        put("SPEED_RESET", true);
        put("BONUS_SCORE", true);
    }};
    
    /**
     * 아이템 인벤토리 최대 크기
     */
    private int maxInventorySize = 3;
    
    /**
     * 아이템 자동 사용 여부
     */
    private boolean itemAutoUse = false;
    
    /**
     * 설정 유효성 검증
     */
    public boolean isValid() {
        return playType != null && gameplayType != null;
    }
    
    /**
     * 특정 아이템이 활성화되었는지 확인
     * 
     * @param itemTypeName 아이템 타입 이름 (예: "BOMB", "PLUS")
     * @return 활성화 여부
     */
    public boolean isItemEnabled(String itemTypeName) {
        return itemEnabled.getOrDefault(itemTypeName, false);
    }
    
    /**
     * 특정 아이템 활성화/비활성화
     * 
     * @param itemTypeName 아이템 타입 이름
     * @param enabled 활성화 여부
     */
    public void setItemEnabled(String itemTypeName, boolean enabled) {
        itemEnabled.put(itemTypeName, enabled);
    }
}
