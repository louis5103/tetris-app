package seoultech.se.core.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Hard Drop 명령
 * 
 * 이 Command는 현재 테트로미노를 즉시 바닥까지 떨어뜨리고 고정하고 싶다는 의도를 표현합니다.
 * 
 * Hard Drop의 동작 과정:
 * 1. 현재 블록이 더 이상 아래로 갈 수 없을 때까지 이동
 * 2. 즉시 고정 (Lock Delay 없음)
 * 3. 떨어진 거리만큼 점수 획득 (보통 1칸당 2점)
 * 4. 라인 클리어 체크
 * 5. 새 블록 생성
 * 
 * Hard Drop은 테트리스에서 가장 많이 사용되는 기술입니다.
 * 숙련된 플레이어는 거의 모든 블록을 Hard Drop으로 배치합니다.
 * 블록이 천천히 떨어지기를 기다리는 것보다 훨씬 빠르니까요.
 * 
 * 사용 예시:
 * - 사용자가 스페이스바를 누름
 * - GameController가 new HardDropCommand() 생성
 * - GameService에 Command 전달
 * - GameService가 GameEngine.hardDrop() 호출
 * - Engine이 블록을 바닥까지 이동시키고 즉시 고정
 * - LockResult 반환 (라인 클리어 정보 포함)
 * - 여러 Event로 변환: TetrominoLocked, LineCleared, ScoreAdded 등
 * 
 * Hard Drop과 Soft Drop의 차이:
 * - Soft Drop: DOWN 키를 누르고 있으면 블록이 빠르게 내려옴 (1칸당 1점)
 * - Hard Drop: 스페이스바를 누르면 즉시 바닥까지 떨어지고 고정됨 (1칸당 2점)
 * 
 * 파라미터가 필요없는 이유:
 * Hard Drop은 "현재 블록을 바닥까지"라는 의미이므로,
 * 어느 블록인지, 얼마나 떨어뜨릴지 같은 정보가 필요 없습니다.
 * GameState에 이미 현재 블록 정보가 있고,
 * GameEngine이 바닥까지의 거리를 계산하니까요.
 */
@JsonIgnoreProperties(ignoreUnknown = true)  // P2P 전송 시 추가 필드(description 등) 무시
public class HardDropCommand implements GameCommand {
    
    @Override
    public CommandType getType() {
        return CommandType.HARD_DROP;
    }
    
    @Override
    public String getDescription() {
        return "Hard Drop";
    }
    
    @Override
    public String toString() {
        return "HardDropCommand{}";
    }
}
