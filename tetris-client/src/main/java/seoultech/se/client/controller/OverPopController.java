package seoultech.se.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import seoultech.se.backend.score.GameMode;
import seoultech.se.backend.score.ScoreRankDto;
import seoultech.se.backend.score.ScoreRequestDto;
import seoultech.se.backend.score.ScoreService;
import seoultech.se.client.service.NavigationService;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class OverPopController extends BaseController {

    private final NavigationService navigationService;
    private final ScoreService scoreService;

    @FXML
    private Label scoreLabel;

    @FXML
    private HBox nameInputBox;

    @FXML
    private TextField usernameInput;

    @FXML
    private TableView<Map<String, Object>> scoreBoardTable;

    @FXML
    private Button mainButton;

    @FXML
    private Button restartButton;

    @FXML
    private BorderPane rootPane;

    private long currentScore;
    private Button[] buttons;
    private int currentButtonIndex = 0;
    private boolean isItemMode; // Add this to store mode

    @FXML
    public void initialize() {
        nameInputBox.setVisible(false);
        nameInputBox.setManaged(false);

        buttons = new Button[]{mainButton, restartButton};
        setupButtonNavigation();
    }

    private void setupButtonNavigation() {
        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            buttons[i].focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (isNowFocused && currentButtonIndex != index) {
                    currentButtonIndex = index;
                    syncButtonHighlight();
                }
            });
            buttons[i].setOnMouseEntered(event -> {
                if (currentButtonIndex != index) {
                    currentButtonIndex = index;
                    buttons[index].requestFocus();
                    syncButtonHighlight();
                }
            });
        }

        Platform.runLater(() -> {
            if (rootPane.getScene() != null) {
                rootPane.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, this::handleKeyPressed);
            } else {
                rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                    if (newScene != null && oldScene == null) {
                        newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, this::handleKeyPressed);
                    }
                });
            }
        });
        updateButtonHighlight();
    }

    private void handleKeyPressed(javafx.scene.input.KeyEvent event) {
        if (event.getCode().isModifierKey()) return;

        switch (event.getCode()) {
            case LEFT:
            case UP:
                currentButtonIndex = (currentButtonIndex - 1 + buttons.length) % buttons.length;
                updateButtonHighlight();
                event.consume();
                break;
            case RIGHT:
            case DOWN:
                currentButtonIndex = (currentButtonIndex + 1) % buttons.length;
                updateButtonHighlight();
                event.consume();
                break;
            case ENTER:
                buttons[currentButtonIndex].fire();
                event.consume();
                break;
            default:
                break;
        }
    }

    private void updateButtonHighlight() {
        syncButtonHighlight();
        if (currentButtonIndex >= 0 && currentButtonIndex < buttons.length) {
            buttons[currentButtonIndex].requestFocus();
        }
    }

    private void syncButtonHighlight() {
        for (Button button : buttons) {
            button.getStyleClass().remove("highlighted");
        }
        if (currentButtonIndex >= 0 && currentButtonIndex < buttons.length) {
            buttons[currentButtonIndex].getStyleClass().add("highlighted");
        }
    }

    public void setScore(long score) {
        this.currentScore = score;
        // FIXME: This should be passed from the game screen.
        this.isItemMode = false;
        scoreLabel.setText(String.valueOf(currentScore));

        List<ScoreRankDto> scores = scoreService.getTopScores(isItemMode, 10);
        loadScores(scores, isItemMode);

        boolean isTopTen = scores.size() < 10 || scores.stream().anyMatch(s -> currentScore > s.getScore());
        if (isTopTen) {
            Platform.runLater(() -> {
                nameInputBox.setVisible(true);
                nameInputBox.setManaged(true);
                usernameInput.requestFocus();
            });
        }
    }

    private void loadScores(List<ScoreRankDto> scores, boolean isItemMode) {
        Platform.runLater(() -> {
            List<Map<String, Object>> scoreMaps = scores.stream()
                    .map(score -> Map.<String, Object>of(
                            "rank", score.getRank(),
                            "player", score.getName(),
                            "score", score.getScore(),
                            "difficulty", score.getGameMode() + (isItemMode ? " (Item)" : "")
                    ))
                    .collect(Collectors.toList());

            Map<String, Object> currentPlayerScoreMap = Map.of(
                    "rank", "-",
                    "player", "You",
                    "score", (int) currentScore,
                    "difficulty", "NORMAL" + (isItemMode ? " (Item)" : "")
            );

            List<Map<String, Object>> displayList = Stream.concat(scoreMaps.stream(), Stream.of(currentPlayerScoreMap))
                    .sorted(Comparator.comparingInt((Map<String, Object> m) -> (int) m.get("score")).reversed())
                    .collect(Collectors.toList());

            ObservableList<Map<String, Object>> scoreData = FXCollections.observableArrayList(displayList);
            scoreBoardTable.setItems(scoreData);

            scoreBoardTable.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(Map<String, Object> item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().remove("score-highlight");
                    if (item != null && !empty && "You".equals(item.get("player"))) {
                        getStyleClass().add("score-highlight");
                    }
                }
            });
        });
    }

    @FXML
    private void handleNameInput(ActionEvent event) {
        saveScoreAndRefreshUi().thenRun(() -> {
            Platform.runLater(() -> restartButton.requestFocus());
        }).exceptionally(ex -> {
            System.err.println("Failed to save score on name input: " + ex.getMessage());
            return null;
        });
    }

    private CompletableFuture<Void> saveScoreAndRefreshUi() {
        String username = usernameInput.getText();
        if (username == null || username.trim().isEmpty()) {
            System.out.println("Username is required.");
            return CompletableFuture.failedFuture(new IllegalArgumentException("Username is required."));
        }

        ScoreRequestDto newScore = new ScoreRequestDto();
        newScore.setName(username);
        newScore.setScore((int) currentScore);
        newScore.setGameMode(GameMode.NORMAL); // FIXME: Assuming normal mode
        newScore.setItemMode(this.isItemMode);

        return CompletableFuture.runAsync(() -> scoreService.saveScore(newScore))
            .thenRun(() -> {
                Platform.runLater(() -> {
                    nameInputBox.setVisible(false);
                    nameInputBox.setManaged(false);
                    List<ScoreRankDto> updatedScores = scoreService.getTopScores(this.isItemMode, 10);
                    loadScores(updatedScores, this.isItemMode);
                });
            });
    }

    @FXML
    private void handleMainButton(ActionEvent event) {
        Runnable navigate = () -> {
            try {
                closePopup(event);
                navigationService.navigateTo("/view/main-view.fxml");
            } catch (Exception ex) {
                System.err.println("❌ Failed to navigate to main view: " + ex.getMessage());
                ex.printStackTrace();
            }
        };

        if (nameInputBox.isVisible()) {
            saveScoreAndRefreshUi().thenRun(() -> {
                Platform.runLater(navigate);
            }).exceptionally(ex -> {
                System.err.println("Failed to save score before navigation: " + ex.getMessage());
                Platform.runLater(navigate);
                return null;
            });
        } else {
            navigate.run();
        }
    }

    @FXML
    private void handleRestartButton(ActionEvent event) {
        Runnable navigate = () -> {
            try {
                closePopup(event);
                navigationService.navigateTo("/view/game-view.fxml");
            } catch (Exception ex) {
                System.err.println("❌ Failed to restart game: " + ex.getMessage());
                ex.printStackTrace();
            }
        };

        if (nameInputBox.isVisible()) {
            saveScoreAndRefreshUi().thenRun(() -> {
                Platform.runLater(navigate);
            }).exceptionally(ex -> {
                System.err.println("Failed to save score before navigation: " + ex.getMessage());
                Platform.runLater(navigate);
                return null;
            });
        } else {
            navigate.run();
        }
    }

    private void closePopup(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
