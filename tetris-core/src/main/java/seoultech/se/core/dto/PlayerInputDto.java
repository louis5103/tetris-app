package seoultech.se.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import seoultech.se.core.command.GameCommand;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerInputDto {
    private String sessionId;    // 게임 방 ID
    private long sequenceId;     // 입력 순서 (Reconciliation의 핵심)
    private GameCommand command; // 수행할 동작 (MOVE_LEFT, ROTATE 등)
    // JWT 토큰은 WebSocket 헤더로 전달하므로 여기서는 제외 가능하나, 
    // Payload 검증이 필요하다면 포함할 수 있음.
}
