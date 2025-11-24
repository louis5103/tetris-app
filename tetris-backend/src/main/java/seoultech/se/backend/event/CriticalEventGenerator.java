package seoultech.se.backend.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import seoultech.se.core.GameState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Critical Event 생성기 (서버 측)
 *
 * GameState의 변화를 분석하여 UI 이벤트를 생성합니다.
 * - LINE_CLEAR: 라인 클리어 발생 시
 * - T_SPIN: T-Spin 발생 시
 * - COMBO: 콤보 발생 시
 * - LEVEL_UP: 레벨업 발생 시
 * - PERFECT_CLEAR: Perfect Clear 발생 시
 * - GAME_OVER: 게임 오버 시
 */
@Slf4j
@Component
public class CriticalEventGenerator {

    private final AtomicInteger eventSequenceId = new AtomicInteger(0);

    /**
     * GameState 변화를 분석하여 Critical Events 생성
     *
     * @param oldState 이전 상태 (null일 수 있음)
     * @param newState 새로운 상태
     * @return 생성된 이벤트 리스트
     */
    public List<UIEvent> generate(GameState oldState, GameState newState) {
        List<UIEvent> events = new ArrayList<>();

        if (newState == null) {
            return events;
        }

        // 1. Game Over 체크 (최우선)
        if (newState.isGameOver()) {
            events.add(generateGameOverEvent(newState));
            return events; // Game Over 시 다른 이벤트는 무시
        }

        // 2. Perfect Clear 체크
        if (newState.isLastIsPerfectClear()) {
            events.add(generatePerfectClearEvent(newState));
        }

        // 3. Line Clear 체크
        if (newState.getLastLinesCleared() > 0) {
            events.add(generateLineClearEvent(newState));
        }

        // 4. T-Spin 체크
        if (newState.isLastLockWasTSpin()) {
            events.add(generateTSpinEvent(newState));
        }

        // 5. Combo 체크
        if (newState.getComboCount() > 0) {
            events.add(generateComboEvent(newState));
        }

        // 6. Level Up 체크
        if (oldState != null && newState.getLevel() > oldState.getLevel()) {
            events.add(generateLevelUpEvent(newState));
        } else if (oldState == null && newState.isLastLeveledUp()) {
            // oldState가 없는 경우 lastLeveledUp 플래그 확인
            events.add(generateLevelUpEvent(newState));
        }

        log.debug("Generated {} critical events for state change", events.size());
        return events;
    }

    /**
     * LINE_CLEAR 이벤트 생성
     */
    private UIEvent generateLineClearEvent(GameState state) {
        Map<String, Object> data = new HashMap<>();
        data.put("lines", state.getLastLinesCleared());
        data.put("score", state.getLastScoreEarned());
        data.put("level", state.getLevel());

        // 클리어된 라인 정보 (애니메이션용)
        if (state.getLastClearedRows() != null) {
            data.put("clearedRows", state.getLastClearedRows());
        }

        return UIEvent.builder()
                .type(UIEventType.LINE_CLEAR)
                .priority(UIEventType.LINE_CLEAR.getDefaultPriority())
                .duration(UIEventType.LINE_CLEAR.getDefaultDuration())
                .sequenceId(eventSequenceId.getAndIncrement())
                .timestamp(System.currentTimeMillis())
                .data(data)
                .build();
    }

    /**
     * T_SPIN 이벤트 생성
     */
    private UIEvent generateTSpinEvent(GameState state) {
        Map<String, Object> data = new HashMap<>();

        // T-Spin 타입 결정
        String spinType = state.isLastLockWasTSpinMini() ? "mini" : "full";
        data.put("spinType", spinType);

        // T-Spin으로 인한 라인 클리어
        data.put("lines", state.getLastLinesCleared());

        // 보너스 점수 계산 (T-Spin은 추가 점수를 줌)
        long bonus = calculateTSpinBonus(state);
        data.put("bonus", bonus);

        return UIEvent.builder()
                .type(UIEventType.T_SPIN)
                .priority(UIEventType.T_SPIN.getDefaultPriority())
                .duration(UIEventType.T_SPIN.getDefaultDuration())
                .sequenceId(eventSequenceId.getAndIncrement())
                .timestamp(System.currentTimeMillis())
                .data(data)
                .build();
    }

