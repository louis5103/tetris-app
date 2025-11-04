package seoultech.se.core.random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import seoultech.se.core.model.enumType.Difficulty;
import seoultech.se.core.model.enumType.TetrominoType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TetrominoGenerator 단위 테스트
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 2
 */
@DisplayName("TetrominoGenerator 테스트")
class TetrominoGeneratorTest {
    
    @Test
    @DisplayName("7-bag 시스템 기본 동작")
    void test7BagSystemBasic() {
        RandomGenerator random = new RandomGenerator(123L);
        TetrominoGenerator generator = new TetrominoGenerator(random, Difficulty.NORMAL);
        
        Set<TetrominoType> firstBag = new HashSet<>();
        
        // 첫 7개 블록 생성
        for (int i = 0; i < 7; i++) {
            TetrominoType type = generator.next();
            firstBag.add(type);
        }
        
        // 첫 7개는 모든 타입이 정확히 1번씩 나와야 함
        assertEquals(7, firstBag.size(), "첫 7개는 모든 타입이 나와야 함");
        
        // ✅ ITEM 타입을 제외한 모든 타입이 포함되어 있는지 확인
        TetrominoType[] normalTypes = {
            TetrominoType.I, TetrominoType.J, TetrominoType.L,
            TetrominoType.O, TetrominoType.S, TetrominoType.T, TetrominoType.Z
        };
        
        for (TetrominoType type : normalTypes) {
            assertTrue(firstBag.contains(type), type + " 타입이 누락됨");
        }
    }
    
    @Test
    @DisplayName("14개 연속 생성 - 두 번째 가방 자동 생성")
    void test14BlocksGeneration() {
        RandomGenerator random = new RandomGenerator(456L);
        TetrominoGenerator generator = new TetrominoGenerator(random, Difficulty.NORMAL);
        
        Set<TetrominoType> all14 = new HashSet<>();
        
        // 14개 생성 (가방 2개)
        for (int i = 0; i < 14; i++) {
            TetrominoType type = generator.next();
            all14.add(type);
        }
        
        // 14개 안에 모든 타입이 최소 1번은 나와야 함
        assertEquals(7, all14.size());
    }
    
    @Test
    @DisplayName("Normal 모드 확률 분포 (700개 = 가방 100개)")
    void testNormalModeProbability() {
        RandomGenerator random = new RandomGenerator(789L);
        TetrominoGenerator generator = new TetrominoGenerator(random, Difficulty.NORMAL);
        
        Map<TetrominoType, Integer> counts = new HashMap<>();
        int totalCount = 700;  // 정확히 100개 가방
        
        // ✅ ITEM 타입을 제외한 7개 타입 초기화
        TetrominoType[] normalTypes = {
            TetrominoType.I, TetrominoType.J, TetrominoType.L,
            TetrominoType.O, TetrominoType.S, TetrominoType.T, TetrominoType.Z
        };
        for (TetrominoType type : normalTypes) {
            counts.put(type, 0);
        }
        
        for (int i = 0; i < totalCount; i++) {
            TetrominoType type = generator.next();
            counts.merge(type, 1, Integer::sum);
        }
        
        // Normal 모드: 정확히 균등 분배 (각 100번씩)
        // ✅ ITEM 타입을 제외한 7개 타입만 체크
        for (TetrominoType type : normalTypes) {
            int count = counts.get(type);
            assertEquals(100, count, 
                type + " 타입이 정확히 100번 나와야 함 (실제: " + count + ")");
        }
    }
    
    @Test
    @DisplayName("Easy 모드 I형 블록 증가")
    void testEasyModeIBlockIncrease() {
        RandomGenerator random = new RandomGenerator(111L);
        TetrominoGenerator generator = new TetrominoGenerator(random, Difficulty.EASY);
        
        Map<TetrominoType, Integer> counts = new HashMap<>();
        int totalCount = 1000;
        
        for (int i = 0; i < totalCount; i++) {
            TetrominoType type = generator.next();
            counts.merge(type, 1, Integer::sum);
        }
        
        // Easy 모드: I형 블록이 다른 블록보다 많아야 함
        int iBlockCount = counts.get(TetrominoType.I);
        int avgOtherCount = counts.values().stream()
            .filter(count -> count != iBlockCount)
            .mapToInt(Integer::intValue)
            .sum() / 6;
        
        assertTrue(iBlockCount > avgOtherCount,
            String.format("Easy 모드에서 I형(%d) > 평균(%d)이어야 함",
                iBlockCount, avgOtherCount));
        
        System.out.println("Easy 모드 I형 블록: " + iBlockCount + " / " + totalCount);
    }
    
