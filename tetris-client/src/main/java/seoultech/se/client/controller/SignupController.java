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
 * 📝 회원가입 화면 컨트롤러
 * 
 * 회원가입 화면의 UI 이벤트를 처리합니다.
 * - SIGN UP 버튼: 회원가입 작동 로그 출력
 * - BACK TO LOGIN 버튼: login-view로 이동
 */
@Component
public class SignupController extends BaseController {

    @Autowired
    private NavigationService navigationService;

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
    }

    /**
     * SIGN UP 버튼 클릭 핸들러
     * 회원가입 작동 로그를 출력합니다.
     */
    @FXML
    public void handleSignup(ActionEvent event) {
        System.out.println("회원가입 작동");
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
}
