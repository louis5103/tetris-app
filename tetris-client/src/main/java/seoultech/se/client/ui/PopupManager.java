package seoultech.se.client.ui;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import seoultech.se.backend.score.ScoreRankDto;
import seoultech.se.backend.score.ScoreRequestDto;
import seoultech.se.backend.score.ScoreService;

/**
 * 게임 내 팝업 오버레이를 관리하는 클래스
 * 
 * 이 클래스는 다음과 같은 팝업들을 표시하고 관리합니다:
 * - Pause 오버레이 (일시정지 팝업)
 * - Game Over 오버레이 (게임 종료 팝업)
 * 
 * GameController에서 팝업 관련 책임을 분리하여
 * 단일 책임 원칙(SRP)을 준수합니다.
 */
public class PopupManager {
    
    /**
     * 팝업에서 발생하는 액션을 처리하기 위한 콜백 인터페이스
     */
    public interface PopupActionCallback {
        /**
         * Resume 버튼 클릭 시 호출 (Pause 팝업)
         */
        void onResumeRequested();
        
        /**
         * Quit 버튼 클릭 시 호출 (Pause 팝업)
         */
        void onQuitRequested();
        
        /**
         * Main 버튼 클릭 시 호출 (Game Over 팝업)
         */
        void onMainMenuRequested();
        
        /**
         * Restart 버튼 클릭 시 호출 (Game Over 팝업)
         */
        void onRestartRequested();
    }
    
    // UI 요소들
    private final VBox pauseOverlay;
    private final VBox gameOverOverlay;

    // 게임 오버 화면의 UI 요소 (final 제거)
    private Label gameOverTitleLabel;
    private Label finalScoreLabel;
    private TextField usernameInput;
    private TableView<Map<String, Object>> scoreBoardTable;
    private TableColumn<Map<String, Object>, String> rankColumn;
    private TableColumn<Map<String, Object>, String> difficultyColumn;
    private TableColumn<Map<String, Object>, String> playerColumn;
    private TableColumn<Map<String, Object>, String> scoreColumn;
    private HBox nameInputBox;

    // 서비스
    private final ScoreService scoreService;

    // 게임 상태 저장을 위한 필드
    private long currentScore;
    private boolean isItemMode;
    private seoultech.se.core.model.enumType.Difficulty difficulty;


    // 모드 선택 팝업
    private VBox modeSelectionOverlay;
    private ModeSelectionPopup modeSelectionPopup;
    
    // 콜백
    private PopupActionCallback callback;
    
    /**
     * PopupManager 생성자
     * 
     * @param pauseOverlay 일시정지 오버레이 VBox
     * @param gameOverOverlay 게임 오버 오버레이 VBox
     * @param scoreService 점수 서비스
     */
    public PopupManager(VBox pauseOverlay, VBox gameOverOverlay, ScoreService scoreService) {
        this.pauseOverlay = pauseOverlay;
        this.gameOverOverlay = gameOverOverlay;
        this.scoreService = scoreService;

        // lookup을 사용하여 필요한 UI 요소들을 gameOverOverlay 내에서 찾아서 초기화
        if (gameOverOverlay != null) {
            this.gameOverTitleLabel = (Label) gameOverOverlay.lookup("#gameOverTitleLabel");
            this.finalScoreLabel = (Label) gameOverOverlay.lookup("#finalScoreLabel");
            this.usernameInput = (TextField) gameOverOverlay.lookup("#usernameInput");
            this.scoreBoardTable = (TableView<Map<String, Object>>) gameOverOverlay.lookup("#scoreBoardTable");
            this.nameInputBox = (HBox) gameOverOverlay.lookup("#nameInputBox");
        }
        
        // TableColumn은 TableView의 자식이므로 TableView에서 lookup (null-safe)
        if (this.scoreBoardTable != null) {
            javafx.collections.ObservableList<TableColumn<Map<String, Object>, ?>> columns = this.scoreBoardTable.getColumns();
            this.rankColumn = (TableColumn<Map<String, Object>, String>) columns.get(0);
            this.difficultyColumn = (TableColumn<Map<String, Object>, String>) columns.get(1);
            this.playerColumn = (TableColumn<Map<String, Object>, String>) columns.get(2);
            this.scoreColumn = (TableColumn<Map<String, Object>, String>) columns.get(3);
            // this.rankColumn = (TableColumn<Map<String, Object>, String>) scoreBoardTable.lookup("#rankColumn");
            // this.difficultyColumn = (TableColumn<Map<String, Object>, String>) scoreBoardTable.lookup("#difficultyColumn");
            // this.playerColumn = (TableColumn<Map<String, Object>, String>) scoreBoardTable.lookup("#playerColumn");
            // this.scoreColumn = (TableColumn<Map<String, Object>, String>) scoreBoardTable.lookup("#scoreColumn");
        }
        
        // FXML의 onAction을 제거하고 프로그램적으로 이벤트 핸들러 등록 (null-safe)
        if (this.usernameInput != null) {
            this.usernameInput.setOnAction(event -> handleNameInput());
        }
    }
    
