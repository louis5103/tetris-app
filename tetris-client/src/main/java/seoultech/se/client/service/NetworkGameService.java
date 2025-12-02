package seoultech.se.client.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import seoultech.se.backend.network.P2PService;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.GameEngine;
import seoultech.se.core.engine.factory.GameEngineFactory; // Factory 필요
import seoultech.se.core.engine.item.ItemEngine; // ItemEngine 필요

import java.util.function.Consumer;

/**
 * P2P 게임 로직 관리 서비스
 * 
 * 역할:
 * - 서버 없는 P2P 게임의 "호스트" 역할 수행 (게임 엔진 구동)
 * - "게스트" 역할 수행 (입력 전송 및 상태 수신)
 * - P2PService와 GameController 연결
 */
@Service
public class NetworkGameService {

    @Autowired
    private P2PService p2pService;

    private GameEngine gameEngine; // 호스트일 때만 사용
    private boolean isHost;
    private GameState myState;
    private GameState opponentState;
    
    private Consumer<GameState> onMyStateUpdate;
    private Consumer<GameState> onOpponentStateUpdate;

    /**
     * P2P 게임 시작
     * @param isHost 내가 호스트인지 여부
     */
    public void startP2PGame(boolean isHost) {
        this.isHost = isHost;
        
        if (isHost) {
            // 호스트는 게임 엔진을 직접 생성하여 돌림
            // TODO: GameModeConfig 설정 (UI에서 받아와야 함)
            GameModeConfig config = GameModeConfig.createDefaultClassic();
            
            // GameEngine 생성 (팩토리 패턴 사용 권장)
            // 주의: GameEngine 생성자가 복잡할 수 있음. 기존 코드 참고.
            // 여기서는 간단히 가정.
             this.gameEngine = new seoultech.se.core.engine.impl.ClassicGameEngine(config); 
             // ItemEngine 등 필요한 의존성 주입 필요할 수 있음.
             
             // 게임 루프 시작 (별도 스레드)
             new Thread(this::gameLoop).start();
        }
        
        // P2P 입력 수신 콜백 설정
        p2pService.setOnInputReceived(input -> {
            if (isHost) {
                // 호스트: 상대방(게스트)의 입력을 받아서 엔진에 적용
                processGuestInput(input);
            } else {
                // 게스트: 호스트가 보낸 GameState 수신 (PlayerInputDto를 GameStateDto로 재활용하거나 별도 DTO 필요)
                // 현재 P2PService는 PlayerInputDto만 주고받도록 되어 있어서 수정 필요할 수 있음.
                // 일단은 입력만 주고받는 것으로 가정하고, 상태 동기화는 별도 구현 필요.
            }
        });
    }
    
    private void gameLoop() {
        // 호스트 전용 게임 루프
        while (true) {
            // 1. 내 입력 처리
            // 2. 게스트 입력 처리
            // 3. 중력 적용
            // 4. 상태 업데이트 및 게스트에게 전송
            
            try {
                Thread.sleep(100); // 100ms Tick
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    private void processGuestInput(seoultech.se.core.dto.PlayerInputDto input) {
        // 게스트의 입력을 게임 엔진에 적용
    }
    
    public void sendMyInput(GameCommand command) {
        // 내 입력을 상대에게 전송 (게스트 -> 호스트)
        // 호스트는 내부적으로 처리
    }
}
