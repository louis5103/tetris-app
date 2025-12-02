package seoultech.se.core.random;

import seoultech.se.core.model.enumType.TetrominoType;

import java.util.Random;

/**
 * 테트리스 게임용 난수 생성기 (단순화)
 * 
 * <p>주요 기능:</p>
 * <ul>
 *   <li>Seed 기반 재현 가능한 난수 생성</li>
 *   <li>균등한 확률의 TetrominoType 생성</li>
 * </ul>
 * 
 * <p>사용 예시:</p>
 * <pre>{@code
 * // Seed를 사용한 재현 가능한 난수
 * RandomGenerator generator = new RandomGenerator(12345L);
 * TetrominoType type = generator.generateTetromino();
 * }</pre>
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 1 (리팩토링: Phase 5)
 */
public class RandomGenerator {
    
    private final Random random;
    
    /**
     * 기본 생성자 (시스템 시간 기반 Seed)
     */
    public RandomGenerator() {
        this.random = new Random();
    }
    
    /**
     * Seed를 지정한 생성자 (테스트용 - 재현 가능)
     * 
     * @param seed 난수 생성 시드
     */
    public RandomGenerator(long seed) {
        this.random = new Random(seed);
    }
    
    /**
     * TetrominoType 생성 (균등 확률)
     * 
     * <p>모든 블록 타입이 동일한 확률(1/7)로 생성됩니다.</p>
     * 
     * @return 생성된 블록 타입
     */
    public TetrominoType generateTetromino() {
        // 균등한 확률로 블록 타입 선택
        TetrominoType[] allTypes = TetrominoType.values();
        int index = random.nextInt(allTypes.length);
        return allTypes[index];
    }
    
    /**
     * 0 이상 bound 미만의 정수 난수 생성
     * 
     * @param bound 상한 (exclusive)
     * @return 생성된 난수
     */
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }
    
    /**
     * 0.0 이상 1.0 미만의 실수 난수 생성
     * 
     * @return 생성된 난수
     */
    public double nextDouble() {
        return random.nextDouble();
    }
    
    /**
     * true/false를 주어진 확률로 생성
     * 
     * @param probability true가 나올 확률 (0.0 ~ 1.0)
     * @return 확률에 따른 boolean 값
     */
    public boolean nextBoolean(double probability) {
        if (probability < 0.0 || probability > 1.0) {
            throw new IllegalArgumentException(
                "Probability must be between 0.0 and 1.0, but was: " + probability
            );
        }
        return random.nextDouble() < probability;
    }
    
    /**
     * 배열에서 랜덤 요소 선택
     * 
     * @param array 선택할 배열
     * @param <T> 배열 요소 타입
     * @return 랜덤 선택된 요소
     */
    public <T> T selectRandom(T[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException("Array cannot be null or empty");
        }
        int index = random.nextInt(array.length);
        return array[index];
    }
    
    /**
     * 현재 Random 인스턴스 반환 (고급 사용)
     * 
     * @return Random 인스턴스
     */
    public Random getRandom() {
        return random;
    }
}
