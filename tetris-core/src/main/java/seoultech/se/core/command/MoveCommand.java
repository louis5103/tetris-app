package seoultech.se.core.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 블록 이동 명령
 * 
 * 이 Command는 테트로미노를 특정 방향으로 한 칸 이동시키고 싶다는 의도를 표현합니다.
 * 실제로 이동이 가능한지, 벽이나 다른 블록에 막히는지는 GameEngine이 판단합니다.
 * 
 * Soft Drop:
 * isSoftDrop 플래그가 true이면 수동 DOWN 입력으로 간주됩니다.
 * 표준 테트리스에서 Soft Drop은 1칸당 1점을 획득합니다.
 * 자동 낙하(gravity)는 점수를 주지 않습니다.
 * 
 * 사용 예시:
 * - 사용자가 왼쪽 화살표를 누름
 * - GameController가 new MoveCommand(Direction.LEFT, false) 생성
 * - GameService에 Command 전달
 * - GameService가 GameEngine.tryMoveLeft() 호출
 * - 결과를 Event로 변환하여 UI에 알림
 * 
 * 왜 실행 로직이 없나요?
 * Command에 execute() 메서드를 두지 않는 이유는 다음과 같습니다.
 * 
 * 1. 네트워크 전송 가능: 이 객체를 JSON으로 직렬화하여 서버로 보낼 수 있습니다.
 *    만약 execute(Board board) 같은 메서드가 있다면, Board를 함께 전송해야 하는데 이것은 불가능합니다.
 * 
 * 2. 서버-클라이언트 공유: 같은 Command를 클라이언트와 서버가 공유할 수 있습니다.
 *    클라이언트는 이 Command를 만들어서 보내고, 서버는 받아서 실행합니다.
 * 
 * 3. 테스트 용이성: Command가 단순한 데이터 객체이므로 테스트하기 쉽습니다.
 *    예: assertEquals(Direction.LEFT, command.getDirection())
 * 
 * 4. 리플레이 기능: Command들을 순서대로 저장했다가 나중에 재생할 수 있습니다.
 *    이것은 리플레이 시스템이나 디버깅에 매우 유용합니다.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor  // JSON 역직렬화를 위해 필요
@JsonIgnoreProperties(ignoreUnknown = true)  // P2P 전송 시 추가 필드(description 등) 무시
public class MoveCommand implements GameCommand {
    /**
     * 이동할 방향
     * LEFT, RIGHT, DOWN 중 하나입니다
     */
    private Direction direction;
    
    /**
     * Soft Drop 여부
     * true면 수동 DOWN 입력 (점수 획득)
     * false면 자동 낙하 (점수 없음)
     */
    private boolean isSoftDrop;
    
    /**
     * 기본 생성자 - Soft Drop 여부가 false로 설정됨 (자동 낙하)
     */
    public MoveCommand(Direction direction) {
        this.direction = direction;
        this.isSoftDrop = false;
    }
    
    @Override
    public CommandType getType() {
        return CommandType.MOVE;
    }
    
    @Override
    public String getDescription() {
        return "Move " + direction + (isSoftDrop ? " (Soft Drop)" : "");
    }
    
    @Override
    public String toString() {
        return "MoveCommand{direction=" + direction + ", isSoftDrop=" + isSoftDrop + "}";
    }
}
