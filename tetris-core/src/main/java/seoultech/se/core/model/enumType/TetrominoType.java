package seoultech.se.core.model.enumType;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.MODULE)
public enum TetrominoType {
    I(new int[][]{
            {0, 0, 0, 0},
            {1, 1, 1, 1},
            {0, 0, 0, 0},
            {0, 0, 0, 0}
    }, Color.CYAN, 2, 1),  // 🔥 FIX: pivotX=2, pivotY=1 (블록이 있는 row에 pivot 배치)

    J(new int[][]{
            {0, 1, 0},
            {0, 1, 0},
            {1, 1, 0}
    }, Color.BLUE, 1, 1),

    L(new int[][]{
            {0, 1, 0},
            {0, 1, 0},
            {0, 1, 1}
    }, Color.ORANGE, 1, 1),

    O(new int[][]{
            {1, 1},
            {1, 1}
    }, Color.YELLOW, 0, 0),

    S(new int[][]{
            {0, 0, 0},
            {0, 1, 1},
            {1, 1, 0}
    }, Color.GREEN, 1, 1),

    T(new int[][]{
            {0, 1, 0},
            {1, 1, 1},
            {0, 0, 0}
    }, Color.MAGENTA, 1, 1),

    Z(new int[][]{
            {0, 0, 0},
            {1, 1, 0},
            {0, 1, 1}
    }, Color.RED, 1, 1),
    
    /**
     * 아이템 블록 (1칸짜리)
     * 아이템 사용 시 테트로미노가 이 타입으로 변환됩니다.
     */
    ITEM(new int[][]{
            {1}
    }, Color.NONE, 0, 0),
    
    /**
     * 무게추 아이템 (Phase 4)
     * 
     * Req2 명세:
     * - 총 4칸 너비의 특수 블록
     * - 초기: 좌우 이동 가능
     * - 바닥/블록에 닿으면: 좌우 이동 불가, 아래로만 이동
     * - 떨어지면서 아래에 있는 모든 블록 제거
     * 
     * 형태:
     *   OO
     *   OOOO
     */
    WEIGHT_BOMB(new int[][]{
            {0, 1, 1, 0},
            {1, 1, 1, 1}
    }, Color.GRAY, 1, 0);  // pivotX=1, pivotY=0

    public final int [][] shape;
    public final Color color;
    public final int pivotX;
    public final int pivotY;


    public static TetrominoType getRandomTetrominoType() {
        // ITEM, WEIGHT_BOMB 타입은 랜덤 생성에서 제외
        TetrominoType[] types = {I, J, L, O, S, T, Z};
        int randomIndex = (int) (Math.random() * types.length);
        return types[randomIndex];
    }
}
