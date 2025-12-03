package seoultech.se.core.random;

import seoultech.se.core.model.enumType.Difficulty;
import seoultech.se.core.model.enumType.TetrominoType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 7-bag 알고리즘 기반 테트로미노 생성기
 * 
 * <p>7-bag 시스템:</p>
 * <ul>
 *   <li>7개의 블록(I, O, T, S, Z, J, L)을 한 "가방"에 넣음</li>
 *   <li>가방을 섞어서 순서대로 꺼냄</li>
 *   <li>가방이 비면 새로운 가방을 생성</li>
 *   <li>13개 연속으로 같은 블록이 나오지 않음을 보장</li>
 * </ul>
 * 
 * <p>난이도 조정:</p>
 * <ul>
 *   <li>Easy: 가방에 I형 블록을 추가로 넣을 확률 20%</li>
 *   <li>Normal: 기본 7-bag</li>
 *   <li>Hard: 가방에서 I형 블록을 제거할 확률 20%</li>
 * </ul>
 * 
 * <p>사용 예시:</p>
 * <pre>{@code
 * RandomGenerator random = new RandomGenerator();
 * TetrominoGenerator generator = new TetrominoGenerator(random, Difficulty.EASY);
 * 
 * TetrominoType next = generator.next();  // 다음 블록 생성
 * }</pre>
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 2
 */
public class TetrominoGenerator {
    
    private final RandomGenerator random;
    private final Difficulty difficulty;
    
    /**
     * 현재 가방 (남은 블록들)
     */
    private List<TetrominoType> currentBag;

    /**
     * 다음 가방 (미리보기 및 리필용)
     */
    private List<TetrominoType> nextBag;
    
    /**
     * 가방 크기 (기본 7개)
     */
    private static final int BAG_SIZE = 7;
    
    /**
     * Easy 모드에서 I형 블록 추가 확률
     */
    private static final double EASY_I_BLOCK_ADD_CHANCE = 0.2;
    
    /**
     * Hard 모드에서 I형 블록 제거 확률
     */
    private static final double HARD_I_BLOCK_REMOVE_CHANCE = 0.2;
    
    /**
     * 생성자
     * 
     * @param random 난수 생성기
     * @param difficulty 난이도
     */
    public TetrominoGenerator(RandomGenerator random, Difficulty difficulty) {
        this.random = random;
        this.difficulty = difficulty;
        this.currentBag = createNewBag();
        this.nextBag = createNewBag();
    }
    
    /**
     * 다음 블록 생성
     * 
     * <p>가방에서 블록을 하나 꺼냅니다.
     * 가방이 비어있으면 자동으로 새 가방을 생성합니다.</p>
     * 
     * @return 다음 테트로미노 타입
     */
    public synchronized TetrominoType next() {
        // 가방이 비었으면 리필
        if (currentBag.isEmpty()) {
            refillBag();
        }
        
        // 가방에서 첫 번째 블록 꺼내기
        return currentBag.remove(0);
    }

    private void refillBag() {
        this.currentBag = new ArrayList<>(this.nextBag);
        this.nextBag = createNewBag();
    }

    private List<TetrominoType> createNewBag() {
        List<TetrominoType> bag = new ArrayList<>();

        // 기본 7개 블록
        bag.add(TetrominoType.I);
        bag.add(TetrominoType.O);
        bag.add(TetrominoType.T);
        bag.add(TetrominoType.S);
        bag.add(TetrominoType.Z);
        bag.add(TetrominoType.J);
        bag.add(TetrominoType.L);

        // 난이도에 따라 가방 조정
        adjustBagForDifficulty(bag);

        Collections.shuffle(bag, random.getRandom());
        return bag;
    }

    private void adjustBagForDifficulty(List<TetrominoType> bag) {
        switch (difficulty) {
            case EASY:
                // 20% 확률로 I형 블록 추가
                if (random.nextBoolean(EASY_I_BLOCK_ADD_CHANCE)) {
                    bag.add(TetrominoType.I);
                }
                break;

            case HARD:
                // 20% 확률로 I형 블록 제거
                if (random.nextBoolean(HARD_I_BLOCK_REMOVE_CHANCE)) {
                    bag.remove(TetrominoType.I);
                }
                break;

            case NORMAL:
            default:
                // 조정 없음
                break;
        }
    }

    public synchronized List<TetrominoType> preview(int count) {
        List<TetrominoType> upcoming = new ArrayList<>(currentBag);
        upcoming.addAll(nextBag);

        int previewSize = Math.min(count, upcoming.size());
        return new ArrayList<>(upcoming.subList(0, previewSize));
    }
    
    /**
     * 현재 가방의 남은 블록 개수 반환
     * 
     * @return 남은 블록 개수
     */
    public int getRemainingBlocksInBag() {
        return currentBag.size();
    }
    
    /**
     * 현재 난이도 반환
     * 
     * @return 난이도
     */
    public Difficulty getDifficulty() {
        return difficulty;
    }
    
    /**
     * 통계용: 현재까지 생성된 블록 타입별 개수
     * (구현 시 필요하면 추가)
     */
    // private Map<TetrominoType, Integer> statistics = new HashMap<>();
}
