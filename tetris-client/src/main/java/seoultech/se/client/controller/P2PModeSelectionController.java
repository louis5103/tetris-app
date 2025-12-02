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
    public void handleHostGame() {
        // 호스트 모드로 게임 시작 준비
        // P2P 소켓은 이미 P2PService @PostConstruct에서 열림
        
        String myIp = NetworkUtils.getLocalIpAddress();
        int myPort = p2pService.getLocalPort();
        
        System.out.println("Hosting game setup at " + myIp + ":" + myPort);
        
        // 게임 시작 로직은 MainController에서 transitionToP2PGame()을 통해 수행됨
    }

    @FXML
    public void handleConnectGame() {
        String targetIp = ipAddressField.getText();
        String targetPortStr = portField.getText();
        connectToGame(targetIp, targetPortStr);
    }

    public void connectToGame(String targetIp, String targetPortStr) {
        if (targetIp.isEmpty() || targetPortStr.isEmpty()) {
            // TODO: 에러 처리
            return;
        }

        try {
            int targetPort = Integer.parseInt(targetPortStr);
            
            // 1. 상대방에게 연결 시도 (UDP Hole Punching / Handshake)
            p2pService.connectToPeer(targetIp, targetPort);
            
            // 게임 시작 로직은 MainController에서 수행됨
            
        } catch (NumberFormatException e) {
            // TODO: 포트 번호 에러 처리
        }
    }
}
