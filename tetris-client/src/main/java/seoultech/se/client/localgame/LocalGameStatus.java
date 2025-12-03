package seoultech.se.client.game;

import seoultech.se.core.GameState;

/**
 * 로컬 배틀 모드의 두 플레이어 상태를 담는 데이터 클래스.
 */
public class LocalGameStatus {
    private final GameState player1State;
    private final GameState player2State;

    public LocalGameStatus(GameState player1State, GameState player2State) {
        this.player1State = player1State;
        this.player2State = player2State;
    }

    public GameState getPlayer1State() {
        return player1State;
    }

    public GameState getPlayer2State() {
        return player2State;
    }
}
