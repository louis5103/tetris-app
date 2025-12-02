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
    @FXML private Button p2pDirectButton; // P2P Direct 버튼 추가

    @Autowired(required = false)
    private MultiplayerMatchingService matchingService;

    @Autowired
    private SettingsService settingsService;
    
    @Autowired
    private seoultech.se.client.controller.P2PModeSelectionController p2pController; // P2P Controller 주입

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
     * P2P Direct 버튼 핸들러
     */
    @FXML
    public void handleP2PDirect(ActionEvent event) {
        try {
            // 팝업 Stage 닫기 (현재 창)
            Stage currentStage = (Stage) p2pDirectButton.getScene().getWindow();
            currentStage.close();
            
            // P2P 모드 선택 팝업 (커스텀 UI) 생성
            seoultech.se.client.ui.P2PModeSelectionPopup popup = new seoultech.se.client.ui.P2PModeSelectionPopup();
            
            // 팝업을 위한 새 Stage 생성
            Stage p2pStage = new Stage();
            Scene scene = new Scene(popup);
            p2pStage.setScene(scene);
            p2pStage.setTitle("P2P Direct Connect");
            p2pStage.setResizable(false);
            
            // 콜백 연결 (Controller 메서드 호출)
            popup.setOnHost(() -> {
                p2pStage.close();
                System.out.println("Host mode selected");
                if (p2pController != null) {
                    p2pController.handleHostGame();
                    transitionToP2PGame(true); // Host 모드로 게임 화면 진입
                }
            });
            
            popup.setOnConnect(() -> {
                String ip = popup.getIpAddress();
                String port = popup.getPort();
                System.out.println("Connect to " + ip + ":" + port);
                p2pStage.close();
                if (p2pController != null) {
                    p2pController.connectToGame(ip, port);
                    transitionToP2PGame(false); // Guest 모드로 게임 화면 진입
                }
            });
            
            popup.setOnCancel(() -> {
                p2pStage.close();
            });
            
            p2pStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("오류", "P2P 모드 실행 중 오류 발생: " + e.getMessage());
        }
    }

    /**
     * P2P 게임 화면으로 전환
     */
    private void transitionToP2PGame(boolean isHost) {
        try {
            // game-view.fxml 로드
            FXMLLoader loader = new FXMLLoader(
                TetrisApplication.class.getResource("/view/game-view.fxml")
            );

            ApplicationContext context = ApplicationContextProvider.getApplicationContext();
            MultiGameController controller = context.getBean(MultiGameController.class);
            loader.setController(controller);

            Parent gameRoot = loader.load();

            // 1. 게임 모드 설정 (기본값)
            seoultech.se.core.config.GameModeConfig config = seoultech.se.core.config.GameModeConfig.createDefaultClassic();
            controller.initGame(config);

            // 2. P2P 모드 설정 (NetworkGameService 연결)
            seoultech.se.client.service.NetworkGameService netService = context.getBean(seoultech.se.client.service.NetworkGameService.class);
            
            // MultiGameController에 P2P 모드용 초기화 메서드가 없으므로, 
            // 기존 initMultiplayer를 우회하거나 P2P 전용 초기화 로직을 추가해야 함.
            // 여기서는 NetworkExecutionStrategy를 가짜로 만들거나 P2P용으로 개조해야 함.
            // 임시 해결책: MultiGameController에 initP2PMode 추가 필요.
            // 일단은 여기서 콜백을 직접 연결하여 NetworkGameService를 시작함.
            
            controller.startGame(); // UI 초기화
            
            netService.startP2PGame(isHost, 
                myState -> {
                    // 내 상태 업데이트 (Reflection or public method needed on Controller)
                    // MultiGameController.onMyStateUpdate는 private임.
                    // BaseGameController.boardController.setGameState(myState) + updateUI 호출 필요
                    Platform.runLater(() -> {
                        controller.boardController.setGameState(myState);
                        controller.updateUI(controller.boardController.getGameState(), myState); // oldState 처리 필요
                    });
                },
                opponentState -> {
                    // 상대 상태 업데이트
                    // MultiGameController.opponentBoardView.update(opponentState)
                    Platform.runLater(() -> {
                        // Reflection으로 opponentBoardView 접근하거나 getter 필요
                        // controller.getOpponentBoardView().update(opponentState);
                    });
                }
            );

            // Scene 변경
            Stage stage = new Stage();
            Scene gameScene = new Scene(gameRoot);
            stage.setScene(gameScene);
            stage.setTitle("Tetris - P2P Direct (" + (isHost ? "HOST" : "GUEST") + ")");
            stage.setResizable(false);
            
            settingsService.applyScreenSizeClass();
            stage.sizeToScene();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("오류", "게임 화면 로딩 실패: " + e.getMessage());
        }
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