    /**
     * COMBO 이벤트 생성
     */
    private UIEvent generateComboEvent(GameState state) {
        Map<String, Object> data = new HashMap<>();
        data.put("combo", state.getComboCount());

        // 콤보 보너스 점수 계산
        long bonus = calculateComboBonus(state);
        data.put("bonus", bonus);

        return UIEvent.builder()
                .type(UIEventType.COMBO)
                .priority(UIEventType.COMBO.getDefaultPriority())
                .duration(UIEventType.COMBO.getDefaultDuration())
                .sequenceId(eventSequenceId.getAndIncrement())
                .timestamp(System.currentTimeMillis())
                .data(data)
                .build();
    }

    /**
     * LEVEL_UP 이벤트 생성
     */
    private UIEvent generateLevelUpEvent(GameState state) {
        Map<String, Object> data = new HashMap<>();
        data.put("newLevel", state.getLevel());
        data.put("requiredLines", state.getLinesForNextLevel());

        return UIEvent.builder()
                .type(UIEventType.LEVEL_UP)
                .priority(UIEventType.LEVEL_UP.getDefaultPriority())
                .duration(UIEventType.LEVEL_UP.getDefaultDuration())
                .sequenceId(eventSequenceId.getAndIncrement())
                .timestamp(System.currentTimeMillis())
                .data(data)
                .build();
    }

    /**
     * PERFECT_CLEAR 이벤트 생성
     */
    private UIEvent generatePerfectClearEvent(GameState state) {
        Map<String, Object> data = new HashMap<>();

        // Perfect Clear 보너스 점수 (매우 큼)
        long bonus = calculatePerfectClearBonus(state);
        data.put("bonus", bonus);
        data.put("level", state.getLevel());

        return UIEvent.builder()
                .type(UIEventType.PERFECT_CLEAR)
                .priority(UIEventType.PERFECT_CLEAR.getDefaultPriority())
                .duration(UIEventType.PERFECT_CLEAR.getDefaultDuration())
                .sequenceId(eventSequenceId.getAndIncrement())
                .timestamp(System.currentTimeMillis())
                .data(data)
                .build();
    }

    /**
     * GAME_OVER 이벤트 생성
     */
    private UIEvent generateGameOverEvent(GameState state) {
        Map<String, Object> data = new HashMap<>();
        data.put("finalScore", state.getScore());
        data.put("finalLevel", state.getLevel());
        data.put("totalLines", state.getLinesCleared());

        if (state.getGameOverReason() != null) {
            data.put("reason", state.getGameOverReason());
        }

        return UIEvent.builder()
                .type(UIEventType.GAME_OVER)
                .priority(UIEventType.GAME_OVER.getDefaultPriority())
                .duration(UIEventType.GAME_OVER.getDefaultDuration())
                .sequenceId(eventSequenceId.getAndIncrement())
                .timestamp(System.currentTimeMillis())
                .data(data)
                .build();
    }

    // ========== 보너스 점수 계산 메서드 ==========

    /**
     * T-Spin 보너스 점수 계산
     */
    private long calculateTSpinBonus(GameState state) {
        int lines = state.getLastLinesCleared();
        boolean isMini = state.isLastLockWasTSpinMini();

        // T-Spin 보너스 (기준표)
        if (isMini) {
            // T-Spin Mini
            switch (lines) {
                case 0: return 100 * state.getLevel();
                case 1: return 200 * state.getLevel();
                case 2: return 400 * state.getLevel();
                default: return 0;
            }
        } else {
            // T-Spin (Full)
            switch (lines) {
                case 0: return 400 * state.getLevel();
                case 1: return 800 * state.getLevel();
                case 2: return 1200 * state.getLevel();
                case 3: return 1600 * state.getLevel();
                default: return 0;
            }
        }
    }

    /**
     * 콤보 보너스 점수 계산
     */
    private long calculateComboBonus(GameState state) {
        int combo = state.getComboCount();
        // 콤보 보너스: 50 * 콤보 수 * 레벨
        return 50L * combo * state.getLevel();
    }

    /**
     * Perfect Clear 보너스 점수 계산
     */
    private long calculatePerfectClearBonus(GameState state) {
        // Perfect Clear는 매우 큰 보너스
        int lines = state.getLastLinesCleared();

        switch (lines) {
            case 1: return 800 * state.getLevel();
            case 2: return 1200 * state.getLevel();
            case 3: return 1800 * state.getLevel();
            case 4: return 2000 * state.getLevel(); // Tetris Perfect Clear
            default: return 1000 * state.getLevel();
        }
    }

    /**
     * 시퀀스 ID 초기화 (게임 시작 시)
     */
    public void reset() {
        eventSequenceId.set(0);
        log.debug("Event sequence ID reset");
    }
}
