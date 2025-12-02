package seoultech.se.core;

import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * GameState의 경량 스냅샷
 * 
 * UI 변경 감지를 위해 필요한 최소한의 필드만 포함합니다.
 * deepCopy() 대신 사용하여 성능을 최적화합니다.
 * 
 * 사용 시나리오:
 * - 게임 루프 틱: oldState vs newState 비교
 * - 입력 핸들러: 변경 감지
 * - showUiHints(): UI 업데이트 결정
 * 
 * 성능 개선:
 * - 전체 GameState deepCopy: ~1-2ms (Grid 포함)
 * - GameStateSnapshot 생성: ~0.1ms 미만 (필드 복사만)
 * - 60fps 게임에서 큰 차이 발생
 */
public class GameStateSnapshot {
    // 점수/레벨/라인 관련
    private final long score;
    private final int level;
    private final int linesCleared;
    private final int lastLinesCleared;
    
    // 콤보/Back-to-Back
    private final int comboCount;
    private final int backToBackCount;
    
    // T-Spin 관련
    private final boolean lastLockWasTSpin;
    private final boolean lastLockWasTSpinMini;
    
    // Hold 관련
    private final TetrominoType heldPiece;
    private final ItemType heldItemType;
    
    // 게임 상태
    private final boolean isPaused;
    private final boolean isGameOver;
    
    // 아이템 관련
    private final ItemType nextBlockItemType;
    private final ItemType currentItemType;
    
    /**
     * GameState로부터 스냅샷 생성
     * 
     * @param state 원본 GameState
     */
    public GameStateSnapshot(GameState state) {
        // 점수/레벨/라인
        this.score = state.getScore();
        this.level = state.getLevel();
        this.linesCleared = state.getLinesCleared();
        this.lastLinesCleared = state.getLastLinesCleared();
        
        // 콤보
        this.comboCount = state.getComboCount();
        this.backToBackCount = state.getBackToBackCount();
        
        // T-Spin
        this.lastLockWasTSpin = state.isLastLockWasTSpin();
        this.lastLockWasTSpinMini = state.isLastLockWasTSpinMini();
        
        // Hold
        this.heldPiece = state.getHeldPiece();
        this.heldItemType = state.getHeldItemType();
        
        // 상태
        this.isPaused = state.isPaused();
        this.isGameOver = state.isGameOver();
        
        // 아이템
        this.nextBlockItemType = state.getNextBlockItemType();
        this.currentItemType = state.getCurrentItemType();
    }
    
    // Getters
    public long getScore() { return score; }
    public int getLevel() { return level; }
    public int getLinesCleared() { return linesCleared; }
    public int getLastLinesCleared() { return lastLinesCleared; }
    public int getComboCount() { return comboCount; }
    public int getBackToBackCount() { return backToBackCount; }
    public boolean isLastLockWasTSpin() { return lastLockWasTSpin; }
    public boolean isLastLockWasTSpinMini() { return lastLockWasTSpinMini; }
    public TetrominoType getHeldPiece() { return heldPiece; }
    public ItemType getHeldItemType() { return heldItemType; }
    public boolean isPaused() { return isPaused; }
    public boolean isGameOver() { return isGameOver; }
    public ItemType getNextBlockItemType() { return nextBlockItemType; }
    public ItemType getCurrentItemType() { return currentItemType; }
    
    /**
     * 두 스냅샷이 다른지 비교
     * 
     * @param other 비교할 스냅샷
     * @return 하나라도 다르면 true
     */
    public boolean isDifferentFrom(GameStateSnapshot other) {
        if (other == null) return true;
        
        return this.score != other.score ||
               this.level != other.level ||
               this.linesCleared != other.linesCleared ||
               this.lastLinesCleared != other.lastLinesCleared ||
               this.comboCount != other.comboCount ||
               this.backToBackCount != other.backToBackCount ||
               this.lastLockWasTSpin != other.lastLockWasTSpin ||
               this.lastLockWasTSpinMini != other.lastLockWasTSpinMini ||
               this.heldPiece != other.heldPiece ||
               this.heldItemType != other.heldItemType ||
               this.isPaused != other.isPaused ||
               this.isGameOver != other.isGameOver ||
               this.nextBlockItemType != other.nextBlockItemType ||
               this.currentItemType != other.currentItemType;
    }
    
    @Override
    public String toString() {
        return String.format("GameStateSnapshot[score=%d, level=%d, lines=%d, combo=%d, b2b=%d]",
            score, level, linesCleared, comboCount, backToBackCount);
    }
}
