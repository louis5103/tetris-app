package seoultech.se.backend.score;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import seoultech.se.backend.BaseTimeEntity;
import seoultech.se.core.model.enumType.Difficulty;

/*
 * 작성자: 문주성
 * 공부할 것: Builder
 */

@Entity
@Table(name = "scores")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = false)
    private String name;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Difficulty difficulty;

    private boolean isItemMode;
}
