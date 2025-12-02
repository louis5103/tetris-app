package seoultech.se.client.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import seoultech.se.backend.score.ScoreService;
import seoultech.se.client.config.ApplicationContextProvider;
import seoultech.se.client.model.GameAction;
import seoultech.se.client.service.KeyMappingService;
import seoultech.se.client.service.NavigationService;

@Component
public class SettingSceneController extends BaseController {

    @Autowired
    private NavigationService navigationService;
    @Autowired
    private KeyMappingService keyMappingService;

    private ScoreService scoreService;

    @FXML
    private Slider soundSlider;
    @FXML
    private RadioButton screenSizeXS;
    @FXML
    private RadioButton screenSizeS;
    @FXML
    private RadioButton screenSizeM;
    @FXML
    private RadioButton screenSizeL;
    @FXML
    private RadioButton screenSizeXL;
    @FXML
    private RadioButton difficultyEasy;
    @FXML
    private RadioButton difficultyNormal;
    @FXML
    private RadioButton difficultyHard;
    @FXML
    private RadioButton colorModeDefault;
    @FXML
    private RadioButton colorModeRGBlind;
    @FXML
    private RadioButton colorModeBYBlind;
    // @FXML
    // private Button keySettingButton;
    @FXML
    private Button clearScoreBoardButton;
    // @FXML
    // private Button customSettingButton;

    // Key Mapping Buttons
    @FXML
    private Button leftButton, rightButton, rotateButton, downButton, floorButton;
    private GameAction waitingForKey = null;
    private Button activeButton = null;

    // Custom Settings (Deprecated - removed)
    // Custom setting feature has been removed

    @FXML
    private Button resetButton;
    @FXML
    private Button backButton;

