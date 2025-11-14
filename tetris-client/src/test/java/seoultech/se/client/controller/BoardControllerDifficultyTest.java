package seoultech.se.client.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.model.enumType.Difficulty;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * Phase 4 통합 테스트: 난이도 시스템이 게임 로직에 통합되었는지 검증
 * 
 * <p>테스트 범위:</p>
 * <ul>
 *   <li>난이도별 BoardController 생성</li>
 *   <li>TetrominoGenerator의 7-bag 시스템 동작</li>
 *   <li>난이도별 I형 블록 확률 분포</li>
 *   <li>난이도별 점수 배율 적용</li>
 *   <li>난이도 변경 기능</li>
 * </ul>
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 4
 */
@DisplayName("Phase 4: 난이도 시스템 게임 로직 통합 테스트")
class BoardControllerDifficultyTest {
    
    // =========================================================================
    // 1. BoardController 생성 및 난이도 설정 테스트
    // =========================================================================
    
    @Test
    @DisplayName("1-1. 기본 생성자는 NORMAL 난이도를 사용해야 함")
    void testDefaultConstructorUsesNormalDifficulty() {
        // When: 기본 생성자로 BoardController 생성
        BoardController controller = new BoardController();
        
        // Then: NORMAL 난이도가 설정되어야 함
        assertNotNull(controller.getDifficulty(), "Difficulty가 null입니다");
        assertEquals(Difficulty.NORMAL, controller.getDifficulty(), 
            "기본 난이도가 NORMAL이 아닙니다");
        
        System.out.println("✅ 기본 생성자는 NORMAL 난이도 사용: " + controller.getDifficulty());
    }
    
    @Test
    @DisplayName("1-2. Config를 받는 생성자는 NORMAL 난이도를 사용해야 함")
    void testConfigConstructorUsesNormalDifficulty() {
        // Given: GameModeConfig
        GameModeConfig config = GameModeConfig.classic();
        
        // When: Config 생성자로 BoardController 생성
        BoardController controller = new BoardController(config);
        
        // Then: NORMAL 난이도가 설정되어야 함
        assertEquals(Difficulty.NORMAL, controller.getDifficulty());
        
        System.out.println("✅ Config 생성자는 NORMAL 난이도 사용");
    }
    
    @Test
    @DisplayName("1-3. Config와 Difficulty를 받는 생성자가 정상 작동해야 함")
    void testConfigAndDifficultyConstructor() {
        // Given: GameModeConfig와 각 난이도
        GameModeConfig config = GameModeConfig.classic();
        
        // When & Then: Easy 난이도
        BoardController easyController = new BoardController(config, Difficulty.EASY);
        assertEquals(Difficulty.EASY, easyController.getDifficulty());
        
        // When & Then: Hard 난이도
        BoardController hardController = new BoardController(config, Difficulty.HARD);
        assertEquals(Difficulty.HARD, hardController.getDifficulty());
        
        System.out.println("✅ Config+Difficulty 생성자 정상 작동");
    }
    
    @Test
    @DisplayName("1-4. setDifficulty()로 난이도 변경이 가능해야 함")
    void testSetDifficulty() {
        // Given: NORMAL 난이도로 시작
        BoardController controller = new BoardController();
        assertEquals(Difficulty.NORMAL, controller.getDifficulty());
        
        // When: EASY로 변경
        controller.setDifficulty(Difficulty.EASY);
        
        // Then: 난이도가 변경되어야 함
        assertEquals(Difficulty.EASY, controller.getDifficulty());
        
        // When: HARD로 변경
        controller.setDifficulty(Difficulty.HARD);
        
        // Then: 난이도가 변경되어야 함
        assertEquals(Difficulty.HARD, controller.getDifficulty());
        
        System.out.println("✅ setDifficulty()로 난이도 변경 가능");
    }
    
    // =========================================================================
    // 2. TetrominoGenerator 통합 테스트
    // =========================================================================
    
    @Test
    @DisplayName("2-1. 7-bag 시스템이 정상 작동해야 함 (Normal 모드)")
    void testSevenBagSystemNormalMode() {
        // Given: Normal 난이도 BoardController
        BoardController controller = new BoardController(GameModeConfig.classic(), Difficulty.NORMAL);
        
        // When: 블록을 많이 생성하여 분포 확인 (700개 = 7-bag 100개)
        Map<TetrominoType, Integer> distribution = new HashMap<>();
        // Classic 모드에서는 기본 7가지 블록만 사용 (ITEM, WEIGHT_BOMB 제외)
        for (TetrominoType type : TetrominoType.values()) {
            if (type != TetrominoType.ITEM && type != TetrominoType.WEIGHT_BOMB) {
                distribution.put(type, 0);
            }
        }
        
        // 블록 생성 및 카운팅 (Next Queue를 통해 확인)
        for (int i = 0; i < 700; i++) {
            TetrominoType type = controller.getGameState().getCurrentTetromino().getType();
            distribution.put(type, distribution.get(type) + 1);
            
            // 새 블록 생성
            controller.resetGame();
        }
        
        // Then: 모든 블록이 거의 균등하게 출현해야 함 (각각 약 100개)
        System.out.println("📊 Normal 모드 블록 분포 (700개):");
        for (TetrominoType type : distribution.keySet()) {
            int count = distribution.get(type);
            double percentage = (count / 700.0) * 100;
            System.out.println("   " + type + ": " + count + " (" + 
                String.format("%.1f", percentage) + "%)");
            
            // 각 블록이 80~120개 사이 (14.3% ± 3%)
            assertTrue(count >= 80 && count <= 120, 
                type + " 블록의 출현 횟수가 예상 범위를 벗어남: " + count);
        }
        
        System.out.println("✅ 7-bag 시스템 정상 작동 (Normal)");
    }
    
