package seoultech.se.backend.settings;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingRequestDto {
    
    private String settingsName;
    private String email;

    private GameLevel gameLevel;
    private ScreenSize screenSize;
    private ColorMode colorMode;
    private int musicVolume;

    private String leftKey;
    private String rightKey;
    private String downKey;
    private String hardDownKey;
    private String rotateKey;

}
