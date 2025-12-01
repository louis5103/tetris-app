package seoultech.se.core.dto;

import lombok.Builder;
import lombok.Data;

/**
 * GameState DTO for API Response
 *
 * 클라이언트로 전송되는 게임 상태 정보
 * Core의 GameState를 JSON으로 직렬화하기 위한 DTO
 * 경량 버전: Cell[][] 대신 int[][] 사용하여 크기 최적화
 */
@Data
@Builder
public class GameStateDto {

    /**
     * 점수
     */
    private long score;

    /**
     * 레벨
     */
    private int level;

    /**
     * 클리어한 라인 수
     */
    private int lines;

    /**
     * 현재 테트로미노 정보
     */
    private TetrominoDto currentTetromino;

    /**
     * 보드 그리드 (2차원 배열)
     * 0 = 빈 칸, 1-7 = 블록 타입
     */
    private int[][] grid;

    /**
     * 다음 블록 목록 (Next Queue)
     */
    private String[] nextPieces;

    /**
     * 홀드된 블록
     */
    private String heldPiece;

    /**
     * 콤보 카운트
     */
    private int comboCount;

    /**
     * Back-to-Back 카운트
     */
    private int backToBackCount;

    /**
     * 게임 오버 여부
     */
    private boolean gameOver;

    /**
     * 게임 오버 이유
     */
    private String gameOverReason;

    /**
     * 마지막으로 처리된 시퀀스 번호 (멀티플레이용)
     */
    private int lastProcessedSequence;

    /**
     * 현재 테트로미노 DTO
     */
    @Data
    @Builder
    public static class TetrominoDto {
        private String type;  // I, O, T, S, Z, J, L
        private int x;
        private int y;
        private int rotation;
    }
}

