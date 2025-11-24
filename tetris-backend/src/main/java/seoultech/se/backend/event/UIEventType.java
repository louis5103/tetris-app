package seoultech.se.backend.event;

/**
 * UI 이벤트 타입
 *
 * Critical Events: 서버에서 생성하여 점수 계산 포함
 * Local Events: 클라이언트에서 생성하여 즉시 피드백
 */
public enum UIEventType {
    // Critical Events (서버 생성)
    LINE_CLEAR(15, 800),
    T_SPIN(14, 1000),
    COMBO(12, 600),
    LEVEL_UP(13, 1200),
    PERFECT_CLEAR(16, 2000),
    GAME_OVER(20, 3000),

    // Multiplayer Events (서버 생성)
    ATTACK_SENT(10, 500),
    ATTACK_RECEIVED(10, 1000),

    // Local Events (클라이언트 생성 - 참고용)
    BLOCK_MOVE(1, 50),
    BLOCK_ROTATE(1, 50),
    BLOCK_LOCK(5, 100),
    GHOST_PIECE_UPDATE(1, 50),
    HOLD_SWAP(5, 200);

    private final int defaultPriority;
    private final long defaultDuration;

    UIEventType(int defaultPriority, long defaultDuration) {
        this.defaultPriority = defaultPriority;
        this.defaultDuration = defaultDuration;
    }

    public int getDefaultPriority() {
        return defaultPriority;
    }

    public long getDefaultDuration() {
        return defaultDuration;
    }
}
