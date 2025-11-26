package seoultech.se.client.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.stage.Stage;
import seoultech.se.client.config.ClientSettings;
import seoultech.se.client.config.GameModeProperties;
import seoultech.se.client.config.mode.ArcadeModeSettings;
import seoultech.se.client.config.mode.ClassicModeSettings;
import seoultech.se.client.constants.ColorBlindMode;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.engine.mode.PlayType;

@Service
public class SettingsService {

    @Autowired
    private GameModeProperties gameModeProperties;

    @Autowired
    private ClientSettings clientSettings;
    
    // ========== application.yml 기본값 주입 ==========
    
    @Value("${tetris.sound.volume}")
    private double defaultSoundVolume;
    
    @Value("${tetris.ui.color-mode}")
    private String defaultColorMode;
    
    @Value("${tetris.ui.screen-size}")
    private String defaultScreenSize;
    
    @Value("${tetris.ui.stage-width}")
    private double defaultStageWidth;
    
    @Value("${tetris.ui.stage-height}")
    private double defaultStageHeight;
    
    // ✨ Phase 5: 난이도 기본값 추가
    @Value("${tetris.ui.difficulty}")
    private String defaultDifficulty;

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

            soundVolume.set(getSetting(settings, "soundVolume", defaultSoundVolume));
            colorMode.set(getSetting(settings, "colorMode", defaultColorMode));
            screenSize.set(getSetting(settings, "screenSize", defaultScreenSize));
            difficulty.set(getSetting(settings, "difficulty", defaultDifficulty));

            double width = getSetting(settings, "stageWidth", defaultStageWidth);
            double height = getSetting(settings, "stageHeight", defaultStageHeight);

            applyResolution(width, height);
            applyScreenSizeClass();

