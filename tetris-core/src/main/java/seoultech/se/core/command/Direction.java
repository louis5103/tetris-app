package seoultech.se.core.command;

/**
 * 이동 방향을 나타내는 열거형
 * 
 * MoveCommand에서 블록의 이동 방향을 지정할 때 사용됩니다.
 * 왼쪽, 오른쪽, 아래 세 가지 방향만 존재합니다.
 * (위로는 이동할 수 없으므로 UP은 없습니다)
 */
public enum Direction {
    /**
     * 왼쪽으로 한 칸 이동
     */
    LEFT,
    
    /**
     * 오른쪽으로 한 칸 이동
     */
    RIGHT,
    
    /**
     * 아래로 한 칸 이동
     * 게임 루프나 사용자의 DOWN 키 입력에서 사용됩니다
     */
    DOWN
}
