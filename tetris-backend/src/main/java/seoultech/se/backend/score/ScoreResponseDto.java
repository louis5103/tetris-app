package seoultech.se.backend.score;


import lombok.Getter;
import lombok.NoArgsConstructor;
import seoultech.se.core.model.enumType.Difficulty;

@Getter
@NoArgsConstructor
public class ScoreResponseDto {

    private String name;
    private int score;
    private Difficulty difficulty;
    private boolean isItemMode;

    public ScoreResponseDto(ScoreEntity entity) {
        this.name = entity.getName();
        this.score = entity.getScore();
        this.difficulty = entity.getDifficulty();
        this.isItemMode = entity.isItemMode();
    }
}
