package seoultech.se.core;

/**
 * 게임 전반에 사용되는 상수들을 정의한 클래스
 * 
 * 이 클래스는 매직 넘버를 제거하고 의미를 명확하게 하기 위해 만들어졌습니다.
 * 모든 상수는 public static final로 선언되어 어디서든 접근 가능합니다.
 */
public final class GameConstants {
    
    // 인스턴스화 방지
    private GameConstants() {
        throw new AssertionError("GameConstants는 인스턴스화할 수 없습니다.");
    }
    
    // ========== Hard Drop 관련 ==========
    
    /**
     * Hard Drop 시 1칸당 획득하는 점수
     */
    public static final int HARD_DROP_SCORE_PER_CELL = 2;
    
    // ========== 기본 라인 클리어 점수 ==========
    
    /**
     * 1줄 클리어 기본 점수 (Single)
     */
    public static final int SCORE_SINGLE = 100;
    
    /**
     * 2줄 클리어 기본 점수 (Double)
     */
    public static final int SCORE_DOUBLE = 300;
    
    /**
     * 3줄 클리어 기본 점수 (Triple)
     */
    public static final int SCORE_TRIPLE = 500;
    
    /**
     * 4줄 클리어 기본 점수 (Tetris)
     */
    public static final int SCORE_TETRIS = 800;
    
    // ========== T-Spin 점수 ==========
    
    /**
     * T-Spin Mini (라인 클리어 없음) 점수
     */
    public static final int TSPIN_MINI_NO_LINE = 100;
    
    /**
     * T-Spin Mini Single 점수
     */
    public static final int TSPIN_MINI_SINGLE = 200;
    
    /**
     * T-Spin Mini Double 점수
     */
    public static final int TSPIN_MINI_DOUBLE = 400;
    
    /**
     * T-Spin (라인 클리어 없음) 점수
     */
    public static final int TSPIN_NO_LINE = 400;
    
    /**
     * T-Spin Single 점수
     */
    public static final int TSPIN_SINGLE = 800;
    
    /**
     * T-Spin Double 점수
     */
    public static final int TSPIN_DOUBLE = 1200;
    
    /**
     * T-Spin Triple 점수
     */
    public static final int TSPIN_TRIPLE = 1600;
    
    // ========== Perfect Clear 보너스 ==========
    
    /**
     * Perfect Clear Single 보너스 점수
     */
    public static final int PERFECT_CLEAR_SINGLE = 800;
    
    /**
     * Perfect Clear Double 보너스 점수
     */
    public static final int PERFECT_CLEAR_DOUBLE = 1200;
    
    /**
     * Perfect Clear Triple 보너스 점수
     */
    public static final int PERFECT_CLEAR_TRIPLE = 1800;
    
    /**
     * Perfect Clear Tetris 보너스 점수
     */
    public static final int PERFECT_CLEAR_TETRIS = 2000;
    
    // ========== 보너스 배수 및 계수 ==========
    
    /**
     * Back-to-Back 보너스 배수 (1.5배)
     */
    public static final double BACK_TO_BACK_MULTIPLIER = 1.5;
    
    /**
     * 콤보 1단계당 획득하는 점수 계수 (레벨과 곱해짐)
     */
    public static final int COMBO_BONUS_PER_LEVEL = 50;
    
    // ========== 라인 클리어 관련 ==========
    
    /**
     * Tetris 라인 클리어 수 (4줄)
     * B2B 판정에 사용됩니다
     */
    public static final int TETRIS_LINE_COUNT = 4;
}
