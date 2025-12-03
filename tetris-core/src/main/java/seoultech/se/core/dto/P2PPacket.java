package seoultech.se.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class P2PPacket {
    private String type; // "INPUT", "STATE", "HANDSHAKE", "GAME_OVER"
    private PlayerInputDto input;
    private ServerStateDto state; // 이름은 ServerStateDto지만 P2P에서도 사용 (GameStateDto 포함)
    private Integer udpPort; // HANDSHAKE 시 실제 UDP 리스닝 포트 전달
    private Boolean gameOver; // GAME_OVER 패킷: 누가 게임 오버되었는지
    private Boolean isWinner; // 게임 종료 시 수신자의 승리 여부
}
