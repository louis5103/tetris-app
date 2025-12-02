package seoultech.se.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 릴레이 세션 생성/조회 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelaySessionResponseDto {
    private String sessionId;
    private String playerAId;
    private String playerBId;
    private String relayServerAddress;  // 릴레이 서버 주소
    private int relayServerPort;        // 릴레이 서버 UDP 포트
    private String status;              // "WAITING", "ACTIVE", "CLOSED"
    private Long packetCount;
}
