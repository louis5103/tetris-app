package seoultech.se.core.engine;

import java.util.ArrayList;
import java.util.List;

import seoultech.se.core.GameConstants;
import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.model.Cell;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.RotationDirection;
import seoultech.se.core.model.enumType.RotationState;
import seoultech.se.core.model.enumType.TetrominoType;
import seoultech.se.core.model.enumType.WallKickEventData;

/**
 * 클래식 게임 엔진
 * 
 * 기본 테트리스 로직만 포함 (아이템 없음)
 * 
 * Input, Output: GameState
 * 기능: 블록 이동, 회전, 고정 등 게임 내 주요 로직 처리
 * 각 메서드는 새로운 GameState 객체를 반환하여 불변성을 유지
 * Phase 2: Result 객체 제거 - GameState만으로 모든 정보 전달
 */
public class ClassicGameEngine implements GameEngine {
    
    private static final int[][] T_SPIN_CORNERS = {
        {-1, -1},  // 좌상
        {1, -1},   // 우상
        {-1, 1},   // 좌하
        {1, 1}     // 우하
    };
    
    /**
     * 게임 모드 설정
     */
    private GameModeConfig config;
    
    // ========== 생성자 및 초기화 ==========
    
    /**
     * 기본 생성자
     */
    public ClassicGameEngine() {
        this.config = null;
    }
    
    /**
     * 게임 엔진 초기화
     * 
     * @param config 게임 모드 설정
     */
    @Override
    public void initialize(GameModeConfig config) {
        this.config = config;
        System.out.println("✅ [ClassicGameEngine] Initialized (Classic Mode - No Items)");
    }
    
    /**
     * 아이템 시스템 활성화 여부
     * 
     * @return Classic 모드는 항상 false
     */
    @Override
    public boolean isItemSystemEnabled() {
        return false;
    }
    
    // ========== 이동 관련 메서드 ==========
    
    @Override
    public GameState tryMoveLeft(GameState state) {
        // Phase 4: 무게추가 잠긴 상태면 좌우 이동 불가
        if (state.isWeightBombLocked() && 
            state.getCurrentTetromino().getType() == TetrominoType.WEIGHT_BOMB) {
            return state;  // 이동 불가
        }
        
        int newX = state.getCurrentX() - 1;

        if(isValidPosition(state, state.getCurrentTetromino(), newX, state.getCurrentY())) {
            GameState newState = state.deepCopy();
            newState.setCurrentX(newX);
            newState.setLastActionWasRotation(false);  // 이동 시 회전 플래그 리셋
            return newState;
        }
        return state;  // 실패 시 원본 상태 반환
    }
    
    @Override
    public GameState tryMoveRight(GameState state) {
        // Phase 4: 무게추가 잠긴 상태면 좌우 이동 불가
        if (state.isWeightBombLocked() && 
            state.getCurrentTetromino().getType() == TetrominoType.WEIGHT_BOMB) {
            return state;  // 이동 불가
        }
        
        int newX = state.getCurrentX() + 1;

        if(isValidPosition(state, state.getCurrentTetromino(), newX, state.getCurrentY())) {
            GameState newState = state.deepCopy();
            newState.setCurrentX(newX);
            newState.setLastActionWasRotation(false);  // 이동 시 회전 플래그 리셋
            return newState;
        }
        return state;  // 실패 시 원본 상태 반환
    }
    
