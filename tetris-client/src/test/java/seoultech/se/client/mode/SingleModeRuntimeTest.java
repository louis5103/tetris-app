package seoultech.se.client.mode;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.mode.GameModeType;

/**
 * SingleMode 런타임 테스트
 * 
 * 기본 기능이 정상 동작하는지 확인합니다.
 */
public class SingleModeRuntimeTest {
    
    public static void main(String[] args) {
        System.out.println("=== SingleMode 런타임 테스트 시작 ===\n");
        
        try {
            // 1. SingleMode 생성 테스트
            System.out.println("1. SingleMode 인스턴스 생성...");
            SingleMode singleMode = new SingleMode();
            System.out.println("   ✅ 생성 성공");
            
            // 2. 기본 설정 확인
            System.out.println("\n2. 기본 설정 확인...");
            GameModeConfig config = singleMode.getConfig();
            System.out.println("   - 하드드롭 활성화: " + config.isHardDropEnabled());
            System.out.println("   - 홀드 활성화: " + config.isHoldEnabled());
            System.out.println("   - 시작 레벨: " + config.getStartLevel());
            System.out.println("   ✅ 설정 조회 성공");
            
            // 3. GameState 초기화 테스트
            System.out.println("\n3. GameState 초기화...");
            GameState gameState = new GameState(10, 20);
            singleMode.initialize(gameState);
            System.out.println("   ✅ 초기화 성공");
            
            // 4. 타입 확인
            System.out.println("\n4. 게임 모드 타입 확인...");
            GameModeType type = singleMode.getType();
            System.out.println("   - 모드 타입: " + type);
            assert type == GameModeType.SINGLE : "모드 타입이 SINGLE이 아닙니다!";
            System.out.println("   ✅ 타입 확인 성공");
            
            // 5. 커스텀 설정 테스트
            System.out.println("\n5. 커스텀 설정 테스트...");
            GameModeConfig customConfig = GameModeConfig.builder()
                .hardDropEnabled(false)
                .holdEnabled(false)
                .startLevel(5)
                .build();
            singleMode.setConfig(customConfig);
            
            GameModeConfig newConfig = singleMode.getConfig();
            System.out.println("   - 하드드롭 활성화: " + newConfig.isHardDropEnabled());
            System.out.println("   - 홀드 활성화: " + newConfig.isHoldEnabled());
            System.out.println("   - 시작 레벨: " + newConfig.getStartLevel());
            assert !newConfig.isHardDropEnabled() : "하드드롭이 비활성화되지 않았습니다!";
            assert !newConfig.isHoldEnabled() : "홀드가 비활성화되지 않았습니다!";
            assert newConfig.getStartLevel() == 5 : "시작 레벨이 5가 아닙니다!";
            System.out.println("   ✅ 커스텀 설정 성공");
            
            // 6. 정리
            System.out.println("\n6. 정리...");
            singleMode.cleanup();
            System.out.println("   ✅ 정리 성공");
            
            System.out.println("\n=== ✅ 모든 테스트 통과! ===");
            
        } catch (Exception e) {
            System.err.println("\n❌ 테스트 실패!");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
