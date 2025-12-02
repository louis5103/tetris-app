package seoultech.se.server.game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
 * - Phase 1: 세션 타임아웃 자동 정리 (application.yml에서 설정 가능)
 */
@Service
public class GameSessionManager {

    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();
    private final GameEnginePool gameEnginePool;
    private final ServerConfigFactory serverConfigFactory;

    /**
     * Phase 1: 세션 타임아웃 설정 (application.yml에서 주입)
     * 기본값: 30분 (1800000 밀리초)
     */
    @Value("${game.session.timeout:1800000}")
    private long sessionTimeoutMs;

    @Autowired
    public GameSessionManager(GameEnginePool gameEnginePool, ServerConfigFactory serverConfigFactory) {
        this.gameEnginePool = gameEnginePool;
        this.serverConfigFactory = serverConfigFactory;
        System.out.println("✅ [GameSessionManager] Created with GameEnginePool and ServerConfigFactory");
    }

    /**
     * 세션 생성 (GameplayType + Difficulty + SessionType 지정)
     *
     * @param sessionId 세션 ID
     * @param gameplayType 게임플레이 타입 (CLASSIC, ARCADE)
     * @param difficulty 난이도 (EASY, NORMAL, HARD)
     * @param sessionType 세션 타입 (SINGLE/MULTI)
     * @return 생성된 세션
     */
    public GameSession createSession(String sessionId, GameplayType gameplayType, Difficulty difficulty, SessionType sessionType) {
        // Pool에서 싱글톤 GameEngine 가져오기
        GameEngine sharedEngine = gameEnginePool.getEngine(gameplayType);

        // 세션 생성
        GameSession session = new GameSession(sessionId, sharedEngine, sessionType);

        // 기본 Config 설정 (Factory 사용)
        GameModeConfig defaultConfig = serverConfigFactory.createConfig(gameplayType, difficulty);
        session.setGameModeConfig(null, defaultConfig); // null = 초기 설정 (호스트 검증 생략)

        sessions.put(sessionId, session);

        System.out.println("🎮 [GameSessionManager] Session created: " + sessionId +
            ", Type: " + sessionType + ", GameplayType: " + gameplayType + ", Difficulty: " + difficulty);

        return session;
    }

    /**
     * 세션 생성 (GameplayType + Difficulty 지정, SessionType은 SINGLE)
     *
     * @param sessionId 세션 ID
     * @param gameplayType 게임플레이 타입 (CLASSIC, ARCADE)
     * @param difficulty 난이도 (EASY, NORMAL, HARD)
     * @return 생성된 세션
     */
    public GameSession createSession(String sessionId, GameplayType gameplayType, Difficulty difficulty) {
        return createSession(sessionId, gameplayType, difficulty, SessionType.SINGLE);
    }

    /**
     * 세션 생성 (GameplayType 지정, Difficulty 기본값)
     *
     * @param sessionId 세션 ID
     * @param gameplayType 게임플레이 타입 (CLASSIC, ARCADE)
     * @return 생성된 세션
     */
    public GameSession createSession(String sessionId, GameplayType gameplayType) {
        return createSession(sessionId, gameplayType, Difficulty.NORMAL, SessionType.SINGLE);
    }

    /**
     * 세션 생성 (기본값: CLASSIC, NORMAL, SINGLE)
     *
     * @param sessionId 세션 ID
     * @return 생성된 세션
     */
    public GameSession createSession(String sessionId) {
        return createSession(sessionId, GameplayType.CLASSIC, Difficulty.NORMAL, SessionType.SINGLE);
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
     * 세션에서 플레이어 제거
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
     * 플레이어 온라인 상태 설정
     * 
     * @param sessionId 세션 ID
     * @param playerId 플레이어 ID
     * @param isOnline 온라인 여부
     */
    public void setPlayerOnline(String sessionId, String playerId, boolean isOnline) {
        GameSession session = sessions.get(sessionId);
        if (session != null) {
            session.setPlayerOnline(playerId, isOnline);
        }
    }

    /**
     * Phase 1: 세션에서 플레이어 제거
     *
     * @param sessionId 게임 세션 ID
     * @param playerId 플레이어 ID
     * @return 제거 성공 여부
     */
    public boolean removePlayerFromSession(String sessionId, String playerId) {
        GameSession session = sessions.get(sessionId);
        if (session != null) {
            boolean removed = session.removePlayer(playerId);

            // 세션에 플레이어가 없으면 세션도 제거
            if (removed && session.getPlayerCount() == 0) {
                removeSession(sessionId);
                System.out.println("🗑️ [GameSessionManager] Empty session removed: " + sessionId);
            }

            return removed;
        }
        return false;
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
     * 멀티플레이 세션 목록 조회
     *
     * @return 모든 멀티플레이 세션 (Map<SessionId, GameSession>)
     */
    public Map<String, GameSession> getMultiplayerSessions() {
        Map<String, GameSession> multiSessions = new ConcurrentHashMap<>();

        for (Map.Entry<String, GameSession> entry : sessions.entrySet()) {
            GameSession session = entry.getValue();
            if (session.getSessionType() == SessionType.MULTI) {
                multiSessions.put(entry.getKey(), session);
            }
        }

        return multiSessions;
    }

    /**
     * 모든 세션 제거
     */
    public void clearAllSessions() {
        sessions.clear();
        System.out.println("🗑️ [GameSessionManager] All sessions cleared");
    }

    /**
     * Phase 1: 비활성 세션 자동 정리 (매 1분마다 실행)
     *
     * - application.yml의 game.session.timeout 설정 사용
     * - 마지막 활동으로부터 timeout 시간이 지난 세션 삭제
     * - 삭제된 세션 수를 로그에 출력
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void cleanupInactiveSessions() {
        long currentTime = System.currentTimeMillis();
        int removedCount = 0;

        // 비활성 세션 찾아서 제거
        var iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            GameSession session = entry.getValue();

            long inactiveTime = currentTime - session.getLastActivityTime();

            if (inactiveTime > sessionTimeoutMs) {
                String sessionId = entry.getKey();
                iterator.remove();
                removedCount++;

                System.out.println("⏰ [GameSessionManager] Session timeout: " + sessionId +
                    " (inactive for " + (inactiveTime / 1000) + " seconds)");
            }
        }

        // 정리 결과 로그 (세션이 삭제된 경우만)
        if (removedCount > 0) {
            System.out.println("🧹 [GameSessionManager] Cleanup completed: " + removedCount +
                " session(s) removed, " + sessions.size() + " active session(s) remaining");
        }
    }
}

