package seoultech.se.client.strategy;

import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;

/**
 * 게임 명령 실행 전략 인터페이스
 *
 * BoardController가 로컬/네트워크를 구분하지 않고 명령을 실행할 수 있도록 추상화합니다.
 *
 * Strategy Pattern을 적용하여:
 * - 싱글플레이: LocalExecutionStrategy (GameEngine 직접 호출)
 * - 멀티플레이: NetworkExecutionStrategy (MultiPlayStrategies를 통한 네트워크 전송)
 *
 * 이를 통해 BoardController는 게임 모드에 대한 의존성 없이
 * 단순히 executeCommand()만 호출하면 됩니다.
 */
public interface GameExecutionStrategy {
    /**
     * 게임 명령을 실행하고 새로운 GameState를 반환합니다
     *
     * 로컬 모드: GameEngine이 즉시 처리하여 새로운 상태 반환
     * 네트워크 모드: Client-side prediction 상태를 즉시 반환하고,
     *               서버 응답은 비동기로 reconciliation 수행
     *
     * @param command 실행할 게임 명령
     * @param currentState 현재 게임 상태
     * @return 새로운 게임 상태 (실패 시 원본 상태 반환)
     */
    GameState execute(GameCommand command, GameState currentState);
}
