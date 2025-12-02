package seoultech.se.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import seoultech.se.backend.network.P2PService;
import seoultech.se.client.service.NetworkGameService;
import seoultech.se.client.util.NetworkUtils;

@Controller
public class P2PModeSelectionController extends BaseController {

    @FXML private TextField ipAddressField;
    @FXML private TextField portField;
    @FXML private Button connectButton;
    @FXML private Button hostButton;

    @Autowired
    private P2PService p2pService;

    @Autowired
    private NetworkGameService networkGameService;

    @FXML
    public void initialize() {
        String myIp = NetworkUtils.getLocalIpAddress();
        // TODO: P2PService에서 포트를 가져와서 보여주면 좋을듯
        System.out.println("My IP: " + myIp);
    }

    @FXML
    private void handleHostGame() {
        // 호스트 모드로 게임 시작
        // 1. P2P 소켓 열기 (이미 P2PService @PostConstruct에서 열림)
        // 2. 대기 화면으로 전환 (내 IP/Port 표시)
        // 3. 상대방 접속 대기
        
        String myIp = NetworkUtils.getLocalIpAddress();
        int myPort = p2pService.getLocalPort();
        
        System.out.println("Hosting game at " + myIp + ":" + myPort);
        // TODO: 대기 화면으로 전환 로직
        
        // 임시: 바로 게임 시작 (상대방 연결 대기 로직 필요)
        networkGameService.startP2PGame(true); // true = isHost
    }

    @FXML
    private void handleConnectGame() {
        String targetIp = ipAddressField.getText();
        String targetPortStr = portField.getText();

        if (targetIp.isEmpty() || targetPortStr.isEmpty()) {
            // TODO: 에러 처리
            return;
        }

        try {
            int targetPort = Integer.parseInt(targetPortStr);
            
            // 1. 상대방에게 연결 시도 (UDP Hole Punching / Handshake)
            p2pService.connectToPeer(targetIp, targetPort);
            
            // 2. 게임 시작 (Guest 모드)
            networkGameService.startP2PGame(false); // false = isHost
            
        } catch (NumberFormatException e) {
            // TODO: 포트 번호 에러 처리
        }
    }
}
