package seoultech.se.backend.mapper;

import org.springframework.stereotype.Component;

import seoultech.se.core.GameState;
import seoultech.se.core.dto.GameStateDto;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.Color;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * GameStateDto → GameState 변환 매퍼
 * 
 * 서버로부터 받은 경량 DTO를 클라이언트에서 사용하는 GameState로 변환
 * 주의: 일부 정보는 손실될 수 있음 (예: itemMarker 등)
 */
@Component
public class GameStateDtoToGameStateMapper {

    /**
     * GameStateDto → GameState 변환
     * 
     * @param dto 서버로부터 받은 GameStateDto
     * @return GameState 객체
     */
    public GameState toGameState(GameStateDto dto) {
        if (dto == null) {
            return null;
        }

        // GameState 생성 (10x20 보드)
        GameState state = new GameState(10, 20);

        // 점수 및 통계 정보 설정
        state.setScore(dto.getScore());
        state.setLevel(dto.getLevel());
        state.setLinesCleared(dto.getLines());
        state.setComboCount(dto.getComboCount());
        state.setBackToBackCount(dto.getBackToBackCount());
        state.setGameOver(dto.isGameOver());
        state.setGameOverReason(dto.getGameOverReason());

        // Grid 변환 (int[][] → Cell[][])
        if (dto.getGrid() != null) {
            int height = dto.getGrid().length;
            int width = height > 0 ? dto.getGrid()[0].length : 0;
            
            for (int row = 0; row < height && row < 20; row++) {
                for (int col = 0; col < width && col < 10; col++) {
                    int cellValue = dto.getGrid()[row][col];
                    if (cellValue > 0 && cellValue < Color.values().length) {
                        // Color의 ordinal()로 변환 (0-7 → Color enum)
                        Color color = Color.values()[cellValue];
                        state.getGrid()[row][col] = Cell.of(color, true);
                    } else {
                        state.getGrid()[row][col] = Cell.empty();
                    }
                }
            }
        }

        // 현재 테트로미노 설정
        if (dto.getCurrentTetromino() != null) {
            GameStateDto.TetrominoDto tetDto = dto.getCurrentTetromino();
            try {
                TetrominoType type = TetrominoType.valueOf(tetDto.getType());
                Tetromino tetromino = new Tetromino(type);
                // 회전 상태 설정
                if (tetDto.getRotation() >= 0 && tetDto.getRotation() < 4) {
                    for (int i = 0; i < tetDto.getRotation(); i++) {
                        tetromino.rotate();
                    }
                }
                state.setCurrentTetromino(tetromino);
                state.setCurrentX(tetDto.getX());
                state.setCurrentY(tetDto.getY());
            } catch (IllegalArgumentException e) {
                System.err.println("⚠️ [GameStateDtoToGameStateMapper] Invalid tetromino type: " + tetDto.getType());
            }
        }

        // Next Queue 설정
        if (dto.getNextPieces() != null) {
            TetrominoType[] nextQueue = new TetrominoType[dto.getNextPieces().length];
            for (int i = 0; i < dto.getNextPieces().length; i++) {
                if (dto.getNextPieces()[i] != null) {
                    try {
                        nextQueue[i] = TetrominoType.valueOf(dto.getNextPieces()[i]);
                    } catch (IllegalArgumentException e) {
                        nextQueue[i] = null;
                    }
                }
            }
            state.setNextQueue(nextQueue);
        }

        // Hold 블록 설정
        if (dto.getHeldPiece() != null) {
            try {
                state.setHeldPiece(TetrominoType.valueOf(dto.getHeldPiece()));
            } catch (IllegalArgumentException e) {
                state.setHeldPiece(null);
            }
        }

        return state;
    }
}

