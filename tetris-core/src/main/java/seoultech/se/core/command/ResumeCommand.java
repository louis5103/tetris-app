package seoultech.se.core.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 게임 재개 명령
 * 
 * 이 Command는 일시정지된 게임을 다시 시작하고 싶다는 의도를 표현합니다.
 * 
 * Resume의 효과:
 * 
 * 1. 게임 루프 재시작:
 *    - 멈춰있던 AnimationTimer나 게임 루프가 다시 실행됨
 *    - 블록이 다시 자동으로 떨어지기 시작함
 * 
 * 2. 입력 처리 재개:
 *    - 모든 게임 플레이 Command가 다시 처리됨
 *    - 이동, 회전, Hard Drop 등이 정상적으로 동작함
 * 
 * 3. UI 복원:
 *    - "PAUSED" 메시지 제거
 *    - 게임 화면을 원래대로 되돌림
 * 
 * 타이밍 주의사항:
 * 
 * Resume 후 바로 게임이 시작되면 플레이어가 당황할 수 있습니다.
 * 특히 "3... 2... 1... GO!" 같은 카운트다운 없이 바로 시작되면
 * 블록이 예상보다 빨리 떨어져서 실수할 수 있죠.
 * 
 * 따라서 좋은 UX는 다음과 같습니다:
 * - Resume Command를 받으면 즉시 시작하지 않고
 * - "3... 2... 1..." 카운트다운을 표시
 * - 카운트다운이 끝나면 게임 루프 재시작
 * 
 * 혹은 더 간단하게:
 * - Resume 버튼을 누르면 바로 시작되지만
 * - 게임 화면에 "Press any key to resume" 메시지 표시
 * - 아무 키나 누르면 그때 실제로 Resume
 * 
 * GameState에서의 처리:
 * 
 * Pause와 마찬가지로 isPaused 플래그를 false로 설정합니다.
 * 하지만 시간 관련 처리가 중요합니다.
 * 
 * Pause 전의 lastUpdateTime을 그대로 사용하면,
 * Resume 후 첫 프레임에서 엄청난 시간이 경과한 것처럼 계산됩니다.
 * 예를 들어 10분 동안 Pause했다가 Resume하면,
 * 블록이 한 번에 600줄을 떨어지는 계산이 될 수 있죠.
 * 
 * 따라서 Resume할 때는 lastUpdateTime을 현재 시간으로 리셋해야 합니다:
 * lastUpdateTime = System.nanoTime();
 * 
 * 이렇게 하면 Pause 시간은 게임 시간에 포함되지 않습니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)  // P2P 전송 시 추가 필드(description 등) 무시
public class ResumeCommand implements GameCommand {
    
    @Override
    public CommandType getType() {
        return CommandType.RESUME;
    }
    
    @Override
    public String getDescription() {
        return "Resume Game";
    }
    
    @Override
    public String toString() {
        return "ResumeCommand{}";
    }
}
