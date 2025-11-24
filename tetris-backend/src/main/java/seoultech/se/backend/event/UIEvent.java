package seoultech.se.backend.event;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * UI 이벤트 DTO
 *
 * 서버에서 생성하여 클라이언트로 전송되는 이벤트
 * 우선순위 기반으로 순차 표시됨
 */
@Data
@Builder
public class UIEvent {

    /**
     * 이벤트 타입
     */
    private UIEventType type;

    /**
     * 우선순위 (높을수록 먼저 표시)
     */
    private int priority;

    /**
     * 표시 시간 (ms)
     */
    private long duration;

    /**
     * 생성 시간
     */
    private long timestamp;

    /**
     * 시퀀스 ID (순서 보장용)
     */
    private int sequenceId;

    /**
     * 이벤트 데이터 (JSON 형태로 전송)
     */
    private Map<String, Object> data;
}
