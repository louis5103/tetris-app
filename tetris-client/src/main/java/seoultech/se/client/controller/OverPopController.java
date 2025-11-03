package seoultech.se.client.controller;

import javafx.application.Platform;
import javafx.scene.control.TableRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import seoultech.se.client.dto.ScoreRequest;
import seoultech.se.client.dto.ScoreResponse;
import seoultech.se.client.service.NavigationService;
import seoultech.se.client.service.ClientScoreService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class OverPopController extends BaseController {

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private ClientScoreService scoreService;

    @FXML
    private Label scoreLabel;
    @FXML
    private HBox nameInputBox;

    @FXML
    private TextField usernameInput;

    @FXML
    private TableView<Map<String, Object>> scoreBoardTable;


    private long currentScore;

    @FXML
    public void initialize() {
        nameInputBox.setVisible(false);
        nameInputBox.setManaged(false);
    }

    public void setScore(long score) {
        this.currentScore = score;
        scoreLabel.setText(String.valueOf(currentScore));
        scoreService.getScores().thenAccept(scores -> {
            loadScores(scores);

            boolean isTopTen = scores.size() < 10 || (scores.size() >= 10 && currentScore > scores.get(9).getScore());

            if (isTopTen) {
                Platform.runLater(() -> {
                    nameInputBox.setVisible(true);
                    nameInputBox.setManaged(true);
                });
            }
        }).exceptionally(e -> {
            System.err.println("Error fetching scores: " + e.getMessage());
            return null;
        });
    }

    private void loadScores(List<ScoreResponse> scores) {
        Platform.runLater(() -> {
            List<ScoreResponse> displayScores = new ArrayList<>(scores);
            ScoreResponse currentPlayerScore = new ScoreResponse();
            currentPlayerScore.setName("You");
            currentPlayerScore.setScore((int) currentScore);
            // FIXME: These should be passed from the game screen.
            currentPlayerScore.setGameMode("NORMAL");
            currentPlayerScore.setItemMode(false);
            displayScores.add(currentPlayerScore);
            displayScores.sort((s1, s2) -> Integer.compare(s2.getScore(), s1.getScore()));

            ObservableList<Map<String, Object>> scoreData = FXCollections.observableArrayList();
            List<Map<String, Object>> scoresMaps = IntStream.range(0, displayScores.size())
                    .mapToObj(i -> {
                        ScoreResponse scoreResponse = displayScores.get(i);
                        String difficulty = scoreResponse.getGameMode() + (scoreResponse.isItemMode() ? " (Item)" : "");
                        return Map.<String, Object>of(
                                "rank", i + 1,
                                "player", scoreResponse.getName(),
                                "score", scoreResponse.getScore(),
                                "difficulty", difficulty
                        );
                    })
                    .collect(Collectors.toList());
            scoreData.addAll(scoresMaps);
            scoreBoardTable.setItems(scoreData);

            scoreBoardTable.setRowFactory(tv -> new TableRow<>() {
                @Override
                protected void updateItem(Map<String, Object> item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        getStyleClass().remove("score-highlight");
                    } else {
                        if ("You".equals(item.get("player"))) {
                            if (!getStyleClass().contains("score-highlight")) {
                                getStyleClass().add("score-highlight");
                            }
                        } else {
                            getStyleClass().remove("score-highlight");
                        }
                    }
                }
            });
        });
    }

    @FXML
    private void handleMainButton(ActionEvent event) {
        handleNavigation(e -> {
            try {
                closePopup(e);
                navigationService.navigateTo("/view/main-view.fxml");
            } catch (Exception ex) {
                System.err.println("❌ Failed to navigate to main view: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, event);
    }

    @FXML
    private void handleRestartButton(ActionEvent event) {
        handleNavigation(e -> {
            try {
                closePopup(e);
                navigationService.navigateTo("/view/game-view.fxml");
            } catch (Exception ex) {
                System.err.println("❌ Failed to restart game: " + ex.getMessage());
                ex.printStackTrace();
            }
        }, event);
    }

    private void handleNavigation(Consumer<ActionEvent> navigationAction, ActionEvent event) {
        if (nameInputBox.isVisible()) {
            String username = usernameInput.getText();
            if (username == null || username.trim().isEmpty()) {
                // Maybe show an alert to the user
                System.out.println("Username is required to save a new high score.");
                return; // Block navigation
            }
            // Assuming default game mode for now. This should be passed from the game screen.
            ScoreRequest newScore = new ScoreRequest(username, (int) currentScore, "NORMAL", false);
            scoreService.saveScore(newScore).thenRun(() -> {
                Platform.runLater(() -> navigationAction.accept(event));
            }).exceptionally(e -> {
                System.err.println("Failed to save score: " + e.getMessage());
                // Decide if you still want to navigate or show an error
                return null;
            });
        } else {
            navigationAction.accept(event);
        }
    }

    private void closePopup(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
