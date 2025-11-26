package seoultech.se.server.dto;

import lombok.Data;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.model.enumType.Difficulty;

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
     * 난이도 (EASY, NORMAL, HARD)
     * 서버에서 GameModeConfig 생성 시 사용
     */
    private Difficulty difficulty;

    /**
     * 플레이어 ID (optional, JWT에서 가져올 수도 있음)
     */
    private String playerId;
}
