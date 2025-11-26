package seoultech.se.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import seoultech.se.core.engine.ArcadeGameEngine;
import seoultech.se.core.engine.ClassicGameEngine;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.engine.item.ItemManager;

/**
 * Core 모듈 설정 클래스
 * 
 * Strategy Pattern을 적용하여 게임 모드에 따라 다른 GameEngine을 빈으로 등록합니다.
 * 
 * 조건부 빈 등록:
 * - tetris.game.item.enabled = false (기본값) → ClassicGameEngine
 * - tetris.game.item.enabled = true → ArcadeGameEngine + ItemManager
 * 
 * 사용 예시:
 * ```java
 * @Autowired
 * private GameEngine gameEngine;  // 자동으로 Classic 또는 Arcade 주입
 * ```
 */
@Configuration
@ComponentScan(basePackages = "seoultech.se.core")
public class CoreConfig {
    
    /**
     * Classic 게임 엔진 빈
     * 
     * 조건: tetris.game.item.enabled = false 또는 설정 없음 (기본값)
     * 
     * @return ClassicGameEngine 인스턴스
     */
    @Bean
    @ConditionalOnProperty(
        name = "tetris.game.item.enabled", 
        havingValue = "false", 
        matchIfMissing = true
    )
    public GameEngine classicGameEngine() {
        System.out.println("🎮 [CoreConfig] Registering ClassicGameEngine bean");
        return new ClassicGameEngine();
    }
    
    /**
     * Arcade 게임 엔진 빈
     *
     * 조건: tetris.game.item.enabled = true
     * 
     * 주의: CoreConfig는 Spring Context 초기화용 기본 빈만 제공합니다.
     * 실제 게임 실행 시에는 GameModeConfigFactory에서 YML 기반으로 생성된
     * Config를 사용하므로 이 빈의 설정값은 사용되지 않습니다.
     *
     * @return ArcadeGameEngine 인스턴스
     */
    @Bean
    @ConditionalOnProperty(
        name = "tetris.game.item.enabled",
        havingValue = "true"
    )
    public GameEngine arcadeGameEngine() {
        System.out.println("🎮 [CoreConfig] Registering ArcadeGameEngine bean (Default Config)");
        System.out.println("   ⚠️  This uses hardcoded default. Real game uses YML-based config from Factory.");
        
        // Spring Context 초기화용 기본 설정 (YML 값과 동기화 필요)
        GameModeConfig defaultConfig = createDefaultArcadeConfig();
        return new ArcadeGameEngine(defaultConfig);
    }
    
    /**
     * Arcade 기본 설정 생성
     * 
     * 주의: 이 설정은 game-modes.yml의 arcade 설정과 동기화되어야 합니다.
     * YML 값을 변경하면 여기도 함께 변경해야 합니다.
     * 
     * @return 기본 Arcade GameModeConfig
     */
    private GameModeConfig createDefaultArcadeConfig() {
        return GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .difficulty(seoultech.se.core.model.enumType.Difficulty.NORMAL)
            .srsEnabled(true)
            .rotation180Enabled(false)
            .hardDropEnabled(true)
            .holdEnabled(true)
            .ghostPieceEnabled(true)
            .dropSpeedMultiplier(1.0)
            .softDropSpeed(20.0)
            .lockDelay(500)
            .maxLockResets(15)
            .linesPerItem(10)
            .itemDropRate(0.15)  // Deprecated
            .maxInventorySize(3)
            .itemAutoUse(false)
            .enabledItemTypes(java.util.EnumSet.of(
                seoultech.se.core.engine.item.ItemType.LINE_CLEAR,
                seoultech.se.core.engine.item.ItemType.WEIGHT_BOMB,
                seoultech.se.core.engine.item.ItemType.PLUS,
                seoultech.se.core.engine.item.ItemType.SPEED_RESET,
                seoultech.se.core.engine.item.ItemType.BONUS_SCORE,
                seoultech.se.core.engine.item.ItemType.BOMB
            ))
            .build();
    }
    
    /**
     * ItemManager 빈
     * 
     * 조건: tetris.game.item.enabled = true
     * 의존성: GameModeConfig (YML 기반 설정)
     * 
     * 리팩토링 완료: ItemConfig 제거, YML 설정 직접 사용
     * 
     * @param gameModeConfig 게임 모드 설정
     * @return ItemManager 인스턴스
     */
    @Bean
    @ConditionalOnProperty(
        name = "tetris.game.item.enabled", 
        havingValue = "true"
    )
    public ItemManager itemManager(GameModeConfig gameModeConfig) {
        System.out.println("📦 [CoreConfig] Registering ItemManager bean (YML-based)");
        
        if (gameModeConfig == null || !gameModeConfig.isItemSystemEnabled()) {
            System.out.println("⚠️ [CoreConfig] Item system not enabled, using default");
            return new ItemManager();
        }
        
        // GameModeConfig에서 직접 값 가져오기 (ItemConfig 제거)
        return new ItemManager(
            gameModeConfig.getLinesPerItem(),
            gameModeConfig.getEnabledItemTypes()
        );
    }
    
    /**
     * GameModeConfig 빈
     * 
     * 테스트나 독립 실행 시 기본 설정 제공
     * (실제 환경에서는 GameModeConfigFactory에서 생성된 Config 사용)
     * 
     * 리팩토링 완료: ItemConfig 제거, YML 기반 설정 사용
     * 
     * @return GameModeConfig 인스턴스
     */
    @Bean
    @ConditionalOnProperty(
        name = "tetris.game.standalone", 
        havingValue = "true",
        matchIfMissing = false
    )
    public GameModeConfig defaultGameModeConfig() {
        System.out.println("⚙️ [CoreConfig] Creating default GameModeConfig (YML-based)");
        
        // 기본 Classic 모드 설정 (아이템 없음)
        return GameModeConfig.builder()
            .gameplayType(seoultech.se.core.config.GameplayType.CLASSIC)
            .difficulty(seoultech.se.core.model.enumType.Difficulty.NORMAL)
            .linesPerItem(0)
            .itemDropRate(0.0)  // Deprecated
            .maxInventorySize(0)
            .itemAutoUse(false)
            .enabledItemTypes(java.util.Collections.emptySet())
            .build();
    }
}
