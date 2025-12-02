package seoultech.se.client.model;

/**
 * 게임 액션 열거형
 * 
 * 키보드 입력을 게임 행동으로 매핑하기 위한 추상화 레이어입니다.
 * 
 * 왜 필요한가?
 * - 사용자마다 다른 키 설정을 사용할 수 있어야 함
 * - 멀티플레이어에서 각 클라이언트의 키 설정이 독립적
 * - 키보드 레이아웃 변경 (WASD ↔ 화살표 등)
 * 
 * 사용 흐름:
 * 1. 키 입력 발생 (KeyCode.LEFT)
 * 2. KeyMappingService가 GameAction으로 변환 (MOVE_LEFT)
 * 3. GameController가 Command 생성 (MoveCommand(Direction.LEFT))
 * 4. BoardController가 실행 및 Event 발행
 */
public enum GameAction {
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
     * 시계방향 회전
     */
    ROTATE_CLOCKWISE,
    
    /**
     * 반시계방향 회전
     */
    ROTATE_COUNTER_CLOCKWISE,
    
    /**
     * 하드 드롭 (즉시 고정)
     */
    HARD_DROP,
    
    /**
     * Hold 기능
     */
    HOLD,
    
    /**
     * 일시정지/재개 토글
     */
    PAUSE_RESUME;
}
