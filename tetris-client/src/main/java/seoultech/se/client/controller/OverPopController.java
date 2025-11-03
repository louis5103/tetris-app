package seoultech.se.client.controller;

import javafx.application.Platform;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import seoultech.se.client.dto.ScoreRequest;
import seoultech.se.client.service.NavigationService;
import seoultech.se.client.service.ClientScoreService;

import java.util.List;
import java.util.function.Consumer;

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

    private long currentScore;

    @FXML
    public void initialize() {
        nameInputBox.setVisible(false);
        nameInputBox.setManaged(false);
    }

    public void setScore(long score) {
        this.currentScore = score;
        Platform.runLater(() -> scoreLabel.setText(String.valueOf(score)));
        checkTopScoreAndShowInput();
    }

    private void checkTopScoreAndShowInput() {
        scoreService.getScores().thenAccept(scores -> {
            boolean isNewHighScore = scores.isEmpty() || currentScore > scores.get(0).getScore();
            if (isNewHighScore) {
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
