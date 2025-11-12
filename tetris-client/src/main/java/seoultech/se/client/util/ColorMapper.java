package seoultech.se.client.util;

import javafx.scene.paint.Color;
import seoultech.se.client.constants.UIConstants;
import seoultech.se.client.constants.ColorBlindMode;

/**
 * 색상 변환 유틸리티 클래스
 * 
 * Core 모듈의 Color enum을 JavaFX Color나 CSS 클래스로 변환합니다.
 * 이를 통해 색상 관리를 중앙화하고 일관성을 유지합니다.
 */
public final class ColorMapper {
    
    // 인스턴스 생성 방지
    private ColorMapper() {
        throw new AssertionError("Cannot instantiate ColorMapper");
    }
    
    // ========== Core Color → JavaFX Color ==========
    
    /**
     * Core 모듈의 Color enum을 JavaFX Color로 변환
     * 
     * @param color Core Color enum
     * @return 대응하는 JavaFX Color
     */
    public static Color toJavaFXColor(seoultech.se.core.model.enumType.Color color) {
        return switch (color) {
            case RED     -> Color.rgb(255, 68, 68);
            case GREEN   -> Color.rgb(68, 255, 68);
            case BLUE    -> Color.rgb(68, 68, 255);
            case YELLOW  -> Color.rgb(255, 255, 68);
            case CYAN    -> Color.rgb(68, 255, 255);
            case MAGENTA -> Color.rgb(255, 68, 255);
            case ORANGE  -> Color.rgb(255, 136, 68);
            default      -> Color.rgb(128, 128, 128); // Gray (default value)
        };
    }
    
    // ========== Core Color → CSS 클래스 ==========
    
    /**
     * Core 모듈의 Color enum을 CSS 클래스 이름으로 변환
     * 
     * @param color Core Color enum
     * @return 대응하는 CSS 클래스 이름, 매칭되지 않으면 null
     */
    public static String toCssClass(seoultech.se.core.model.enumType.Color color) {
        return switch (color) {
            case RED     -> UIConstants.TETROMINO_RED_CLASS;
            case GREEN   -> UIConstants.TETROMINO_GREEN_CLASS;
            case BLUE    -> UIConstants.TETROMINO_BLUE_CLASS;
            case YELLOW  -> UIConstants.TETROMINO_YELLOW_CLASS;
            case CYAN    -> UIConstants.TETROMINO_CYAN_CLASS;
            case MAGENTA -> UIConstants.TETROMINO_MAGENTA_CLASS;
            case ORANGE  -> UIConstants.TETROMINO_ORANGE_CLASS;
            case GRAY    -> UIConstants.TETROMINO_GRAY_CLASS; // 🔥 FIX: WEIGHT_BOMB 전용
            default      -> null;
        };
    }

    public static String toCssClass(seoultech.se.core.model.enumType.Color color, ColorBlindMode mode) {
        String baseClass = toCssClass(color);

        if (baseClass == null || mode == ColorBlindMode.NORMAL) {
            return baseClass;
        }

        // CSS 클래스 이름 규칙: tetromino-<mode>-<color>
        // 예: tetromino-rbblind-red
        String colorName = baseClass.replace("tetromino-", "");
        return "tetromino" + mode.getSuffix() + "-" + colorName;
    }
    

    
    // ========== 특수 색상 접근자 ==========
    
    /**
     * 빈 셀의 배경색 반환
     * 
     * @return 빈 셀 색상
     */
    public static Color getEmptyCellColor() {
        return UIConstants.EMPTY_CELL_COLOR;
    }
    
    /**
     * 셀 테두리 색상 반환
     * 
     * @return 테두리 색상
     */
    public static Color getCellBorderColor() {
        return UIConstants.CELL_BORDER_COLOR;
    }
}
