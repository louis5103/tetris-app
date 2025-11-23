package seoultech.se.client.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import seoultech.se.client.config.mode.ArcadeModeSettings;
import seoultech.se.client.config.mode.ClassicModeSettings;

@Component
@ConfigurationProperties(prefix = "client")
@Getter
@Setter
public class ClientSettings {

    private Modes modes;
    private GeneralSettings setting;

    @Getter
    @Setter
    public static class Modes {
        private ClassicModeSettings classic;
        private ArcadeModeSettings arcade;
    }
}
