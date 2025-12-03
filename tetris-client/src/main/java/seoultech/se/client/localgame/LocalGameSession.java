package seoultech.se.client.localgame;

import seoultech.se.client.localgame.LocalGameStatus;
import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.TetrominoType;
import seoultech.se.core.random.TetrominoGenerator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 로컬 2인용 대전 모드를 관리하는 게임 세션 클래스.
 * 서버의 GameSession을 로컬 환경에 맞게 단순화한 버전.
 */
public class LocalGameSession {

    private final Map<String, GameState> playerStates = new ConcurrentHashMap<>();
    private final Map<String, Integer> pendingAttackLines = new ConcurrentHashMap<>();
    private final Map<String, TetrominoGenerator> playerGenerators = new ConcurrentHashMap<>();
    private final GameEngine gameEngine;
    private GameModeConfig gameModeConfig;
    private final Object lock = new Object();

    public LocalGameSession(GameEngine gameEngine, GameModeConfig config) {
        this.gameEngine = gameEngine;
        this.gameModeConfig = config;
        System.out.println("✅ LocalGameSession Created, Mode: " + config.getGameplayType());
    }

    public void addPlayer(String playerId) {
        synchronized (lock) {
            seoultech.se.core.random.RandomGenerator randomGen = new seoultech.se.core.random.RandomGenerator();
            TetrominoGenerator generator = new TetrominoGenerator(randomGen, gameModeConfig.getDifficulty());
            playerGenerators.put(playerId, generator);

            GameState initialState = new GameState(10, 20);
            initialState.setLinesUntilNextItem(gameModeConfig.getLinesPerItem()); // 아이템 등장까지 남은 라인 수 초기화
            spawnNextBlock(initialState, playerId);

            playerStates.put(playerId, initialState);
            pendingAttackLines.put(playerId, 0);

            System.out.println("✅ Player added to LocalGameSession: " + playerId);
        }
    }

    private void spawnNextBlock(GameState state, String playerId) {
        TetrominoGenerator generator = playerGenerators.get(playerId);
        if (generator == null || state.isGameOver()) return;

        // 1. 다음 블록을 생성기에서 가져옴
        TetrominoType nextType = generator.next();
        state.setCurrentTetromino(new Tetromino(nextType));
        state.setCurrentX((state.getBoardWidth() - state.getCurrentTetromino().getCurrentShape()[0].length) / 2);
        state.setCurrentY(0);
        state.setHoldUsedThisTurn(false);

        // 아이템 적용
        if (state.getNextBlockItemType() != null) {
            state.setCurrentItemType(state.getNextBlockItemType());
            state.setNextBlockItemType(null); // 사용했으므로 초기화
        } else {
            state.setCurrentItemType(null);
        }


        // 2. Next Queue 업데이트
        List<TetrominoType> previewTypes = generator.preview(state.getNextQueue().length);
        for (int i = 0; i < previewTypes.size(); i++) {
            state.getNextQueue()[i] = previewTypes.get(i);
        }
    }

    public LocalGameStatus processCommand(String playerId, seoultech.se.core.command.GameCommand command) {
        synchronized (lock) {
            // Pause/Resume는 모든 플레이어에게 적용
            if (command.getType() == seoultech.se.core.command.CommandType.PAUSE || 
                command.getType() == seoultech.se.core.command.CommandType.RESUME) {
                
                for (String pId : playerStates.keySet()) {
                    GameState s = playerStates.get(pId);
                    if (s != null) {
                        playerStates.put(pId, gameEngine.executeCommand(command, s));
                    }
                }
                return new LocalGameStatus(playerStates.get("P1"), playerStates.get("P2"));
            }

            GameState currentState = playerStates.get(playerId);
            if (currentState == null || currentState.isGameOver()) {
                return new LocalGameStatus(playerStates.get("P1"), playerStates.get("P2"));
            }

            GameState nextState = gameEngine.executeCommand(command, currentState);

            boolean pieceLocked = (currentState.getCurrentTetromino() != null && nextState.getCurrentTetromino() == null);

            if (pieceLocked) {
                // 1. 쌓인 공격이 있다면 현재 플레이어에게 적용
                int garbageToAdd = pendingAttackLines.getOrDefault(playerId, 0);
                if (garbageToAdd > 0) {
                    boolean isGameOverByAttack = nextState.addGarbageLines(garbageToAdd);
                    if(isGameOverByAttack) nextState.setGameOver(true);
                    pendingAttackLines.put(playerId, 0); // 적용 후 초기화
                }

                // 2. 방금 클리어한 라인으로 상대에게 공격
                if (nextState.getLastLinesCleared() > 0) {
                    processAttack(nextState, playerId);
                }
                
                // 3. 새 블록 스폰
                if (!nextState.isGameOver()) {
                    spawnNextBlock(nextState, playerId);
                }
            }

            playerStates.put(playerId, nextState);
            return new LocalGameStatus(playerStates.get("P1"), playerStates.get("P2"));
        }
    }

    public LocalGameStatus applyGravity(String playerId) {
        seoultech.se.core.command.MoveCommand downCommand = new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.DOWN);
        return processCommand(playerId, downCommand);
    }
    
    private void processAttack(GameState attackerState, String attackerId) {
        int linesCleared = attackerState.getLastLinesCleared();
        if (linesCleared <= 1) return; // 2줄 이상부터 공격

        String opponentId = getOpponentId(attackerId);
        if (opponentId == null || playerStates.get(opponentId).isGameOver()) return;

        int attackLines = 0;
        switch (linesCleared) {
            case 2: attackLines = 1; break; // Double
            case 3: attackLines = 2; break; // Triple
            case 4: attackLines = 4; break; // Tetris
        }
        if (attackerState.isLastLockWasTSpin()) {
            attackLines = linesCleared * 2; // T-Spin bonus
        }

        if (attackLines > 0) {
            int currentOpponentPending = pendingAttackLines.getOrDefault(opponentId, 0);
            pendingAttackLines.put(opponentId, currentOpponentPending + attackLines);
            System.out.println("⚔️ Attack: " + attackerId + " -> " + opponentId + " (" + attackLines + " lines pending)");
        }
    }

    private String getOpponentId(String playerId) {
        return "P1".equals(playerId) ? "P2" : "P1";
    }

    public GameState getStateForPlayer(String playerId) {
        return playerStates.get(playerId);
    }
}
