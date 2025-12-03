package seoultech.se.client.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * 릴레이 모드 매칭 대기 팝업
 *
 * 기능:
 * 1. Host가 매칭 버튼을 누르면 대기 화면 표시
 * 2. Guest 연결 시 "Player Matched!" 메시지 표시
 * 3. 3초 카운트다운 후 게임 시작
 */
public final class MatchingWaitPopup extends VBox {

    private Label statusLabel;
    private Label countdownLabel;
    private Label sessionInfoLabel;
    private Button cancelButton;
    private Runnable onCancel;
    private Runnable onMatchComplete;

    private volatile boolean isMatched = false;
    private volatile boolean isCancelled = false;

    public MatchingWaitPopup() {
        this.setAlignment(Pos.CENTER);
        this.setSpacing(20);
        this.setPadding(new Insets(40));
        this.setStyle("-fx-background-color: rgba(0, 0, 0, 0.95); -fx-background-radius: 15; -fx-border-color: #00ff00; -fx-border-width: 3; -fx-border-radius: 15;");
        this.setPrefSize(500, 400);

        // 제목
        Label titleLabel = new Label("WAITING FOR PLAYER");
        titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 28px; -fx-font-weight: bold;");

        // 상태 메시지
        statusLabel = new Label("Waiting for opponent to join...");
        statusLabel.setStyle("-fx-text-fill: #ffaa00; -fx-font-size: 18px; -fx-wrap-text: true;");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(400);
        statusLabel.setAlignment(Pos.CENTER);

        // 카운트다운 라벨 (매칭 후에만 표시)
        countdownLabel = new Label("");
        countdownLabel.setStyle("-fx-text-fill: #00ff00; -fx-font-size: 48px; -fx-font-weight: bold;");
        countdownLabel.setVisible(false);

        // 세션 정보
        sessionInfoLabel = new Label("");
        sessionInfoLabel.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 14px; -fx-font-family: 'Courier New';");
        sessionInfoLabel.setWrapText(true);
        sessionInfoLabel.setMaxWidth(400);
        sessionInfoLabel.setAlignment(Pos.CENTER);

        // 로딩 애니메이션 (텍스트 기반)
        Label loadingLabel = new Label("●  ●  ●");
        loadingLabel.setStyle("-fx-text-fill: white; -fx-font-size: 24px;");

        // 간단한 점 애니메이션
        Thread animationThread = new Thread(() -> {
            String[] frames = {"●  ○  ○", "○  ●  ○", "○  ○  ●", "○  ●  ○"};
            int frameIndex = 0;
            while (!isMatched && !isCancelled) {
                try {
                    final String frame = frames[frameIndex];
                    Platform.runLater(() -> loadingLabel.setText(frame));
                    frameIndex = (frameIndex + 1) % frames.length;
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    break;
                }
            }
            Platform.runLater(() -> loadingLabel.setVisible(false));
        });
        animationThread.setDaemon(true);
        animationThread.start();

        // 취소 버튼
        cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("menu-button-small");
        cancelButton.setOnAction(e -> {
            isCancelled = true;
            if (onCancel != null) onCancel.run();
        });

        this.getChildren().addAll(titleLabel, statusLabel, loadingLabel, countdownLabel, sessionInfoLabel, cancelButton);
    }

    /**
     * 세션 정보 설정
     */
    public void setSessionInfo(String sessionId, String role) {
        sessionInfoLabel.setText(String.format("Session: %s\nRole: %s", sessionId, role));
    }

    /**
     * 매칭 성공 시 호출 (카운트다운 시작)
     */
    public void onPlayerMatched() {
        if (isMatched) return;
        isMatched = true;

        Platform.runLater(() -> {
            statusLabel.setText("Player Matched!");
            statusLabel.setStyle("-fx-text-fill: #00ff00; -fx-font-size: 24px; -fx-font-weight: bold;");
            countdownLabel.setVisible(true);
            cancelButton.setDisable(true);
        });
    }

    /**
     * 카운트다운 업데이트 (3, 2, 1)
     */
    public void updateCountdown(int seconds) {
        Platform.runLater(() -> {
            if (seconds > 0) {
                countdownLabel.setText(String.valueOf(seconds));
            } else {
                countdownLabel.setText("START!");
            }
        });
    }

    /**
     * 취소 콜백 설정
     */
    public void setOnCancel(Runnable onCancel) {
        this.onCancel = onCancel;
    }

    /**
     * 매칭 완료 콜백 설정
     */
    public void setOnMatchComplete(Runnable onMatchComplete) {
        this.onMatchComplete = onMatchComplete;
    }

    /**
     * 카운트다운 완료 후 게임 시작
     */
    public void completeMatching() {
        if (onMatchComplete != null) {
            Platform.runLater(onMatchComplete);
        }
    }

    public boolean isCancelled() {
        return isCancelled;
    }
}
