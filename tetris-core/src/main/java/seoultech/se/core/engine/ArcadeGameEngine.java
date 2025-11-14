package seoultech.se.core.engine;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.item.ItemManager;
import seoultech.se.core.item.ItemType;
import seoultech.se.core.model.enumType.TetrominoType;

/**
 * 아케이드 게임 엔진
 * 
 * ClassicGameEngine을 확장하여 아이템 시스템을 추가합니다.
 * 
 * 추가 기능:
 * - 10줄 클리어마다 아이템 생성
 * - 줄 삭제 아이템 ('L') 지원
 * - 무게추 아이템 지원
 * 
 * 설계 원칙:
 * - Template Method Pattern: ClassicGameEngine을 확장
 * - Strategy Pattern: 아이템별로 다른 전략 적용
 * - Open/Closed Principle: 새 아이템 추가 시 기존 코드 수정 불필요
 */
public class ArcadeGameEngine extends ClassicGameEngine {
    
    /**
     * 아이템 관리자
     */
    private ItemManager itemManager;
    
    /**
     * 게임 모드 설정
     */
    private GameModeConfig config;
    
    // ========== 생성자 및 초기화 ==========
    
    /**
     * 기본 생성자
     */
    public ArcadeGameEngine() {
        super();
        this.itemManager = null;
        this.config = null;
    }
    
    /**
     * ItemManager를 주입받는 생성자
     * 
     * @param itemManager 아이템 관리자
     */
    public ArcadeGameEngine(ItemManager itemManager) {
        super();
        this.itemManager = itemManager;
        this.config = null;
    }
    
    /**
     * 게임 엔진 초기화
     * 
     * @param config 게임 모드 설정
     */
    @Override
    public void initialize(GameModeConfig config) {
        super.initialize(config);
        this.config = config;
        
        // ItemManager가 이미 주입되었으면 초기화하지 않음
        if (itemManager == null && config != null && config.getItemConfig() != null) {
            this.itemManager = new ItemManager(
                config.getItemConfig().getDropRate(),
                config.getItemConfig().getEnabledItems()
            );
        }
        
        if (itemManager != null) {
            System.out.println("✅ [ArcadeGameEngine] Initialized (Arcade Mode - Items Enabled)");
            System.out.println("   - Item drop rate: " + (int)(itemManager.getItemDropRate() * 100) + "%");
            System.out.println("   - Enabled items: " + itemManager.getEnabledItems());
        } else {
            System.out.println("⚠️ [ArcadeGameEngine] Initialized but ItemManager is null!");
        }
    }
    
    /**
     * 아이템 시스템 활성화 여부
     * 
     * @return Arcade 모드는 항상 true
     */
    @Override
    public boolean isItemSystemEnabled() {
        return itemManager != null;
    }
    
    /**
     * 아이템 매니저 반환
     * 
     * @return 아이템 매니저
     */
    public ItemManager getItemManager() {
        return itemManager;
    }
    
    // ========== 아이템 시스템 오버라이드 ==========
    