    @FXML
    @Override
    public void initialize() {
        super.initialize();

        this.settingsService = ApplicationContextProvider.getApplicationContext().getBean(seoultech.se.client.service.SettingsService.class);
        this.scoreService = ApplicationContextProvider.getApplicationContext().getBean(ScoreService.class);

        loadSettingsToUI();

        soundSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            System.out.println("🔊 Sound volume set to: " + newVal.intValue());
            settingsService.soundVolumeProperty().setValue(newVal.intValue());
            settingsService.saveSettings();
            //TODO : 사운드 볼륨 조절 기능 구현
        });

        updateButtonLabels();
        // loadCustomSettings() removed - custom settings feature deprecated

    }

    private void loadSettingsToUI() {
        settingsService.loadSettings();

        soundSlider.setValue(settingsService.soundVolumeProperty().getValue());
        String screenSize = settingsService.screenSizeProperty().getValue();
        String colorMode = settingsService.colorModeProperty().getValue();
        
        // ✨ Phase 5: 난이도 로드
        String difficulty = settingsService.difficultyProperty().getValue();

        switch (screenSize) {
            case "screenSizeXS":
                screenSizeXS.setSelected(true);
                break;
            case "screenSizeS":
                screenSizeS.setSelected(true);
                break;
            case "screenSizeM":
                screenSizeM.setSelected(true);
                break;
            case "screenSizeL":
                screenSizeL.setSelected(true);
                break;
            case "screenSizeXL":
                screenSizeXL.setSelected(true);
                break;
            default:
                System.out.println("❗ Unknown screen size in settings: " + screenSize);
        }
        
        // ✨ Phase 5: 난이도 UI 설정
        switch (difficulty) {
            case "difficultyEasy":
                difficultyEasy.setSelected(true);
                break;
            case "difficultyNormal":
                difficultyNormal.setSelected(true);
                break;
            case "difficultyHard":
                difficultyHard.setSelected(true);
                break;
            default:
                System.out.println("❗ Unknown difficulty in settings: " + difficulty);
                difficultyNormal.setSelected(true); // 기본값
        }

        switch (colorMode) {
            case "colorModeDefault":
                colorModeDefault.setSelected(true);
                break;
            case "colorModeRGBlind":
                colorModeRGBlind.setSelected(true);
                break;
            case "colorModeBYBlind":
                colorModeBYBlind.setSelected(true);
                break;
            default:
                System.out.println("❗ Unknown color mode in settings: " + colorMode);
        }
    }

    @FXML
    public void handleScreenSizeChange(ActionEvent event) {
        RadioButton selectedRadioButton = (RadioButton) event.getSource();

        double width = 500;
        double height = 700;

        //TODO : 해상도 hardcoding 제거
        switch (selectedRadioButton.getId()) {
            case "screenSizeXS":
                width = 300;
                height = width * 1.2;
                break;
            case "screenSizeS":
                width = 400;
                height = width * 1.2;
                break;
            case "screenSizeM":
                width = 500;
                height = width * 1.2;
                break;
            case "screenSizeL":
                width = 600;
                height = width * 1.2;
                break;
            case "screenSizeXL":
                width = 700;
                height = width * 1.2;
                break;
            default:
                System.out.println("❗ Unknown screen size selected");
        }
        settingsService.screenSizeProperty().setValue(selectedRadioButton.getId());
        settingsService.applyResolution(width, height);
        settingsService.saveSettings();
        System.out.println("🖥️ Screen size set to: " + selectedRadioButton.getId());
    }

    @FXML
    public void handleDifficultyChange(ActionEvent event) {
        RadioButton selectedRadioButton = (RadioButton) event.getSource();
        settingsService.difficultyProperty().setValue(selectedRadioButton.getId());
        settingsService.saveSettings();
        System.out.println("🕹️ Difficulty set to: " + selectedRadioButton.getId());
    }

    @FXML
    public void handleColorModeChange(ActionEvent event) {
        RadioButton selectedRadioButton = (RadioButton) event.getSource();
        settingsService.colorModeProperty().setValue(selectedRadioButton.getId());
        settingsService.saveSettings();

        switch (selectedRadioButton.getId()) {
            case "colorModeDefault":
                System.out.println("🎨 Color mode set to: Default");
                //TODO : 색약모드 해제 기능 구현
                // settingsService.applyColorMode("default");
                break;
            case "colorModeRGBlind":
                System.out.println("🎨 Color mode set to: Red-Green Blindness");
                //TODO : 적녹색약 모드 적용 기능 구현
                // settingsService.applyColorMode("rgblind");
                break;
            case "colorModeBYBlind":
                System.out.println("🎨 Color mode set to: Blue-Yellow Blindness");
                //TODO : 황색약 모드 적용 기능 구현
                // settingsService.applyColorMode("yblind");
                break;
            default:
                System.out.println("❗ Unknown color mode selected");
        }
    }

    @FXML
    public void handleClearScoreBoardButton(ActionEvent event) {
        System.out.println("🧹 Clear Score Board button clicked");
        try {
            scoreService.deleteScoreBoard();
            System.out.println("✅ Score board cleared successfully.");
        } catch (Exception e) {
            System.err.println("❌ Failed to clear score board: " + e.getMessage());
        }
    }

    @FXML
    public void handleCustomSettingButton(ActionEvent event) throws IOException {
        navigationService.navigateTo("/view/custom-setting-view.fxml");
    }

    public void handleKeySettingButton(ActionEvent event) throws IOException {
        navigationService.navigateTo("/view/key-setting-view.fxml");
    }

    @FXML
    public void handleResetButton(ActionEvent event) {
        System.out.println("🔄 Reset all settings to default");
        settingsService.restoreDefaults();
        keyMappingService.resetToDefault();
        loadSettingsToUI();
    }

    @FXML
    public void handleBackButton(ActionEvent event) throws IOException {
        navigationService.navigateTo("/view/main-view.fxml");
        //다른곳에서 setting으로 이동시에는 이전 페이지로 돌아가도록 수정 필요
    }

    /**
     * 모든 버튼의 레이블을 현재 키 매핑으로 업데이트
     */
    private void updateButtonLabels() {
        updateButtonLabel(leftButton, GameAction.MOVE_LEFT, "Left");
        updateButtonLabel(rightButton, GameAction.MOVE_RIGHT, "Right");
        updateButtonLabel(downButton, GameAction.MOVE_DOWN, "Down");
        updateButtonLabel(floorButton, GameAction.HARD_DROP, "Hard Drop");
        updateButtonLabel(rotateButton, GameAction.ROTATE_CLOCKWISE, "Rotate");
    }

    /**
     * 버튼 레이블 업데이트 (액션명 + 현재 키)
     */
    private void updateButtonLabel(Button button, GameAction action, String actionName) {
        keyMappingService.getKey(action).ifPresentOrElse(
            key -> button.setText(actionName + ": " + key.getName()),
            () -> button.setText(actionName + ": (NONE)")
        );
    }

    @FXML
    private void handleLeftButton() {
        startKeyCapture(GameAction.MOVE_LEFT, leftButton);
    }

    @FXML
    private void handleRightButton() {
        startKeyCapture(GameAction.MOVE_RIGHT, rightButton);
    }

    @FXML
    private void handleDownButton() {
        startKeyCapture(GameAction.MOVE_DOWN, downButton);
    }

    @FXML
    private void handleFloorButton() {
        startKeyCapture(GameAction.HARD_DROP, floorButton);
    }

    @FXML
    private void handleRotateButton() {
        startKeyCapture(GameAction.ROTATE_CLOCKWISE, rotateButton);
    }

    /**
     * 키 입력 대기 모드 시작
     */
    private void startKeyCapture(GameAction action, Button button) {
        waitingForKey = action;
        activeButton = button;
        button.setText("Press any key...");
        button.setStyle("-fx-background-color: #4CAF50;");

        // 키 입력 리스너 등록
        rootPane.setOnKeyPressed(this::handleKeyCaptured);
        rootPane.requestFocus();
    }

    /**
     * 키 입력 감지 및 매핑 저장
     */
    private void handleKeyCaptured(KeyEvent event) {
        if (waitingForKey == null) {
            return;
        }

        KeyCode key = event.getCode();

        // ESC는 취소
        if (key == KeyCode.ESCAPE) {
            cancelKeyCapture();
            return;
        }

        // 키 매핑 저장
        boolean success = keyMappingService.setKeyMapping(waitingForKey, key);

        if (success) {
            System.out.println("✅ Key mapped: " + waitingForKey + " → " + key);
            updateButtonLabels();
        } else {
            System.err.println("❌ Failed to map key: " + key);
        }

        cancelKeyCapture();
        event.consume();
    }

    /**
     * 키 입력 대기 취소
     */
    private void cancelKeyCapture() {
        if (activeButton != null) {
            activeButton.setStyle("");
        }
        waitingForKey = null;
        activeButton = null;
        rootPane.setOnKeyPressed(null);
        updateButtonLabels();
    }

    // loadCustomSettings() removed - custom settings feature deprecated

    // addCustomSetting() removed - custom settings feature deprecated

    // createCustomSettingButton() removed - custom settings feature deprecated

    // applyCustomSettings() removed - custom settings feature deprecated

    // handleSaveCustomButton(), handleDeleteCustomButton(), selectCustomSetting() removed - custom settings feature deprecated

    /**
     * 로그아웃 버튼 클릭 핸들러
     * login-view로 이동합니다.
     */
    @FXML
    private void handleLogoutButton() {
        try {
            System.out.println("🚪 로그아웃 - 로그인 화면으로 이동");
            navigationService.navigateTo("/view/login-view.fxml");
            System.out.println("✅ login-view로 이동 완료");
        } catch (IOException e) {
            System.err.println("❌ login-view 로드 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
