package seoultech.se.client.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import javafx.util.Duration;
import seoultech.se.client.TetrisApplication;
import seoultech.se.client.config.ApplicationContextProvider;
import seoultech.se.client.service.MultiplayerMatchingService;
import seoultech.se.client.service.NavigationService;
import seoultech.se.client.service.SettingsService;
import seoultech.se.core.config.GameplayType;

/**
 * 매칭 대기 화면 컨트롤러
 */
@Component
public class MatchingController extends BaseController {

    @FXML private Label titleLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private Label timerLabel;
    @FXML private Label gameModeLabel;
    @FXML private Label difficultyLabel;
    @FXML private Button cancelButton;

    @Autowired(required = false)
    private MultiplayerMatchingService matchingService;

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private SettingsService settingsService;

    @Autowired(required = false)
    private seoultech.se.client.service.AuthService authService;

    private Timeline timerTimeline;
    private int elapsedSeconds = 0;
    private String serverBaseUrl;
    private String jwtToken;
    private GameplayType gameplayType;
    private boolean matchingStarted = false;

    // 매칭 타임아웃 (60초)
    private static final int MATCHING_TIMEOUT_SECONDS = 60;

    @FXML
    public void initialize() {
        super.initialize();
        System.out.println("✅ MatchingController initialized");
    }

    /**
     * 매칭 시작 (MainController에서 호출)
     *
     * @param serverBaseUrl 서버 URL
     * @param jwtToken JWT 토큰
     * @param gameplayType 게임 모드
     */
    public void startMatching(String serverBaseUrl, String jwtToken, GameplayType gameplayType) {
        if (matchingStarted) {
            System.err.println("⚠️ Matching already started");
            return;
        }

        this.serverBaseUrl = serverBaseUrl;
        this.jwtToken = jwtToken;
        this.gameplayType = gameplayType;
        this.matchingStarted = true;

        // UI 업데이트
        gameModeLabel.setText("모드: " + gameplayType.getDisplayName());
        difficultyLabel.setText("난이도: " + settingsService.getCurrentDifficulty().getDisplayName());

        // 타이머 시작
        startTimer();

        // 매칭 서비스 호출
        System.out.println("🔍 Starting matchmaking...");
        matchingService.startMatching(
            serverBaseUrl,
            jwtToken,
            sessionId -> onMatchSuccess(sessionId),
            errorMsg -> onMatchFailed(errorMsg)
        );
    }

    /**
     * 타이머 시작
     */
    private void startTimer() {
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            elapsedSeconds++;
            timerLabel.setText("대기 시간: " + elapsedSeconds + "초");

            // 타임아웃 체크
            if (elapsedSeconds >= MATCHING_TIMEOUT_SECONDS) {
                onMatchTimeout();
            }
        }));
        timerTimeline.setCycleCount(Animation.INDEFINITE);
        timerTimeline.play();
    }

    /**
     * 타이머 중지
     */
    private void stopTimer() {
        if (timerTimeline != null) {
            timerTimeline.stop();
            timerTimeline = null;
        }
    }

    /**
     * 매칭 성공 콜백
     */
    private void onMatchSuccess(String sessionId) {
        Platform.runLater(() -> {
            stopTimer();
            System.out.println("✅ Match found! Session: " + sessionId);

            try {
                // 게임 화면으로 전환
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
                backToMainMenu();
            }
        });
    }

    /**
     * 매칭 실패 콜백
     */
    private void onMatchFailed(String errorMsg) {
        Platform.runLater(() -> {
            stopTimer();
            System.err.println("❌ Matching failed: " + errorMsg);
            showErrorAlert("매칭 실패", "서버에 연결할 수 없습니다:\n" + errorMsg);
            backToMainMenu();
        });
    }

    /**
     * 매칭 타임아웃 처리
     */
    private void onMatchTimeout() {
        stopTimer();
        System.err.println("⏱️ Matching timeout");

        // 매칭 취소
        if (matchingService != null) {
            matchingService.cancelMatching(serverBaseUrl);
        }

        Platform.runLater(() -> {
            showErrorAlert("매칭 타임아웃",
                "매칭 시간이 초과되었습니다.\n" +
                "현재 대기 중인 플레이어가 없습니다.\n" +
                "나중에 다시 시도해주세요.");
            backToMainMenu();
        });
    }

    /**
     * 매칭 취소 버튼 핸들러
     */
    @FXML
    public void handleCancelMatching(ActionEvent event) {
        System.out.println("🛑 User cancelled matching");
        stopTimer();

        // 매칭 취소 API 호출
        if (matchingService != null) {
            matchingService.cancelMatching(serverBaseUrl);
        }

        backToMainMenu();
    }

    /**
     * 메인 메뉴로 돌아가기
     */
    private void backToMainMenu() {
        try {
            navigationService.navigateTo("/view/main-view.fxml");
        } catch (IOException e) {
            System.err.println("❌ Failed to navigate back to main menu");
            e.printStackTrace();
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
        stopTimer();
    }
}
