package seoultech.se.client.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import seoultech.se.backend.mapper.GameStateDtoToGameStateMapper;
import seoultech.se.client.service.GameApiService;
import seoultech.se.client.strategy.NetworkExecutionStrategy;
import seoultech.se.client.ui.OpponentBoardView;
import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;

@Component
@Scope("prototype")
public class MultiGameController extends BaseGameController {

    @Autowired
    private GameApiService gameApiService;

    @Autowired(required = false)
    private GameStateDtoToGameStateMapper dtoToStateMapper;

    private NetworkExecutionStrategy executionStrategy;
    private OpponentBoardView opponentBoardView;
    private String sessionId;

    /**
     * 멀티플레이 모드 초기화 (외부 호출)
     */
    public void initMultiplayer(seoultech.se.client.strategy.NetworkExecutionStrategy strategy, String sessionId) {
        this.executionStrategy = strategy;
        this.sessionId = sessionId;
        
        // 서버에서 초기 상태 받아오기
        GameState initialState = boardController.getGameState(); // 기본값
        if (gameApiService != null && dtoToStateMapper != null) {
            seoultech.se.core.dto.ServerStateDto initialServerState = gameApiService.getInitialState(sessionId);
            if (initialServerState != null && initialServerState.getMyGameState() != null) {
                // GameStateDto를 GameState로 변환
                GameState serverInitialState = dtoToStateMapper.toGameState(initialServerState.getMyGameState());
                if (serverInitialState != null) {
                    initialState = serverInitialState;
                    // BoardController에 초기 상태 설정
                    boardController.setGameState(initialState);
                    System.out.println("✅ [MultiGameController] Initial state received and set from server");
                    
                    // 상대방 초기 상태도 설정
                    if (initialServerState.getOpponentGameState() != null) {
                        GameState opponentInitialState = dtoToStateMapper.toGameState(initialServerState.getOpponentGameState());
                        if (opponentInitialState != null && opponentBoardView != null) {
                            Platform.runLater(() -> {
                                opponentBoardView.update(opponentInitialState);
                            });
                        }
                    }
                }
            }
        }
        
        // NetworkCallback 연결
        strategy.setupMultiplayMode(
            sessionId,
            initialState,
            this::onMyStateUpdate,
            this::onOpponentStateUpdate,
            this::onAttackLinesReceived
        );
        
        boardController.setExecutionStrategy(strategy);
        
        // 서버에 게임 시작 알림
        if (gameApiService != null) {
            gameApiService.startGame(sessionId);
        }
    }

    @Override
    protected void onInitComplete() {
        System.out.println("🌐 [MultiGameController] Initializing Multiplayer Mode...");
        
        // 상대방 보드 설정
        this.opponentBoardView = new OpponentBoardView();
        if (opponentContainer != null) {
            opponentContainer.getChildren().setAll(opponentBoardView);
            opponentContainer.setVisible(true);
            opponentContainer.setManaged(true);
        }
        
        // 아이템 인벤토리 숨김 (멀티플레이는 아이템 미지원 가정, 혹은 추후 추가)
        if (itemInventoryContainer != null) {
            itemInventoryContainer.setVisible(false);
            itemInventoryContainer.setManaged(false);
        }
        
        // 멀티플레이는 Pause 불가
        if (inputHandler != null) {
            inputHandler.setMultiplayerMode(true);
        }
    }

    @Override
    public void startGame() {
        System.out.println("▶️ [MultiGameController] Game Started (Server Auth)");
        if (gameOverLabel != null) gameOverLabel.setVisible(false);
        popupManager.hideAllPopups();
        boardGridPane.requestFocus();
        // 멀티플레이는 GameLoopManager를 사용하지 않음 (서버 중력)
    }

    @Override
    public void cleanup() {
        System.out.println("🧹 [MultiGameController] Cleanup");
        if (executionStrategy != null) {
            executionStrategy.cleanup();
        }
        // ✅ 입력 차단 제거: cleanup()은 게임 종료 시 호출되며, InputHandler의 isGameOver() 체크로 자동 차단됨
    }