    /**
     * 아래로 이동을 시도합니다
     * 
     * 이동할 수 없으면 고정(lock)이 필요하다는 신호입니다.
     * 하지만 이 메서드는 고정을 수행하지 않습니다.
     * 호출자가 GameState 비교를 통해 lockTetromino()를 호출해야 합니다.
     * 
     * Soft Drop:
     * isSoftDrop이 true이면 수동 DOWN 입력으로 간주하여 1점을 부여합니다.
     * isSoftDrop이 false이면 자동 낙하로 간주하여 점수를 주지 않습니다.
     * 
     * Phase 4: 무게추 로직
     * - 아래로 이동 시도 시 바닥/블록에 닿으면 isWeightBombLocked = true
     * - 잠긴 후에는 좌우 이동 불가
     * 
     * @param state 현재 게임 상태
     * @param isSoftDrop 수동 DOWN 입력 여부
     * @return 새로운 게임 상태 (이동 실패 시 원본 상태 반환)
     */
    @Override
    public GameState tryMoveDown(GameState state, boolean isSoftDrop) {
        int newY = state.getCurrentY() + 1;

        if(isValidPosition(state, state.getCurrentTetromino(), state.getCurrentX(), newY)) {
            GameState newState = state.deepCopy();
            newState.setCurrentY(newY);
            newState.setLastActionWasRotation(false);  // 이동 시 회전 플래그 리셋
            
            // Soft Drop 점수 추가 (1칸당 1점)
            if (isSoftDrop) {
                newState.addScore(1);
            }
            
            return newState;
        } else {
            // Phase 4: 무게추가 바닥/블록에 닿으면 잠김
            if (state.getCurrentTetromino().getType() == TetrominoType.WEIGHT_BOMB && 
                !state.isWeightBombLocked()) {
                GameState newState = state.deepCopy();
                newState.setWeightBombLocked(true);
                System.out.println("⚓ [ClassicGameEngine] WEIGHT_BOMB locked - horizontal movement disabled");
                return newState;  // 상태만 변경, 위치는 그대로
            }
            
            return state;  // 실패 시 원본 상태 반환 (고정 필요 신호)
        }
    }
    
    // ========== 회전 관련 메서드 ==========
    
    /**
     * 회전을 시도합니다 (SRS Wall Kick 포함)
     * 
     * SRS(Super Rotation System)는 현대 테트리스의 표준 회전 시스템입니다.
     * 단순히 회전만 하는 것이 아니라, 벽이나 다른 블록에 막혔을 때
     * 자동으로 위치를 조정하여 회전을 성공시키려고 시도합니다.
     * 
     * 5가지 위치를 순서대로 시도하며, 하나라도 성공하면 회전이 완료됩니다.
     * 
     * Phase 4: 무게추는 회전 불가 (O 블록처럼)
     * 
     * @param state 현재 게임 상태
     * @param direction 회전 방향 (시계/반시계)
     * @param srsEnabled SRS 활성화 여부 (true: Wall Kick 사용, false: 기본 회전만)
     * @return 새로운 게임 상태 (회전 실패 시 원본 상태 반환)
     */
    @Override
    public GameState tryRotate(GameState state, RotationDirection direction, boolean srsEnabled) {
        // O 블록: 회전해도 모양이 같음 - 원본 상태 반환
        if(state.getCurrentTetromino().getType() == TetrominoType.O) {
            return state;
        }
        
        // Phase 4: 무게추는 회전 불가
        if(state.getCurrentTetromino().getType() == TetrominoType.WEIGHT_BOMB) {
            return state;
        }

        Tetromino rotated = state.getCurrentTetromino().getRotatedInstance(direction);

        if (!srsEnabled) {
            // SRS 비활성화: 기본 회전만 (Wall Kick 없이)
            if(isValidPosition(state, rotated, state.getCurrentX(), state.getCurrentY())) {
                GameState newState = state.deepCopy();
                newState.setCurrentTetromino(rotated);
                newState.setLastActionWasRotation(true);
                newState.setLastRotationKickIndex(0);  // 기본 위치 사용
                return newState;
            }
            return state;  // 실패 시 원본 상태 반환
        }

        // SRS 활성화: Wall Kick 사용
        // 월킥 데이터 가져오기
        int[][] kickData = WallKickEventData.getKickData(
                state.getCurrentTetromino().getType(),
                state.getCurrentTetromino().getRotationState(),
                rotated.getRotationState()
        );

        // 월킥 시도
        for(int kickIndex = 0; kickIndex < kickData.length; kickIndex++) {
            int[] offset = kickData[kickIndex];
            int newX = state.getCurrentX() + offset[0];
            int newY = state.getCurrentY() + offset[1];

            if(isValidPosition(state, rotated, newX, newY)) {
                GameState newState = state.deepCopy();
                newState.setCurrentTetromino(rotated);
                newState.setCurrentX(newX);
                newState.setCurrentY(newY);
                newState.setLastActionWasRotation(true);  // 회전 성공 시 플래그 설정
                newState.setLastRotationKickIndex(kickIndex);  // kickIndex 저장
                return newState;
            }
        }
        return state;  // 실패 시 원본 상태 반환
    }
    
    // ========== Hard Drop ==========
    
