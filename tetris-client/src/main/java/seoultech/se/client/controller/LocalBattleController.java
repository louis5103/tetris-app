package seoultech.se.client.controller;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.Rectangle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import seoultech.se.client.localgame.LocalGameSession;
import seoultech.se.client.localgame.LocalGameStatus;
import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.Color;
import seoultech.se.core.model.enumType.RotationDirection;
import seoultech.se.core.model.enumType.TetrominoType;

import java.util.EnumMap;
import java.util.Map;

@Component
public class LocalBattleController {

    // FXML Injected fields
    @FXML private BorderPane rootPane;
    @FXML private GridPane p1HoldGridPane, p1BoardGridPane, p1NextGridPane;
    @FXML private Label p1ScoreLabel, p1LinesLabel, p1GameOverLabel;
    @FXML private GridPane p2HoldGridPane, p2BoardGridPane, p2NextGridPane;
    @FXML private Label p2ScoreLabel, p2LinesLabel, p2GameOverLabel;

    @Autowired
    private GameEngine gameEngine;

    private LocalGameSession localGameSession;
    private AnimationTimer gameLoop;

    private Rectangle[][] p1Cells;
    private Rectangle[][] p2Cells;

    private long lastGravityUpdateTime = 0;
    private static final long GRAVITY_UPDATE_INTERVAL_MS = 500; // 0.5초마다 중력 적용

    private final Map<Color, javafx.scene.paint.Color> colorMap = new EnumMap<>(Color.class);

