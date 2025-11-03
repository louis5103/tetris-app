package seoultech.se.client.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ScoreResponse {
    private String name;
    private int score;
    private String gameMode;
    private boolean isItemMode;
}