    /**
     * Hard Drop을 실행합니다
     * 
     * 블록을 즉시 바닥까지 떨어뜨리고 고정합니다.
     * 이 메서드는 두 단계를 합친 것입니다:
     * 1. 바닥까지 이동
     * 2. 즉시 고정 (lockTetromino 호출)
     * 
     * 성능 최적화: deepCopy를 한 번만 수행
     * 
     * @return 새로운 게임 상태 (고정 완료, 라인 클리어 처리 완료)
     */
    @Override
    public GameState hardDrop(GameState state){
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

        // 3. 즉시 고정 (이미 deepCopy되었으므로 내부에서 다시 복사하지 않음)
        return lockTetrominoInternal(droppedState, false);
    }
    
    // ========== Hold 기능 ==========
    
    /**
     * Hold 기능을 실행합니다
     * 
     * Hold는 현재 테트로미노를 보관하고, 보관된 블록이 있으면 그것을 꺼내오는 기능입니다.
     * 
     * 규칙:
     * 1. 한 턴에 한 번만 사용 가능 (holdUsedThisTurn 플래그로 체크)
     * 2. Hold가 비어있으면: 현재 블록 보관 + Next에서 새 블록 가져오기
     * 3. Hold에 블록이 있으면: 현재 블록과 Hold 블록 교체
     * 
     * Phase 5: 아이템 타입 및 무게추 상태 보존
     * 
     * @param state 현재 게임 상태
     * @return 새로운 게임 상태 (Hold 실패 시 원본 상태 반환)
     */
    @Override
    public GameState tryHold(GameState state) {
        // 이미 이번 턴에 Hold를 사용했는지 확인
        if (state.isHoldUsedThisTurn()) {
            return state;  // 실패 시 원본 상태 반환
        }
        
        // Next Queue 검증
        if (state.getNextQueue() == null || state.getNextQueue().length == 0) {
            System.err.println("⚠️ [ClassicGameEngine] tryHold() failed: Next Queue is not initialized!");
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
            newState.setHeldItemType(currentItemType);  // Phase 5
            newState.setHeldWeightBombLocked(currentWeightBombLocked);  // Phase 5
            
            // Next Queue 첫 번째 요소 검증
            if (newState.getNextQueue()[0] == null) {
                System.err.println("⚠️ [ClassicGameEngine] tryHold() failed: Next Queue[0] is null!");
                return state;
            }
            
            // Phase 5: 무게추는 Next Queue에서 가져오지 않음
            if (currentType == TetrominoType.WEIGHT_BOMB) {
                System.out.println("⚓ [ClassicGameEngine] WEIGHT_BOMB held - will spawn from Next Queue");
            }
            
            // Next Queue에서 새 블록 가져오기
            TetrominoType nextType = newState.getNextQueue()[0];
            Tetromino newTetromino = new Tetromino(nextType);
            
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
            
            // Phase 5: 새 블록은 일반 블록 (아이템 없음)
            newState.setCurrentItemType(null);
            newState.setWeightBombLocked(false);
            
        } else {
            // Hold에 블록이 있음: 현재 블록과 교체
            newState.setHeldPiece(currentType);
            newState.setHeldItemType(currentItemType);  // Phase 5
            newState.setHeldWeightBombLocked(currentWeightBombLocked);  // Phase 5
            
            // Hold된 블록을 꺼내서 현재 블록으로 설정
            Tetromino heldTetromino;
            
            // Phase 5: 무게추인 경우 특수 처리
            if (previousHeld == TetrominoType.WEIGHT_BOMB) {
                heldTetromino = new Tetromino(TetrominoType.WEIGHT_BOMB);
                System.out.println("⚓ [ClassicGameEngine] Swapping WEIGHT_BOMB from Hold");
            } else {
                heldTetromino = new Tetromino(previousHeld);
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
            
            // Phase 5: Hold된 블록의 아이템 정보 복원
            newState.setCurrentItemType(previousItemType);
            newState.setWeightBombLocked(previousWeightBombLocked);
            
            if (previousItemType != null) {
                System.out.println("📦 [ClassicGameEngine] Restored item type from Hold: " + previousItemType);
            }
            if (previousWeightBombLocked) {
                System.out.println("⚓ [ClassicGameEngine] Restored WEIGHT_BOMB locked state from Hold");
            }
        }
        
        // Hold 사용 플래그 설정
        newState.setHoldUsedThisTurn(true);
        
        // 회전 플래그 리셋 (새로운 블록이라 이전 회전 정보 무효화)
        newState.setLastActionWasRotation(false);
        
        return newState;
    }
    
    // ========== 테트로미노 고정 ==========
    
    /**
     * 테트로미노를 보드에 고정하고 라인 클리어를 처리합니다
     * 
     * @param state 현재 게임 상태
     * @return 고정 결과
     */
    @Override
    public GameState lockTetromino(GameState state) {
        return lockTetrominoInternal(state, true);
    }
    
    /**
     * 테트로미노를 보드에 고정하는 내부 메서드
     * 
     * Phase 2: Result 객체 제거 - GameState만으로 모든 정보 전달
     * 
     * 실행 순서:
     * 1. 게임 오버 체크 (먼저!)
     * 2. 블록 고정
     * 3. 라인 클리어
     * 4. 점수 계산
     * 5. Lock 메타데이터를 GameState에 저장
     * 
     * @param state 현재 게임 상태
     * @param needsCopy deepCopy가 필요한지 여부
     * @return 고정이 완료된 새로운 게임 상태
     */
    private GameState lockTetrominoInternal(GameState state, boolean needsCopy) {
        GameState newState = needsCopy ? state.deepCopy() : state;
        
        // 고정하기 전에 블록 정보 저장 (EventMapper에서 사용)
        Tetromino lockedTetromino = state.getCurrentTetromino();
        int lockedX = state.getCurrentX();
        int lockedY = state.getCurrentY();

        // T-Spin 감지 (블록이 고정되기 전에 체크)
        boolean isTSpin = detectTSpin(state);
        boolean isTSpinMini = false;
        if (isTSpin) {
            isTSpinMini = detectTSpinMini(state);
        }
        
        // GameState에 T-Spin 정보 저장
        newState.setLastLockWasTSpin(isTSpin);
        newState.setLastLockWasTSpinMini(isTSpinMini);

        int[][] shape = state.getCurrentTetromino().getCurrentShape();

        // 1. 게임 오버 체크 (블록을 고정하기 전에 먼저 확인)
        for(int row = 0; row < shape.length; row++) {
            for(int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    int absY = state.getCurrentY() + (row - state.getCurrentTetromino().getPivotY());
                    
                    if(absY < 0) {
                        // 게임 오버
                        newState.setGameOver(true);
                        newState.setGameOverReason("[ClassicGameEngine] Game Over: Block locked above the board.");
                        
                        // 게임 오버 시에도 Lock 메타데이터 저장
                        newState.setLastLockedTetromino(lockedTetromino);
                        newState.setLastLockedX(lockedX);
                        newState.setLastLockedY(lockedY);
                        newState.setLastLinesCleared(0);
                        newState.setLastClearedRows(new int[0]);
                        newState.setLastScoreEarned(0);
                        newState.setLastIsPerfectClear(false);
                        newState.setLastLeveledUp(false);
                        
                        return newState;
                    }
                }
            }
        }

        // 2. Grid에 테트로미노 고정
        // Phase 3: 아이템 블록인 경우 'L' 마커 추가
        java.util.List<int[]> blockPositions = new java.util.ArrayList<>();
        
        for(int row = 0; row < shape.length; row++) {
            for(int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    int absX = state.getCurrentX() + (col - state.getCurrentTetromino().getPivotX());
                    int absY = state.getCurrentY() + (row - state.getCurrentTetromino().getPivotY());

                    if(absY >= 0 && absY < state.getBoardHeight() &&
                       absX >= 0 && absX < state.getBoardWidth()
                    ) {
                        newState.getGrid()[absY][absX].setColor(state.getCurrentTetromino().getColor());
                        newState.getGrid()[absY][absX].setOccupied(true);
                        
                        // 블록 위치 저장 (아이템 마커 추가용)
                        blockPositions.add(new int[]{absY, absX});
                    }
                }
            }
        }
        
