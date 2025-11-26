package seoultech.se.client.strategy;

import seoultech.se.backend.network.NetworkGameClient;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;

/**
 * 네트워크 실행 전략 (멀티플레이)
 *
 * MultiPlayStrategies를 활용하여 Client-side prediction과
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
     * @param networkGameClient 네트워크 통신을 담당하는 NetworkGameClient 인스턴스
     */
    public NetworkExecutionStrategy(NetworkGameClient multiPlayStrategies) {
        if (multiPlayStrategies == null) {
            throw new IllegalArgumentException("MultiPlayStrategies cannot be null");
        }
        this.networkGameClient = multiPlayStrategies;
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
