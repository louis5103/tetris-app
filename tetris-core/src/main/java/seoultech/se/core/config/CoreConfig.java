package seoultech.se.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import seoultech.se.core.engine.ArcadeGameEngine;
import seoultech.se.core.engine.ClassicGameEngine;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.item.ItemConfig;
import seoultech.se.core.item.ItemManager;

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
     * Stateless 리팩토링: Config를 생성자로 주입
     *
     * @return ArcadeGameEngine 인스턴스
     */
    @Bean
    @ConditionalOnProperty(
        name = "tetris.game.item.enabled",
        havingValue = "true"
    )
    public GameEngine arcadeGameEngine() {
        System.out.println("🎮 [CoreConfig] Registering ArcadeGameEngine bean (Stateless)");
        return new ArcadeGameEngine(GameModeConfig.arcade());
    }
    
    /**
     * ItemManager 빈
     * 
     * 조건: tetris.game.item.enabled = true
     * 의존성: ItemConfig (GameModeConfig에서 가져옴)
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
        System.out.println("📦 [CoreConfig] Registering ItemManager bean");
        
        ItemConfig itemConfig = gameModeConfig.getItemConfig();
        
        if (itemConfig == null) {
            // ItemConfig가 없으면 기본값으로 생성
            System.out.println("⚠️ [CoreConfig] ItemConfig is null, using default values");
            return new ItemManager();
        }
        
        return new ItemManager(
            itemConfig.getDropRate(),
            itemConfig.getEnabledItems()
        );
    }
    
    /**
     * GameModeConfig 빈
     * 
     * 테스트나 독립 실행 시 기본 설정 제공
     * (실제 환경에서는 SettingsService에서 생성된 Config 사용)
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
        System.out.println("⚙️ [CoreConfig] Creating default GameModeConfig");
        
        // 기본 Classic 모드 설정
        return GameModeConfig.builder()
            .gameModeType(seoultech.se.core.mode.GameModeType.CLASSIC)
            .difficulty(seoultech.se.core.model.enumType.Difficulty.NORMAL)
            .itemConfig(null)  // Classic 모드는 아이템 없음
            .build();
    }
}
