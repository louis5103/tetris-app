package seoultech.se.server.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import seoultech.se.core.dto.PlayerInputDto;
import seoultech.se.core.dto.ServerStateDto;
import seoultech.se.server.game.GameSession;
import seoultech.se.server.game.GameSessionManager;

@Controller
@RequestMapping("/api/game")
@RequiredArgsConstructor
public class GameSessionController {
    private final GameSessionManager gameSessionManager;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 게임 시작 트리거 (멀티플레이용)
     *
     * 클라이언트가 게임 화면 로드 완료 후 호출
     * 서버의 GameTickService가 이 세션을 처리하도록 활성화
     *
     * @param sessionId 게임 세션 ID
     * @return 성공 시 200 OK
     */
    @PostMapping("/start/{sessionId}")
    public ResponseEntity<String> startGame(@PathVariable String sessionId, Principal principal) {
        String playerId = (principal != null) ? principal.getName() : "anonymous";

        GameSession session = gameSessionManager.getSession(sessionId);

        if (session == null) {
            System.out.println("❌ [GameSessionController] Session not found: " + sessionId);
            return ResponseEntity.notFound().build();
        }

        // 게임 시작 (isGameStarted = true 설정)
        session.startGame();

        System.out.println("🎮 [GameSessionController] Game started: Session=" + sessionId + ", Player=" + playerId);

        return ResponseEntity.ok("Game started");
    }

    @MessageMapping("/game/input")
    public void handleInput(PlayerInputDto input, Principal principal) {
        // Principal이 null일 경우 임시로 "anonymous" 사용 (디버깅용)
        String playerId = (principal != null) ? principal.getName() : "anonymous";

        if (principal == null) {
            System.out.println("⚠️ [GameSessionController] Principal is null, using 'anonymous'");
        }

        GameSession session = gameSessionManager.getSession(input.getSessionId());

        if (session == null) {
            System.out.println("❌ [GameSessionController] Session not found: " + input.getSessionId());
            return;
        }

        // 1. 입력 처리 및 Sender 기준 상태 생성
        ServerStateDto senderResponse = session.processInput(playerId, input);
        if (senderResponse == null) return;

        // 2. Sender에게 전송 (Reconciliation/Rendering)
        messagingTemplate.convertAndSendToUser(
            playerId,
            "/topic/game/sync",
            senderResponse
        );

        // 3. Opponent에게 전송 (Rendering)
        // 중요: Sender 기준의 DTO를 그대로 보내면 안됨! (Identity Crisis 방지)
        // Opponent 기준으로 데이터를 뒤집어서(Swap) 전송해야 함.
        
        java.util.List<String> players = session.getPlayerIds();
        String opponentId = players.stream()
            .filter(id -> !id.equals(playerId))
            .findFirst()
            .orElse(null);

        if (opponentId != null) {
            // Opponent 기준 DTO 생성
            ServerStateDto opponentResponse = ServerStateDto.builder()
                .lastProcessedSequence(0) // Opponent는 이 입력을 보낸게 아니므로 시퀀스 무관
                .myGameState(senderResponse.getOpponentGameState()) // 내(Opponent) 상태 = Sender가 본 Opponent 상태
                .opponentGameState(senderResponse.getMyGameState()) // 상대(Sender) 상태 = Sender가 본 자신 상태
                .events(senderResponse.getEvents()) // 이벤트는 공유 (필요 시 분리 가능)
                .attackLinesReceived(0) // 입력에 의한 즉각적인 공격 수신은 별도 처리 필요할 수 있음 (현재는 0 처리)
                .build();

            // Opponent가 이미 구독 중인 /queue/game-state로 전송
            messagingTemplate.convertAndSendToUser(
                opponentId,
                "/queue/game-state",
                opponentResponse
            );
        }
    }
}
