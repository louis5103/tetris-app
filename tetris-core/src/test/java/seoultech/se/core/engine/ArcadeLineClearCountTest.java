package seoultech.se.core.engine;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.Color;
import seoultech.se.core.model.enumType.TetrominoType;

import static org.junit.jupiter.api.Assertions.*;

class ArcadeLineClearCountTest {

    @Test
    @DisplayName("Arcade: normal full-row clear increments linesCleared")
    void arcadeNormalClearIncrementsCounter() {
        GameModeConfig config = GameModeConfig.createDefaultArcade();
        ArcadeGameEngine engine = new ArcadeGameEngine(config);

        GameState state = new GameState(10, 20);

        // Pre-fill bottom row as full
        int bottom = state.getBoardHeight() - 1;
        for (int col = 0; col < state.getBoardWidth(); col++) {
            state.getGrid()[bottom][col] = Cell.of(Color.RED, true);
        }

        // Place a simple O tetromino at top (won't interfere)
        state.setCurrentTetromino(new Tetromino(TetrominoType.O));
        state.setCurrentX(state.getBoardWidth() / 2 - 1);
        state.setCurrentY(0);
        state.setCurrentItemType(null);

        GameState after = engine.lockTetromino(state);

        assertFalse(after.isGameOver(), "Should not be game over");
        assertEquals(1, after.getLastLinesCleared(), "lastLinesCleared should be 1 for the full bottom row");
        assertTrue(after.getLinesCleared() >= 1, "Total linesCleared should have incremented by at least 1");
    }
}
