package seoultech.se.client.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

import lombok.Getter;
import seoultech.se.client.mode.SingleMode;
import seoultech.se.core.GameEngine;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.command.MoveCommand;
import seoultech.se.core.command.RotateCommand;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.mode.GameMode;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.TetrominoType;

@Getter
@Component
public class BoardController {
    private GameState gameState;
    private final Random random = new Random();
    private GameMode gameMode;
    private GameEngine gameEngine;  // 게임 엔진 추가
    private List<TetrominoType> currentBag = new ArrayList<>();
    private List<TetrominoType> nextBag = new ArrayList<>();
    private int bagIndex = 0;
    private long gameStartTime;

    /**
     * 기본 생성자 (Classic 모드)
     */
    public BoardController() {
        this(GameModeConfig.classic());
    }
    
    /**
     * GameModeConfig를 받는 생성자
     * 
     * @param config 게임 모드 설정
     */
    public BoardController(GameModeConfig config) {
        this.gameState = new GameState(10, 20);
        this.gameStartTime = System.currentTimeMillis();
        
        // GameEngine 생성 및 초기화
        this.gameEngine = new GameEngine();
        this.gameEngine.initialize(config);
        
        // GameModeConfig에 따라 SingleMode 생성
        this.gameMode = new SingleMode(config);
        this.gameMode.initialize(this.gameState);
        
        initializeNextQueue();
        
        System.out.println("📦 BoardController created with config: " + 
            (config.getGameplayType() != null ? config.getGameplayType() : "CLASSIC") +
            ", SRS: " + config.isSrsEnabled());
    }
    
    public void setGameMode(GameMode gameMode) {
        if (this.gameMode != null) {
            this.gameMode.cleanup();
        }
        this.gameMode = gameMode;
        this.gameMode.initialize(this.gameState);
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
                newState = GameEngine.tryMoveLeft(gameState);
                break;
            case RIGHT:
                newState = GameEngine.tryMoveRight(gameState);
                break;
            case DOWN:
                newState = GameEngine.tryMoveDown(gameState, command.isSoftDrop());
                if (newState == gameState) {
                    newState = lockAndSpawnNext();
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
        return GameEngine.tryRotate(gameState, command.getDirection(), srsEnabled);
    }

    private GameState handleHardDropCommand() {
        if (!getConfig().isHardDropEnabled()) {
            return gameState;
        }
        GameState newState = GameEngine.hardDrop(gameState);
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
        GameState newState = GameEngine.tryHold(gameState);
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
        
        GameState newState = GameEngine.lockTetromino(gameState);
        
        // Lock 후 아이템 효과 적용
        if (itemType != null && gameEngine != null && actualRow >= 0 && actualCol >= 0) {
            // 저장한 위치 사용
            seoultech.se.core.item.Item item = gameEngine.getItemManager() != null 
                ? gameEngine.getItemManager().getItem(itemType) 
                : null;
            
            if (item != null) {
                System.out.println("🔥 [BoardController] Applying item effect: " + itemType + 
                    " at position (" + actualRow + ", " + actualCol + ")");
                seoultech.se.core.item.ItemEffect effect = item.apply(newState, actualRow, actualCol);
                
                if (effect.isSuccess()) {
                    // 점수 추가
                    newState.setScore(newState.getScore() + effect.getBonusScore());
                    
                    System.out.println("🎯 [BoardController] Item effect applied: " + itemType + 
                        " - Blocks cleared: " + effect.getBlocksCleared() + 
                        ", Bonus: " + effect.getBonusScore());
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
        if (currentBag.isEmpty() || bagIndex >= currentBag.size()) {
            currentBag = nextBag;
            nextBag = createAndShuffleBag();
            bagIndex = 0;
        }
        TetrominoType nextType = currentBag.get(bagIndex);
        bagIndex++;
        return nextType;
    }

    private List<TetrominoType> createAndShuffleBag() {
        List<TetrominoType> bag = new ArrayList<>();
        // ITEM 타입은 제외 (7-bag 시스템에 포함하지 않음)
        TetrominoType[] normalTypes = {
            TetrominoType.I, 
            TetrominoType.J, 
            TetrominoType.L, 
            TetrominoType.O, 
            TetrominoType.S, 
            TetrominoType.T, 
            TetrominoType.Z
        };
        
        for (TetrominoType type : normalTypes) {
            bag.add(type);
        }
        
        // 셔플
        for (int i = bag.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            TetrominoType temp = bag.get(i);
            bag.set(i, bag.get(j));
            bag.set(j, temp);
        }
        return bag;
    }

    private void refillBag() {
        currentBag = createAndShuffleBag();
        nextBag = createAndShuffleBag();
        bagIndex = 0;
    }

    private void initializeNextQueue() {
        refillBag();
        updateNextQueue(gameState);
        spawnNewTetromino(gameState);
    }

    private void updateNextQueue(GameState state) {
        TetrominoType[] queue = new TetrominoType[6];
        
        for (int i = 0; i < 6; i++) {
            int index = bagIndex + i;
            
            if (index < currentBag.size()) {
                queue[i] = currentBag.get(index);
            } else {
                int nextBagIndex = index - currentBag.size();
                
                // ✅ nextBag 검증 추가
                if (nextBag == null || nextBag.isEmpty()) {
                    System.err.println("⚠️ [BoardController] nextBag is not initialized! Refilling bags...");
                    refillBag();
                }
                
                if (nextBagIndex < nextBag.size()) {
                    queue[i] = nextBag.get(nextBagIndex);
                } else {
                    // ✅ 범위 초과 시 기본값 (fallback)
                    System.err.println("⚠️ [BoardController] nextBag index out of bounds (" + 
                        nextBagIndex + " >= " + nextBag.size() + "). Using I block as fallback.");
                    queue[i] = TetrominoType.I;
                }
            }
        }
        
        state.setNextQueue(queue);
    }
    
    public void resetGame() {
        if (gameMode != null) {
            gameMode.cleanup();
        }
        this.gameState = new GameState(10, 20);
        this.gameStartTime = System.currentTimeMillis();
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
