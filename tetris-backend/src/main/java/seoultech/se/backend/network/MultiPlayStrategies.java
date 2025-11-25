package seoultech.se.backend.network;

import java.util.LinkedList;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import seoultech.se.core.GameEngine;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.dto.PlayerInputDto;
import seoultech.se.core.dto.ServerStateDto;

@Component
@RequiredArgsConstructor
public class MultiPlayStrategies {
    private final NetworkClient networkClient;
    private final GameEngine gameEngine;
    private final BoardRenderer boardRenderer;

    private final LinkedList<PlayerInputDto> inputBuffer = new LinkedList<>();
    private long localSequence = 0;
    private GameState clientState;
    private String sessionId;


    public void init(String sessionId, GameState initialState) {
        this.sessionId = sessionId;
        this.clientState = initialState;

        networkClient.subscribeToSync(this::onServerUpdate);
    }

    public void excuteCommand(GameCommand command){
        this.clientState = gameEngine.executeCommand(this.clientState, command);

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

        for(PlayterInputDto input : inputBuffer){
            predictedState = gameEngine.executeCommand(predictedState, input.getCommand());
        }

        this.clientState = predictedState;
        boardRenderer.drawBoard(this.clientState);

        if(serverState.getOpponentGameState() != null) {
            boardRenderer.drawOpponent(serverState.getOpponentGameState());
        }
    }
}
