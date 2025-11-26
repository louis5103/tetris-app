package seoultech.se.client.ui;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import seoultech.se.client.constants.UIConstants;

/**
 * 게임 내 알림 메시지를 관리하는 클래스
 * 
 * 이 클래스는 다음과 같은 알림들을 표시하고 관리합니다:
 * - Combo 메시지 (좌측, 페이드아웃)
 * - 라인 클리어 타입 (중앙)
 * - Back-to-Back 메시지 (우측)
 * - 라인 클리어 수 알림 (우측 중간)
 * 
 * GameController에서 알림 관련 책임을 분리하여
 * 단일 책임 원칙(SRP)을 준수합니다.
 */
public class NotificationManager {
    
    // UI 요소들
    private final HBox topEventLine;
    private final Label comboLabel;
    private final Label lineClearTypeLabel;
    private final Label backToBackLabel;
    private final Label lineClearNotificationLabel;
    
    // Combo 페이드아웃을 위한 타이머
    private AnimationTimer comboFadeTimer;
    private long comboShowTime = 0;
    
    /**
     * NotificationManager 생성자
     * 
     * @param topEventLine Combo와 B2B를 표시하는 HBox
     * @param comboLabel Combo 메시지 Label
     * @param lineClearTypeLabel 라인 클리어 타입 Label
     * @param backToBackLabel Back-to-Back 메시지 Label
     * @param lineClearNotificationLabel 라인 클리어 수 알림 Label
     */
    public NotificationManager(
            HBox topEventLine,
            Label comboLabel,
            Label lineClearTypeLabel,
            Label backToBackLabel,
            Label lineClearNotificationLabel) {
        
        this.topEventLine = topEventLine;
        this.comboLabel = comboLabel;
        this.lineClearTypeLabel = lineClearTypeLabel;
        this.backToBackLabel = backToBackLabel;
        this.lineClearNotificationLabel = lineClearNotificationLabel;
        
        setupComboFadeTimer();
    }
    
