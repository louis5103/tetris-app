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
    
    // 매칭 대기 중 실행되는 로컬 게임 컨트롤러
    private SingleGameController localGameController;

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

            // 백그라운드에서 매칭 서비스 시작
            if (matchingService != null) {
                System.out.println("🔍 Starting background matchmaking...");
                matchingService.startMatching(
                    serverBaseUrl,
                    jwtToken,
                    notification -> onMatchSuccess(mainStage, notification, selectedMode.getGameplayType()),
                    errorMsg -> onMatchFailed(errorMsg)
                );
            }

            // game-view.fxml 로드 (로컬 싱글 플레이)
            FXMLLoader loader = new FXMLLoader(
                TetrisApplication.class.getResource("/view/game-view.fxml")
            );

            // Controller 설정 (Spring DI)
            ApplicationContext context = ApplicationContextProvider.getApplicationContext();
            localGameController = context.getBean(SingleGameController.class);
            loader.setController(localGameController);

            // FXML 로드
            Parent gameRoot = loader.load();

            // 게임 초기화
            seoultech.se.client.service.GameModeConfigFactory configFactory = context.getBean(seoultech.se.client.service.GameModeConfigFactory.class);
            seoultech.se.core.config.GameModeConfig config = configFactory.create(selectedMode.getGameplayType(), settingsService.getCurrentDifficulty());
            
            localGameController.initGame(config);
            localGameController.startGame();

            // 메인 윈도우의 Scene 변경
            Scene gameScene = new Scene(gameRoot);
            mainStage.setScene(gameScene);
            mainStage.setTitle("Tetris - 매칭 대기 중... (로컬 플레이)");
            mainStage.setResizable(false);

            // 화면 크기 CSS 클래스 적용
            settingsService.applyScreenSizeClass();
            mainStage.sizeToScene();

            System.out.println("✅ Local single-player started while waiting for match");

        } catch (IOException e) {
            System.err.println("❌ Failed to load game-view.fxml");
            e.printStackTrace();
            showErrorAlert("화면 로딩 오류", "게임 화면을 불러올 수 없습니다: " + e.getMessage());
        }
    }

    /**
     * 매칭 성공 콜백
     */
    private void onMatchSuccess(Stage mainStage, seoultech.se.backend.dto.MatchFoundNotification notification, GameplayType gameplayType) {
        javafx.application.Platform.runLater(() -> {
            // 매칭 성공 시 로컬 게임 종료
            if (localGameController != null) {
                System.out.println("🧹 [MultiplayerModeSelection] Stopping local background game...");
                localGameController.cleanup();
                localGameController = null;
            }

            System.out.println("✅ Match found!");
            System.out.println("   - Session: " + notification.getSessionId());
            System.out.println("   - Opponent: " + notification.getOpponentName());
            System.out.println("   - Opponent Email: " + notification.getOpponentEmail());

            try {
                // match-found-view.fxml 로드
                FXMLLoader loader = new FXMLLoader(
                    TetrisApplication.class.getResource("/view/match-found-view.fxml")
                );

                // Controller Factory 설정 (Spring DI)
                ApplicationContext context = ApplicationContextProvider.getApplicationContext();
                loader.setControllerFactory(context::getBean);

                // FXML 로드
                Parent matchFoundRoot = loader.load();

                // MatchFoundController에 매칭 정보 설정 및 카운트다운 시작
                MatchFoundController controller = loader.getController();

                // 서버로부터 받은 실제 상대방 정보 사용 (서버 타임스탬프 포함)
                controller.startCountdown(
                    notification.getSessionId(),
                    notification.getOpponentName(),
                    notification.getOpponentEmail(),
                    gameplayType,
                    notification.getServerTimestamp()
                );

                // Scene 변경
                Scene matchFoundScene = new Scene(matchFoundRoot);
                mainStage.setScene(matchFoundScene);
                mainStage.setTitle("Tetris - 매칭 완료!");
                mainStage.setResizable(false);

                // 화면 크기 CSS 클래스 적용
                settingsService.applyScreenSizeClass();
                mainStage.sizeToScene();

                System.out.println("✅ Match found screen loaded");

            } catch (IOException e) {
                System.err.println("❌ Failed to load match-found-view.fxml");
                e.printStackTrace();
                showErrorAlert("화면 로딩 오류", "매칭 완료 화면을 불러올 수 없습니다: " + e.getMessage());
            }
        });
    }

    /**
     * 매칭 실패 콜백
     */
    private void onMatchFailed(String errorMsg) {
        javafx.application.Platform.runLater(() -> {
            System.err.println("❌ Matching failed: " + errorMsg);
            // 실패해도 로컬 싱글 플레이는 계속 진행
            System.out.println("⚠️ Continuing with local single-player mode");
        });
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
