package seoultech.se.client.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.model.enumType.Difficulty;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * GameModeConfig 팩토리
 * 
 * YML 프로퍼티(@Value) + Difficulty(런타임 선택)를 통합하여
 * GameModeConfig(게임 실행 설정)을 생성합니다.
 * 
 * 리팩토링: Settings 클래스 제거, @Value로 YML 직접 읽기
 * 
 * 책임:
 * 1. YML 프로퍼티(@Value) → GameModeConfig 변환
 * 2. Difficulty 배율 적용 (dropSpeed, lockDelay)
 * 3. Classic/Arcade 필드를 명확히 분리하여 관리
 */
@Component
public class GameModeConfigFactory {
    
    // ========== Classic Mode 설정 ==========
    @Value("${tetris.modes.classic.srsEnabled}")
    private boolean classicSrsEnabled;
    
    @Value("${tetris.modes.classic.rotation180Enabled}")
    private boolean classicRotation180Enabled;
    
    @Value("${tetris.modes.classic.hardDropEnabled}")
    private boolean classicHardDropEnabled;
    
    @Value("${tetris.modes.classic.holdEnabled}")
    private boolean classicHoldEnabled;
    
    @Value("${tetris.modes.classic.ghostPieceEnabled}")
    private boolean classicGhostPieceEnabled;
    
    @Value("${tetris.modes.classic.dropSpeedMultiplier}")
    private double classicDropSpeedMultiplier;
    
    @Value("${tetris.modes.classic.softDropSpeed}")
    private int classicSoftDropSpeed;
    
    @Value("${tetris.modes.classic.lockDelay}")
    private int classicLockDelay;
    
    @Value("${tetris.modes.classic.maxLockResets}")
    private int classicMaxLockResets;
    
    // ========== Arcade Mode 설정 ==========
    @Value("${tetris.modes.arcade.srsEnabled}")
    private boolean arcadeSrsEnabled;
    
    @Value("${tetris.modes.arcade.rotation180Enabled}")
    private boolean arcadeRotation180Enabled;
    
    @Value("${tetris.modes.arcade.hardDropEnabled}")
    private boolean arcadeHardDropEnabled;
    
    @Value("${tetris.modes.arcade.holdEnabled}")
    private boolean arcadeHoldEnabled;
    
    @Value("${tetris.modes.arcade.ghostPieceEnabled}")
    private boolean arcadeGhostPieceEnabled;
    
    @Value("${tetris.modes.arcade.dropSpeedMultiplier}")
    private double arcadeDropSpeedMultiplier;
    
    @Value("${tetris.modes.arcade.softDropSpeed}")
    private int arcadeSoftDropSpeed;
    
    @Value("${tetris.modes.arcade.lockDelay}")
    private int arcadeLockDelay;
    
    @Value("${tetris.modes.arcade.maxLockResets}")
    private int arcadeMaxLockResets;

    // ========== Time Attack 설정 ==========
    @Value("${tetris.modes.time-attack.timeLimitSeconds}")
    private int timeAttackTimeLimitSeconds;
    
    // ========== Arcade Item 설정 ==========
    @Value("${tetris.modes.arcade.item.linesPerItem}")
    private int arcadeLinesPerItem;
    
    @Value("${tetris.modes.arcade.item.dropRate}")
    private double arcadeItemDropRate;
    
    @Value("${tetris.modes.arcade.item.maxInventorySize}")
    private int arcadeMaxInventorySize;
    
    @Value("${tetris.modes.arcade.item.autoUse}")
    private boolean arcadeItemAutoUse;
    
    @Value("#{'${tetris.modes.arcade.item.enabledTypes}'.split(',')}")
    private List<String> arcadeEnabledItemTypes;
    
    /**
     * Classic 모드 설정 생성
     * 
     * @param difficulty 선택된 난이도
     * @return 최종 GameModeConfig
     */
    public GameModeConfig createClassicConfig(Difficulty difficulty) {
        // Difficulty 배율 적용
        DifficultyMultiplier multiplier = getDifficultyMultiplier(difficulty);
        
        return GameModeConfig.builder()
            .gameplayType(GameplayType.CLASSIC)
            .difficulty(difficulty)
            
            // 회전 시스템
            .srsEnabled(classicSrsEnabled)
            .rotation180Enabled(classicRotation180Enabled)
            
            // 기능 활성화
            .hardDropEnabled(classicHardDropEnabled)
            .holdEnabled(classicHoldEnabled)
            .ghostPieceEnabled(classicGhostPieceEnabled)
            
            // 속도 설정 (Difficulty 배율 적용)
            .dropSpeedMultiplier(classicDropSpeedMultiplier * multiplier.speedMultiplier)
            .softDropSpeed(classicSoftDropSpeed)
            
            // 락 시스템 (Difficulty 배율 적용)
            .lockDelay((int)(classicLockDelay * multiplier.lockDelayMultiplier))
            .maxLockResets(classicMaxLockResets)
            
            // 아이템 없음 (Classic 모드)
            .linesPerItem(0)
            .itemDropRate(0.0)
            .maxInventorySize(0)
            .itemAutoUse(false)
            .enabledItemTypes(java.util.Collections.emptySet())
            
            .build();
    }
    