    /**
     * 초기화: 모든 팝업을 숨김 상태로 설정
     */
    public void init() {
        hideAllPopups();
    }
    
    /**
     * 팝업 액션 콜백을 설정합니다
     * 
     * @param callback 팝업 액션 콜백
     */
    public void setCallback(PopupActionCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 오버레이의 가시성을 설정하는 헬퍼 메서드
     * Platform.runLater 코드 중복을 제거하기 위해 추출
     * 
     * @param overlay 제어할 오버레이 VBox
     * @param visible true면 표시, false면 숨김
     */
    private void setOverlayVisibility(VBox overlay, boolean visible) {
        Platform.runLater(() -> {
            System.out.println("🎯 [PopupManager] setOverlayVisibility - overlay: " + (overlay != null ? "OK" : "NULL") + ", visible: " + visible);
            if (overlay != null) {
                overlay.setVisible(visible);
                overlay.setManaged(visible);
                System.out.println("✅ [PopupManager] Overlay visibility set to: " + visible);
            } else {
                System.err.println("❌ [PopupManager] Overlay is NULL - cannot set visibility!");
            }
        });
    }
    
    /**
     * 일시정지 팝업을 표시합니다
     */
    public void showPausePopup() {
        setOverlayVisibility(pauseOverlay, true);
    }
    
    /**
     * 일시정지 팝업을 숨깁니다
     */
    public void hidePausePopup() {
        setOverlayVisibility(pauseOverlay, false);
    }
    
    /**
     * 게임 오버 팝업을 표시하고 관련 로직을 모두 처리합니다.
     * 
     * @param finalScore 최종 점수
     * @param isItemMode 아이템 모드 여부
     * @param difficulty 난이도
     */
    public void showGameOverPopup(long finalScore, boolean isItemMode, seoultech.se.core.model.enumType.Difficulty difficulty) {
        showGameOverPopup(finalScore, isItemMode, difficulty, "GAME OVER");
    }

    /**
     * 게임 오버 팝업을 표시하고 관련 로직을 모두 처리합니다. (제목 지정 가능)
     * 
     * @param finalScore 최종 점수
     * @param isItemMode 아이템 모드 여부
     * @param difficulty 난이도
     * @param title 팝업 제목 (예: "GAME OVER", "YOU WIN", "YOU LOSE")
     */
    public void showGameOverPopup(long finalScore, boolean isItemMode, seoultech.se.core.model.enumType.Difficulty difficulty, String title) {
        System.out.println("🎯 [PopupManager] showGameOverPopup called - Title: " + title + ", Score: " + finalScore);
        System.out.println("🔍 [PopupManager] gameOverOverlay: " + (gameOverOverlay != null ? "OK" : "NULL"));
        System.out.println("🔍 [PopupManager] gameOverTitleLabel: " + (gameOverTitleLabel != null ? "OK" : "NULL"));
        System.out.println("🔍 [PopupManager] finalScoreLabel: " + (finalScoreLabel != null ? "OK" : "NULL"));

        this.currentScore = finalScore;
        this.isItemMode = isItemMode;
        this.difficulty = difficulty;

        Platform.runLater(() -> {
            System.out.println("🎯 [PopupManager] Inside Platform.runLater - updating labels");
            if (finalScoreLabel != null) {
                finalScoreLabel.setText(String.valueOf(finalScore));
                System.out.println("✅ [PopupManager] finalScoreLabel updated");
            }
            if (gameOverTitleLabel != null) {
                gameOverTitleLabel.setText(title);
                System.out.println("✅ [PopupManager] gameOverTitleLabel updated to: " + title);
                // 승리 시 녹색, 패배 시 빨간색 등 스타일 변경 가능
                if ("YOU WIN".equals(title)) {
                    gameOverTitleLabel.setStyle("-fx-text-fill: #44FF44;"); // Green
                } else if ("YOU LOSE".equals(title)) {
                    gameOverTitleLabel.setStyle("-fx-text-fill: #FF4444;"); // Red
                } else {
                    gameOverTitleLabel.setStyle(""); // Default
                }
            }
        });

        System.out.println("🔍 [PopupManager] Loading scores...");
        List<ScoreRankDto> scores = scoreService.getTopScores(isItemMode, 10);
        System.out.println("✅ [PopupManager] Scores loaded: " + scores.size() + " entries");
        loadScores(scores);

        boolean isTopTen = scores.size() < 10 || scores.stream().anyMatch(s -> currentScore > s.getScore());
        if (isTopTen) {
            System.out.println("🏆 [PopupManager] Player is in top 10!");
            Platform.runLater(() -> {
                if (nameInputBox != null && usernameInput != null) {
                    nameInputBox.setVisible(true);
                    nameInputBox.setManaged(true);
                    usernameInput.requestFocus();
                    System.out.println("✅ [PopupManager] Name input box shown");
                }
            });
        }

        System.out.println("🔍 [PopupManager] About to call setOverlayVisibility...");
        setOverlayVisibility(gameOverOverlay, true);
        System.out.println("✅ [PopupManager] showGameOverPopup completed");
    }
    
    /**
     * 게임 오버 팝업을 숨깁니다
     */
    public void hideGameOverPopup() {
        setOverlayVisibility(gameOverOverlay, false);
    }
    
    /**
     * 모든 팝업을 숨깁니다
     */
    public void hideAllPopups() {
        hidePausePopup();
        hideGameOverPopup();
    }
    
    /**
     * 일시정지 팝업이 표시되어 있는지 확인합니다
     * 
     * @return 표시되어 있으면 true
     */
    public boolean isPausePopupVisible() {
        return pauseOverlay != null && pauseOverlay.isVisible();
    }
    
    /**
     * 게임 오버 팝업이 표시되어 있는지 확인합니다
     * 
     * @return 표시되어 있으면 true
     */
    public boolean isGameOverPopupVisible() {
        return gameOverOverlay != null && gameOverOverlay.isVisible();
    }
    
    // ========== 버튼 핸들러 메서드 (GameController의 @FXML 메서드에서 호출) ==========
    
    /**
     * Resume 버튼 핸들러
     * GameController의 @FXML handleResumeFromOverlay()에서 호출됩니다
     */
    public void handleResumeAction() {
        hidePausePopup();
        if (callback != null) {
            callback.onResumeRequested();
        }
    }
    
    /**
     * Quit 버튼 핸들러
     * GameController의 @FXML handleQuitFromOverlay()에서 호출됩니다
     */
    public void handleQuitAction() {
        if (callback != null) {
            callback.onQuitRequested();
        }
    }
    
    /**
     * Main Menu 버튼 핸들러
     * GameController의 @FXML handleMainFromOverlay()에서 호출됩니다
     */
    public void handleMainMenuAction() {
        if (callback != null) {
            callback.onMainMenuRequested();
        }
    }
    
    /**
     * Restart 버튼 핸들러
     * GameController의 @FXML handleRestartFromOverlay()에서 호출됩니다
     */
    public void handleRestartAction() {
        if (callback != null) {
            callback.onRestartRequested();
        }
    }

    // ========== Game Over Popup 내부 로직 ==========

    private void loadScores(List<ScoreRankDto> scores) {
        Platform.runLater(() -> {
            if (scoreBoardTable == null) return;

            List<Map<String, Object>> scoreMaps = scores.stream()
                    .map(score -> Map.<String, Object>of(
                            "rank", score.getRank(),
                            "player", score.getName(),
                            "score", score.getScore(),
                            "difficulty", score.getDifficulty() + (isItemMode ? " (Item)" : "")
                    ))
                    .collect(Collectors.toList());
            
            ObservableList<Map<String, Object>> scoreData = FXCollections.observableArrayList(scoreMaps);
            scoreBoardTable.setItems(scoreData);
        });
    }

    private void handleNameInput() {
        saveScoreAndRefreshUi().thenRun(() -> {
            Platform.runLater(() -> {
                if (gameOverOverlay != null) {
                    gameOverOverlay.requestFocus(); // 포커스를 다른 곳으로 이동
                }
            });
        });
    }

    private CompletableFuture<Void> saveScoreAndRefreshUi() {
        if (usernameInput == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("usernameInput is not initialized."));
        }
        String username = usernameInput.getText();
        if (username == null || username.trim().isEmpty()) {
            // 이름이 비어있으면 저장하지 않고 완료된 Future 반환
            return CompletableFuture.completedFuture(null);
        }

        ScoreRequestDto newScore = new ScoreRequestDto();
        newScore.setName(username);
        newScore.setScore((int) currentScore);
        newScore.setDifficulty(this.difficulty);
        newScore.setItemMode(this.isItemMode);

        return CompletableFuture.runAsync(() -> scoreService.saveScore(newScore))
            .thenRun(() -> {
                Platform.runLater(() -> {
                    if (nameInputBox != null) {
                        nameInputBox.setVisible(false);
                        nameInputBox.setManaged(false);
                    }
                    List<ScoreRankDto> updatedScores = scoreService.getTopScores(this.isItemMode, 10);
                    loadScores(updatedScores);
                });
            });
    }

