package seoultech.se.backend.settings;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SettingResponseDto {
    private String settingsName;

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
