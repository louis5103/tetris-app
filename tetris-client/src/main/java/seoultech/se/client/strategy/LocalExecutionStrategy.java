package seoultech.se.client.strategy;

import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.engine.GameEngine;

/**
 * 로컬 실행 전략 (싱글플레이)
 *
 * GameEngine을 직접 호출하여 게임 로직을 로컬에서 실행합니다.
 * 네트워크 통신 없이 즉시 결과를 반환합니다.
 *
 * 사용 시나리오:
 * - 싱글플레이 모드 (Classic, Arcade)
 * - 오프라인 게임
 */
public class LocalExecutionStrategy implements GameExecutionStrategy {
    private final GameEngine gameEngine;

    /**
     * LocalExecutionStrategy 생성자
     *
     * @param gameEngine 게임 로직을 처리할 GameEngine 인스턴스
     */
    public LocalExecutionStrategy(GameEngine gameEngine) {
        if (gameEngine == null) {
            throw new IllegalArgumentException("GameEngine cannot be null");
        }
        this.gameEngine = gameEngine;
    }

    /**
     * 명령을 로컬 GameEngine에서 즉시 실행
     *
     * GameEngine.executeCommand() default 메서드를 호출하여
     * Command를 적절한 게임 로직으로 변환하고 실행합니다.
     *
     * @param command 실행할 게임 명령
     * @param currentState 현재 게임 상태
     * @return GameEngine이 처리한 새로운 게임 상태
     */
    @Override
    public GameState execute(GameCommand command, GameState currentState) {
        // GameEngine의 executeCommand() default 메서드 호출
        // 내부적으로 CommandType에 따라 적절한 메서드로 라우팅됨
        return gameEngine.executeCommand(command, currentState);
    }
}
