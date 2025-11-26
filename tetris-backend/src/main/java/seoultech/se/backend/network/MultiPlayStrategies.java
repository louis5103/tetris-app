package seoultech.se.backend.network;

import java.util.LinkedList;
import java.util.function.Consumer;

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

    private final LinkedList<PlayerInputDto> inputBuffer = new LinkedList<>();
    private long localSequence = 0;
    private GameState clientState;
    private String sessionId;
    private Consumer<GameState> opponentStateCallback;

    /**
     * 세션 초기화
     *
     * @param sessionId STOMP 세션 ID
     * @param initialState 초기 게임 상태
     */
    public void init(String sessionId, GameState initialState) {
        this.sessionId = sessionId;
        this.clientState = initialState;

        networkClient.subscribeToSync(this::onServerUpdate);
        System.out.println("✅ MultiPlayStrategies initialized - Session: " + sessionId);
    }

    /**
     * 게임 명령 실행 (Client-side prediction + Server transmission)
     *
     * 1. 로컬에서 즉시 실행 (Client-side prediction)
     * 2. 서버에 전송
     * 3. 예측된 상태를 즉시 반환 (렌더링용)
     * 4. 서버 응답은 onServerUpdate()에서 비동기로 처리
     *
     * @param command 실행할 명령
     * @param currentState 현재 상태 (사용 안 함, 내부 clientState 사용)
     * @return Client-side predicted state
     */
    public GameState executeCommand(GameCommand command, GameState currentState) {
        // 1. Client-side prediction: 로컬에서 즉시 실행
        this.clientState = gameEngine.executeCommand(command, this.clientState);

        // 2. 서버에 전송
        long seq = ++localSequence;
        PlayerInputDto inputDto = PlayerInputDto.builder()
            .sessionId(sessionId)
            .command(command)
            .sequenceId(seq)
            .build();

        inputBuffer.addLast(inputDto);
        networkClient.sendInput(inputDto);

        // 3. 예측된 상태 즉시 반환 (BoardController가 렌더링)
        return this.clientState;
    }

    /**
     * 현재 클라이언트 상태 반환
     *
     * @return 현재 클라이언트의 게임 상태
     */
    public GameState getClientState() {
        return clientState;
    }

    /**
     * 서버로부터 권위 있는 GameState를 받아서 조정 (Server reconciliation)
     *
     * 1. 서버가 처리한 명령까지 input buffer에서 제거
     * 2. 서버의 권위 있는 상태로 시작
     * 3. 아직 서버에서 처리 안 된 입력들을 다시 적용 (Reconciliation)
     *
     * @param serverState 서버로부터 받은 상태 업데이트
     */
    private void onServerUpdate(ServerStateDto serverState) {
        // 1. 서버가 처리한 명령까지 buffer에서 제거
        long lastAck = serverState.getLastProcessedSequence();
        inputBuffer.removeIf(input -> input.getSequenceId() <= lastAck);

        // 2. 서버의 권위 있는 상태로 시작
        GameState predictedState = serverState.getMyGameState();

        // 3. 아직 서버에서 처리 안 된 입력들을 다시 적용 (Reconciliation)
        for(PlayerInputDto input : inputBuffer) {
            predictedState = gameEngine.executeCommand(input.getCommand(), predictedState);
        }

        // 4. 조정된 상태 저장
        this.clientState = predictedState;

        // 렌더링은 GameController가 담당
        // 상대방 상태는 콜백으로 전달
        if(serverState.getOpponentGameState() != null && opponentStateCallback != null) {
            opponentStateCallback.accept(serverState.getOpponentGameState());
            System.out.println("👥 [MultiPlayStrategies] Opponent state forwarded to callback");
        }
    }

    /**
     * ✨ 상대방 상태 업데이트 콜백 설정
     *
     * @param callback 상대방 GameState를 받을 콜백 함수
     */
    public void setOpponentStateCallback(Consumer<GameState> callback) {
        this.opponentStateCallback = callback;
    }

    /**
     * ✨ 네트워크 연결 정리
     *
     * 게임 종료 또는 재시작 시 호출됩니다.
     * 연결을 정리하고 내부 상태를 초기화합니다.
     */
    public void disconnect() {
        // NetworkClient는 별도로 관리되므로 여기서는 내부 상태만 정리
        inputBuffer.clear();
        localSequence = 0;
        clientState = null;
        sessionId = null;
        opponentStateCallback = null;
        System.out.println("✅ MultiPlayStrategies disconnected and cleaned up");
    }
}
