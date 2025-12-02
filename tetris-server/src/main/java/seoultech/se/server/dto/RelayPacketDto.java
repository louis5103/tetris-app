package seoultech.se.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 릴레이 서버와 클라이언트 간 통신 패킷
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelayPacketDto {
    private String type;        // "CONNECT", "DATA", "DISCONNECT", "PING"
    private String sessionId;   // 릴레이 세션 ID
    private String playerId;    // 송신자 플레이어 ID
    private String payload;     // 실제 P2P 패킷 데이터 (JSON 문자열)
}
