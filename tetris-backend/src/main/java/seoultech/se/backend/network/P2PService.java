package seoultech.se.backend.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import seoultech.se.core.dto.PlayerInputDto;

/**
 * P2P UDP 통신 서비스
 * 
 * 역할:
 * - UDP 소켓 관리
 * - 직접 데이터 전송/수신
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
    private Consumer<PlayerInputDto> onInputReceived;

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
     * 상대방 연결 정보 설정 (Signaling 후 호출)
     */
    public void connectToPeer(String ip, int port) {
        try {
            this.opponentIp = InetAddress.getByName(ip);
            this.opponentPort = port;
            this.isConnected = true;
            
            System.out.println("🔹 [P2P] Target set to: " + ip + ":" + port);
            
            // Hole Punching: 상대에게 더미 패킷 전송하여 내 방화벽 열기
            sendPing();
            
        } catch (Exception e) {
            System.err.println("❌ [P2P] Invalid peer address: " + e.getMessage());
        }
    }

    /**
     * 입력 데이터 전송
     */
    public void sendInput(PlayerInputDto input) {
        if (!isConnected || socket == null || opponentIp == null) return;
        
        try {
            // JSON 직렬화 (추후 최적화 가능)
            byte[] data = objectMapper.writeValueAsBytes(input);
            DatagramPacket packet = new DatagramPacket(data, data.length, opponentIp, opponentPort);
            socket.send(packet);
        } catch (Exception e) {
            // UDP는 전송 실패해도 무시 (손실 허용)
            // System.err.println("❌ [P2P] Send failed"); 
        }
    }

    /**
     * 입력 수신 콜백 설정
     */
    public void setOnInputReceived(Consumer<PlayerInputDto> callback) {
        this.onInputReceived = callback;
    }

    /**
     * 데이터 수신 루프
     */
    private void listen() {
        byte[] buffer = new byte[4096]; // 4KB buffer
        while (isRunning && socket != null && !socket.isClosed()) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                // 데이터 파싱
                String json = new String(packet.getData(), 0, packet.getLength());
                
                // PING 무시
                if (json.equals("PING")) {
                    // System.out.println("🔹 [P2P] Received PING from " + packet.getAddress());
                    continue;
                }

                if (onInputReceived != null) {
                    try {
                        PlayerInputDto input = objectMapper.readValue(json, PlayerInputDto.class);
                        onInputReceived.accept(input);
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
    
    /**
     * Hole Punching용 Ping 전송
     */
    private void sendPing() {
        if (socket == null || opponentIp == null) return;
        try {
            byte[] data = "PING".getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, opponentIp, opponentPort);
            
            // 몇 번 보내서 확실하게 뚫기
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
        System.out.println("🔹 [P2P] Socket closed");
    }
}
