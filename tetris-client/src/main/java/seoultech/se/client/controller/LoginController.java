package seoultech.se.client.controller;

import java.io.IOException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.util.Duration;
import seoultech.se.client.dto.LoginRequest;
import seoultech.se.client.dto.LoginResponse;
import seoultech.se.client.service.AuthService;
import seoultech.se.client.service.NavigationService;
import seoultech.se.client.service.UserApiService;

/**
 * 🔐 로그인 화면 컨트롤러
 *
 * 로그인 화면의 UI 이벤트를 처리합니다.
 * - 로그인 버튼: 서버에 로그인 요청 후 main-view로 이동
 * - SIGN UP 버튼: signup-view로 이동
 */
@Component
public class LoginController extends BaseController {

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private UserApiService userApiService;

    @Autowired
    private AuthService authService;

    @FXML
    private Label titleLabel;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Text statusText;

    @FXML
    private Button loginButton;

    @FXML
    private Button signupButton;

    @FXML
    private Button backButton;

    private MediaPlayer mediaPlayer;

    @FXML
    @Override
    public void initialize() {
        super.initialize();

        // 상태 텍스트 초기화
        if (statusText != null) {
            statusText.setText("");
            statusText.setVisible(false);
        }

        // 타이틀 애니메이션 효과 (Scale Pulse)
        if (titleLabel != null) {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(1), titleLabel);
            scaleTransition.setFromX(1.0);
            scaleTransition.setFromY(1.0);
            scaleTransition.setToX(1.2);
            scaleTransition.setToY(1.2);
            scaleTransition.setCycleCount(ScaleTransition.INDEFINITE);
            scaleTransition.setAutoReverse(true);
            scaleTransition.play();
            System.out.println("✨ Title animation started in Login View");
        }

        // 배경 음악 재생
        try {
            if (mediaPlayer == null) {
                URL resource = getClass().getResource("/Tetris - Bradinsky.mp3");
                if (resource != null) {
                    Media media = seoultech.se.client.util.MediaUtils.loadMedia(resource);
                    mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                } else {
                    System.err.println("❌ Could not find music file: /Tetris - Bradinsky.mp3");
                }
            }
            
            if (mediaPlayer != null) {
                mediaPlayer.play();
                System.out.println("🎵 Background music started in Login View");
            }
        } catch (Exception e) {
            System.err.println("❌ Error playing music: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 배경 음악 중지
     */
    public void stopBackgroundMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            System.out.println("🔇 Background music stopped in Login View");
        }
    }

    /**
     * 로그인 버튼 클릭 핸들러
     * 서버에 로그인 요청 후 성공하면 main-view로 이동합니다.
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        // 입력값 가져오기
        String email = emailField.getText();
        String password = passwordField.getText();

        // 입력값 검증
        String validationError = validateInput(email, password);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        // 버튼 비활성화 (중복 클릭 방지)
        loginButton.setDisable(true);
        showInfo("로그인 처리 중...");

        // 백그라운드 스레드에서 API 호출
        new Thread(() -> {
            try {
                // 로그인 요청 DTO 생성
                LoginRequest request = new LoginRequest(email, password);

                // 서버에 로그인 요청
                LoginResponse response = userApiService.login(request);

                // AuthService에 사용자 정보 및 토큰 저장
                // 서버에서 사용자 이름을 받지 못하므로 이메일의 앞부분을 이름으로 사용
                String userName = email.split("@")[0];
                authService.setAuthenticatedUser(
                    response.getId(),
                    email,
                    userName,
                    response.getToken(),
                    response.getRefreshToken()
                );

                // UI 스레드에서 결과 처리
                Platform.runLater(() -> {
                    showSuccess("로그인 성공! 메인 화면으로 이동합니다.");
                    System.out.println("✅ 로그인 성공: " + email);

                    // 0.5초 후 메인 화면으로 이동
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            Platform.runLater(() -> {
                                try {
                                    stopBackgroundMusic(); // 음악 중지
                                    navigationService.navigateTo("/view/main-view.fxml");
                                } catch (IOException e) {
                                    System.err.println("❌ main-view 로드 실패: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                });

            } catch (UserApiService.ApiException e) {
                // UI 스레드에서 에러 처리
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    loginButton.setDisable(false);
                });

            } catch (Exception e) {
                // UI 스레드에서 예외 처리
                Platform.runLater(() -> {
                    showError("예상치 못한 오류가 발생했습니다.");
                    loginButton.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * SIGN UP 버튼 클릭 핸들러
     * signup-view로 이동합니다.
     */
    @FXML
    public void handleGoToSignup(ActionEvent event) {
        try {
            System.out.println("📝 회원가입 화면으로 이동");
            stopBackgroundMusic(); // 음악 중지
            navigationService.navigateTo("/view/signup-view.fxml");
            System.out.println("✅ signup-view로 이동 완료");
        } catch (IOException e) {
            System.err.println("❌ signup-view 로드 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * BACK 버튼 클릭 핸들러
     * (현재 FXML에서는 visible=false이지만 향후 사용을 위해 구현)
     */
    @FXML
    public void handleBack(ActionEvent event) {
        System.out.println("🔙 뒤로가기 버튼 클릭");
        // 필요시 이전 화면으로 이동하는 로직 추가
    }

    /**
     * 입력값 검증
     *
     * @param email 이메일
     * @param password 비밀번호
     * @return 에러 메시지 (유효하면 null)
     */
    private String validateInput(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            return "이메일을 입력하세요.";
        }

        if (!isValidEmail(email)) {
            return "유효한 이메일 주소를 입력하세요.";
        }

        if (password == null || password.isEmpty()) {
            return "비밀번호를 입력하세요.";
        }

        if (password.length() < 8) {
            return "비밀번호는 8자 이상이어야 합니다.";
        }

        return null;
    }

    /**
     * 이메일 형식 검증
     *
     * @param email 이메일 주소
     * @return 유효하면 true
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * 에러 메시지 표시
     *
     * @param message 에러 메시지
     */
    private void showError(String message) {
        if (statusText != null) {
            statusText.setText(message);
            statusText.setFill(Color.RED);
            statusText.setVisible(true);
        }
        System.err.println("❌ " + message);
    }

    /**
     * 성공 메시지 표시
     *
     * @param message 성공 메시지
     */
    private void showSuccess(String message) {
        if (statusText != null) {
            statusText.setText(message);
            statusText.setFill(Color.GREEN);
            statusText.setVisible(true);
        }
        System.out.println("✅ " + message);
    }

    /**
     * 정보 메시지 표시
     *
     * @param message 정보 메시지
     */
    private void showInfo(String message) {
        if (statusText != null) {
            statusText.setText(message);
            statusText.setFill(Color.BLUE);
            statusText.setVisible(true);
        }
        System.out.println("ℹ️ " + message);
    }
}