    public void initialize() {
        System.out.println("✅ LocalBattleController initialized.");
        initializeColorMap();
        p1Cells = initializeBoard(p1BoardGridPane, 10, 20);
        p2Cells = initializeBoard(p2BoardGridPane, 10, 20);

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
            }
        });
    }

    private Rectangle[][] initializeBoard(GridPane gridPane, int width, int height) {
        gridPane.getChildren().clear();
        Rectangle[][] cells = new Rectangle[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Rectangle cell = new Rectangle(20, 20, colorMap.get(Color.NONE));
                cell.setStroke(javafx.scene.paint.Color.rgb(120, 120, 120, 0.5));
                cells[y][x] = cell;
                gridPane.add(cell, x, y);
            }
        }
        return cells;
    }

    private void initializeColorMap() {
        colorMap.put(Color.CYAN, javafx.scene.paint.Color.CYAN);
        colorMap.put(Color.YELLOW, javafx.scene.paint.Color.YELLOW);
        colorMap.put(Color.MAGENTA, javafx.scene.paint.Color.PURPLE);
        colorMap.put(Color.BLUE, javafx.scene.paint.Color.BLUE);
        colorMap.put(Color.ORANGE, javafx.scene.paint.Color.ORANGE);
        colorMap.put(Color.GREEN, javafx.scene.paint.Color.GREEN);
        colorMap.put(Color.RED, javafx.scene.paint.Color.RED);
        colorMap.put(Color.GRAY, javafx.scene.paint.Color.GRAY);
        colorMap.put(Color.NONE, javafx.scene.paint.Color.TRANSPARENT);
    }


    public void initGame(GameModeConfig config) {
        System.out.println("🎮 Initializing Local Battle game with mode: " + config.getGameplayType());
        this.localGameSession = new LocalGameSession(gameEngine, config);
        localGameSession.addPlayer("P1");
        localGameSession.addPlayer("P2");

        // 초기 UI 그리기
        updateUI(new LocalGameStatus(localGameSession.getStateForPlayer("P1"), localGameSession.getStateForPlayer("P2")));
    }

    public void startGame() {
        System.out.println("🚀 Starting Local Battle game.");
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long nowMs = now / 1_000_000;
                if (nowMs - lastGravityUpdateTime > GRAVITY_UPDATE_INTERVAL_MS) {
                    onGameLoopTick();
                    lastGravityUpdateTime = nowMs;
                }
            }
        };
        gameLoop.start();
        rootPane.requestFocus();
    }
    
    private void onGameLoopTick() {
        if (localGameSession == null) return;
        LocalGameStatus status;
        status = localGameSession.applyGravity("P1");
        status = localGameSession.applyGravity("P2");
        updateUI(status);
    }
    
    private void handleKeyEvent(KeyEvent event) {
        if (localGameSession == null) return;

        LocalGameStatus status = null;
        seoultech.se.core.command.GameCommand command = null;
        KeyCode code = event.getCode();

        // Player 1 Controls (WASD + C/Space)
        if (code == KeyCode.W) {
            command = new seoultech.se.core.command.RotateCommand(RotationDirection.CLOCKWISE);
            status = localGameSession.processCommand("P1", command);
        } else if (code == KeyCode.A) {
            command = new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.LEFT);
            status = localGameSession.processCommand("P1", command);
        } else if (code == KeyCode.S) {
            command = new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.DOWN);
            status = localGameSession.processCommand("P1", command);
        } else if (code == KeyCode.D) {
            command = new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.RIGHT);
            status = localGameSession.processCommand("P1", command);
        } else if (code == KeyCode.SPACE) {
            command = new seoultech.se.core.command.HardDropCommand();
            status = localGameSession.processCommand("P1", command);
        } else if (code == KeyCode.C) {
            command = new seoultech.se.core.command.HoldCommand();
            status = localGameSession.processCommand("P1", command);
        }

        // Player 2 Controls (Arrow Keys + Period/Slash)
        else if (code == KeyCode.UP) {
            command = new seoultech.se.core.command.RotateCommand(RotationDirection.CLOCKWISE);
            status = localGameSession.processCommand("P2", command);
        } else if (code == KeyCode.LEFT) {
            command = new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.LEFT);
            status = localGameSession.processCommand("P2", command);
        } else if (code == KeyCode.DOWN) {
            command = new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.DOWN);
            status = localGameSession.processCommand("P2", command);
        } else if (code == KeyCode.RIGHT) {
            command = new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.RIGHT);
            status = localGameSession.processCommand("P2", command);
        } else if (code == KeyCode.PERIOD) {
            command = new seoultech.se.core.command.HardDropCommand();
            status = localGameSession.processCommand("P2", command);
        } else if (code == KeyCode.SLASH) {
            command = new seoultech.se.core.command.HoldCommand();
            status = localGameSession.processCommand("P2", command);
        }

        if (status != null) {
            updateUI(status);
            event.consume();
        }
    }

    private void updateUI(LocalGameStatus status) {
        // Player 1
        GameState p1State = status.getPlayer1State();
        if(p1State != null) {
            drawBoard(p1Cells, p1State);
            drawPreview(p1HoldGridPane, p1State.getHeldPiece());
            if (p1State.getNextQueue() != null && p1State.getNextQueue().length > 0) {
                drawPreview(p1NextGridPane, p1State.getNextQueue()[0]);
            }
            p1ScoreLabel.setText(String.valueOf(p1State.getScore()));
            p1LinesLabel.setText(String.valueOf(p1State.getLinesCleared()));
            p1GameOverLabel.setVisible(p1State.isGameOver());
        }

        // Player 2
        GameState p2State = status.getPlayer2State();
        if(p2State != null) {
            drawBoard(p2Cells, p2State);
            drawPreview(p2HoldGridPane, p2State.getHeldPiece());
            if (p2State.getNextQueue() != null && p2State.getNextQueue().length > 0) {
                drawPreview(p2NextGridPane, p2State.getNextQueue()[0]);
            }
            p2ScoreLabel.setText(String.valueOf(p2State.getScore()));
            p2LinesLabel.setText(String.valueOf(p2State.getLinesCleared()));
            p2GameOverLabel.setVisible(p2State.isGameOver());
        }
        
        if (p1State != null && p2State != null && p1State.isGameOver() && p2State.isGameOver() && gameLoop != null) {
            gameLoop.stop();
        }
    }

    private void drawBoard(Rectangle[][] cells, GameState state) {
        int boardHeight = state.getBoardHeight();
        int boardWidth = state.getBoardWidth();
        
        // 1. Create a temporary grid of colors from the locked pieces
        Color[][] finalBoard = new Color[boardHeight][boardWidth];
        Cell[][] grid = state.getGrid();
        for (int y = 0; y < boardHeight; y++) {
            for (int x = 0; x < boardWidth; x++) {
                finalBoard[y][x] = grid[y][x] != null ? grid[y][x].getColor() : Color.NONE;
            }
        }

        // 2. Stamp the current moving piece onto the temporary grid using pivot logic
        Tetromino piece = state.getCurrentTetromino();
        if (piece != null) {
            int[][] shape = piece.getCurrentShape();
            int pieceX = state.getCurrentX();
            int pieceY = state.getCurrentY();
            int pivotX = piece.getPivotX();
            int pivotY = piece.getPivotY();

            for (int y = 0; y < shape.length; y++) {
                for (int x = 0; x < shape[y].length; x++) {
                    if (shape[y][x] == 1) {
                        int boardX = pieceX + (x - pivotX);
                        int boardY = pieceY + (y - pivotY);
                        if (boardY >= 0 && boardY < boardHeight && boardX >= 0 && boardX < boardWidth) {
                            finalBoard[boardY][boardX] = piece.getColor();
                        }
                    }
                }
            }
        }

        // 3. Update the UI from the final grid
        for (int y = 0; y < boardHeight; y++) {
            for (int x = 0; x < boardWidth; x++) {
                cells[y][x].setFill(colorMap.get(finalBoard[y][x]));
            }
        }
    }

    private void drawPreview(GridPane gridPane, TetrominoType type) {
        gridPane.getChildren().clear();
        if (type == null) return;

        Tetromino tetromino = new Tetromino(type);
        javafx.scene.paint.Color color = colorMap.get(tetromino.getColor());
        int[][] shape = tetromino.getCurrentShape();
        
        for (int y = 0; y < shape.length; y++) {
            for (int x = 0; x < shape[y].length; x++) {
                if (shape[y][x] == 1) {
                    Rectangle cell = new Rectangle(15, 15, color);
                    gridPane.add(cell, x, y);
                }
            }
        }
    }
}
