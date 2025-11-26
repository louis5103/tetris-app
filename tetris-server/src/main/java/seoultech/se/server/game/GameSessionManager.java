package seoultech.se.server.game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.engine.factory.GameEnginePool;
import seoultech.se.core.model.enumType.Difficulty;
import seoultech.se.server.service.ServerConfigFactory;

/**
 * 게임 세션 매니저
 *
 * Stateless 리팩토링: GameEnginePool을 통해 싱글톤 GameEngine 사용
 *
 * 변경 사항:
 * - GameEnginePool 주입
 * - 각 세션은 공유 GameEngine을 사용
 * - 메모리 효율: 1000개 세션이 2개의 GameEngine만 공유
 * - ServerConfigFactory 주입: 세션 생성 시 기본 GameModeConfig 생성
 */
@Service
public class GameSessionManager {

    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final GameEnginePool gameEnginePool;
    private final ServerConfigFactory serverConfigFactory;

    @Autowired
    public GameSessionManager(GameEnginePool gameEnginePool, ServerConfigFactory serverConfigFactory) {
        this.gameEnginePool = gameEnginePool;
        this.serverConfigFactory = serverConfigFactory;
        System.out.println("✅ [GameSessionManager] Created with GameEnginePool and ServerConfigFactory");
    }

    /**
     * 세션 생성 (GameplayType + Difficulty 지정)
     *
     * @param sessionId 세션 ID
     * @param gameplayType 게임플레이 타입 (CLASSIC, ARCADE)
     * @param difficulty 난이도 (EASY, NORMAL, HARD)
     * @return 생성된 세션
     */
    public GameSession createSession(String sessionId, GameplayType gameplayType, Difficulty difficulty) {
        // Pool에서 싱글톤 GameEngine 가져오기
        GameEngine sharedEngine = gameEnginePool.getEngine(gameplayType);

        // 세션 생성
        GameSession session = new GameSession(sessionId, sharedEngine);
        
        // 기본 Config 설정 (Factory 사용)
        GameModeConfig defaultConfig = serverConfigFactory.createConfig(gameplayType, difficulty);
        session.setGameModeConfig(null, defaultConfig); // null = 초기 설정 (호스트 검증 생략)
        
        sessions.put(sessionId, session);

        System.out.println("🎮 [GameSessionManager] Session created: " + sessionId +
            ", GameplayType: " + gameplayType + ", Difficulty: " + difficulty);

        return session;
    }

    /**
     * 세션 생성 (GameplayType 지정, Difficulty 기본값)
     *
     * @param sessionId 세션 ID
     * @param gameplayType 게임플레이 타입 (CLASSIC, ARCADE)
     * @return 생성된 세션
     */
    public GameSession createSession(String sessionId, GameplayType gameplayType) {
        return createSession(sessionId, gameplayType, Difficulty.NORMAL);
    }

    /**
     * 세션 생성 (기본값: CLASSIC, NORMAL)
     *
     * @param sessionId 세션 ID
     * @return 생성된 세션
     */
    public GameSession createSession(String sessionId) {
        return createSession(sessionId, GameplayType.CLASSIC, Difficulty.NORMAL);
    }

    /**
     * 세션 조회
     *
     * @param sessionId 세션 ID
     * @return 세션 (없으면 null)
     */
    public GameSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * 세션 제거
     *
     * @param sessionId 세션 ID
     */
    public void removeSession(String sessionId) {
        GameSession removed = sessions.remove(sessionId);
        if (removed != null) {
            System.out.println("🗑️ [GameSessionManager] Session removed: " + sessionId);
        }
    }

    /**
     * 모든 세션 개수 반환
     *
     * @return 활성 세션 개수
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }

    /**
     * 모든 세션 제거
     */
    public void clearAllSessions() {
        sessions.clear();
        System.out.println("🗑️ [GameSessionManager] All sessions cleared");
    }
}

