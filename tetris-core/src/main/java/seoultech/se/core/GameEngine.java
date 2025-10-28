package seoultech.se.core;

import java.util.ArrayList;
import java.util.List;

import seoultech.se.core.model.Cell;
import seoultech.se.core.model.Tetromino;
import seoultech.se.core.model.enumType.RotationDirection;
import seoultech.se.core.model.enumType.RotationState;
import seoultech.se.core.model.enumType.TetrominoType;
import seoultech.se.core.model.enumType.WallKickEventData;

/**
 * 게임 엔진 클래스
 * Input, Output: GameState
 * 기능: 블록 이동, 회전, 고정 등 게임 내 주요 로직 처리
 * 각 메서드는 새로운 GameState 객체를 반환하여 불변성을 유지
 * Phase 2: Result 객체 제거 - GameState만으로 모든 정보 전달
 */
public class GameEngine {
    private static final int[][] T_SPIN_CORNERS = {
        {-1, -1},  // 좌상
        {1, -1},   // 우상
        {-1, 1},   // 좌하
        {1, 1}     // 우하
    };

    public static GameState tryMoveLeft(GameState state) {
        int newX = state.getCurrentX() - 1;

        if(isValidPosition(state, state.getCurrentTetromino(), newX, state.getCurrentY())) {
            GameState newState = state.deepCopy();
            newState.setCurrentX(newX);
            newState.setLastActionWasRotation(false);  // 이동 시 회전 플래그 리셋
            return newState;
        }
        return state;  // 실패 시 원본 상태 반환
    }

    // ========== 이동 관련 메서드 ==========