        // Phase 3: 'L' 마커 추가 (아이템 블록인 경우)
        if (state.getCurrentItemType() != null && 
            state.getCurrentItemType() == seoultech.se.core.item.ItemType.LINE_CLEAR &&
            !blockPositions.isEmpty()) {
            // 무작위로 하나의 블록에 'L' 마커 추가
            java.util.Random random = new java.util.Random();
            int randomIndex = random.nextInt(blockPositions.size());
            int[] markerPos = blockPositions.get(randomIndex);
            
            newState.getGrid()[markerPos[0]][markerPos[1]].setItemMarker(
                seoultech.se.core.item.ItemType.LINE_CLEAR
            );
            
            System.out.println("Ⓛ [ClassicGameEngine] LINE_CLEAR marker added at (" + 
                markerPos[0] + ", " + markerPos[1] + ")");
        }

        // 3. 라인 클리어 체크 및 실행
        checkAndClearLines(newState, isTSpin, isTSpinMini);

        // 4. 점수 및 통계 업데이트
        boolean leveledUp = false;
        
        if(newState.getLastLinesCleared() > 0) {
            newState.addScore(newState.getLastScoreEarned());
            
            // 라인 클리어 추가 및 레벨업 체크
            leveledUp = newState.addLinesCleared(newState.getLastLinesCleared());

            // 콤보 업데이트
            newState.setComboCount(newState.getComboCount() + 1);
            newState.setLastActionClearedLines(true);

            // B2B 업데이트
            boolean isDifficult = newState.getLastLinesCleared() == GameConstants.TETRIS_LINE_COUNT 
                                || newState.isLastLockWasTSpin();
            if (isDifficult && newState.isLastClearWasDifficult()) {
                newState.setBackToBackCount(newState.getBackToBackCount() + 1);
            } else if (isDifficult) {
                newState.setBackToBackCount(1);
            } else {
                newState.setBackToBackCount(0);
            }
            newState.setLastClearWasDifficult(isDifficult);
        } else { 
            // 라인 클리어 실패 → 모든 연속 보너스 초기화
            newState.setComboCount(0);
            newState.setLastActionClearedLines(false);
            newState.setBackToBackCount(0);
            newState.setLastClearWasDifficult(false);
        }