    @Test
    @DisplayName("Hard 모드 I형 블록 감소")
    void testHardModeIBlockDecrease() {
        RandomGenerator random = new RandomGenerator(222L);
        TetrominoGenerator generator = new TetrominoGenerator(random, Difficulty.HARD);
        
        Map<TetrominoType, Integer> counts = new HashMap<>();
        int totalCount = 1000;
        
        for (int i = 0; i < totalCount; i++) {
            TetrominoType type = generator.next();
            counts.merge(type, 1, Integer::sum);
        }
        
        // Hard 모드: I형 블록이 다른 블록보다 적어야 함
        int iBlockCount = counts.get(TetrominoType.I);
        int avgOtherCount = counts.values().stream()
            .filter(count -> count != iBlockCount)
            .mapToInt(Integer::intValue)
            .sum() / 6;
        
        assertTrue(iBlockCount < avgOtherCount,
            String.format("Hard 모드에서 I형(%d) < 평균(%d)이어야 함",
                iBlockCount, avgOtherCount));
        
        System.out.println("Hard 모드 I형 블록: " + iBlockCount + " / " + totalCount);
    }
    
    @Test
    @DisplayName("preview 메서드 - 가방 수정 안 됨")
    void testPreviewDoesNotModifyBag() {
        RandomGenerator random = new RandomGenerator(333L);
        TetrominoGenerator generator = new TetrominoGenerator(random, Difficulty.NORMAL);
        
        // 현재 남은 블록 수 저장
        int initialRemaining = generator.getRemainingBlocksInBag();
        
        // 미리보기 (가방 수정하지 않아야 함)
        List<TetrominoType> preview = generator.preview(5);
        
        // 검증
        assertEquals(5, preview.size());
        assertEquals(initialRemaining, generator.getRemainingBlocksInBag(),
            "preview는 가방을 수정하지 않아야 함");
    }
    
    @Test
    @DisplayName("getRemainingBlocksInBag 메서드")
    void testGetRemainingBlocksInBag() {
        RandomGenerator random = new RandomGenerator(444L);
        TetrominoGenerator generator = new TetrominoGenerator(random, Difficulty.NORMAL);
        
        // 초기 가방 크기 (7개)
        int initial = generator.getRemainingBlocksInBag();
        assertTrue(initial >= 7 && initial <= 8,  // Easy 모드가 아니면 7개
            "초기 가방 크기: " + initial);
        
        // 3개 꺼내기
        generator.next();
        generator.next();
        generator.next();
        
        // 남은 개수 확인
        int remaining = generator.getRemainingBlocksInBag();
        assertEquals(initial - 3, remaining);
    }
    
    @Test
    @DisplayName("getDifficulty 메서드")
    void testGetDifficulty() {
        RandomGenerator random = new RandomGenerator();
        
        TetrominoGenerator easyGen = new TetrominoGenerator(random, Difficulty.EASY);
        assertEquals(Difficulty.EASY, easyGen.getDifficulty());
        
        TetrominoGenerator normalGen = new TetrominoGenerator(random, Difficulty.NORMAL);
        assertEquals(Difficulty.NORMAL, normalGen.getDifficulty());
        
        TetrominoGenerator hardGen = new TetrominoGenerator(random, Difficulty.HARD);
        assertEquals(Difficulty.HARD, hardGen.getDifficulty());
    }
    
    @Test
    @DisplayName("Seed 재현성 테스트")
    void testSeedReproducibility() {
        RandomGenerator random1 = new RandomGenerator(555L);
        RandomGenerator random2 = new RandomGenerator(555L);
        
        TetrominoGenerator gen1 = new TetrominoGenerator(random1, Difficulty.NORMAL);
        TetrominoGenerator gen2 = new TetrominoGenerator(random2, Difficulty.NORMAL);
        
        // 같은 Seed면 같은 순서로 생성되어야 함
        for (int i = 0; i < 50; i++) {
            assertEquals(gen1.next(), gen2.next(),
                i + "번째 블록이 다름");
        }
    }
}
