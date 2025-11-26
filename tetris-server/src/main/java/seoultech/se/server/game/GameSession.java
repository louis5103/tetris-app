package seoultech.se.server.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import seoultech.se.core.GameState;
import seoultech.se.core.dto.PlayerInputDto;
import seoultech.se.core.dto.ServerStateDto;
import seoultech.se.core.engine.GameEngine;

/**
 * 게임 세션
 *
 * Stateless 리팩토링: 싱글톤 GameEngine을 공유하여 사용
 *
 * 변경 사항:
 * - GameEngine을 외부에서 주입받음 (GameEnginePool의 싱글톤)
 * - GameEngineFactory.createGameEngine() 제거
 * - 여러 세션이 동일한 GameEngine 인스턴스를 공유
 *
 * Thread-safety:
 * - GameEngine은 Stateless이므로 동시 접근 안전
 * - playerStates는 ConcurrentHashMap으로 보호
 * - processInput은 synchronized로 보호
 */
public class GameSession {

    private final String sessionId;
    private final Map<String, GameState> playerStates = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSequences = new ConcurrentHashMap<>();
    private final GameEngine gameEngine; // 싱글톤 공유

    private final Object lock = new Object(); // 동기화를 위한 락 객체

    /**
     * 생성자 (GameEngine 주입)
     *
     * @param sessionId 세션 ID
     * @param gameEngine 싱글톤 GameEngine (GameEnginePool에서 제공)
     */
    public GameSession(String sessionId, GameEngine gameEngine) {
        this.sessionId = sessionId;
        this.gameEngine = gameEngine;
        System.out.println("✅ [GameSession] Created: " + sessionId +
            ", Engine: " + (gameEngine != null ? gameEngine.getClass().getSimpleName() : "null"));
    }

    public void joinPlayer(String playerId) {
        playerStates.put(playerId, new GameState(10, 20)); // 초기 상태
        lastSequences.put(playerId, 0L); // 초기 시퀀스 번호
    }

    public ServerStateDto processInput(String playerId, PlayerInputDto input){
        synchronized(lock){
            GameState currentState = playerStates.get(playerId);
            
            // 1. 시퀀스 검증 (오래된 패킷 무시)
            long lastSeq = lastSequences.getOrDefault(playerId, 0L);
            if (input.getSequenceId() <= lastSeq) {
                return null; // 이미 처리된 입력은 무시
            }

            // 2. 서버 권한으로 로직 실행
            GameState nextState = gameEngine.executeCommand(input.getCommand(), currentState);
            
            // 3. 상태 업데이트
            playerStates.put(playerId, nextState);
            lastSequences.put(playerId, input.getSequenceId());

            // 4. 이벤트 감지 (예: 라인 클리어)
            List<String> events = new ArrayList<>();
            if (nextState.getLastLinesCleared() > 0) {
                events.add("LINE_CLEAR");
                // TODO: 상대방에게 공격(Garbage Lines) 로직 추가
            }

            // 5. 상대방 ID 찾기
            String opponentId = playerStates.keySet().stream()
                    .filter(id -> !id.equals(playerId))
                    .findFirst()
                    .orElse(null);

            return ServerStateDto.builder()
                    .lastProcessedSequence(input.getSequenceId())
                    .myGameState(nextState)
                    .opponentGameState(opponentId != null ? playerStates.get(opponentId) : null)
                    .events(events)
                    .build();
        }
    }
}
