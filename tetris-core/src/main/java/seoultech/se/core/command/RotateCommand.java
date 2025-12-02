package seoultech.se.core.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import seoultech.se.core.model.enumType.RotationDirection;

/**
 * 블록 회전 명령
 * 
 * 이 Command는 테트로미노를 시계방향 또는 반시계방향으로 회전시키고 싶다는 의도를 표현합니다.
 * 
 * 현대 테트리스의 회전 시스템 (SRS - Super Rotation System)
 * 
 * 단순히 블록을 90도 회전시키는 것이 아니라, 회전 후 벽이나 다른 블록에 막히면
 * 자동으로 위치를 조정하여 회전을 성공시키려고 시도합니다. 이것을 Wall Kick이라고 합니다.
 * 
 * Wall Kick은 5가지 위치를 순서대로 시도합니다:
 * 1. 원래 위치에서 그냥 회전
 * 2. 한 칸 왼쪽으로 이동하고 회전
 * 3. 한 칸 오른쪽으로 이동하고 회전
 * 4. 한 칸 위로 이동하고 회전
 * 5. 두 칸 위로 이동하고 회전 (I 블록만)
 * 
 * 이 모든 복잡한 로직은 GameEngine에 구현되어 있습니다.
 * 이 Command는 단지 "회전하고 싶다"는 의도만 전달하면 됩니다.
 * 
 * 사용 예시:
 * - 사용자가 위쪽 화살표 또는 X 키를 누름
 * - GameController가 new RotateCommand(RotationDirection.CLOCKWISE) 생성
 * - GameService에 Command 전달
 * - GameService가 GameEngine.tryRotate() 호출
 * - Engine이 SRS Wall Kick을 시도
 * - 성공하면 어떤 Kick이 사용되었는지 RotationResult에 포함
 * - 결과를 Event로 변환하여 UI에 알림
 * 
 * RotationResult에는 kickIndex가 포함되는데, 이것은 디버깅이나
 * 고급 플레이어를 위한 정보 표시에 사용될 수 있습니다.
 * 예: "Kick 3으로 회전 성공!" 같은 메시지를 표시할 수 있죠.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor  // JSON 역직렬화를 위해 필요
@JsonIgnoreProperties(ignoreUnknown = true)  // P2P 전송 시 추가 필드(description 등) 무시
public class RotateCommand implements GameCommand {
    /**
     * 회전 방향
     * CLOCKWISE(시계방향) 또는 COUNTER_CLOCKWISE(반시계방향)
     */
    private RotationDirection direction;
    
    @Override
    public CommandType getType() {
        return CommandType.ROTATE;
    }
    
    @Override
    public String getDescription() {
        return "Rotate " + direction;
    }
    
    @Override
    public String toString() {
        return "RotateCommand{direction=" + direction + "}";
    }
}
