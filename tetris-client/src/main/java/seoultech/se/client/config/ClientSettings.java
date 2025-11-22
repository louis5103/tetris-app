package seoultech.se.client.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import seoultech.se.client.config.mode.ArcadeModeSettings;
import seoultech.se.client.config.mode.ClassicModeSettings;

@Component
@ConfigurationProperties(prefix = "client.modes")
@Getter
@Setter
public class ClientSettings {

    private ClassicModeSettings classic;
    private ArcadeModeSettings arcade;

}