    /**
     * Hold 기능 (아이템 지원)
     * 
     * ClassicGameEngine의 tryHold를 오버라이드하여 아이템 로직 추가:
     * 1. 현재 블록의 아이템 타입 저장
     * 2. 무게추 잠김 상태 저장
     * 3. Hold에서 꺼낼 때 아이템 정보 복원
     * 
     * @param state 현재 게임 상태
     * @return 새로운 게임 상태
     */
    @Override
    public GameState tryHold(GameState state) {
        // 이미 이번 턴에 Hold를 사용했는지 확인
        if (state.isHoldUsedThisTurn()) {
            return state;
        }
        
        // Next Queue 검증
        if (state.getNextQueue() == null || state.getNextQueue().length == 0) {
            System.err.println("⚠️ [ArcadeGameEngine] tryHold() failed: Next Queue is not initialized!");
            return state;
        }
        
        GameState newState = state.deepCopy();
        TetrominoType currentType = newState.getCurrentTetromino().getType();
        TetrominoType previousHeld = newState.getHeldPiece();
        
        // Phase 5: 현재 블록의 아이템 정보 저장
        seoultech.se.core.item.ItemType currentItemType = newState.getCurrentItemType();
        boolean currentWeightBombLocked = newState.isWeightBombLocked();
        
        // Phase 5: Hold된 블록의 아이템 정보 가져오기
        seoultech.se.core.item.ItemType previousItemType = newState.getHeldItemType();
        boolean previousWeightBombLocked = newState.isHeldWeightBombLocked();
        
        if (previousHeld == null) {
            // Hold가 비어있음: 현재 블록을 보관하고 Next에서 새 블록 가져오기
            newState.setHeldPiece(currentType);
            newState.setHeldItemType(currentItemType);
            newState.setHeldWeightBombLocked(currentWeightBombLocked);
            
            // Next Queue 첫 번째 요소 검증
            if (newState.getNextQueue()[0] == null) {
                System.err.println("⚠️ [ArcadeGameEngine] tryHold() failed: Next Queue[0] is null!");
                return state;
            }
            
            // 무게추는 Next Queue에서 가져오지 않음
            if (currentType == seoultech.se.core.model.enumType.TetrominoType.WEIGHT_BOMB) {
                System.out.println("⚓ [ArcadeGameEngine] WEIGHT_BOMB held - will spawn from Next Queue");
            }
            
            // Next Queue에서 새 블록 가져오기
            TetrominoType nextType = newState.getNextQueue()[0];
            seoultech.se.core.model.Tetromino newTetromino = 
                new seoultech.se.core.model.Tetromino(nextType);
            
            // 새 블록 스폰 위치 설정
            int spawnX = newState.getBoardWidth() / 2 - 1;
            int spawnY = 0;
            
            // 스폰 위치 충돌 검사
            if (!isValidPosition(newState, newTetromino, spawnX, spawnY)) {
                newState.setGameOver(true);
                newState.setGameOverReason("Cannot spawn new tetromino after hold: spawn position blocked");
                return newState;
            }
            
            // 스폰 성공
            newState.setCurrentTetromino(newTetromino);
            newState.setCurrentX(spawnX);
            newState.setCurrentY(spawnY);
            
            // 새 블록은 일반 블록 (아이템 없음)
            newState.setCurrentItemType(null);
            newState.setWeightBombLocked(false);
            
        } else {
            // Hold에 블록이 있음: 현재 블록과 교체
            newState.setHeldPiece(currentType);
            newState.setHeldItemType(currentItemType);
            newState.setHeldWeightBombLocked(currentWeightBombLocked);
            
            // Hold된 블록을 꺼내서 현재 블록으로 설정
            seoultech.se.core.model.Tetromino heldTetromino;
            
            // 무게추인 경우 특수 처리
            if (previousHeld == seoultech.se.core.model.enumType.TetrominoType.WEIGHT_BOMB) {
                heldTetromino = new seoultech.se.core.model.Tetromino(
                    seoultech.se.core.model.enumType.TetrominoType.WEIGHT_BOMB
                );
                System.out.println("⚓ [ArcadeGameEngine] Swapping WEIGHT_BOMB from Hold");
            } else {
                heldTetromino = new seoultech.se.core.model.Tetromino(previousHeld);
            }
            
            // 스폰 위치 설정
            int spawnX = newState.getBoardWidth() / 2 - 1;
            int spawnY = 0;
            
            // 스폰 위치 충돌 검사
            if (!isValidPosition(newState, heldTetromino, spawnX, spawnY)) {
                newState.setGameOver(true);
                newState.setGameOverReason("Cannot swap held tetromino: spawn position blocked");
                return newState;
            }
            
            // 스폰 성공
            newState.setCurrentTetromino(heldTetromino);
            newState.setCurrentX(spawnX);
            newState.setCurrentY(spawnY);
            
            // Hold된 블록의 아이템 정보 복원
            newState.setCurrentItemType(previousItemType);
            newState.setWeightBombLocked(previousWeightBombLocked);
            
            if (previousItemType != null) {
                System.out.println("📦 [ArcadeGameEngine] Restored item type from Hold: " + previousItemType);
            }
            if (previousWeightBombLocked) {
                System.out.println("⚓ [ArcadeGameEngine] Restored WEIGHT_BOMB locked state from Hold");
            }
        }
        
        // Hold 사용 플래그 설정
        newState.setHoldUsedThisTurn(true);
        
        // 회전 플래그 리셋
        newState.setLastActionWasRotation(false);
        
        return newState;
    }
    