    @Test
    @DisplayName("2-2. Easy 모드에서 I형 블록이 증가해야 함")
    void testEasyModeIBlockIncrease() {
        // Given: Easy 난이도 BoardController
        BoardController controller = new BoardController(GameModeConfig.classic(), Difficulty.EASY);
        
        // When: 블록을 많이 생성하여 I형 블록 비율 확인
        int iBlockCount = 0;
        int totalCount = 1000;
        
        for (int i = 0; i < totalCount; i++) {
            TetrominoType type = controller.getGameState().getCurrentTetromino().getType();
            if (type == TetrominoType.I) {
                iBlockCount++;
            }
            controller.resetGame();
        }
        
        double iBlockPercentage = (iBlockCount / (double) totalCount) * 100;
        
        // Then: I형 블록이 15% 이상 (Normal 14.3%보다 높아야 함)
        System.out.println("📊 Easy 모드 I형 블록 비율: " + 
            String.format("%.1f", iBlockPercentage) + "% (" + iBlockCount + "/" + totalCount + ")");
        
        assertTrue(iBlockPercentage > 15.0, 
            "Easy 모드의 I형 블록 비율이 너무 낮음: " + iBlockPercentage + "%");
        
        System.out.println("✅ Easy 모드: I형 블록 증가 확인");
    }
    
    @Test
    @DisplayName("2-3. Hard 모드에서 I형 블록이 감소해야 함")
    void testHardModeIBlockDecrease() {
        // Given: Hard 난이도 BoardController
        BoardController controller = new BoardController(GameModeConfig.classic(), Difficulty.HARD);
        
        // When: 블록을 많이 생성하여 I형 블록 비율 확인
        int iBlockCount = 0;
        int totalCount = 1000;
        
        for (int i = 0; i < totalCount; i++) {
            TetrominoType type = controller.getGameState().getCurrentTetromino().getType();
            if (type == TetrominoType.I) {
                iBlockCount++;
            }
            controller.resetGame();
        }
        
        double iBlockPercentage = (iBlockCount / (double) totalCount) * 100;
        
        // Then: I형 블록이 13% 이하 (Normal 14.3%보다 낮아야 함)
        System.out.println("📊 Hard 모드 I형 블록 비율: " + 
            String.format("%.1f", iBlockPercentage) + "% (" + iBlockCount + "/" + totalCount + ")");
        
        assertTrue(iBlockPercentage < 13.0, 
            "Hard 모드의 I형 블록 비율이 너무 높음: " + iBlockPercentage + "%");
        
        System.out.println("✅ Hard 모드: I형 블록 감소 확인");
    }
    
    // =========================================================================
    // 3. 점수 배율 테스트
    // =========================================================================
    
    @Test
    @DisplayName("3-1. Easy 모드는 점수가 50% 감소해야 함 (SRS 표준)")
    void testEasyModeScoreMultiplier() {
        // Given: Easy 난이도 설정 확인
        double easyScoreMultiplier = Difficulty.EASY.getScoreMultiplier();
        
        // Then: Easy 모드의 점수 배율이 0.5여야 함 (SRS 표준: 낮은 난이도 = 낮은 배율)
        assertEquals(0.5, easyScoreMultiplier, 0.001, 
            "Easy 모드의 점수 배율이 0.5가 아님");
        
        System.out.println("✅ Easy 모드 점수 배율: " + easyScoreMultiplier + "x (SRS 표준)");
    }
    
    @Test
    @DisplayName("3-2. Normal 모드는 기본 점수를 사용해야 함")
    void testNormalModeScoreMultiplier() {
        // Given: Normal 난이도 설정 확인
        double normalScoreMultiplier = Difficulty.NORMAL.getScoreMultiplier();
        
        // Then: Normal 모드의 점수 배율이 1.0이어야 함
        assertEquals(1.0, normalScoreMultiplier, 0.001);
        
        System.out.println("✅ Normal 모드 점수 배율: " + normalScoreMultiplier + "x");
    }
    
