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
    private String type; // "INPUT", "STATE", "HANDSHAKE"
    private PlayerInputDto input;
    private ServerStateDto state; // 이름은 ServerStateDto지만 P2P에서도 사용 (GameStateDto 포함)
}
