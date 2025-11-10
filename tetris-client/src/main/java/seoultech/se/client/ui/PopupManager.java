package seoultech.se.client.ui;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

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
    private final Label finalScoreLabel;
    
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
     * @param finalScoreLabel 게임 오버 화면의 최종 점수 Label
     */
    public PopupManager(VBox pauseOverlay, VBox gameOverOverlay, Label finalScoreLabel) {
        this.pauseOverlay = pauseOverlay;
        this.gameOverOverlay = gameOverOverlay;
        this.finalScoreLabel = finalScoreLabel;
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
            overlay.setVisible(visible);
            overlay.setManaged(visible);
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
     * 게임 오버 팝업을 표시합니다
     * 
     * @param finalScore 최종 점수
     */
    public void showGameOverPopup(long finalScore) {
        Platform.runLater(() -> {
            finalScoreLabel.setText(String.valueOf(finalScore));
        });
        setOverlayVisibility(gameOverOverlay, true);
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
        return pauseOverlay.isVisible();
    }
    
    /**
     * 게임 오버 팝업이 표시되어 있는지 확인합니다
     * 
     * @return 표시되어 있으면 true
     */
    public boolean isGameOverPopupVisible() {
        return gameOverOverlay.isVisible();
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
        hidePausePopup();
        if (callback != null) {
            callback.onQuitRequested();
        }
    }
    
    /**
     * Main Menu 버튼 핸들러
     * GameController의 @FXML handleMainFromOverlay()에서 호출됩니다
     */
    public void handleMainMenuAction() {
        hideGameOverPopup();
        if (callback != null) {
            callback.onMainMenuRequested();
        }
    }
    
    /**
     * Restart 버튼 핸들러
     * GameController의 @FXML handleRestartFromOverlay()에서 호출됩니다
     */
    public void handleRestartAction() {
        hideGameOverPopup();
        if (callback != null) {
            callback.onRestartRequested();
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
        modeSelectionOverlay.getChildren().clear();
        modeSelectionOverlay.getChildren().add(modeSelectionPopup);
        
        // 초기에는 숨김
        modeSelectionOverlay.setVisible(false);
        modeSelectionOverlay.setManaged(false);
        
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
                modeSelectionOverlay.setVisible(true);
                modeSelectionOverlay.setManaged(true);
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
