package seoultech.se.server.service;

import org.springframework.stereotype.Service;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.enumType.Difficulty;

/**
 * 서버 측 GameModeConfig 팩토리
 * 
 * 멀티플레이어 세션을 위한 기본 Config 생성 및 Difficulty 배율 적용
 * 
 * 책임:
 * 1. 서버 측 기본 Config 생성 (Classic, Arcade)
 * 2. Difficulty 배율 적용 (dropSpeed, lockDelay)
 * 3. 호스트가 설정한 난이도에 따른 Config 생성
 * 
 * 클라이언트 팩토리와 차이점:
 * - ClientSettings(YML) 없이 하드코딩된 기본값 사용
 * - 멀티플레이어 세션용 표준 설정 제공
 * - 서버가 권위 있는 Config 생성
 */
@Service
public class ServerConfigFactory {
    
    // ========== 기본 설정 상수 ==========
    
    // Classic 모드 기본값
    private static final double CLASSIC_DROP_SPEED = 1.0;
    private static final double CLASSIC_SOFT_DROP_SPEED = 20.0;
    private static final int CLASSIC_LOCK_DELAY = 500;
    private static final int CLASSIC_MAX_LOCK_RESETS = 15;
    
    // Arcade 모드 기본값
    private static final double ARCADE_DROP_SPEED = 1.5;
    private static final double ARCADE_SOFT_DROP_SPEED = 25.0;
    private static final int ARCADE_LOCK_DELAY = 300;
    private static final int ARCADE_MAX_LOCK_RESETS = 10;
    
    /**
     * GameplayType + Difficulty → GameModeConfig 생성
     * 
     * @param gameplayType CLASSIC or ARCADE
     * @param difficulty EASY, NORMAL, HARD
     * @return 최종 GameModeConfig
     */
    public GameModeConfig createConfig(GameplayType gameplayType, Difficulty difficulty) {
        return switch (gameplayType) {
            case CLASSIC -> createClassicConfig(difficulty);
            case ARCADE -> createArcadeConfig(difficulty);
            case TIME_ATTACK -> createTimeAttackConfig(difficulty);
        };
    }
    
    /**
     * Classic 모드 Config 생성
     * 
     * @param difficulty 난이도
     * @return GameModeConfig
     */
    public GameModeConfig createClassicConfig(Difficulty difficulty) {
        DifficultyMultiplier multiplier = getDifficultyMultiplier(difficulty);
        
        return GameModeConfig.builder()
            .gameplayType(GameplayType.CLASSIC)
            .difficulty(difficulty)
            
            // 회전 시스템 (기본값)
            .srsEnabled(true)
            .rotation180Enabled(false)
            
            // 기능 활성화 (전부 켜기)
            .hardDropEnabled(true)
            .holdEnabled(true)
            .ghostPieceEnabled(true)
            
            // 속도 설정 (Difficulty 배율 적용)
            .dropSpeedMultiplier(CLASSIC_DROP_SPEED * multiplier.speedMultiplier)
            .softDropSpeed(CLASSIC_SOFT_DROP_SPEED)
            
            // 락 시스템 (Difficulty 배율 적용)
            .lockDelay((int)(CLASSIC_LOCK_DELAY * multiplier.lockDelayMultiplier))
            .maxLockResets(CLASSIC_MAX_LOCK_RESETS)
            
            // 아이템 없음 (Classic 모드)
            .linesPerItem(0)
            .itemDropRate(0.0)  // Deprecated
            .maxInventorySize(0)
            .itemAutoUse(false)
            .enabledItemTypes(java.util.Collections.emptySet())
            
            .build();
    }
    
    /**
     * Arcade 모드 Config 생성
     * 
     * @param difficulty 난이도
     * @return GameModeConfig
     */
    public GameModeConfig createArcadeConfig(Difficulty difficulty) {
        DifficultyMultiplier multiplier = getDifficultyMultiplier(difficulty);
        
        return GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .difficulty(difficulty)
            
            // 회전 시스템 (기본값)
            .srsEnabled(true)
            .rotation180Enabled(false)
            
            // 기능 활성화 (전부 켜기)
            .hardDropEnabled(true)
            .holdEnabled(true)
            .ghostPieceEnabled(true)
            
            // 속도 설정 (Difficulty 배율 적용)
            .dropSpeedMultiplier(ARCADE_DROP_SPEED * multiplier.speedMultiplier)
            .softDropSpeed(ARCADE_SOFT_DROP_SPEED)
            
            // 락 시스템 (Difficulty 배율 적용)
            .lockDelay((int)(ARCADE_LOCK_DELAY * multiplier.lockDelayMultiplier))
            .maxLockResets(ARCADE_MAX_LOCK_RESETS)
            
            // 아이템 설정 (기본 Arcade 아이템 - ItemConfig 제거)
            .linesPerItem(10)
            .itemDropRate(0.1)  // Deprecated
            .maxInventorySize(3)
            .itemAutoUse(false)
            .enabledItemTypes(java.util.EnumSet.allOf(ItemType.class))
            
            .build();
    }

    /**
     * Time Attack 모드 Config 생성 (Classic 기반)
     * 
     * @param difficulty 난이도
     * @return GameModeConfig
     */
    public GameModeConfig createTimeAttackConfig(Difficulty difficulty) {
        DifficultyMultiplier multiplier = getDifficultyMultiplier(difficulty);
        
        return GameModeConfig.builder()
            .gameplayType(GameplayType.TIME_ATTACK)
            .difficulty(difficulty)
            
            .srsEnabled(true)
            .rotation180Enabled(false)
            
            .hardDropEnabled(true)
            .holdEnabled(true)
            .ghostPieceEnabled(true)
            
            .dropSpeedMultiplier(CLASSIC_DROP_SPEED * multiplier.speedMultiplier)
            .softDropSpeed(CLASSIC_SOFT_DROP_SPEED)
            
            .lockDelay((int)(CLASSIC_LOCK_DELAY * multiplier.lockDelayMultiplier))
            .maxLockResets(CLASSIC_MAX_LOCK_RESETS)
            
            .linesPerItem(0)
            .itemDropRate(0.0)
            .maxInventorySize(0)
            .itemAutoUse(false)
            .enabledItemTypes(java.util.Collections.emptySet())
            
            .build();
    }
    
    /**
     * Difficulty 배율 정보
     * 
     * @param difficulty 난이도
     * @return 배율 정보
     */
    private DifficultyMultiplier getDifficultyMultiplier(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> new DifficultyMultiplier(0.8, 1.2);    // 속도 느림, 락 딜레이 김
            case NORMAL -> new DifficultyMultiplier(1.0, 1.0);  // 기본
            case HARD -> new DifficultyMultiplier(1.2, 0.8);    // 속도 빠름, 락 딜레이 짧음
        };
    }
    
    /**
     * Difficulty 배율 정보 (내부 클래스)
     */
    private static class DifficultyMultiplier {
        final double speedMultiplier;      // dropSpeedMultiplier에 곱함
        final double lockDelayMultiplier;  // lockDelay에 곱함
        
        DifficultyMultiplier(double speedMultiplier, double lockDelayMultiplier) {
            this.speedMultiplier = speedMultiplier;
            this.lockDelayMultiplier = lockDelayMultiplier;
        }
    }
}
