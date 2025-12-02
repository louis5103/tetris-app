package seoultech.se.client.controller;

/**
 * BoardController 통합 테스트
 * 
 * @deprecated Strategy 패턴으로 리팩토링되면서 GameMode 클래스가 제거됨.
 * 새로운 테스트는 LocalExecutionStrategy와 NetworkExecutionStrategy를 테스트해야 함.
 */
@Deprecated
public class BoardControllerIntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("⚠️ This test is deprecated. Strategy pattern has replaced GameMode classes.");
        System.out.println("   Please create new tests for LocalExecutionStrategy and NetworkExecutionStrategy.");
    }
    
    /*
     * 기존 테스트 코드는 Strategy 패턴으로 리팩토링되면서 더 이상 유효하지 않습니다.
     * 
     * 변경 사항:
     * - SingleMode/MultiMode 클래스 제거
     * - LocalExecutionStrategy/NetworkExecutionStrategy로 대체
     * - GameModeConfig는 불변 객체로 변경
     * - BoardController는 Strategy 패턴 사용
     * 
     * TODO: 새로운 테스트 작성 필요
     * - LocalExecutionStrategy 테스트
     * - NetworkExecutionStrategy 테스트
     * - GameModeConfig 빌더 테스트
     */
}
