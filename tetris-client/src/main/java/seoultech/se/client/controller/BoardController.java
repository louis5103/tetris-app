package seoultech.se.client.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

import lombok.Getter;
import seoultech.se.client.mode.SingleMode;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.command.MoveCommand;
import seoultech.se.core.command.RotateCommand;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.mode.GameMode;
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
        seoultech.se.core.factory.GameEngineFactory factory = new seoultech.se.core.factory.GameEngineFactory();
        this.gameEngine = factory.createGameEngine(config);
        this.gameEngine.initialize(config);
        
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
    
    public GameModeConfig getConfig() {
        return gameMode != null ? gameMode.getConfig() : GameModeConfig.classic();
    }
    
    public GameState executeCommand(GameCommand command) {
        if (gameState.isGameOver()) {
            return gameState;
        }
        if (gameState.isPaused() && 
            command.getType() != seoultech.se.core.command.CommandType.RESUME &&
            command.getType() != seoultech.se.core.command.CommandType.PAUSE) {
            return gameState;
        }
        GameState newState = null;
        switch (command.getType()) {
            case MOVE:
                newState = handleMoveCommand((MoveCommand) command);
                break;
            case ROTATE:
                newState = handleRotateCommand((RotateCommand) command);
                break;
            case HARD_DROP:
                newState = handleHardDropCommand();
                break;
            case HOLD:
                newState = handleHoldCommand();
                break;
            case PAUSE:
                newState = handlePauseCommand();
                break;
            case RESUME:
                newState = handleResumeCommand();
                break;
            default:
                return gameState;
        }
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
                    System.out.println("⬇️ [BoardController] DOWN succeeded - block moved");
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
        
        // Lock 전에 아이템 타입과 위치 확인
        seoultech.se.core.item.ItemType itemType = gameState.getCurrentItemType();
        
        // 실제 블록의 위치 계산 (pivot이 아닌 실제 블록 위치)
        int actualRow = -1;
        int actualCol = -1;
        
        if (itemType != null && gameState.getCurrentTetromino() != null) {
            seoultech.se.core.model.Tetromino tetromino = gameState.getCurrentTetromino();
            int[][] shape = tetromino.getCurrentShape();
            int pivotX = tetromino.getPivotX();
            int pivotY = tetromino.getPivotY();
            int currentX = gameState.getCurrentX();
            int currentY = gameState.getCurrentY();
            
            // 첫 번째 블록의 실제 위치 찾기
            boolean found = false;
            for (int r = 0; r < shape.length && !found; r++) {
                for (int c = 0; c < shape[0].length && !found; c++) {
                    if (shape[r][c] == 1) {
                        actualRow = currentY + (r - pivotY);
                        actualCol = currentX + (c - pivotX);
                        found = true;
                        System.out.println("🎯 [BoardController] HARD DROP - Item block actual position: (" + actualRow + ", " + actualCol + ")");
                        System.out.println("   - Pivot position was: (" + currentY + ", " + currentX + ")");
                    }
                }
            }
        }
        
        GameState newState = gameEngine.hardDrop(gameState);
        
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
        
        // Lock 후 아이템 효과 적용
        if (itemType != null && gameEngine != null && actualRow >= 0 && actualCol >= 0) {
            // 저장한 위치 사용
            seoultech.se.core.item.Item item = (gameEngine instanceof seoultech.se.core.engine.ArcadeGameEngine) 
                ? ((seoultech.se.core.engine.ArcadeGameEngine)gameEngine).getItemManager().getItem(itemType) 
                : null;
            
            if (item != null) {
                System.out.println("🔥 [BoardController] HARD DROP - Applying item effect: " + itemType + 
                    " at position (" + actualRow + ", " + actualCol + ")");
                seoultech.se.core.item.ItemEffect effect = item.apply(newState, actualRow, actualCol);
                
                if (effect.isSuccess()) {
                    // ✨ Phase 4: 아이템 점수에도 난이도 배율 적용
                    long itemScore = effect.getBonusScore();
                    long adjustedItemScore = (long) (itemScore * difficulty.getScoreMultiplier());
                    newState.setScore(newState.getScore() + adjustedItemScore);
                    
                    System.out.println("🎯 [BoardController] HARD DROP - Item effect applied: " + itemType + 
                        " - Blocks cleared: " + effect.getBlocksCleared() + 
                        ", Bonus: " + itemScore + " × " + difficulty.getScoreMultiplier() + 
                        " = " + adjustedItemScore);
                } else {
                    System.out.println("⚠️ [BoardController] HARD DROP - Item effect failed: " + itemType);
                }
            }
        }
        
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
        // Lock 전에 아이템 타입과 위치 확인
        seoultech.se.core.item.ItemType itemType = gameState.getCurrentItemType();
        
        // 실제 블록의 위치 계산 (pivot이 아닌 실제 블록 위치)
        int actualRow = -1;
        int actualCol = -1;
        
        if (itemType != null && gameState.getCurrentTetromino() != null) {
            seoultech.se.core.model.Tetromino tetromino = gameState.getCurrentTetromino();
            int[][] shape = tetromino.getCurrentShape();
            int pivotX = tetromino.getPivotX();
            int pivotY = tetromino.getPivotY();
            int currentX = gameState.getCurrentX();
            int currentY = gameState.getCurrentY();
            
            // 첫 번째 블록의 실제 위치 찾기
            boolean found = false;
            for (int r = 0; r < shape.length && !found; r++) {
                for (int c = 0; c < shape[0].length && !found; c++) {
                    if (shape[r][c] == 1) {
                        actualRow = currentY + (r - pivotY);
                        actualCol = currentX + (c - pivotX);
                        found = true;
                        System.out.println("🎯 [BoardController] Item block actual position: (" + actualRow + ", " + actualCol + ")");
                        System.out.println("   - Pivot position was: (" + currentY + ", " + currentX + ")");
                    }
                }
            }
        }
        
        System.out.println("🎮 [BoardController] Calling lockTetromino on: " + gameEngine.getClass().getSimpleName());
        GameState newState = gameEngine.lockTetromino(gameState);
        
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
        
        // Lock 후 아이템 효과 적용
        if (itemType != null && gameEngine != null && actualRow >= 0 && actualCol >= 0) {
            // 저장한 위치 사용
            seoultech.se.core.item.Item item = (gameEngine instanceof seoultech.se.core.engine.ArcadeGameEngine) 
                ? ((seoultech.se.core.engine.ArcadeGameEngine)gameEngine).getItemManager().getItem(itemType) 
                : null;
            
            if (item != null) {
                System.out.println("🔥 [BoardController] Applying item effect: " + itemType + 
                    " at position (" + actualRow + ", " + actualCol + ")");
                seoultech.se.core.item.ItemEffect effect = item.apply(newState, actualRow, actualCol);
                
                if (effect.isSuccess()) {
                    // ✨ Phase 4: 아이템 점수에도 난이도 배율 적용
                    long itemScore = effect.getBonusScore();
                    long adjustedItemScore = (long) (itemScore * difficulty.getScoreMultiplier());
                    newState.setScore(newState.getScore() + adjustedItemScore);
                    
                    System.out.println("🎯 [BoardController] Item effect applied: " + itemType + 
                        " - Blocks cleared: " + effect.getBlocksCleared() + 
                        ", Bonus: " + itemScore + " × " + difficulty.getScoreMultiplier() + 
                        " = " + adjustedItemScore);
                } else {
                    System.out.println("⚠️ [BoardController] Item effect failed: " + itemType);
                }
            }
        }
        
        if (!newState.isGameOver()) {
            spawnNewTetromino(newState);
            updateNextQueue(newState);
        }
        return newState;
    }

    private void spawnNewTetromino(GameState state) {
        TetrominoType nextType = getNextTetrominoType();
        Tetromino newTetromino = new Tetromino(nextType);
        state.setCurrentTetromino(newTetromino);
        state.setCurrentX(state.getBoardWidth() / 2 - 1);
        state.setCurrentY(0);
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
            // GameEngine도 재초기화
            if (gameEngine != null) {
                gameEngine.initialize(gameMode.getConfig());
            }
        }
    }
    
    public void cleanup() {
        if (gameMode != null) {
            gameMode.cleanup();
        }
    }
}
