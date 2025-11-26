package seoultech.se.client.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import seoultech.se.backend.service.GameService;
import seoultech.se.client.TetrisApplication;
import seoultech.se.client.config.ApplicationContextProvider;
import seoultech.se.client.service.NavigationService;
import seoultech.se.client.service.SettingsService;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.engine.mode.PlayType;


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
    private HBox singlePlayMenuBox;
    @FXML
    private HBox battleModeMenuBox;
    @FXML
    private HBox p2pModeMenuBox;

    @FXML
    private Button singlePlayButton;
    @FXML
    private Button classicButton;
    @FXML
    private Button arcadeButton;
    @FXML
    private Button singleBackButton;
    @FXML
    private Button battleModeButton;
    @FXML
    private Button battleClassicButton;
    @FXML
    private Button battleArcadeButton;
    @FXML
    private Button battleTimeAttackButton;
    @FXML
    private Button battleBackButton;
    @FXML
    private Button p2pModeButton;
    @FXML
    private Button p2pServerButton;
    @FXML
    private Button p2pClientButton;
    @FXML
    private Button p2pBackButton;
    @FXML
    private Button multiplayerButton;
    @FXML
    private Button scoreButton;
    @FXML
    private Button endButton;
    @FXML   
    private Button settingsButton;

    @FXML
    private javafx.scene.layout.BorderPane rootPane;
    
    @FXML
    private javafx.scene.layout.Pane overlayPane;

    private Button[] buttons;
    private int currentButtonIndex = 0;
    
    @Autowired
    private SettingsService settingsService;

    @Autowired(required = false)
    private seoultech.se.client.service.AuthService authService;

    /**
     * UI 초기화 메서드
     * FXML 파일이 로드된 후 자동으로 호출됩니다
     */
    public void initialize() {
        super.initialize();
        System.out.println("✅ MainController initialized with Spring DI");
        System.out.println("📊 Service Status: " + gameService.getStatus());

        // 초기에는 하위 메뉴들 숨김
        setSinglePlayMenuVisibility(false);
        setSinglePlayButtonVisibility(true);
        setBattleModeMenuVisibility(false);
        setBattleModeButtonVisibility(true);
        setP2pModeMenuVisibility(false);
        setP2pModeButtonVisibility(true);

        buttons = new Button[] {
            singlePlayButton,   // 0
            battleModeButton,   // 1
            p2pModeButton,      // 2
            multiplayerButton,  // 3
            scoreButton,        // 4
            endButton           // 5
        };

        // 버튼이 모두 로드되었는지 확인
        System.out.println("📋 Button Array Order:");
        boolean allButtonsLoaded = true;
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i] == null) {
                allButtonsLoaded = false;
                System.err.println("⚠️ Button " + i + " not loaded from FXML");
                break;
            } else {
                System.out.println("  [" + i + "] " + buttons[i].getText() + " (fx:id=" + buttons[i].getId() + ")");
            }
        }

        if (!allButtonsLoaded) {
            System.err.println("❌ Not all buttons loaded. Skipping key navigation setup.");
            return;
        }

        // 버튼 이벤트 리스너 설정
        setupButtonEventListeners();

        // Scene이 준비되면 키 이벤트 설정 (한 번만 등록)
        if (rootPane.getScene() != null) {
            // Scene이 이미 존재하면 즉시 등록
            rootPane.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, this::handleKeyPressed);
            System.out.println("🎯 Key navigation setup completed on Scene");
        } else {
            // Scene이 아직 없으면 리스너로 대기
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && oldScene == null) {
                    // Scene이 처음 설정될 때만 등록
                    newScene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, this::handleKeyPressed);
                    System.out.println("🎯 Scene key handler registered");
                }
            });
        }
        
        // 초기 버튼 하이라이트
        updateButtonHighlight();
        
        System.out.println("🎮 Key navigation: ↑/↓ to move, Enter to select");
        System.out.println("🖱️  Mouse: Click buttons directly or use Tab to navigate");
    }

    /**
     * 버튼 배열에 이벤트 리스너를 설정하는 메서드
     * 버튼 배열이 변경될 때마다 호출되어야 함
     */
    private void setupButtonEventListeners() {
        // 각 버튼에 이벤트 리스너 추가
        for (int i = 0; i < buttons.length; i++) {
            final int index = i;
            
            // 1. 포커스 리스너 (Tab 네비게이션)
            buttons[i].focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (isNowFocused && currentButtonIndex != index) {
                    // Tab으로 이동했을 때만 currentButtonIndex 업데이트
                    System.out.println("🔄 Focus changed by Tab: " + currentButtonIndex + " → " + index);
                    currentButtonIndex = index;
                    syncButtonHighlight(); // requestFocus 없이 하이라이트만 동기화
                }
            });
            
            // 2. 마우스 진입 이벤트 (호버 시 하이라이트 및 포커스 이동)
            buttons[i].setOnMouseEntered(event -> {
                if (currentButtonIndex != index) {
                    currentButtonIndex = index;
                    buttons[index].requestFocus(); // 포커스도 이동
                    syncButtonHighlight();
                    System.out.println("🖱️  Mouse hover: focus moved to button " + index + " [" + buttons[index].getText() + "]");
                }
            });
            
            // 3. 마우스 이탈 이벤트는 제거하지 않음 (선택 상태 유지)
        }
    }

    /**
     * 키 입력 이벤트를 처리하는 메서드
     */
    private void handleKeyPressed(javafx.scene.input.KeyEvent event) {
        // COMMAND 등 수정자 키는 무시
        if (event.getCode().isModifierKey()) {
            return;
        }
        
        System.out.println("🔑 Key pressed: " + event.getCode() + " | Current: " + currentButtonIndex);
        
        switch (event.getCode()) {
            case UP:
                int prevIndex = currentButtonIndex;
                currentButtonIndex = (currentButtonIndex - 1 + buttons.length) % buttons.length;
                updateButtonHighlight();
                System.out.println("⬆️ UP: " + prevIndex + " → " + currentButtonIndex + " [" + buttons[currentButtonIndex].getText() + "]");
                event.consume();
                break;
            case DOWN:
                prevIndex = currentButtonIndex;
                currentButtonIndex = (currentButtonIndex + 1) % buttons.length;
                updateButtonHighlight();
                System.out.println("⬇️ DOWN: " + prevIndex + " → " + currentButtonIndex + " [" + buttons[currentButtonIndex].getText() + "]");
                event.consume();
                break;
            case ENTER:
                System.out.println("✅ ENTER: Firing button " + currentButtonIndex + " [" + buttons[currentButtonIndex].getText() + "]");
                buttons[currentButtonIndex].fire();
                event.consume();
                break;
            default:
                break;
        }
    }

    /**
     * 현재 선택된 버튼을 시각적으로 강조하고 포커스 이동
     * (키보드 화살표 키 사용 시)
     */
    private void updateButtonHighlight() {
        syncButtonHighlight();
        
        // 키보드 네비게이션일 때만 포커스 이동
        if (currentButtonIndex >= 0 && currentButtonIndex < buttons.length) {
            buttons[currentButtonIndex].requestFocus();
            System.out.println("🎯 Highlighted button " + currentButtonIndex + ": " + buttons[currentButtonIndex].getText());
        }
    }
    
    /**
     * 하이라이트만 동기화 (포커스 이동 없이)
     * (Tab 네비게이션 또는 마우스 클릭 시)
     */
    private void syncButtonHighlight() {
        // 모든 버튼의 하이라이트 제거
        for (Button button : buttons) {
            button.getStyleClass().remove("highlighted");
        }
        
        // 현재 버튼에 하이라이트 추가
        if (currentButtonIndex >= 0 && currentButtonIndex < buttons.length) {
            buttons[currentButtonIndex].getStyleClass().add("highlighted");
        }
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
     * 메인 메뉴 버튼들의 가시성을 설정합니다.
     */
    private void setSinglePlayButtonVisibility(boolean visible) {
        singlePlayButton.setVisible(visible);
        // managed는 변경하지 않아서 레이아웃 위치 유지
    }

    /**
     * 싱글 플레이어 하위 메뉴 버튼들의 가시성을 설정합니다.
     */
    private void setSinglePlayMenuVisibility(boolean visible) {
        singlePlayMenuBox.setVisible(visible);
        if (visible) {
            singlePlayMenuBox.setManaged(true);
        }
    }

    /**
     * 배틀 모드 버튼의 가시성을 설정합니다.
     */
    private void setBattleModeButtonVisibility(boolean visible) {
        battleModeButton.setVisible(visible);
    }

    /**
     * 배틀 모드 하위 메뉴 버튼들의 가시성을 설정합니다.
     */
    private void setBattleModeMenuVisibility(boolean visible) {
        battleModeMenuBox.setVisible(visible);
        if (visible) {
            battleModeMenuBox.setManaged(true);
        }
    }

    /**
     * P2P 모드 버튼의 가시성을 설정합니다.
     */
    private void setP2pModeButtonVisibility(boolean visible) {
        p2pModeButton.setVisible(visible);
        // managed는 변경하지 않아서 레이아웃 위치 유지
    }

    /**
     * P2P 모드 하위 메뉴 버튼들의 가시성을 설정합니다.
     */
    private void setP2pModeMenuVisibility(boolean visible) {
        p2pModeMenuBox.setVisible(visible);
        if (visible) {
            p2pModeMenuBox.setManaged(true);
        }
    }

    /**
     * SINGLE PLAY 모드 버튼 액션
     * 클래식 모드, 아이템 모드를 선택할 수 있도록 버튼 보여줌.
     */
    public void handleSinglePlayModeAction(ActionEvent event) {
        System.out.println("🎮 SINGLE PLAY mode selected");
        setSinglePlayButtonVisibility(false);
        setSinglePlayMenuVisibility(true);
        
        // 오버레이 활성화 및 메인 메뉴 버튼 비활성화
        showOverlay();
        disableMainMenuButtons();
        
        // // singlePlayMenuBox를 오버레이 위로
        // singlePlayMenuBox.toFront();
        
        // 버튼 배열: 오직 싱글 플레이 하위 메뉴 버튼들만 포함
        buttons = new Button[] {
            classicButton,      // 0
            arcadeButton,       // 1
            singleBackButton    // 2
        };
        
        // 버튼 이벤트 리스너 재설정
        setupButtonEventListeners();
        
        // 현재 버튼 인덱스를 첫 번째 버튼(Classic)으로 초기화
        currentButtonIndex = 0;
        updateButtonHighlight();
        
        System.out.println("🔄 Button navigation updated to show CLASSIC and ARCADE modes only");
    }
    /**
     * CLASSIC 모드 버튼 액션
     * 클래식 모드 (로컬 싱글, SRS 회전 시스템)로 게임 시작
     */
    public void handleClassicModeAction(ActionEvent event) {
        System.out.println("🎮 CLASSIC mode selected");
        
        // 저장된 커스텀 설정 로드, 없으면 기본 프리셋 사용
        GameModeConfig config = settingsService.loadCustomGameModeConfig(GameplayType.CLASSIC);
        if (config == null) {
            config = GameModeConfig.classic();
            System.out.println("📋 Using default CLASSIC preset");
        } else {
            System.out.println("📋 Using custom CLASSIC settings");
        }
        
        // 설정 저장
        settingsService.saveGameModeSettings(PlayType.LOCAL_SINGLE, GameplayType.CLASSIC, config.isSrsEnabled());
        
        // 게임 시작
        startGameWithConfig(event, config, "CLASSIC");
    }
    
    /**
     * ARCADE 모드 버튼 액션
     * 아케이드 모드 (로컬 싱글, 빠른 속도)로 게임 시작
     */
    public void handleArcadeModeAction(ActionEvent event) {
        System.out.println("🕹️ ARCADE mode selected");
        
        // 저장된 커스텀 설정 로드, 없으면 기본 프리셋 사용
        GameModeConfig config = settingsService.loadCustomGameModeConfig(GameplayType.ARCADE);
        if (config == null) {
            config = GameModeConfig.arcade();
            System.out.println("📋 Using default ARCADE preset");
        } else {
            System.out.println("📋 Using custom ARCADE settings");
        }
        
        // 설정 저장
        settingsService.saveGameModeSettings(PlayType.LOCAL_SINGLE, GameplayType.ARCADE, config.isSrsEnabled());
        
        // 게임 시작
        startGameWithConfig(event, config, "ARCADE");
    }

    public void handleSingleBackAction(ActionEvent event) {
        System.out.println("🔙 SINGLE PLAY back to main menu");
        singlePlayMenuBox.setVisible(false);
        singlePlayMenuBox.setManaged(false);
        setSinglePlayButtonVisibility(true);
        
        // 오버레이 비활성화 및 메인 메뉴 버튼 활성화
        hideOverlay();
        enableMainMenuButtons();
        
        // 버튼 배열을 메인 메뉴 버튼으로 변경
        buttons = new Button[] {
            singlePlayButton,   // 0
            battleModeButton,   // 1
            p2pModeButton,      // 2
            multiplayerButton,  // 3
            scoreButton,        // 4
            endButton           // 5
        };
        
        // 버튼 이벤트 리스너 재설정
        setupButtonEventListeners();
        
        // 현재 버튼 인덱스를 singlePlayButton으로 초기화
        currentButtonIndex = 0;
        updateButtonHighlight();
        
        System.out.println("🔄 Button navigation updated to main menu");
    }

    /**
     * BATTLE MODE 버튼 액션
     * 배틀 모드의 하위 메뉴를 선택할 수 있도록 버튼 보여줌.
     */
    public void handleBattleModeAction(ActionEvent event) {
        System.out.println("⚔️ BATTLE MODE selected");
        setBattleModeButtonVisibility(false);
        setBattleModeMenuVisibility(true);
        
        // 오버레이 활성화 및 메인 메뉴 버튼 비활성화
        showOverlay();
        disableMainMenuButtons();
        
        // // battleModeMenuBox를 오버레이 위로
        // battleModeMenuBox.toFront();
        
        // 버튼 배열: 오직 배틀 모드 하위 메뉴 버튼들만 포함
        buttons = new Button[] {
            battleClassicButton,    // 0
            battleArcadeButton,     // 1
            battleTimeAttackButton, // 2
            battleBackButton        // 3
        };
        
        // 버튼 이벤트 리스너 재설정
        setupButtonEventListeners();
        
        // 현재 버튼 인덱스를 첫 번째 버튼으로 초기화
        currentButtonIndex = 0;
        updateButtonHighlight();
        
        System.out.println("🔄 Button navigation updated to show BATTLE MODE options only");
    }

    /**
     * Battle Classic 모드 버튼 액션 (미구현)
     */
    public void handleBattleClassicModeAction(ActionEvent event) {
        System.out.println("⚔️ [미구현] Battle Classic mode selected");
        System.out.println("📋 This feature is coming soon!");
    }

    /**
     * Battle Arcade 모드 버튼 액션 (미구현)
     */
    public void handleBattleArcadeModeAction(ActionEvent event) {
        System.out.println("⚔️ [미구현] Battle Arcade mode selected");
        System.out.println("📋 This feature is coming soon!");
    }

    /**
     * Battle Time Attack 모드 버튼 액션 (미구현)
     */
    public void handleBattleTimeAttackModeAction(ActionEvent event) {
        System.out.println("⏱️ [미구현] Battle Time Attack mode selected");
        System.out.println("📋 This feature is coming soon!");
    }

    /**
     * Battle Mode Back 버튼 액션
     * 배틀 모드 메뉴에서 메인 메뉴로 돌아갑니다.
     */
    public void handleBattleBackAction(ActionEvent event) {
        System.out.println("🔙 BATTLE MODE back to main menu");
        battleModeMenuBox.setVisible(false);
        battleModeMenuBox.setManaged(false);
        setBattleModeButtonVisibility(true);
        
        // 오버레이 비활성화 및 메인 메뉴 버튼 활성화
        hideOverlay();
        enableMainMenuButtons();
        
        // 버튼 배열을 메인 메뉴 버튼으로 변경
        buttons = new Button[] {
            singlePlayButton,   // 0
            battleModeButton,   // 1
            p2pModeButton,      // 2
            multiplayerButton,  // 3
            scoreButton,        // 4
            endButton           // 5
        };
        
        // 버튼 이벤트 리스너 재설정
        setupButtonEventListeners();
        
        // 현재 버튼 인덱스를 battleModeButton으로 초기화
        currentButtonIndex = 1;
        updateButtonHighlight();
        
        System.out.println("🔄 Button navigation updated to main menu");
    }

    /**
     * P2P MODE 버튼 액션
     * P2P 모드의 하위 메뉴를 선택할 수 있도록 버튼 보여줌.
     */
    public void handleP2PModeAction(ActionEvent event) {
        System.out.println("🔗 P2P MODE selected");
        setP2pModeButtonVisibility(false);
        setP2pModeMenuVisibility(true);
        
        // 오버레이 활성화 및 메인 메뉴 버튼 비활성화
        showOverlay();
        disableMainMenuButtons();
        
        // // p2pModeMenuBox를 오버레이 위로
        // p2pModeMenuBox.toFront();
        
        // 버튼 배열: 오직 P2P 모드 하위 메뉴 버튼들만 포함
        buttons = new Button[] {
            p2pServerButton,        // 0
            p2pClientButton,        // 1
            p2pBackButton           // 2
        };
        
        // 버튼 이벤트 리스너 재설정
        setupButtonEventListeners();
        
        // 현재 버튼 인덱스를 첫 번째 버튼으로 초기화
        currentButtonIndex = 0;
        updateButtonHighlight();
        
        System.out.println("🔄 Button navigation updated to show P2P MODE options only");
    }

    /**
     * P2P Server 버튼 액션 (미구현)
     */
    public void handleP2pServerAction(ActionEvent event) {
        System.out.println("🖥️ [미구현] P2P Server mode selected");
        System.out.println("📋 This feature is coming soon!");
    }

    /**
     * P2P Client 버튼 액션 (미구현)
     */
    public void handleP2pClientAction(ActionEvent event) {
        System.out.println("💻 [미구현] P2P Client mode selected");
        System.out.println("📋 This feature is coming soon!");
    }

    /**
     * P2P Mode Back 버튼 액션
     * P2P 모드 메뉴에서 메인 메뉴로 돌아갑니다.
     */
    public void handleP2pBackAction(ActionEvent event) {
        System.out.println("🔙 P2P MODE back to main menu");
        p2pModeMenuBox.setVisible(false);
        p2pModeMenuBox.setManaged(false);
        setP2pModeButtonVisibility(true);
        
        // 오버레이 비활성화 및 메인 메뉴 버튼 활성화
        hideOverlay();
        enableMainMenuButtons();
        
        // 버튼 배열을 메인 메뉴 버튼으로 변경
        buttons = new Button[] {
            singlePlayButton,   // 0
            battleModeButton,   // 1
            p2pModeButton,      // 2
            multiplayerButton,  // 3
            scoreButton,        // 4
            endButton           // 5
        };
        
        // 버튼 이벤트 리스너 재설정
        setupButtonEventListeners();
        
        // 현재 버튼 인덱스를 p2pModeButton으로 초기화
        currentButtonIndex = 2;
        updateButtonHighlight();
        
        System.out.println("🔄 Button navigation updated to main menu");
    }

    
    /**
     * MULTIPLAYER 모드 버튼 액션
     * 온라인 멀티플레이 모드로 게임 시작
     */
    public void handleMultiplayerModeAction(ActionEvent event) {
        System.out.println("👥 MULTIPLAYER mode selected");

        // 클래식 설정으로 시작
        GameModeConfig config = GameModeConfig.classic();

        // 설정 저장
        settingsService.saveGameModeSettings(PlayType.ONLINE_MULTI, GameplayType.CLASSIC, true);

        // 멀티플레이 모드로 게임 시작 (매칭 서비스 호출)
        startGameWithConfig(event, config, "MULTIPLAYER", PlayType.ONLINE_MULTI);
    }
    
    /**
     * CLASSIC 모드 설정 버튼 액션
     * 클래식 모드 상세 설정을 팝업으로 표시
     */
    public void handleClassicSettingsAction(ActionEvent event) {
        System.out.println("⚙️ CLASSIC settings button clicked");
        showModeSettingsPopup("CLASSIC", GameplayType.CLASSIC, PlayType.LOCAL_SINGLE);
    }
    
    /**
     * ARCADE 모드 설정 버튼 액션
     * 아케이드 모드 상세 설정을 팝업으로 표시
     */
    public void handleArcadeSettingsAction(ActionEvent event) {
        System.out.println("⚙️ ARCADE settings button clicked");
        showModeSettingsPopup("ARCADE", GameplayType.ARCADE, PlayType.LOCAL_SINGLE);
    }
    
    /**
     * MULTIPLAYER 모드 설정 버튼 액션
     * 멀티플레이 모드 상세 설정을 팝업으로 표시
     */
    public void handleMultiplayerSettingsAction(ActionEvent event) {
        System.out.println("⚙️ MULTIPLAYER settings button clicked");
        showModeSettingsPopup("MULTIPLAYER", GameplayType.CLASSIC, PlayType.ONLINE_MULTI);
    }
    
    /**
     * 모드 설정 팝업 표시
     * 
     * @param modeName 모드 이름
     * @param gameplayType 게임플레이 타입
     * @param playType 플레이 타입
     */
    private void showModeSettingsPopup(String modeName, GameplayType gameplayType, PlayType playType) {
        // 저장된 커스텀 설정 로드, 없으면 기본 프리셋 사용
        GameModeConfig currentConfig = settingsService.loadCustomGameModeConfig(gameplayType);
        if (currentConfig == null) {
            // 저장된 설정이 없으면 프리셋 사용
            if (gameplayType == GameplayType.ARCADE) {
                currentConfig = GameModeConfig.arcade();
            } else {
                currentConfig = GameModeConfig.classic();
            }
            System.out.println("📋 No custom settings found, using default preset for " + modeName);
        } else {
            System.out.println("📋 Loaded custom settings for " + modeName);
        }
        
        // 커스텀 다이얼로그 생성
        javafx.scene.control.Dialog<GameModeConfig> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle(modeName + " 모드 설정");
        dialog.setHeaderText(modeName + " 모드 상세 설정");
        
        // 다이얼로그 버튼
        javafx.scene.control.ButtonType applyButtonType = new javafx.scene.control.ButtonType("적용", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(applyButtonType, javafx.scene.control.ButtonType.CANCEL);
        
        // 설정 UI 구성
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));
        
        int row = 0;
        
        // 기본 정보
        grid.add(new javafx.scene.control.Label("게임플레이 타입:"), 0, row);
        grid.add(new javafx.scene.control.Label(gameplayType.getDisplayName()), 1, row++);
        
        grid.add(new javafx.scene.control.Label("플레이 타입:"), 0, row);
        grid.add(new javafx.scene.control.Label(playType.getDisplayName()), 1, row++);
        
        // 구분선
        javafx.scene.control.Separator separator1 = new javafx.scene.control.Separator();
        grid.add(separator1, 0, row++, 2, 1);
        
        // SRS 회전 설정
        javafx.scene.control.CheckBox srsCheckBox = new javafx.scene.control.CheckBox();
        srsCheckBox.setSelected(currentConfig.isSrsEnabled());
        grid.add(new javafx.scene.control.Label("SRS 회전 시스템:"), 0, row);
        grid.add(srsCheckBox, 1, row++);
        
        // 180도 회전 설정
        javafx.scene.control.CheckBox rotation180CheckBox = new javafx.scene.control.CheckBox();
        rotation180CheckBox.setSelected(currentConfig.isRotation180Enabled());
        grid.add(new javafx.scene.control.Label("180도 회전:"), 0, row);
        grid.add(rotation180CheckBox, 1, row++);
        
        // 하드 드롭 설정
        javafx.scene.control.CheckBox hardDropCheckBox = new javafx.scene.control.CheckBox();
        hardDropCheckBox.setSelected(currentConfig.isHardDropEnabled());
        grid.add(new javafx.scene.control.Label("하드 드롭:"), 0, row);
        grid.add(hardDropCheckBox, 1, row++);
        
        // 홀드 기능 설정
        javafx.scene.control.CheckBox holdCheckBox = new javafx.scene.control.CheckBox();
        holdCheckBox.setSelected(currentConfig.isHoldEnabled());
        grid.add(new javafx.scene.control.Label("홀드 기능:"), 0, row);
        grid.add(holdCheckBox, 1, row++);
        
        // 고스트 피스 설정
        javafx.scene.control.CheckBox ghostCheckBox = new javafx.scene.control.CheckBox();
        ghostCheckBox.setSelected(currentConfig.isGhostPieceEnabled());
        grid.add(new javafx.scene.control.Label("고스트 블록:"), 0, row);
        grid.add(ghostCheckBox, 1, row++);
        
        // 구분선
        javafx.scene.control.Separator separator2 = new javafx.scene.control.Separator();
        grid.add(separator2, 0, row++, 2, 1);
        
        // 드롭 속도 설정
        javafx.scene.control.Label dropSpeedLabel = new javafx.scene.control.Label(
            String.format("%.1fx", currentConfig.getDropSpeedMultiplier()));
        javafx.scene.control.Slider dropSpeedSlider = new javafx.scene.control.Slider(0.5, 3.0, currentConfig.getDropSpeedMultiplier());
        dropSpeedSlider.setShowTickMarks(true);
        dropSpeedSlider.setShowTickLabels(true);
        dropSpeedSlider.setMajorTickUnit(0.5);
        dropSpeedSlider.setBlockIncrement(0.1);
        dropSpeedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            dropSpeedLabel.setText(String.format("%.1fx", newVal.doubleValue()));
        });
        grid.add(new javafx.scene.control.Label("낙하 속도 배율:"), 0, row);
        grid.add(dropSpeedSlider, 1, row);
        grid.add(dropSpeedLabel, 2, row++);
        
        // 소프트 드롭 속도 설정
        javafx.scene.control.Label softDropLabel = new javafx.scene.control.Label(
            String.format("%.0f", currentConfig.getSoftDropSpeed()));
        javafx.scene.control.Slider softDropSlider = new javafx.scene.control.Slider(1.0, 50.0, currentConfig.getSoftDropSpeed());
        softDropSlider.setShowTickMarks(true);
        softDropSlider.setShowTickLabels(true);
        softDropSlider.setMajorTickUnit(10);
        softDropSlider.setBlockIncrement(1);
        softDropSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            softDropLabel.setText(String.format("%.0f", newVal.doubleValue()));
        });
        grid.add(new javafx.scene.control.Label("소프트 드롭 속도:"), 0, row);
        grid.add(softDropSlider, 1, row);
        grid.add(softDropLabel, 2, row++);
        
        // 락 딜레이 설정
        javafx.scene.control.Label lockDelayLabel = new javafx.scene.control.Label(
            String.format("%dms", currentConfig.getLockDelay()));
        javafx.scene.control.Slider lockDelaySlider = new javafx.scene.control.Slider(100, 1000, currentConfig.getLockDelay());
        lockDelaySlider.setShowTickMarks(true);
        lockDelaySlider.setShowTickLabels(true);
        lockDelaySlider.setMajorTickUnit(100);
        lockDelaySlider.setBlockIncrement(50);
        lockDelaySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lockDelayLabel.setText(String.format("%dms", newVal.intValue()));
        });
        grid.add(new javafx.scene.control.Label("락 딜레이:"), 0, row);
        grid.add(lockDelaySlider, 1, row);
        grid.add(lockDelayLabel, 2, row++);
        
        dialog.getDialogPane().setContent(grid);
        
        // 결과 변환기
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == applyButtonType) {
                return GameModeConfig.builder()
                    .gameplayType(gameplayType)
                    .srsEnabled(srsCheckBox.isSelected())
                    .rotation180Enabled(rotation180CheckBox.isSelected())
                    .hardDropEnabled(hardDropCheckBox.isSelected())
                    .holdEnabled(holdCheckBox.isSelected())
                    .ghostPieceEnabled(ghostCheckBox.isSelected())
                    .dropSpeedMultiplier(dropSpeedSlider.getValue())
                    .softDropSpeed(softDropSlider.getValue())
                    .lockDelay((int) lockDelaySlider.getValue())
                    .build();
            }
            return null;
        });
        
        // 다이얼로그 표시 및 결과 처리
        dialog.showAndWait().ifPresent(config -> {
            // 커스텀 설정을 SettingsService에 저장
            settingsService.saveCustomGameModeConfig(gameplayType, config);
            settingsService.saveGameModeSettings(playType, gameplayType, config.isSrsEnabled());
            System.out.println("✅ " + modeName + " mode custom settings saved");
            System.out.println("   - SRS: " + config.isSrsEnabled());
            System.out.println("   - 180° Rotation: " + config.isRotation180Enabled());
            System.out.println("   - Hard Drop: " + config.isHardDropEnabled());
            System.out.println("   - Hold: " + config.isHoldEnabled());
            System.out.println("   - Ghost: " + config.isGhostPieceEnabled());
            System.out.println("   - Drop Speed: " + config.getDropSpeedMultiplier() + "x");
            System.out.println("   - Soft Drop: " + config.getSoftDropSpeed());
            System.out.println("   - Lock Delay: " + config.getLockDelay() + "ms");
        });
    }
    
    /**
     * 게임 모드 설정을 적용하여 게임을 시작합니다
     * 
     * @param event 버튼 클릭 이벤트
     * @param config 게임 모드 설정
     * @param modeName 모드 이름 (로그용)
     */
    /**
     * 게임 시작 (PlayType 기본값: LOCAL_SINGLE)
     */
    private void startGameWithConfig(ActionEvent event, GameModeConfig config, String modeName) {
        startGameWithConfig(event, config, modeName, PlayType.LOCAL_SINGLE);
    }

    /**
     * 게임 시작 (PlayType 지정 가능)
     */
    private void startGameWithConfig(ActionEvent event, GameModeConfig config, String modeName, PlayType playType) {
        try {
            // 1단계: 현재 Stage 가져오기 (rootPane을 통해 안전하게 가져오기)
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if (stage == null) {
                System.err.println("❌ Cannot get Stage from rootPane");
                return;
            }

            // 2단계: game-view.fxml 로드
            FXMLLoader loader = new FXMLLoader(
                TetrisApplication.class.getResource("/view/game-view.fxml")
            );

            // 3단계: Controller Factory 설정 (Spring DI)
            ApplicationContext context = ApplicationContextProvider.getApplicationContext();
            loader.setControllerFactory(context::getBean);

            // 4단계: FXML 로드
            Parent gameRoot = loader.load();

            // 5단계: GameController에 설정 전달 (PlayType 포함)
            GameController controller = loader.getController();
            controller.setGameModeConfig(config, playType);
            
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
            
            // 화면 크기 CSS 클래스 적용
            settingsService.applyScreenSizeClass();
            
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

            // 7단계: 멀티플레이 모드인 경우 매칭 시작
            if (playType == PlayType.ONLINE_MULTI) {
                System.out.println("🔍 Initiating multiplayer matching...");
                // 서버 기본 URL (HTTP)
                String serverBaseUrl = "http://localhost:8080";
                // JWT 토큰 획득
                String jwtToken = authService != null ? authService.getCurrentToken() : "";

                controller.startMultiplayerMatching(serverBaseUrl, jwtToken);
            }

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
     * 오버레이를 표시하여 배경을 어둡게 하고 다른 버튼들의 상호작용을 차단합니다.
     */
    private void showOverlay() {
        if (overlayPane != null) {
            overlayPane.setVisible(true);
            overlayPane.setManaged(false); // managed=false로 레이아웃에 영향 없게
            overlayPane.toFront(); // 오버레이를 최상위로
            
            // 오버레이 클릭 시 아무 동작 안하도록 (클릭 차단)
            overlayPane.setOnMouseClicked(e -> {
                e.consume();
                System.out.println("🚫 Overlay clicked - interaction blocked");
            });
            
            System.out.println("🔒 Overlay activated - background dimmed and interaction blocked");
        }
    }
    
    /**
     * 오버레이를 숨기고 모든 버튼들의 상호작용을 활성화합니다.
     */
    private void hideOverlay() {
        if (overlayPane != null) {
            overlayPane.setVisible(false);
            overlayPane.setManaged(false);
            System.out.println("🔓 Overlay deactivated - all buttons enabled");
        }
    }
    
    /**
     * 메인 메뉴 버튼들의 상호작용을 비활성화합니다.
     */
    private void disableMainMenuButtons() {
        singlePlayButton.setDisable(true);
        battleModeButton.setDisable(true);
        p2pModeButton.setDisable(true);
        multiplayerButton.setDisable(true);
        scoreButton.setDisable(true);
        endButton.setDisable(true);
        settingsButton.setDisable(true);
    }
    
    /**
     * 메인 메뉴 버튼들의 상호작용을 활성화합니다.
     */
    private void enableMainMenuButtons() {
        singlePlayButton.setDisable(false);
        battleModeButton.setDisable(false);
        p2pModeButton.setDisable(false);
        multiplayerButton.setDisable(false);
        scoreButton.setDisable(false);
        endButton.setDisable(false);
        settingsButton.setDisable(false);
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
