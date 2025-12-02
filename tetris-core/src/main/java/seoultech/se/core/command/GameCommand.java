package seoultech.se.core.command;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * 게임 명령의 기본 인터페이스
 *
 * 이 인터페이스는 Command 패턴의 핵심입니다.
 * 중요한 것은 이 Command가 "어떻게 실행하는가"를 알지 못한다는 점입니다.
 * 단지 "무엇을 하고 싶은가"라는 의도만 표현합니다.
 *
 * 예를 들어:
 * - MoveCommand는 "왼쪽으로 이동하고 싶다"는 의도만 담습니다
 * - 실제로 블록이 왼쪽으로 이동 가능한지, 어떻게 이동시킬지는 GameEngine의 몫입니다
 *
 * 이런 설계의 장점:
 * 1. Command를 JSON으로 직렬화하여 네트워크로 전송 가능
 * 2. 같은 Command를 클라이언트와 서버가 공유 가능
 * 3. Command를 큐에 저장하여 나중에 재생 가능 (리플레이 기능)
 * 4. 테스트하기 쉬움 (Command 생성만 확인하면 됨)
 *
 * Jackson 다형성 지원:
 * @JsonTypeInfo - JSON에서 "type" 필드를 사용하여 구체 클래스를 결정
 * @JsonSubTypes - type 값과 실제 클래스의 매핑 정의
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = MoveCommand.class, name = "MOVE"),
    @JsonSubTypes.Type(value = RotateCommand.class, name = "ROTATE"),
    @JsonSubTypes.Type(value = HardDropCommand.class, name = "HARD_DROP"),
    @JsonSubTypes.Type(value = HoldCommand.class, name = "HOLD"),
    @JsonSubTypes.Type(value = PauseCommand.class, name = "PAUSE"),
    @JsonSubTypes.Type(value = ResumeCommand.class, name = "RESUME")
})
public interface GameCommand {
    /**
     * 이 Command의 타입을 반환합니다
     * 
     * CommandType은 Command를 구분하는 식별자입니다.
     * JSON 직렬화/역직렬화시 어떤 Command 클래스로 변환할지 결정하는 데 사용됩니다.
     * 
     * @return Command의 타입
     */
    CommandType getType();
    
    /**
     * getType()의 별칭 (backward compatibility)
     * 
     * @return Command의 타입
     */
    default CommandType getCommandType() {
        return getType();
    }
    
    /**
     * Command에 대한 설명을 반환합니다 (디버깅용)
     * 
     * 이 메서드는 주로 로깅이나 디버깅에 사용됩니다.
     * 예: "Move LEFT", "Rotate CLOCKWISE", "Hard Drop"
     * 
     * @return Command의 설명 문자열
     */
    default String getDescription() {
        return getCommandType().toString();
    }
}
