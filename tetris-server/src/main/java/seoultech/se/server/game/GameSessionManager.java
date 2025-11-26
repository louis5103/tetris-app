package seoultech.se.server.game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.factory.GameEnginePool;

/**
 * 게임 세션 매니저
 *
 * Stateless 리팩토링: GameEnginePool을 통해 싱글톤 GameEngine 사용
 *
 * 변경 사항:
 * - GameEnginePool 주입
 * - 각 세션은 공유 GameEngine을 사용
 * - 메모리 효율: 1000개 세션이 2개의 GameEngine만 공유
 */
@Service
public class GameSessionManager {

    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final GameEnginePool gameEnginePool;

    @Autowired
    public GameSessionManager(GameEnginePool gameEnginePool) {
        this.gameEnginePool = gameEnginePool;
        System.out.println("✅ [GameSessionManager] Created with GameEnginePool");
    }

    /**
     * 세션 생성 (GameplayType 지정)
     *
     * @param sessionId 세션 ID
     * @param gameplayType 게임플레이 타입 (CLASSIC, ARCADE)
     * @return 생성된 세션
     */
    public GameSession createSession(String sessionId, GameplayType gameplayType) {
        // Pool에서 싱글톤 GameEngine 가져오기
        GameEngine sharedEngine = gameEnginePool.getEngine(gameplayType);

        GameSession session = new GameSession(sessionId, sharedEngine);
        sessions.put(sessionId, session);

        System.out.println("🎮 [GameSessionManager] Session created: " + sessionId +
            ", GameplayType: " + gameplayType);

        return session;
    }

    /**
     * 세션 생성 (기본값: CLASSIC)
     *
     * @param sessionId 세션 ID
     * @return 생성된 세션
     */
    public GameSession createSession(String sessionId) {
        return createSession(sessionId, GameplayType.CLASSIC);
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

