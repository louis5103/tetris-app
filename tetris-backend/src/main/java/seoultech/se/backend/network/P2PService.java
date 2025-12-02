package seoultech.se.backend.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import seoultech.se.core.dto.P2PPacket;
import seoultech.se.core.dto.PlayerInputDto;
import seoultech.se.core.dto.ServerStateDto;

/**
 * P2P UDP 통신 서비스
 * 
 * 역할:
 * - UDP 소켓 관리
 * - 직접 데이터 전송/수신 (INPUT, STATE)
 * - Hole Punching
 */
@Service
public class P2PService {
    private DatagramSocket socket;
    private int localPort;
    private InetAddress opponentIp;
    private int opponentPort;
    private volatile boolean isConnected = false;
    private volatile boolean isRunning = false;
    private volatile boolean autoConnectLocked = false; // 명시적 재연결 후 자동 연결 방지
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Consumer<P2PPacket> onPacketReceived;

    @PostConstruct
    public void init() {
        try {
            // 모든 네트워크 인터페이스(0.0.0.0)에서 수신하도록 명시적 바인딩
            this.socket = new DatagramSocket(null);
            this.socket.setReuseAddress(true);
            this.socket.bind(new java.net.InetSocketAddress("0.0.0.0", 0));
            this.localPort = socket.getLocalPort();
            this.isRunning = true;
            
            System.out.println("🔹 [P2P] UDP Socket bound to 0.0.0.0:" + localPort);
            System.out.println("🔹 [P2P] Make sure this port is accessible from other devices");
            
            // 수신 스레드 시작
            Thread receiverThread = new Thread(this::listen);
            receiverThread.setDaemon(true);
            receiverThread.setName("P2P-Receiver");
            receiverThread.start();
            
        } catch (SocketException e) {
            System.err.println("❌ [P2P] Failed to bind UDP socket: " + e.getMessage());
        }
    }

    /**
     * 상대방 연결 정보 설정
     */
    public void connectToPeer(String ip, int port) {
        try {
            this.opponentIp = InetAddress.getByName(ip);
            this.opponentPort = port;
            this.isConnected = true;
            this.autoConnectLocked = true; // 명시적 연결 후 자동 연결 차단
            
            System.out.println("🔹 [P2P] Target set to: " + ip + ":" + port);
            sendPing();
            
        } catch (Exception e) {
            System.err.println("❌ [P2P] Invalid peer address: " + e.getMessage());
        }
    }

