package seoultech.se.core.engine;

import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.model.enumType.RotationDirection;

/**
 * 게임 엔진 인터페이스
 * 
 * Strategy Pattern을 적용하여 게임 모드에 따라 다른 엔진을 사용합니다.
 * - ClassicGameEngine: 기본 테트리스 (아이템 없음)
 * - ArcadeGameEngine: 아케이드 모드 (아이템 있음)
 * 
 * 설계 원칙:
 * - 모든 메서드는 GameState를 받아 새로운 GameState를 반환 (불변성)
 * - 실패 시 원본 상태 반환
 * - Phase 2: Result 객체 제거, GameState만으로 모든 정보 전달
 */
public interface GameEngine {
    
    /**
     * 게임 엔진 초기화
     * 
     * @param config 게임 모드 설정
     */
    void initialize(GameModeConfig config);
    
    /**
     * 왼쪽 이동 시도
     * 
     * @param state 현재 게임 상태
     * @return 새로운 게임 상태 (이동 실패 시 원본 상태 반환)
     */
    GameState tryMoveLeft(GameState state);
    
    /**
     * 오른쪽 이동 시도
     * 
     * @param state 현재 게임 상태
     * @return 새로운 게임 상태 (이동 실패 시 원본 상태 반환)
     */
    GameState tryMoveRight(GameState state);
    
    /**
     * 아래로 이동 시도
     * 
     * Soft Drop:
     * - isSoftDrop이 true이면 수동 DOWN 입력으로 간주하여 1점 부여
     * - isSoftDrop이 false이면 자동 낙하로 간주하여 점수 없음
     * 
     * @param state 현재 게임 상태
     * @param isSoftDrop 수동 DOWN 입력 여부
     * @return 새로운 게임 상태 (이동 실패 시 원본 상태 반환)
     */
    GameState tryMoveDown(GameState state, boolean isSoftDrop);
    
    /**
     * 회전 시도 (SRS Wall Kick 포함)
     * 
     * @param state 현재 게임 상태
     * @param direction 회전 방향 (시계/반시계)
     * @param srsEnabled SRS 활성화 여부
     * @return 새로운 게임 상태 (회전 실패 시 원본 상태 반환)
     */
    GameState tryRotate(GameState state, RotationDirection direction, boolean srsEnabled);
    
    /**
     * 회전 시도 (기본값: SRS 활성화)
     * 
     * @param state 현재 게임 상태
     * @param direction 회전 방향
     * @return 새로운 게임 상태
     */
    default GameState tryRotate(GameState state, RotationDirection direction) {
        return tryRotate(state, direction, true);
    }
    
    /**
     * Hard Drop 실행
     * 
     * 블록을 즉시 바닥까지 떨어뜨리고 고정합니다.
     * 
     * @param state 현재 게임 상태
     * @return 새로운 게임 상태 (고정 완료, 라인 클리어 처리 완료)
     */
    GameState hardDrop(GameState state);
    
    /**
     * Hold 기능 실행
     * 
     * 규칙:
     * - 한 턴에 한 번만 사용 가능
     * - Hold가 비어있으면: 현재 블록 보관 + Next에서 새 블록 가져오기
     * - Hold에 블록이 있으면: 현재 블록과 Hold 블록 교체
     * 
     * @param state 현재 게임 상태
     * @return 새로운 게임 상태 (Hold 실패 시 원본 상태 반환)
     */
    GameState tryHold(GameState state);
    
    /**
     * 테트로미노를 보드에 고정하고 라인 클리어 처리
     * 
     * 실행 순서:
     * 1. 게임 오버 체크
     * 2. 블록 고정
     * 3. 라인 클리어
     * 4. 점수 계산
     * 5. Hold 재사용 가능하게 설정
     * 
     * @param state 현재 게임 상태
     * @return 고정이 완료된 새로운 게임 상태
     */
    GameState lockTetromino(GameState state);
    
    /**
     * 아이템 시스템 활성화 여부
     * 
     * @return 아이템 시스템이 활성화되어 있으면 true
     */
    boolean isItemSystemEnabled();
    
    /**
     * 명령 실행 (Command Pattern)
     * 
     * @param command 게임 명령
     * @param state 현재 게임 상태
     * @return 새로운 게임 상태
     */
    default GameState executeCommand(GameCommand command, GameState state) {
        if (command == null || state == null) {
            return state;
        }
        
        switch (command.getCommandType()) {
            case MOVE:
                // MoveCommand를 캐스팅하여 Direction 확인
                if (command instanceof seoultech.se.core.command.MoveCommand) {
                    seoultech.se.core.command.MoveCommand moveCmd = (seoultech.se.core.command.MoveCommand) command;
                    switch (moveCmd.getDirection()) {
                        case LEFT:
                            return tryMoveLeft(state);
                        case RIGHT:
                            return tryMoveRight(state);
                        case DOWN:
                            // ✨ DOWN 이동 시도 후, 실패하면 블록 고정 처리
                            System.out.println("▶️ [GameEngine] executeCommand(DOWN) - calling tryMoveDown()...");
                            GameState newState = tryMoveDown(state, moveCmd.isSoftDrop());
                            if (newState == state) {
                                // 이동 실패: 블록을 고정 (lockTetromino가 라인 클리어까지 처리)
                                // 새 블록 생성은 BoardController에서 처리
                                System.out.println("🔐 [GameEngine] tryMoveDown FAILED → calling lockTetromino()");
                                return lockTetromino(state);
                            }
                            System.out.println("✅ [GameEngine] tryMoveDown SUCCESS → returning new state");
                            return newState;
                        default:
                            return state;
                    }
                }
                return state;
            case ROTATE:
                // RotateCommand를 캐스팅하여 방향 확인
                if (command instanceof seoultech.se.core.command.RotateCommand) {
                    seoultech.se.core.command.RotateCommand rotateCmd = (seoultech.se.core.command.RotateCommand) command;
                    return tryRotate(state, rotateCmd.getDirection());
                }
                return state;
            case HARD_DROP:
                System.out.println("⚡ [GameEngine] executeCommand(HARD_DROP) - calling hardDrop()");
                return hardDrop(state);
            case HOLD:
                return tryHold(state);
            case PAUSE:
                // GameState를 복사하여 isPaused를 true로 설정
                if (!state.isPaused()) {
                    GameState pausedState = state.deepCopy();
                    pausedState.setPaused(true);
                    return pausedState;
                }
                return state;
            case RESUME:
                // GameState를 복사하여 isPaused를 false로 설정
                if (state.isPaused()) {
                    GameState resumedState = state.deepCopy();
                    resumedState.setPaused(false);
                    return resumedState;
                }
                return state;
            default:
                return state;
        }
    }
}
