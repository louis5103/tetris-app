package seoultech.se.server.controller;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;
import seoultech.se.core.dto.PlayerInputDto;
import seoultech.se.core.dto.ServerStateDto;
import seoultech.se.server.game.GameSession;
import seoultech.se.server.game.GameSessionManager;

@Controller
@RequiredArgsConstructor
public class GameSessionController {
    private final GameSessionManager gameSessionManager;
    private final SimpMessagingTemplate messagingTemplate;

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

        ServerStateDto response = session.processInput(playerId, input);
        if (response == null) return;

        // 1. 나에게 상태 전송 (Reconciliation용)
        messagingTemplate.convertAndSendToUser(
            playerId,
            "/topic/game/sync",
            response
        );

        // 2. 상대에게 내 변경사항 전송 (Rendering용)
        // 실제로는 상대방 ID를 찾아서 보내야 함
        messagingTemplate.convertAndSend(
            "/topic/game/" + input.getSessionId() + "/opponent",
            response
        );
    }
}
