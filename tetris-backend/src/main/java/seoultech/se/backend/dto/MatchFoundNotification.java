package seoultech.se.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 매칭 완료 알림 DTO
 *
 * 서버가 WebSocket을 통해 클라이언트에게 매칭 완료를 알림
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
}
