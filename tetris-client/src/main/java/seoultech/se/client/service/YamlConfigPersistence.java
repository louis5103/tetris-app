package seoultech.se.client.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import seoultech.se.client.config.ClientSettings;
import seoultech.se.client.config.GeneralSettings;
import seoultech.se.client.config.mode.ArcadeModeSettings;
import seoultech.se.client.config.mode.ClassicModeSettings;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * YAML 설정 파일 저장 전용 컴포넌트
 * 
 * 설정 파일 구조:
 * - config/client/setting.yml: GeneralSettings (기본 설정)
 * - config/client/classic.yml: ClassicModeSettings
 * - config/client/arcade.yml: ArcadeModeSettings
 */
@Component
public class YamlConfigPersistence {
    
    @Value("${spring.config.location:classpath:config/}")
    private String configLocation;
    
    private final Yaml yaml;
    
    public YamlConfigPersistence() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        this.yaml = new Yaml(options);
    }
    
    /**
     * GeneralSettings를 setting.yml에 저장
     */
    public void saveGeneralSettings(GeneralSettings settings) throws IOException {
        Path configPath = resolveConfigPath("client/setting.yml");
        
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> client = new LinkedHashMap<>();
        Map<String, Object> setting = new LinkedHashMap<>();
        
        setting.put("soundVolume", settings.getSoundVolume());
        setting.put("colorMode", settings.getColorMode());
        setting.put("screenSize", settings.getScreenSize());
        setting.put("stageWidth", settings.getStageWidth());
        setting.put("stageHeight", settings.getStageHeight());
        setting.put("difficulty", settings.getDifficulty());
        
        client.put("setting", setting);
        root.put("client", client);
        
        writeYamlFile(configPath, root);
        System.out.println("✅ General settings saved to: " + configPath);
    }
    
    /**
     * ClassicModeSettings를 classic.yml에 저장
     */
    public void saveClassicModeSettings(ClassicModeSettings settings) throws IOException {
        Path configPath = resolveConfigPath("client/classic.yml");
        
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> client = new LinkedHashMap<>();
        Map<String, Object> modes = new LinkedHashMap<>();
        Map<String, Object> classic = new LinkedHashMap<>();
        
        classic.put("srsEnabled", settings.isSrsEnabled());
        classic.put("rotation180Enabled", settings.isRotation180Enabled());
        classic.put("hardDropEnabled", settings.isHardDropEnabled());
        classic.put("holdEnabled", settings.isHoldEnabled());
        classic.put("ghostPieceEnabled", settings.isGhostPieceEnabled());
        classic.put("dropSpeedMultiplier", settings.getDropSpeedMultiplier());
        classic.put("softDropSpeed", settings.getSoftDropSpeed());
        classic.put("lockDelay", settings.getLockDelay());
        classic.put("maxLockResets", settings.getMaxLockResets());
        
        modes.put("classic", classic);
        client.put("modes", modes);
        root.put("client", client);
        
        writeYamlFile(configPath, root);
        System.out.println("✅ Classic mode settings saved to: " + configPath);
    }
    
    /**
     * ArcadeModeSettings를 arcade.yml에 저장
     */
    public void saveArcadeModeSettings(ArcadeModeSettings settings) throws IOException {
        Path configPath = resolveConfigPath("client/arcade.yml");
        
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> client = new LinkedHashMap<>();
        Map<String, Object> modes = new LinkedHashMap<>();
        Map<String, Object> arcade = new LinkedHashMap<>();
        
        arcade.put("srsEnabled", settings.isSrsEnabled());
        arcade.put("rotation180Enabled", settings.isRotation180Enabled());
        arcade.put("hardDropEnabled", settings.isHardDropEnabled());
        arcade.put("holdEnabled", settings.isHoldEnabled());
        arcade.put("ghostPieceEnabled", settings.isGhostPieceEnabled());
        arcade.put("dropSpeedMultiplier", settings.getDropSpeedMultiplier());
        arcade.put("softDropSpeed", settings.getSoftDropSpeed());
        arcade.put("lockDelay", settings.getLockDelay());
        arcade.put("maxLockResets", settings.getMaxLockResets());
        arcade.put("itemDropRate", settings.getItemDropRate());
        arcade.put("maxInventorySize", settings.getMaxInventorySize());
        arcade.put("itemAutoUse", settings.isItemAutoUse());
        arcade.put("enabledItems", settings.getEnabledItems());
        
        modes.put("arcade", arcade);
        client.put("modes", modes);
        root.put("client", client);
        
        writeYamlFile(configPath, root);
        System.out.println("✅ Arcade mode settings saved to: " + configPath);
    }
    
    /**
     * 전체 ClientSettings 저장 (모든 yml 파일에 분산 저장)
     */
    public void saveAllSettings(ClientSettings clientSettings) throws IOException {
        saveGeneralSettings(clientSettings.getSetting());
        saveClassicModeSettings(clientSettings.getModes().getClassic());
        saveArcadeModeSettings(clientSettings.getModes().getArcade());
        System.out.println("✅ All settings saved successfully");
    }
    
    /**
     * 설정 파일 경로 해석
     * classpath: 또는 file: 프로토콜 지원
     */
    private Path resolveConfigPath(String relativePath) throws IOException {
        String location = configLocation.replace("classpath:", "")
                                       .replace("file:", "");
        
        // 절대 경로로 변환
        Path basePath;
        if (location.startsWith("/")) {
            basePath = Paths.get(location);
        } else {
            // 상대 경로인 경우 프로젝트 resources 디렉토리 기준
            basePath = Paths.get("src/main/resources", location);
        }
        
        Path fullPath = basePath.resolve(relativePath);
        
        // 디렉토리가 없으면 생성
        Files.createDirectories(fullPath.getParent());
        
        return fullPath;
    }
    
    /**
     * YAML 파일 쓰기
     */
    private void writeYamlFile(Path path, Map<String, Object> data) throws IOException {
        try (Writer writer = new FileWriter(path.toFile())) {
            yaml.dump(data, writer);
        }
    }
}
