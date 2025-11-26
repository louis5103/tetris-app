package seoultech.se.client.config.mode;

import lombok.Getter;
import lombok.Setter;

/**
 * 클래식 모드 설정
 * config/client/classic.yml과 1:1 매핑
 */
@Getter
@Setter
public class ClassicModeSettings {
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
}