    @Test
    @DisplayName("3-3. Hard 모드는 점수가 50% 증가해야 함 (SRS 표준)")
    void testHardModeScoreMultiplier() {
        // Given: Hard 난이도 설정 확인
        double hardScoreMultiplier = Difficulty.HARD.getScoreMultiplier();
        
        // Then: Hard 모드의 점수 배율이 1.5여야 함 (SRS 표준: 높은 난이도 = 높은 배율)
        assertEquals(1.5, hardScoreMultiplier, 0.001);
        
        System.out.println("✅ Hard 모드 점수 배율: " + hardScoreMultiplier + "x (SRS 표준)");
    }
    
    // =========================================================================
    // 4. RandomGenerator & TetrominoGenerator 필드 테스트
    // =========================================================================
    
    @Test
    @DisplayName("4-1. RandomGenerator가 정상적으로 생성되어야 함")
    void testRandomGeneratorCreated() {
        // Given & When: BoardController 생성
        BoardController controller = new BoardController();
        
        // Then: RandomGenerator가 null이 아니어야 함
        assertNotNull(controller.getRandomGenerator(), 
            "RandomGenerator가 생성되지 않았습니다");
        
        System.out.println("✅ RandomGenerator 정상 생성");
    }
    
    @Test
    @DisplayName("4-2. TetrominoGenerator가 정상적으로 생성되어야 함")
    void testTetrominoGeneratorCreated() {
        // Given & When: BoardController 생성
        BoardController controller = new BoardController();
        
        // Then: TetrominoGenerator가 null이 아니어야 함
        assertNotNull(controller.getTetrominoGenerator(), 
            "TetrominoGenerator가 생성되지 않았습니다");
        
        System.out.println("✅ TetrominoGenerator 정상 생성");
    }
    
    @Test
    @DisplayName("4-3. resetGame() 시 TetrominoGenerator가 재생성되어야 함")
    void testTetrominoGeneratorResetOnGameReset() {
        // Given: BoardController 생성
        BoardController controller = new BoardController();
        var originalGenerator = controller.getTetrominoGenerator();
        
        // When: resetGame() 호출
        controller.resetGame();
        var newGenerator = controller.getTetrominoGenerator();
        
        // Then: 새로운 TetrominoGenerator가 생성되어야 함
        assertNotSame(originalGenerator, newGenerator, 
            "resetGame() 후에도 같은 TetrominoGenerator를 사용하고 있습니다");
        
        System.out.println("✅ resetGame() 시 TetrominoGenerator 재생성 확인");
    }
    
    // =========================================================================
    // 5. 전체 통합 테스트
    // =========================================================================
    
    @Test
    @DisplayName("5. 전체 시스템 통합: 난이도 → 블록 생성 → 점수 계산")
    void testFullSystemIntegration() {
        System.out.println("\n========================================");
        System.out.println("전체 시스템 통합 테스트");
        System.out.println("========================================\n");
        
        // 1. Easy 모드 검증
        System.out.println("1️⃣ Easy 모드:");
        BoardController easyController = new BoardController(GameModeConfig.classic(), Difficulty.EASY);
        assertEquals(Difficulty.EASY, easyController.getDifficulty());
        assertEquals(0.5, easyController.getDifficulty().getScoreMultiplier(), 0.001);  // SRS 표준: 낮은 난이도 = 낮은 배율
        assertNotNull(easyController.getTetrominoGenerator());
        System.out.println("   ✅ 난이도: EASY, 점수 배율: 0.5x, Generator: OK\n");
        
        // 2. Normal 모드 검증
        System.out.println("2️⃣ Normal 모드:");
        BoardController normalController = new BoardController(GameModeConfig.classic(), Difficulty.NORMAL);
        assertEquals(Difficulty.NORMAL, normalController.getDifficulty());
        assertEquals(1.0, normalController.getDifficulty().getScoreMultiplier(), 0.001);
        assertNotNull(normalController.getTetrominoGenerator());
        System.out.println("   ✅ 난이도: NORMAL, 점수 배율: 1.0x, Generator: OK\n");
        
        // 3. Hard 모드 검증
        System.out.println("3️⃣ Hard 모드:");
        BoardController hardController = new BoardController(GameModeConfig.classic(), Difficulty.HARD);
        assertEquals(Difficulty.HARD, hardController.getDifficulty());
        assertEquals(1.5, hardController.getDifficulty().getScoreMultiplier(), 0.001);  // SRS 표준: 높은 난이도 = 높은 배율
        assertNotNull(hardController.getTetrominoGenerator());
        System.out.println("   ✅ 난이도: HARD, 점수 배율: 1.5x, Generator: OK\n");
        
        System.out.println("========================================");
        System.out.println("✅ 전체 시스템 통합 성공!");
        System.out.println("========================================\n");
    }
}
