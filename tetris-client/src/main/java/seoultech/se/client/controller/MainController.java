package seoultech.se.client.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import seoultech.se.backend.service.GameService;
import seoultech.se.client.TetrisApplication;
import seoultech.se.client.config.ApplicationContextProvider;
import seoultech.se.client.service.NavigationService;
import seoultech.se.client.service.SettingsService;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.mode.PlayType;


/**
 * 🎮 JavaFX 메인 메뉴 컨트롤러 (Spring DI 통합)
 * 
 * JavaFX UI와 Spring Boot 서비스를 연결하는 컨트롤러
 * - @Component로 Spring DI 컨테이너에 등록
 * - @Autowired로 서비스 레이어 주입
 * - START 버튼을 누르면 게임 화면(game-view.fxml)으로 전환
 * 
 * 핵심 개념:
 * ApplicationContextProvider를 통해 Spring Context에 접근하여
 * 게임 화면의 Controller(GameController)를 Spring Bean으로 생성합니다.
 */
@Component
public class MainController extends BaseController {
    
    @Autowired
    private GameService gameService;

    @Autowired
    private NavigationService navigationService;

   @FXML
    private Button startButton;
    @FXML
    private Button itemStartButton;
    @FXML
    private Button scoreButton;
    @FXML
    private Button endButton;
    @FXML   
    private Button settingsButton;

    @FXML
    private javafx.scene.layout.BorderPane rootPane;

    private Button[] buttons;
    private int currentButtonIndex = 0;
    
    @Autowired
    private SettingsService settingsService;
    
    /**
     * UI 초기화 메서드
     * FXML 파일이 로드된 후 자동으로 호출됩니다
     */
    public void initialize() {
        super.initialize();
        System.out.println("✅ MainController initialized with Spring DI");
        System.out.println("📊 Service Status: " + gameService.getStatus());

        buttons = new Button[] {
            startButton,
            itemStartButton,
            scoreButton,
            endButton,
            settingsButton
        };

        // rootPane이 키 이벤트를 받을 수 있도록 설정
        rootPane.setFocusTraversable(true);
        rootPane.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);

