package seoultech.se.core.model.enumType;

import java.util.Map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WallKickEventData {

    // J, L, S, T, Z Tetrominoes Kick Data
    // 시계 방향 회전 순서
    JLSTZ_SPAWN_TO_RIGHT(new int[][]{{0, 0}, {-1, 0}, {-1, 1}, {0, -2}, {-1, -2}}),
    JLSTZ_RIGHT_TO_REVERSE(new int[][]{{0, 0}, {1, 0}, {1, -1}, {0, 2}, {1, 2}}),
    JLSTZ_REVERSE_TO_LEFT(new int[][]{{0, 0}, {1, 0}, {1, 1}, {0, -2}, {1, -2}}),
    JLSTZ_LEFT_TO_SPAWN(new int[][]{{0, 0}, {-1, 0}, {-1, -1}, {0, 2}, {-1, 2}}),

    // 반시계 방향 회전 순서
    JLSTZ_SPAWN_TO_LEFT(new int[][]{{0, 0}, {1, 0}, {1, 1}, {0, -2}, {1, -2}}),
    JLSTZ_LEFT_TO_REVERSE(new int[][]{{0, 0}, {-1, 0}, {-1, -1}, {0, 2}, {-1, 2}}),
    JLSTZ_REVERSE_TO_RIGHT(new int[][]{{0, 0}, {-1, 0}, {-1, 1}, {0, -2}, {-1, -2}}),
    JLSTZ_RIGHT_TO_SPAWN(new int[][]{{0, 0}, {1, 0}, {1, -1}, {0, 2}, {1, 2}}),


    // I Tetromino Kick Data
    // 시계 방향 회전 순서
    I_SPAWN_TO_RIGHT(new int[][]{{0, 0}, {-2, 0}, {1, 0}, {-2, -1}, {1, 2}}),
    I_RIGHT_TO_REVERSE(new int[][]{{0, 0}, {-1, 0}, {2, 0}, {-1, 2}, {2, -1}}),
    I_REVERSE_TO_LEFT(new int[][]{{0, 0}, {2, 0}, {-1, 0}, {2, 1}, {-1, -2}}),
    I_LEFT_TO_SPAWN(new int[][]{{0, 0}, {1, 0}, {-2, 0}, {1, -2}, {-2, 1}}),

    // 반시계 방향 회전 순서
    I_SPAWN_TO_LEFT(new int[][]{{0, 0}, {-1, 0}, {2, 0}, {-1, 2}, {2, -1}}),
    I_LEFT_TO_REVERSE(new int[][]{{0, 0}, {-2, 0}, {1, 0}, {-2, -1}, {1, 2}}),
    I_REVERSE_TO_RIGHT(new int[][]{{0, 0}, {1, 0}, {-2, 0}, {1, -2}, {-2, 1}}),
    I_RIGHT_TO_SPAWN(new int[][]{{0, 0}, {2, 0}, {-1, 0}, {2, 1}, {-1, -2}});

    private final int[][] offsets;

    public static int[][] getKickData(TetrominoType type, RotationState from, RotationState to) {
        RotationTransition transition = new RotationTransition(from, to);
        WallKickEventData data = (type == TetrominoType.I) ? I_KICKS.get(transition) : JLSTZ_KICKS.get(transition);
        return (data != null) ? data.getOffsets() : new int[][]{{0, 0}};
    }

    // J,L,S,T,Z 블록의 회전 규칙을 담는 Map
    private static final Map<RotationTransition, WallKickEventData> JLSTZ_KICKS = Map.of(
            new RotationTransition(RotationState.SPAWN, RotationState.RIGHT), JLSTZ_SPAWN_TO_RIGHT,
            new RotationTransition(RotationState.RIGHT, RotationState.SPAWN), JLSTZ_RIGHT_TO_SPAWN,
            new RotationTransition(RotationState.RIGHT, RotationState.REVERSE), JLSTZ_RIGHT_TO_REVERSE,
            new RotationTransition(RotationState.REVERSE, RotationState.RIGHT), JLSTZ_REVERSE_TO_RIGHT,
            new RotationTransition(RotationState.REVERSE, RotationState.LEFT), JLSTZ_REVERSE_TO_LEFT,
            new RotationTransition(RotationState.LEFT, RotationState.REVERSE), JLSTZ_LEFT_TO_REVERSE,
            new RotationTransition(RotationState.LEFT, RotationState.SPAWN), JLSTZ_LEFT_TO_SPAWN,
            new RotationTransition(RotationState.SPAWN, RotationState.LEFT), JLSTZ_SPAWN_TO_LEFT
    );

    // I 블록의 회전 규칙을 담는 Map
    private static final Map<RotationTransition, WallKickEventData> I_KICKS = Map.of(
            new RotationTransition(RotationState.SPAWN, RotationState.RIGHT), I_SPAWN_TO_RIGHT,
            new RotationTransition(RotationState.RIGHT, RotationState.SPAWN), I_RIGHT_TO_SPAWN,
            new RotationTransition(RotationState.RIGHT, RotationState.REVERSE), I_RIGHT_TO_REVERSE,
            new RotationTransition(RotationState.REVERSE, RotationState.RIGHT), I_REVERSE_TO_RIGHT,
            new RotationTransition(RotationState.REVERSE, RotationState.LEFT), I_REVERSE_TO_LEFT,
            new RotationTransition(RotationState.LEFT, RotationState.REVERSE), I_LEFT_TO_REVERSE,
            new RotationTransition(RotationState.LEFT, RotationState.SPAWN), I_LEFT_TO_SPAWN,
            new RotationTransition(RotationState.SPAWN, RotationState.LEFT), I_SPAWN_TO_LEFT
    );

    private record RotationTransition(RotationState from, RotationState to) {}
}
