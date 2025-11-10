package seoultech.se.client.mode;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.mode.GameMode;
import seoultech.se.core.mode.GameModeType;

/**
 * 싱글플레이어 모드
 * 
 * 순수한 로컬 게임 모드입니다.
 * 네트워크 연결 없이 혼자서 플레이하는 기본 모드입니다.
 * 
 * 특징:
 * - 네트워크 통신 없음
 * - 공격/방어 시스템 없음
 * - 순수한 점수/레벨 관리만 수행
 * - 가장 단순하고 안정적인 모드
 * 
 * 설계 원칙:
 * 1. 단순성 (Simplicity)
 *    - 추가 로직 최소화
 *    - 기본 게임 메커니즘만 사용
 * 
 * 2. 명확한 책임 (Single Responsibility)
 *    - 싱글플레이어 게임 로직만 담당
 *    - 멀티플레이어 로직과 완전 분리
 * 
 * 3. 확장성 (Extensibility)
 *    - GameMode 인터페이스 구현
 *    - 필요시 커스텀 설정 추가 가능
 * 
 * 사용 예시:
 * 
 * // Spring에서 자동 주입
 * @Autowired
 * private SingleMode singleMode;
 * 
 * // 설정 적용
 * singleMode.setConfig(GameModeConfig.classic());
 * 
 * // BoardController에 설정
 * boardController.setGameMode(singleMode);
 */
@Component
@Getter
@Setter
public class SingleMode implements GameMode {
    
    /**
     * 게임 모드 설정
     * 외부에서 주입 가능하도록 Setter 제공
     */
    private GameModeConfig config = GameModeConfig.classic(); // 기본값: 클래식 모드
    
    /**
     * 게임 상태 참조 (초기화 시 설정)
     */
    private GameState gameState;
    
    /**
     * 기본 생성자 (Classic 모드)
     */
    public SingleMode() {
        this(GameModeConfig.classic());
    }
    
    /**
     * GameModeConfig를 받는 생성자
     * 
     * @param config 게임 모드 설정
     */
    public SingleMode(GameModeConfig config) {
        this.config = config;
        System.out.println("🎮 SingleMode created with config: " + 
            (config.getGameplayType() != null ? config.getGameplayType() : "CLASSIC") +
            ", SRS: " + config.isSrsEnabled());
    }
    
    // ========== GameMode 인터페이스 구현 ==========
    
    @Override
    public GameModeType getType() {
        return GameModeType.SINGLE;
    }
    
    @Override
    public GameModeConfig getConfig() {
        return config;
    }
    
    @Override
    public void initialize(GameState initialState) {
        this.gameState = initialState;
        
        // 싱글모드는 추가 초기화 불필요
        // 네트워크 연결, 아이템 매니저 등이 없음
        
        System.out.println("[SingleMode] 싱글플레이어 모드 초기화 완료");
    }
    
    /**
     * ⭐ 라인 클리어 후 추가 처리
     * 
     * Phase 2: LockResult 제거 - GameState만으로 모든 정보 전달
     * 
     * 싱글플레이어 모드에서는 추가 처리가 없습니다.
     * 
     * 멀티플레이어와의 차이점:
     * - ❌ 공격 전송 없음
     * - ❌ 가비지 라인 수신 없음
     * - ❌ 상대방 정보 업데이트 없음
     * 
     * 아이템 모드와의 차이점:
     * - ❌ 아이템 드롭 없음
     * - ❌ 아이템 효과 적용 없음
     * 
     * 점수와 레벨은 GameEngine에서 이미 처리되므로
     * 여기서는 추가로 할 일이 없습니다.
     * 
     * @param state 현재 게임 상태 (Lock 메타데이터 포함)
     */
    @Override
    public void onLineClear(GameState state) {
        // 싱글플레이어는 순수한 로컬 게임
        // 라인 클리어에 대한 추가 처리 없음
        
        // 디버그 로그 (선택적)
        if (state.getLastLinesCleared() > 0) {
            System.out.println(String.format(
                "[SingleMode] %d줄 클리어 (점수: %d, 레벨: %d)",
                state.getLastLinesCleared(),
                state.getScore(),
                state.getLevel()
            ));
        }
    }
    
    /**
     * 모드 종료 시 정리
     * 
     * 싱글플레이어 모드는 정리할 리소스가 없습니다.
     * - 네트워크 연결 없음
     * - 타이머 없음
     * - 외부 리소스 없음
     */
    @Override
    public void cleanup() {
        System.out.println("[SingleMode] 싱글플레이어 모드 종료");
        this.gameState = null;
    }
    
    // ========== 편의 메서드 ==========
    
    /**
     * 통계 정보 출력
     * 
     * 싱글플레이어 세션 종료 시 통계를 출력합니다.
     */
    public void printStatistics() {
        if (gameState == null) {
            System.out.println("[SingleMode] 게임 상태 없음");
            return;
        }
        
        System.out.println("\n========== 싱글플레이어 통계 ==========");
        System.out.println("최종 점수: " + gameState.getScore());
        System.out.println("최종 레벨: " + gameState.getLevel());
        System.out.println("클리어한 라인: " + gameState.getLinesCleared());
        System.out.println("=====================================\n");
    }
}
