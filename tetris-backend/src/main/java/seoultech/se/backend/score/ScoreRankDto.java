package seoultech.se.backend.score;

import java.time.LocalDateTime;
import seoultech.se.core.model.enumType.Difficulty;

public interface ScoreRankDto {
    Integer getRank();
    String getName();
    Integer getScore();
    Difficulty getDifficulty();
    LocalDateTime getCreatedAt();
}
