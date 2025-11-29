package seoultech.se.client.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;
import seoultech.se.client.TetrisApplication;
import seoultech.se.client.config.ApplicationContextProvider;
import seoultech.se.client.service.MultiplayerMatchingService;
import seoultech.se.client.service.SettingsService;
import seoultech.se.core.config.GameplayType;

/**
 * 매칭 완료 화면 컨트롤러
 *
 * 매칭이 성공했을 때 보여지는 화면으로:
 * - 상대방 정보 표시
 * - 게임 시작 카운트다운 (3초)
 * - 자동으로 게임 화면으로 전환
 */
@Component
public class MatchFoundController extends BaseController {

    @FXML private Label titleLabel;
    @FXML private Label opponentNameLabel;
    @FXML private Label opponentEmailLabel;
    @FXML private Label gameModeLabel;
    @FXML private Label countdownLabel;
    @FXML private Label messageLabel;

    @Autowired(required = false)
    private MultiplayerMatchingService matchingService;

    @Autowired
    private SettingsService settingsService;

    private Timeline countdownTimeline;
    private int countdown = 3;
    private String sessionId;
    private GameplayType gameplayType;
    private long serverTimestamp; // 서버 기준 시간
    private long countdownEndTime; // 카운트다운 종료 시간

    @FXML
    public void initialize() {
        super.initialize();
        System.out.println("✅ MatchFoundController initialized");
    }

    /**
     * 매칭 정보 설정 및 카운트다운 시작
     *
     * @param sessionId 세션 ID
     * @param opponentName 상대방 이름
     * @param opponentEmail 상대방 이메일
     * @param gameplayType 게임 모드
     * @param serverTimestamp 서버 타임스탬프 (카운트다운 동기화용)
     */
    public void startCountdown(String sessionId, String opponentName, String opponentEmail,
                              GameplayType gameplayType, long serverTimestamp) {
        this.sessionId = sessionId;
        this.gameplayType = gameplayType;
        this.serverTimestamp = serverTimestamp;

        // 카운트다운 종료 시간 계산 (서버 시간 기준 + 3초)
        this.countdownEndTime = serverTimestamp + (3 * 1000);

        // 네트워크 지연 계산 (클라이언트 시간 - 서버 시간)
        long networkDelay = System.currentTimeMillis() - serverTimestamp;
        System.out.println("🕐 [MatchFoundController] Network delay: " + networkDelay + "ms");

        // UI 업데이트
        if (opponentName != null && !opponentName.isEmpty()) {
            opponentNameLabel.setText("상대: " + opponentName);
        } else {
            opponentNameLabel.setText("상대: 알 수 없음");
        }

        if (opponentEmail != null && !opponentEmail.isEmpty()) {
            opponentEmailLabel.setText(opponentEmail);
        } else {
            opponentEmailLabel.setVisible(false);
        }

        gameModeLabel.setText("모드: " + gameplayType.getDisplayName());

        // 초기 카운트다운 값 계산 (서버 동기화)
        long currentTime = System.currentTimeMillis();
        long remainingTime = countdownEndTime - currentTime;
        int initialCountdown = (int) Math.ceil(remainingTime / 1000.0);

        if (initialCountdown < 0) {
            initialCountdown = 0;
        } else if (initialCountdown > 3) {
            initialCountdown = 3;
        }

        countdown = initialCountdown;
        countdownLabel.setText(String.valueOf(countdown));

        // 카운트다운 시작 (서버 동기화)
        startSynchronizedCountdownTimer();
    }

    /**
     * 서버 동기화된 카운트다운 타이머 시작
     */
    private void startSynchronizedCountdownTimer() {
        // 100ms마다 체크하여 더 정확한 동기화 제공
        countdownTimeline = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            long currentTime = System.currentTimeMillis();
            long remainingTime = countdownEndTime - currentTime;

            if (remainingTime <= 0) {
                // 카운트다운 종료 - 게임 시작
                countdown = 0;
                countdownLabel.setText("시작!");
                messageLabel.setText("게임을 시작합니다!");
                countdownTimeline.stop();

                // 0.5초 후 게임 화면으로 전환
                Timeline delayTimeline = new Timeline(
                    new KeyFrame(Duration.millis(500), e -> startGame())
                );
                delayTimeline.play();

            } else {
                // 남은 시간을 초 단위로 표시 (올림)
                int newCountdown = (int) Math.ceil(remainingTime / 1000.0);

                // 카운트다운이 변경되었을 때만 UI 업데이트 및 애니메이션
                if (newCountdown != countdown) {
                    countdown = newCountdown;
                    countdownLabel.setText(String.valueOf(countdown));
                    System.out.println("⏱️ [MatchFoundController] Countdown: " + countdown);

                    // 카운트다운 애니메이션 효과
                    countdownLabel.setScaleX(1.5);
                    countdownLabel.setScaleY(1.5);

                    Timeline scaleTimeline = new Timeline(
                        new KeyFrame(Duration.millis(300), e -> {
                            countdownLabel.setScaleX(1.0);
                            countdownLabel.setScaleY(1.0);
                        })
                    );
                    scaleTimeline.play();
                }
            }
        }));

        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    /**
     * 게임 시작 - 게임 화면으로 전환
     */
    private void startGame() {
        stopCountdown();

        Platform.runLater(() -> {
            try {
                // 현재 Stage 가져오기
                Stage stage = (Stage) titleLabel.getScene().getWindow();
                if (stage == null) {
                    System.err.println("❌ Cannot get Stage");
                    return;
                }

                // game-view.fxml 로드
                FXMLLoader loader = new FXMLLoader(
                    TetrisApplication.class.getResource("/view/game-view.fxml")
                );

                // Controller Factory 설정 (Spring DI)
                ApplicationContext context = ApplicationContextProvider.getApplicationContext();
                loader.setControllerFactory(context::getBean);

                // FXML 로드
                Parent gameRoot = loader.load();

                // GameController에 게임 모드 설정
                GameController controller = loader.getController();
                controller.setGameMode(gameplayType, true);

                // NetworkExecutionStrategy 생성 및 설정
                seoultech.se.client.strategy.NetworkExecutionStrategy networkStrategy =
                    matchingService.createNetworkExecutionStrategy();
                controller.setupMultiplayMode(networkStrategy, sessionId);

                // Scene 변경
                Scene gameScene = new Scene(gameRoot);
                stage.setScene(gameScene);
                stage.setTitle("Tetris - MULTIPLAYER");
                stage.setResizable(false);

                // 화면 크기 CSS 클래스 적용
                settingsService.applyScreenSizeClass();
                stage.sizeToScene();

                System.out.println("✅ MULTIPLAYER mode started successfully");

            } catch (IOException e) {
                System.err.println("❌ Failed to load game-view.fxml");
                e.printStackTrace();
                showErrorAlert("게임 로딩 오류", "게임 화면을 불러올 수 없습니다: " + e.getMessage());
            }
        });
    }

    /**
     * 카운트다운 중지
     */
    private void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    /**
     * 에러 알림 표시
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * 정리 작업
     */
    public void cleanup() {
        stopCountdown();
    }
}
