package seoultech.se.client.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.stage.Stage;
import seoultech.se.client.service.NavigationService;

@Component
public class PausePopController extends BaseController {

    @Autowired
    private NavigationService navigationService;
    
    private Runnable resumeCallback;

    /**
     * 게임 재개 콜백을 설정합니다.
     * @param resumeCallback GameController의 resumeGame 메서드
     */
    public void setResumeCallback(Runnable resumeCallback) {
        this.resumeCallback = resumeCallback;
    }

    /**
     * Resume 버튼 핸들러 - 게임 재개
     */
    @FXML
    private void handleResume(ActionEvent event) {
        closePopup(event);
        if (resumeCallback != null) {
            resumeCallback.run();
        }
    }

    /**
     * Quit 버튼 핸들러 - 메인 화면으로 이동
     */
    @FXML
    private void handleQuit(ActionEvent event) {
        try {
            closePopup(event);
            navigationService.navigateTo("/view/main-view.fxml");
        } catch (Exception e) {
            System.err.println("❌ Failed to navigate to main view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 팝업 창 닫기
     */
    private void closePopup(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
    }
}
