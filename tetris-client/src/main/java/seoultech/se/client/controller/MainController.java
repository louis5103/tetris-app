package seoultech.se.client.controller;

import java.io.IOException;
import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javafx.animation.ScaleTransition;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.text.Text;
import javafx.scene.layout.HBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;
import seoultech.se.backend.service.GameService;
import seoultech.se.client.TetrisApplication;
import seoultech.se.client.config.ApplicationContextProvider;
import seoultech.se.client.service.NavigationService;
import seoultech.se.client.service.SettingsService;
import seoultech.se.core.GameState;
import seoultech.se.core.config.GameplayType;


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
    private Label titleLabel;
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
    private MediaPlayer mediaPlayer;
    
    @Autowired
    private SettingsService settingsService;

    @Autowired(required = false)
    private seoultech.se.client.service.AuthService authService;

    @Autowired(required = false)
    private seoultech.se.client.service.MultiplayerMatchingService matchingService;

    @Autowired
    private seoultech.se.client.controller.P2PModeSelectionController p2pController;
    
    @Autowired(required = false)
    private seoultech.se.backend.network.P2PService p2pService;

    /**
     * UI 초기화 메서드
     * FXML 파일이 로드된 후 자동으로 호출됩니다
     */
    public void initialize() {
        super.initialize();
        System.out.println("✅ MainController initialized with Spring DI");
        System.out.println("📊 Service Status: " + gameService.getStatus());

        // 타이틀 애니메이션 효과 (Scale Pulse)
        if (titleLabel != null) {
            ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(1), titleLabel);
            scaleTransition.setFromX(1.0);
            scaleTransition.setFromY(1.0);
            scaleTransition.setToX(1.2);
            scaleTransition.setToY(1.2);
            scaleTransition.setCycleCount(ScaleTransition.INDEFINITE);
            scaleTransition.setAutoReverse(true);
            scaleTransition.play();
            System.out.println("✨ Title animation started");
        }

        // 배경 음악 재생
        try {
            if (mediaPlayer == null) {
                URL resource = getClass().getResource("/Tetris - Bradinsky.mp3");
                if (resource != null) {
                    Media media = seoultech.se.client.util.MediaUtils.loadMedia(resource);
                    mediaPlayer = new MediaPlayer(media);
                    mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                } else {
                    System.err.println("❌ Could not find music file: /Tetris - Bradinsky.mp3");
                }
            }
            
            if (mediaPlayer != null) {
                mediaPlayer.play();
                System.out.println("🎵 Background music started");
            }
        } catch (Exception e) {
            System.err.println("❌ Error playing music: " + e.getMessage());
            e.printStackTrace();
        }

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
     * 배경 음악 중지
     */
    public void stopBackgroundMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            System.out.println("🔇 Background music stopped");
        }
    }

    /**
     * 설정 버튼 액션 - 기존 설정 화면으로 이동
     * (키 매핑, 커스터마이징 등)
     */
    public void handleSettingsButtonAction(ActionEvent event) throws IOException {
        System.out.println("⚙️ Settings button clicked");
        stopBackgroundMusic();
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
        
        // 게임 시작 (싱글플레이) - GameController에서 설정 로드
        startGameWithGameplayType(event, GameplayType.CLASSIC, false, "CLASSIC");
    }
    
    /**
     * ARCADE 모드 버튼 액션
     * 아케이드 모드 (로컬 싱글, 빠른 속도)로 게임 시작
     */
    public void handleArcadeModeAction(ActionEvent event) {
        System.out.println("🕹️ ARCADE mode selected");
        
        // 게임 시작 (싱글플레이) - GameController에서 설정 로드
        startGameWithGameplayType(event, GameplayType.ARCADE, false, "ARCADE");

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
     * Battle Classic 모드 버튼 액션
     */
    public void handleBattleClassicModeAction(ActionEvent event) {
        System.out.println("⚔️ Battle Classic mode selected");
        startLocalBattle(GameplayType.CLASSIC, "Local Battle - Classic");
    }

    /**
     * Battle Arcade 모드 버튼 액션
     */
    public void handleBattleArcadeModeAction(ActionEvent event) {
        System.out.println("⚔️ Battle Arcade mode selected");
        startLocalBattle(GameplayType.ARCADE, "Local Battle - Arcade");
    }

    /**
     * Battle Time Attack 모드 버튼 액션
     */
    public void handleBattleTimeAttackModeAction(ActionEvent event) {
        System.out.println("⏱️ Battle Time Attack mode selected");
        startLocalBattle(GameplayType.TIME_ATTACK, "Local Battle - Time Attack");
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
        // 바로 P2P 팝업 표시 (Server/Client 선택 단계 생략)
        showP2PPopup(true);
    }

    /**
     * P2P Server 버튼 액션
     * 호스트 모드로 P2P 대기 화면 진입
     */
    public void handleP2pServerAction(ActionEvent event) {
        System.out.println("🖥️ P2P Server (Host) mode selected");
        showP2PPopup(true);
    }

    /**
     * P2P Client 버튼 액션
     * 클라이언트 모드로 P2P 연결 화면 진입
     */
    public void handleP2pClientAction(ActionEvent event) {
        System.out.println("💻 P2P Client (Guest) mode selected");
        showP2PPopup(false);
    }

    private void showP2PPopup(boolean isHostMode) {
        try {
            seoultech.se.client.ui.P2PModeSelectionPopup popup = new seoultech.se.client.ui.P2PModeSelectionPopup();
            
            // 호스트 정보를 팝업에 표시
            if (p2pService != null) {
                String myIp = seoultech.se.client.util.NetworkUtils.getLocalIpAddress();
                int myPort = p2pService.getLocalPort();
                popup.setHostInfo(myIp, myPort);
            }
            
            Stage p2pStage = new Stage();
            Scene scene = new Scene(popup);
            p2pStage.setScene(scene);
            p2pStage.setTitle("P2P Setup");
            p2pStage.setResizable(false);
            
            popup.setOnHost(() -> {
                p2pStage.close();
                seoultech.se.core.model.enumType.Difficulty difficulty = popup.getSelectedDifficulty();
                seoultech.se.core.config.GameplayType gameplayType = popup.getSelectedGameplayType();
                
                if (popup.isRelayMode()) {
                    // 릴레이 모드 - Host
                    handleRelayMode(popup, true, true, difficulty, gameplayType);
                } else {
                    // 직접 P2P 모드
                    if (p2pController != null) {
                        p2pController.handleHostGame();
                        transitionToP2PGame(true, difficulty, gameplayType);
                    }
                }
            });
            
            popup.setOnConnect(() -> {
                p2pStage.close();
                seoultech.se.core.model.enumType.Difficulty difficulty = popup.getSelectedDifficulty();
                seoultech.se.core.config.GameplayType gameplayType = popup.getSelectedGameplayType();
                
                if (popup.isRelayMode()) {
                    // 릴레이 모드 - Guest
                    handleRelayMode(popup, false, false, difficulty, gameplayType);
                } else {
                    // 직접 P2P 모드
                    String ip = popup.getIpAddress();
                    String port = popup.getPort();
                    if (p2pController != null) {
                        p2pController.connectToGame(ip, port);
                        transitionToP2PGame(false, difficulty, gameplayType);
                    }
                }
            });
            
            popup.setOnCancel(() -> {
                p2pStage.close();
            });
            
            p2pStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            // showErrorAlert("오류", "P2P 모드 실행 중 오류 발생: " + e.getMessage());
        }
    }
    
    private void handleRelayMode(seoultech.se.client.ui.P2PModeSelectionPopup popup, 
                                  boolean isHostMode, boolean isHost, seoultech.se.core.model.enumType.Difficulty difficulty, seoultech.se.core.config.GameplayType gameplayType) {
        String relayServerIp = popup.getRelayServerIp();
        String relayServerPort = popup.getRelayServerPort();
        String sessionId = popup.getSessionId();
        
        if (relayServerIp.isEmpty() || relayServerPort.isEmpty() || sessionId.isEmpty()) {
            System.err.println("❌ [Relay] Missing relay server configuration");
            return;
        }
        
        try {
            int relayPort = Integer.parseInt(relayServerPort);
            String playerId = isHost ? "player-host" : "player-guest";
            String role = isHost ? "HOST" : "GUEST";
            
            System.out.println("🔄 [Relay] Connecting via relay server as " + role + ":");
            System.out.println("   └ Server: " + relayServerIp + ":" + relayPort);
            System.out.println("   └ Session: " + sessionId);
            System.out.println("   └ Player ID: " + playerId);
            
            // 릴레이 서버를 통한 연결
            if (p2pService != null) {
                p2pService.connectViaRelay(relayServerIp, relayPort, sessionId, playerId);
            }
            
            // 게임 화면으로 전환
            transitionToP2PGame(isHost, difficulty, gameplayType);
            
        } catch (NumberFormatException e) {
            System.err.println("❌ [Relay] Invalid port number: " + relayServerPort);
        }
    }

    private void transitionToP2PGame(boolean isHost, seoultech.se.core.model.enumType.Difficulty difficulty, seoultech.se.core.config.GameplayType gameplayType) {
        // 릴레이 모드인 경우 매칭 대기 팝업 표시
        if (p2pService != null && p2pService.isRelayMode()) {
            showMatchingWaitPopupAndTransition(isHost, difficulty, gameplayType);
        } else {
            // 직접 P2P 모드는 바로 게임 화면으로 전환
            performGameTransition(isHost, null, difficulty, gameplayType);
        }
    }

    private void showMatchingWaitPopupAndTransition(boolean isHost, seoultech.se.core.model.enumType.Difficulty difficulty, seoultech.se.core.config.GameplayType gameplayType) {
        try {
            seoultech.se.client.ui.MatchingWaitPopup matchingPopup = new seoultech.se.client.ui.MatchingWaitPopup();

            // 세션 정보 설정
            String sessionId = "relay-session"; // 실제 세션 ID로 교체 필요
            matchingPopup.setSessionInfo(sessionId, isHost ? "HOST" : "GUEST");

            Stage popupStage = new Stage();
            Scene popupScene = new Scene(matchingPopup);
            popupStage.setScene(popupScene);
            popupStage.setTitle("Waiting for Player...");
            popupStage.setResizable(false);
            popupStage.initOwner(rootPane.getScene().getWindow());
            popupStage.initModality(javafx.stage.Modality.WINDOW_MODAL);

            // 취소 버튼 처리
            matchingPopup.setOnCancel(() -> {
                popupStage.close();
                if (p2pService != null) {
                    p2pService.close();
                }
                System.out.println("❌ [MainController] Matching cancelled by user");
            });

            popupStage.show();

            // NetworkGameService 설정 및 게임 시작 준비
            ApplicationContext context = ApplicationContextProvider.getApplicationContext();
            seoultech.se.client.service.NetworkGameService netService = context.getBean(seoultech.se.client.service.NetworkGameService.class);

            // 매칭 완료 콜백 설정
            netService.setOnPlayerMatched(unused -> {
                if (!matchingPopup.isCancelled()) {
                    matchingPopup.onPlayerMatched();
                }
            });

            // 카운트다운 업데이트 콜백 설정
            netService.setOnCountdownUpdate(count -> {
                if (!matchingPopup.isCancelled()) {
                    matchingPopup.updateCountdown(count);

                    // 카운트다운 0이면 팝업 닫고 게임 화면으로 전환
                    if (count == 0) {
                        Platform.runLater(() -> {
                            try {
                                Thread.sleep(500); // "START!" 메시지 표시 시간
                                popupStage.close();
                                performGameTransition(isHost, netService, difficulty, gameplayType);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            });



            // P2P 게임 시작 (대기 상태) - 임시 콜백 설정
            netService.startP2PGame(isHost,
                myState -> {
                    System.out.println("🎮 [Matching] My state callback (waiting stage)");
                },
                opponentState -> {
                    System.out.println("👥 [Matching] Opponent state callback (waiting stage)");
                },
                unused -> {
                    System.out.println("✅ [Matching] Game start callback (waiting stage)");
                },
                isWinner -> {
                    System.out.println("🏁 [Matching] Game result callback (waiting stage)");
                }
            );

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ [MainController] Failed to show matching popup: " + e.getMessage());
        }
    }

    private void performGameTransition(boolean isHost, seoultech.se.client.service.NetworkGameService existingNetService, 
                                       seoultech.se.core.model.enumType.Difficulty difficulty, seoultech.se.core.config.GameplayType gameplayType) {
        try {
            stopBackgroundMusic();
            ApplicationContext context = ApplicationContextProvider.getApplicationContext();

            // 먼저 컨트롤러를 생성
            MultiGameController gameViewController = context.getBean(MultiGameController.class);

            FXMLLoader loader = new FXMLLoader(
                TetrisApplication.class.getResource("/view/game-view.fxml")
            );

            // 생성된 컨트롤러를 설정
            loader.setController(gameViewController);
            Parent gameRoot = loader.load();

            // 선택된 난이도와 게임 모드로 설정 생성
            seoultech.se.core.config.GameModeConfig config = seoultech.se.core.config.GameModeConfig.createForP2P(gameplayType, difficulty);
                
            gameViewController.initGame(config);

            seoultech.se.client.service.NetworkGameService netService = existingNetService != null ?
                existingNetService : context.getBean(seoultech.se.client.service.NetworkGameService.class);

            // P2P 모드 초기화
            gameViewController.initP2PMode(netService, isHost);

            // 콜백 설정 (기존 NetworkGameService가 있든 없든 콜백 설정 필요)
            if (existingNetService != null) {
                // 기존 서비스의 콜백만 재설정
                netService.updateCallbacks(
                    myState -> {
                        System.out.println("🎮 [MainController " + (isHost ? "Host" : "Guest") + "] My state callback triggered!");
                        if (myState == null) {
                            System.err.println("❌ [MainController] myState is NULL!");
                            return;
                        }

                        // NetworkGameService가 이미 JavaFX 스레드에서 호출하므로 직접 실행
                        GameState oldState = gameViewController.getBoardController().getGameState();
                        gameViewController.getBoardController().setGameState(myState);
                        gameViewController.updateUI(oldState, myState);
                    },
                    opponentState -> {
                        if (opponentState == null) {
                            System.err.println("❌ [MainController] opponentState is NULL!");
                            return;
                        }
                        gameViewController.getOpponentBoardView().update(opponentState);
                    },
                    unused -> {
                        System.out.println("✅ [MainController] P2P Game Started callback!");
                        gameViewController.startGame();
                    },
                    isWinner -> {
                         System.out.println("🏁 [MainController] P2P Game Result: " + isWinner);
                         gameViewController.handleP2PGameResult(isWinner);
                    }
                );
            } else {
                // 새로운 서비스 시작
                netService.startP2PGame(isHost,
                    myState -> {
                        System.out.println("🎮 [MainController " + (isHost ? "Host" : "Guest") + "] My state callback triggered!");
                        if (myState == null) {
                            System.err.println("❌ [MainController] myState is NULL!");
                            return;
                        }

                        // NetworkGameService가 이미 JavaFX 스레드에서 호출하므로 직접 실행
                        GameState oldState = gameViewController.getBoardController().getGameState();
                        gameViewController.getBoardController().setGameState(myState);
                        gameViewController.updateUI(oldState, myState);
                    },
                    opponentState -> {
                        if (opponentState == null) {
                            System.err.println("❌ [MainController] opponentState is NULL!");
                            return;
                        }
                        gameViewController.getOpponentBoardView().update(opponentState);
                    },
                    unused -> {
                        System.out.println("✅ [MainController] P2P Game Started callback!");
                        gameViewController.startGame();
                    },
                    isWinner -> {
                         System.out.println("🏁 [MainController] P2P Game Result: " + isWinner);
                         gameViewController.handleP2PGameResult(isWinner);
                    }
                );
            }

            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene gameScene = new Scene(gameRoot);
            stage.setScene(gameScene);
            stage.setTitle("Tetris - P2P " + (p2pService.isRelayMode() ? "Relay" : "Direct") + " (" + (isHost ? "HOST" : "GUEST") + ")");

            settingsService.applyScreenSizeClass();
            stage.sizeToScene();

            // Scene이 완전히 렌더링된 후 포커스 재요청
            Platform.runLater(() -> {
                Platform.runLater(() -> { // 이중 runLater로 확실한 지연
                    System.out.println("🎯 [MainController] Requesting focus after scene loaded...");
                    gameViewController.getBoardGridPane().requestFocus();

                    boolean hasFocus = gameViewController.getBoardGridPane().isFocused();
                    System.out.println("🎯 [MainController] Final focus check: " + hasFocus);
                });
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
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

        // 매칭 서비스 및 인증 서비스 확인
        if (matchingService == null) {
            System.err.println("❌ MultiplayerMatchingService not available");
            showErrorAlert("멀티플레이 오류", "매칭 서비스를 사용할 수 없습니다.\n서버 모듈이 포함되어 있는지 확인하세요.");
            return;
        }

        if (authService == null) {
            System.err.println("❌ AuthService not available");
            showErrorAlert("인증 오류", "로그인 정보를 확인할 수 없습니다.\n먼저 로그인해주세요.");
            return;
        }

        // JWT 토큰 가져오기
        String jwtToken = authService.getCurrentToken();
        if (jwtToken == null || jwtToken.isEmpty()) {
            System.err.println("❌ No JWT token available");
            showErrorAlert("인증 오류", "로그인이 필요합니다.\n먼저 로그인해주세요.");
            return;
        }

        // 서버 URL 가져오기
        String serverBaseUrl = settingsService.getServerBaseUrl();
        System.out.println("📡 Connecting to server: " + serverBaseUrl);

        try {
            // 모드 선택 팝업 표시
            FXMLLoader loader = new FXMLLoader(
                TetrisApplication.class.getResource("/view/multiplayer-mode-selection.fxml")
            );

            // Controller Factory 설정 (Spring DI)
            ApplicationContext context = ApplicationContextProvider.getApplicationContext();
            loader.setControllerFactory(context::getBean);

            // FXML 로드
            Parent popupRoot = loader.load();

            // MultiplayerModeSelectionController에 연결 정보 전달
            MultiplayerModeSelectionController popupController = loader.getController();
            popupController.setConnectionInfo(serverBaseUrl, jwtToken);

            // 새 Stage에서 팝업 표시
            Stage popupStage = new Stage();
            popupStage.setScene(new Scene(popupRoot));
            popupStage.setTitle("멀티플레이 설정");
            popupStage.setResizable(false);
            popupStage.initOwner(rootPane.getScene().getWindow());
            popupStage.initModality(javafx.stage.Modality.WINDOW_MODAL);

            // 화면 크기 CSS 클래스 적용
            settingsService.applyScreenSizeClass();
            popupStage.sizeToScene();

            popupStage.showAndWait();

            System.out.println("✅ Multiplayer mode selection popup shown");

        } catch (IOException e) {
            System.err.println("❌ Failed to load multiplayer-mode-selection.fxml");
            e.printStackTrace();
            showErrorAlert("화면 로딩 오류", "모드 선택 화면을 불러올 수 없습니다: " + e.getMessage());
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

    private void startLocalBattle(GameplayType gameplayType, String modeName) {
        try {
            stopBackgroundMusic();
            Stage stage = (Stage) rootPane.getScene().getWindow();
            if (stage == null) {
                System.err.println("❌ Cannot get Stage from rootPane");
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                TetrisApplication.class.getResource("/view/local-battle-view.fxml")
            );

            ApplicationContext context = ApplicationContextProvider.getApplicationContext();
            
            LocalBattleController controller = context.getBean(LocalBattleController.class);
            loader.setController(controller);

            Parent gameRoot = loader.load();

            seoultech.se.client.service.GameModeConfigFactory configFactory = context.getBean(seoultech.se.client.service.GameModeConfigFactory.class);
            seoultech.se.core.model.enumType.Difficulty difficulty = settingsService.getCurrentDifficulty();
            seoultech.se.core.config.GameModeConfig config = configFactory.create(gameplayType, difficulty);
            
            controller.initGame(config);
            controller.startGame();
            
            Scene gameScene = new Scene(gameRoot);
            stage.setScene(gameScene);
            stage.setTitle("Tetris - " + modeName);
            stage.setResizable(false);
            
            settingsService.applyScreenSizeClass();
            stage.sizeToScene();

            System.out.println("✅ " + modeName + " mode started successfully");

        } catch (IOException e) {
            System.err.println("❌ Failed to load local-battle-view.fxml");
            System.err.println("   Error: " + e.getMessage());
            e.printStackTrace();
        }
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
    /**
     * 게임 시작
     * 
     * @param event 액션 이벤트
     * @param config 게임 모드 설정
     * @param modeName 모드 이름 (MULTIPLAYER인 경우 멀티플레이로 판단)
     */
    private void startGameWithGameplayType(ActionEvent event, GameplayType gameplayType, boolean isMultiplayer, String modeName) {
        try {
            stopBackgroundMusic();
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

            // 3단계: 컨트롤러 설정 (Spring DI)
            ApplicationContext context = ApplicationContextProvider.getApplicationContext();
            
            // SingleGameController 빈 가져오기
            SingleGameController controller = context.getBean(SingleGameController.class);
            loader.setController(controller); // 동적 컨트롤러 설정

            // 4단계: FXML 로드
            Parent gameRoot = loader.load();

            // 5단계: 게임 모드 설정 및 초기화
            seoultech.se.client.service.GameModeConfigFactory configFactory = context.getBean(seoultech.se.client.service.GameModeConfigFactory.class);
            seoultech.se.core.model.enumType.Difficulty difficulty = settingsService.getCurrentDifficulty();
            seoultech.se.core.config.GameModeConfig config = configFactory.create(gameplayType, difficulty);
            
            controller.initGame(config);
            controller.startGame();
            
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
        stopBackgroundMusic();
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
