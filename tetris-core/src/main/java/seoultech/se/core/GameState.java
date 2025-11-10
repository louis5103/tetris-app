package seoultech.se.core;

import lombok.Data;
import seoultech.se.core.item.ItemType;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.TetrominoType;

@Data
public class GameState {
    // 보드 기본 정보
    private final int boardWidth;
    private final int boardHeight;
    private final Cell[][] grid;

    // 현재 테트로미노 정보
    private Tetromino currentTetromino;
    private int currentX;
    private int currentY;
    
    // 아이템 시스템 (아케이드 모드)
    /**
     * 현재 테트로미노가 아이템 블록인지 여부
     * null이면 일반 블록, ItemType이 설정되어 있으면 아이템 블록
     */
    private ItemType currentItemType;

    // Hold 기능 관련 정보
    private boolean holdUsedThisTurn;
    private TetrominoType heldPiece;

    // Next Queue (7-bag 시스템)
    private TetrominoType[] nextQueue;

    // 게임 통계 정보
    private long score;
    private int linesCleared;
    private int level;
    private int linesForNextLevel;  // 다음 레벨까지 필요한 라인 수
    private boolean isGameOver;
    private String gameOverReason;

    // 콤보 및 백투백 정보
    private int comboCount;
    private boolean lastActionClearedLines; // 마지막 행동이 라인 클리어였는지 여부

    private int backToBackCount;
    private boolean lastClearWasDifficult; // Tetris 또는 T-spin이었는지 여부

    // Lock Delay 관련 정보
    private boolean isLockDelayActive;
    private int lockDelayResets;
    
    // 게임 상태
    private boolean isPaused;
    
    // T-Spin 감지를 위한 정보
    private boolean lastActionWasRotation;  // 마지막 액션이 회전이었는지
    private int lastRotationKickIndex;  // 회전 시 사용한 Wall Kick 인덱스 (0-4)
    
    // T-Spin 결과 메타데이터 (마지막 고정 블록에 대한 정보)
    private boolean lastLockWasTSpin;  // 마지막 고정이 T-Spin이었는지
    private boolean lastLockWasTSpinMini;  // 마지막 고정이 T-Spin Mini였는지
    
    // Phase 2: Lock 관련 메타데이터 (EventMapper가 이벤트 생성 시 사용)
    private Tetromino lastLockedTetromino;  // 마지막으로 고정된 블록
    private int lastLockedX;  // 마지막으로 고정된 블록의 X 위치
    private int lastLockedY;  // 마지막으로 고정된 블록의 Y 위치
    private int lastLinesCleared;  // 마지막 액션에서 지워진 라인 수
    private int[] lastClearedRows;  // 마지막 액션에서 지워진 라인들의 행 번호
    private long lastScoreEarned;  // 마지막 액션에서 획득한 점수
    private boolean lastIsPerfectClear;  // 마지막 액션이 Perfect Clear였는지
    private boolean lastLeveledUp;  // 마지막 액션에서 레벨업이 발생했는지


    // 생성자
    public GameState(int width, int height) {
        this.boardWidth = width;
        this.boardHeight = height;
        this.grid = new Cell[height][width];

        // Cell 초기화
        for(int row = 0; row < height; row++) {
            for(int col = 0; col < width; col++) {
                grid[row][col] = Cell.empty();
            }
        }
        
        // Next Queue 초기화
        this.nextQueue = new TetrominoType[6]; // 6개를 미리 보기.

        // 초기 통계값.
        this.score = 0;
        this.level = 1;
        this.linesCleared = 0;
        this.linesForNextLevel = 10;  // 레벨 1에서는 10라인으로 레벨업
        this.isGameOver = false;

        // 콤보/B2B 초기화
        this.comboCount = 0;
        this.backToBackCount = 0;
        this.lastActionClearedLines = false;
        this.lastClearWasDifficult = false;

        // Hold 초기화
        this.heldPiece = null;
        this.holdUsedThisTurn = false;
        
        // 아이템 시스템 초기화
        this.currentItemType = null;

        // Lock Delay 초기화
        this.isLockDelayActive = false;
        this.lockDelayResets = 0;
        
        // 게임 상태 초기화
        this.isPaused = false;
        
        // T-Spin 감지 초기화
        this.lastActionWasRotation = false;
        this.lastRotationKickIndex = 0;
        this.lastLockWasTSpin = false;
        this.lastLockWasTSpinMini = false;
        
        // Phase 2: Lock 메타데이터 초기화
        this.lastLockedTetromino = null;
        this.lastLockedX = 0;
        this.lastLockedY = 0;
        this.lastLinesCleared = 0;
        this.lastClearedRows = new int[0];
        this.lastScoreEarned = 0;
        this.lastIsPerfectClear = false;
        this.lastLeveledUp = false;
    }
    
