package seoultech.se.client.config.mode;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * 아케이드 모드 설정
 * config/client/arcade.yml과 1:1 매핑
 */
@Getter
@Setter
public class ArcadeModeSettings {
    // 회전 시스템
    private boolean srsEnabled;
    private boolean rotation180Enabled;
    
    // 기능 활성화
    private boolean hardDropEnabled;
    private boolean holdEnabled;
    private boolean ghostPieceEnabled;
    
    // 속도 설정
    private double dropSpeedMultiplier;
    private double softDropSpeed;
    
    // 락 시스템
    private int lockDelay;
    private int maxLockResets;
    
    // 아이템 시스템
    private double itemDropRate;
    private int maxInventorySize;
    private boolean itemAutoUse;
    private Map<String, Boolean> enabledItems;
}
