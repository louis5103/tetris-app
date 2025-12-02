package seoultech.se.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 매칭 완료 알림 DTO (서버 → 클라이언트)
 *
 * WebSocket을 통해 클라이언트에게 매칭 완료를 알림
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchFoundNotification {
    /**
     * 세션 ID
     */
    private String sessionId;

    /**
     * 상대방 이름
     */
    private String opponentName;

    /**
     * 상대방 이메일
     */
    private String opponentEmail;

    /**
     * 게임 모드
     */
    private String gameplayType;

    /**
     * 게임 시작까지 대기 시간 (초)
     */
    private int countdownSeconds;

    /**
     * 서버 타임스탬프 (밀리초) - 카운트다운 동기화용
     */
    private long serverTimestamp;

    /**
     * 생성 헬퍼 메서드 (타임스탬프 자동 생성)
     */
    public static MatchFoundNotification create(String sessionId, String opponentName, String opponentEmail, String gameplayType) {
        MatchFoundNotification notification = new MatchFoundNotification();
        notification.setSessionId(sessionId);
        notification.setOpponentName(opponentName);
        notification.setOpponentEmail(opponentEmail);
        notification.setGameplayType(gameplayType);
        notification.setCountdownSeconds(3); // 기본 3초
        notification.setServerTimestamp(System.currentTimeMillis()); // 서버 시간
        return notification;
    }

    /**
     * 생성 헬퍼 메서드 (타임스탬프 명시적 지정 - 동기화용)
     */
    public static MatchFoundNotification create(String sessionId, String opponentName, String opponentEmail, String gameplayType, long serverTimestamp) {
        MatchFoundNotification notification = new MatchFoundNotification();
        notification.setSessionId(sessionId);
        notification.setOpponentName(opponentName);
        notification.setOpponentEmail(opponentEmail);
        notification.setGameplayType(gameplayType);
        notification.setCountdownSeconds(3); // 기본 3초
        notification.setServerTimestamp(serverTimestamp); // 명시적 타임스탬프
        return notification;
    }
}
