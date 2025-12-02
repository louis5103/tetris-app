package seoultech.se.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.model.enumType.Difficulty;

/**
 * 게임 모드 설정 전송 DTO
 * 
 * GameModeConfig를 네트워크로 직렬화하여 클라이언트에게 전송하기 위한 DTO입니다.
 * 멀티플레이어 세션에서 서버의 권위 있는 Config를 모든 클라이언트가 공유하도록 합니다.
 * 
 * 사용 시나리오:
 * 1. 호스트가 Config 설정 (POST /api/session/{sessionId}/config)
 * 2. 클라이언트가 세션 조인 시 Config 수신 (SessionCreateResponse에 포함)
 * 3. WebSocket으로 Config 변경 알림 (게임 시작 전)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionConfigDto {
    
    // ========== 게임 타입 ==========
    
    /**
     * 게임플레이 타입 (CLASSIC, ARCADE)
     */
    private GameplayType gameplayType;
    
    /**
     * 난이도 (EASY, NORMAL, HARD)
     */
    private Difficulty difficulty;
    
    // ========== 회전 시스템 ==========
    
    /**
     * SRS(Super Rotation System) 활성화 여부
     */
    private boolean srsEnabled;
    
    /**
     * 180도 회전 허용 여부
     */
    private boolean rotation180Enabled;
    
    // ========== 기능 활성화 ==========
    
    /**
     * 하드드롭 활성화 여부
     */
    private boolean hardDropEnabled;
    
    /**
     * 홀드 기능 활성화 여부
     */
    private boolean holdEnabled;
    
    /**
     * 고스트 블록 표시 여부
     */
    private boolean ghostPieceEnabled;
    
    // ========== 속도 설정 ==========
    
    /**
     * 낙하 속도 배율
     */
    private double dropSpeedMultiplier;
    
    /**
     * 소프트 드롭 속도
     */
    private double softDropSpeed;
    
    // ========== 락 시스템 ==========
    
    /**
     * 락 딜레이 시간 (밀리초)
     */
    private int lockDelay;
    
    /**
     * 최대 락 리셋 횟수
     */
    private int maxLockResets;
    
    // ========== 변환 메서드 ==========
    
    /**
     * GameModeConfig → SessionConfigDto 변환
     * 
     * @param config 변환할 GameModeConfig
     * @return DTO 객체
     */
    public static SessionConfigDto fromGameModeConfig(GameModeConfig config) {
        if (config == null) {
            return null;
        }
        
        return SessionConfigDto.builder()
            .gameplayType(config.getGameplayType())
            .difficulty(config.getDifficulty())
            .srsEnabled(config.isSrsEnabled())
            .rotation180Enabled(config.isRotation180Enabled())
            .hardDropEnabled(config.isHardDropEnabled())
            .holdEnabled(config.isHoldEnabled())
            .ghostPieceEnabled(config.isGhostPieceEnabled())
            .dropSpeedMultiplier(config.getDropSpeedMultiplier())
            .softDropSpeed(config.getSoftDropSpeed())
            .lockDelay(config.getLockDelay())
            .maxLockResets(config.getMaxLockResets())
            .build();
    }
    
    /**
     * SessionConfigDto → GameModeConfig 변환
     * 
     * @return GameModeConfig 객체
     */
    public GameModeConfig toGameModeConfig() {
        return GameModeConfig.builder()
            .gameplayType(gameplayType)
            .difficulty(difficulty)
            .srsEnabled(srsEnabled)
            .rotation180Enabled(rotation180Enabled)
            .hardDropEnabled(hardDropEnabled)
            .holdEnabled(holdEnabled)
            .ghostPieceEnabled(ghostPieceEnabled)
            .dropSpeedMultiplier(dropSpeedMultiplier)
            .softDropSpeed(softDropSpeed)
            .lockDelay(lockDelay)
            .maxLockResets(maxLockResets)
            .build();
    }
}
