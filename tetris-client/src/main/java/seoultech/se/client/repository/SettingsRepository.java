package seoultech.se.client.repository;

import org.springframework.stereotype.Repository;
import seoultech.se.client.model.Setting;

import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

@Repository
public class SettingsRepository {
    private static final String PREFS_NODE = "tetris_custom_settings";
    private final Preferences preferences;

    public SettingsRepository() {
        this.preferences = Preferences.userRoot().node(PREFS_NODE);
    }

    public List<Setting> loadSettings() {
        List<Setting> settings = new ArrayList<>();
        try {
            String[] settingKeys = preferences.childrenNames();
            for (String key : settingKeys) {
                Preferences settingNode = preferences.node(key);
                String name = settingNode.get("name", null);
                if (name != null) {
                    Setting setting = new Setting(name);
                    setting.setSelected(settingNode.getBoolean("selected", false));

                    Map<String, String> configs = new HashMap<>();
                    Preferences configNode = settingNode.node("config");
                    String[] configKeys = configNode.keys();
                    for (String configKey : configKeys) {
                        configs.put(configKey, configNode.get(configKey, null));
                    }
                    setting.setConfigurations(configs);
                    settings.add(setting);
                }
            }
        } catch (BackingStoreException e) {
            System.err.println("Error loading custom settings: " + e.getMessage());
        }
        return settings;
    }

    public void saveSettings(List<Setting> settings) {
        try {
            // Clear existing settings
            for (String child : preferences.childrenNames()) {
                preferences.node(child).removeNode();
            }

            // Save new settings
            for (Setting setting : settings) {
                Preferences settingNode = preferences.node(setting.getKey());
                settingNode.put("name", setting.getName());
                settingNode.putBoolean("selected", setting.isSelected());

                Preferences configNode = settingNode.node("config");
                configNode.clear(); // Clear old configs for this setting
                for (Map.Entry<String, String> entry : setting.getConfigurations().entrySet()) {
                    configNode.put(entry.getKey(), entry.getValue());
                }
            }
            preferences.flush();
        } catch (BackingStoreException e) {
            System.err.println("Error saving custom settings: " + e.getMessage());
        }
    }

    public Setting getActiveSetting() {
        return loadSettings().stream()
            .filter(Setting::isSelected)
            .findFirst()
            .orElse(null);
    }
}
