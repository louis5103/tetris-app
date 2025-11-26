package seoultech.se.client.service;

import org.springframework.stereotype.Component;
import seoultech.se.client.config.ClientSettings;
import seoultech.se.client.config.mode.ArcadeModeSettings;
import seoultech.se.client.config.mode.ClassicModeSettings;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.engine.item.ItemConfig;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.enumType.Difficulty;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * GameModeConfig 팩토리
 * 
 * ClientSettings(사용자 저장 설정) + Difficulty(런타임 선택)를 통합하여
 * GameModeConfig(게임 실행 설정)을 생성합니다.
 * 
 * 책임:
 * 1. ClientSettings → GameModeConfig 변환
 * 2. Difficulty 배율 적용 (dropSpeed, lockDelay)
 * 3. ItemConfig 생성 (Arcade 모드)
 */
@Component
public class GameModeConfigFactory {
    
    /**
     * Classic 모드 설정 생성
     * 
     * @param settings ClassicModeSettings (YML에서 로드된 설정)
     * @param difficulty 선택된 난이도
     * @return 최종 GameModeConfig
     */
    public GameModeConfig createClassicConfig(ClassicModeSettings settings, Difficulty difficulty) {
        // Difficulty 배율 적용
        DifficultyMultiplier multiplier = getDifficultyMultiplier(difficulty);
        
        return GameModeConfig.builder()
            .gameplayType(GameplayType.CLASSIC)
            .difficulty(difficulty)
            
            // 회전 시스템
            .srsEnabled(settings.isSrsEnabled())
            .rotation180Enabled(settings.isRotation180Enabled())
            
            // 기능 활성화
            .hardDropEnabled(settings.isHardDropEnabled())
            .holdEnabled(settings.isHoldEnabled())
            .ghostPieceEnabled(settings.isGhostPieceEnabled())
            
            // 속도 설정 (Difficulty 배율 적용)
            .dropSpeedMultiplier(settings.getDropSpeedMultiplier() * multiplier.speedMultiplier)
            .softDropSpeed(settings.getSoftDropSpeed())
            
            // 락 시스템 (Difficulty 배율 적용)
            .lockDelay((int)(settings.getLockDelay() * multiplier.lockDelayMultiplier))
            .maxLockResets(settings.getMaxLockResets())
            
            // 아이템 없음
            .itemConfig(null)
            
            .build();
    }
    
    /**
     * Arcade 모드 설정 생성
     * 
     * @param settings ArcadeModeSettings (YML에서 로드된 설정)
     * @param difficulty 선택된 난이도
     * @return 최종 GameModeConfig
     */
    public GameModeConfig createArcadeConfig(ArcadeModeSettings settings, Difficulty difficulty) {
        // Difficulty 배율 적용
        DifficultyMultiplier multiplier = getDifficultyMultiplier(difficulty);
        
        // ItemConfig 생성
        ItemConfig itemConfig = createItemConfig(settings);
        
        return GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .difficulty(difficulty)
            
            // 회전 시스템
            .srsEnabled(settings.isSrsEnabled())
            .rotation180Enabled(settings.isRotation180Enabled())
            
            // 기능 활성화
            .hardDropEnabled(settings.isHardDropEnabled())
            .holdEnabled(settings.isHoldEnabled())
            .ghostPieceEnabled(settings.isGhostPieceEnabled())
            
            // 속도 설정 (Difficulty 배율 적용)
            .dropSpeedMultiplier(settings.getDropSpeedMultiplier() * multiplier.speedMultiplier)
            .softDropSpeed(settings.getSoftDropSpeed())
            
            // 락 시스템 (Difficulty 배율 적용)
            .lockDelay((int)(settings.getLockDelay() * multiplier.lockDelayMultiplier))
            .maxLockResets(settings.getMaxLockResets())
            
            // 아이템 설정
            .itemConfig(itemConfig)
            
            .build();
    }
    
    /**
     * ClientSettings + GameplayType + Difficulty → GameModeConfig
     * 
     * @param clientSettings 전체 클라이언트 설정
     * @param gameplayType CLASSIC or ARCADE
     * @param difficulty 선택된 난이도
     * @return GameModeConfig
     */
    public GameModeConfig create(ClientSettings clientSettings, GameplayType gameplayType, Difficulty difficulty) {
        ClientSettings.Modes modes = clientSettings.getModes();
        
        return switch (gameplayType) {
            case CLASSIC -> createClassicConfig(modes.getClassic(), difficulty);
            case ARCADE -> createArcadeConfig(modes.getArcade(), difficulty);
        };
    }
    
    /**
     * ArcadeModeSettings → ItemConfig 변환
     */
    private ItemConfig createItemConfig(ArcadeModeSettings settings) {
        // 활성화된 아이템 타입 수집
        Set<ItemType> enabledItems = new HashSet<>();
        Map<String, Boolean> enabledItemsMap = settings.getEnabledItems();
        
        if (enabledItemsMap != null) {
            for (Map.Entry<String, Boolean> entry : enabledItemsMap.entrySet()) {
                if (entry.getValue()) {
                    try {
                        enabledItems.add(ItemType.valueOf(entry.getKey()));
                    } catch (IllegalArgumentException e) {
                        System.err.println("⚠️ Invalid item type: " + entry.getKey());
                    }
                }
            }
        }
        
        return ItemConfig.builder()
            .dropRate(settings.getItemDropRate())
            .enabledItems(enabledItems)
            .maxInventorySize(settings.getMaxInventorySize())
            .autoUse(settings.isItemAutoUse())
            .build();
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