    public static GameState tryMoveRight(GameState state) {
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
     * @param state 현재 게임 상태
     * @param isSoftDrop 수동 DOWN 입력 여부
     * @return 새로운 게임 상태 (이동 실패 시 원본 상태 반환)
     */
    public static GameState tryMoveDown(GameState state, boolean isSoftDrop) {
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
        }
        return state;  // 실패 시 원본 상태 반환 (고정 필요 신호)
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
     * @param state 현재 게임 상태
     * @param direction 회전 방향 (시계/반시계)
     * @param srsEnabled SRS 활성화 여부 (true: Wall Kick 사용, false: 기본 회전만)
     * @return 새로운 게임 상태 (회전 실패 시 원본 상태 반환)
     */
    public static GameState tryRotate(GameState state, RotationDirection direction, boolean srsEnabled) {
        // O 블록 : 회전해도 모양이 같음 - 원본 상태 반환
        if(state.getCurrentTetromino().getType() == TetrominoType.O) {
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
    
    /**
     * 회전을 시도합니다 (기본값: SRS 활성화)
     * 
     * 하위 호환성을 위한 오버로드 메서드입니다.
     * 
     * @param state 현재 게임 상태
     * @param direction 회전 방향 (시계/반시계)
     * @return 새로운 게임 상태 (회전 실패 시 원본 상태 반환)
     */
    public static GameState tryRotate(GameState state, RotationDirection direction) {
        return tryRotate(state, direction, true);  // 기본값: SRS 활성화
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
    public static GameState hardDrop(GameState state){
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
     * 중요: Next Queue 동기화
     * - 이 메서드는 nextQueue[0]을 읽기만 하고 제거하지 않습니다
     * - 실제 큐 업데이트는 BoardController에서 spawnNextTetromino() 호출 시 처리됩니다
     * - Hold 후 lockTetromino() → BoardController가 새 블록 스폰 → 큐 업데이트
     * 
     * @param state 현재 게임 상태
     * @return 새로운 게임 상태 (Hold 실패 시 원본 상태 반환)
     */
    public static GameState tryHold(GameState state) {
        // 이미 이번 턴에 Hold를 사용했는지 확인
        if (state.isHoldUsedThisTurn()) {
            return state;  // 실패 시 원본 상태 반환
        }
        
        // ✅ Next Queue 검증 추가
        if (state.getNextQueue() == null || state.getNextQueue().length == 0) {
            System.err.println("⚠️ [GameEngine] tryHold() failed: Next Queue is not initialized!");
            return state;  // Hold 실패 - 원본 상태 반환
        }
        
        GameState newState = state.deepCopy();
        TetrominoType currentType = newState.getCurrentTetromino().getType();
        TetrominoType previousHeld = newState.getHeldPiece();
        
        if (previousHeld == null) {
            // Hold가 비어있음: 현재 블록을 보관하고 Next에서 새 블록 가져오기
            newState.setHeldPiece(currentType);
            
            // ✅ Next Queue 첫 번째 요소 검증
            if (newState.getNextQueue()[0] == null) {
                System.err.println("⚠️ [GameEngine] tryHold() failed: Next Queue[0] is null!");
                return state;  // Hold 실패 - 원본 상태 반환
            }
            
            // Next Queue에서 새 블록 가져오기 (읽기만 함, 제거는 BoardController에서)
            // 주의: nextQueue[0]은 BoardController의 spawnNextTetromino()에서 제거됩니다
            TetrominoType nextType = newState.getNextQueue()[0];
            Tetromino newTetromino = new Tetromino(nextType);
            
            // 새 블록 스폰 위치 설정
            int spawnX = newState.getBoardWidth() / 2 - 1;
            int spawnY = 0;
            
            // 스폰 위치 충돌 검사
            if (!isValidPosition(newState, newTetromino, spawnX, spawnY)) {
                // 스폰 위치에 블록이 있으면 게임 오버
                newState.setGameOver(true);
                newState.setGameOverReason("Cannot spawn new tetromino after hold: spawn position blocked");
                return newState;
            }
            
            // 스폰 성공
            newState.setCurrentTetromino(newTetromino);
            newState.setCurrentX(spawnX);
            newState.setCurrentY(spawnY);
            
            // 주의: Next Queue 업데이트는 BoardController에서 처리됩니다
            // Hold 사용 후 lockTetromino() 호출 시 BoardController가 감지하고
            // spawnNextTetromino()를 통해 큐를 업데이트합니다 (7-bag 시스템 동기화)
            
        } else {
            // Hold에 블록이 있음: 현재 블록과 교체
            newState.setHeldPiece(currentType);
            
            // Hold된 블록을 꺼내서 현재 블록으로 설정
            Tetromino heldTetromino = new Tetromino(previousHeld);
            
            // 스폰 위치 설정
            int spawnX = newState.getBoardWidth() / 2 - 1;
            int spawnY = 0;
            
            // 스폰 위치 충돌 검사
            if (!isValidPosition(newState, heldTetromino, spawnX, spawnY)) {
                // 스폰 위치에 블록이 있으면 게임 오버
                newState.setGameOver(true);
                newState.setGameOverReason("Cannot swap held tetromino: spawn position blocked");
                return newState;
            }
            
            // 스폰 성공
            newState.setCurrentTetromino(heldTetromino);
            newState.setCurrentX(spawnX);
            newState.setCurrentY(spawnY);
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
     * 이 메서드는 여러 단계를 거칩니다:
     * 1. 게임 오버 체크 (블록이 보드 위쪽에 고정되는지 먼저 확인)
     * 2. 테트로미노의 각 블록을 grid에 추가
     * 3. 라인 클리어 체크 및 실행
     * 4. 점수 계산
     * 5. Hold 재사용 가능하게 설정
     * 
     * @param state 현재 게임 상태
     * @return 고정 결과 (게임 오버 여부, 라인 클리어 정보 포함)
     */
    public static GameState lockTetromino(GameState state) {
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
     * 5. Lock 메타데이터를 GameState에 저장 (EventMapper가 사용)
     * 
     * @param state 현재 게임 상태
     * @param needsCopy deepCopy가 필요한지 여부 (false면 이미 복사된 상태로 간주)
     * @return 고정이 완료된 새로운 게임 상태 (메타데이터 포함)
     */
    private static GameState lockTetrominoInternal(GameState state, boolean needsCopy) {
        GameState newState = needsCopy ? state.deepCopy() : state;
        
        // 고정하기 전에 블록 정보 저장! (EventMapper에서 사용)
        Tetromino lockedTetromino = state.getCurrentTetromino();
        int lockedX = state.getCurrentX();
        int lockedY = state.getCurrentY();

        // T-Spin 감지 (블록이 고정되기 전에 체크해야 정확함!)
        // 고정 후에는 T 블록 자신도 "채워진 것"으로 판정되어 오류 발생
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
        // 블록의 어느 부분이라도 보드 위쪽(y < 0)에 있으면 게임 오버
        for(int row = 0; row < shape.length; row++) {
            for(int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    int absY = state.getCurrentY() + (row - state.getCurrentTetromino().getPivotY());
                    
                    if(absY < 0) {
                        // 게임 오버 - 블록이 보드 위쪽에 고정됨
                        newState.setGameOver(true);
                        newState.setGameOverReason("[GameEngine] (Method: lockTetromino) Game Over: Block locked above the board.");
                        
                        // Phase 2: 게임 오버 시에도 Lock 메타데이터 저장
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

        // 2. Grid에 테트로미노 고정 (게임 오버가 아닌 경우에만 실행됨)
        for(int row = 0; row < shape.length; row++) {
            for(int col = 0; col < shape[row].length; col++) {
                if (shape[row][col] == 1) {
                    int absX = state.getCurrentX() + (col - state.getCurrentTetromino().getPivotX());
                    int absY = state.getCurrentY() + (row - state.getCurrentTetromino().getPivotY());

                    // 셀에 색상 채우기
                    // (이미 게임 오버 체크를 통과했으므로 absY >= 0 보장됨)
                    if(absY >= 0 && absY < state.getBoardHeight() &&
                       absX >= 0 && absX < state.getBoardWidth()
                    ) {
                        newState.getGrid()[absY][absX].setColor(state.getCurrentTetromino().getColor());
                        newState.getGrid()[absY][absX].setOccupied(true);
                    }
                }
            }
        }

        // 3. 라인 클리어 체크 및 실행 (T-Spin 정보 전달)
        // Phase 2: GameState에 직접 라인 클리어 정보를 저장
        checkAndClearLines(newState, isTSpin, isTSpinMini);

        // 4. 점수 및 통계 업데이트
        boolean leveledUp = false;
        
        if(newState.getLastLinesCleared() > 0) {
            newState.addScore(newState.getLastScoreEarned());
            
            // 라인 클리어 추가 및 레벨업 체크
            leveledUp = newState.addLinesCleared(newState.getLastLinesCleared());

            // 콤보 업데이트 (연속 라인 클리어 횟수)
            // 0 → 1 (첫 콤보), 1 → 2 (콤보 계속), 2 → 3, ...
            newState.setComboCount(newState.getComboCount() + 1);
            newState.setLastActionClearedLines(true);

            // B2B (Back-to-Back) 업데이트
            // Tetris(4줄) 또는 T-Spin을 연속으로 성공하면 B2B 카운트 증가
            boolean isDifficult = newState.getLastLinesCleared() == GameConstants.TETRIS_LINE_COUNT 
                                || newState.isLastLockWasTSpin();
            if (isDifficult && newState.isLastClearWasDifficult()) {
                // 이전에도 difficult였고 지금도 difficult → B2B 계속
                newState.setBackToBackCount(newState.getBackToBackCount() + 1);
            } else if (isDifficult) {
                // 처음으로 difficult 클리어 → B2B 시작
                newState.setBackToBackCount(1);
            } else {
                // 일반 클리어 (1~3줄) → B2B 종료
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

        // 5. Hold 재사용 가능하게 설정.
        newState.setHoldUsedThisTurn(false);
        
        // 6. 회전 플래그 리셋 (다음 블록을 위해)
        newState.setLastActionWasRotation(false);
        
        // Phase 2: Lock 메타데이터를 GameState에 저장
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
     * T-Spin 판별 조건:
     * 1. T 블록이어야 함
     * 2. 마지막 액션이 회전이어야 함 (lastActionWasRotation = true)
     * 3. 3-Corner Rule: T 블록의 4개 코너 중 3개 이상이 채워져 있어야 함
     * 
     * 3-Corner Rule:
     * T 블록의 pivot(중심)을 기준으로 4개의 코너 위치를 확인합니다.
     * 코너가 보드 밖이거나 블록으로 채워져 있으면 "차있음"으로 판정합니다.
     * 
     * @param state 현재 게임 상태
     * @return T-Spin이면 true, 아니면 false
     */
    private static boolean detectTSpin(GameState state) {
        // 1. T 블록이 아니면 T-Spin이 아님
        if (state.getCurrentTetromino().getType() != TetrominoType.T) {
            return false;
        }
        
        // 2. 마지막 액션이 회전이 아니면 T-Spin이 아님
        if (!state.isLastActionWasRotation()) {
            return false;
        }
        
        // 3. 3-Corner Rule 체크
        return check3CornerRule(state);
    }
    
    /**
     * 3-Corner Rule을 체크합니다
     * 
     * T 블록의 pivot을 중심으로 4개의 코너 위치를 확인합니다.
     * - 좌상 (px-1, py-1)
     * - 우상 (px+1, py-1)
     * - 좌하 (px-1, py+1)
     * - 우하 (px+1, py+1)
     * 
     * 코너가 보드 밖이거나 블록으로 채워져 있으면 "채워짐"으로 판정합니다.
     * 4개 중 3개 이상이 채워져 있으면 true를 반환합니다.
     * 
     * @param state 현재 게임 상태
     * @return 3개 이상의 코너가 채워져 있으면 true
     */
    private static boolean check3CornerRule(GameState state) {
        int px = state.getCurrentX();
        int py = state.getCurrentY();
        
        
        int filledCorners = 0;

        for (int[] corner : T_SPIN_CORNERS) {
            int checkX = px + corner[0];
            int checkY = py + corner[1];
            
            // 코너가 보드 밖이거나 블록으로 채워져 있으면 "채워짐"
            if (isCornerFilled(state, checkX, checkY)) {
                filledCorners++;
            }
        }
        
        // 3개 이상의 코너가 채워져 있으면 T-Spin
        return filledCorners >= 3;
    }
    
    /**
     * 특정 위치의 코너가 채워져 있는지 확인합니다
     * 
     * 코너가 채워진 것으로 판정되는 경우:
     * 1. 보드 밖인 경우
     * 2. 블록이 이미 있는 경우
     * 
     * @param state 현재 게임 상태
     * @param x 체크할 X 좌표
     * @param y 체크할 Y 좌표
     * @return 코너가 채워져 있으면 true
     */
    private static boolean isCornerFilled(GameState state, int x, int y) {
        // 보드 밖 = 채워진 것으로 판정
        if (x < 0 || x >= state.getBoardWidth() || 
            y < 0 || y >= state.getBoardHeight()) {
            return true;
        }
        
        // 블록이 있으면 채워진 것으로 판정
        return state.getGrid()[y][x].isOccupied();
    }
    
    /**
     * T-Spin Mini 여부를 감지합니다
     * 
     * 조건:
     * 1. T-Spin이어야 함 (3-Corner Rule 만족)
     * 2. Wall Kick 5번째 테스트(index 4) 사용 안 함
     * 3. 정면 2개 코너 중 1개 이상 비어있음
     * 
     * @param state 현재 게임 상태
     * @return T-Spin Mini이면 true, 아니면 false
     */
    private static boolean detectTSpinMini(GameState state) {
        // 먼저 T-Spin인지 확인
        if (!detectTSpin(state)) {
            return false;
        }
        
        // Wall Kick 5번째 테스트 사용 시 T-Spin Mini 아님
        if (state.getLastRotationKickIndex() == 4) {
            return false;
        }
        
        // 정면 2개 코너 체크
        return checkFrontCornersForMini(state);
    }
    
    /**
     * T 블록의 정면 2개 코너를 체크합니다
     * 
     * 회전 상태에 따라 정면이 달라집니다:
     * - 0(상향): 위쪽 2개 코너 [{-1,-1}, {1,-1}]
     * - 1(우향): 오른쪽 2개 코너 [{1,-1}, {1,1}]
     * - 2(하향): 아래쪽 2개 코너 [{-1,1}, {1,1}]
     * - 3(좌향): 왼쪽 2개 코너 [{-1,-1}, {-1,1}]
     * 
     * @param state 현재 게임 상태
     * @return 정면 2개 코너 중 1개 이상이 비어있으면 true (T-Spin Mini)
     */
    private static boolean checkFrontCornersForMini(GameState state) {
        int px = state.getCurrentX();
        int py = state.getCurrentY();
        RotationState rotation = state.getCurrentTetromino().getRotationState();
        
        int[][] frontCorners;
        switch (rotation) {
            case SPAWN: // 상향: 위쪽 2개
                frontCorners = new int[][]{{-1, -1}, {1, -1}};
                break;
            case RIGHT: // 우향: 오른쪽 2개
                frontCorners = new int[][]{{1, -1}, {1, 1}};
                break;
            case REVERSE: // 하향: 아래쪽 2개
                frontCorners = new int[][]{{-1, 1}, {1, 1}};
                break;
            case LEFT: // 좌향: 왼쪽 2개
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
        
        // 정면 2개 코너 중 1개 이상이 비어있으면 Mini
        return filledCount < 2;
    }
    
    // ========== 라인 클리어 ===================
    /**
     * 라인 클리어를 체크하고 실행합니다
     * Phase 2: GameState에 직접 라인 클리어 정보를 저장 (반환값 없음)
     * 
     * @param state 현재 게임 상태
     * @param isTSpin T-Spin 여부 (블록 고정 전에 미리 감지된 값)
     * @param isTSpinMini T-Spin Mini 여부 (블록 고정 전에 미리 감지된 값)
     */
    private static void checkAndClearLines(GameState state, boolean isTSpin, boolean isTSpinMini) {
        List<Integer> clearedRowsList = new ArrayList<>();

        // 라인 체크 (아래에서 위로)
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
            
            // T-Spin Mini (라인 없음)는 점수를 받아야 함!
            if (isTSpin && isTSpinMini) {
                long score = GameConstants.TSPIN_MINI_NO_LINE * state.getLevel();
                state.setLastScoreEarned(score);
            } else if (isTSpin && !isTSpinMini) {
                // 일반 T-Spin (라인 없음)도 점수를 받음
                long score = GameConstants.TSPIN_NO_LINE * state.getLevel();
                state.setLastScoreEarned(score);
            } else {
                // 일반 고정 (라인 없음, T-Spin 아님)
                state.setLastScoreEarned(0);
            }
            return;
        }

        // 라인 클리어 실행 (수정된 버전)
        // 여러 줄이 동시에 클리어될 때 인덱스 문제를 해결하기 위해
        // 클리어되지 않은 라인들만 모아서 아래부터 다시 배치합니다
        
        // 성능 개선: HashSet으로 변환하여 O(1) 조회 성능 확보
        java.util.Set<Integer> clearedRowsSet = new java.util.HashSet<>(clearedRowsList);
        
        // 1. 클리어되지 않은 라인들만 수집
        List<Cell[]> remainingRows = new ArrayList<>();
        for (int row = state.getBoardHeight() - 1; row >= 0; row--) {
            if (!clearedRowsSet.contains(row)) {  // O(1) 조회
                // 이 줄은 클리어되지 않았으므로 보존
                Cell[] rowCopy = new Cell[state.getBoardWidth()];
                for (int col = 0; col < state.getBoardWidth(); col++) {
                    rowCopy[col] = state.getGrid()[row][col].copy();
                }
                remainingRows.add(rowCopy);
            }
        }
        
        // 2. 보드를 아래에서부터 다시 채우기
        int targetRow = state.getBoardHeight() - 1;
        for (Cell[] rowData : remainingRows) {
            for (int col = 0; col < state.getBoardWidth(); col++) {
                state.getGrid()[targetRow][col] = rowData[col];
            }
            targetRow--;
        }
        
        // 3. 남은 위쪽 줄들을 빈 칸으로 초기화 (버그 수정)
        while (targetRow >= 0) {
            for (int col = 0; col < state.getBoardWidth(); col++) {
                state.getGrid()[targetRow][col] = Cell.empty();
            }
            targetRow--;
        }

        int linesCleared = clearedRowsList.size();

        // Perfect clear 체크
        boolean isPerfectClear = checkPerfectClear(state);

        // T-Spin과 T-Spin Mini는 이미 블록 고정 전에 감지되어 매개변수로 전달됨
        // (블록 고정 후에는 T 블록 자신도 "채워진 것"으로 판정되어 오류 발생)

        // 점수 계산
        long score = calculateScore(linesCleared, isTSpin, isTSpinMini, isPerfectClear,
                state.getLevel(), state.getComboCount(), state.getBackToBackCount()
        );

        // Phase 2: GameState에 라인 클리어 정보 직접 저장
        state.setLastLinesCleared(linesCleared);
        
        // clearedRowsList를 int[] 배열로 변환
        int[] clearedRowsArray = new int[clearedRowsList.size()];
        for (int i = 0; i < clearedRowsList.size(); i++) {
            clearedRowsArray[i] = clearedRowsList.get(i);
        }
        state.setLastClearedRows(clearedRowsArray);
        
        state.setLastScoreEarned(score);
        state.setLastIsPerfectClear(isPerfectClear);
    }

    private static boolean checkPerfectClear(GameState state) {
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
     * 점수를 계산합니다
     * 
     * 테트리스의 점수 시스템은 매우 복잡합니다:
     * - 기본 점수: 라인 수에 따라 다름 (Single < Double < Triple < Tetris)
     * - T-Spin 보너스: T-Spin은 더 높은 점수
     * - Perfect Clear 보너스: 모든 블록을 지우면 추가 점수
     * - 콤보 보너스: 연속으로 라인을 지우면 추가 점수
     * - B2B 보너스: Tetris나 T-Spin을 연속으로 하면 1.5배
     * - 레벨 배수: 레벨이 높을수록 점수가 높음
     */
    private static long calculateScore(int lines, boolean tSpin, boolean tSpinMini,
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

        // 레벨 배수
        return baseScore * level;
    }

    // ========== 위치 검증 헬퍼 메서드 ==========
    
    /**
     * 주어진 위치에 테트로미노를 놓을 수 있는지 검증합니다
     * 
     * 이 메서드는 GameEngine의 거의 모든 메서드에서 사용됩니다.
     * 이동하기 전, 회전하기 전 항상 검증이 필요하니까요.
     * 
     * @param state 현재 게임 상태
     * @param tetromino 검증할 테트로미노
     * @param x 검증할 X 위치
     * @param y 검증할 Y 위치
     * @return true면 놓을 수 있음, false면 충돌
     */
    private static boolean isValidPosition(GameState state, Tetromino tetromino, int x, int y){
        int[][] shape = tetromino.getCurrentShape();
        
        // ✅ 방어적 검사: shape이 비어있거나 null인 경우
        if (shape == null || shape.length == 0) {
            System.err.println("⚠️ [GameEngine] isValidPosition(): shape is null or empty!");
            return false;
        }

        for(int row = 0; row < shape.length; row++){
            // ✅ 방어적 검사: 각 행이 비어있거나 null인 경우
            if (shape[row] == null || shape[row].length == 0) {
                System.err.println("⚠️ [GameEngine] isValidPosition(): shape[" + row + "] is null or empty!");
                continue;  // 빈 행은 건너뛰기
            }
            
            for(int col = 0; col < shape[row].length; col++){
                if(shape[row][col] == 1) {
                    int absX = x + (col - tetromino.getPivotX());
                    int absY = y + (row - tetromino.getPivotY());

                    // 보드 경계 체크
                    if(absX < 0 || absX >= state.getBoardWidth() || absY >= state.getBoardHeight()) {
                        return false;
                    }
                    // 다른 블록과 충돌 체크. (보드 위쪽은 통과 spawn 위치이므로 허용)
                    if(absY >= 0 && state.getGrid()[absY][absX].isOccupied()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    
}
