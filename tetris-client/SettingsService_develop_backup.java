package seoultech.se.client.service;

import java.util.prefs.Preferences;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.stage.Stage;
import seoultech.se.client.constants.ColorBlindMode;

@Service
public class SettingsService {

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

    public void loadSettings() {
        soundVolume.set(preferences.getDouble("soundVolume", 80));
        colorMode.set(preferences.get("colorMode", "colorModeDefault"));
        screenSize.set(preferences.get("screenSize", "screenSizeM"));
        difficulty.set(preferences.get("difficulty", "difficultyNormal"));
        double width = preferences.getDouble("stageWidth", 500);
        double height = preferences.getDouble("stageHeight", 600);
        applyResolution(width, height);
        System.out.println("✅ Settings loaded successfully from preferences.");
    }

    public void saveSettings() {
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
}
