package seoultech.se.client.service;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.stage.Stage;
import seoultech.se.client.config.ClientSettings;
import seoultech.se.client.config.GeneralSettings;
import seoultech.se.client.constants.ColorBlindMode;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.model.enumType.Difficulty;

@Service
public class SettingsService {

    @Autowired
    private ClientSettings clientSettings;
    
    @Autowired
    private YamlConfigPersistence yamlPersistence;
    
    @Autowired
    private GameModeConfigFactory configFactory;
    
    // ========== application.yml 기본값은 GeneralSettings에서 주입 ==========
    // ClientSettings의 GeneralSettings가 기본값을 포함하고 있음

    private Stage primaryStage;
    private final DoubleProperty stageWidth = new SimpleDoubleProperty();
    private final DoubleProperty stageHeight = new SimpleDoubleProperty();

    private final DoubleProperty soundVolume = new SimpleDoubleProperty(80); // Default volume is 80
    private final StringProperty colorMode = new SimpleStringProperty("colorModeDefault"); // default, rg_blind, yb_blind
    private final StringProperty difficulty = new SimpleStringProperty("difficultyNormal"); // easy, normal, hard
    private final StringProperty screenSize = new SimpleStringProperty("screenSizeM"); // XS, S, M, L, XL

    private String settingsFilePath = "src/main/resources/config/client/setting.yml";
    private String classicModeFilePath = "src/main/resources/config/client/classic.yml";
    private String arcadeModeFilePath = "src/main/resources/config/client/arcade.yml";

    private final Yaml yaml;

