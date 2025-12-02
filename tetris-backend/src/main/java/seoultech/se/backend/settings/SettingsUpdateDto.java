package seoultech.se.backend.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingsUpdateDto {
    private GameLevel gameLevel;
    private ScreenSize screenSize;
    private ColorMode colorMode;
    private String leftKey;
    private String rightKey;
    private String downKey;
    private String hardDownKey;
    private String rotateKey;
    private Integer musicVolume;
}
