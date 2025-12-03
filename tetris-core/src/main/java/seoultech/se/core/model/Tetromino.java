package seoultech.se.core.model;

import lombok.Getter;
import seoultech.se.core.model.enumType.Color;
import seoultech.se.core.model.enumType.RotationDirection;
import seoultech.se.core.model.enumType.RotationState;
import seoultech.se.core.model.enumType.TetrominoType;
import seoultech.se.core.random.RandomGenerator;

@Getter
public class Tetromino {
    
    /**
     * 전역 RandomGenerator (재현 가능한 테스트를 위해)
     * - 기본값: 시스템 시간 기반 난수
     * - 테스트 시: setRandomGenerator(new RandomGenerator(seed))로 주입
     */
    private static RandomGenerator randomGenerator = new RandomGenerator();
    
    /**
     * RandomGenerator 설정 (테스트용)
     * @param generator 사용할 RandomGenerator
     */
    public static void setRandomGenerator(RandomGenerator generator) {
        randomGenerator = generator;
    }
    
    private final TetrominoType type;
    private int[][] currentShape;
    private RotationState rotationState;
    
    /**
     * 아이템 마커가 부착된 블록의 인덱스 (0-based)
     * - 테트로미노 생성 시 RandomGenerator로 결정
     * - rotate/move 시에도 동일한 블록에 마커 유지
     * - 예: T블록이 4개 블록을 가질 때, 0~3 중 하나
     */
    private final int itemMarkerBlockIndex;

    // Constructor
    public Tetromino(TetrominoType type) {
        this.type = type;
        this.rotationState = RotationState.SPAWN;

        this.currentShape = new int[type.shape.length][];
        for (int i = 0; i < type.shape.length; i++) {
            currentShape[i] = new int[type.shape[i].length];
            System.arraycopy(type.shape[i], 0, currentShape[i], 0, type.shape[i].length);
        }
        
        // 아이템 마커 인덱스 RandomGenerator로 결정 (생성 시 한 번만)
        int blockCount = 0;
        for (int row = 0; row < currentShape.length; row++) {
            for (int col = 0; col < currentShape[row].length; col++) {
                if (currentShape[row][col] == 1) {
                    blockCount++;
                }
            }
        }
        // ✅ FIX: RandomGenerator 사용 (재현 가능한 테스트 지원)
        this.itemMarkerBlockIndex = randomGenerator.nextInt(Math.max(1, blockCount));
    }

    // Method to rotate the tetromino clockwise
    @Deprecated
    public void rotate() {

        int height = this.currentShape.length;
        int width = this.currentShape[0].length;
        int[][] rotatedShape = new int[width][height];
        
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                rotatedShape[col][height - 1 - row] = this.currentShape[row][col];
            }
        }
        this.currentShape = rotatedShape;
        this.rotationState = rotationState.rotateClockwise();
    }

    /**
     * Private constructor for rotation (preserves itemMarkerBlockIndex)
     */
    private Tetromino(TetrominoType type, int itemMarkerBlockIndex) {
        this.type = type;
        this.rotationState = RotationState.SPAWN;
        this.itemMarkerBlockIndex = itemMarkerBlockIndex;
        
        this.currentShape = new int[type.shape.length][];
        for (int i = 0; i < type.shape.length; i++) {
            currentShape[i] = new int[type.shape[i].length];
            System.arraycopy(type.shape[i], 0, currentShape[i], 0, type.shape[i].length);
        }
    }

    // Method to get a new Tetromino instance with rotated shape
    public Tetromino getRotatedInstance(RotationDirection direction) {

        // ✅ 아이템 마커 인덱스 보존
        Tetromino rotatedTetromino = new Tetromino(this.type, this.itemMarkerBlockIndex);
        int height = this.currentShape.length;
        int width = this.currentShape[0].length;

        if (direction == RotationDirection.CLOCKWISE) {
            int[][] rotatedShape = new int[width][height];
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    rotatedShape[col][height - 1 - row] = this.currentShape[row][col];
                }
            }
            rotatedTetromino.currentShape = rotatedShape;
            rotatedTetromino.rotationState = this.rotationState.rotateClockwise();
        } else {
            int[][] rotatedShape = new int[width][height];
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    rotatedShape[width - 1 - col][row] = this.currentShape[row][col];
                }
            }
            rotatedTetromino.currentShape = rotatedShape;
            rotatedTetromino.rotationState = this.rotationState.rotateCounterClockwise();
        }
        return rotatedTetromino;
    }

    // Method to rotate the tetromino counter-clockwise
    public Color getColor() { return type.color; }
    public int getPivotX() { return type.pivotX; }
    public int getPivotY() { return type.pivotY; }
}