        // 5. Hold 재사용 가능하게 설정
        newState.setHoldUsedThisTurn(false);
        
        // 6. 회전 플래그 리셋
        newState.setLastActionWasRotation(false);
        
        // Lock 메타데이터 저장
        newState.setLastLockedTetromino(lockedTetromino);
        newState.setLastLockedX(lockedX);
        newState.setLastLockedY(lockedY);
        newState.setLastLeveledUp(leveledUp);
        
        return newState;
    }

    // ========== T-Spin 감지 ==========
    
    /**
     * T-Spin 여부를 감지합니다
     * 
     * @param state 현재 게임 상태
     * @return T-Spin이면 true
     */
    private boolean detectTSpin(GameState state) {
        if (state.getCurrentTetromino().getType() != TetrominoType.T) {
            return false;
        }
        
        if (!state.isLastActionWasRotation()) {
            return false;
        }
        
        return check3CornerRule(state);
    }
    
    /**
     * 3-Corner Rule 체크
     * 
     * @param state 현재 게임 상태
     * @return 3개 이상의 코너가 채워져 있으면 true
     */
    private boolean check3CornerRule(GameState state) {
        int px = state.getCurrentX();
        int py = state.getCurrentY();
        
        int filledCorners = 0;

        for (int[] corner : T_SPIN_CORNERS) {
            int checkX = px + corner[0];
            int checkY = py + corner[1];
            
            if (isCornerFilled(state, checkX, checkY)) {
                filledCorners++;
            }
        }
        
        return filledCorners >= 3;
    }
    
    /**
     * 코너가 채워져 있는지 확인
     * 
     * @param state 현재 게임 상태
     * @param x X 좌표
     * @param y Y 좌표
     * @return 코너가 채워져 있으면 true
     */
    private boolean isCornerFilled(GameState state, int x, int y) {
        // 보드 밖 = 채워진 것으로 판정
        if (x < 0 || x >= state.getBoardWidth() || 
            y < 0 || y >= state.getBoardHeight()) {
            return true;
        }
        
        return state.getGrid()[y][x].isOccupied();
    }
    
    /**
     * T-Spin Mini 감지
     * 
     * @param state 현재 게임 상태
     * @return T-Spin Mini이면 true
     */
    private boolean detectTSpinMini(GameState state) {
        if (!detectTSpin(state)) {
            return false;
        }
        
        // Wall Kick 5번째 테스트 사용 시 T-Spin Mini 아님
        if (state.getLastRotationKickIndex() == 4) {
            return false;
        }
        
        return checkFrontCornersForMini(state);
    }
    
    /**
     * T 블록의 정면 2개 코너 체크
     * 
     * @param state 현재 게임 상태
     * @return 정면 2개 코너 중 1개 이상이 비어있으면 true
     */
    private boolean checkFrontCornersForMini(GameState state) {
        int px = state.getCurrentX();
        int py = state.getCurrentY();
        RotationState rotation = state.getCurrentTetromino().getRotationState();
        
        int[][] frontCorners;
        switch (rotation) {
            case SPAWN:
                frontCorners = new int[][]{{-1, -1}, {1, -1}};
                break;
            case RIGHT:
                frontCorners = new int[][]{{1, -1}, {1, 1}};
                break;
            case REVERSE:
                frontCorners = new int[][]{{-1, 1}, {1, 1}};
                break;
            case LEFT:
                frontCorners = new int[][]{{-1, -1}, {-1, 1}};
                break;
            default:
                return false;
        }
        
        int filledCount = 0;
        for (int[] corner : frontCorners) {
            int checkX = px + corner[0];
            int checkY = py + corner[1];
            if (isCornerFilled(state, checkX, checkY)) {
                filledCount++;
            }
        }
        
        return filledCount < 2;
    }
    
    // ========== 라인 클리어 ==========
    
    /**
     * 라인 클리어 체크 및 실행
     * 
     * @param state 현재 게임 상태
     * @param isTSpin T-Spin 여부
     * @param isTSpinMini T-Spin Mini 여부
     */
    private void checkAndClearLines(GameState state, boolean isTSpin, boolean isTSpinMini) {
        List<Integer> clearedRowsList = new ArrayList<>();

        // 라인 체크
        for (int row = state.getBoardHeight() - 1; row >= 0; row--) {
            boolean isFullLine = true;

            for(int col = 0; col < state.getBoardWidth(); col++) {
                if(!state.getGrid()[row][col].isOccupied()) {
                    isFullLine = false;
                    break;
                }
            }

            if (isFullLine) {
                clearedRowsList.add(row);
            }
        }

        if (clearedRowsList.isEmpty()){
            // 라인 클리어 없음
            state.setLastLinesCleared(0);
            state.setLastClearedRows(new int[0]);
            state.setLastIsPerfectClear(false);
            
            // T-Spin Mini (라인 없음)는 점수를 받음
            if (isTSpin && isTSpinMini) {
                long score = GameConstants.TSPIN_MINI_NO_LINE * state.getLevel();
                state.setLastScoreEarned(score);
            } else if (isTSpin && !isTSpinMini) {
                long score = GameConstants.TSPIN_NO_LINE * state.getLevel();
                state.setLastScoreEarned(score);
            } else {
                state.setLastScoreEarned(0);
            }
            return;
        }

        // 라인 클리어 실행
        java.util.Set<Integer> clearedRowsSet = new java.util.HashSet<>(clearedRowsList);
        
        List<Cell[]> remainingRows = new ArrayList<>();
        for (int row = state.getBoardHeight() - 1; row >= 0; row--) {
            if (!clearedRowsSet.contains(row)) {
                Cell[] rowCopy = new Cell[state.getBoardWidth()];
                for (int col = 0; col < state.getBoardWidth(); col++) {
                    rowCopy[col] = state.getGrid()[row][col].copy();
                }
                remainingRows.add(rowCopy);
            }
        }
        
        // 보드를 아래에서부터 다시 채우기
        int targetRow = state.getBoardHeight() - 1;
        for (Cell[] rowData : remainingRows) {
            for (int col = 0; col < state.getBoardWidth(); col++) {
                state.getGrid()[targetRow][col] = rowData[col];
            }
            targetRow--;
        }
        
        // 남은 위쪽 줄들을 빈 칸으로 초기화
        while (targetRow >= 0) {
            for (int col = 0; col < state.getBoardWidth(); col++) {
                state.getGrid()[targetRow][col] = Cell.empty();
            }
            targetRow--;
        }

        int linesCleared = clearedRowsList.size();
        boolean isPerfectClear = checkPerfectClear(state);
        long score = calculateScore(linesCleared, isTSpin, isTSpinMini, isPerfectClear,
                state.getLevel(), state.getComboCount(), state.getBackToBackCount()
        );

        // GameState에 라인 클리어 정보 저장
        state.setLastLinesCleared(linesCleared);
        
        int[] clearedRowsArray = new int[clearedRowsList.size()];
        for (int i = 0; i < clearedRowsList.size(); i++) {
            clearedRowsArray[i] = clearedRowsList.get(i);
        }
        state.setLastClearedRows(clearedRowsArray);
        
        state.setLastScoreEarned(score);
        state.setLastIsPerfectClear(isPerfectClear);
    }

    /**
     * Perfect Clear 체크
     * 
     * @param state 현재 게임 상태
     * @return Perfect Clear이면 true
     */
    private boolean checkPerfectClear(GameState state) {
        for (int row = 0; row < state.getBoardHeight(); row++) {
            for (int col = 0; col < state.getBoardWidth(); col++) {
                if (state.getGrid()[row][col].isOccupied()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 점수 계산
     * 
     * @param lines 클리어된 줄 수
     * @param tSpin T-Spin 여부
     * @param tSpinMini T-Spin Mini 여부
     * @param perfectClear Perfect Clear 여부
     * @param level 현재 레벨
     * @param combo 콤보 카운트
     * @param b2b Back-to-Back 카운트
     * @return 점수
     */
    private long calculateScore(int lines, boolean tSpin, boolean tSpinMini,
                                 boolean perfectClear, int level, int combo, int b2b
    ) {
        long baseScore = 0;

        // 기본 점수 계산
        if (tSpin) {
            if (tSpinMini) {
                baseScore = lines == 0 ? GameConstants.TSPIN_MINI_NO_LINE 
                          : lines == 1 ? GameConstants.TSPIN_MINI_SINGLE 
                          : GameConstants.TSPIN_MINI_DOUBLE;
            } else {
                baseScore = lines == 0 ? GameConstants.TSPIN_NO_LINE 
                          : lines == 1 ? GameConstants.TSPIN_SINGLE 
                          : lines == 2 ? GameConstants.TSPIN_DOUBLE 
                          : GameConstants.TSPIN_TRIPLE;
            }
        } else {
            switch (lines) {
                case 1: baseScore = GameConstants.SCORE_SINGLE; break;
                case 2: baseScore = GameConstants.SCORE_DOUBLE; break;
                case 3: baseScore = GameConstants.SCORE_TRIPLE; break;
                case 4: baseScore = GameConstants.SCORE_TETRIS; break;
            }
        }

        // B2B 보너스
        if (b2b > 0 && (lines == GameConstants.TETRIS_LINE_COUNT || tSpin)) {
            baseScore = (long)(baseScore * GameConstants.BACK_TO_BACK_MULTIPLIER);
        }

        // 콤보 보너스
        if (combo > 0) {
            baseScore += combo * GameConstants.COMBO_BONUS_PER_LEVEL * level;
        }

        // 퍼펙트 클리어 보너스
        if (perfectClear) {
            baseScore += lines == 1 ? GameConstants.PERFECT_CLEAR_SINGLE 
                       : lines == 2 ? GameConstants.PERFECT_CLEAR_DOUBLE 
                       : lines == 3 ? GameConstants.PERFECT_CLEAR_TRIPLE 
                       : GameConstants.PERFECT_CLEAR_TETRIS;
        }

        return baseScore * level;
    }

    // ========== 위치 검증 헬퍼 메서드 ==========
    
    /**
     * 주어진 위치에 테트로미노를 놓을 수 있는지 검증
     * 
     * @param state 현재 게임 상태
     * @param tetromino 검증할 테트로미노
     * @param x X 위치
     * @param y Y 위치
     * @return true면 놓을 수 있음
     */
    private boolean isValidPosition(GameState state, Tetromino tetromino, int x, int y){
        int[][] shape = tetromino.getCurrentShape();
        
        if (shape == null || shape.length == 0) {
            System.err.println("⚠️ [ClassicGameEngine] isValidPosition(): shape is null or empty!");
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

                    // 보드 경계 체크
                    if(absX < 0 || absX >= state.getBoardWidth() || absY >= state.getBoardHeight()) {
                        return false;
                    }
                    // 다른 블록과 충돌 체크
                    if(absY >= 0 && state.getGrid()[absY][absX].isOccupied()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
