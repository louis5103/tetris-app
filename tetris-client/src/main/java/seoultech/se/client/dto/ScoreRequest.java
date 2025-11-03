package seoultech.se.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreRequest {
    private String name;
    private int score;
    // ENUM or String based on your backend GameMode
    private String gameMode;
    private boolean isItemMode;
}
