package seoultech.se.core.random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import seoultech.se.core.config.DifficultySettings;
import seoultech.se.core.model.enumType.TetrominoType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RandomGenerator 단위 테스트
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 1
 */
@DisplayName("RandomGenerator 테스트")
class RandomGeneratorTest {
    
    @Test
    @DisplayName("Seed를 사용한 재현 가능한 난수 생성")
    void testSeedReproducibility() {
        RandomGenerator gen1 = new RandomGenerator(12345L);
        RandomGenerator gen2 = new RandomGenerator(12345L);
        
        DifficultySettings settings = DifficultySettings.createNormalDefaults();
        
        // 같은 Seed면 같은 결과가 나와야 함
        for (int i = 0; i < 100; i++) {
            assertEquals(
                gen1.generateTetromino(settings),
                gen2.generateTetromino(settings),
                "Seed가 같으면 같은 블록이 생성되어야 함"
            );
        }
    }
    
    @Test
    @DisplayName("Normal 모드 확률 분포 검증 (1000개)")
    void testNormalModeProbabilityDistribution() {
        RandomGenerator generator = new RandomGenerator(999L);
        DifficultySettings normal = DifficultySettings.createNormalDefaults();
        
        Map<TetrominoType, Integer> counts = new HashMap<>();
        int totalCount = 1000;
        
        // ✅ ITEM 타입을 제외한 7개 타입
        TetrominoType[] normalTypes = {
            TetrominoType.I, TetrominoType.J, TetrominoType.L,
            TetrominoType.O, TetrominoType.S, TetrominoType.T, TetrominoType.Z
        };
        
        // 1000개 생성
        for (int i = 0; i < totalCount; i++) {
            TetrominoType type = generator.generateTetromino(normal);
            counts.merge(type, 1, Integer::sum);
        }
        
        // 모든 타입이 최소 1번은 나와야 함 (ITEM 제외)
        for (TetrominoType type : normalTypes) {
            assertTrue(counts.containsKey(type), 
                type + " 타입이 생성되지 않음");
            assertTrue(counts.get(type) > 0,
                type + " 타입이 0번 생성됨");
        }
        
        // Normal 모드에서는 모든 블록이 비슷한 확률 (약 14.3% ± 5%)
        double expectedRate = 1.0 / 7.0;  // 14.3%
        double tolerance = 0.05;  // ±5%
        
        for (TetrominoType type : normalTypes) {
            double actualRate = counts.get(type) / (double) totalCount;
            assertTrue(
                Math.abs(actualRate - expectedRate) < tolerance,
                String.format(
                    "%s 확률이 예상 범위를 벗어남: 예상=%.1f%%, 실제=%.1f%%",
                    type, expectedRate * 100, actualRate * 100
                )
            );
        }
    }
    
    @Test
    @DisplayName("Easy 모드 I형 블록 증가 검증 (1000개)")
    void testEasyModeIBlockIncrease() {
        RandomGenerator generator = new RandomGenerator(777L);
        DifficultySettings easy = DifficultySettings.createEasyDefaults();
        
        Map<TetrominoType, Integer> counts = new HashMap<>();
        int totalCount = 1000;
        
        for (int i = 0; i < totalCount; i++) {
            TetrominoType type = generator.generateTetromino(easy);
            counts.merge(type, 1, Integer::sum);
        }
        
        // Easy 모드: I형 블록 약 16.7% (1.2 / (1.2 + 6))
        double iBlockRate = counts.get(TetrominoType.I) / (double) totalCount;
        
        // 16.7% ± 3% 범위 검증
        assertTrue(iBlockRate > 0.137, 
            "I형 블록 비율이 너무 낮음: " + (iBlockRate * 100) + "%");
        assertTrue(iBlockRate < 0.197,
            "I형 블록 비율이 너무 높음: " + (iBlockRate * 100) + "%");
        
        System.out.println("Easy 모드 I형 블록 비율: " + (iBlockRate * 100) + "%");
    }
    
