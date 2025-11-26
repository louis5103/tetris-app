package seoultech.se.core.engine.factory;

import org.springframework.stereotype.Component;

import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.engine.ArcadeGameEngine;
import seoultech.se.core.engine.ClassicGameEngine;
import seoultech.se.core.engine.GameEngine;

/**
 * GameEngine Factory
 * 
 * GameModeConfig에 따라 적절한 GameEngine 인스턴스를 생성합니다.
 * 
 * Strategy Pattern + Factory Pattern:
 * - CLASSIC 모드 → ClassicGameEngine
 * - ARCADE 모드 → ArcadeGameEngine (with ItemManager)
 * 
 * 사용 예시:
 * ```java
 * GameModeConfig config = GameModeConfig.arcade();
 * GameEngine engine = gameEngineFactory.createGameEngine(config);
 * ```
 */
@Component
public class GameEngineFactory {
    
    /**
     * GameModeConfig에 따라 적절한 GameEngine을 생성합니다
     *
     * Stateless 리팩토링: Config를 생성자로 주입
     *
     * @param config 게임 모드 설정
     * @return 생성된 GameEngine 인스턴스
     */
    public GameEngine createGameEngine(GameModeConfig config) {
        if (config == null) {
            System.out.println("⚠️ [GameEngineFactory] Config is null, creating ClassicGameEngine with default config");
            return new ClassicGameEngine();
        }

        GameplayType gameplayType = config.getGameplayType();

        if (gameplayType == GameplayType.ARCADE) {
            System.out.println("🎮 [GameEngineFactory] Creating ArcadeGameEngine (Stateless)");
            return new ArcadeGameEngine(config);
        } else {
            System.out.println("🎮 [GameEngineFactory] Creating ClassicGameEngine (Stateless)");
            return new ClassicGameEngine(config);
        }
    }
    
    /**
     * 게임플레이 타입만으로 GameEngine을 생성합니다
     * 
     * @param gameplayType 게임플레이 타입
     * @return 생성된 GameEngine 인스턴스
     */
    public GameEngine createGameEngine(GameplayType gameplayType) {
        if (gameplayType == GameplayType.ARCADE) {
            GameModeConfig config = GameModeConfig.arcade();
            return createGameEngine(config);
        } else {
            GameModeConfig config = GameModeConfig.classic();
            return createGameEngine(config);
        }
    }
}
