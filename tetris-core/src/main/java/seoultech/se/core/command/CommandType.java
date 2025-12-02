package seoultech.se.core.command;

/**
 * Command의 종류를 나타내는 열거형
 * 
 * 이 enum은 모든 가능한 게임 명령의 타입을 정의합니다.
 * JSON 직렬화/역직렬화시 Command를 구분하는 식별자로 사용됩니다.
 * 
 * 예를 들어 서버가 클라이언트로부터 JSON을 받으면:
 * {"type": "MOVE", "direction": "LEFT"}
 * 
 * type 필드를 보고 이것이 MoveCommand임을 알 수 있고,
 * direction 필드를 읽어서 MoveCommand 객체를 생성합니다.
 */
public enum CommandType {
    /**
     * 블록 이동 명령
     * MoveCommand에 대응됩니다
     */
    MOVE,
    
    /**
     * 왼쪽으로 이동
     */
    MOVE_LEFT,
    
    /**
     * 오른쪽으로 이동
     */
    MOVE_RIGHT,
    
    /**
     * 아래로 이동 (소프트 드롭)
     */
    MOVE_DOWN,
    
    /**
     * 블록 회전 명령
     * RotateCommand에 대응됩니다
     */
    ROTATE,
    
    /**
     * 시계방향 회전
     */
    ROTATE_CW,
    
    /**
     * 반시계방향 회전
     */
    ROTATE_CCW,
    
    /**
     * 하드 드롭 명령 (블록을 즉시 바닥까지 떨어뜨림)
     * HardDropCommand에 대응됩니다
     */
    HARD_DROP,
    
    /**
     * Hold 명령 (현재 블록을 저장하고 다음 블록으로 교체)
     * HoldCommand에 대응됩니다
     */
    HOLD,
    
    /**
     * 게임 일시정지 명령
     * PauseCommand에 대응됩니다
     */
    PAUSE,
    
    /**
     * 게임 재개 명령
     * ResumeCommand에 대응됩니다
     */
    RESUME,
    
    /**
     * 소프트 드롭 명령 (블록을 빠르게 내림)
     * 
     * 참고: 현재는 DOWN 방향의 MoveCommand로 처리되지만,
     * 나중에 점수 계산을 다르게 하려면 별도 Command로 분리할 수 있습니다
     */
    SOFT_DROP
}
