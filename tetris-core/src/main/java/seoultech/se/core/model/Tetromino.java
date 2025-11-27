package seoultech.se.core.model;

import lombok.Getter;
import seoultech.se.core.model.enumType.Color;
import seoultech.se.core.model.enumType.RotationDirection;
import seoultech.se.core.model.enumType.RotationState;
import seoultech.se.core.model.enumType.TetrominoType;

@Getter
public class Tetromino {
    private final TetrominoType type;
    private int[][] currentShape;
    private RotationState rotationState;

    // Constructor
    public Tetromino(TetrominoType type) {
        this.type = type;
        this.rotationState = RotationState.SPAWN;

        this.currentShape = new int[type.shape.length][];
        for (int i = 0; i < type.shape.length; i++) {
            currentShape[i] = new int[type.shape[i].length];
            System.arraycopy(type.shape[i], 0, currentShape[i], 0, type.shape[i].length);
        }
    }

    // Method to rotate the tetromino clockwise
    @Deprecated
    public void rotate() {
        if (type == TetrominoType.O) return ;

        int size = this.currentShape.length;
        int[][] rotatedShape = new int[size][size];
        for (int row=0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                rotatedShape[col][size - 1 - row] = this.currentShape[row][col];
            }
        }
        this.currentShape = rotatedShape;
        this.rotationState = rotationState.rotateClockwise();
    }

    // Method to get a new Tetromino instance with rotated shape
    public Tetromino getRotatedInstance(RotationDirection direction) {

        Tetromino rotatedTetromino = new Tetromino(this.type);
        int size = this.currentShape.length;
        int[][] rotatedShape = new int[size][size];

        if (direction == RotationDirection.CLOCKWISE) {
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    rotatedShape[col][size - 1 - row] = this.currentShape[row][col];
                }
            }
            rotatedTetromino.rotationState = this.rotationState.rotateClockwise();
        } else {
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    rotatedShape[size - 1 - col][row] = this.currentShape[row][col];
                }
            }
            rotatedTetromino.rotationState = this.rotationState.rotateCounterClockwise();
        }
        rotatedTetromino.currentShape = rotatedShape;
        return rotatedTetromino;
    }

    // Method to rotate the tetromino counter-clockwise
    public Color getColor() { return type.color; }
    public int getPivotX() { return type.pivotX; }
    public int getPivotY() { return type.pivotY; }
}
