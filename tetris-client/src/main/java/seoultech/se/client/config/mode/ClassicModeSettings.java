package seoultech.se.client.config.mode;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
public class ClassicModeSettings {
    private boolean srsEnabled;
    private boolean hardDropEnabled;
    private boolean holdEnabled;
    private double dropSpeedMultiplier;
    private int lockDelay;
}