            System.out.println("✅ Settings loaded successfully from " + settingsFilePath);
        } catch (Exception e) {
            System.out.println("❗ Failed to load settings from " + settingsFilePath + ", using defaults. " + e.getMessage());
            restoreDefaults();
        }
    }

    public void saveSettings() {
        try {
            // 파일을 먼저 읽어옴
            Map<String, Object> data;
            try (FileInputStream in = new FileInputStream(settingsFilePath)) {
                data = yaml.load(in);
            } catch (IOException e) {
                // 파일이 없거나 읽을 수 없으면 새로운 맵 생성
                data = new LinkedHashMap<>();
            }

            // client.setting 경로에 접근 (없으면 생성)
            Map<String, Object> client = (Map<String, Object>) data.computeIfAbsent("client", k -> new LinkedHashMap<>());
            Map<String, Object> settings = (Map<String, Object>) client.computeIfAbsent("setting", k -> new LinkedHashMap<>());

            // 현재 설정 값으로 업데이트
            settings.put("soundVolume", soundVolume.get());
            settings.put("colorMode", colorMode.get());
            settings.put("screenSize", screenSize.get());
            settings.put("stageWidth", stageWidth.get());
            settings.put("stageHeight", stageHeight.get());
            settings.put("difficulty", difficulty.get());
            
            // 파일에 다시 씀
            try (FileWriter writer = new FileWriter(settingsFilePath)) {
                yaml.dump(data, writer);
                System.out.println("✅ Settings saved successfully to " + settingsFilePath);
            }
        } catch (IOException e) {
            System.err.println("❗ Failed to save settings to " + settingsFilePath + ": " + e.getMessage());
        }
    }

    public void restoreDefaults() {
        // application.yml의 기본값 사용
        soundVolume.set(defaultSoundVolume);
        colorMode.set(defaultColorMode);
        screenSize.set(defaultScreenSize);
        difficulty.set(defaultDifficulty);
        
        applyResolution(defaultStageWidth, defaultStageHeight);
        saveSettings(); // 기본값을 YAML 파일에 저장
        
        System.out.println("✅ Settings restored to defaults and saved to " + settingsFilePath);
    }

    // Helper method to safely get nested map
    private Map<String, Object> getNestedMap(Map<String, Object> map, String path) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = map;
        for (String key : keys) {
            current = (Map<String, Object>) current.get(key);
            if (current == null) {
                return new HashMap<>(); // Return empty map if path is invalid
            }
        }
        return current;
    }

    // Helper method to get a setting with a default value
    private <T> T getSetting(Map<String, Object> settings, String key, T defaultValue) {
        Object value = settings.get(key);
        if (value != null && defaultValue.getClass().isInstance(value)) {
            if (defaultValue instanceof Double && value instanceof Integer) {
                return (T) Double.valueOf((Integer) value);
            }
            return (T) value;
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
            
            // 게임플레이 타입에 따라 프리셋 사용
            if (gameplayType == GameplayType.ARCADE) {
                // 아케이드 모드는 아이템 설정 포함
                return buildArcadeConfig();
            } else {
                ClassicModeSettings classicSettings = clientSettings.getModes().getClassic();
                return GameModeConfig.classic(classicSettings.isSrsEnabled());
            }
        } catch (Exception e) {
            System.err.println("❗ Failed to build game mode config: " + e.getMessage());
            e.printStackTrace();
            // 기본값 반환
            return GameModeConfig.classic(true);
        }
    }
    
    /**
     * 아케이드 모드 설정 빌드 (아이템 설정 포함)
     * 
     * @return 아케이드 모드 설정
     */
    private GameModeConfig buildArcadeConfig() {
        System.out.println("🎮 [SettingsService] Building ARCADE config...");
        ArcadeModeSettings arcadeSettings = clientSettings.getModes().getArcade();

        // ItemConfig 생성
        seoultech.se.core.engine.item.ItemConfig itemConfig = buildItemConfig();
        
        System.out.println("✅ ItemConfig created - isEnabled: " + itemConfig.isEnabled());
        
        // 아케이드 모드 기본 설정에 아이템 설정 추가
        return GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .dropSpeedMultiplier(arcadeSettings.getDropSpeedMultiplier())
            .lockDelay(arcadeSettings.getLockDelay())
            .srsEnabled(arcadeSettings.isSrsEnabled())
            .itemConfig(itemConfig)
            .build();
    }
    
    /**
     * ItemConfig 생성
     * GameModeProperties 설정을 기반으로 ItemConfig를 빌드합니다.
     * 
     * @return ItemConfig 객체
     */
    private seoultech.se.core.engine.item.ItemConfig buildItemConfig() {
        // 활성화된 아이템 타입 수집
        java.util.Set<seoultech.se.core.engine.item.ItemType> enabledItems = 
            new java.util.HashSet<>();
        
        for (seoultech.se.core.engine.item.ItemType itemType : 
             seoultech.se.core.engine.item.ItemType.values()) {
            if (gameModeProperties.isItemEnabled(itemType.name())) {
                enabledItems.add(itemType);
            }
        }
        
        System.out.println("📊 Item drop rate: " + (int)(gameModeProperties.getItemDropRate() * 100) + "%");
        System.out.println("📊 Enabled items: " + enabledItems);
        
        return seoultech.se.core.engine.item.ItemConfig.builder()
            .dropRate(gameModeProperties.getItemDropRate())
            .enabledItems(enabledItems)
            .maxInventorySize(gameModeProperties.getMaxInventorySize())
            .autoUse(gameModeProperties.isItemAutoUse())
            .build();
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

            if (gameplayType == GameplayType.ARCADE && config.getItemConfig() != null) {
                seoultech.se.core.engine.item.ItemConfig itemConfig = config.getItemConfig();
                modeSettings.put("itemDropRate", itemConfig.getDropRate());
                modeSettings.put("maxInventorySize", itemConfig.getMaxInventorySize());
                modeSettings.put("itemAutoUse", itemConfig.isAutoUse());
                
                Map<String, Boolean> enabledItems = new LinkedHashMap<>();
                for (seoultech.se.core.engine.item.ItemType itemType : seoultech.se.core.engine.item.ItemType.values()) {
                    enabledItems.put(itemType.name(), itemConfig.getEnabledItems().contains(itemType));
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
                seoultech.se.core.engine.item.ItemConfig itemConfig = buildItemConfigFromMap(modeSettings);
                builder.itemConfig(itemConfig);
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

    private seoultech.se.core.engine.item.ItemConfig buildItemConfigFromMap(Map<String, Object> modeSettings) {
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

        return seoultech.se.core.engine.item.ItemConfig.builder()
            .dropRate(getSetting(modeSettings, "itemDropRate", 0.1))
            .enabledItems(enabledItems)
            .maxInventorySize(getSetting(modeSettings, "maxInventorySize", 3))
            .autoUse(getSetting(modeSettings, "itemAutoUse", false))
            .build();
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
            difficultyId = defaultDifficulty;
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
}

