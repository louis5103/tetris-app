package seoultech.se.client.constants;

/**
 * 색맹 모드 정의
 */
public enum ColorBlindMode {
    NORMAL("normal", ""),
    RED_GREEN_BLIND("rgblind", "-rgblind"),
    BLUE_YELLOW_BLIND("byblind", "-byblind");

    private final String id;
    private final String suffix;

    ColorBlindMode(String id, String suffix) {
        this.id = id;
        this.suffix = suffix;
    }

    public String getId() {
        return id;
    }

    public String getSuffix() {
        return suffix;
    }
}