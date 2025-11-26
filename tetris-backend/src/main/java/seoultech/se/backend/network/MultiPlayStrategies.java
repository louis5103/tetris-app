package seoultech.se.backend.network;

import java.util.LinkedList;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.dto.PlayerInputDto;
import seoultech.se.core.dto.ServerStateDto;
import seoultech.se.core.engine.GameEngine;

@Component
@RequiredArgsConstructor
public class MultiPlayStrategies {
    private final NetworkClient networkClient;
    private final GameEngine gameEngine;

    private BoardRenderer boardRenderer;  // Setter injection으로 변경

    private final LinkedList<PlayerInputDto> inputBuffer = new LinkedList<>();
    private long localSequence = 0;
    private GameState clientState;
    private String sessionId;

    /**
     * BoardRenderer를 설정합니다.
     * GameController에서 초기화된 BoardRenderer를 주입받습니다.
     *
     * @param boardRenderer GameController에서 생성된 BoardRenderer 인스턴스
     */
    public void setBoardRenderer(BoardRenderer boardRenderer) {
        this.boardRenderer = boardRenderer;
    }

    public void init(String sessionId, GameState initialState) {
        this.sessionId = sessionId;
        this.clientState = initialState;

        networkClient.subscribeToSync(this::onServerUpdate);
    }

    public void excuteCommand(GameCommand command){
        this.clientState = gameEngine.executeCommand(command, this.clientState);

        long seq = ++localSequence;
        PlayerInputDto inputDto = PlayerInputDto.builder()
            .sessionId(sessionId)
            .command(command)
            .sequenceId(seq)
            .build();

        inputBuffer.addLast(inputDto);
        networkClient.sendInput(inputDto);
        boardRenderer.drawBoard(this.clientState);
    }

    private void onServerUpdate(ServerStateDto serverState) {
        long lastAck = serverState.getLastProcessedSequence();
        inputBuffer.removeIf(input -> input.getSequenceId() <= lastAck);

        GameState predictedState = serverState.getMyGameState();

        for(PlayerInputDto input : inputBuffer){
            predictedState = gameEngine.executeCommand(input.getCommand(), predictedState);
        }

        this.clientState = predictedState;
        boardRenderer.drawBoard(this.clientState);

        if(serverState.getOpponentGameState() != null) {
            boardRenderer.drawOpponent(serverState.getOpponentGameState());
        }
    }
}
