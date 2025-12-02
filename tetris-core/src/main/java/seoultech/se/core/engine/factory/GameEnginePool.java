package seoultech.se.core.engine.factory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.engine.GameEngine;

/**
 * GameEngine Pool (Singleton 관리)
 *
 * Stateless 리팩토링: GameEngine을 GameplayType별로 싱글톤으로 캐싱
 *
 * 목적:
 * - GameEngine은 Stateless이므로 여러 세션이 공유 가능
 * - 메모리 효율: 1000개 세션이 있어도 2개의 GameEngine만 생성 (CLASSIC, ARCADE)
 * - Thread-safe: GameEngine은 불변 설정만 보유하므로 동시 접근 안전
 *
 * 사용 예시:
 * ```java
 * @Autowired
 * private GameEnginePool enginePool;
 *
 * GameEngine engine = enginePool.getEngine(GameplayType.CLASSIC);
 * GameState newState = engine.tryMoveLeft(currentState);
 * ```
 *
 * 설계 원칙:
 * - Singleton Pattern: GameplayType별로 하나의 인스턴스만 생성
 * - Thread-Safe: ConcurrentHashMap 사용
 * - Lazy Initialization: 첫 요청 시 생성
 */
@Component
public class GameEnginePool {

    private final GameEngineFactory gameEngineFactory;

    /**
     * GameplayType별 싱글톤 GameEngine 캐시
     * Key: GameplayType (CLASSIC, ARCADE)
     * Value: GameEngine 인스턴스
     */
    private final Map<GameplayType, GameEngine> engineCache = new ConcurrentHashMap<>();

    @Autowired
    public GameEnginePool(GameEngineFactory gameEngineFactory) {
        this.gameEngineFactory = gameEngineFactory;
        System.out.println("✅ [GameEnginePool] Created (Stateless Engine Singleton Manager)");
    }

    /**
     * GameplayType에 해당하는 GameEngine을 반환 (캐싱)
     *
     * Thread-safe: computeIfAbsent는 원자적 연산
     *
     * @param gameplayType 게임플레이 타입
     * @return 싱글톤 GameEngine 인스턴스
     */
    public GameEngine getEngine(GameplayType gameplayType) {
        if (gameplayType == null) {
            gameplayType = GameplayType.CLASSIC;
        }

        return engineCache.computeIfAbsent(gameplayType, type -> {
            GameModeConfig config = createDefaultConfig(type);
            GameEngine engine = gameEngineFactory.createGameEngine(config);
            System.out.println("🎮 [GameEnginePool] Engine cached: " + type);
            return engine;
        });
    }

    /**
     * Config 기반으로 GameEngine을 반환 (캐싱)
     *
     * 동일한 GameplayType이면 동일한 인스턴스 반환
     *
     * @param config 게임 모드 설정
     * @return 싱글톤 GameEngine 인스턴스
     */
    public GameEngine getEngine(GameModeConfig config) {
        if (config == null) {
            return getEngine(GameplayType.CLASSIC);
        }
        return getEngine(config.getGameplayType());
    }

    /**
     * GameplayType에 대한 기본 Config 생성
     *
     * @param type 게임플레이 타입
     * @return 기본 GameModeConfig
     */
    private GameModeConfig createDefaultConfig(GameplayType type) {
        if (type == GameplayType.ARCADE) {
            return GameModeConfig.createDefaultArcade();
        } else {
            return GameModeConfig.createDefaultClassic();
        }
    }

    /**
     * 캐시 초기화 (테스트용)
     *
     * 운영 환경에서는 사용하지 않음
     */
    public void clearCache() {
        engineCache.clear();
        System.out.println("🔄 [GameEnginePool] Cache cleared");
    }

    /**
     * 캐시된 엔진 개수 반환
     *
     * @return 캐시된 엔진 개수
     */
    public int getCachedEngineCount() {
        return engineCache.size();
    }

    /**
     * 특정 타입의 엔진이 캐시되어 있는지 확인
     *
     * @param gameplayType 게임플레이 타입
     * @return 캐시 여부
     */
    public boolean isCached(GameplayType gameplayType) {
        return engineCache.containsKey(gameplayType);
    }

    @Override
    public String toString() {
        return String.format("GameEnginePool[CachedEngines=%d, Types=%s]",
            engineCache.size(),
            engineCache.keySet());
    }
}
