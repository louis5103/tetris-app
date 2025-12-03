package seoultech.se.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import seoultech.se.server.dto.RelayPacketDto;
import seoultech.se.server.dto.RelaySessionDto;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P2P UDP 릴레이 서비스
 * 
 * 역할:
 * - NAT/방화벽 환경에서 P2P 패킷 중계
 * - 클라이언트 간 직접 연결 불가 시 대안 제공
 * - 학교 와이파이, 모바일 네트워크 등에서 P2P 플레이 가능
 * 
 * 동작 방식:
 * 1. 클라이언트 A → 릴레이 서버: 패킷 전송
 * 2. 릴레이 서버 → 클라이언트 B: 패킷 전달
 * 3. 양방향으로 모든 P2P 트래픽 중계
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class P2PRelayService {
    
    @Value("${relay.udp.port:9090}")
    private int relayPort;
    
    private DatagramSocket socket;
    private volatile boolean isRunning = false;
    private Thread receiverThread;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, RelaySessionDto> sessions = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        try {
            this.socket = new DatagramSocket(null);
            this.socket.setReuseAddress(true);
            this.socket.bind(new InetSocketAddress("0.0.0.0", relayPort));
            this.isRunning = true;
            
            log.info("🔄 [Relay] UDP Relay Server started on port: {}", relayPort);
            
            // 패킷 수신 스레드 시작
            receiverThread = new Thread(this::receiveLoop);
            receiverThread.setDaemon(true);
            receiverThread.setName("P2P-Relay-Receiver");
            receiverThread.start();
            
            // 세션 정리 스레드 시작
            Thread cleanupThread = new Thread(this::cleanupLoop);
            cleanupThread.setDaemon(true);
            cleanupThread.setName("P2P-Relay-Cleanup");
            cleanupThread.start();
            
        } catch (SocketException e) {
            log.error("❌ [Relay] Failed to start relay server: {}", e.getMessage());
        }
    }
    
    /**
     * 새 릴레이 세션 생성
     */
    public RelaySessionDto createSession(String sessionId, String playerAId, String playerBId) {
        RelaySessionDto session = RelaySessionDto.builder()
                .sessionId(sessionId)
                .playerAId(playerAId)
                .playerBId(playerBId)
                .createdAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .playerAConnected(false)
                .playerBConnected(false)
                .packetCount(0L)
                .build();
        
        sessions.put(sessionId, session);
        log.info("✅ [Relay] Session created: {} (A={}, B={})", sessionId, playerAId, playerBId);
        return session;
    }
    
    /**
     * 세션 조회
     */
    public RelaySessionDto getSession(String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * 세션 삭제
     */
    public void removeSession(String sessionId) {
        RelaySessionDto session = sessions.remove(sessionId);
        if (session != null) {
            log.info("🗑️ [Relay] Session removed: {}", sessionId);
        }
    }
    
    /**
     * 패킷 수신 및 중계 루프
     */
    private void receiveLoop() {
        byte[] buffer = new byte[65536]; // 최대 UDP 패킷 크기
        
        while (isRunning && socket != null && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                InetSocketAddress senderAddress = new InetSocketAddress(
                    packet.getAddress(), 
                    packet.getPort()
                );
                
                // 패킷 파싱
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                
                try {
                    RelayPacketDto relayPacket = objectMapper.readValue(data, RelayPacketDto.class);
                    handleRelayPacket(relayPacket, senderAddress);
                } catch (Exception e) {
                    log.warn("⚠️ [Relay] Invalid packet from {}: {}", senderAddress, e.getMessage());
                }
                
            } catch (IOException e) {
                if (isRunning) {
                    log.error("❌ [Relay] Receive error: {}", e.getMessage());
                }
            }
        }
    }
    
    /**
     * 릴레이 패킷 처리
     */
    private void handleRelayPacket(RelayPacketDto relayPacket, InetSocketAddress senderAddress) {
        String sessionId = relayPacket.getSessionId();
        String playerId = relayPacket.getPlayerId();
        String type = relayPacket.getType();

        log.info("📨 [Relay] Received packet: type={}, session={}, player={}, from={}",
            type, sessionId, playerId, senderAddress);
        
        // 세션이 없으면 자동 생성 (CONNECT 타입일 때만)
        RelaySessionDto session = sessions.get(sessionId);
        if (session == null && "CONNECT".equals(type)) {
            log.info("🔨 [Relay] Auto-creating session: {} for player: {}", sessionId, playerId);
            // playerA는 항상 host, playerB는 항상 guest
            String playerAId = "player-host";
            String playerBId = "player-guest";
            session = createSession(sessionId, playerAId, playerBId);
        }
        
        if (session == null) {
            log.warn("⚠️ [Relay] Unknown session: {} (type: {})", sessionId, type);
            return;
        }
        
        switch (type) {
            case "CONNECT":
                // 플레이어 연결 등록
                session.updatePlayerConnection(playerId, senderAddress);
                log.info("🔗 [Relay] Player connected: {} from {}", playerId, senderAddress);
                log.info("   └ Session status: Host={}, Guest={}", 
                    session.isPlayerAConnected(), session.isPlayerBConnected());
                
                // 양쪽 모두 연결되었으면 서로에게 알림
                if (session.isActive()) {
                    log.info("✅ [Relay] Both players connected! Session {} is now active", sessionId);
                    notifyOpponentConnection(session, "player-host");
                    notifyOpponentConnection(session, "player-guest");
                }
                break;
                
            case "DATA":
                // 데이터 패킷 중계 (양쪽 모두 연결되었을 때만)
                if (!session.isActive()) {
                    log.warn("⚠️ [Relay] Cannot relay packet - session not fully active");
                    log.warn("   └ Host connected: {}, Guest connected: {}",
                        session.isPlayerAConnected(), session.isPlayerBConnected());
                    return;
                }

                // 페이로드에서 P2P 패킷 타입 추출 (디버깅용)
                String payload = relayPacket.getPayload();
                if (payload != null && payload.contains("\"type\":")) {
                    int typeStart = payload.indexOf("\"type\":\"") + 8;
                    int typeEnd = payload.indexOf("\"", typeStart);
                    if (typeEnd > typeStart) {
                        String p2pType = payload.substring(typeStart, typeEnd);
                        log.info("   └ P2P packet type: {}", p2pType);
                    }
                }

                relayPacketToOpponent(session, playerId, relayPacket.getPayload());
                session.setPacketCount(session.getPacketCount() + 1);
                session.setLastActivityAt(LocalDateTime.now());
                break;
                
            case "DISCONNECT":
                // 연결 해제
                if (playerId.equals(session.getPlayerAId())) {
                    session.setPlayerAConnected(false);
                } else if (playerId.equals(session.getPlayerBId())) {
                    session.setPlayerBConnected(false);
                }
                log.info("🔌 [Relay] Player disconnected: {}", playerId);
                break;
                
            case "PING":
                // 연결 유지 (활동 시간 갱신)
                session.setLastActivityAt(LocalDateTime.now());
                break;
                
            default:
                log.warn("⚠️ [Relay] Unknown packet type: {}", type);
        }
    }
    
    /**
     * 상대방에게 패킷 전송
     */
    private void relayPacketToOpponent(RelaySessionDto session, String senderId, String payload) {
        InetSocketAddress opponentAddress = session.getOpponentAddress(senderId);
        
        if (opponentAddress == null) {
            log.warn("⚠️ [Relay] Opponent not connected for player: {}", senderId);
            return;
        }
        
        try {
            // payload는 escape된 JSON 문자열이므로 unescape 후 전송
            String unescapedPayload = payload.replace("\\\"", "\"");
            byte[] data = unescapedPayload.getBytes();
            
            DatagramPacket packet = new DatagramPacket(
                data, 
                data.length, 
                opponentAddress
            );
            socket.send(packet);
            
            log.info("📤 [Relay] Relayed {} bytes: {} → {} (payload preview: {}...)", 
                data.length, senderId, opponentAddress, 
                unescapedPayload.substring(0, Math.min(50, unescapedPayload.length())));
                
        } catch (IOException e) {
            log.error("❌ [Relay] Failed to relay packet: {}", e.getMessage());
        }
    }
    
    /**
     * 상대방에게 연결 알림 전송
     */
    private void notifyOpponentConnection(RelaySessionDto session, String connectedPlayerId) {
        InetSocketAddress opponentAddress = session.getOpponentAddress(connectedPlayerId);
        if (opponentAddress == null) return;
        
        try {
            RelayPacketDto notification = RelayPacketDto.builder()
                .type("PEER_CONNECTED")
                .sessionId(session.getSessionId())
                .playerId(connectedPlayerId)
                .build();
            
            byte[] data = objectMapper.writeValueAsBytes(notification);
            DatagramPacket packet = new DatagramPacket(data, data.length, opponentAddress);
            socket.send(packet);
            
        } catch (Exception e) {
            log.error("❌ [Relay] Failed to send connection notification: {}", e.getMessage());
        }
    }
    
    /**
     * 타임아웃된 세션 정리 루프
     */
    private void cleanupLoop() {
        while (isRunning) {
            try {
                Thread.sleep(60000); // 1분마다 체크
                
                sessions.entrySet().removeIf(entry -> {
                    RelaySessionDto session = entry.getValue();
                    if (session.isTimedOut()) {
                        log.info("🗑️ [Relay] Session timed out: {}", entry.getKey());
                        return true;
                    }
                    return false;
                });
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public int getRelayPort() {
        return relayPort;
    }
    
    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
            .filter(RelaySessionDto::isActive)
            .count();
    }
    
    @PreDestroy
    public void close() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (receiverThread != null) {
            receiverThread.interrupt();
        }
        log.info("🛑 [Relay] UDP Relay Server stopped");
    }
}