    /**
     * 위치 검증 헬퍼 메서드 (ClassicGameEngine과 동일)
     */
    private boolean isValidPosition(GameState state, seoultech.se.core.model.Tetromino tetromino, int x, int y) {
        int[][] shape = tetromino.getCurrentShape();
        
        if (shape == null || shape.length == 0) {
            return false;
        }

        for(int row = 0; row < shape.length; row++){
            if (shape[row] == null || shape[row].length == 0) {
                continue;
            }
            
            for(int col = 0; col < shape[row].length; col++){
                if(shape[row][col] == 1) {
                    int absX = x + (col - tetromino.getPivotX());
                    int absY = y + (row - tetromino.getPivotY());

                    if(absX < 0 || absX >= state.getBoardWidth() || absY >= state.getBoardHeight()) {
                        return false;
                    }
                    if(absY >= 0 && state.getGrid()[absY][absX].isOccupied()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * 아래로 이동 시도 (무게추 블록 제거 지원)
     * 
     * Phase 4: 무게추가 떨어질 때마다 아래 블록 제거
     * 
     * @param state 현재 게임 상태
     * @param isSoftDrop 수동 DOWN 입력 여부
     * @return 새로운 게임 상태
     */
    @Override
    public GameState tryMoveDown(GameState state, boolean isSoftDrop) {
        // Phase 4: 무게추 낙하 중 블록 제거
        if (state.getCurrentTetromino().getType() == seoultech.se.core.model.enumType.TetrominoType.WEIGHT_BOMB) {
            // 이동 전에 아래 블록 제거
            int blocksCleared = seoultech.se.core.item.impl.WeightBombItem.processWeightBombFall(state);
            
            if (blocksCleared > 0) {
                // 점수 추가 (블록당 10점)
                state.addScore(blocksCleared * 10);
            }
        }
        
        // 기본 이동 처리
        return super.tryMoveDown(state, isSoftDrop);
    }
    
    /**
     * 테트로미노를 보드에 고정하고 라인 클리어 처리 (아이템 지원)
     * 
     * ClassicGameEngine의 lockTetromino를 오버라이드하여 아이템 로직 추가:
     * 1. 무게추 최종 처리 (Phase 4)
     * 2. 기본 고정 처리 (ClassicGameEngine)
     * 3. 'L' 마커 줄 삭제 (Phase 3)
     * 4. 라인 클리어 시 아이템 드롭 체크 (10줄마다)
     * 
     * @param state 현재 게임 상태
     * @return 고정이 완료된 새로운 게임 상태
     */
    @Override
    public GameState lockTetromino(GameState state) {
        System.out.println("🚀 [ArcadeGameEngine] lockTetromino() CALLED - Class: " + this.getClass().getSimpleName());
        
        // 1. Phase 4: 무게추 최종 처리 (고정 전)
        int weightBombScore = 0;
        GameState stateAfterWeightBomb = state;
        
        if (state.getCurrentTetromino().getType() == seoultech.se.core.model.enumType.TetrominoType.WEIGHT_BOMB) {
            // 무게추 위치 계산
            int[] weightBombX = seoultech.se.core.item.impl.WeightBombItem.getWeightBombXPositions(state);
            int weightBombY = state.getCurrentY();
            
            // 🔥 CRITICAL FIX: deepCopy 후 블록 제거
            stateAfterWeightBomb = state.deepCopy();
            
            // 수직 경로의 모든 블록 제거
            int blocksCleared = seoultech.se.core.item.impl.WeightBombItem.clearVerticalPath(
                stateAfterWeightBomb, weightBombX, weightBombY
            );
            
            // 점수 계산 (블록당 10점)
            weightBombScore = blocksCleared * 10;
            
            System.out.println("⚓ [ArcadeGameEngine] WEIGHT_BOMB cleared: " + 
                blocksCleared + " blocks, " + weightBombScore + " points");
            
            // 🔥 CRITICAL FIX: 블록 제거 후 무게추를 다시 아래로 떨어뜨림
            if (blocksCleared > 0) {
                int newY = stateAfterWeightBomb.getCurrentY();
                int maxDropDistance = stateAfterWeightBomb.getBoardHeight();
                int dropCount = 0;
                
                // 바닥까지 떨어뜨리기 (무한 루프 방지)
                while (isValidPosition(stateAfterWeightBomb, 
                                      stateAfterWeightBomb.getCurrentTetromino(), 
                                      stateAfterWeightBomb.getCurrentX(), 
                                      newY + 1) && dropCount < maxDropDistance) {
                    newY++;
                    dropCount++;
                }
                
                // 무한 루프 감지
                if (dropCount >= maxDropDistance) {
                    System.err.println("⚠️ [ArcadeGameEngine] WEIGHT_BOMB drop exceeded max distance!");
                    System.err.println("   - Current Y: " + stateAfterWeightBomb.getCurrentY());
                    System.err.println("   - Board height: " + maxDropDistance);
                }
                
                stateAfterWeightBomb.setCurrentY(newY);
                
                System.out.println("⚓ [ArcadeGameEngine] WEIGHT_BOMB dropped to Y=" + newY + 
                    " (dropped " + dropCount + " rows)");
            }
        }
        
        // 2. 기본 고정 처리 (부모 클래스)
        GameState newState = super.lockTetromino(stateAfterWeightBomb);
        
        // 게임 오버 시 early return
        if (newState.isGameOver()) {
            System.out.println("❌ [ArcadeGameEngine] Game Over detected, skipping item logic");
            return newState;
        }
        
        // Phase 4: 무게추 점수 추가
        if (weightBombScore > 0) {
            newState.addScore(weightBombScore);
        }
        
        // 3. 'L' 마커 줄 삭제 처리 (Phase 3)
        int lineClearMarkerLines = 0;
        if (itemManager != null) {
            java.util.List<Integer> markedLines = 
                seoultech.se.core.item.impl.LineClearItem.findAndClearMarkedLines(newState);
            
            if (!markedLines.isEmpty()) {
                lineClearMarkerLines = markedLines.size();
                
                // 'L' 마커 줄 삭제
                int blocksCleared = 
                    seoultech.se.core.item.impl.LineClearItem.clearLines(newState, markedLines);
                
                // 점수 추가 (줄당 100점 기본 + 블록당 10점)
                long lineBonus = markedLines.size() * 100 * newState.getLevel();
                long blockBonus = blocksCleared * 10;
                newState.addScore(lineBonus + blockBonus);
                
                // 라인 카운트 추가 (레벨업 진행을 위해)
                newState.addLinesCleared(markedLines.size());
                
                System.out.println("Ⓛ [ArcadeGameEngine] LINE_CLEAR effect: " + 
                    markedLines.size() + " line(s), " + blocksCleared + " blocks");
                System.out.println("   - Line bonus: " + lineBonus);
                System.out.println("   - Block bonus: " + blockBonus);
            }
        }
        
        // 4. 아이템 드롭 체크 (10줄마다)
        // 주의: 기본 라인 클리어 + 'L' 마커 라인 클리어 모두 포함
        int totalLinesCleared = newState.getLastLinesCleared() + lineClearMarkerLines;
        
        System.out.println("🔍 [ArcadeGameEngine] lockTetromino - itemManager: " + 
            (itemManager != null ? "initialized" : "NULL") + 
            ", lastLinesCleared: " + newState.getLastLinesCleared() +
            ", lineClearMarkerLines: " + lineClearMarkerLines +
            ", totalLinesCleared: " + totalLinesCleared);
        
        if (itemManager != null && totalLinesCleared > 0) {
            ItemType droppedItem = itemManager.checkAndGenerateItem(totalLinesCleared);
            
            if (droppedItem != null) {
                // 다음 블록에 아이템 타입 설정
                newState.setNextBlockItemType(droppedItem);
                System.out.println("🎁 [ArcadeGameEngine] Item dropped: " + droppedItem);
            }
        }
        
        // Phase 4: 무게추 상태 초기화
        newState.setWeightBombLocked(false);
        
        return newState;
    }
    
    /**
     * Hard Drop 오버라이드 - lockTetromino()를 호출하도록 수정
     * 
     * 기본 구현은 lockTetrominoInternal()을 직접 호출하여 
     * ArcadeGameEngine의 아이템 로직을 건너뛰므로,
     * lockTetromino()를 통해 호출하도록 변경
     */
    @Override
    public GameState hardDrop(GameState state) {
        System.out.println("🚀 [ArcadeGameEngine] hardDrop() CALLED");
        
        // 1. 바닥까지 이동 거리 계산 (원본 state는 수정하지 않음)
        int dropDistance = 0;
        int finalY = state.getCurrentY();

        while(isValidPosition(state, state.getCurrentTetromino(), 
                              state.getCurrentX(), finalY + 1)
        ) {
            finalY++;
            dropDistance++;
        }

        // 2. deepCopy 후 최종 위치 설정 및 점수 추가
        GameState droppedState = state.deepCopy();
        droppedState.setCurrentY(finalY);
        droppedState.addScore(dropDistance * 2);

        // 3. lockTetromino() 호출 (오버라이드된 메서드 사용)
        return lockTetromino(droppedState);
    }
}