    /**
     * 패킷 전송 (공통)
     */
    public void sendPacket(P2PPacket packet) {
        // HANDSHAKE는 초기 연결용이므로 isConnected 체크 우회
        boolean isHandshake = "HANDSHAKE".equals(packet.getType());
        if (!isHandshake && (!isConnected || socket == null || opponentIp == null)) {
            System.err.println("⚠️ [P2P] Cannot send " + packet.getType() + " packet:");
            System.err.println("   └ isConnected: " + isConnected);
            System.err.println("   └ socket: " + (socket != null ? "OK" : "NULL"));
            System.err.println("   └ opponentIp: " + (opponentIp != null ? opponentIp.getHostAddress() : "NULL"));
            System.err.println("   └ opponentPort: " + opponentPort);
            return;
        }
        if (socket == null || opponentIp == null) {
            System.err.println("⚠️ [P2P] Cannot send HANDSHAKE - socket or opponentIp is null");
            return;
        }
        
        try {
            byte[] data = objectMapper.writeValueAsBytes(packet);
            DatagramPacket udpPacket = new DatagramPacket(data, data.length, opponentIp, opponentPort);
            socket.send(udpPacket);
            System.out.println("✉️ [P2P] Packet sent successfully:");
            System.out.println("   └ Type: " + packet.getType());
            System.out.println("   └ Target: " + opponentIp.getHostAddress() + ":" + opponentPort);
            System.out.println("   └ Size: " + data.length + " bytes");
        } catch (Exception e) {
            System.err.println("❌ [P2P] Send error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 입력 데이터 전송 (Wrapper)
     */
    public void sendInput(PlayerInputDto input) {
        System.out.println("📤 [P2P] Sending INPUT packet:");
        System.out.println("   └ connected: " + isConnected);
        System.out.println("   └ socket: " + (socket != null ? "OK" : "NULL"));
        System.out.println("   └ opponentIp: " + (opponentIp != null ? opponentIp.getHostAddress() : "NULL"));
        System.out.println("   └ opponentPort: " + opponentPort);
        System.out.println("   └ command: " + (input != null && input.getCommand() != null ? input.getCommand().getType() : "NULL"));
        sendPacket(P2PPacket.builder()
            .type("INPUT")
            .input(input)
            .build());
    }

    /**
     * 상태 데이터 전송 (Wrapper)
     */
    public void sendState(ServerStateDto state) {
        sendPacket(P2PPacket.builder()
            .type("STATE")
            .state(state)
            .build());
    }

    /**
     * 패킷 수신 콜백 설정
     */
    public void setOnPacketReceived(Consumer<P2PPacket> callback) {
        this.onPacketReceived = callback;
    }

    /**
     * 데이터 수신 루프
     */
    private void listen() {
        byte[] buffer = new byte[32768]; // 버퍼 크기 증가 (State는 클 수 있음)
        while (isRunning && socket != null && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String json = new String(packet.getData(), 0, packet.getLength());
                
                System.out.println("📬 [P2P] Raw packet received from " + 
                    packet.getAddress().getHostAddress() + ":" + packet.getPort() + 
                    " (" + packet.getLength() + " bytes)");
                
                // 🔧 송신자 주소 저장 (HANDSHAKE 패킷용 - 재연결에 필요한 IP 저장)
                // HANDSHAKE는 임시 포트로 올 수 있으므로 IP만 저장하고 포트는 재연결 시 업데이트
                if (json.contains("\"type\":\"HANDSHAKE\"") && opponentIp == null) {
                    opponentIp = packet.getAddress();
                    // 포트는 HANDSHAKE 응답의 udpPort 필드로 업데이트될 예정
                    System.out.println("📍 [P2P] Saved peer IP from HANDSHAKE: " + opponentIp.getHostAddress());
                }
                
                // 🔧 자동 연결 (HANDSHAKE가 아닌 첫 패킷 수신 시만, 명시적 재연결 후에는 차단)
                if (!autoConnectLocked && (opponentPort == 0 || !isConnected) && !json.contains("\"type\":\"HANDSHAKE\"")) {
                    if (opponentIp == null) opponentIp = packet.getAddress();
                    opponentPort = packet.getPort();
                    isConnected = true;
                    System.out.println("🔗 [P2P] Auto-connected to peer: " + 
                        opponentIp.getHostAddress() + ":" + opponentPort);
                }
                
                if (json.equals("PING")) continue;

                if (onPacketReceived != null) {
                    try {
                        P2PPacket p2pPacket = objectMapper.readValue(json, P2PPacket.class);
                        System.out.println("✅ [P2P] Packet parsed successfully: type=" + p2pPacket.getType());
                        onPacketReceived.accept(p2pPacket);
                    } catch (Exception e) {
                        System.err.println("❌ [P2P] JSON parse error: " + e.getMessage());
                        System.err.println("   └ JSON content (first 200 chars): " + 
                            json.substring(0, Math.min(200, json.length())));
                    }
                }
                
            } catch (IOException e) {
                if (isRunning) {
                    // System.err.println("⚠️ [P2P] Receive error: " + e.getMessage());
                }
            }
        }
    }
    
    private void sendPing() {
        if (socket == null || opponentIp == null) return;
        try {
            byte[] data = "PING".getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, opponentIp, opponentPort);
            for (int i = 0; i < 5; i++) {
                socket.send(packet);
                Thread.sleep(100);
            }
        } catch (Exception e) {}
    }

    public int getLocalPort() {
        return localPort;
    }
    
    public String getOpponentIp() {
        return opponentIp != null ? opponentIp.getHostAddress() : null;
    }
    
    @PreDestroy
    public void close() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
