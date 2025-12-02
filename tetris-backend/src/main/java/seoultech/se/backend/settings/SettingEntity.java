package seoultech.se.backend.settings;

import org.hibernate.annotations.Check;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.GenerationType;

@Entity
@Getter
@Setter
@Table(name = "settings")
public class SettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(nullable = false, unique = true) 
    private String settingsName;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private GameLevel gameLevel = GameLevel.NORAML;

    @Enumerated(EnumType.STRING)
    private ScreenSize screenSize = ScreenSize.MEDIUM;

    @Enumerated(EnumType.STRING)
    private ColorMode colorMode = ColorMode.NORMAL; 

    @Column(nullable = false, length = 15)
    private String leftKey;
    
    @Column(nullable = false, length = 15)   
    private String rightKey;

    @Column(nullable = false, length = 15)
    private String downKey;

    @Column(nullable = false, length = 15)
    private String hardDownKey;

    @Column(nullable = false, length = 15)
    private String rotateKey;

    @Column(nullable = false)
    @Check(constraints = "music_volume >= 0 AND music_volume <= 100")
    private int musicVolume = 50;


    
    
}
