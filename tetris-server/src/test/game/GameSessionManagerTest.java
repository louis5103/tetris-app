package seoultech.se.server.game;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.factory.GameEngineFactory;
import seoultech.se.core.factory.GameEnginePool;

class GameSessionManagerTest {

    private GameSessionManager sessionManager;
    private GameEnginePool gameEnginePool;

    @BeforeEach
    void setUp() {
        // GameEnginePool 생성 (테스트용)
        GameEngineFactory factory = new GameEngineFactory();
        gameEnginePool = new GameEnginePool(factory);
        sessionManager = new GameSessionManager(gameEnginePool);
    }

    @Test
    @DisplayName("세션 생성 및 조회 성공")
    void createAndGetSession() {
        // Given
        String sessionId = "room-1";

        // When
        GameSession createdSession = sessionManager.createSession(sessionId);
        GameSession retrievedSession = sessionManager.getSession(sessionId);

        // Then
        assertThat(createdSession).isNotNull();
        assertThat(retrievedSession).isNotNull();
        assertThat(createdSession).isSameAs(retrievedSession); // 같은 인스턴스여야 함
    }

    @Test
    @DisplayName("세션 삭제 성공")
    void removeSession() {
        // Given
        String sessionId = "room-to-delete";
        sessionManager.createSession(sessionId);

        // When
        sessionManager.removeSession(sessionId);

        // Then
        GameSession session = sessionManager.getSession(sessionId);
        assertThat(session).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 세션 조회 시 null 반환")
    void getNonExistentSession() {
        GameSession session = sessionManager.getSession("ghost-room");
        assertThat(session).isNull();
    }
}
