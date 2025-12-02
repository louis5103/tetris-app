package seoultech.se.backend.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seoultech.se.core.dto.GameStateDto;
import seoultech.se.core.GameState;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.Color;
import seoultech.se.core.model.enumType.RotationState;
import seoultech.se.core.model.enumType.TetrominoType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GameStateMapper 테스트
 */
class GameStateMapperTest {

    private GameStateMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GameStateMapper();
    }

    @Test
    @DisplayName("GameState를 GameStateDto로 변환 - 기본 필드")
    void toDtoBasicFields() {
        // given
        GameState state = new GameState(10, 20);

        try {
            setField(state, "score", 1000L);
            setField(state, "level", 3);
            setField(state, "linesCleared", 10);
            setField(state, "comboCount", 2);
            setField(state, "backToBackCount", 1);
            setField(state, "isGameOver", false);
        } catch (Exception e) {
            fail("Failed to set fields: " + e.getMessage());
        }

        // when
        GameStateDto dto = mapper.toDto(state, 5);

        // then
        assertNotNull(dto);
        assertEquals(1000L, dto.getScore());
        assertEquals(3, dto.getLevel());
        assertEquals(10, dto.getLines());
        assertEquals(2, dto.getComboCount());
        assertEquals(1, dto.getBackToBackCount());
        assertFalse(dto.isGameOver());
        assertNull(dto.getGameOverReason());
        assertEquals(5, dto.getLastProcessedSequence());
    }

    @Test
    @DisplayName("GameState를 GameStateDto로 변환 - 게임 오버 상태")
    void toDtoGameOver() {
        // given
        GameState state = new GameState(10, 20);

        try {
            setField(state, "isGameOver", true);
            setField(state, "gameOverReason", "Top out");
        } catch (Exception e) {
            fail("Failed to set fields: " + e.getMessage());
        }

        // when
        GameStateDto dto = mapper.toDto(state, 10);

        // then
        assertTrue(dto.isGameOver());
        assertEquals("Top out", dto.getGameOverReason());
    }

    @Test
    @DisplayName("Tetromino를 TetrominoDto로 변환")
    void toTetromino() throws Exception {
        // given
        GameState state = new GameState(10, 20);
        Tetromino tetromino = new Tetromino(TetrominoType.I);

        setField(state, "currentTetromino", tetromino);
        setField(state, "currentX", 3);
        setField(state, "currentY", 0);

        // when
        GameStateDto dto = mapper.toDto(state, 0);

        // then
        assertNotNull(dto.getCurrentTetromino());
        assertEquals("I", dto.getCurrentTetromino().getType());
        assertEquals(3, dto.getCurrentTetromino().getX());
        assertEquals(0, dto.getCurrentTetromino().getY());
        assertEquals(0, dto.getCurrentTetromino().getRotation());
    }

    @Test
    @DisplayName("Grid를 int 배열로 변환 - 빈 그리드")
    void toGridArrayEmpty() {
        // given
        GameState state = new GameState(10, 20);

        // when
        GameStateDto dto = mapper.toDto(state, 0);

        // then
        assertNotNull(dto.getGrid());
        assertEquals(20, dto.getGrid().length);
        assertEquals(10, dto.getGrid()[0].length);

        // 모든 셀이 0이어야 함
        for (int row = 0; row < 20; row++) {
            for (int col = 0; col < 10; col++) {
                assertEquals(0, dto.getGrid()[row][col]);
            }
        }
    }

    @Test
    @DisplayName("Grid를 int 배열로 변환 - 일부 셀 채워짐")
    void toGridArrayWithOccupiedCells() throws Exception {
        // given
        GameState state = new GameState(10, 20);
        Cell[][] grid = state.getGrid();

        // 일부 셀을 채움
        grid[19][0] = Cell.of(Color.CYAN, true);
        grid[19][1] = Cell.of(Color.RED, true);

        // when
        GameStateDto dto = mapper.toDto(state, 0);

        // then
        assertNotNull(dto.getGrid());

        // 채워진 셀은 Color의 ordinal 값이어야 함
        assertEquals(Color.CYAN.ordinal(), dto.getGrid()[19][0]);
        assertEquals(Color.RED.ordinal(), dto.getGrid()[19][1]);

        // 나머지는 0이어야 함
        assertEquals(0, dto.getGrid()[0][0]);
    }

    @Test
    @DisplayName("NextQueue를 String 배열로 변환")
    void toNextPiecesArray() throws Exception {
        // given
        GameState state = new GameState(10, 20);
        TetrominoType[] nextQueue = new TetrominoType[]{
            TetrominoType.I,
            TetrominoType.T,
            TetrominoType.O
        };

        setField(state, "nextQueue", nextQueue);

        // when
        GameStateDto dto = mapper.toDto(state, 0);

        // then
        assertNotNull(dto.getNextPieces());
        assertEquals(3, dto.getNextPieces().length);
        assertEquals("I", dto.getNextPieces()[0]);
        assertEquals("T", dto.getNextPieces()[1]);
        assertEquals("O", dto.getNextPieces()[2]);
    }

    @Test
    @DisplayName("HeldPiece를 String으로 변환")
    void toHeldPiece() throws Exception {
        // given
        GameState state = new GameState(10, 20);
        setField(state, "heldPiece", TetrominoType.Z);

        // when
        GameStateDto dto = mapper.toDto(state, 0);

        // then
        assertEquals("Z", dto.getHeldPiece());
    }

    @Test
    @DisplayName("HeldPiece가 null일 때")
    void toHeldPieceNull() {
        // given
        GameState state = new GameState(10, 20);

        // when
        GameStateDto dto = mapper.toDto(state, 0);

        // then
        assertNull(dto.getHeldPiece());
    }

    @Test
    @DisplayName("null GameState 처리")
    void toDtoNullState() {
        // when
        GameStateDto dto = mapper.toDto(null, 0);

        // then
        assertNull(dto);
    }

    @Test
    @DisplayName("CurrentTetromino가 null일 때")
    void toDtoNullTetromino() throws Exception {
        // given
        GameState state = new GameState(10, 20);
        setField(state, "currentTetromino", null);

        // when
        GameStateDto dto = mapper.toDto(state, 0);

        // then
        assertNull(dto.getCurrentTetromino());
    }

    @Test
    @DisplayName("Tetromino 회전 상태 변환")
    void toTetrominoWithRotation() throws Exception {
        // given
        GameState state = new GameState(10, 20);
        Tetromino tetromino = new Tetromino(TetrominoType.T);

        // 시계방향 회전 (SPAWN -> RIGHT)
        tetromino.rotate();

        setField(state, "currentTetromino", tetromino);
        setField(state, "currentX", 5);
        setField(state, "currentY", 10);

        // when
        GameStateDto dto = mapper.toDto(state, 0);

        // then
        assertNotNull(dto.getCurrentTetromino());
        assertEquals("T", dto.getCurrentTetromino().getType());
        assertEquals(5, dto.getCurrentTetromino().getX());
        assertEquals(10, dto.getCurrentTetromino().getY());
        assertEquals(RotationState.RIGHT.ordinal(), dto.getCurrentTetromino().getRotation());
    }

    // ========== Helper Methods ==========

    /**
     * Reflection을 사용하여 필드 설정
     */
    private void setField(Object target, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
