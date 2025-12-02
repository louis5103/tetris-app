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
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Consumer<P2PPacket> onPacketReceived;

    @PostConstruct
    public void init() {
        try {
            // 빈 포트 자동 할당
            this.socket = new DatagramSocket();
            this.localPort = socket.getLocalPort();
            this.isRunning = true;
            
            System.out.println("🔹 [P2P] UDP Socket bound to port: " + localPort);
            
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
        if (!isConnected || socket == null || opponentIp == null) return;
        
        try {
            byte[] data = objectMapper.writeValueAsBytes(packet);
            DatagramPacket udpPacket = new DatagramPacket(data, data.length, opponentIp, opponentPort);
            socket.send(udpPacket);
        } catch (Exception e) {
            // UDP 전송 실패는 무시 (로그 최소화)
        }
    }

    /**
     * 입력 데이터 전송 (Wrapper)
     */
    public void sendInput(PlayerInputDto input) {
        sendPacket(new P2PPacket("INPUT", input, null));
    }

    /**
     * 상태 데이터 전송 (Wrapper)
     */
    public void sendState(ServerStateDto state) {
        sendPacket(new P2PPacket("STATE", null, state));
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
                
                if (json.equals("PING")) continue;

                if (onPacketReceived != null) {
                    try {
                        P2PPacket p2pPacket = objectMapper.readValue(json, P2PPacket.class);
                        onPacketReceived.accept(p2pPacket);
                    } catch (Exception e) {
                        // JSON 파싱 에러 무시
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
    
    @PreDestroy
    public void close() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}