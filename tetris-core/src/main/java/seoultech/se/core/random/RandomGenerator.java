package seoultech.se.core.random;

import seoultech.se.core.config.DifficultySettings;
import seoultech.se.core.model.enumType.TetrominoType;

import java.util.Random;

/**
 * 테트리스 게임용 난수 생성기
 * 
 * <p>주요 기능:</p>
 * <ul>
 *   <li>Seed 기반 재현 가능한 난수 생성</li>
 *   <li>가중치 기반 TetrominoType 생성</li>
 *   <li>난이도별 I형 블록 확률 조정</li>
 * </ul>
 * 
 * <p>사용 예시:</p>
 * <pre>{@code
 * // Seed를 사용한 재현 가능한 난수
 * RandomGenerator generator = new RandomGenerator(12345L);
 * 
 * // 난이도 설정을 반영한 블록 생성
 * DifficultySettings easy = DifficultySettings.createEasyDefaults();
 * TetrominoType type = generator.generateTetromino(easy);
 * }</pre>
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 1
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
     * 난이도 설정을 반영한 TetrominoType 생성
     * 
     * <p>가중치 기반 확률:</p>
     * <ul>
     *   <li>I형 블록: 기본 1/7, 난이도 배율 적용</li>
     *   <li>나머지 블록: 각각 1/7 균등 분배</li>
     * </ul>
     * 
     * <p>예시 (Easy 모드, I-block multiplier = 1.2):</p>
     * <ul>
     *   <li>I형: 1.2 / (1.2 + 6) = 16.7%</li>
     *   <li>나머지: 각 13.9%</li>
     * </ul>
     * 
     * @param settings 난이도 설정
     * @return 생성된 블록 타입
     */
    public TetrominoType generateTetromino(DifficultySettings settings) {
        // I형 블록 가중치 (난이도 배율 적용)
        double iBlockWeight = settings.getIBlockMultiplier();
        
        // 나머지 블록 가중치 (각 1.0)
        double otherBlockWeight = 6.0;
        
        // 전체 가중치 합계
        double totalWeight = iBlockWeight + otherBlockWeight;
        
        // 0 ~ totalWeight 범위의 난수 생성
        double randomValue = random.nextDouble() * totalWeight;
        
        // I형 블록 구간에 속하면 I 반환
        if (randomValue < iBlockWeight) {
            return TetrominoType.I;
        }
        
        // 나머지 블록 중 랜덤 선택
        TetrominoType[] otherTypes = {
            TetrominoType.O,
            TetrominoType.T,
            TetrominoType.S,
            TetrominoType.Z,
            TetrominoType.J,
            TetrominoType.L
        };
        
        int index = random.nextInt(otherTypes.length);
        return otherTypes[index];
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
