package seoultech.se.client.controller;

import java.util.List;

import org.springframework.stereotype.Component;

import lombok.Getter;
import seoultech.se.client.mode.SingleMode;
import seoultech.se.client.strategy.GameExecutionStrategy;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.mode.GameMode;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.Difficulty;
import seoultech.se.core.model.enumType.TetrominoType;
import seoultech.se.core.random.RandomGenerator;
import seoultech.se.core.random.TetrominoGenerator;

@Getter
@Component
public class BoardController {
    private GameState gameState;
    private GameMode gameMode;

    // ✨ Strategy Pattern: 로컬/네트워크 실행 전략
    private GameExecutionStrategy executionStrategy;

    // ✨ Phase 4: 난이도 시스템 통합
    private Difficulty difficulty;  // 현재 난이도
    private TetrominoGenerator tetrominoGenerator;  // 7-bag 생성기

    private long gameStartTime;

    /**
     * 기본 생성자 (Classic 모드, Normal 난이도)
     */
    public BoardController() {
        this(GameModeConfig.classic(), Difficulty.NORMAL);
    }
    
    /**
     * GameModeConfig를 받는 생성자 (Normal 난이도)
     * 
     * @param config 게임 모드 설정
     */
    public BoardController(GameModeConfig config) {
        this(config, Difficulty.NORMAL);
    }
    
    /**
     * GameModeConfig와 Difficulty를 받는 생성자 ✨ Phase 4
     * 
     * @param config 게임 모드 설정
     * @param difficulty 난이도
     */
    public BoardController(GameModeConfig config, Difficulty difficulty) {
        this.gameState = new GameState(10, 20);
        this.gameStartTime = System.currentTimeMillis();
        this.difficulty = difficulty;
        
        // ✨ Phase 4: TetrominoGenerator 초기화 (결정론적 생성)
        this.tetrominoGenerator = new TetrominoGenerator(new RandomGenerator(), difficulty);
        
        // GameModeConfig에 따라 SingleMode 생성
        this.gameMode = new SingleMode(config);
        this.gameMode.initialize(this.gameState);
        
        initializeNextQueue();
        
        System.out.println("📦 BoardController created with config: " + 
            (config.getGameplayType() != null ? config.getGameplayType().getDisplayName() : "CLASSIC") +
            ", SRS: " + config.isSrsEnabled() +
            ", Difficulty: " + difficulty);
    }
    
    public void setGameMode(GameMode gameMode) {
        if (this.gameMode != null) {
            this.gameMode.cleanup();
        }
        this.gameMode = gameMode;
        this.gameMode.initialize(this.gameState);
    }
    