    /**
     * Arcade 모드 설정 생성
     * 
     * @param difficulty 선택된 난이도
     * @return 최종 GameModeConfig
     */
    public GameModeConfig createArcadeConfig(Difficulty difficulty) {
        // Difficulty 배율 적용
        DifficultyMultiplier multiplier = getDifficultyMultiplier(difficulty);

        System.out.println("🏭 [GameModeConfigFactory] Creating Arcade Config");
        System.out.println("   - linesPerItem from YML: " + arcadeLinesPerItem);
        System.out.println("   - maxInventorySize from YML: " + arcadeMaxInventorySize);
        System.out.println("   - autoUse from YML: " + arcadeItemAutoUse);
        System.out.println("   - enabledTypes from YML (raw): " + arcadeEnabledItemTypes);

        Set<seoultech.se.core.engine.item.ItemType> parsedTypes = parseItemTypes(arcadeEnabledItemTypes);
        System.out.println("   - Parsed enabledItemTypes: " + parsedTypes);
        System.out.println("   - isItemSystemEnabled will be: " + (arcadeLinesPerItem > 0 && !parsedTypes.isEmpty()));

        return GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .difficulty(difficulty)

            // 회전 시스템
            .srsEnabled(arcadeSrsEnabled)
            .rotation180Enabled(arcadeRotation180Enabled)

            // 기능 활성화
            .hardDropEnabled(arcadeHardDropEnabled)
            .holdEnabled(arcadeHoldEnabled)
            .ghostPieceEnabled(arcadeGhostPieceEnabled)

            // 속도 설정 (Difficulty 배율 적용)
            .dropSpeedMultiplier(arcadeDropSpeedMultiplier * multiplier.speedMultiplier)
            .softDropSpeed(arcadeSoftDropSpeed)

            // 락 시스템 (Difficulty 배율 적용)
            .lockDelay((int)(arcadeLockDelay * multiplier.lockDelayMultiplier))
            .maxLockResets(arcadeMaxLockResets)

            // ========== 아이템 설정 ==========
            .linesPerItem(arcadeLinesPerItem)
            .itemDropRate(arcadeItemDropRate)  // Deprecated
            .maxInventorySize(arcadeMaxInventorySize)
            .itemAutoUse(arcadeItemAutoUse)
            .enabledItemTypes(parsedTypes)  // 미리 파싱한 값 사용
            
            .build();
    }

    public GameModeConfig createTimeAttackConfig(Difficulty difficulty) {
        // Difficulty 배율 적용
        DifficultyMultiplier multiplier = getDifficultyMultiplier(difficulty);
        
        return GameModeConfig.builder()
            .gameplayType(GameplayType.TIME_ATTACK)
            .difficulty(difficulty)
            .timeLimitSeconds(timeAttackTimeLimitSeconds)
            
            // 회전 시스템
            .srsEnabled(classicSrsEnabled)
            .rotation180Enabled(classicRotation180Enabled)
            
            // 기능 활성화
            .hardDropEnabled(classicHardDropEnabled)
            .holdEnabled(classicHoldEnabled)
            .ghostPieceEnabled(classicGhostPieceEnabled)
            
            // 속도 설정 (Difficulty 배율 적용)
            .dropSpeedMultiplier(classicDropSpeedMultiplier * multiplier.speedMultiplier)
            .softDropSpeed(classicSoftDropSpeed)
            
            // 락 시스템 (Difficulty 배율 적용)
            .lockDelay((int)(classicLockDelay * multiplier.lockDelayMultiplier))
            .maxLockResets(classicMaxLockResets)
            
            // 아이템 없음 (Time Attack 모드)
            .linesPerItem(0)
            .itemDropRate(0.0)
            .maxInventorySize(0)
            .itemAutoUse(false)
            .enabledItemTypes(java.util.Collections.emptySet())
            
            .build();
    }
    
    /**
     * GameplayType + Difficulty → GameModeConfig
     * 
     * @param gameplayType CLASSIC or ARCADE
     * @param difficulty 선택된 난이도
     * @return GameModeConfig
     */
    public GameModeConfig create(GameplayType gameplayType, Difficulty difficulty) {
        return switch (gameplayType) {
            case CLASSIC -> createClassicConfig(difficulty);
            case ARCADE -> createArcadeConfig(difficulty);
            case TIME_ATTACK -> createTimeAttackConfig(difficulty);
        };
    }
    
    /**
     * String 리스트를 ItemType EnumSet으로 변환
     */
    private Set<seoultech.se.core.engine.item.ItemType> parseItemTypes(List<String> itemTypeStrings) {
        System.out.println("🔍 [GameModeConfigFactory] Parsing item types from YML:");
        System.out.println("   - Raw strings: " + itemTypeStrings);

        Set<seoultech.se.core.engine.item.ItemType> result = itemTypeStrings.stream()
            .map(String::trim)  // 공백 제거
            .filter(s -> !s.isEmpty())  // 빈 문자열 제거
            .map(s -> {
                System.out.println("   - Parsing: '" + s + "'");
                return seoultech.se.core.engine.item.ItemType.valueOf(s);
            })
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(seoultech.se.core.engine.item.ItemType.class)));

        System.out.println("   - Parsed item types: " + result);
        return result;
    }
    
    /**
     * Difficulty 배율 정보
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
