package seoultech.se.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.model.enumType.TetrominoType;
import seoultech.se.core.random.RandomGenerator;

/**
 * Tetromino의 RandomGenerator 사용 검증
 * 
 * 목적:
 * - new java.util.Random() 대신 RandomGenerator 사용 확인
 * - Seed 기반 재현 가능한 테스트 검증
 * - 아이템 마커 인덱스의 일관성 확인
 */
@DisplayName("Tetromino RandomGenerator 통합 테스트")
class TetrominoRandomGeneratorTest {

    @AfterEach
    void tearDown() {
        // 테스트 후 기본 RandomGenerator로 복원
        Tetromino.setRandomGenerator(new RandomGenerator());
    }

    @Test
    @DisplayName("동일한 Seed로 생성한 Tetromino는 동일한 markerIndex를 가진다")
    void testSameSeededTetrominosHaveSameMarkerIndex() {
        // given: Seed 12345로 설정
        Tetromino.setRandomGenerator(new RandomGenerator(12345L));
        Tetromino tetromino1 = new Tetromino(TetrominoType.T);
        int markerIndex1 = tetromino1.getItemMarkerBlockIndex();
        
        // given: 같은 Seed 12345로 재설정
        Tetromino.setRandomGenerator(new RandomGenerator(12345L));
        Tetromino tetromino2 = new Tetromino(TetrominoType.T);
        int markerIndex2 = tetromino2.getItemMarkerBlockIndex();
        
        System.out.println("Seed 12345 - First markerIndex: " + markerIndex1);
        System.out.println("Seed 12345 - Second markerIndex: " + markerIndex2);
        
        // then: 동일한 Seed이므로 같은 markerIndex
        assertEquals(markerIndex1, markerIndex2, 
            "동일한 Seed로 생성한 Tetromino는 같은 markerIndex를 가져야 함");
    }
    
    @Test
    @DisplayName("다른 Seed로 생성한 Tetromino는 다른 markerIndex를 가질 수 있다")
    void testDifferentSeededTetrominosCanHaveDifferentMarkerIndex() {
        // given: Seed 12345
        Tetromino.setRandomGenerator(new RandomGenerator(12345L));
        Tetromino tetromino1 = new Tetromino(TetrominoType.T);
        int markerIndex1 = tetromino1.getItemMarkerBlockIndex();
        
        // given: Seed 67890 (다른 Seed)
        Tetromino.setRandomGenerator(new RandomGenerator(67890L));
        Tetromino tetromino2 = new Tetromino(TetrominoType.T);
        int markerIndex2 = tetromino2.getItemMarkerBlockIndex();
        
        System.out.println("Seed 12345 - markerIndex: " + markerIndex1);
        System.out.println("Seed 67890 - markerIndex: " + markerIndex2);
        
        // then: 다른 Seed이므로 다를 수 있음 (확률적으로는 같을 수도 있음)
        // T블록은 4개 블록이므로 0~3 범위
        assertTrue(markerIndex1 >= 0 && markerIndex1 < 4);
        assertTrue(markerIndex2 >= 0 && markerIndex2 < 4);
    }
    
    @Test
    @DisplayName("같은 Seed로 여러 타입의 Tetromino를 순차적으로 생성하면 재현 가능")
    void testSequentialCreationWithSameSeed() {
        // given: Seed 99999로 설정
        Tetromino.setRandomGenerator(new RandomGenerator(99999L));
        
        Tetromino t1 = new Tetromino(TetrominoType.T);
        Tetromino i1 = new Tetromino(TetrominoType.I);
        Tetromino o1 = new Tetromino(TetrominoType.O);
        
        int t1Marker = t1.getItemMarkerBlockIndex();
        int i1Marker = i1.getItemMarkerBlockIndex();
        int o1Marker = o1.getItemMarkerBlockIndex();
        
        System.out.println("First run - T: " + t1Marker + ", I: " + i1Marker + ", O: " + o1Marker);
        
        // given: 같은 Seed 99999로 재설정
        Tetromino.setRandomGenerator(new RandomGenerator(99999L));
        
        Tetromino t2 = new Tetromino(TetrominoType.T);
        Tetromino i2 = new Tetromino(TetrominoType.I);
        Tetromino o2 = new Tetromino(TetrominoType.O);
        
        int t2Marker = t2.getItemMarkerBlockIndex();
        int i2Marker = i2.getItemMarkerBlockIndex();
        int o2Marker = o2.getItemMarkerBlockIndex();
        
        System.out.println("Second run - T: " + t2Marker + ", I: " + i2Marker + ", O: " + o2Marker);
        
        // then: 같은 순서로 생성했으므로 모두 동일
        assertEquals(t1Marker, t2Marker, "T블록 markerIndex 일치");
        assertEquals(i1Marker, i2Marker, "I블록 markerIndex 일치");
        assertEquals(o1Marker, o2Marker, "O블록 markerIndex 일치");
    }
    
