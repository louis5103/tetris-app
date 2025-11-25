package seoultech.se.server.game;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class GameSessionManager {
    private final Map<String, GameSession> sessions = new ConcurrentHashMap<>();

    public GameSession createSession(String sessionId){
        GameSession session = new GameSession(sessionId, null);
        sessions.put(sessionId, session);
        return session;
    }

    public GameSession getSession(String sessionId){
        return sessions.get(sessionId);
    }

    public void removeSession(String sessionId){
        sessions.remove(sessionId);
    }
}
