package seoultech.se.client.service;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

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

    private static final String SETTINGS_FILE = "tetris_settings";

    public SettingsService() {
        // Constructor - initialization happens in @PostConstruct
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
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(new File(SETTINGS_FILE))) {
            props.load(in);
            soundVolume.set(Double.parseDouble(props.getProperty("soundVolume", "80")));
            colorMode.set(props.getProperty("colorMode", "colorModeDefault"));
            screenSize.set(props.getProperty("screenSize", "screenSizeM"));
            difficulty.set(props.getProperty("difficulty", "difficultyNormal"));
            double width = Double.parseDouble(props.getProperty("stageWidth", "500"));
            double height = Double.parseDouble(props.getProperty("stageHeight", "600"));
            applyResolution(width, height);
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
        props.setProperty("difficulty", difficulty.get());
        props.setProperty("screenSize", screenSize.get());
        props.setProperty("stageWidth", String.valueOf(stageWidth.get()));
        props.setProperty("stageHeight", String.valueOf(stageHeight.get()));
        try {
            props.store(new java.io.FileOutputStream(new File(SETTINGS_FILE)), null);
            System.out.println("✅ Settings saved successfully.");
        } catch (Exception e) {
            System.out.println("❗ Failed to save settings.");
        }
    }

    public void restoreDefaults() {
        setSoundVolume(80);
        setColorBlindMode(ColorBlindMode.NORMAL);
        setScreenSize("screenSizeM");
        setDifficulty("difficultyNormal");
        applyResolution(500, 700);
        // restoreDefaults 내의 각 setter가 saveSettings를 호출하므로 중복 호출 제거
        // saveSettings(); 
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
        // applyResolution을 호출하여 프로퍼티와 실제 Stage 크기를 모두 업데이트합니다.
        applyResolution(width, this.stageHeight.get());
        saveSettings();
    }

    public double getStageHeight() {
        return stageHeight.get();
    }

    public void setStageHeight(double height) {
        // applyResolution을 호출하여 프로퍼티와 실제 Stage 크기를 모두 업데이트합니다.
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
