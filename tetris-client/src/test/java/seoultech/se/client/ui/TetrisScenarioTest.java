package seoultech.se.client.ui;

import static org.testfx.api.FxAssert.verifyThat;
import static org.testfx.matcher.base.NodeMatchers.isVisible;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationTest;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Labeled;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import seoultech.se.client.TetrisApplication;
import seoultech.se.client.config.ApplicationContextProvider;
import seoultech.se.client.controller.GameController;

/**
 * 🧪 JavaFX + TestFX E2E 시나리오 테스트
 * 
 * 사용자의 실제 행동 흐름(Scenario)을 검증하는 통합 테스트입니다.
 * 단순한 버튼 클릭이 아닌, 화면 전환과 상태 변화를 포함한 완전한 사용자 경험을 테스트합니다.
 * 
 * 테스트 시나리오:
 * 1. 설정 화면 왕복 (메인 → 설정 → 메인)
 * 2. 스코어보드 왕복 (메인 → 스코어 → 메인)
 * 3. 게임 진입 및 일시정지 후 나가기 (메인 → 게임 → 일시정지 → 메인)
 * 4. 아이템 모드 게임 오버 및 복귀 (메인 → 아케이드 → 게임 오버 → 메인)
 * 
 * 기술 스택:
 * - JUnit 5
 * - TestFX (ApplicationTest)
 * - Spring Boot (ApplicationContextProvider를 통한 빈 접근)
 * - Reflection (게임 오버 강제 트리거)
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TetrisScenarioTest extends ApplicationTest {

    private static ConfigurableApplicationContext springContext;
    private Stage stage;

    /**
     * 🚀 Spring Boot 컨텍스트 초기화 (모든 테스트 전에 한 번 실행)
     */
    @BeforeAll
    public static void setUpClass() {
        // Headless 모드 설정 (CI/CD 환경 대응)
        System.setProperty("java.awt.headless", "false");
        System.setProperty("testfx.robot", "glass");
        System.setProperty("testfx.headless", "true");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        
        // Spring Boot 컨텍스트 시작
        System.setProperty("spring.main.web-application-type", "none");
        SpringApplication app = new SpringApplication(TetrisApplication.class);
        app.setAdditionalProfiles("desktop-client");
        springContext = app.run();
        
        System.out.println("✅ Spring Boot context initialized for tests");
    }

    /**
     * 🎨 JavaFX 애플리케이션 시작
     */
    @Override
    public void start(Stage stage) throws Exception {
        this.stage = stage;
        
        // Spring Context에서 SettingsService 가져오기
        var settingsService = springContext.getBean(seoultech.se.client.service.SettingsService.class);
        settingsService.setPrimaryStage(stage);

        // FXML 로더 설정
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
            getClass().getResource("/view/main-view.fxml")
        );
        loader.setControllerFactory(springContext::getBean);
        javafx.scene.Parent root = loader.load();
        
        // Scene 생성 및 Stage 설정
        javafx.scene.Scene scene = new javafx.scene.Scene(
            root, 
            settingsService.getStageWidth(), 
            settingsService.getStageHeight()
        );
        
        stage.setTitle("Tetris Test");
        stage.setScene(scene);
        stage.setResizable(false);
        settingsService.applyScreenSizeClass();
        stage.show();
        
        System.out.println("✅ JavaFX application started for test");
    }

    /**
     * 각 테스트 전에 실행 - 메인 화면으로 리셋
     */
    @BeforeEach
    public void setUp() throws Exception {
        // 혹시 모를 이전 테스트의 영향을 제거하기 위해 메인 화면 확인
        waitForFxEvents();
    }

    /**
     * 각 테스트 후 정리
     */
    @AfterEach
    public void tearDown() {
        // UI가 안정화될 시간을 줌
        sleep(300);
    }

    /**
     * 🛑 모든 테스트 종료 후 정리
     */
    @AfterAll
    public static void tearDownClass() {
        if (springContext != null) {
            springContext.close();
            System.out.println("✅ Spring Boot context closed");
        }
        Platform.exit();
    }

    // ==================== 테스트 시나리오 ====================

    /**
     * 시나리오 1: 설정 화면 왕복 테스트
     * 
     * 흐름:
     * 1. 메인 화면에서 '설정' 버튼(#settingsButton) 클릭
     * 2. 설정 화면 진입 확인 (#soundSlider 보임 여부)
     * 3. '뒤로가기' 버튼(#backButton) 클릭
     * 4. 메인 화면 복귀 확인 (#titleLabel 보임 여부)
     */
    @Test
    @Order(1)
    @DisplayName("시나리오 1: 설정 화면 왕복 테스트")
    public void testSettingsNavigation() {
        System.out.println("\n🧪 Starting Test 1: Settings Navigation");
        
        // 1. 메인 화면 확인
        verifyThat("#titleLabel", isVisible(), 
                   info -> info.append("메인 화면의 타이틀이 보여야 합니다"));
        System.out.println("   ✓ 메인 화면 확인 완료");
        
        // 2. 설정 버튼 클릭
        clickOn("#settingsButton");
        waitForFxEvents();
        sleep(500); // 화면 전환 대기
        System.out.println("   ✓ 설정 버튼 클릭 완료");
        
        // 3. 설정 화면 진입 확인
        verifyThat("#soundSlider", isVisible(),
                   info -> info.append("설정 화면의 사운드 슬라이더가 보여야 합니다"));
        System.out.println("   ✓ 설정 화면 진입 확인");
        
        // 4. 뒤로가기 버튼 클릭
        clickOn("#backButton");
        waitForFxEvents();
        sleep(500); // 화면 전환 대기
        System.out.println("   ✓ 뒤로가기 버튼 클릭 완료");
        
        // 5. 메인 화면 복귀 확인
        verifyThat("#titleLabel", isVisible(),
                   info -> info.append("메인 화면으로 복귀해야 합니다"));
        System.out.println("   ✅ 설정 화면 왕복 테스트 성공!");
    }

    /**
     * 시나리오 2: 스코어보드 왕복 테스트
     * 
     * 흐름:
     * 1. 메인 화면에서 '스코어' 버튼(#scoreButton) 클릭
     * 2. 스코어보드 진입 확인 (#scoreBoardContainer 보임 여부)
     * 3. '뒤로가기' 버튼(#backButton) 클릭
     * 4. 메인 화면 복귀 확인
     */
    @Test
    @Order(2)
    @DisplayName("시나리오 2: 스코어보드 왕복 테스트")
    public void testScoreBoardNavigation() {
        System.out.println("\n🧪 Starting Test 2: ScoreBoard Navigation");
        
        // 1. 메인 화면 확인
        verifyThat("#titleLabel", isVisible(),
                   info -> info.append("메인 화면의 타이틀이 보여야 합니다"));
        System.out.println("   ✓ 메인 화면 확인 완료");
        
        // 2. 스코어 버튼 클릭
        clickOn("#scoreButton");
        waitForFxEvents();
        sleep(1000); // 화면 전환 대기 (NavigationService의 Scene 교체 + ScoreBoard 초기화 시간)
        System.out.println("   ✓ 스코어 버튼 클릭 완료");
        
        // 3. 스코어보드 화면 진입 확인
        verifyThat("#scoreBoardContainer", isVisible(),
                   info -> info.append("스코어보드 컨테이너가 보여야 합니다"));
        System.out.println("   ✓ 스코어보드 화면 진입 확인");
        
        // 4. 뒤로가기 버튼 클릭
        clickOn("#backButton");
        waitForFxEvents();
        sleep(1000); // 화면 전환 대기
        System.out.println("   ✓ 뒤로가기 버튼 클릭 완료");
        
        // 5. 메인 화면 복귀 확인
        verifyThat("#titleLabel", isVisible(),
                   info -> info.append("메인 화면으로 복귀해야 합니다"));
        System.out.println("   ✅ 스코어보드 왕복 테스트 성공!");
    }

    /**
     * 시나리오 3: 게임 진입 및 일시정지 후 나가기
     * 
     * 흐름:
     * 1. 메인 → 싱글 플레이(#singlePlayButton) → 클래식 모드(#classicButton) 진입
     * 2. 게임 화면 로드 확인 (#scoreLabel 보임 여부)
     * 3. 키보드 'P' 눌러서 일시정지 팝업(#pauseOverlay) 띄우기
     * 4. 팝업 내 'Quit' 버튼(텍스트가 "Quit"인 버튼) 클릭
     * 5. 메인 화면 복귀 확인
     */
    @Test
    @Order(3)
    @DisplayName("시나리오 3: 게임 진입 및 일시정지 후 나가기")
    public void testGamePauseAndQuit() {
        System.out.println("\n🧪 Starting Test 3: Game Pause and Quit");
        
        // 1. 메인 화면 확인
        verifyThat("#titleLabel", isVisible(),
                   info -> info.append("메인 화면의 타이틀이 보여야 합니다"));
        System.out.println("   ✓ 메인 화면 확인 완료");
        
        // 2. 싱글 플레이 버튼 클릭
        clickOn("#singlePlayButton");
        waitForFxEvents();
        sleep(500);
        System.out.println("   ✓ 싱글 플레이 버튼 클릭 완료");
        
        // 3. 클래식 모드 버튼 클릭
        clickOn("#classicButton");
        waitForFxEvents();
        sleep(1000); // 게임 화면 로딩 대기
        System.out.println("   ✓ 아케이드 모드 버튼 클릭 완료");
        
        // 4. 게임 화면 로드 확인
        verifyThat("#scoreLabel", isVisible(),
                   info -> info.append("게임 화면의 점수 레이블이 보여야 합니다"));
        System.out.println("   ✓ 게임 화면 로드 확인");
        
        // 5. 'P' 키를 눌러 일시정지
        press(KeyCode.P).release(KeyCode.P);
        waitForFxEvents();
        sleep(500);
        System.out.println("   ✓ 일시정지 키 입력 완료");
        
        // 6. 일시정지 팝업 확인
        verifyThat("#pauseOverlay", isVisible(),
                   info -> info.append("일시정지 팝업이 보여야 합니다"));
        System.out.println("   ✓ 일시정지 팝업 확인");
        
        // 7. 'Quit' 버튼 클릭 (텍스트로 찾기)
        clickOn(findButtonByText("Quit"));
        waitForFxEvents();
        sleep(800); // 화면 전환 대기
        System.out.println("   ✓ Quit 버튼 클릭 완료");
        
        // 8. 메인 화면 복귀 확인
        verifyThat("#titleLabel", isVisible(),
                   info -> info.append("메인 화면으로 복귀해야 합니다"));
        System.out.println("   ✅ 게임 일시정지 및 종료 테스트 성공!");
    }

    /**
     * 시나리오 4: 아이템 모드 게임 오버 및 복귀
     * 
     * 흐름:
     * 1. 메인 → 싱글 플레이 → 아케이드 모드(#arcadeButton) 진입
     * 2. 게임 화면 및 아이템 인벤토리(#itemInventoryContainer) 확인
     * 3. ApplicationContextProvider로 GameController를 가져온 뒤,
     *    Reflection을 사용하여 processGameOver(1000L) 메서드를 강제 호출
     * 4. 게임 오버 팝업(#gameOverOverlay) 및 점수(#finalScoreLabel) 확인
     * 5. 팝업 내 'Main' 버튼(텍스트가 "Main"인 버튼) 클릭
     * 6. 메인 화면 복귀 확인
     */
    @Test
    @Order(4)
    @DisplayName("시나리오 4: 아이템 모드 게임 오버 및 복귀")
    public void testItemModeGameOver() throws Exception {
        System.out.println("\n🧪 Starting Test 4: Item Mode Game Over");
        
        // 1. 메인 화면 확인
        verifyThat("#titleLabel", isVisible(),
                   info -> info.append("메인 화면의 타이틀이 보여야 합니다"));
        System.out.println("   ✓ 메인 화면 확인 완료");
        
        // 2. 싱글 플레이 버튼 클릭
        clickOn("#singlePlayButton");
        waitForFxEvents();
        sleep(500);
        System.out.println("   ✓ 싱글 플레이 버튼 클릭 완료");
        
        // 3. 아케이드 모드 버튼 클릭
        clickOn("#arcadeButton");
        waitForFxEvents();
        sleep(1000); // 게임 화면 로딩 대기
        System.out.println("   ✓ 아케이드 모드 버튼 클릭 완료");
        
        // 4. 게임 화면 로드 확인
        verifyThat("#scoreLabel", isVisible(),
                   info -> info.append("게임 화면의 점수 레이블이 보여야 합니다"));
        System.out.println("   ✓ 게임 화면 로드 확인");
        
        // 5. 아이템 인벤토리 확인 (아케이드 모드 전용)
        verifyThat("#itemInventoryContainer", isVisible(),
                   info -> info.append("아이템 인벤토리가 보여야 합니다"));
        System.out.println("   ✓ 아이템 인벤토리 확인");
        
        // 6. GameController를 Spring Context에서 가져오기
        GameController gameController = ApplicationContextProvider
                .getApplicationContext()
                .getBean(GameController.class);
        System.out.println("   ✓ GameController 빈 획득 완료");
        
        // 7. Reflection을 사용하여 processGameOver 메서드 강제 호출
        // (private 메서드이므로 setAccessible(true) 필요)
        CountDownLatch latch = new CountDownLatch(1);
        
        interact(() -> {
            try {
                Method processGameOverMethod = GameController.class
                        .getDeclaredMethod("processGameOver", long.class);
                processGameOverMethod.setAccessible(true);
                processGameOverMethod.invoke(gameController, 1000L);
                System.out.println("   ✓ processGameOver(1000L) 호출 완료");
            } catch (Exception e) {
                System.err.println("   ✗ processGameOver 호출 실패: " + e.getMessage());
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });
        
        // 메서드 호출 완료 대기
        latch.await(5, TimeUnit.SECONDS);
        waitForFxEvents();
        sleep(800); // 게임 오버 팝업 애니메이션 대기
        
        // 8. 게임 오버 팝업 확인
        verifyThat("#gameOverOverlay", isVisible(),
                   info -> info.append("게임 오버 팝업이 보여야 합니다"));
        System.out.println("   ✓ 게임 오버 팝업 확인");
        
        // 9. 점수 레이블 확인
        verifyThat("#finalScoreLabel", isVisible(),
                   info -> info.append("최종 점수 레이블이 보여야 합니다"));
        System.out.println("   ✓ 최종 점수 레이블 확인");
        
        // 10. 'Main' 버튼 클릭 (텍스트로 찾기)
        clickOn(findButtonByText("Main"));
        waitForFxEvents();
        sleep(800); // 화면 전환 대기
        System.out.println("   ✓ Main 버튼 클릭 완료");
        
        // 11. 메인 화면 복귀 확인
        verifyThat("#titleLabel", isVisible(),
                   info -> info.append("메인 화면으로 복귀해야 합니다"));
        System.out.println("   ✅ 아이템 모드 게임 오버 테스트 성공!");
    }

    // ==================== 헬퍼 메서드 ====================

    /**
     * 텍스트로 버튼 찾기
     * 
     * TestFX의 기본 lookup으로는 텍스트 검색이 제한적이므로
     * 명시적으로 버튼의 텍스트를 확인하여 찾습니다.
     * 
     * @param text 버튼에 표시된 텍스트
     * @return 찾은 버튼 Node
     */
    private Node findButtonByText(String text) {
        return lookup(".button")
                .match(node -> {
                    if (node instanceof Labeled) {
                        return text.equals(((Labeled) node).getText());
                    }
                    return false;
                })
                .query();
    }

    /**
     * FX 이벤트가 모두 처리될 때까지 대기
     */
    private void waitForFxEvents() {
        try {
            FxRobot robot = new FxRobot();
            robot.interact(() -> {
                // Platform.runLater의 모든 작업이 완료될 때까지 대기
            });
        } catch (Exception e) {
            // 무시 (이미 FX 스레드에서 실행 중일 수 있음)
        }
    }
}
