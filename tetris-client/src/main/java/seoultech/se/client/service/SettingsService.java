package seoultech.se.client.service;

import java.util.prefs.Preferences;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.stage.Stage;
import seoultech.se.client.constants.ColorBlindMode;
import seoultech.se.client.config.GameModeProperties;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.mode.PlayType;

@Service
public class SettingsService {

    @Autowired
    private GameModeProperties gameModeProperties;

    private Stage primaryStage;
    private final DoubleProperty stageWidth = new SimpleDoubleProperty(500);
    private final DoubleProperty stageHeight = new SimpleDoubleProperty(600);

    private final DoubleProperty soundVolume = new SimpleDoubleProperty(80); // Default volume is 80
    private final StringProperty colorMode = new SimpleStringProperty("colorModeDefault"); // default, rg_blind, yb_blind
    private final StringProperty difficulty = new SimpleStringProperty("difficultyNormal"); // easy, normal, hard
    private final StringProperty screenSize = new SimpleStringProperty("screenSizeM"); // XS, S, M, L, XL

    private static final String PREFS_NODE = "tetris_settings";
    private final Preferences preferences;

    public SettingsService() {
        this.preferences = Preferences.userRoot().node(PREFS_NODE);
    }
    
    /**
     * 초기화: 설정 파일에서 로드하거나 기본값 설정
     */
    @PostConstruct
    public void init() {
        loadSettings();
        
        // 화면 크기 변경 리스너 추가
        screenSize.addListener((observable, oldValue, newValue) -> {
            applyScreenSizeClass();
        });
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void applyResolution(double width, double height) {
        stageWidth.set(width);
        stageHeight.set(height);
        if (primaryStage != null) {
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
            primaryStage.centerOnScreen();
        }
    }
    
    /**
     * 화면 크기 설정을 CSS 클래스로 적용
     * Scene의 루트 노드에 화면 크기별 CSS 클래스를 적용합니다.
     */
    public void applyScreenSizeClass() {
        if (primaryStage != null && primaryStage.getScene() != null) {
            javafx.scene.Parent root = primaryStage.getScene().getRoot();
            if (root != null) {
                // 기존 화면 크기 클래스 제거
                root.getStyleClass().removeIf(styleClass -> 
                    styleClass.startsWith("screenSize"));
                
                // 새로운 화면 크기 클래스 추가
                String sizeClass = screenSize.get();
                if (sizeClass != null && !sizeClass.isEmpty()) {
                    root.getStyleClass().add(sizeClass);
                    System.out.println("✅ Applied screen size class: " + sizeClass);
                }
            }
        }
    }

    public void loadSettings() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(new File(SETTINGS_FILE))) {
            props.load(in);
            soundVolume.set(Double.parseDouble(props.getProperty("soundVolume", "80")));
            colorMode.set(props.getProperty("colorMode", "colorModeDefault"));
            screenSize.set(props.getProperty("screenSize", "screenSizeM"));
            double width = Double.parseDouble(props.getProperty("stageWidth", "500"));
            double height = Double.parseDouble(props.getProperty("stageHeight", "600"));
            applyResolution(width, height);
            applyScreenSizeClass();
            System.out.println("✅ Settings loaded successfully.");
        } catch (Exception e) {
            System.out.println("❗ Failed to load settings, using defaults.");
            restoreDefaults();
        }
    }

    public void saveSettings() {
        Properties props = new Properties();
        props.setProperty("soundVolume", String.valueOf(soundVolume.get()));
        props.setProperty("colorMode", colorMode.get());
        props.setProperty("screenSize", screenSize.get());
        props.setProperty("stageWidth", String.valueOf(stageWidth.get()));
        props.setProperty("stageHeight", String.valueOf(stageHeight.get()));
        
        // 게임 모드 설정 저장 (GameModeProperties를 통해)
        if (gameModeProperties != null) {
            props.setProperty("game.mode.playType", gameModeProperties.getPlayType().name());
            props.setProperty("game.mode.gameplayType", gameModeProperties.getGameplayType().name());
            props.setProperty("game.mode.srsEnabled", String.valueOf(gameModeProperties.isSrsEnabled()));
        }
        
        preferences.putDouble("soundVolume", soundVolume.get());
        preferences.put("colorMode", colorMode.get());
        preferences.put("difficulty", difficulty.get());
        preferences.put("screenSize", screenSize.get());
        preferences.putDouble("stageWidth", stageWidth.get());
        preferences.putDouble("stageHeight", stageHeight.get());
        try {
            preferences.flush(); // Ensure changes are written to persistent store
            System.out.println("✅ Settings saved successfully to preferences.");
        } catch (Exception e) {
            System.err.println("❗ Failed to save settings to preferences: " + e.getMessage());
        }
    }

    public void restoreDefaults() {
        setSoundVolume(80);
        setColorBlindMode(ColorBlindMode.NORMAL);
        setScreenSize("screenSizeM");
        setDifficulty("difficultyNormal");
        applyResolution(500, 700);
        saveSettings(); // Save the default settings
    }

    // ========== Property Accessors for JavaFX Binding ========== 

    public DoubleProperty soundVolumeProperty() { 
        return soundVolume;
    }

    public StringProperty colorModeProperty() {
        return colorMode;
    }

    public StringProperty screenSizeProperty() {
        return screenSize;
    }

    public DoubleProperty stageWidthProperty() {
        return stageWidth;
    }

    public DoubleProperty stageHeightProperty() {
        return stageHeight;
    }

    public StringProperty difficultyProperty() {
        return difficulty;
    }

    // ========== Standard Getters and Setters ========== 

    public double getSoundVolume() {
        return soundVolume.get();
    }

    public void setSoundVolume(double volume) {
        soundVolume.set(volume);
        saveSettings();
    }

    public String getScreenSize() {
        return screenSize.get();
    }

    public void setScreenSize(String size) {
        screenSize.set(size);
        saveSettings();
    }

    public String getDifficulty() {
        return difficulty.get();
    }

    public void setDifficulty(String diff) {
        difficulty.set(diff);
        saveSettings();
    }

    public double getStageWidth() {
        return stageWidth.get();
    }

    public void setStageWidth(double width) {
        applyResolution(width, this.stageHeight.get());
        saveSettings();
    }

    public double getStageHeight() {
        return stageHeight.get();
    }

    public void setStageHeight(double height) {
        applyResolution(this.stageWidth.get(), height);
        saveSettings();
    }

    /**
     * colorMode 문자열을 ColorBlindMode enum으로 변환
     * 
     * @return 대응하는 ColorBlindMode
     */
    public ColorBlindMode getColorBlindMode() {
        String mode = colorMode.get();
        return switch (mode) {
            case "colorModeRGBlind" -> ColorBlindMode.RED_GREEN_BLIND;
            case "colorModeBYBlind" -> ColorBlindMode.BLUE_YELLOW_BLIND;
            default -> ColorBlindMode.NORMAL;
        };
    }

    /**
     * ColorBlindMode를 설정하고 저장
     * 
     * @param mode 설정할 색맹 모드
     */
    public void setColorBlindMode(ColorBlindMode mode) {
        String modeString = switch (mode) {
            case RED_GREEN_BLIND -> "colorModeRGBlind";
            case BLUE_YELLOW_BLIND -> "colorModeYBBlind";
            default -> "colorModeDefault";
        };
        // colorMode.set(modeString)을 직접 호출하는 대신,
        // 일관성을 위해 내부 프로퍼티를 직접 수정합니다.
        if (!colorMode.get().equals(modeString)) {
            colorMode.set(modeString);
            saveSettings();
        }
    }
    
    // ========== Game Mode Configuration ==========
    
    /**
     * GameModeConfig 빌드
     * GameModeProperties의 설정을 기반으로 GameModeConfig 객체를 생성합니다.
     * 
     * @return GameModeConfig 객체
     */
    public GameModeConfig buildGameModeConfig() {
        try {
            // 유효성 검증
            if (!validateGameModeSettings()) {
                System.err.println("⚠️ Invalid game mode settings detected, using defaults");
            }
            
            GameplayType gameplayType = gameModeProperties.getGameplayType();
            boolean srsEnabled = gameModeProperties.isSrsEnabled();
            
            // 게임플레이 타입에 따라 프리셋 사용
            if (gameplayType == GameplayType.ARCADE) {
                return GameModeConfig.arcade();
            } else {
                return GameModeConfig.classic(srsEnabled);
            }
        } catch (Exception e) {
            System.err.println("❗ Failed to build game mode config: " + e.getMessage());
            e.printStackTrace();
            // 기본값 반환
            return GameModeConfig.classic(true);
        }
    }
    
    /**
     * 게임 모드 설정 저장
     * 
     * @param playType 플레이 타입
     * @param gameplayType 게임플레이 타입
     * @param srsEnabled SRS 활성화 여부
     */
    public void saveGameModeSettings(PlayType playType, GameplayType gameplayType, boolean srsEnabled) {
        try {
            // GameModeProperties 업데이트
            gameModeProperties.setPlayType(playType);
            gameModeProperties.setGameplayType(gameplayType);
            gameModeProperties.setSrsEnabled(srsEnabled);
            
            // 마지막 선택 저장
            gameModeProperties.setLastPlayType(playType);
            gameModeProperties.setLastGameplayType(gameplayType);
            gameModeProperties.setLastSrsEnabled(srsEnabled);
            
            // 기존 설정 저장 메서드 호출
            saveSettings();
            
            System.out.println("✅ Game mode settings saved: " + 
                playType.getDisplayName() + " / " + 
                gameplayType.getDisplayName() + " / SRS=" + srsEnabled);
        } catch (Exception e) {
            System.err.println("❗ Failed to save game mode settings: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 커스텀 게임 모드 설정 저장 (모든 설정 포함)
     * 
     * @param gameplayType 게임플레이 타입
     * @param config 게임 모드 설정
     */
    public void saveCustomGameModeConfig(GameplayType gameplayType, GameModeConfig config) {
        try {
            Properties props = new Properties();
            
            // 기존 설정 파일 로드
            try (FileInputStream in = new FileInputStream(new File(SETTINGS_FILE))) {
                props.load(in);
            } catch (Exception e) {
                // 파일이 없으면 새로 생성
            }
            
            // 모드별 키 접두사
            String prefix = "custom." + gameplayType.name().toLowerCase() + ".";
            
            // 모든 설정 저장
            props.setProperty(prefix + "srsEnabled", String.valueOf(config.isSrsEnabled()));
            props.setProperty(prefix + "rotation180Enabled", String.valueOf(config.isRotation180Enabled()));
            props.setProperty(prefix + "hardDropEnabled", String.valueOf(config.isHardDropEnabled()));
            props.setProperty(prefix + "holdEnabled", String.valueOf(config.isHoldEnabled()));
            props.setProperty(prefix + "ghostPieceEnabled", String.valueOf(config.isGhostPieceEnabled()));
            props.setProperty(prefix + "dropSpeedMultiplier", String.valueOf(config.getDropSpeedMultiplier()));
            props.setProperty(prefix + "softDropSpeed", String.valueOf(config.getSoftDropSpeed()));
            props.setProperty(prefix + "lockDelay", String.valueOf(config.getLockDelay()));
            
            // 파일에 저장
            try (java.io.FileOutputStream out = new java.io.FileOutputStream(new File(SETTINGS_FILE))) {
                props.store(out, "Tetris Game Settings");
                System.out.println("✅ Custom game mode config saved for " + gameplayType.getDisplayName());
            }
        } catch (Exception e) {
            System.err.println("❗ Failed to save custom game mode config: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 커스텀 게임 모드 설정 로드
     * 
     * @param gameplayType 게임플레이 타입
     * @return 저장된 커스텀 설정, 없으면 null
     */
    public GameModeConfig loadCustomGameModeConfig(GameplayType gameplayType) {
        try {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(new File(SETTINGS_FILE))) {
                props.load(in);
            }
            
            String prefix = "custom." + gameplayType.name().toLowerCase() + ".";
            
            // 저장된 설정이 있는지 확인
            if (!props.containsKey(prefix + "srsEnabled")) {
                return null; // 저장된 커스텀 설정 없음
            }
            
            // 모든 설정 로드
            return GameModeConfig.builder()
                .gameplayType(gameplayType)
                .srsEnabled(Boolean.parseBoolean(props.getProperty(prefix + "srsEnabled", "true")))
                .rotation180Enabled(Boolean.parseBoolean(props.getProperty(prefix + "rotation180Enabled", "false")))
                .hardDropEnabled(Boolean.parseBoolean(props.getProperty(prefix + "hardDropEnabled", "true")))
                .holdEnabled(Boolean.parseBoolean(props.getProperty(prefix + "holdEnabled", "true")))
                .ghostPieceEnabled(Boolean.parseBoolean(props.getProperty(prefix + "ghostPieceEnabled", "true")))
                .dropSpeedMultiplier(Double.parseDouble(props.getProperty(prefix + "dropSpeedMultiplier", "1.0")))
                .softDropSpeed(Double.parseDouble(props.getProperty(prefix + "softDropSpeed", "20.0")))
                .lockDelay(Integer.parseInt(props.getProperty(prefix + "lockDelay", "500")))
                .build();
        } catch (Exception e) {
            System.err.println("❗ Failed to load custom game mode config: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 게임 모드 설정 유효성 검증
     * 
     * @return 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateGameModeSettings() {
        boolean isValid = true;
        
        if (gameModeProperties.getPlayType() == null) {
            System.err.println("❗ PlayType is null, setting to default: LOCAL_SINGLE");
            gameModeProperties.setPlayType(PlayType.LOCAL_SINGLE);
            isValid = false;
        }
        
        if (gameModeProperties.getGameplayType() == null) {
            System.err.println("❗ GameplayType is null, setting to default: CLASSIC");
            gameModeProperties.setGameplayType(GameplayType.CLASSIC);
            isValid = false;
        }
        
        return isValid;
    }
    
    /**
     * 마지막 선택 설정 복원
     */
    public void restoreLastGameModeSettings() {
        try {
            PlayType lastPlayType = gameModeProperties.getLastPlayType();
            GameplayType lastGameplayType = gameModeProperties.getLastGameplayType();
            boolean lastSrsEnabled = gameModeProperties.isLastSrsEnabled();
            
            if (lastPlayType != null && lastGameplayType != null) {
                gameModeProperties.setPlayType(lastPlayType);
                gameModeProperties.setGameplayType(lastGameplayType);
                gameModeProperties.setSrsEnabled(lastSrsEnabled);
                
                System.out.println("✅ Last game mode settings restored: " + 
                    lastPlayType.getDisplayName() + " / " + 
                    lastGameplayType.getDisplayName());
            }
        } catch (Exception e) {
            System.err.println("❗ Failed to restore last game mode settings: " + e.getMessage());
        }
    }
    
    /**
     * GameModeProperties 반환 (외부 접근용)
     * 
     * @return GameModeProperties
     */
    public GameModeProperties getGameModeProperties() {
        return gameModeProperties;
    }
}
