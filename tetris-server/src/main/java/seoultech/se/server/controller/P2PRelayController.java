package seoultech.se.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import seoultech.se.server.dto.RelaySessionDto;
import seoultech.se.server.dto.RelaySessionResponseDto;
import seoultech.se.server.service.P2PRelayService;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * P2P 릴레이 REST API
 * 
 * 엔드포인트:
 * - POST /api/relay/session: 릴레이 세션 생성
 * - GET /api/relay/session/{sessionId}: 세션 정보 조회
 * - DELETE /api/relay/session/{sessionId}: 세션 종료
 * - GET /api/relay/status: 릴레이 서버 상태 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/relay")
@RequiredArgsConstructor
public class P2PRelayController {
    
    private final P2PRelayService relayService;
    
    /**
     * 새 릴레이 세션 생성
     * 
     * POST /api/relay/session
     * Body: {
     *   "sessionId": "unique-id",
     *   "playerAId": "player-a",
     *   "playerBId": "player-b"
     * }
     */
    @PostMapping("/session")
    public ResponseEntity<RelaySessionResponseDto> createSession(
            @RequestBody Map<String, String> request) {
        
        String sessionId = request.get("sessionId");
        String playerAId = request.get("playerAId");
        String playerBId = request.get("playerBId");
        
        if (sessionId == null || playerAId == null || playerBId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        try {
            RelaySessionDto session = relayService.createSession(sessionId, playerAId, playerBId);
            
            // 릴레이 서버 주소 가져오기
            String serverAddress = InetAddress.getLocalHost().getHostAddress();
            
            RelaySessionResponseDto response = RelaySessionResponseDto.builder()
                    .sessionId(session.getSessionId())
                    .playerAId(session.getPlayerAId())
                    .playerBId(session.getPlayerBId())
                    .relayServerAddress(serverAddress)
                    .relayServerPort(relayService.getRelayPort())
                    .status("WAITING")
                    .packetCount(0L)
                    .build();
            
            log.info("✅ [Relay API] Session created: {}", sessionId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ [Relay API] Failed to create session: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 세션 정보 조회
     * 
     * GET /api/relay/session/{sessionId}
     */
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<RelaySessionResponseDto> getSession(@PathVariable String sessionId) {
        RelaySessionDto session = relayService.getSession(sessionId);
        
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            String serverAddress = InetAddress.getLocalHost().getHostAddress();
            String status = session.isActive() ? "ACTIVE" : "WAITING";
            
            RelaySessionResponseDto response = RelaySessionResponseDto.builder()
                    .sessionId(session.getSessionId())
                    .playerAId(session.getPlayerAId())
                    .playerBId(session.getPlayerBId())
                    .relayServerAddress(serverAddress)
                    .relayServerPort(relayService.getRelayPort())
                    .status(status)
                    .packetCount(session.getPacketCount())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("❌ [Relay API] Failed to get session: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 세션 삭제
     * 
     * DELETE /api/relay/session/{sessionId}
     */
    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        relayService.removeSession(sessionId);
        log.info("🗑️ [Relay API] Session deleted: {}", sessionId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 릴레이 서버 상태 조회
     * 
     * GET /api/relay/status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("active", true);
            status.put("port", relayService.getRelayPort());
            status.put("activeSessions", relayService.getActiveSessionCount());
            status.put("serverAddress", InetAddress.getLocalHost().getHostAddress());
            
            return ResponseEntity.ok(status);
            
        } catch (Exception e) {
            log.error("❌ [Relay API] Failed to get status: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
