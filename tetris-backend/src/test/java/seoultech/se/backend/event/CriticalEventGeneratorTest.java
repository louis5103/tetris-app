package seoultech.se.backend.event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import seoultech.se.core.GameState;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CriticalEventGenerator 테스트
 */
class CriticalEventGeneratorTest {

    private CriticalEventGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new CriticalEventGenerator();
        generator.reset();
    }

    @Test
    @DisplayName("LINE_CLEAR 이벤트 생성 테스트")
    void generateLineClearEvent() {
        // given
        GameState state = new GameState(10, 20);

        // Reflection으로 필드 설정
        try {
            setField(state, "lastLinesCleared", 4);
            setField(state, "lastScoreEarned", 800L);
            setField(state, "level", 3);
            setField(state, "lastClearedRows", new int[]{16, 17, 18, 19});
        } catch (Exception e) {
            fail("Failed to set fields: " + e.getMessage());
        }

        // when
        List<UIEvent> events = generator.generate(null, state);

        // then
        assertFalse(events.isEmpty());

        // LINE_CLEAR 이벤트 찾기
        UIEvent lineClearEvent = events.stream()
                .filter(e -> e.getType() == UIEventType.LINE_CLEAR)
                .findFirst()
                .orElse(null);

        assertNotNull(lineClearEvent);
        assertEquals(UIEventType.LINE_CLEAR, lineClearEvent.getType());
        assertEquals(15, lineClearEvent.getPriority());
        assertEquals(800L, lineClearEvent.getDuration());

        // 데이터 검증
        assertEquals(4, lineClearEvent.getData().get("lines"));
        assertEquals(800L, lineClearEvent.getData().get("score"));
        assertEquals(3, lineClearEvent.getData().get("level"));
    }

    @Test
    @DisplayName("T_SPIN 이벤트 생성 테스트")
    void generateTSpinEvent() {
        // given
        GameState state = new GameState(10, 20);

        try {
            setField(state, "lastLockWasTSpin", true);
            setField(state, "lastLockWasTSpinMini", false);
            setField(state, "lastLinesCleared", 2);
            setField(state, "level", 5);
        } catch (Exception e) {
            fail("Failed to set fields: " + e.getMessage());
        }

        // when
        List<UIEvent> events = generator.generate(null, state);

        // then
        UIEvent tSpinEvent = events.stream()
                .filter(e -> e.getType() == UIEventType.T_SPIN)
                .findFirst()
                .orElse(null);

        assertNotNull(tSpinEvent);
        assertEquals(UIEventType.T_SPIN, tSpinEvent.getType());
        assertEquals(14, tSpinEvent.getPriority());

        // 데이터 검증
        assertEquals("full", tSpinEvent.getData().get("spinType"));
        assertEquals(2, tSpinEvent.getData().get("lines"));
        assertNotNull(tSpinEvent.getData().get("bonus"));
    }

    @Test
    @DisplayName("COMBO 이벤트 생성 테스트")
    void generateComboEvent() {
        // given
        GameState state = new GameState(10, 20);

        try {
            setField(state, "comboCount", 5);
            setField(state, "level", 2);
            setField(state, "lastLinesCleared", 1); // 콤보를 위해 라인 클리어 필요
        } catch (Exception e) {
            fail("Failed to set fields: " + e.getMessage());
        }

        // when
        List<UIEvent> events = generator.generate(null, state);

        // then
        UIEvent comboEvent = events.stream()
                .filter(e -> e.getType() == UIEventType.COMBO)
                .findFirst()
                .orElse(null);

        assertNotNull(comboEvent);
        assertEquals(UIEventType.COMBO, comboEvent.getType());
        assertEquals(12, comboEvent.getPriority());

        // 데이터 검증
        assertEquals(5, comboEvent.getData().get("combo"));

        // 콤보 보너스 검증 (50 * combo * level)
        long expectedBonus = 50L * 5 * 2;
        assertEquals(expectedBonus, comboEvent.getData().get("bonus"));
    }

    @Test
    @DisplayName("LEVEL_UP 이벤트 생성 테스트")
    void generateLevelUpEvent() {
        // given
        GameState oldState = new GameState(10, 20);
        GameState newState = new GameState(10, 20);

        try {
            setField(oldState, "level", 2);
            setField(newState, "level", 3);
            setField(newState, "linesForNextLevel", 30);
        } catch (Exception e) {
            fail("Failed to set fields: " + e.getMessage());
        }

        // when
        List<UIEvent> events = generator.generate(oldState, newState);

        // then
        UIEvent levelUpEvent = events.stream()
                .filter(e -> e.getType() == UIEventType.LEVEL_UP)
                .findFirst()
                .orElse(null);

        assertNotNull(levelUpEvent);
        assertEquals(UIEventType.LEVEL_UP, levelUpEvent.getType());
        assertEquals(13, levelUpEvent.getPriority());

        // 데이터 검증
        assertEquals(3, levelUpEvent.getData().get("newLevel"));
        assertEquals(30, levelUpEvent.getData().get("requiredLines"));
    }

    @Test
    @DisplayName("PERFECT_CLEAR 이벤트 생성 테스트")
    void generatePerfectClearEvent() {
        // given
        GameState state = new GameState(10, 20);

        try {
            setField(state, "lastIsPerfectClear", true);
            setField(state, "lastLinesCleared", 4);
            setField(state, "level", 4);
        } catch (Exception e) {
            fail("Failed to set fields: " + e.getMessage());
        }

        // when
        List<UIEvent> events = generator.generate(null, state);

        // then
        UIEvent perfectClearEvent = events.stream()
                .filter(e -> e.getType() == UIEventType.PERFECT_CLEAR)
                .findFirst()
                .orElse(null);

        assertNotNull(perfectClearEvent);
        assertEquals(UIEventType.PERFECT_CLEAR, perfectClearEvent.getType());
        assertEquals(16, perfectClearEvent.getPriority());
        assertEquals(2000L, perfectClearEvent.getDuration());

        // 데이터 검증
        assertNotNull(perfectClearEvent.getData().get("bonus"));
        assertEquals(4, perfectClearEvent.getData().get("level"));
    }

    @Test
    @DisplayName("GAME_OVER 이벤트 생성 테스트")
    void generateGameOverEvent() {
        // given
        GameState state = new GameState(10, 20);

        try {
            setField(state, "isGameOver", true);
            setField(state, "score", 5000L);
            setField(state, "level", 5);
            setField(state, "linesCleared", 50);
            setField(state, "gameOverReason", "Top out");
        } catch (Exception e) {
            fail("Failed to set fields: " + e.getMessage());
        }

        // when
        List<UIEvent> events = generator.generate(null, state);

        // then
        assertEquals(1, events.size()); // Game Over 시 다른 이벤트 무시

        UIEvent gameOverEvent = events.get(0);
        assertEquals(UIEventType.GAME_OVER, gameOverEvent.getType());
        assertEquals(20, gameOverEvent.getPriority());
        assertEquals(3000L, gameOverEvent.getDuration());

        // 데이터 검증
        assertEquals(5000L, gameOverEvent.getData().get("finalScore"));
        assertEquals(5, gameOverEvent.getData().get("finalLevel"));
        assertEquals(50, gameOverEvent.getData().get("totalLines"));
        assertEquals("Top out", gameOverEvent.getData().get("reason"));
    }

    @Test
    @DisplayName("복합 이벤트 생성 테스트 - Tetris + Combo")
    void generateMultipleEvents() {
        // given
        GameState state = new GameState(10, 20);

        try {
            setField(state, "lastLinesCleared", 4);
            setField(state, "lastScoreEarned", 800L);
            setField(state, "comboCount", 3);
            setField(state, "level", 3);
        } catch (Exception e) {
            fail("Failed to set fields: " + e.getMessage());
        }

        // when
        List<UIEvent> events = generator.generate(null, state);

        // then
        assertEquals(2, events.size());

        // LINE_CLEAR와 COMBO 이벤트가 모두 생성되어야 함
        assertTrue(events.stream().anyMatch(e -> e.getType() == UIEventType.LINE_CLEAR));
        assertTrue(events.stream().anyMatch(e -> e.getType() == UIEventType.COMBO));
    }

    @Test
    @DisplayName("이벤트 우선순위 정렬 테스트")
    void eventPrioritySorting() {
        // given
        GameState state = new GameState(10, 20);

        try {
            setField(state, "lastLinesCleared", 4);
            setField(state, "lastIsPerfectClear", true);
            setField(state, "comboCount", 5);
            setField(state, "level", 3);
        } catch (Exception e) {
            fail("Failed to set fields: " + e.getMessage());
        }

        // when
        List<UIEvent> events = generator.generate(null, state);

        // then
        // PERFECT_CLEAR(16) > LINE_CLEAR(15) > COMBO(12) 순서여야 함
        assertTrue(events.size() >= 3);

        UIEvent perfectClear = events.stream()
                .filter(e -> e.getType() == UIEventType.PERFECT_CLEAR)
                .findFirst().orElse(null);
        UIEvent lineClear = events.stream()
                .filter(e -> e.getType() == UIEventType.LINE_CLEAR)
                .findFirst().orElse(null);
        UIEvent combo = events.stream()
                .filter(e -> e.getType() == UIEventType.COMBO)
                .findFirst().orElse(null);

        assertNotNull(perfectClear);
        assertNotNull(lineClear);
        assertNotNull(combo);

        assertTrue(perfectClear.getPriority() > lineClear.getPriority());
        assertTrue(lineClear.getPriority() > combo.getPriority());
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
