package seoultech.se.client.controller;

import seoultech.se.client.mode.SingleMode;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.mode.GameModeType;

/**
 * BoardController 통합 테스트
 * 
 * GameMode 통합이 정상 동작하는지 확인합니다.
 */
public class BoardControllerIntegrationTest {
    
    public static void main(String[] args) {
        System.out.println("=== BoardController 통합 테스트 시작 ===\n");
        
        try {
            // 1. BoardController 생성 테스트
            System.out.println("1. BoardController 인스턴스 생성...");
            BoardController controller = new BoardController();
            System.out.println("   ✅ 생성 성공");
            
            // 2. 기본 GameMode 확인 (SingleMode)
            System.out.println("\n2. 기본 GameMode 확인...");
            assert controller.getGameMode() != null : "GameMode가 null입니다!";
            assert controller.getGameMode().getType() == GameModeType.SINGLE : "기본 모드가 SINGLE이 아닙니다!";
            System.out.println("   - 기본 모드: " + controller.getGameMode().getType());
            System.out.println("   ✅ 기본 SingleMode 설정 확인");
            
            // 3. 설정 확인
            System.out.println("\n3. 기본 설정 확인...");
            GameModeConfig config = controller.getConfig();
            System.out.println("   - 하드드롭: " + config.isHardDropEnabled());
            System.out.println("   - 홀드: " + config.isHoldEnabled());
            assert config.isHardDropEnabled() : "하드드롭이 활성화되어 있지 않습니다!";
            assert config.isHoldEnabled() : "홀드가 활성화되어 있지 않습니다!";
            System.out.println("   ✅ 설정 확인 성공");
            
            // 4. 커스텀 GameMode 설정 테스트
            System.out.println("\n4. 커스텀 GameMode 설정...");
            GameModeConfig customConfig = GameModeConfig.builder()
                .hardDropEnabled(false)
                .holdEnabled(false)
                .startLevel(3)
                .build();
            
            SingleMode customMode = new SingleMode();
            customMode.setConfig(customConfig);
            controller.setGameMode(customMode);
            
            GameModeConfig newConfig = controller.getConfig();
            System.out.println("   - 하드드롭: " + newConfig.isHardDropEnabled());
            System.out.println("   - 홀드: " + newConfig.isHoldEnabled());
            System.out.println("   - 시작 레벨: " + newConfig.getStartLevel());
            assert !newConfig.isHardDropEnabled() : "하드드롭이 비활성화되지 않았습니다!";
            assert !newConfig.isHoldEnabled() : "홀드가 비활성화되지 않았습니다!";
            assert newConfig.getStartLevel() == 3 : "시작 레벨이 3이 아닙니다!";
            System.out.println("   ✅ 커스텀 설정 성공");
            
            // 5. GameState 확인
            System.out.println("\n5. GameState 확인...");
            assert controller.getGameState() != null : "GameState가 null입니다!";
            System.out.println("   - 보드 크기: " + controller.getGameState().getBoardWidth() + "x" + controller.getGameState().getBoardHeight());
            System.out.println("   - 초기 점수: " + controller.getGameState().getScore());
            System.out.println("   - 초기 레벨: " + controller.getGameState().getLevel());
            System.out.println("   ✅ GameState 확인 성공");
            
            // 6. resetGame() 테스트
            System.out.println("\n6. resetGame() 테스트...");
            controller.resetGame();
            assert controller.getGameMode() != null : "리셋 후 GameMode가 null입니다!";
            assert controller.getGameState().getScore() == 0 : "리셋 후 점수가 0이 아닙니다!";
            System.out.println("   ✅ 게임 리셋 성공");
            
            // 7. 정리
            System.out.println("\n7. 정리...");
            controller.cleanup();
            System.out.println("   ✅ 정리 성공");
            
            System.out.println("\n=== ✅ 모든 통합 테스트 통과! ===");
            
        } catch (Exception e) {
            System.err.println("\n❌ 테스트 실패!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