    @Test
    @DisplayName("모든 Tetromino 타입에서 markerIndex가 유효한 범위 내에 있는지 확인")
    void testAllTetrominoTypesHaveValidMarkerIndex() {
        // given
        Tetromino.setRandomGenerator(new RandomGenerator(55555L));
        
        TetrominoType[] types = TetrominoType.values();
        
        // when & then
        for (TetrominoType type : types) {
            Tetromino tetromino = new Tetromino(type);
            int markerIndex = tetromino.getItemMarkerBlockIndex();
            int blockCount = countBlocks(tetromino);
            
            System.out.println(type + " - Blocks: " + blockCount + ", MarkerIndex: " + markerIndex);
            
            assertTrue(markerIndex >= 0, type + ": markerIndex >= 0");
            assertTrue(markerIndex < blockCount, type + ": markerIndex < " + blockCount);
        }
    }
    
    @Test
    @DisplayName("RandomGenerator 없이 생성하면 기본 RandomGenerator 사용")
    void testDefaultRandomGeneratorIsUsed() {
        // given: RandomGenerator 설정 없음 (기본값 사용)
        Tetromino.setRandomGenerator(new RandomGenerator());
        
        // when
        Tetromino tetromino1 = new Tetromino(TetrominoType.T);
        Tetromino tetromino2 = new Tetromino(TetrominoType.T);
        
        int marker1 = tetromino1.getItemMarkerBlockIndex();
        int marker2 = tetromino2.getItemMarkerBlockIndex();
        
        System.out.println("Default RandomGenerator - marker1: " + marker1 + ", marker2: " + marker2);
        
        // then: 기본 RandomGenerator는 시스템 시간 기반이므로 다를 수 있음
        assertTrue(marker1 >= 0 && marker1 < 4, "marker1 유효 범위");
        assertTrue(marker2 >= 0 && marker2 < 4, "marker2 유효 범위");
    }
    
    @Test
    @DisplayName("Seed를 사용하여 특정 markerIndex 재현 가능")
    void testSpecificMarkerIndexReproduction() {
        // given: 특정 Seed를 사용하여 원하는 markerIndex 생성
        // Seed 1000: T블록의 markerIndex = ?
        Tetromino.setRandomGenerator(new RandomGenerator(1000L));
        Tetromino tetromino = new Tetromino(TetrominoType.T);
        int expectedMarkerIndex = tetromino.getItemMarkerBlockIndex();
        
        System.out.println("Seed 1000 produces markerIndex: " + expectedMarkerIndex);
        
        // when: 같은 Seed로 재생성
        Tetromino.setRandomGenerator(new RandomGenerator(1000L));
        Tetromino reproducedTetromino = new Tetromino(TetrominoType.T);
        int actualMarkerIndex = reproducedTetromino.getItemMarkerBlockIndex();
        
        // then: 정확히 재현됨
        assertEquals(expectedMarkerIndex, actualMarkerIndex, 
            "Seed 1000으로 생성한 markerIndex는 항상 동일해야 함");
    }
    
    // Helper method
    private int countBlocks(Tetromino tetromino) {
        int count = 0;
        int[][] shape = tetromino.getCurrentShape();
        for (int row = 0; row < shape.length; row++) {
            for (int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    count++;
                }
            }
        }
        return count;
    }
}
