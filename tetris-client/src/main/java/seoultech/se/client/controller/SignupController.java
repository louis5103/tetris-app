package seoultech.se.client.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import seoultech.se.client.dto.SignupRequest;
import seoultech.se.client.dto.SignupResponse;
import seoultech.se.client.service.NavigationService;
import seoultech.se.client.service.UserApiService;

/**
 * 📝 회원가입 화면 컨트롤러
 *
 * 회원가입 화면의 UI 이벤트를 처리합니다.
 * - SIGN UP 버튼: 서버에 회원가입 요청 전송
 * - BACK TO LOGIN 버튼: login-view로 이동
 */
@Component
public class SignupController extends BaseController {

    @Autowired
    private NavigationService navigationService;

    @Autowired
    private UserApiService userApiService;

    @FXML
    private TextField nameField;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Text statusText;

    @FXML
    private Button signupButton;

    @FXML
    private Button backButton;

    @FXML
    @Override
    public void initialize() {
        super.initialize();

        // 상태 텍스트 초기화
        if (statusText != null) {
            statusText.setText("");
            statusText.setVisible(false);
        }
    }

    /**
     * SIGN UP 버튼 클릭 핸들러
     * 서버에 회원가입 요청을 전송합니다.
     */
    @FXML
    public void handleSignup(ActionEvent event) {
        // 입력값 가져오기
        String name = nameField.getText();
        String email = emailField.getText();
        String password = passwordField.getText();

        // 입력값 검증
        String validationError = validateInput(name, email, password);
        if (validationError != null) {
            showError(validationError);
            return;
        }

        // 버튼 비활성화 (중복 클릭 방지)
        signupButton.setDisable(true);
        showInfo("회원가입 처리 중...");

        // 백그라운드 스레드에서 API 호출
        new Thread(() -> {
            try {
                // 회원가입 요청 DTO 생성
                SignupRequest request = new SignupRequest(name, email, password);

                // 서버에 회원가입 요청
                SignupResponse response = userApiService.signup(request);

                // UI 스레드에서 결과 처리
                Platform.runLater(() -> {
                    showSuccess("회원가입 성공! 로그인 화면으로 이동합니다.");
                    System.out.println("✅ 회원가입 성공: " + response.getEmail());

                    // 1초 후 로그인 화면으로 이동
                    new Thread(() -> {
                        try {
                            Thread.sleep(1000);
                            Platform.runLater(() -> {
                                try {
                                    navigationService.navigateTo("/view/login-view.fxml");
                                } catch (IOException e) {
                                    System.err.println("❌ login-view 로드 실패: " + e.getMessage());
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
                    signupButton.setDisable(false);
                });

            } catch (Exception e) {
                // UI 스레드에서 예외 처리
                Platform.runLater(() -> {
                    showError("예상치 못한 오류가 발생했습니다.");
                    signupButton.setDisable(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * BACK TO LOGIN 버튼 클릭 핸들러
     * login-view로 이동합니다.
     */
    @FXML
    public void handleBack(ActionEvent event) {
        try {
            System.out.println("🔙 로그인 화면으로 돌아가기");
            navigationService.navigateTo("/view/login-view.fxml");
            System.out.println("✅ login-view로 이동 완료");
        } catch (IOException e) {
            System.err.println("❌ login-view 로드 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 입력값 검증
     *
     * @param name 이름
     * @param email 이메일
     * @param password 비밀번호
     * @return 에러 메시지 (유효하면 null)
     */
    private String validateInput(String name, String email, String password) {
        if (name == null || name.trim().isEmpty()) {
            return "이름을 입력하세요.";
        }

        if (name.length() > 20) {
            return "이름은 20자 이하여야 합니다.";
        }

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