    @Test
    @DisplayName("Hard 모드 I형 블록 감소 검증 (1000개)")
    void testHardModeIBlockDecrease() {
        RandomGenerator generator = new RandomGenerator(555L);
        DifficultySettings hard = DifficultySettings.createHardDefaults();
        
        Map<TetrominoType, Integer> counts = new HashMap<>();
        int totalCount = 1000;
        
        for (int i = 0; i < totalCount; i++) {
            TetrominoType type = generator.generateTetromino(hard);
            counts.merge(type, 1, Integer::sum);
        }
        
        // Hard 모드: I형 블록 약 11.8% (0.8 / (0.8 + 6))
        double iBlockRate = counts.get(TetrominoType.I) / (double) totalCount;
        
        // 11.8% ± 3% 범위 검증
        assertTrue(iBlockRate > 0.088,
            "I형 블록 비율이 너무 낮음: " + (iBlockRate * 100) + "%");
        assertTrue(iBlockRate < 0.148,
            "I형 블록 비율이 너무 높음: " + (iBlockRate * 100) + "%");
        
        System.out.println("Hard 모드 I형 블록 비율: " + (iBlockRate * 100) + "%");
    }
    
    @Test
    @DisplayName("nextInt 메서드 테스트")
    void testNextInt() {
        RandomGenerator generator = new RandomGenerator(123L);
        
        // 100번 실행하여 범위 검증
        for (int i = 0; i < 100; i++) {
            int value = generator.nextInt(10);
            assertTrue(value >= 0 && value < 10,
                "nextInt(10)의 결과는 0~9 범위여야 함: " + value);
        }
    }
    
    @Test
    @DisplayName("nextDouble 메서드 테스트")
    void testNextDouble() {
        RandomGenerator generator = new RandomGenerator(456L);
        
        // 100번 실행하여 범위 검증
        for (int i = 0; i < 100; i++) {
            double value = generator.nextDouble();
            assertTrue(value >= 0.0 && value < 1.0,
                "nextDouble()의 결과는 0.0~1.0 범위여야 함: " + value);
        }
    }
    
    @Test
    @DisplayName("nextBoolean 메서드 테스트")
    void testNextBoolean() {
        RandomGenerator generator = new RandomGenerator(789L);
        
        // 확률 0.7로 100번 실행
        int trueCount = 0;
        for (int i = 0; i < 100; i++) {
            if (generator.nextBoolean(0.7)) {
                trueCount++;
            }
        }
        
        // 약 70% 정도가 true여야 함 (±20% 오차 허용)
        assertTrue(trueCount > 50 && trueCount < 90,
            "확률 0.7에서 true 비율: " + trueCount + "%");
    }
    
    @Test
    @DisplayName("nextBoolean 잘못된 확률 입력")
    void testNextBooleanInvalidProbability() {
        RandomGenerator generator = new RandomGenerator();
        
        // 음수 확률
        assertThrows(IllegalArgumentException.class,
            () -> generator.nextBoolean(-0.1));
        
        // 1.0 초과
        assertThrows(IllegalArgumentException.class,
            () -> generator.nextBoolean(1.5));
    }
    
    @Test
    @DisplayName("selectRandom 메서드 테스트")
    void testSelectRandom() {
        RandomGenerator generator = new RandomGenerator(321L);
        
        TetrominoType[] types = {
            TetrominoType.I,
            TetrominoType.O,
            TetrominoType.T
        };
        
        // 100번 실행하여 모든 타입이 최소 1번은 선택되는지 확인
        Map<TetrominoType, Integer> counts = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            TetrominoType selected = generator.selectRandom(types);
            counts.merge(selected, 1, Integer::sum);
        }
        
        // 모든 타입이 최소 1번은 선택되어야 함
        for (TetrominoType type : types) {
            assertTrue(counts.containsKey(type),
                type + "이 한 번도 선택되지 않음");
        }
    }
    
    @Test
    @DisplayName("selectRandom null 배열")
    void testSelectRandomNullArray() {
        RandomGenerator generator = new RandomGenerator();
        
        assertThrows(IllegalArgumentException.class,
            () -> generator.selectRandom(null));
    }
    
    @Test
    @DisplayName("selectRandom 빈 배열")
    void testSelectRandomEmptyArray() {
        RandomGenerator generator = new RandomGenerator();
        
        assertThrows(IllegalArgumentException.class,
            () -> generator.selectRandom(new TetrominoType[0]));
    }
    
    @Test
    @DisplayName("getRandom 메서드")
    void testGetRandom() {
        RandomGenerator generator = new RandomGenerator();
        
        assertNotNull(generator.getRandom());
    }
}
