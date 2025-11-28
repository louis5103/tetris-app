package seoultech.se.client.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import seoultech.se.client.TetrisApplication;
import seoultech.se.client.config.ApplicationContextProvider;
import seoultech.se.client.service.MultiplayerMatchingService;
import seoultech.se.client.service.SettingsService;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.model.enumType.Difficulty;

/**
 * 멀티플레이 모드 및 난이도 선택 컨트롤러
 */
@Component
public class MultiplayerModeSelectionController extends BaseController {

    @FXML private ComboBox<GameplayTypeItem> gameModeComboBox;
    @FXML private ComboBox<DifficultyItem> difficultyComboBox;
    @FXML private Button startButton;
    @FXML private Button cancelButton;

    @Autowired(required = false)
    private MultiplayerMatchingService matchingService;

    @Autowired
    private SettingsService settingsService;

    @Autowired(required = false)
    private seoultech.se.client.service.AuthService authService;

    private String serverBaseUrl;
    private String jwtToken;

    @FXML
    public void initialize() {
        super.initialize();

        // 게임 모드 ComboBox 초기화
        gameModeComboBox.setItems(FXCollections.observableArrayList(
            new GameplayTypeItem(GameplayType.CLASSIC, "클래식"),
            new GameplayTypeItem(GameplayType.ARCADE, "아케이드")
        ));
        gameModeComboBox.getSelectionModel().selectFirst();

        // 난이도 ComboBox 초기화
        difficultyComboBox.setItems(FXCollections.observableArrayList(
            new DifficultyItem(Difficulty.EASY, "쉬움"),
            new DifficultyItem(Difficulty.NORMAL, "보통"),
            new DifficultyItem(Difficulty.HARD, "어려움")
        ));

        // 현재 설정된 난이도 선택
        Difficulty currentDifficulty = settingsService.getCurrentDifficulty();
        for (DifficultyItem item : difficultyComboBox.getItems()) {
            if (item.getDifficulty() == currentDifficulty) {
                difficultyComboBox.getSelectionModel().select(item);
                break;
            }
        }

        System.out.println("✅ MultiplayerModeSelectionController initialized");
    }

    /**
     * 서버 URL과 JWT 토큰 설정 (MainController에서 호출)
     */
    public void setConnectionInfo(String serverBaseUrl, String jwtToken) {
        this.serverBaseUrl = serverBaseUrl;
        this.jwtToken = jwtToken;
    }

    /**
     * 시작 버튼 핸들러
     */
    @FXML
    public void handleStart(ActionEvent event) {
        GameplayTypeItem selectedMode = gameModeComboBox.getSelectionModel().getSelectedItem();
        DifficultyItem selectedDifficulty = difficultyComboBox.getSelectionModel().getSelectedItem();

        if (selectedMode == null || selectedDifficulty == null) {
            showErrorAlert("선택 오류", "게임 모드와 난이도를 선택해주세요.");
            return;
        }

        System.out.println("🎮 Selected mode: " + selectedMode.getGameplayType().getDisplayName());
        System.out.println("🎯 Selected difficulty: " + selectedDifficulty.getDifficulty().getDisplayName());

        try {
            // 팝업 Stage 닫기
            Stage popupStage = (Stage) startButton.getScene().getWindow();

            // 메인 윈도우 가져오기
            Stage mainStage = (Stage) popupStage.getOwner();
            if (mainStage == null) {
                System.err.println("❌ Cannot get main Stage");
                return;
            }

            // 팝업 닫기
            popupStage.close();

            // matching-view.fxml 로드
            FXMLLoader loader = new FXMLLoader(
                TetrisApplication.class.getResource("/view/matching-view.fxml")
            );

            // Controller Factory 설정 (Spring DI)
            ApplicationContext context = ApplicationContextProvider.getApplicationContext();
            loader.setControllerFactory(context::getBean);

            // FXML 로드
            Parent matchingRoot = loader.load();

            // MatchingController에 매칭 시작
            MatchingController matchingController = loader.getController();

            // 메인 윈도우의 Scene 변경
            Scene matchingScene = new Scene(matchingRoot);
            mainStage.setScene(matchingScene);
            mainStage.setTitle("Tetris - 매칭 중...");

            // 화면 크기 CSS 클래스 적용
            settingsService.applyScreenSizeClass();
            mainStage.sizeToScene();

            // 매칭 시작 (선택한 모드와 난이도로)
            matchingController.startMatching(
                serverBaseUrl,
                jwtToken,
                selectedMode.getGameplayType()
            );

            System.out.println("✅ Matching screen loaded");

        } catch (IOException e) {
            System.err.println("❌ Failed to load matching-view.fxml");
            e.printStackTrace();
            showErrorAlert("화면 로딩 오류", "매칭 화면을 불러올 수 없습니다: " + e.getMessage());
        }
    }

    /**
     * 취소 버튼 핸들러
     */
    @FXML
    public void handleCancel(ActionEvent event) {
        System.out.println("🔙 User cancelled multiplayer mode selection");
        try {
            Stage stage = (Stage) cancelButton.getScene().getWindow();
            stage.close();
        } catch (Exception e) {
            System.err.println("❌ Failed to close popup: " + e.getMessage());
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
     * GameplayType 아이템 (ComboBox용)
     */
    private static class GameplayTypeItem {
        private final GameplayType gameplayType;
        private final String displayName;

        public GameplayTypeItem(GameplayType gameplayType, String displayName) {
            this.gameplayType = gameplayType;
            this.displayName = displayName;
        }

        public GameplayType getGameplayType() {
            return gameplayType;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Difficulty 아이템 (ComboBox용)
     */
    private static class DifficultyItem {
        private final Difficulty difficulty;
        private final String displayName;

        public DifficultyItem(Difficulty difficulty, String displayName) {
            this.difficulty = difficulty;
            this.displayName = displayName;
        }

        public Difficulty getDifficulty() {
            return difficulty;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
