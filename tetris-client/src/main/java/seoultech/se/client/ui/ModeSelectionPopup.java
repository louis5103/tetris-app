package seoultech.se.client.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.Getter;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.mode.PlayType;

/**
 * 게임 모드 선택 팝업 컴포넌트
 * 
 * 사용자가 게임 시작 전에 다음 항목을 선택할 수 있습니다:
 * - 플레이 타입 (로컬 싱글 / 온라인 멀티)
 * - 게임플레이 타입 (클래식 / 아케이드)
 * - SRS 옵션 (Super Rotation System)
 * 
 * PopupManager를 통해 game-view.fxml의 overlay에 동적으로 추가됩니다.
 */
public class ModeSelectionPopup extends VBox {
    
    @Getter
    private PlayType selectedPlayType = PlayType.LOCAL_SINGLE;
    
    @Getter
    private GameplayType selectedGameplayType = GameplayType.CLASSIC;
    
    @Getter
    private boolean srsEnabled = true;
    
    private final ToggleGroup playTypeGroup;
    private final ToggleGroup gameplayTypeGroup;
    private final CheckBox srsCheckBox;
    
    private Runnable onStartCallback;
    private Runnable onCancelCallback;
    
    /**
     * ModeSelectionPopup 생성자
     * UI 컴포넌트들을 초기화하고 레이아웃을 구성합니다.
     */
    public ModeSelectionPopup() {
        super(20);  // spacing
        setAlignment(Pos.CENTER);
        setPadding(new Insets(40));
        getStyleClass().add("mode-selection-popup");
        
        // ========== 제목 ==========
        Label title = new Label("게임 모드 선택");
        title.getStyleClass().add("popup-title");
        
        // ========== 플레이 타입 선택 ==========
        Label playTypeLabel = new Label("플레이 타입:");
        playTypeLabel.getStyleClass().add("section-label");
        
        playTypeGroup = new ToggleGroup();
        
        RadioButton singleRadio = createPlayTypeRadio(
            PlayType.LOCAL_SINGLE.getDisplayName(),
            PlayType.LOCAL_SINGLE.getDescription(),
            PlayType.LOCAL_SINGLE,
            true
        );
        
        RadioButton multiRadio = createPlayTypeRadio(
            PlayType.ONLINE_MULTI.getDisplayName(),
            PlayType.ONLINE_MULTI.getDescription(),
            PlayType.ONLINE_MULTI,
            false
        );
        
        VBox playTypeBox = new VBox(10, playTypeLabel, singleRadio, multiRadio);
        playTypeBox.getStyleClass().add("selection-box");
        
        // ========== 게임플레이 타입 선택 ==========
        Label gameplayLabel = new Label("게임플레이 타입:");
        gameplayLabel.getStyleClass().add("section-label");
        
        gameplayTypeGroup = new ToggleGroup();
        
        RadioButton classicRadio = createGameplayTypeRadio(
            GameplayType.CLASSIC.getDisplayName(),
            GameplayType.CLASSIC.getDescription(),
            GameplayType.CLASSIC,
            true
        );
        
        RadioButton arcadeRadio = createGameplayTypeRadio(
            GameplayType.ARCADE.getDisplayName(),
            GameplayType.ARCADE.getDescription(),
            GameplayType.ARCADE,
            false
        );
        
        VBox gameplayBox = new VBox(10, gameplayLabel, classicRadio, arcadeRadio);
        gameplayBox.getStyleClass().add("selection-box");
        
        // ========== SRS 옵션 ==========
        srsCheckBox = new CheckBox("SRS (Super Rotation System) 활성화");
        srsCheckBox.setSelected(true);
        srsCheckBox.getStyleClass().add("srs-checkbox");
        
        Label srsHint = new Label("※ SRS는 현대적인 블록 회전 시스템입니다");
        srsHint.getStyleClass().add("hint-label");
        
        VBox srsBox = new VBox(5, srsCheckBox, srsHint);
        srsBox.setAlignment(Pos.CENTER_LEFT);
        srsBox.getStyleClass().add("srs-box");
        
        // ========== 버튼 ==========
        Button startButton = new Button("게임 시작");
        startButton.getStyleClass().addAll("primary-button", "game-start-button");
        startButton.setOnAction(e -> handleStart());
        
        Button cancelButton = new Button("취소");
        cancelButton.getStyleClass().add("secondary-button");
        cancelButton.setOnAction(e -> handleCancel());
        
        HBox buttonBox = new HBox(15, startButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.getStyleClass().add("button-box");
        
        // ========== 전체 레이아웃 ==========
        Separator separator1 = new Separator();
        Separator separator2 = new Separator();
        Separator separator3 = new Separator();
        
        getChildren().addAll(
            title,
            separator1,
            playTypeBox,
            separator2,
            gameplayBox,
            separator3,
            srsBox,
            buttonBox
        );
        
        // 최소 크기 설정
        setMinWidth(450);
        setMaxWidth(550);
    }
    
    /**
     * 플레이 타입 라디오 버튼 생성 헬퍼 메서드
     */
    private RadioButton createPlayTypeRadio(String text, String tooltip, PlayType playType, boolean selected) {
        RadioButton radio = new RadioButton(text);
        radio.setToggleGroup(playTypeGroup);
        radio.setSelected(selected);
        radio.setUserData(playType);
        radio.getStyleClass().add("play-type-radio");
        
        if (tooltip != null && !tooltip.isEmpty()) {
            Tooltip tip = new Tooltip(tooltip);
            radio.setTooltip(tip);
        }
        
        return radio;
    }
    
    /**
     * 게임플레이 타입 라디오 버튼 생성 헬퍼 메서드
     */
    private RadioButton createGameplayTypeRadio(String text, String tooltip, GameplayType gameplayType, boolean selected) {
        RadioButton radio = new RadioButton(text);
        radio.setToggleGroup(gameplayTypeGroup);
        radio.setSelected(selected);
        radio.setUserData(gameplayType);
        radio.getStyleClass().add("gameplay-type-radio");
        
        if (tooltip != null && !tooltip.isEmpty()) {
            Tooltip tip = new Tooltip(tooltip);
            radio.setTooltip(tip);
        }
        
        return radio;
    }
    
    /**
     * 게임 시작 버튼 핸들러
     */
    private void handleStart() {
        // 선택 값 저장
        Toggle selectedPlayToggle = playTypeGroup.getSelectedToggle();
        Toggle selectedGameplayToggle = gameplayTypeGroup.getSelectedToggle();
        
        if (selectedPlayToggle != null) {
            selectedPlayType = (PlayType) selectedPlayToggle.getUserData();
        }
        
        if (selectedGameplayToggle != null) {
            selectedGameplayType = (GameplayType) selectedGameplayToggle.getUserData();
        }
        
        srsEnabled = srsCheckBox.isSelected();
        
        System.out.println("🎮 Mode selected: " + 
            selectedPlayType.getDisplayName() + " / " + 
            selectedGameplayType.getDisplayName() + " / SRS=" + srsEnabled);
        
        // 콜백 호출
        if (onStartCallback != null) {
            onStartCallback.run();
        }
    }
    
    /**
     * 취소 버튼 핸들러
     */
    private void handleCancel() {
        System.out.println("❌ Mode selection cancelled");
        
        if (onCancelCallback != null) {
            onCancelCallback.run();
        }
    }
    
    /**
     * 게임 시작 콜백 설정
     * 
     * @param callback 게임 시작 시 실행할 Runnable
     */
    public void setOnStart(Runnable callback) {
        this.onStartCallback = callback;
    }
    
    /**
     * 취소 콜백 설정
     * 
     * @param callback 취소 시 실행할 Runnable
     */
    public void setOnCancel(Runnable callback) {
        this.onCancelCallback = callback;
    }
    
    /**
     * 마지막 선택 값으로 UI 복원
     * 
     * @param playType 플레이 타입
     * @param gameplayType 게임플레이 타입
     * @param srsEnabled SRS 활성화 여부
     */
    public void restoreSelection(PlayType playType, GameplayType gameplayType, boolean srsEnabled) {
        // 플레이 타입 복원
        for (Toggle toggle : playTypeGroup.getToggles()) {
            if (toggle.getUserData() == playType) {
                toggle.setSelected(true);
                break;
            }
        }
        
        // 게임플레이 타입 복원
        for (Toggle toggle : gameplayTypeGroup.getToggles()) {
            if (toggle.getUserData() == gameplayType) {
                toggle.setSelected(true);
                break;
            }
        }
        
        // SRS 체크박스 복원
        srsCheckBox.setSelected(srsEnabled);
        
        // 필드 업데이트
        this.selectedPlayType = playType;
        this.selectedGameplayType = gameplayType;
        this.srsEnabled = srsEnabled;
    }
}
