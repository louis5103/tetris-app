package seoultech.se.server.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.dto.PlayerInputDto;
import seoultech.se.core.dto.ServerStateDto;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.factory.GameEngineFactory;

public class GameSession {
    private final String sessionId;
    private final Map<String, GameState> playerStates = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSequences = new ConcurrentHashMap<>();
    private final GameEngine gameEngine;

    private final Object lock = new Object(); // 동기화를 위한 락 객체

    public GameSession(String sessionId, GameModeConfig config) {
        this.sessionId = sessionId;
        this.gameEngine = new GameEngineFactory().createGameEngine(config);
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
