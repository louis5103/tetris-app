package seoultech.se.core.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Hold 명령
 * 
 * 이 Command는 현재 테트로미노를 Hold 공간에 저장하고,
 * Hold에 있던 블록(있다면)을 꺼내오고 싶다는 의도를 표현합니다.
 * 
 * Hold 시스템의 규칙:
 * 
 * 1. 처음 Hold를 사용하면:
 *    - 현재 블록이 Hold 공간에 저장됨
 *    - Next Queue에서 다음 블록이 나옴
 * 
 * 2. 이미 Hold에 블록이 있으면:
 *    - 현재 블록과 Hold 블록이 서로 교체됨
 *    - Hold에서 꺼낸 블록은 spawn 위치에 나타남
 * 
 * 3. 한 턴에 한 번만 사용 가능:
 *    - 블록을 고정(Lock)할 때까지 Hold를 다시 사용할 수 없음
 *    - 이것을 막지 않으면 Hold를 무한정 반복할 수 있어서 게임이 망가집니다
 *    - GameState의 holdUsedThisTurn 플래그로 관리됨
 * 
 * Hold의 전략적 가치:
 * 
 * Hold는 테트리스에서 가장 강력한 도구 중 하나입니다.
 * 예를 들어 I 블록(긴 막대)이 필요한 상황에서 다른 블록이 나오면,
 * 일단 Hold에 저장해두고 나중에 사용할 수 있습니다.
 * 
 * 숙련된 플레이어는 Hold를 다음과 같이 활용합니다:
 * - 긴급 상황을 위해 I 블록이나 T 블록을 저장
 * - 완벽한 배치를 위해 블록 순서를 조정
 * - T-Spin 같은 고급 테크닉을 위한 블록 준비
 * 
 * 구현 시 주의사항:
 * 
 * GameEngine에서 Hold를 구현할 때는 다음을 체크해야 합니다:
 * - holdUsedThisTurn이 true면 실패 (HoldResult.failed 반환)
 * - Hold가 가능하면 블록 교체 후 holdUsedThisTurn = true 설정
 * - 블록이 고정될 때 holdUsedThisTurn = false로 리셋
 * 
 * 이런 제약이 없다면 플레이어는 Hold를 연속으로 눌러서
 * 원하는 블록이 나올 때까지 계속 바꿀 수 있게 됩니다.
 * 이것은 게임 밸런스를 완전히 무너뜨리죠.
 */
@JsonIgnoreProperties(ignoreUnknown = true)  // P2P 전송 시 추가 필드(description 등) 무시
public class HoldCommand implements GameCommand {
    
    @Override
    public CommandType getType() {
        return CommandType.HOLD;
    }
    
    @Override
    public String getDescription() {
        return "Hold";
    }
    
    @Override
    public String toString() {
        return "HoldCommand{}";
    }
}
