package seoultech.se.backend.mapper;

import org.springframework.stereotype.Component;
import seoultech.se.backend.dto.GameStateDto;
import seoultech.se.core.GameState;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * GameState Mapper
 *
 * Core의 GameState를 DTO로 변환하는 매퍼
 */
@Component
public class GameStateMapper {

    /**
     * GameState → GameStateDto 변환
     *
     * @param state Core의 GameState
     * @param lastProcessedSequence 마지막 처리된 시퀀스 번호 (멀티플레이용)
     * @return GameStateDto
     */
    public GameStateDto toDto(GameState state, int lastProcessedSequence) {
        if (state == null) {
            return null;
        }

        return GameStateDto.builder()
                .score(state.getScore())
                .level(state.getLevel())
                .lines(state.getLinesCleared())
                .currentTetromino(toTetrominoDto(state.getCurrentTetromino(),
                        state.getCurrentX(), state.getCurrentY()))
                .grid(toGridArray(state.getGrid()))
                .nextPieces(toNextPiecesArray(state.getNextQueue()))
                .heldPiece(toTypeString(state.getHeldPiece()))
                .comboCount(state.getComboCount())
                .backToBackCount(state.getBackToBackCount())
                .gameOver(state.isGameOver())
                .gameOverReason(state.getGameOverReason())
                .lastProcessedSequence(lastProcessedSequence)
                .build();
    }

    /**
     * Tetromino → TetrominoDto 변환
     */
    private GameStateDto.TetrominoDto toTetrominoDto(Tetromino tetromino, int x, int y) {
        if (tetromino == null) {
            return null;
        }

        return GameStateDto.TetrominoDto.builder()
                .type(tetromino.getType().name())
                .x(x)
                .y(y)
                .rotation(tetromino.getRotationState().ordinal())
                .build();
    }

    /**
     * Cell[][] → int[][] 변환
     */
    private int[][] toGridArray(Cell[][] grid) {
        if (grid == null) {
            return new int[0][0];
        }

        int height = grid.length;
        int width = grid[0].length;
        int[][] result = new int[height][width];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                Cell cell = grid[row][col];
                if (cell == null || !cell.isOccupied()) {
                    result[row][col] = 0;
                } else {
                    // Color의 ordinal() + 1로 블록 색상 표현
                    // NONE=0, 기타 색상=1~7
                    result[row][col] = cell.getColor().ordinal();
                }
            }
        }

        return result;
    }

    /**
     * TetrominoType[] → String[] 변환
     */
    private String[] toNextPiecesArray(TetrominoType[] nextQueue) {
        if (nextQueue == null) {
            return new String[0];
        }

        String[] result = new String[nextQueue.length];
        for (int i = 0; i < nextQueue.length; i++) {
            result[i] = nextQueue[i] != null ? nextQueue[i].name() : null;
        }
        return result;
    }

    /**
     * TetrominoType → String 변환
     */
    private String toTypeString(TetrominoType type) {
        return type != null ? type.name() : null;
    }
}
