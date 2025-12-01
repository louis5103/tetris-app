package seoultech.se.backend.network;

import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.dto.PlayerInputDto;
import seoultech.se.core.dto.ServerStateDto;

/**
 * 네트워크 게임 클라이언트 (Thin Client)
 *
 * 책임:
 * - 사용자 입력을 서버로 전송
 * - 서버로부터 권위 있는 GameState 수신 및 콜백 전달
 * - UI 렌더링을 위한 상태 중계
 *
 * 변경 사항 (Thin Client 리팩토링):
 * - ❌ Client-side Prediction 제거 (게임 로직 실행 안함)
 * - ❌ Input Buffer 제거 (Reconciliation 불필요)
 * - ❌ GameEngine 의존성 제거
 * - ✅ 단순 입력 전송 및 서버 상태 수신만 담당
 */
@Component
@RequiredArgsConstructor
public class NetworkGameClient {
    private final NetworkTemplate networkClient;

    private long localSequence = 0;
    private GameState clientState; // 서버로부터 받은 최신 상태 (렌더링용)
    private String sessionId;
    private Consumer<GameState> myStateCallback; // ✨ 자신의 보드 업데이트 콜백
    private Consumer<GameState> opponentStateCallback;
    private Consumer<Integer> attackLinesCallback;

    /**
     * 세션 초기화
     *
     * @param sessionId STOMP 세션 ID
     * @param initialState 초기 게임 상태
     */
    public void init(String sessionId, GameState initialState) {
        this.sessionId = sessionId;
        this.clientState = initialState;

        // 1. 사용자 입력에 대한 서버 응답 구독 (/user/topic/game/sync)
        networkClient.subscribeToSync(this::onServerUpdate);

        // 2. 서버 자동 게임 루프(GameTickService)로부터 상태 업데이트 구독 (/user/queue/game-state)
        networkClient.subscribeToGameState(this::onServerUpdate);

        System.out.println("✅ [NetworkGameClient] Initialized - Session: " + sessionId);
        System.out.println("   - Subscribed to /user/topic/game/sync (input responses)");
        System.out.println("   - Subscribed to /user/queue/game-state (server gravity)");
    }

    /**
     * 게임 명령 실행 (Thin Client - 입력 전송만)
     *
     * Thin Client 모델:
     * 1. 서버에 명령 전송
     * 2. 현재 클라이언트 상태 반환 (서버 응답 대기 중)
     * 3. 서버 응답은 onServerUpdate()에서 비동기로 처리하여 clientState 업데이트
     *
     * 게임 로직은 실행하지 않음!
     *
     * @param command 실행할 명령
     * @param currentState 현재 상태 (사용 안 함, 서버가 처리)
     * @return 현재 클라이언트 상태 (서버 응답 전)
     */
    public GameState executeCommand(GameCommand command, GameState currentState) {
        // 1. 서버에 입력 전송만 수행 (게임 로직 실행 안함!)
        long seq = ++localSequence;
        PlayerInputDto inputDto = PlayerInputDto.builder()
            .sessionId(sessionId)
            .command(command)
            .sequenceId(seq)
            .build();

        networkClient.sendInput(inputDto);

        System.out.println("📤 [NetworkGameClient] Command sent to server: " + command.getType() + " (seq=" + seq + ")");

        // 2. 현재 상태 반환 (서버 응답 전, 이전 상태)
        // 서버 응답이 오면 onServerUpdate()에서 clientState가 업데이트됨
        return this.clientState != null ? this.clientState : currentState;
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
     * 서버로부터 권위 있는 GameState 수신 (Thin Client)
     *
     * Thin Client 모델:
     * 1. 서버의 권위 있는 상태를 그대로 저장
     * 2. 상대방 상태 콜백 호출
     * 3. 공격 라인 콜백 호출
     *
     * Reconciliation 없음! 서버 상태를 신뢰
     *
     * @param serverState 서버로부터 받은 상태 업데이트
     */
    private void onServerUpdate(ServerStateDto serverState) {
        // Performance: 로그 출력 최소화 (틱마다 발생하므로)
        // System.out.println("📥 [NetworkGameClient] ========== SERVER UPDATE RECEIVED ==========");
        
        // 1. 서버의 권위 있는 상태를 그대로 저장 (Reconciliation 없음)
        this.clientState = serverState.getMyGameState();

        if (this.clientState == null) {
            System.err.println("❌ [NetworkGameClient] ERROR: Server sent NULL game state!");
            return;
        }

        // 2. ✨ 자신의 보드 상태 업데이트 (렌더링 트리거)
        if (myStateCallback != null) {
            myStateCallback.accept(this.clientState);
        } else {
            System.err.println("❌ [NetworkGameClient] ERROR: myStateCallback is NULL!");
        }

        // 3. 공격 라인 처리
        if (serverState.getAttackLinesReceived() > 0 && attackLinesCallback != null) {
            attackLinesCallback.accept(serverState.getAttackLinesReceived());
            System.out.println("⚔️ [NetworkGameClient] Attack lines: " + serverState.getAttackLinesReceived());
        }

        // 4. 상대방 상태는 콜백으로 전달
        if (serverState.getOpponentGameState() != null && opponentStateCallback != null) {
            opponentStateCallback.accept(serverState.getOpponentGameState());
        }
    }

    /**
     * ✨ 자신의 보드 상태 업데이트 콜백 설정
     *
     * @param callback 자신의 GameState를 받아 렌더링할 콜백 함수
     */
    public void setMyStateCallback(Consumer<GameState> callback) {
        this.myStateCallback = callback;
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
     * ✨ 공격 라인 수신 콜백 설정
     *
     * @param callback 공격 라인 수를 받을 콜백 함수
     */
    public void setAttackLinesCallback(Consumer<Integer> callback) {
        this.attackLinesCallback = callback;
    }

    /**
     * ✨ 네트워크 연결 정리
     *
     * 게임 종료 또는 재시작 시 호출됩니다.
     * 연결을 정리하고 내부 상태를 초기화합니다.
     */
    public void cleanup() {
        System.out.println("🧹 [NetworkGameClient] Cleaning up resources...");

        // NetworkTemplate 연결 정리
        if (networkClient != null) {
            networkClient.disconnect();
        }

        // 시퀀스 리셋
        localSequence = 0;

        // 상태 초기화
        clientState = null;
        sessionId = null;

        // 콜백 해제
        myStateCallback = null;
        opponentStateCallback = null;
        attackLinesCallback = null;

        System.out.println("✅ [NetworkGameClient] Cleanup complete");
    }
    
    /**
     * @deprecated Use cleanup() instead
     */
    @Deprecated
    public void disconnect() {
        cleanup();
    }
}
