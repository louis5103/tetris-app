package seoultech.se.backend.score;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import seoultech.se.core.model.enumType.Difficulty;

@Getter
@Setter
public class ScoreRequestDto {

    @NotBlank
    @Size(max = 20)
    private String name;

    @NotNull
    @PositiveOrZero
    private int score;

    private Difficulty difficulty;
    private boolean isItemMode;

    public ScoreEntity toEntity() {
        return ScoreEntity.builder()
            .name(this.name)
            .score(this.score)
            .difficulty(this.difficulty)
            .isItemMode(this.isItemMode)
            .build();
    }


}
