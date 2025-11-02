package seoultech.se.client.constants;

import javafx.scene.paint.Color;

/**
 * UI 관련 상수들을 관리하는 클래스
 * 
 * 이 클래스는 JavaFX UI에서 사용되는 모든 상수값들을 중앙에서 관리합니다.
 * 크기, 색상, 타이밍, CSS 클래스 이름 등을 포함합니다.
 */
public final class UIConstants {
    
    // 인스턴스 생성 방지
    private UIConstants() {
        throw new AssertionError("Cannot instantiate UIConstants");
    }
    
    // ========== 셀 크기 상수 ==========
    
    /**
     * 메인 게임 보드의 각 셀 크기 (픽셀)
     */
    public static final double CELL_SIZE = 30.0;
    
    /**
     * Hold/Next 미리보기 영역의 각 셀 크기 (픽셀)
     */
    public static final double PREVIEW_CELL_SIZE = 20.0;
    
    /**
     * 셀 테두리 두께 (픽셀)
     */
    public static final double CELL_BORDER_WIDTH = 0.5;
    
    // ========== 색상 상수 ==========
    
    /**
     * 빈 셀의 배경색
     */
    public static final Color EMPTY_CELL_COLOR = Color.rgb(26, 26, 26);
    
    /**
     * 셀 테두리 색상
     */
    public static final Color CELL_BORDER_COLOR = Color.rgb(51, 51, 51);
    
    // ========== 게임 타이밍 상수 ==========
    
    /**
     * 초기 블록 낙하 간격 (나노초)
     * 500ms = 500,000,000ns
     */
    public static final long INITIAL_DROP_INTERVAL_NS = 500_000_000L;
    
    /**
     * 최소 블록 낙하 간격 (나노초)
     * 100ms = 100,000,000ns
     */
    public static final long MIN_DROP_INTERVAL_NS = 100_000_000L;
    
    /**
     * 레벨당 속도 증가량 (나노초)
     * 50ms = 50,000,000ns
     */
    public static final long DROP_INTERVAL_DECREASE_PER_LEVEL_NS = 50_000_000L;
    
    /**
     * Combo 메시지 표시 시간 (나노초)
     * 3초 = 3,000,000,000ns
     */
    public static final long COMBO_DISPLAY_DURATION_NS = 3_000_000_000L;
    
    /**
     * 일반 알림 메시지 표시 시간 (밀리초)
     * 2초 = 2000ms
     */
    public static final long NOTIFICATION_DISPLAY_DURATION_MS = 2000L;
    
    // ========== CSS 클래스 이름 ==========
    
    /**
     * 보드 셀의 기본 CSS 클래스
     */
    public static final String BOARD_CELL_CLASS = "board-cell";
    
    /**
     * 미리보기 셀의 CSS 클래스
     */
    public static final String PREVIEW_CELL_CLASS = "preview-cell";
    
    // 일반 테트로미노 색상 CSS 클래스
    public static final String TETROMINO_RED_CLASS = "tetromino-red";
    public static final String TETROMINO_GREEN_CLASS = "tetromino-green";
    public static final String TETROMINO_BLUE_CLASS = "tetromino-blue";
    public static final String TETROMINO_YELLOW_CLASS = "tetromino-yellow";
    public static final String TETROMINO_CYAN_CLASS = "tetromino-cyan";
    public static final String TETROMINO_MAGENTA_CLASS = "tetromino-magenta";
    public static final String TETROMINO_ORANGE_CLASS = "tetromino-orange";
    
    // 적록색맹 모드 CSS 클래스
    public static final String TETROMINO_RGBLIND_RED_CLASS = "tetromino-rgblind-red";
    public static final String TETROMINO_RGBLIND_GREEN_CLASS = "tetromino-rgblind-green";
    public static final String TETROMINO_RGBLIND_BLUE_CLASS = "tetromino-rgblind-blue";
    public static final String TETROMINO_RGBLIND_YELLOW_CLASS = "tetromino-rgblind-yellow";
    public static final String TETROMINO_RGBLIND_CYAN_CLASS = "tetromino-rgblind-cyan";
    public static final String TETROMINO_RGBLIND_MAGENTA_CLASS = "tetromino-rgblind-magenta";
    public static final String TETROMINO_RGBLIND_ORANGE_CLASS = "tetromino-rgblind-orange";
    
    // 청황색맹 모드 CSS 클래스
    public static final String TETROMINO_BYBLIND_RED_CLASS = "tetromino-byblind-red";
    public static final String TETROMINO_BYBLIND_GREEN_CLASS = "tetromino-byblind-green";
    public static final String TETROMINO_BYBLIND_BLUE_CLASS = "tetromino-byblind-blue";
    public static final String TETROMINO_BYBLIND_YELLOW_CLASS = "tetromino-byblind-yellow";
    public static final String TETROMINO_BYBLIND_CYAN_CLASS = "tetromino-byblind-cyan";
    public static final String TETROMINO_BYBLIND_MAGENTA_CLASS = "tetromino-byblind-magenta";
    public static final String TETROMINO_BYBLIND_ORANGE_CLASS = "tetromino-byblind-orange";
    
    /**
     * 모든 테트로미노 CSS 클래스 배열
     * (색맹모드 포함, 한 번에 제거하기 위한 배열)
     */
    public static final String[] ALL_TETROMINO_COLOR_CLASSES = {
        // 일반 모드
        TETROMINO_RED_CLASS,
        TETROMINO_GREEN_CLASS,
        TETROMINO_BLUE_CLASS,
        TETROMINO_YELLOW_CLASS,
        TETROMINO_CYAN_CLASS,
        TETROMINO_MAGENTA_CLASS,
        TETROMINO_ORANGE_CLASS,
        // 적록색맹 모드
        TETROMINO_RGBLIND_RED_CLASS,
        TETROMINO_RGBLIND_GREEN_CLASS,
        TETROMINO_RGBLIND_BLUE_CLASS,
        TETROMINO_RGBLIND_YELLOW_CLASS,
        TETROMINO_RGBLIND_CYAN_CLASS,
        TETROMINO_RGBLIND_MAGENTA_CLASS,
        TETROMINO_RGBLIND_ORANGE_CLASS,
        // 청황색맹 모드
        TETROMINO_BYBLIND_RED_CLASS,
        TETROMINO_BYBLIND_GREEN_CLASS,
        TETROMINO_BYBLIND_BLUE_CLASS,
        TETROMINO_BYBLIND_YELLOW_CLASS,
        TETROMINO_BYBLIND_CYAN_CLASS,
        TETROMINO_BYBLIND_MAGENTA_CLASS,
        TETROMINO_BYBLIND_ORANGE_CLASS
    };
    
    // ========== 미리보기 그리드 크기 ==========
    
    /**
     * Hold/Next 미리보기 그리드의 행 수
     */
    public static final int PREVIEW_GRID_ROWS = 4;
    
    /**
     * Hold/Next 미리보기 그리드의 열 수
     */
    public static final int PREVIEW_GRID_COLS = 4;
}
