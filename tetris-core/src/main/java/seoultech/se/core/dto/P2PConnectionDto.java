package seoultech.se.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class P2PConnectionDto {
    private String sessionId;
    private String playerId;
    private String ipAddress; // 클라이언트의 사설 IP (LAN)
    private int port;         // UDP 수신 포트
    private String type;      // "OFFER" (내 정보 보냄), "ANSWER" (상대 정보 받음/응답)
}
