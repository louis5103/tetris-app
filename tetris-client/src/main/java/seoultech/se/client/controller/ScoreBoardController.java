package seoultech.se.client.controller;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import seoultech.se.client.model.scoreBoard.ScoreBoard;
import seoultech.se.client.service.NavigationService;

@Component
@ConditionalOnProperty(name = "javafx.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ScoreBoardController extends BaseController {
    private final NavigationService navigationService;
    private final ScoreBoard scoreBoard;

    @FXML private VBox scoreBoardContainer;


    @FXML public void initialize() {
        scoreBoardContainer.getChildren().add(scoreBoard);
        scoreBoard.updateDataWhenClicked(false);
    }

    @FXML
    private void handleBackButton() throws IOException {
       navigationService.navigateTo("/view/main-view.fxml");
    }

    @FXML
    private void handleNormalMode() {
        // Handle normal mode selection
        scoreBoard.updateDataWhenClicked(false);
    }

    @FXML
    private void handleItemMode() {
        // Handle item mode selection
        scoreBoard.updateDataWhenClicked(true);
    }

}
