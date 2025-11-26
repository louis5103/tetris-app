package seoultech.se.server.dto;

import lombok.Data;
import seoultech.se.core.config.GameplayType;

/**
 * 세션 생성 요청 DTO
 */
@Data
public class SessionCreateRequest {
    /**
     * 게임플레이 타입 (CLASSIC, ARCADE 등)
     */
    private GameplayType gameplayType;

    /**
     * 플레이어 ID (optional, JWT에서 가져올 수도 있음)
     */
    private String playerId;
}
