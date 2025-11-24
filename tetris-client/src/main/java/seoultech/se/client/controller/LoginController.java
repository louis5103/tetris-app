package seoultech.se.client.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import seoultech.se.client.service.NavigationService;

/**
 * 🔐 로그인 화면 컨트롤러
 * 
 * 로그인 화면의 UI 이벤트를 처리합니다.
 * - 로그인 버튼: main-view로 이동
 * - SIGN UP 버튼: signup-view로 이동
 */
@Component
public class LoginController extends BaseController {

    @Autowired
    private NavigationService navigationService;

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

    @FXML
    @Override
    public void initialize() {
        super.initialize();
    }

    /**
     * 로그인 버튼 클릭 핸들러
     * main-view로 이동합니다.
     */
    @FXML
    public void handleLogin(ActionEvent event) {
        try {
            System.out.println("🔐 로그인 버튼 클릭 - main-view로 이동");
            navigationService.navigateTo("/view/main-view.fxml");
            System.out.println("✅ main-view로 이동 완료");
        } catch (IOException e) {
            System.err.println("❌ main-view 로드 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * SIGN UP 버튼 클릭 핸들러
     * signup-view로 이동합니다.
     */
    @FXML
    public void handleGoToSignup(ActionEvent event) {
        try {
            System.out.println("📝 회원가입 화면으로 이동");
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
}
