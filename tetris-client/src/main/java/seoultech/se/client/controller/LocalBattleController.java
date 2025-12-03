package seoultech.se.client.controller;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import seoultech.se.client.localgame.LocalGameSession;
import seoultech.se.client.localgame.LocalGameStatus;
import seoultech.se.client.service.NavigationService;
import seoultech.se.client.service.SettingsService;
import seoultech.se.client.ui.BoardRenderer;
import seoultech.se.client.util.ColorMapper;
import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.model.enumType.RotationDirection;

import java.io.IOException;

@Component
public class LocalBattleController {

    // FXML Injected fields
    @FXML private BorderPane rootPane;
    @FXML private GridPane p1HoldGridPane, p1BoardGridPane, p1NextGridPane;
    @FXML private Label p1ScoreLabel, p1LinesLabel, p1GameOverLabel;
    @FXML private GridPane p2HoldGridPane, p2BoardGridPane, p2NextGridPane;
    @FXML private Label p2ScoreLabel, p2LinesLabel, p2GameOverLabel;
    @FXML private VBox pauseOverlay;


    @Autowired
    private GameEngine gameEngine;

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private SettingsService settingsService;

    private LocalGameSession localGameSession;
    private AnimationTimer gameLoop;

    private Rectangle[][] p1Cells, p1HoldCells, p1NextCells;
    private Rectangle[][] p2Cells, p2HoldCells, p2NextCells;

    private BoardRenderer p1BoardRenderer, p2BoardRenderer;

    private boolean isPaused = true;

    private long lastGravityUpdateTime = 0;
    private static final long GRAVITY_UPDATE_INTERVAL_MS = 500; // 0.5초마다 중력 적용

    public void initialize() {
        System.out.println("✅ LocalBattleController initialized.");
        p1Cells = initializeBoard(p1BoardGridPane, 10, 20, 20);
        p2Cells = initializeBoard(p2BoardGridPane, 10, 20, 20);
        
        p1HoldCells = initializeBoard(p1HoldGridPane, 4, 4, 15);
        p2HoldCells = initializeBoard(p2HoldGridPane, 4, 4, 15);
        
        p1NextCells = initializeBoard(p1NextGridPane, 4, 4, 15);
        p2NextCells = initializeBoard(p2NextGridPane, 4, 4, 15);

        p1BoardRenderer = new BoardRenderer(p1Cells, p1HoldCells, p1NextCells, settingsService.getColorBlindMode());
        p2BoardRenderer = new BoardRenderer(p2Cells, p2HoldCells, p2NextCells, settingsService.getColorBlindMode());

        pauseOverlay.setVisible(false);

        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyEvent);
            }
        });
    }

    private Rectangle[][] initializeBoard(GridPane gridPane, int width, int height, int cellSize) {
        gridPane.getChildren().clear();
        Rectangle[][] cells = new Rectangle[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Rectangle cell = new Rectangle(cellSize, cellSize, ColorMapper.getEmptyCellColor());
                cell.setStroke(ColorMapper.getCellBorderColor());
                cells[y][x] = cell;
                gridPane.add(cell, x, y);
            }
        }
        return cells;
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
        rootPane.requestFocus();
        togglePause();
    }
    
    private void onGameLoopTick() {
        if (localGameSession == null || isPaused) return;
        LocalGameStatus status;
        status = localGameSession.applyGravity("P1");
        status = localGameSession.applyGravity("P2");
        updateUI(status);
    }
    
    private void handleKeyEvent(KeyEvent event) {
        if (localGameSession == null) return;
        KeyCode code = event.getCode();

        if (code == KeyCode.P) {
            togglePause();
            event.consume();
            return;
        }

        if (isPaused) return;

        LocalGameStatus status = null;
        seoultech.se.core.command.GameCommand command = null;

        // Player 1 Controls (WASD + C/Space)
        if (code == KeyCode.W) {
            command = new seoultech.se.core.command.RotateCommand(RotationDirection.CLOCKWISE);
            status = localGameSession.processCommand("P1", command);
        } else if (code == KeyCode.A) {
            command = new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.LEFT);
            status = localGameSession.processCommand("P1", command);
        } else if (code == KeyCode.S) {
            command = new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.DOWN, true);
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
            command = new seoultech.se.core.command.MoveCommand(seoultech.se.core.command.Direction.DOWN, true);
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
        rootPane.requestFocus();
    }

    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            gameLoop.stop();
            pauseOverlay.setVisible(true);
            // Optionally, send PauseCommand to session if state needs to be aware
            localGameSession.processCommand("P1", new seoultech.se.core.command.PauseCommand());
            localGameSession.processCommand("P2", new seoultech.se.core.command.PauseCommand());
        } else {
            gameLoop.start();
            pauseOverlay.setVisible(false);
            rootPane.requestFocus(); // Return focus to the game pane
            // Optionally, send ResumeCommand
            localGameSession.processCommand("P1", new seoultech.se.core.command.ResumeCommand());
            localGameSession.processCommand("P2", new seoultech.se.core.command.ResumeCommand());
        }
    }

    @FXML
    public void handleResume() {
        if (isPaused) {
            togglePause();
        }
    }

    @FXML
    public void handleQuit() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        try {
            navigationService.navigateTo("/view/main-view.fxml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateUI(LocalGameStatus status) {
        // Player 1
        GameState p1State = status.getPlayer1State();
        if(p1State != null) {
            p1BoardRenderer.drawBoard(p1State);
            p1BoardRenderer.drawHoldPiece(p1State.getHeldPiece());
            if (p1State.getNextQueue() != null && p1State.getNextQueue().length > 0) {
                p1BoardRenderer.drawNextPiece(p1State.getNextQueue()[0]);
            }
            p1ScoreLabel.setText(String.valueOf(p1State.getScore()));
            p1LinesLabel.setText(String.valueOf(p1State.getLinesCleared()));
            p1GameOverLabel.setVisible(p1State.isGameOver());
        }

        // Player 2
        GameState p2State = status.getPlayer2State();
        if(p2State != null) {
            p2BoardRenderer.drawBoard(p2State);
            p2BoardRenderer.drawHoldPiece(p2State.getHeldPiece());
            if (p2State.getNextQueue() != null && p2State.getNextQueue().length > 0) {
                p2BoardRenderer.drawNextPiece(p2State.getNextQueue()[0]);
            }
            p2ScoreLabel.setText(String.valueOf(p2State.getScore()));
            p2LinesLabel.setText(String.valueOf(p2State.getLinesCleared()));
            p2GameOverLabel.setVisible(p2State.isGameOver());
        }
        
        if (p1State != null && p2State != null && p1State.isGameOver() && p2State.isGameOver() && gameLoop != null) {
            gameLoop.stop();
        }
    }
}
