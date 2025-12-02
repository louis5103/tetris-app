package seoultech.se.backend.dto;

import lombok.Builder;
import lombok.Data;
import seoultech.se.backend.event.UIEvent;

import java.util.List;

/**
 * 게임 업데이트 응답 DTO
 *
 * Command 처리 후 서버가 클라이언트에게 전송하는 응답
 * - 처리 성공 여부
 * - 최신 게임 상태 (GameState)
 * - 발생한 Critical Events 목록
 */
@Data
@Builder
public class GameUpdateResponse {

    /**
     * 처리 성공 여부
     */
    private boolean success;

    /**
     * 처리된 시퀀스 번호
     */
    private int sequenceNumber;

    /**
     * 응답 타임스탬프
     */
    private long timestamp;

    /**
     * 최신 게임 상태
     */
    private GameStateDto state;

    /**
     * 발생한 Critical Events
     * (LINE_CLEAR, T_SPIN, COMBO, LEVEL_UP 등)
     */
    private List<UIEvent> events;

    /**
     * 오류 메시지 (success = false일 때)
     */
    private String errorMessage;

    /**
     * 오류 코드 (success = false일 때)
     */
    private String errorCode;
}
