package seoultech.se.client.strategy;

import java.util.function.Consumer;

import seoultech.se.backend.network.NetworkGameClient;
import seoultech.se.backend.network.NetworkTemplate;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;

/**
 * 네트워크 실행 전략 (멀티플레이)
 *
 * NetworkGameClient를 활용하여 Client-side prediction과
 * Server reconciliation을 수행합니다.
 *
 * 실행 흐름:
 * 1. Client-side prediction: 로컬에서 즉시 명령 실행
 * 2. 서버 전송: STOMP를 통해 명령을 서버에 전송
 * 3. 즉시 반환: Predicted state를 BoardController에 반환 (렌더링용)
 * 4. Server reconciliation: 서버 응답을 비동기로 처리하여 상태 조정
 *
 * 사용 시나리오:
 * - 멀티플레이 모드
 * - 온라인 대전
 */
public class NetworkExecutionStrategy implements GameExecutionStrategy {
    private final NetworkGameClient networkGameClient;

    /**
     * NetworkExecutionStrategy 생성자
     *
     * @param networkTemplate 네트워크 통신 템플릿 (현재 사용 안함, 향후 확장용)
     * @param networkGameClient 네트워크 게임 클라이언트 (Client-side prediction 담당)
     */
    public NetworkExecutionStrategy(
            NetworkTemplate networkTemplate,
            NetworkGameClient networkGameClient) {
        if (networkTemplate == null) {
            throw new IllegalArgumentException("NetworkTemplate cannot be null");
        }
        if (networkGameClient == null) {
            throw new IllegalArgumentException("NetworkGameClient cannot be null");
        }
        // networkTemplate은 향후 확장을 위해 검증만 수행
        this.networkGameClient = networkGameClient;
    }

    /**
     * 세션 초기화 및 멀티플레이 모드 설정
     *
     * @param sessionId STOMP 세션 ID
     * @param initialState 초기 게임 상태
     * @param opponentStateCallback 상대방 상태 업데이트 콜백
     * @param attackLinesCallback 공격 라인 수신 콜백
     */
    public void setupMultiplayMode(
            String sessionId,
            GameState initialState,
            Consumer<GameState> opponentStateCallback,
            Consumer<Integer> attackLinesCallback) {
        // NetworkGameClient 초기화
        networkGameClient.init(sessionId, initialState);
        
        // 콜백 설정
        networkGameClient.setOpponentStateCallback(opponentStateCallback);
        networkGameClient.setAttackLinesCallback(attackLinesCallback);
        
        System.out.println("✅ NetworkExecutionStrategy initialized - Session: " + sessionId);
    }

    /**
     * 명령을 네트워크를 통해 실행
     *
     * NetworkGameClient.executeCommand()를 호출하여:
     * 1. 로컬에서 Client-side prediction 수행
     * 2. 서버에 명령 전송
     * 3. Predicted state를 즉시 반환
     *
     * 서버의 권위 있는 응답은 NetworkGameClient.onServerUpdate()에서
     * 비동기로 처리되어 상태를 조정합니다.
     *
     * @param command 실행할 게임 명령
     * @param currentState 현재 게임 상태 (NetworkGameClient 내부 상태 사용)
     * @return Client-side predicted 게임 상태
     */
    @Override
    public GameState execute(GameCommand command, GameState currentState) {
        // NetworkGameClient에 명령 전달
        // 내부적으로 Client-side prediction + 서버 전송 처리
        return networkGameClient.executeCommand(command, currentState);
    }
}