    public CompletableFuture<Void> saveScoreIfPending() {
        if (nameInputBox != null && nameInputBox.isVisible()) {
            return saveScoreAndRefreshUi();
        } else {
            return CompletableFuture.completedFuture(null);
        }
    }
    
    // ========== 모드 선택 팝업 관리 ==========
    
    /**
     * 모드 선택 팝업 초기화
     * 
     * @param overlay 모드 선택 오버레이 VBox (game-view.fxml에서 주입)
     */
    public void initModeSelectionPopup(VBox overlay) {
        this.modeSelectionOverlay = overlay;
        this.modeSelectionPopup = new ModeSelectionPopup();
        
        // 오버레이에 팝업 추가
        if (modeSelectionOverlay != null) {
            modeSelectionOverlay.getChildren().clear();
            modeSelectionOverlay.getChildren().add(modeSelectionPopup);
        }
        
        // 초기에는 숨김
        setOverlayVisibility(modeSelectionOverlay, false);
        
        System.out.println("✅ Mode selection popup initialized");
    }
    
    /**
     * 모드 선택 팝업 표시
     * 
     * @param onStart 게임 시작 콜백
     * @param onCancel 취소 콜백
     */
    public void showModeSelectionPopup(Runnable onStart, Runnable onCancel) {
        if (modeSelectionOverlay != null && modeSelectionPopup != null) {
            Platform.runLater(() -> {
                // 콜백 설정
                modeSelectionPopup.setOnStart(() -> {
                    hideModeSelectionPopup();
                    if (onStart != null) {
                        onStart.run();
                    }
                });
                
                modeSelectionPopup.setOnCancel(() -> {
                    hideModeSelectionPopup();
                    if (onCancel != null) {
                        onCancel.run();
                    }
                });
                
                // 팝업 표시
                setOverlayVisibility(modeSelectionOverlay, true);
            });
        } else {
            System.err.println("❗ Mode selection popup not initialized. Call initModeSelectionPopup() first.");
        }
    }
    
    /**
     * 모드 선택 팝업 숨기기
     */
    public void hideModeSelectionPopup() {
        if (modeSelectionOverlay != null) {
            setOverlayVisibility(modeSelectionOverlay, false);
        }
    }
    
    /**
     * 모드 선택 팝업이 표시되어 있는지 확인
     * 
     * @return 표시되어 있으면 true
     */
    public boolean isModeSelectionPopupVisible() {
        return modeSelectionOverlay != null && modeSelectionOverlay.isVisible();
    }
    
    /**
     * 모드 선택 팝업 인스턴스 반환
     * 
     * @return ModeSelectionPopup 인스턴스
     */
    public ModeSelectionPopup getModeSelectionPopup() {
        return modeSelectionPopup;
    }
}
