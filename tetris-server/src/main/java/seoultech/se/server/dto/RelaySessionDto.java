package seoultech.se.server.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;

/**
 * P2P 릴레이 세션 정보
 * 두 클라이언트 간의 릴레이 연결을 관리
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelaySessionDto {
    private String sessionId;           // 고유 세션 ID
    private String playerAId;           // 플레이어 A ID
    private String playerBId;           // 플레이어 B ID
    
    private InetSocketAddress playerAAddress;  // 플레이어 A UDP 주소
    private InetSocketAddress playerBAddress;  // 플레이어 B UDP 주소
    
    private LocalDateTime createdAt;    // 세션 생성 시간
    private LocalDateTime lastActivityAt; // 마지막 패킷 전송 시간
    
    private boolean playerAConnected;   // 플레이어 A 연결 상태
    private boolean playerBConnected;   // 플레이어 B 연결 상태
    
    private long packetCount;           // 중계된 총 패킷 수
    
    /**
     * 세션이 활성 상태인지 확인
     */
    public boolean isActive() {
        return playerAConnected && playerBConnected;
    }
    
    /**
     * 타임아웃 여부 확인 (5분간 활동 없으면 타임아웃)
     */
    public boolean isTimedOut() {
        if (lastActivityAt == null) return false;
        return lastActivityAt.plusMinutes(5).isBefore(LocalDateTime.now());
    }
    
    /**
     * 특정 플레이어의 상대방 주소 반환
     */
    public InetSocketAddress getOpponentAddress(String playerId) {
        if (playerId.equals(playerAId)) {
            return playerBAddress;
        } else if (playerId.equals(playerBId)) {
            return playerAAddress;
        }
        return null;
    }
    
    /**
     * 플레이어 연결 상태 업데이트
     */
    public void updatePlayerConnection(String playerId, InetSocketAddress address) {
        if (playerId.equals(playerAId)) {
            playerAAddress = address;
            playerAConnected = true;
        } else if (playerId.equals(playerBId)) {
            playerBAddress = address;
            playerBConnected = true;
        }
        lastActivityAt = LocalDateTime.now();
    }
}
