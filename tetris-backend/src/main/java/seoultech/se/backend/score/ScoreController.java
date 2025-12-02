package seoultech.se.backend.score;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/*
 * 작성자: 문주성
 * 공부할 것: ResponseEntity 반환값 
 */


@RestController
@RequestMapping("/tetris/scores")
@RequiredArgsConstructor
public class ScoreController {

    private final ScoreService scoreService;

    /**
     * player의 점수 DB에 저장
     */
    @PostMapping
    public ResponseEntity<ScoreResponseDto> saveScore(@Valid @RequestBody ScoreRequestDto newScore) {
        ScoreResponseDto responseDto = scoreService.saveScore(newScore);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    /**
     * Score Board 조회
     * @Return 점수 목록 반환
     */
    @GetMapping
    public ResponseEntity<List<ScoreResponseDto>> getScoreBoard() {
        List<ScoreResponseDto> scoreBoard = scoreService.getScoreBoard();
        return ResponseEntity.ok(scoreBoard);
    }

    /**
     * Score Board 초기화
     */
    @DeleteMapping
    public ResponseEntity<Void> deleteScoreBoard() {
        scoreService.deleteScoreBoard();
        return ResponseEntity.noContent().build();
    }

}