    /**
     * 난이도 설정 ✨ Phase 4
     * 
     * @param difficulty 새로운 난이도
     */
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
        // TetrominoGenerator 재생성
        this.tetrominoGenerator = new TetrominoGenerator(new RandomGenerator(), difficulty);
        System.out.println("🎮 Difficulty changed to: " + difficulty);
    }

    /**
     * ✨ 게임 실행 전략 설정
     *
     * GameController가 게임 모드에 따라 호출:
     * - 싱글플레이: LocalExecutionStrategy
     * - 멀티플레이: NetworkExecutionStrategy
     *
     * @param strategy 실행 전략
     */
    public void setExecutionStrategy(GameExecutionStrategy strategy) {
        this.executionStrategy = strategy;
        System.out.println("✅ [BoardController] ExecutionStrategy set: " +
            (strategy != null ? strategy.getClass().getSimpleName() : "null"));
    }

    public GameModeConfig getConfig() {
        return gameMode != null ? gameMode.getConfig() : GameModeConfig.classic();
    }
    
    /**
     * ✨ 게임 명령 실행 (Strategy Pattern 적용)
     *
     * executionStrategy가 설정되어 있으면 Strategy를 통해 실행:
     * - LocalExecutionStrategy: GameEngine 직접 호출
     * - NetworkExecutionStrategy: MultiPlayStrategies를 통한 네트워크 전송
     *
     * Strategy가 null이면 IllegalStateException 발생 (Fail-fast)
     *
     * @param command 실행할 게임 명령
     * @return 새로운 게임 상태
     */
    public GameState executeCommand(GameCommand command) {
        if (gameState.isGameOver()) {
            return gameState;
        }
        if (gameState.isPaused() &&
            command.getType() != seoultech.se.core.command.CommandType.RESUME &&
            command.getType() != seoultech.se.core.command.CommandType.PAUSE) {
            return gameState;
        }

        // ✨ Strategy가 설정되지 않았으면 설계 오류 (Fail-fast)
        if (executionStrategy == null) {
            throw new IllegalStateException(
                "GameExecutionStrategy not initialized! " +
                "Call setExecutionStrategy() before executing commands."
            );
        }

        // ✨ Strategy를 통해 명령 실행 (로컬/네트워크 투명)
        GameState newState = executionStrategy.execute(command, gameState);

        // 상태 업데이트
        if (newState != null && newState != gameState) {
            // ✨ Lock 발생 여부 확인: 
            // - lastLinesCleared 카운터가 증가했거나 (라인 클리어 발생)
            // - lastLockedPivotY가 설정되었다면 (블록이 lock됨)
            boolean lockOccurred = (newState.getLastLinesCleared() > gameState.getLastLinesCleared()) ||
                                   (newState.getLastLockedPivotY() >= 0 && gameState.getLastLockedPivotY() < 0);
            
            this.gameState = newState;
            
            // Lock 후 새 블록 생성 필요
            if (lockOccurred && !newState.isGameOver()) {
                // 새 블록 생성
                spawnNewTetromino(this.gameState);
                updateNextQueue(this.gameState);
            }
        }

        return this.gameState;
    }

    // ========== 블록 생성 및 관리 ==========

    private void spawnNewTetromino(GameState state) {
        TetrominoType nextType;
        seoultech.se.core.engine.item.ItemType nextItemType = state.getNextBlockItemType();
        
        // 🎁 아이템이 예약되어 있으면 아이템 테트로미노 생성
        if (nextItemType != null) {
            System.out.println("🎁 [BoardController] Spawning item tetromino: " + nextItemType);
            
            if (nextItemType == seoultech.se.core.engine.item.ItemType.WEIGHT_BOMB) {
                // 무게추는 특수 테트로미노 형태 (OO / OOOO)
                // ✅ FIXED: Lock 시 아이템 효과 적용을 위해 currentItemType 유지
                nextType = TetrominoType.WEIGHT_BOMB;
                state.setCurrentItemType(nextItemType); // 아이템 타입 유지하여 효과 적용 가능하도록
            } else if (nextItemType == seoultech.se.core.engine.item.ItemType.LINE_CLEAR) {
                // LINE_CLEAR 아이템은 일반 테트로미노지만 pivot 블록에 'L' 마커
                nextType = getNextTetrominoType();
                state.setCurrentItemType(nextItemType);
            } else {
                // 기타 아이템 (BOMB, PLUS 등) - 일반 테트로미노에 아이템 효과
                nextType = getNextTetrominoType();
                state.setCurrentItemType(nextItemType);
            }
            
            // 아이템 사용 완료, 초기화
            state.setNextBlockItemType(null);
        } else {
            // 일반 테트로미노 생성 - currentItemType 초기화 필수!
            nextType = getNextTetrominoType();
            state.setCurrentItemType(null);
        }
        
        Tetromino newTetromino = new Tetromino(nextType);
        state.setCurrentTetromino(newTetromino);
        state.setCurrentX(state.getBoardWidth() / 2 - 1);
        state.setCurrentY(0);
        
        System.out.println("🎮 [BoardController] Spawned tetromino: " + nextType + 
            (state.getCurrentItemType() != null ? " with item: " + state.getCurrentItemType() : ""));
    }

    private TetrominoType getNextTetrominoType() {
        // ✨ Phase 4: TetrominoGenerator 사용
        return tetrominoGenerator.next();
    }

    private void initializeNextQueue() {
        // ✨ Phase 4: TetrominoGenerator가 자동으로 관리
        updateNextQueue(gameState);
        spawnNewTetromino(gameState);
    }

    private void updateNextQueue(GameState state) {
        // ✨ Phase 4: TetrominoGenerator.preview() 사용
        List<TetrominoType> preview = tetrominoGenerator.preview(6);
        TetrominoType[] queue = new TetrominoType[6];
        
        for (int i = 0; i < 6; i++) {
            queue[i] = preview.get(i);
        }
        
        state.setNextQueue(queue);
    }
    
    public void resetGame() {
        if (gameMode != null) {
            gameMode.cleanup();
        }
        this.gameState = new GameState(10, 20);
        this.gameStartTime = System.currentTimeMillis();
        
        // ✨ Phase 4: TetrominoGenerator 재생성
        this.tetrominoGenerator = new TetrominoGenerator(new RandomGenerator(), difficulty);
        
        initializeNextQueue();
        if (gameMode != null) {
            gameMode.initialize(gameState);
        }
    }
    
    public void cleanup() {
        if (gameMode != null) {
            gameMode.cleanup();
        }
    }
}
