package seoultech.se.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class P2PModeSelectionPopup extends VBox {

    private TextField ipField;
    private TextField portField;
    private Runnable onHost;
    private Runnable onConnect;
    private Runnable onCancel;

    public P2PModeSelectionPopup() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setPadding(new Insets(20));
        this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-background-radius: 10; -fx-border-color: white; -fx-border-radius: 10;");
        this.setPrefSize(400, 300);

        Label titleLabel = new Label("P2P DIRECT CONNECT");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        // Host Section
        VBox hostBox = new VBox(10);
        hostBox.setAlignment(Pos.CENTER);
        Label hostLabel = new Label("HOST A GAME");
        hostLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        Button hostButton = new Button("Host");
        hostButton.getStyleClass().add("menu-button");
        hostButton.setOnAction(e -> {
            if (onHost != null) onHost.run();
        });
        hostBox.getChildren().addAll(hostLabel, hostButton);

        // Connect Section
        VBox connectBox = new VBox(10);
        connectBox.setAlignment(Pos.CENTER);
        Label connectLabel = new Label("CONNECT TO HOST");
        connectLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        
        ipField = new TextField();
        ipField.setPromptText("Host IP Address");
        ipField.setPrefWidth(200);
        
        portField = new TextField();
        portField.setPromptText("Host Port");
        portField.setPrefWidth(200);
        
        Button connectButton = new Button("Connect");
        connectButton.getStyleClass().add("menu-button");
        connectButton.setOnAction(e -> {
            if (onConnect != null) onConnect.run();
        });
        
        connectBox.getChildren().addAll(connectLabel, ipField, portField, connectButton);

        // Cancel Button
        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("menu-button-small");
        cancelButton.setOnAction(e -> {
            if (onCancel != null) onCancel.run();
        });

        this.getChildren().addAll(titleLabel, hostBox, connectBox, cancelButton);
    }

    public String getIpAddress() {
        return ipField.getText();
    }

    public String getPort() {
        return portField.getText();
    }

    public void setOnHost(Runnable onHost) {
        this.onHost = onHost;
    }

    public void setOnConnect(Runnable onConnect) {
        this.onConnect = onConnect;
    }

    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }
}
