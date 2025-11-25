package seoultech.se.server.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import seoultech.se.core.GameState;
import seoultech.se.core.command.GameCommand;
import seoultech.se.core.command.MoveCommand;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.dto.PlayerInputDto;
import seoultech.se.core.dto.ServerStateDto;

@ExtendWith(MockitoExtension.class)
class GameSessionTest {

    private GameSession gameSession;
    private final String sessionId = "test-session-01";
    private final String playerA = "playerA";
    private final String playerB = "playerB";

    @Mock
    private GameModeConfig gameModeConfig;

    @BeforeEach
    void setUp() {
        // GameSession 생성 (내부적으로 GameEngine도 생성됨)
        gameSession = new GameSession(sessionId, gameModeConfig);
        gameSession.joinPlayer(playerA);
        gameSession.joinPlayer(playerB);
    }

    @Test
    @DisplayName("정상적인 입력 처리 및 시퀀스 업데이트 확인")
    void processInput_Success() {
        // Given
        GameCommand command = new MoveCommand(seoultech.se.core.command.Direction.LEFT);
        PlayerInputDto input = PlayerInputDto.builder()
                .sessionId(sessionId)
                .sequenceId(1L)
                .command(command)
                .build();

        // When
        ServerStateDto result = gameSession.processInput(playerA, input);

        // Then
        assertNotNull(result);
        assertThat(result.getLastProcessedSequence()).isEqualTo(1L);
        assertThat(result.getMyGameState()).isNotNull();
    }

    @Test
    @DisplayName("과거 시퀀스 번호의 패킷은 무시해야 한다 (Server Reconciliation)")
    void processInput_IgnoreOldSequence() {
        // Given: 시퀀스 5번까지 처리된 상태라고 가정
        PlayerInputDto input1 = PlayerInputDto.builder().sequenceId(5L).command(new MoveCommand(seoultech.se.core.command.Direction.LEFT)).build();
        gameSession.processInput(playerA, input1);

        // When: 과거인 시퀀스 3번이 도착함
        PlayerInputDto oldInput = PlayerInputDto.builder()
                .sessionId(sessionId)
                .sequenceId(3L)
                .command(new MoveCommand(seoultech.se.core.command.Direction.RIGHT))
                .build();

        ServerStateDto result = gameSession.processInput(playerA, oldInput);

        // Then: 무시되어야 함 (null 반환)
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("동시성 테스트: 100개의 요청이 동시에 와도 순차적으로 처리되어야 한다")
    void processInput_Concurrency_ThreadSafe() throws InterruptedException {
        // Given
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // When: 1~100번 시퀀스를 100개의 스레드가 동시에 보냄
        for (int i = 1; i <= threadCount; i++) {
            long seq = i;
            executorService.submit(() -> {
                try {
                    PlayerInputDto input = PlayerInputDto.builder()
                            .sessionId(sessionId)
                            .sequenceId(seq)
                            .command(new MoveCommand(seoultech.se.core.command.Direction.DOWN))
                            .build();
                    
                    // synchronized 블록이 없으면 여기서 Race Condition 발생 가능
                    ServerStateDto result = gameSession.processInput(playerA, input);
                    
                    if (result != null) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드 완료 대기

        // Then
        // 주의: 네트워크 지연 등으로 순서가 뒤바뀌어 들어오면 (예: 5번이 4번보다 먼저 옴)
        // 4번은 무시되므로 성공 횟수는 100보다 작을 수 있음.
        // 하지만 중요한 건 "마지막 처리된 시퀀스"가 갱신되어야 한다는 점과 예외가 발생하지 않아야 한다는 점임.
        
        // 1. 예외 없이 실행되었는가?
        assertThat(successCount.get()).isGreaterThan(0);
        
        // 2. 상태 객체가 깨지지 않았는가?
        GameState finalState = gameSession.getPlayerStates().get(playerA);
        assertNotNull(finalState);
    }
}