    public SettingsService() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true); // Ensures pretty printing for flow style, if used
        options.setWidth(100); // Set a reasonable width for the output
        this.yaml = new Yaml(options);
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
        try (FileInputStream in = new FileInputStream(settingsFilePath)) {
            Map<String, Object> data = yaml.load(in);
            Map<String, Object> settings = getNestedMap(data, "client.setting");

            // GeneralSettings null 체크 및 안전한 기본값 제공
            GeneralSettings defaultSettings = getDefaultSettings();
            
            soundVolume.set(getSetting(settings, "soundVolume", defaultSettings.getSoundVolume()));
            colorMode.set(getSetting(settings, "colorMode", defaultSettings.getColorMode()));
            screenSize.set(getSetting(settings, "screenSize", defaultSettings.getScreenSize()));
            difficulty.set(getSetting(settings, "difficulty", defaultSettings.getDifficulty()));

            double width = getSetting(settings, "stageWidth", defaultSettings.getStageWidth());
            double height = getSetting(settings, "stageHeight", defaultSettings.getStageHeight());

            applyResolution(width, height);
            applyScreenSizeClass();

            System.out.println("✅ Settings loaded successfully from " + settingsFilePath);
        } catch (Exception e) {
            System.out.println("❗ Failed to load settings from " + settingsFilePath + ", using defaults. " + e.getMessage());
            restoreDefaults();
        }
    }

    /**
     * 현재 UI 설정을 YAML 파일에 저장
     * JavaFX Property 값들을 GeneralSettings에 반영하고 저장합니다.
     */
    public void saveSettings() {
        try {
            // JavaFX Property 값을 GeneralSettings에 반영
            GeneralSettings generalSettings = getDefaultSettings();
            generalSettings.setSoundVolume(soundVolume.get());
            generalSettings.setColorMode(colorMode.get());
            generalSettings.setScreenSize(screenSize.get());
            generalSettings.setStageWidth(stageWidth.get());
            generalSettings.setStageHeight(stageHeight.get());
            generalSettings.setDifficulty(difficulty.get());
            
            // YamlConfigPersistence를 통해 저장
            yamlPersistence.saveGeneralSettings(generalSettings);
        } catch (IOException e) {
            System.err.println("❗ Failed to save settings: " + e.getMessage());
        }
    }

    public void restoreDefaults() {
        // application.yml의 기본값 사용 (ClientSettings의 GeneralSettings에서)
        GeneralSettings defaultSettings = getDefaultSettings();
        
        soundVolume.set(defaultSettings.getSoundVolume());
        colorMode.set(defaultSettings.getColorMode());
        screenSize.set(defaultSettings.getScreenSize());
        difficulty.set(defaultSettings.getDifficulty());
        
        applyResolution(defaultSettings.getStageWidth(), defaultSettings.getStageHeight());
        saveSettings(); // 기본값을 YAML 파일에 저장
        
        System.out.println("✅ Settings restored to defaults and saved to " + settingsFilePath);
    }
    
    /**
     * 안전한 기본 설정 가져오기
     */
    private GeneralSettings getDefaultSettings() {
        if (clientSettings != null && clientSettings.getSetting() != null) {
            return clientSettings.getSetting();
        }
        
        // Fallback: 하드코딩된 기본값
        GeneralSettings fallback = new GeneralSettings();
        fallback.setSoundVolume(80.0);
        fallback.setColorMode("colorModeDefault");
        fallback.setScreenSize("screenSizeM");
        fallback.setStageWidth(500.0);
        fallback.setStageHeight(700.0);
        fallback.setDifficulty("difficultyNormal");
        return fallback;
    }

    // Helper method to safely get nested map
    private Map<String, Object> getNestedMap(Map<String, Object> map, String path) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = map;
        for (String key : keys) {
            Object next = current.get(key);
            if (next == null) {
                return new HashMap<>(); // Return empty map if path is invalid
            }
            if (!(next instanceof Map)) {
                System.err.println("⚠️ Expected Map at key '" + key + "' but got " + next.getClass().getSimpleName());
                return new HashMap<>();
            }
            current = (Map<String, Object>) next;
        }
        return current;
    }

    // Helper method to get a setting with a default value
    private <T> T getSetting(Map<String, Object> settings, String key, T defaultValue) {
        Object value = settings.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            // 타입별 안전한 변환
            if (defaultValue instanceof Double) {
                if (value instanceof Number) {
                    return (T) Double.valueOf(((Number) value).doubleValue());
                } else if (value instanceof String) {
                    return (T) Double.valueOf(Double.parseDouble((String) value));
                }
            } else if (defaultValue instanceof Integer) {
                if (value instanceof Number) {
                    return (T) Integer.valueOf(((Number) value).intValue());
                } else if (value instanceof String) {
                    return (T) Integer.valueOf(Integer.parseInt((String) value));
                }
            } else if (defaultValue instanceof String) {
                return (T) String.valueOf(value);
            } else if (defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
        } catch (NumberFormatException | ClassCastException e) {
            System.err.println("⚠️ Failed to convert setting '" + key + "' value '" + value + "' to " + defaultValue.getClass().getSimpleName() + ": " + e.getMessage());
        }
        
        return defaultValue;
    }
    
    // For testing purposes
    void setSettingsFilePaths(String settings, String classic, String arcade) {
        this.settingsFilePath = settings;
        this.classicModeFilePath = classic;
        this.arcadeModeFilePath = arcade;
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
    
    // ✨ Phase 5: 난이도 속성 getter
    public StringProperty difficultyProperty() {
        return difficulty;
    }

    public DoubleProperty stageWidthProperty() {
        return stageWidth;
    }

    public DoubleProperty stageHeightProperty() {
        return stageHeight;
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
     * @deprecated GameModeConfigFactory 사용 권장
     */
    @Deprecated
    public GameModeConfig buildGameModeConfig(GameplayType gameplayType) {
        try {
            // 게임플레이 타입에 따라 Factory 사용
            Difficulty currentDifficulty = getCurrentDifficulty();
            if (gameplayType == GameplayType.ARCADE) {
                return configFactory.createArcadeConfig(currentDifficulty);
            } else {
                return configFactory.createClassicConfig(currentDifficulty);
            }
        } catch (Exception e) {
            System.err.println("❗ Failed to build game mode config: " + e.getMessage());
            e.printStackTrace();
            return configFactory.createClassicConfig(getCurrentDifficulty());
        }
    }
    
    /**
     * 아케이드 모드 설정 빌드
     * 
     * GameModeConfigFactory를 사용하여 game-modes.yml 기반 설정 생성
     * 
     * @return 아케이드 모드 설정
     */
    private GameModeConfig buildArcadeConfig() {
        System.out.println("🎮 [SettingsService] Building ARCADE config...");
        Difficulty currentDifficulty = getCurrentDifficulty();
        return configFactory.createArcadeConfig(currentDifficulty);
    }
    
    
    /**
     * 커스텀 게임 모드 설정 저장 (모든 설정 포함)
     * 
     * @param gameplayType 게임플레이 타입
     * @param config 게임 모드 설정
     */
    public void saveCustomGameModeConfig(GameplayType gameplayType, GameModeConfig config) {
        String filePath = switch (gameplayType) {
            case CLASSIC -> classicModeFilePath;
            case ARCADE -> arcadeModeFilePath;
            default -> null;
        };
        if (filePath == null) {
            System.err.println("❗ Cannot save custom config for unsupported gameplay type: " + gameplayType);
            return;
        }

        try {
            Map<String, Object> data;
            try (FileInputStream in = new FileInputStream(filePath)) {
                data = yaml.load(in);
            } catch (IOException e) {
                data = new LinkedHashMap<>();
            }

            Map<String, Object> client = (Map<String, Object>) data.computeIfAbsent("client", k -> new LinkedHashMap<>());
            Map<String, Object> modes = (Map<String, Object>) client.computeIfAbsent("modes", k -> new LinkedHashMap<>());
            Map<String, Object> modeSettings = (Map<String, Object>) modes.computeIfAbsent(gameplayType.name().toLowerCase(), k -> new LinkedHashMap<>());

            modeSettings.put("srsEnabled", config.isSrsEnabled());
            modeSettings.put("rotation180Enabled", config.isRotation180Enabled());
            modeSettings.put("hardDropEnabled", config.isHardDropEnabled());
            modeSettings.put("holdEnabled", config.isHoldEnabled());
            modeSettings.put("ghostPieceEnabled", config.isGhostPieceEnabled());
            modeSettings.put("dropSpeedMultiplier", config.getDropSpeedMultiplier());
            modeSettings.put("softDropSpeed", config.getSoftDropSpeed());
            modeSettings.put("lockDelay", config.getLockDelay());

            if (gameplayType == GameplayType.ARCADE && config.isItemSystemEnabled()) {
                modeSettings.put("linesPerItem", config.getLinesPerItem());
                modeSettings.put("itemDropRate", config.getItemDropRate());  // Deprecated - 하위 호환성
                modeSettings.put("maxInventorySize", config.getMaxInventorySize());
                modeSettings.put("itemAutoUse", config.isItemAutoUse());
                
                Map<String, Boolean> enabledItems = new LinkedHashMap<>();
                for (seoultech.se.core.engine.item.ItemType itemType : seoultech.se.core.engine.item.ItemType.values()) {
                    enabledItems.put(itemType.name(), config.getEnabledItemTypes().contains(itemType));
                }
                modeSettings.put("enabledItems", enabledItems);
            }

            try (FileWriter writer = new FileWriter(filePath)) {
                yaml.dump(data, writer);
                System.out.println("✅ Custom game mode config saved for " + gameplayType.getDisplayName() + " to " + filePath);
            }
        } catch (IOException e) {
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
        String filePath = switch (gameplayType) {
            case CLASSIC -> classicModeFilePath;
            case ARCADE -> arcadeModeFilePath;
            default -> null;
        };
        if (filePath == null) return null;

        try (FileInputStream in = new FileInputStream(filePath)) {
            Map<String, Object> data = yaml.load(in);
            Map<String, Object> modeSettings = getNestedMap(data, "client.modes." + gameplayType.name().toLowerCase());

            if (modeSettings.isEmpty()) {
                System.out.println("⚠️ No custom settings found for " + gameplayType.getDisplayName());
                return null;
            }

            GameModeConfig.GameModeConfigBuilder builder = GameModeConfig.builder()
                .gameplayType(gameplayType)
                .srsEnabled(getSetting(modeSettings, "srsEnabled", true))
                .rotation180Enabled(getSetting(modeSettings, "rotation180Enabled", false))
                .hardDropEnabled(getSetting(modeSettings, "hardDropEnabled", true))
                .holdEnabled(getSetting(modeSettings, "holdEnabled", true))
                .ghostPieceEnabled(getSetting(modeSettings, "ghostPieceEnabled", true))
                .dropSpeedMultiplier(getSetting(modeSettings, "dropSpeedMultiplier", 1.0))
                .softDropSpeed(getSetting(modeSettings, "softDropSpeed", 20.0))
                .lockDelay(getSetting(modeSettings, "lockDelay", 500));

            if (gameplayType == GameplayType.ARCADE) {
                // 아이템 설정 직접 추가 (ItemConfig 제거)
                java.util.Set<seoultech.se.core.engine.item.ItemType> enabledItems = new java.util.HashSet<>();
                Map<String, Boolean> enabledItemsMap = getSetting(modeSettings, "enabledItems", new HashMap<>());
                for (Map.Entry<String, Boolean> entry : enabledItemsMap.entrySet()) {
                    if (entry.getValue()) {
                        try {
                            enabledItems.add(seoultech.se.core.engine.item.ItemType.valueOf(entry.getKey()));
                        } catch (IllegalArgumentException e) {
                            System.err.println("⚠️ Invalid item type in config: " + entry.getKey());
                        }
                    }
                }
                
                builder.linesPerItem(getSetting(modeSettings, "linesPerItem", 10))
                       .itemDropRate(getSetting(modeSettings, "itemDropRate", 0.1))  // Deprecated - 하위 호환성
                       .maxInventorySize(getSetting(modeSettings, "maxInventorySize", 3))
                       .itemAutoUse(getSetting(modeSettings, "itemAutoUse", false))
                       .enabledItemTypes(enabledItems);
            }
            
            GameModeConfig config = builder.build();
            System.out.println("✅ Loaded custom config for " + gameplayType.getDisplayName());
            return config;

        } catch (Exception e) {
            System.err.println("❗ Failed to load custom game mode config: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // buildItemConfigFromMap() 메서드 제거 - ItemConfig 레거시 제거
    // GameModeConfig.builder()에서 아이템 필드 직접 설정
    
    /**
     * ClientSettings 반환 (외부 접근용)
     * 
     * @return ClientSettings
     */
    public ClientSettings getClientSettings() {
        return clientSettings;
    }
    
    // =========================================================================
    // ✨ Phase 5: 난이도 변환 메서드
    // =========================================================================
    
    /**
     * UI 난이도 ID를 Difficulty enum으로 변환
     * 
     * @return Difficulty enum (EASY, NORMAL, HARD)
     */
    public seoultech.se.core.model.enumType.Difficulty getCurrentDifficulty() {
        String difficultyId = difficulty.get();
        
        if (difficultyId == null || difficultyId.isEmpty()) {
            GeneralSettings defaultSettings = getDefaultSettingsSafely();
            difficultyId = defaultSettings.getDifficulty();
        }
        
        switch (difficultyId) {
            case "difficultyEasy":
                return seoultech.se.core.model.enumType.Difficulty.EASY;
            case "difficultyHard":
                return seoultech.se.core.model.enumType.Difficulty.HARD;
            case "difficultyNormal":
            default:
                return seoultech.se.core.model.enumType.Difficulty.NORMAL;
        }
    }
    
    /**
     * GeneralSettings를 안전하게 가져오는 헬퍼 메서드
     * null일 경우 하드코딩된 기본값 반환
     *
     * @return GeneralSettings (null이 아님을 보장)
     */
    private GeneralSettings getDefaultSettingsSafely() {
        GeneralSettings settings = clientSettings.getSetting();

        if (settings == null) {
            System.err.println("⚠️ ClientSettings.setting is null! Using hardcoded fallback defaults.");

            // 하드코딩된 폴백 기본값
            GeneralSettings fallback = new GeneralSettings();
            fallback.setSoundVolume(80.0);
            fallback.setColorMode("colorModeDefault");
            fallback.setScreenSize("screenSizeM");
            fallback.setStageWidth(500.0);
            fallback.setStageHeight(700.0);
            fallback.setDifficulty("difficultyNormal");

            return fallback;
        }

        return settings;
    }

    // ========== Multiplayer Server Settings ==========

    /**
     * 멀티플레이 서버 기본 URL 가져오기
     *
     * @return 서버 기본 URL (예: "http://localhost:8090")
     */
    public String getServerBaseUrl() {
        // ClientSettings에서 서버 URL 가져오기
        if (clientSettings.getServer() != null && clientSettings.getServer().getBaseUrl() != null) {
            return clientSettings.getServer().getBaseUrl();
        }
        // 폴백 기본값
        return "http://localhost:8090";
    }
}