    // 깊은 복사.
    public GameState deepCopy() {
        GameState copy = new GameState(boardWidth, boardHeight);

        // grid 깊은 복사 - 각 셀 초기화.
        for (int row = 0; row < boardHeight; row++) {
            for (int col = 0; col < boardWidth; col++) {
                copy.grid[row][col] = this.grid[row][col].copy();
            }
        }

        // 현재 테트로미노 복사
        copy.currentTetromino = this.currentTetromino != null ? this.currentTetromino : null;
        copy.currentX = this.currentX;
        copy.currentY = this.currentY;
        
        // 아이템 타입 복사
        copy.currentItemType = this.currentItemType;

        // Hold 기능 관련 정보 복사
        copy.holdUsedThisTurn = this.holdUsedThisTurn;
        copy.heldPiece = this.heldPiece;

        // Next Queue 복사
        if(this.nextQueue != null) {
            copy.nextQueue = this.nextQueue.clone();
        }

        // 통계 정보 복사
        copy.score = this.score;
        copy.linesCleared = this.linesCleared;
        copy.level = this.level;
        copy.linesForNextLevel = this.linesForNextLevel;
        copy.isGameOver = this.isGameOver;
        copy.gameOverReason = this.gameOverReason;

        // 콤보/B2B 복사
        copy.comboCount = this.comboCount;
        copy.lastActionClearedLines = this.lastActionClearedLines;

        copy.backToBackCount = this.backToBackCount;
        copy.lastClearWasDifficult = this.lastClearWasDifficult;
        
        // Lock Delay 복사
        copy.isLockDelayActive = this.isLockDelayActive;
        copy.lockDelayResets = this.lockDelayResets;
        
        // 게임 상태 복사
        copy.isPaused = this.isPaused;
        
        // T-Spin 관련 복사
        copy.lastActionWasRotation = this.lastActionWasRotation;
        copy.lastRotationKickIndex = this.lastRotationKickIndex;
        copy.lastLockWasTSpin = this.lastLockWasTSpin;
        copy.lastLockWasTSpinMini = this.lastLockWasTSpinMini;
        
        // Phase 2: Lock 메타데이터 복사
        copy.lastLockedTetromino = this.lastLockedTetromino;
        copy.lastLockedX = this.lastLockedX;
        copy.lastLockedY = this.lastLockedY;
        copy.lastLinesCleared = this.lastLinesCleared;
        copy.lastClearedRows = this.lastClearedRows != null ? this.lastClearedRows.clone() : new int[0];
        copy.lastScoreEarned = this.lastScoreEarned;
        copy.lastIsPerfectClear = this.lastIsPerfectClear;
        copy.lastLeveledUp = this.lastLeveledUp;
        
        return copy;
    }

    public void addScore(long points) {
        this.score += points;
    }

    /**
     * 클리어한 라인 수를 추가하고 레벨업을 체크합니다
     * 
     * 표준 테트리스 레벨 시스템:
     * - 레벨 1 → 2: 10라인 필요
     * - 레벨 2 → 3: 20라인 필요 (추가로)
     * - 레벨 3 → 4: 30라인 필요 (추가로)
     * - ...
     * - 최대 레벨: 15
     * 
     * @param count 클리어한 라인 수
     * @return 레벨업이 발생했으면 true
     */
    public boolean addLinesCleared(int count) {
        int previousLevel = this.level;
        this.linesCleared += count;
        
        // 레벨업 체크
        while (this.linesCleared >= this.linesForNextLevel && this.level < 15) {
            // 레벨업!
            this.level++;
            
            // 다음 레벨까지 필요한 라인 수 업데이트
            // 표준 테트리스: 각 레벨마다 누적 10라인씩 추가 필요 (레벨 2: 20, 레벨 3: 30, ...)
            this.linesForNextLevel = this.level * 10;
        }
        
        // 레벨업이 발생했는지 반환
        return this.level > previousLevel;
    }
}