    @Override
    protected void handleCommand(GameCommand command) {
        // 게임 오버 체크
        if (boardController.getGameState().isGameOver()) {
            return; // 게임 오버 시 명령 무시
        }

        // 명령 필터링: MOVE, ROTATE, HARD_DROP, HOLD만 허용
        seoultech.se.core.command.CommandType commandType = command.getType();
        boolean isAllowed = false;
        
        if (commandType == seoultech.se.core.command.CommandType.MOVE) {
            // MOVE 명령은 모든 방향 허용 (LEFT, RIGHT, DOWN은 자동 낙하로 서버 처리)
            isAllowed = true;
        } else if (commandType == seoultech.se.core.command.CommandType.ROTATE) {
            isAllowed = true;
        } else if (commandType == seoultech.se.core.command.CommandType.HARD_DROP) {
            isAllowed = true;
        } else if (commandType == seoultech.se.core.command.CommandType.HOLD) {
            isAllowed = true;
        }
        
        if (!isAllowed) {
            // 허용되지 않은 명령은 무시 (PAUSE, RESUME, SOFT_DROP 등)
            System.out.println("🚫 [MultiGameController] Command filtered: " + commandType);
            return;
        }

        // 서버에 명령 전송 (Client-side prediction 제거)
        // executeCommand 내부에서 서버 전송만 수행
        GameState oldState = boardController.getGameState();
        GameState newState = boardController.executeCommand(command);
        
        // 서버 응답 대기 중에는 이전 상태 유지
        // 서버 응답은 onMyStateUpdate()에서 처리됨
        if (newState != null && newState != oldState) {
            updateUI(oldState, newState);
        }
    }

    // --- Network Callbacks ---

    private void onMyStateUpdate(GameState newState) {
        Platform.runLater(() -> {
            // 서버 상태로 보정 (Reconciliation)
            System.out.println("📥 [MultiGameController] Received state update from server. Tetromino Y: " + 
                (newState.getCurrentTetromino() != null ? newState.getCurrentY() : "null")); 
            GameState oldState = boardController.getGameState();
            boardController.setGameState(newState);
            updateUI(oldState, newState);
        });
    }

    private void onOpponentStateUpdate(GameState opponentState) {
        Platform.runLater(() -> {
            if (opponentBoardView != null) {
                opponentBoardView.update(opponentState);
            }
        });
    }

    private void onAttackLinesReceived(int lines) {
        Platform.runLater(() -> {
            notificationManager.showAttackNotification(lines);
        });
    }
    
    @Override
    protected void processGameOver(long finalScore) {
        System.out.println("💥 [MultiGameController] Game Over. Score: " + finalScore);
        if (gameOverLabel != null) gameOverLabel.setVisible(true);
        
        // 승리/패배 판정
        // 1. 서버가 강제로 게임 오버를 보냄
        // 2. 내 보드가 실제로 꽉 찼는지 확인 (BLOCK_OUT)
        // 3. 내 보드가 괜찮은데 게임 오버라면 상대방이 죽은 것 -> 승리
        
        String title = "GAME OVER";
        GameState state = boardController.getGameState();
        
        // GameOverReason이 "GAME_OVER"이고 (서버 강제 종료),
        // 내 보드가 꽉 찬게 아니라면 (BLOCK_OUT이 아님), 승리로 간주
        // 주의: 서버에서 "GAME_OVER"를 보낼 때의 조건을 명확히 해야 함
        // 현재는 상대방 죽음 -> 나에게 GAME_OVER 전송 -> 내 보드 멀쩡함 -> 승리
        
        if (state.getGameOverReason() != null && state.getGameOverReason().equals("GAME_OVER")) {
             // 서버가 보낸 일반 게임 종료 신호 (상대방 사망 등)
             // 내가 죽어서 끝난건지 확인
             if (isMyBoardFull(state)) {
                 title = "YOU LOSE";
             } else {
                 title = "YOU WIN";
             }
        } else {
            // 로컬에서 죽은 경우 (BLOCK_OUT)
            title = "YOU LOSE";
        }
        
        System.out.println("🏆 [MultiGameController] Result: " + title + " (Reason: " + state.getGameOverReason() + ")");

        boolean isItemMode = gameModeConfig != null && gameModeConfig.isItemSystemEnabled();
        popupManager.showGameOverPopup(finalScore, isItemMode, settingsService.getCurrentDifficulty(), title);
        
        cleanup();
    }
    
    private boolean isMyBoardFull(GameState state) {
        // 간단한 판정: 현재 블록이 null이거나, spawn 위치에서 충돌했거나
        // GameState.isGameOver()는 이미 true임
        // gameOverReason이 "BLOCK_OUT"이면 확실히 패배
        return "BLOCK_OUT".equals(state.getGameOverReason()) || "LOCK_OUT".equals(state.getGameOverReason());
    }
}