        // Scene이 준비된 후 초기 포커스 설정
        rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> rootPane.requestFocus());
            }
        });
        
        // 초기 버튼 하이라이트
        updateButtonHighlight();
    }

    /**
     * 키 입력 이벤트를 처리하는 메서드
     */
    private void handleKeyPressed(javafx.scene.input.KeyEvent event) {
        System.out.println("🔑 Key pressed: " + event.getCode());
        
        switch (event.getCode()) {
            case UP:
                currentButtonIndex = (currentButtonIndex - 1 + buttons.length) % buttons.length;
                updateButtonHighlight();
                System.out.println("⬆️ Moved to button: " + currentButtonIndex);
                event.consume();
                break;
            case DOWN:
                currentButtonIndex = (currentButtonIndex + 1) % buttons.length;
                updateButtonHighlight();
                System.out.println("⬇️ Moved to button: " + currentButtonIndex);
                event.consume();
                break;
            case ENTER:
                System.out.println("✅ Enter pressed - Firing button: " + currentButtonIndex);
                buttons[currentButtonIndex].fire();
                event.consume();
                break;
            default:
                break;
        }
    }

    /**
     * 현재 선택된 버튼을 시각적으로 강조
     */
    private void updateButtonHighlight() {
        buttons[currentButtonIndex].requestFocus();
    }
    

    private void setupKeyNavigation() {
        // 이 메서드는 더 이상 필요하지 않으므로 내용을 비우거나 삭제할 수 있습니다.
    }

    /**
     * 설정 버튼 액션 - 기존 설정 화면으로 이동
     * (키 매핑, 커스터마이징 등)
     */
    public void handleSettingsButtonAction(ActionEvent event) throws IOException {
        System.out.println("⚙️ Settings button clicked");
        navigationService.navigateTo("/view/setting-view.fxml");
    }

    /**
     * CLASSIC 모드 버튼 액션
     * 클래식 모드 (로컬 싱글, SRS 회전 시스템)로 게임 시작
     */
    public void handleClassicModeAction(ActionEvent event) {
        System.out.println("🎮 CLASSIC mode selected");
        
        // Classic 모드 설정 생성
        GameModeConfig config = GameModeConfig.classic();
        
        // 설정 저장
        settingsService.saveGameModeSettings(PlayType.LOCAL_SINGLE, GameplayType.CLASSIC, true);
        
        // 게임 시작
        startGameWithConfig(event, config, "CLASSIC");
    }
    
    /**
     * ARCADE 모드 버튼 액션
     * 아케이드 모드 (로컬 싱글, 빠른 속도)로 게임 시작
     */
    public void handleArcadeModeAction(ActionEvent event) {
        System.out.println("🕹️ ARCADE mode selected");
        
        // Arcade 모드 설정 생성
        GameModeConfig config = GameModeConfig.arcade();
        
        // 설정 저장
        settingsService.saveGameModeSettings(PlayType.LOCAL_SINGLE, GameplayType.ARCADE, config.isSrsEnabled());
        
        // 게임 시작
        startGameWithConfig(event, config, "ARCADE");
    }
    
    /**
     * MULTIPLAYER 모드 버튼 액션
     * 온라인 멀티플레이 모드로 게임 시작
     */
    public void handleMultiplayerModeAction(ActionEvent event) {
        System.out.println("👥 MULTIPLAYER mode selected");
        
        // TODO: 온라인 연결 체크 및 로비 화면으로 전환
        // 현재는 클래식 설정으로 시작
        GameModeConfig config = GameModeConfig.classic();
        
        // 설정 저장
        settingsService.saveGameModeSettings(PlayType.ONLINE_MULTI, GameplayType.CLASSIC, true);
        
        // 게임 시작 (향후 로비 화면으로 변경 예정)
        startGameWithConfig(event, config, "MULTIPLAYER");
    }
    
    /**
     * 게임 모드 설정을 적용하여 게임을 시작합니다
     * 
     * @param event 버튼 클릭 이벤트
     * @param config 게임 모드 설정
     * @param modeName 모드 이름 (로그용)
     */
    private void startGameWithConfig(ActionEvent event, GameModeConfig config, String modeName) {
        try {
            // 1단계: 현재 Stage 가져오기
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            
            // 2단계: game-view.fxml 로드
            FXMLLoader loader = new FXMLLoader(
                TetrisApplication.class.getResource("/view/game-view.fxml")
            );
            
            // 3단계: Controller Factory 설정 (Spring DI)
            ApplicationContext context = ApplicationContextProvider.getApplicationContext();
            loader.setControllerFactory(context::getBean);
            
            // 4단계: FXML 로드
            Parent gameRoot = loader.load();
            
            // 5단계: GameController에 설정 전달
            // TODO: GameController에 setGameModeConfig() 메서드 추가 필요
            // GameController controller = loader.getController();
            // controller.setGameModeConfig(config);
            
            // 창 크기 변경 전 현재 위치와 크기 저장
            double currentX = stage.getX();
            double currentY = stage.getY();
            double currentWidth = stage.getWidth();
            double currentHeight = stage.getHeight();
            
            // 6단계: Scene 변경
            Scene gameScene = new Scene(gameRoot);
            stage.setScene(gameScene);
            stage.setTitle("Tetris - " + modeName);
            stage.setResizable(false);
            
            // 새 Scene 크기 가져오기
            stage.sizeToScene();
            double newWidth = stage.getWidth();
            double newHeight = stage.getHeight();
            
            // 중앙 위치 유지
            double deltaX = (newWidth - currentWidth) / 2;
            double deltaY = (newHeight - currentHeight) / 2;
            stage.setX(currentX - deltaX);
            stage.setY(currentY - deltaY);
            
            System.out.println("✅ " + modeName + " mode started successfully");
            
        } catch (IOException e) {
            System.err.println("❌ Failed to load game-view.fxml");
            System.err.println("   Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * SCORE 버튼 액션 (향후 구현 예정)
     */
    public void handleScoreButtonAction() throws IOException {
        System.out.println("🏆 Score button clicked");
        navigationService.navigateTo("/view/score-board.fxml");
    }

    /**
     * EXIT 버튼 액션 - 애플리케이션 종료
     * 
     * Platform.exit()는 JavaFX 애플리케이션을 정상적으로 종료합니다.
     * 이것은 단순히 System.exit()를 호출하는 것보다 좋습니다.
     * 왜냐하면 JavaFX가 정리 작업을 수행할 수 있기 때문입니다.
     * 
     * TetrisApplication의 stop() 메서드가 자동으로 호출되어
     * Spring Context도 깨끗하게 종료됩니다.
     */
    public void handleEndButtonAction() {
        System.out.println("❌ Exit button clicked - Closing application");
        System.out.println("👋 Goodbye!");
        Platform.exit();
    }
}
