package seoultech.se.server.matchmaking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import seoultech.se.core.config.GameplayType;
import seoultech.se.core.model.enumType.Difficulty;
import seoultech.se.server.game.GameSessionManager;
import seoultech.se.server.matchmaking.MatchmakingService.MatchmakingResult;
import seoultech.se.server.matchmaking.MatchmakingService.MatchStatus;
import seoultech.se.server.websocket.WebSocketEventListener;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Phase 3: MatchmakingService 통합 테스트
 *
 * 테스트 시나리오:
 * 1. 단일 플레이어 참여 → WAITING 상태
 * 2. 두 번째 플레이어 참여 → 자동 매칭 성공
 * 3. 매칭 취소 기능
 * 4. 중복 참여 방지
 * 5. 큐 크기 조회
 */
class MatchmakingServiceTest {

    @Mock
    private GameSessionManager gameSessionManager;

    @Mock
    private WebSocketEventListener webSocketEventListener;

    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        matchmakingService = new MatchmakingService(gameSessionManager, webSocketEventListener);
    }

    @Test
    @DisplayName("단일 플레이어 참여 시 WAITING 상태 반환")
    void testSinglePlayerJoin_ReturnsWaiting() {
        // Given
        String playerId = "player1";
        GameplayType gameplayType = GameplayType.CLASSIC;
        Difficulty difficulty = Difficulty.NORMAL;

        // When
        MatchmakingResult result = matchmakingService.joinQueue(playerId, gameplayType, difficulty);

        // Then
        assertEquals(MatchStatus.WAITING, result.getStatus());
        assertNull(result.getSessionId());
    }

    @Test
    @DisplayName("두 플레이어 매칭 시 자동 매칭 성공")
    void testTwoPlayersJoin_AutoMatch() {
        // Given
        String player1Id = "player1";
        String player2Id = "player2";
        GameplayType gameplayType = GameplayType.CLASSIC;
        Difficulty difficulty = Difficulty.NORMAL;

        // Mock GameSession
        var mockSession = org.mockito.Mockito.mock(seoultech.se.server.game.GameSession.class);
        when(gameSessionManager.createSession(anyString(), any(GameplayType.class), any(Difficulty.class)))
            .thenReturn(mockSession);

        // When
        MatchmakingResult result1 = matchmakingService.joinQueue(player1Id, gameplayType, difficulty);
        MatchmakingResult result2 = matchmakingService.joinQueue(player2Id, gameplayType, difficulty);

        // Then
        assertEquals(MatchStatus.WAITING, result1.getStatus());
        assertEquals(MatchStatus.MATCHED, result2.getStatus());
        assertNotNull(result2.getSessionId());
        assertEquals(player1Id, result2.getPlayer1Id());
        assertEquals(player2Id, result2.getPlayer2Id());
    }

    @Test
    @DisplayName("매칭 취소 성공")
    void testLeaveQueue_Success() {
        // Given
        String playerId = "player1";
        GameplayType gameplayType = GameplayType.CLASSIC;
        Difficulty difficulty = Difficulty.NORMAL;

        matchmakingService.joinQueue(playerId, gameplayType, difficulty);

        // When
        boolean result = matchmakingService.leaveQueue(playerId);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("존재하지 않는 플레이어 매칭 취소 시 false 반환")
    void testLeaveQueue_PlayerNotInQueue_ReturnsFalse() {
        // Given
        String playerId = "nonexistent";

        // When
        boolean result = matchmakingService.leaveQueue(playerId);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("중복 참여 방지")
    void testDuplicateJoin_ReturnsAlreadyInQueue() {
        // Given
        String playerId = "player1";
        GameplayType gameplayType = GameplayType.CLASSIC;
        Difficulty difficulty = Difficulty.NORMAL;

        // 첫 번째 참여
        matchmakingService.joinQueue(playerId, gameplayType, difficulty);

        // When - 두 번째 참여 시도
        MatchmakingResult result = matchmakingService.joinQueue(playerId, gameplayType, difficulty);

        // Then
        assertEquals(MatchStatus.ALREADY_IN_QUEUE, result.getStatus());
    }

    @Test
    @DisplayName("큐 크기 조회")
    void testGetQueueSize() {
        // Given
        String player1Id = "player1";
        String player2Id = "player2";
        GameplayType gameplayType = GameplayType.CLASSIC;
        Difficulty difficulty = Difficulty.NORMAL;

        // Mock GameSession
        var mockSession = org.mockito.Mockito.mock(seoultech.se.server.game.GameSession.class);
        when(gameSessionManager.createSession(anyString(), any(GameplayType.class), any(Difficulty.class)))
            .thenReturn(mockSession);

        // When
        matchmakingService.joinQueue(player1Id, gameplayType, difficulty);
        matchmakingService.joinQueue(player2Id, gameplayType, difficulty);

        int queueSize = matchmakingService.getQueueSize(gameplayType, difficulty);

        // Then
        assertEquals(0, queueSize); // 두 명이 매칭되어 큐는 비어있음
    }

    @Test
    @DisplayName("다른 게임 모드는 별도 큐로 관리")
    void testDifferentGameModes_SeparateQueues() {
        // Given
        String player1Id = "player1";
        String player2Id = "player2";

        // When
        MatchmakingResult result1 = matchmakingService.joinQueue(
            player1Id, GameplayType.CLASSIC, Difficulty.NORMAL
        );
        MatchmakingResult result2 = matchmakingService.joinQueue(
            player2Id, GameplayType.ARCADE, Difficulty.NORMAL
        );

        // Then
        assertEquals(MatchStatus.WAITING, result1.getStatus());
        assertEquals(MatchStatus.WAITING, result2.getStatus()); // 다른 모드라서 매칭 안됨
    }

    @Test
    @DisplayName("매칭 대기 상태 조회")
    void testGetWaitingStatus() {
        // Given
        String playerId = "player1";
        GameplayType gameplayType = GameplayType.CLASSIC;
        Difficulty difficulty = Difficulty.NORMAL;

        matchmakingService.joinQueue(playerId, gameplayType, difficulty);

        // When
        var status = matchmakingService.getWaitingStatus(playerId);

        // Then
        assertTrue(status.isPresent());
        assertEquals(playerId, status.get().getPlayerId());
        assertEquals(gameplayType, status.get().getGameplayType());
        assertEquals(difficulty, status.get().getDifficulty());
    }
}
