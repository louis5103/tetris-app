package seoultech.se.client.controller;

import org.springframework.beans.factory.annotation.Autowired;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import seoultech.se.client.service.SettingsService;

public abstract class BaseController {

    @Autowired
    protected SettingsService settingsService;

    @FXML
    protected Pane rootPane;

    @FXML
    public void initialize() {
        bindBaseFontSizeToStageSize();
    }

    protected void bindBaseFontSizeToStageSize() {
        if (rootPane != null) {
            NumberBinding baseSizeBinding = Bindings.createDoubleBinding(
                    () -> (rootPane.getWidth() + rootPane.getHeight()) / 100,
                    rootPane.widthProperty(),
                    rootPane.heightProperty()
            );
            rootPane.styleProperty().bind(Bindings.concat("-fx-font-size: ", baseSizeBinding.asString(), "px;"));
        }
    }
}