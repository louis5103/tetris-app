package seoultech.se.core.engine;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.engine.item.ItemManager;
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
     * 아이템 관리자 (불변)
     */
    private final ItemManager itemManager;

    // ========== 생성자 및 초기화 ==========

    /**
     * 기본 생성자 (Arcade 기본 설정)
     */
    public ArcadeGameEngine() {
        this(GameModeConfig.createDefaultArcade());
    }

    /**
     * 생성자 (Config 주입)
     *
     * @param config 게임 모드 설정
     */
    public ArcadeGameEngine(GameModeConfig config) {
        super(config);

        // GameModeConfig에서 직접 ItemManager 생성 (ItemConfig 제거)
        if (config != null && config.isItemSystemEnabled()) {
            this.itemManager = new ItemManager(
                config.getLinesPerItem(),
                config.getEnabledItemTypes()
            );
            System.out.println("[Engine] ArcadeGameEngine initialized - Items enabled (" + 
                itemManager.getEnabledItems().size() + " types, " +
                config.getLinesPerItem() + " lines per item)");
        } else {
            this.itemManager = new ItemManager();
            System.out.println("[Engine] ArcadeGameEngine initialized - Default item config");
        }
    }

    /**
     * 게임 엔진 초기화
     *
     * @deprecated Stateless 리팩토링으로 생성자 주입 방식으로 변경됨
     * @param config 게임 모드 설정
     */
    @Override
    @Deprecated
    public void initialize(GameModeConfig config) {
        System.out.println("⚠️ [ArcadeGameEngine] initialize() is deprecated - use constructor injection");
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
        
        // 📝 Note: 이 deepCopy는 GameEngine의 불변성 패턴 유지 (아이템 효과와 무관)
        GameState newState = state.deepCopy();
        TetrominoType currentType = newState.getCurrentTetromino().getType();
        TetrominoType previousHeld = newState.getHeldPiece();
        
        // Phase 5: 현재 블록의 아이템 정보 저장
        seoultech.se.core.engine.item.ItemType currentItemType = newState.getCurrentItemType();
        boolean currentWeightBombLocked = newState.isWeightBombLocked();
        
        // Phase 5: Hold된 블록의 아이템 정보 가져오기
        seoultech.se.core.engine.item.ItemType previousItemType = newState.getHeldItemType();
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
            int blocksCleared = seoultech.se.core.engine.item.impl.WeightBombItem.processWeightBombFall(state);
            
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
        
        // 🔥 CRITICAL: Pivot 위치를 미리 저장 (lockTetromino 후 currentTetromino가 null이 되기 때문)
        int originalPivotX = state.getCurrentX();
        int originalPivotY = state.getCurrentY();
        seoultech.se.core.engine.item.ItemType originalItemType = state.getCurrentItemType();
        
        // 1. Phase 4: 무게추 최종 처리 (고정 전)
        int weightBombScore = 0;
        GameState stateAfterWeightBomb = state;
        
        if (state.getCurrentTetromino().getType() == seoultech.se.core.model.enumType.TetrominoType.WEIGHT_BOMB) {
            // 무게추 위치 계산
            int[] weightBombX = seoultech.se.core.engine.item.impl.WeightBombItem.getWeightBombXPositions(state);
            int weightBombY = state.getCurrentY();
            
            // 📝 아이템 데이터 처리 통일: WEIGHT_BOMB도 원본 직접 수정으로 변경
            // deepCopy 제거하여 LINE_CLEAR/BOMB/PLUS와 동일한 방식으로 처리
            // 성능 향상: 불필요한 전체 GameState 복사 제거
            stateAfterWeightBomb = state;
            
            // 수직 경로의 모든 블록 제거
            int blocksCleared = seoultech.se.core.engine.item.impl.WeightBombItem.clearVerticalPath(
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
        // 🔥 IMPORTANT: super.lockTetromino()가 먼저 호출되어야 Grid에 블록과 마커가 추가됨
        GameState newState = super.lockTetromino(stateAfterWeightBomb);
        
        // 게임 오버 시 early return
        if (newState.isGameOver()) {
            System.out.println("❌ [ArcadeGameEngine] Game Over detected, skipping item logic");
            return newState;
        }
        
        // 2.5. LINE_CLEAR 마커 처리 (블록 고정 후)
        // 🔥 FIX: super.lockTetromino() 후에 마커가 Grid에 추가되므로 이제 처리 가능
        // ⚠️ GameState 수정 방식: clearLines()가 newState의 Grid를 **직접 수정** (참조)
        int lineClearMarkerLines = 0;
        long lineClearScore = 0;
        
        if (itemManager != null) {
            java.util.List<Integer> markedLines = 
                seoultech.se.core.engine.item.impl.LineClearItem.findAndClearMarkedLines(newState);
            
            if (!markedLines.isEmpty()) {
                lineClearMarkerLines = markedLines.size();
                
                // 'L' 마커 줄 삭제 (GameState.grid를 직접 수정)
                int blocksCleared = 
                    seoultech.se.core.engine.item.impl.LineClearItem.clearLines(newState, markedLines);
                
                // 점수 계산
                long lineBonus = markedLines.size() * 100 * newState.getLevel();
                long blockBonus = blocksCleared * 10;
                lineClearScore = lineBonus + blockBonus;
                
                System.out.println("Ⓛ [ArcadeGameEngine] LINE_CLEAR effect (after lock): " + 
                    markedLines.size() + " line(s), " + blocksCleared + " blocks");
                System.out.println("   - Line bonus: " + lineBonus);
                System.out.println("   - Block bonus: " + blockBonus);
                
                // 점수 및 라인 카운트 추가
                newState.addScore(lineClearScore);
                newState.addLinesCleared(lineClearMarkerLines);
                
                // 🔥 FIX: LINE_CLEAR로 삭제된 줄을 lastClearedRows에 기록 (애니메이션 표시)
                // 기존 lastClearedRows와 병합
                int[] existingClearedRows = newState.getLastClearedRows();
                int[] allClearedRows = new int[existingClearedRows.length + markedLines.size()];
                
                // 기존 클리어된 줄 복사
                System.arraycopy(existingClearedRows, 0, allClearedRows, 0, existingClearedRows.length);
                
                // LINE_CLEAR로 클리어된 줄 추가
                for (int i = 0; i < markedLines.size(); i++) {
                    allClearedRows[existingClearedRows.length + i] = markedLines.get(i);
                }
                
                newState.setLastClearedRows(allClearedRows);
                
                System.out.println("🎬 [ArcadeGameEngine] Updated lastClearedRows for animation: " + 
                    java.util.Arrays.toString(allClearedRows));
            }
        }
        
        // 2.6. 아이템 효과 적용 (BOMB, PLUS 등)
        // ⚠️ GameState 수정 방식: item.apply()가 newState의 Grid를 **직접 수정** (참조)
        int itemEffectLinesCleared = 0;
        
        if (originalItemType != null && itemManager != null) {
            // WEIGHT_BOMB과 LINE_CLEAR는 이미 처리됨
            if (originalItemType != seoultech.se.core.engine.item.ItemType.WEIGHT_BOMB &&
                originalItemType != seoultech.se.core.engine.item.ItemType.LINE_CLEAR) {
                
                // Pivot 위치는 미리 저장한 원본 값 사용
                // (lockTetromino 후 currentTetromino가 null이 되므로)
                int pivotX = originalPivotX;
                int pivotY = originalPivotY;
                
                System.out.println("🎯 [ArcadeGameEngine] Applying item effect: " + originalItemType);
                System.out.println("   - Pivot position (original): (" + pivotY + ", " + pivotX + ")");
                System.out.println("   - GameState modification: DIRECT (grid modified in-place)");
                
                seoultech.se.core.engine.item.Item item = itemManager.getItem(originalItemType);
                if (item != null) {
                    seoultech.se.core.engine.item.ItemEffect effect = item.apply(newState, pivotY, pivotX);
                    
                    if (effect.isSuccess()) {
                        // 아이템 효과로 인한 점수 추가
                        newState.addScore(effect.getBonusScore());
                        
                        // 🔥 FIX: 아이템 효과로 클리어된 라인 수 저장
                        itemEffectLinesCleared = effect.getLinesCleared();
                        
                        // 🔥 FIX: 라인 클리어를 GameState에도 반영 (레벨업 진행)
                        if (itemEffectLinesCleared > 0) {
                            newState.addLinesCleared(itemEffectLinesCleared);
                        }
                        
                        System.out.println("✅ [ArcadeGameEngine] Item effect applied successfully");
                        System.out.println("   - Blocks cleared: " + effect.getBlocksCleared());
                        System.out.println("   - Lines cleared: " + effect.getLinesCleared());
                        System.out.println("   - Bonus score: " + effect.getBonusScore());
                        System.out.println("   - Grid synchronized: YES (modified in-place)");
                    } else {
                        System.err.println("⚠️ [ArcadeGameEngine] Item effect failed: " + originalItemType);
                    }
                } else {
                    System.err.println("⚠️ [ArcadeGameEngine] Item not found: " + originalItemType);
                }
            }
        }
        
        // Phase 4: 무게추 점수 추가
        if (weightBombScore > 0) {
            newState.addScore(weightBombScore);
        }
        
        // 4. 아이템 드롭 체크 (모든 라인 클리어 포함)
        // 🔥 FIX: 기본 라인 클리어 + LINE_CLEAR 마커 + 아이템 효과 라인 클리어
        int totalLinesCleared = newState.getLastLinesCleared() + lineClearMarkerLines + itemEffectLinesCleared;
        
        System.out.println("🔍 [ArcadeGameEngine] lockTetromino - itemManager: " + 
            (itemManager != null ? "initialized" : "NULL") + 
            ", lastLinesCleared: " + newState.getLastLinesCleared() +
            ", lineClearMarkerLines: " + lineClearMarkerLines +
            ", totalLinesCleared: " + totalLinesCleared);
        
        if (itemManager != null && totalLinesCleared > 0) {
            // Stateless API: GameState를 받아 업데이트된 GameState 반환
            newState = itemManager.checkAndGenerateItem(newState, totalLinesCleared);
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

        // 2. 📝 Note: 이 deepCopy는 GameEngine의 불변성 패턴 유지 (아이템 효과와 무관)
        GameState droppedState = state.deepCopy();
        droppedState.setCurrentY(finalY);
        droppedState.addScore(dropDistance * 2);

        // 3. lockTetromino() 호출 (오버라이드된 메서드 사용)
        return lockTetromino(droppedState);
    }
}
