package seoultech.se.core.dto;


import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerStateDto {
    private long lastProcessedSequence; // 서버가 처리한 마지막 시퀀스 번호
    private GameStateDto myGameState;      // 서버가 확정한 나의 상태 (보정용) - 경량 DTO 사용
    private GameStateDto opponentGameState;// 상대방 상태 (렌더링용) - 경량 DTO 사용
    private List<String> events;        // 발생한 이벤트 (LINE_CLEAR, ATTACK 등)
    private int attackLinesReceived;    // 받은 공격 라인 수 (상대방이 나를 공격한 라인)
    private boolean gameOver;           // 게임 오버 여부
}
