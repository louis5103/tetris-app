package seoultech.se.client.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

import lombok.Getter;
import seoultech.se.client.mode.SingleMode;
import seoultech.se.client.strategy.GameExecutionStrategy;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.command.MoveCommand;
import seoultech.se.core.command.RotateCommand;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.GameEngine;
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
    private final Random random = new Random();
    private GameMode gameMode;
    private GameEngine gameEngine;  // 게임 엔진 추가

    // ✨ Strategy Pattern: 로컬/네트워크 실행 전략
    private GameExecutionStrategy executionStrategy;

    // ✨ Phase 4: 난이도 시스템 통합
    private Difficulty difficulty;  // 현재 난이도
    private RandomGenerator randomGenerator;  // 시드 기반 난수 생성기
    private TetrominoGenerator tetrominoGenerator;  // 7-bag 생성기

    private List<TetrominoType> currentBag = new ArrayList<>();
    private List<TetrominoType> nextBag = new ArrayList<>();
    private int bagIndex = 0;
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
        
        // ✨ Phase 4: RandomGenerator와 TetrominoGenerator 초기화
        this.randomGenerator = new RandomGenerator();
        this.tetrominoGenerator = new TetrominoGenerator(randomGenerator, difficulty);
        
        // ✨ Phase 5: GameEngineFactory를 사용하여 적절한 GameEngine 생성
        // Stateless 리팩토링: 생성자에서 이미 config를 주입하므로 initialize() 호출 불필요
        seoultech.se.core.engine.factory.GameEngineFactory factory = new seoultech.se.core.engine.factory.GameEngineFactory();
        this.gameEngine = factory.createGameEngine(config);
        
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
        this.tetrominoGenerator = new TetrominoGenerator(randomGenerator, difficulty);
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
            this.gameState = newState;
        }

        return this.gameState;
    }

    private GameState handleMoveCommand(MoveCommand command) {
        GameState newState;
        switch (command.getDirection()) {
            case LEFT:
                newState = gameEngine.tryMoveLeft(gameState);
                break;
            case RIGHT:
                newState = gameEngine.tryMoveRight(gameState);
                break;
            case DOWN:
                newState = gameEngine.tryMoveDown(gameState, command.isSoftDrop());
                if (newState == gameState) {
                    System.out.println("⬇️ [BoardController] DOWN failed - calling lockAndSpawnNext()");
                    newState = lockAndSpawnNext();
                } else {
                    // System.out.println("⬇️ [BoardController] DOWN succeeded - block moved");
                }
                break;
            default:
                return gameState;
        }
        return newState;
    }

    private GameState handleRotateCommand(RotateCommand command) {
        // GameModeConfig에서 SRS 설정 가져오기
        boolean srsEnabled = getConfig().isSrsEnabled();
        
        // GameEngine에 SRS 설정 전달
        return gameEngine.tryRotate(gameState, command.getDirection(), srsEnabled);
    }

    private GameState handleHardDropCommand() {
        if (!getConfig().isHardDropEnabled()) {
            return gameState;
        }
        
        // Lock 전에 아이템 타입 기록
        seoultech.se.core.engine.item.ItemType itemType = gameState.getCurrentItemType();
        
        System.out.println("🎯 [BoardController] HARD DROP - Item type BEFORE hardDrop(): " + itemType);
        
        // Hard Drop 실행
        GameState newState = gameEngine.hardDrop(gameState);
        
        // 🔥 CRITICAL FIX: Hard Drop 후 Lock된 블록의 Pivot 위치 사용 (아이템 효과 중심점)
        // BOMB/PLUS 등의 아이템은 pivot 중심으로 효과 발동
        int actualRow = newState.getLastLockedPivotY();
        int actualCol = newState.getLastLockedPivotX();
        
        System.out.println("🎯 [BoardController] HARD DROP - Item type AFTER hardDrop(): " + 
            newState.getCurrentItemType());
        System.out.println("🎯 [BoardController] HARD DROP - Locked pivot position from GameState: (" + 
            actualRow + ", " + actualCol + ")");
        
        // ✨ Phase 4: 난이도별 점수 배율 적용
        // GameEngine에서 계산된 점수에 난이도 배율을 곱함
        long originalScore = gameState.getScore();
        long newScore = newState.getScore();
        long scoreGained = newScore - originalScore;
        
        if (scoreGained > 0) {
            double scoreMultiplier = difficulty.getScoreMultiplier();
            long adjustedScoreGained = (long) (scoreGained * scoreMultiplier);
            newState.setScore(originalScore + adjustedScoreGained);
            
            System.out.println("💰 [BoardController] HARD DROP - Score adjustment: " + 
                scoreGained + " × " + scoreMultiplier + " = " + adjustedScoreGained);
        }
        
        // 🔥 CRITICAL: 통합된 아이템 효과 적용 (모든 아이템, 모든 경로)
        applyItemEffectAfterLock(newState, itemType, "HARD DROP");
        
        if (!newState.isGameOver()) {
            spawnNewTetromino(newState);
            updateNextQueue(newState);
        }
        return newState;
    }

    private GameState handleHoldCommand() {
        if (!getConfig().isHoldEnabled()) {
            return gameState;
        }
        GameState newState = gameEngine.tryHold(gameState);
        if (newState != gameState) {
            updateNextQueue(newState);
        }
        return newState;
    }

    private GameState handlePauseCommand() {
        if (!gameState.isPaused()) {
            GameState newState = gameState.deepCopy();
            newState.setPaused(true);
            return newState;
        }
        return gameState;
    }

    private GameState handleResumeCommand() {
        if (gameState.isPaused()) {
            GameState newState = gameState.deepCopy();
            newState.setPaused(false);
            return newState;
        }
        return gameState;
    }

    private GameState lockAndSpawnNext() {
        // Lock 전에 아이템 타입 기록
        seoultech.se.core.engine.item.ItemType itemType = gameState.getCurrentItemType();
        
        System.out.println("🎮 [BoardController] Calling lockTetromino on: " + gameEngine.getClass().getSimpleName());
        GameState newState = gameEngine.lockTetromino(gameState);
        
        // 🔥 CRITICAL FIX: Hard Drop과 동일하게 Lock 후 Pivot 위치 사용 (아이템 효과 중심점)
        // BOMB/PLUS 등의 아이템은 pivot 중심으로 효과 발동
        int actualRow = newState.getLastLockedPivotY();
        int actualCol = newState.getLastLockedPivotX();
        
        System.out.println("🎯 [BoardController] Locked pivot position from GameState: (" + 
            actualRow + ", " + actualCol + ")");
        
        // ✨ Phase 4: 난이도별 점수 배율 적용
        // GameEngine에서 계산된 점수에 난이도 배율을 곱함
        long originalScore = gameState.getScore();
        long newScore = newState.getScore();
        long scoreGained = newScore - originalScore;
        
        if (scoreGained > 0) {
            double scoreMultiplier = difficulty.getScoreMultiplier();
            long adjustedScoreGained = (long) (scoreGained * scoreMultiplier);
            newState.setScore(originalScore + adjustedScoreGained);
            
            System.out.println("💰 [BoardController] Score adjustment: " + 
                scoreGained + " × " + scoreMultiplier + " = " + adjustedScoreGained);
        }
        
        // 🔥 CRITICAL: 통합된 아이템 효과 적용 (모든 아이템, 모든 경로)
        applyItemEffectAfterLock(newState, itemType, "AUTO LOCK");
        
        if (!newState.isGameOver()) {
            spawnNewTetromino(newState);
            updateNextQueue(newState);
        }
        return newState;
    }

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
        this.randomGenerator = new RandomGenerator();
        this.tetrominoGenerator = new TetrominoGenerator(randomGenerator, difficulty);
        
        this.currentBag.clear();
        this.nextBag.clear();
        this.bagIndex = 0;
        initializeNextQueue();
        if (gameMode != null) {
            gameMode.initialize(gameState);
            // Stateless 리팩토링: GameEngine은 불변이므로 재초기화 불필요
            // GameEngine은 이미 생성자에서 config로 초기화됨
        }
    }
    
    public void cleanup() {
        if (gameMode != null) {
            gameMode.cleanup();
        }
    }
    
    /**
     * 🔥 CRITICAL: Lock 후 아이템 효과를 적용하는 공통 메서드
     * 
     * 모든 Lock 경로 (Hard Drop, Soft Drop, Auto Lock)에서 호출됩니다.
     * 모든 아이템 타입에 대해 동일한 로직을 적용합니다.
     * 
     * @param newState Lock 후의 GameState
     * @param itemType Lock 전에 기록한 아이템 타입
     * @param lockSource Lock 발생 지점 (디버깅용)
     */
    private void applyItemEffectAfterLock(GameState newState, seoultech.se.core.engine.item.ItemType itemType, String lockSource) {
        // 아이템이 없으면 스킵
        if (itemType == null) {
            return;
        }
        
        // Arcade 모드가 아니면 스킵
        if (!(gameEngine instanceof seoultech.se.core.engine.ArcadeGameEngine)) {
            System.out.println("ℹ️ [BoardController] " + lockSource + " - Item ignored - not in Arcade mode: " + itemType);
            return;
        }
        
        seoultech.se.core.engine.ArcadeGameEngine arcadeEngine = 
            (seoultech.se.core.engine.ArcadeGameEngine) gameEngine;
        
        // ItemManager null 체크
        if (arcadeEngine.getItemManager() == null) {
            System.err.println("⚠️ [BoardController] " + lockSource + " - ItemManager is null in ArcadeGameEngine");
            return;
        }
        
        // 🔥 CRITICAL: Lock된 pivot 위치 사용 (아이템 효과 중심점)
        int actualRow = newState.getLastLockedPivotY();
        int actualCol = newState.getLastLockedPivotX();
        
        System.out.println("🎯 [BoardController] " + lockSource + " - Locked pivot position: (" + 
            actualRow + ", " + actualCol + "), Item: " + itemType);
        
        // 위치 유효성 검사
        if (actualRow < 0 || actualCol < 0) {
            System.err.println("⚠️ [BoardController] " + lockSource + " - Invalid pivot position: (" + 
                actualRow + ", " + actualCol + ")");
            return;
        }
        
        seoultech.se.core.engine.item.Item item = arcadeEngine.getItemManager().getItem(itemType);
        
        if (item == null) {
            System.err.println("⚠️ [BoardController] " + lockSource + " - Item not found in ItemManager: " + itemType);
            return;
        }
        
        // 🔥 LINE_CLEAR는 ArcadeGameEngine에서 자동 처리되므로 여기서 apply() 호출 안 함
        if (itemType == seoultech.se.core.engine.item.ItemType.LINE_CLEAR) {
            System.out.println("ℹ️ [BoardController] " + lockSource + " - LINE_CLEAR handled by ArcadeGameEngine");
            return;
        }
        
        // 아이템 효과 적용
        System.out.println("🔥 [BoardController] " + lockSource + " - Applying item effect: " + itemType + 
            " at position (" + actualRow + ", " + actualCol + ")");
        
        seoultech.se.core.engine.item.ItemEffect effect = item.apply(newState, actualRow, actualCol);
        
        if (effect.isSuccess()) {
            // ✨ Phase 4: 아이템 점수에도 난이도 배율 적용
            long itemScore = effect.getBonusScore();
            long adjustedItemScore = (long) (itemScore * difficulty.getScoreMultiplier());
            newState.setScore(newState.getScore() + adjustedItemScore);
            
            System.out.println("✅ [BoardController] " + lockSource + " - Item effect applied: " + itemType + 
                " - Blocks cleared: " + effect.getBlocksCleared() + 
                ", Bonus: " + itemScore + " × " + difficulty.getScoreMultiplier() + 
                " = " + adjustedItemScore);
        } else {
            System.out.println("⚠️ [BoardController] " + lockSource + " - Item effect failed: " + itemType);
        }
    }
}