    /**
     * Combo 페이드아웃 타이머 설정
     * 
     * Combo 메시지가 일정 시간 후 자동으로 사라지도록 합니다.
     */
    private void setupComboFadeTimer() {
        comboFadeTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (comboLabel.isVisible() && now - comboShowTime > UIConstants.COMBO_DISPLAY_DURATION_NS) {
                    Platform.runLater(() -> {
                        comboLabel.setVisible(false);
                        comboLabel.setManaged(false);
                        updateTopEventLineAlignment();
                    });
                    stop();
                }
            }
        };
    }
    
    /**
     * Combo 메시지를 표시합니다 (좌측)
     * 
     * @param message 표시할 메시지 (예: "🔥 COMBO x3")
     */
    public void showCombo(String message) {
        Platform.runLater(() -> {
            comboLabel.setText(message);
            comboLabel.setVisible(true);
            comboLabel.setManaged(true);
            comboShowTime = System.nanoTime();
            comboFadeTimer.start();
            
            updateTopEventLineAlignment();
        });
    }
    
    /**
     * 라인 클리어 타입을 표시합니다 (중앙)
     * 
     * @param message 표시할 메시지 (예: "T-SPIN DOUBLE", "TETRIS")
     */
    public void showLineClearType(String message) {
        showTemporaryMessage(lineClearTypeLabel, message);
    }
    
    /**
     * Back-to-Back 메시지를 표시합니다 (우측)
     * 
     * @param message 표시할 메시지 (예: "⚡ B2B x2")
     */
    public void showBackToBack(String message) {
        Platform.runLater(() -> {
            backToBackLabel.setText(message);
            backToBackLabel.setVisible(true);
            backToBackLabel.setManaged(true);
            
            updateTopEventLineAlignment();
            
            // 일정 시간 후 사라지기
            scheduleHide(backToBackLabel, () -> updateTopEventLineAlignment());
        });
    }
    
    /**
     * 라인 클리어 수 알림을 표시합니다 (우측 중간)
     * 
     * @param clearedLines 방금 지운 라인 수
     * @param totalLines 총 라인 수
     */
    public void showLineClearCount(int clearedLines, int totalLines) {
        String message = String.format("+%d LINE%s | Total: %d", 
            clearedLines, 
            clearedLines > 1 ? "S" : "",
            totalLines);
        
        showTemporaryMessage(lineClearNotificationLabel, message);
    }
    
    /**
     * 일시적인 메시지를 표시합니다
     * 
     * @param label 메시지를 표시할 Label
     * @param message 표시할 메시지
     */
    private void showTemporaryMessage(Label label, String message) {
        Platform.runLater(() -> {
            label.setText(message);
            label.setVisible(true);
            label.setManaged(true);
            
            scheduleHide(label, null);
        });
    }
    
    /**
     * Label을 일정 시간 후 숨깁니다
     * 
     * @param label 숨길 Label
     * @param afterHideCallback 숨긴 후 실행할 콜백 (nullable)
     */
    private void scheduleHide(Label label, Runnable afterHideCallback) {
        new Thread(() -> {
            try {
                Thread.sleep(UIConstants.NOTIFICATION_DISPLAY_DURATION_MS);
                Platform.runLater(() -> {
                    label.setVisible(false);
                    label.setManaged(false);
                    if (afterHideCallback != null) {
                        afterHideCallback.run();
                    }
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }).start();
    }
    
    /**
     * topEventLine의 정렬을 동적으로 조정합니다
     * 
     * - 둘 다 숨겨져 있으면: HBox 자체를 숨김
     * - 하나 이상 표시되면: CENTER 정렬로 표시
     */
    private void updateTopEventLineAlignment() {
        boolean comboVisible = comboLabel.isVisible();
        boolean b2bVisible = backToBackLabel.isVisible();
        
        if (!comboVisible && !b2bVisible) {
            // 둘 다 숨겨졌으면 HBox도 숨김
            topEventLine.setVisible(false);
            topEventLine.setManaged(false);
        } else {
            // 하나 이상 표시되면 중앙 정렬
            topEventLine.setAlignment(Pos.CENTER);
            topEventLine.setVisible(true);
            topEventLine.setManaged(true);
        }
    }
    
    /**
     * 모든 알림을 즉시 숨깁니다
     * 
     * 게임 일시정지나 재개 시 사용됩니다.
     */
    public void hideAllNotifications() {
        Platform.runLater(() -> {
            comboLabel.setVisible(false);
            comboLabel.setManaged(false);
            lineClearTypeLabel.setVisible(false);
            lineClearTypeLabel.setManaged(false);
            backToBackLabel.setVisible(false);
            backToBackLabel.setManaged(false);
            lineClearNotificationLabel.setVisible(false);
            lineClearNotificationLabel.setManaged(false);
            
            updateTopEventLineAlignment();
        });
    }
    
    /**
     * ✨ 공격 받음 알림 표시
     *
     * @param attackLines 받은 공격 라인 수
     */
    public void showAttackNotification(int attackLines) {
        Platform.runLater(() -> {
            // 라인 클리어 알림 레이블 사용
            lineClearNotificationLabel.setText("⚔️ ATTACKED: +" + attackLines + " lines");
            lineClearNotificationLabel.setStyle("-fx-text-fill: #FF4444; -fx-font-weight: bold;");
            lineClearNotificationLabel.setVisible(true);
            lineClearNotificationLabel.setManaged(true);

            // 2초 후 숨기기
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(() -> {
                        lineClearNotificationLabel.setVisible(false);
                        lineClearNotificationLabel.setManaged(false);
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }

    /**
     * Combo 타이머를 정리합니다
     *
     * NotificationManager가 더 이상 사용되지 않을 때 호출되어야 합니다.
     */
    public void cleanup() {
        if (comboFadeTimer != null) {
            comboFadeTimer.stop();
        }
    }
}
