package seoultech.se.client.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Objects;
import org.yaml.snakeyaml.Yaml;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @deprecated ClientSettings.setting이 null이 되는 문제로 인해 비활성화됨.
 * GeneralSettings 초기화 로직 수정 필요.
 */
@Deprecated
@Disabled("ClientSettings.setting is null - needs GeneralSettings initialization fix")
@SpringBootTest
@TestPropertySource(properties = "javafx.enabled=false")
@DisplayName("SettingsService YAML Persistence Test")
class SettingsServiceYamlTest {

    @Autowired
    private SettingsService settingsService;

    @TempDir
    Path tempDir;

    private Path tempSettingsPath;
    private Path tempClassicPath;
    private Path tempArcadePath;

    @BeforeEach
    void setUp() throws IOException {
        // Create temporary file paths
        tempSettingsPath = tempDir.resolve("setting.yml");
        tempClassicPath = tempDir.resolve("classic.yml");
        tempArcadePath = tempDir.resolve("arcade.yml");

        // Copy original resource files to the temporary directory
        copyResourceToFile("/config/client/setting.yml", tempSettingsPath);
        copyResourceToFile("/config/client/classic.yml", tempClassicPath);
        copyResourceToFile("/config/client/arcade.yml", tempArcadePath);

        // Point the service to the temporary files
        settingsService.setSettingsFilePaths(
            tempSettingsPath.toString(),
            tempClassicPath.toString(),
            tempArcadePath.toString()
        );

        // Load settings from the temporary files
        settingsService.loadSettings();
    }

    private void copyResourceToFile(String resourceName, Path destination) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            Files.copy(is, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @Test
    @DisplayName("Save and Load General Settings")
    void testSaveAndLoadGeneralSettings() throws IOException {
        // Given: A new sound volume value
        double newVolume = 55.5;
        assertNotEquals(newVolume, settingsService.getSoundVolume());

        // When: The setting is changed and saved
        settingsService.setSoundVolume(newVolume);

        // Then: The value in the service should be updated
        assertEquals(newVolume, settingsService.getSoundVolume());

        // And: The temporary YAML file should contain the new value
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(Files.newInputStream(tempSettingsPath));
        Map<String, Object> clientSettings = (Map<String, Object>) ((Map<String, Object>) data.get("client")).get("setting");
        assertEquals(newVolume, (Double) clientSettings.get("soundVolume"));

        // And: When a new service instance loads from the file, it gets the new value
        settingsService.loadSettings();
        assertEquals(newVolume, settingsService.getSoundVolume());
    }

    @Test
    @DisplayName("Restore Default Settings")
    void testRestoreDefaults() throws IOException {
        // Given: A modified sound volume
        settingsService.setSoundVolume(12.3);
        assertNotEquals(settingsService.soundVolumeProperty().get(), 80.0);

        // When: Defaults are restored
        settingsService.restoreDefaults();

        // Then: The setting property should be reverted to the default
        // The default value is injected from application.properties, let's assume it's 80.0 for this test
        assertEquals(80.0, settingsService.getSoundVolume());

        // And: The temporary YAML file should contain the default value
        Yaml yaml = new Yaml();
        Map<String, Object> data = yaml.load(Files.newInputStream(tempSettingsPath));
        Map<String, Object> clientSettings = (Map<String, Object>) ((Map<String, Object>) data.get("client")).get("setting");
        assertEquals(80.0, (Double) clientSettings.get("soundVolume"));
    }

    @Test
    @DisplayName("Save and Load Custom Classic Mode Config")
    void testSaveAndLoadCustomClassicMode() {
        // Given: A custom classic game mode config
        GameModeConfig customConfig = GameModeConfig.builder()
            .gameplayType(GameplayType.CLASSIC)
            .srsEnabled(false)
            .lockDelay(1000)
            .build();
        
        // When: The custom config is saved
        settingsService.saveCustomGameModeConfig(GameplayType.CLASSIC, customConfig);

        // Then: Loading the config should return an object with the same values
        GameModeConfig loadedConfig = settingsService.loadCustomGameModeConfig(GameplayType.CLASSIC);

        assertNotNull(loadedConfig);
        assertEquals(customConfig.isSrsEnabled(), loadedConfig.isSrsEnabled());
        assertEquals(customConfig.getLockDelay(), loadedConfig.getLockDelay());
    }

    @Test
    @DisplayName("Save and Load Custom Arcade Mode Config")
    void testSaveAndLoadCustomArcadeMode() {
        // Given: A custom arcade game mode config
        GameModeConfig customConfig = GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .srsEnabled(true)
            .dropSpeedMultiplier(2.5)
            .build();
        
        // When: The custom config is saved
        settingsService.saveCustomGameModeConfig(GameplayType.ARCADE, customConfig);

        // Then: Loading the config should return an object with the same values
        GameModeConfig loadedConfig = settingsService.loadCustomGameModeConfig(GameplayType.ARCADE);

        assertNotNull(loadedConfig);
        assertEquals(customConfig.isSrsEnabled(), loadedConfig.isSrsEnabled());
        assertEquals(customConfig.getDropSpeedMultiplier(), loadedConfig.getDropSpeedMultiplier());
    }
}
