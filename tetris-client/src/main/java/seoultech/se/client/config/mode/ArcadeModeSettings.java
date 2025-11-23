package seoultech.se.client.config.mode;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ArcadeModeSettings {
    private boolean srsEnabled;
    private boolean hardDropEnabled;
    private boolean holdEnabled;
    private double dropSpeedMultiplier;
    private int lockDelay;
    private double itemDropRate;
    private int maxInventorySize;
    private boolean itemAutoUse;
    private Map<String, Boolean> enabledItems;
}
