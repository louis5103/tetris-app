package seoultech.se.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class P2PModeSelectionPopup extends VBox {

    private TextField ipField;
    private TextField portField;
    private Label hostInfoLabel;
    private Runnable onHost;
    private Runnable onConnect;
    private Runnable onCancel;
    
    // 릴레이 모드 관련
    private RadioButton directModeRadio;
    private RadioButton relayModeRadio;
    private TextField relayServerIpField;
    private TextField relayServerPortField;
    private TextField sessionIdField;
    private VBox relaySettingsBox;

    public P2PModeSelectionPopup() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(15);
        this.setPadding(new Insets(20));
        this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.9); -fx-background-radius: 10; -fx-border-color: white; -fx-border-radius: 10;");
        this.setPrefSize(500, 550);

        Label titleLabel = new Label("P2P CONNECTION MODE");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        // 연결 모드 선택
        VBox modeSelectionBox = createModeSelectionBox();
        
        // 릴레이 서버 설정 (릴레이 모드 선택 시만 표시)
        relaySettingsBox = createRelaySettingsBox();
        relaySettingsBox.setVisible(false);
        relaySettingsBox.setManaged(false);

        // Host Section
        VBox hostBox = new VBox(10);
        hostBox.setAlignment(Pos.CENTER);
        Label hostLabel = new Label("HOST A GAME");
        hostLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        
        hostInfoLabel = new Label("");
        hostInfoLabel.setStyle("-fx-text-fill: #00ff00; -fx-font-size: 12px; -fx-font-family: 'Courier New';");
        hostInfoLabel.setWrapText(true);
        hostInfoLabel.setMaxWidth(400);
        
        Button hostButton = new Button("Host");
        hostButton.getStyleClass().add("menu-button");
        hostButton.setOnAction(e -> {
            if (onHost != null) onHost.run();
        });
        hostBox.getChildren().addAll(hostLabel, hostInfoLabel, hostButton);

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

        this.getChildren().addAll(titleLabel, modeSelectionBox, relaySettingsBox, hostBox, connectBox, cancelButton);
    }
    
    private VBox createModeSelectionBox() {
        VBox box = new VBox(10);
        box.setAlignment(Pos.CENTER);
        
        Label label = new Label("CONNECTION MODE");
        label.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        
        ToggleGroup modeGroup = new ToggleGroup();
        
        directModeRadio = new RadioButton("Direct P2P (Same Network)");
        directModeRadio.setToggleGroup(modeGroup);
        directModeRadio.setSelected(true);
        directModeRadio.setStyle("-fx-text-fill: white;");
        directModeRadio.setOnAction(e -> {
            relaySettingsBox.setVisible(false);
            relaySettingsBox.setManaged(false);
        });
        
        relayModeRadio = new RadioButton("Relay Mode (School WiFi / Different Networks)");
        relayModeRadio.setToggleGroup(modeGroup);
        relayModeRadio.setStyle("-fx-text-fill: white;");
        relayModeRadio.setOnAction(e -> {
            relaySettingsBox.setVisible(true);
            relaySettingsBox.setManaged(true);
        });
        
        box.getChildren().addAll(label, directModeRadio, relayModeRadio);
        return box;
    }
    
    private VBox createRelaySettingsBox() {
        VBox box = new VBox(8);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: rgba(50, 50, 50, 0.8); -fx-background-radius: 5;");
        
        Label label = new Label("RELAY SERVER SETTINGS");
        label.setStyle("-fx-text-fill: #ffaa00; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        relayServerIpField = new TextField("localhost");
        relayServerIpField.setPromptText("Relay Server IP");
        relayServerIpField.setPrefWidth(200);
        
        relayServerPortField = new TextField("9090");
        relayServerPortField.setPromptText("Relay Server Port");
        relayServerPortField.setPrefWidth(200);
        
        sessionIdField = new TextField();
        sessionIdField.setPromptText("Session ID (e.g., game-123)");
        sessionIdField.setPrefWidth(200);
        
        Label infoLabel = new Label("ℹ️ Both players must use the same Session ID");
        infoLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 10px;");
        infoLabel.setWrapText(true);
        infoLabel.setMaxWidth(300);
        
        box.getChildren().addAll(label, relayServerIpField, relayServerPortField, sessionIdField, infoLabel);
        return box;
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
    
    public void setHostInfo(String ip, int port) {
        hostInfoLabel.setText(String.format("Your IP: %s\nYour Port: %d\n(Share this with the guest)", ip, port));
    }
    
    // 릴레이 모드 관련 getter
    public boolean isRelayMode() {
        return relayModeRadio.isSelected();
    }
    
    public String getRelayServerIp() {
        return relayServerIpField.getText();
    }
    
    public String getRelayServerPort() {
        return relayServerPortField.getText();
    }
    
    public String getSessionId() {
        return sessionIdField.getText();
    }
}
