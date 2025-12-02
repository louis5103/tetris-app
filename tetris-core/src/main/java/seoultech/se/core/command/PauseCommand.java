package seoultech.se.core.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 게임 일시정지 명령
 * 
 * 이 Command는 현재 진행 중인 게임을 일시정지하고 싶다는 의도를 표현합니다.
 * 
 * Pause의 효과:
 * 
 * 1. 게임 루프 중단:
 *    - AnimationTimer나 게임 루프가 멈춤
 *    - 블록이 더 이상 자동으로 떨어지지 않음
 * 
 * 2. 입력 차단:
 *    - 이동, 회전 같은 게임 플레이 Command가 무시됨
 *    - Resume Command만 처리됨
 * 
 * 3. UI 변화:
 *    - "PAUSED" 메시지 표시
 *    - 게임 화면 흐리게 처리 또는 반투명 오버레이
 * 
 * 멀티플레이어에서의 Pause:
 * 
 * 싱글 플레이어와 달리 멀티플레이어에서는 Pause가 복잡합니다.
 * 한 플레이어가 Pause를 누른다고 다른 플레이어까지 멈출 수는 없으니까요.
 * 
 * 보통 다음과 같은 방식으로 처리합니다:
 * - 각 플레이어는 자기 화면만 Pause 가능
 * - Pause 중에는 다른 플레이어의 공격이 들어와도 저장만 됨
 * - Resume하면 저장된 공격이 한꺼번에 적용됨
 * 
 * 혹은 아예 Pause를 허용하지 않고, 게임을 포기(Forfeit)하는 것만 가능하게 만들기도 합니다.
 * 
 * 구현 시 고려사항:
 * 
 * GameState에 isPaused 플래그를 추가해야 합니다.
 * SessionManager는 Pause 상태일 때 게임 플레이 Command를 거부해야 합니다.
 * 하지만 Resume Command는 받아들여야 하죠.
 * 
 * 싱글 플레이어에서는 Pause 중에도 시간이 멈추지만,
 * 멀티플레이어 랭킹 모드에서는 Pause 시간도 총 시간에 포함시킬 수 있습니다.
 * 무한정 Pause하면서 전략을 짜는 것을 방지하기 위해서죠.
 */
@JsonIgnoreProperties(ignoreUnknown = true)  // P2P 전송 시 추가 필드(description 등) 무시
public class PauseCommand implements GameCommand {
    
    @Override
    public CommandType getType() {
        return CommandType.PAUSE;
    }
    
    @Override
    public String getDescription() {
        return "Pause Game";
    }
    
    @Override
    public String toString() {
        return "PauseCommand{}";
    }
}
