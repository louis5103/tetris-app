package seoultech.se.backend.network;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static seoultech.se.backend.network.P2PNetworkErrorHandler.ErrorHandlingAction;

class P2PNetworkErrorHandlerTest {

    private P2PNetworkErrorHandler errorHandler;

    @BeforeEach
    void setUp() {
        errorHandler = new P2PNetworkErrorHandler();
    }

    @Test
    @DisplayName("TIMEOUT 오류 처리 - 첫 번째 재시도")
    void handleTimeout_firstRetry() {
        // given
        String requestId = "request-001";

        // when
        P2PNetworkErrorHandler.ErrorHandlingResult result =
            errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId);

        // then
        assertTrue(result.shouldRetry());
        assertEquals(ErrorHandlingAction.RETRY, result.getAction());
        assertEquals(1, result.getRetryCount());
        assertEquals("재시도합니다", result.getMessage());
    }

    @Test
    @DisplayName("TIMEOUT 오류 처리 - 최대 3회 재시도")
    void handleTimeout_maxRetries() {
        // given
        String requestId = "request-002";

        // when & then
        P2PNetworkErrorHandler.ErrorHandlingResult result1 =
            errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId);
        assertEquals(1, result1.getRetryCount());
        assertTrue(result1.shouldRetry());

        P2PNetworkErrorHandler.ErrorHandlingResult result2 =
            errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId);
        assertEquals(2, result2.getRetryCount());
        assertTrue(result2.shouldRetry());

        P2PNetworkErrorHandler.ErrorHandlingResult result3 =
            errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId);
        assertEquals(3, result3.getRetryCount());
        assertTrue(result3.shouldRetry());

        // 4번째 시도는 실패
        P2PNetworkErrorHandler.ErrorHandlingResult result4 =
            errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId);
        assertEquals(ErrorHandlingAction.FAIL, result4.getAction());
        assertFalse(result4.shouldRetry());
        assertEquals("최대 재시도 횟수를 초과했습니다", result4.getMessage());
    }

    @Test
    @DisplayName("재시도 카운터 초기화")
    void resetRetryCount() {
        // given
        String requestId = "request-003";
        errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId);
        errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId);

        // when
        errorHandler.resetRetryCount(requestId);
        P2PNetworkErrorHandler.ErrorHandlingResult result =
            errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId);

        // then
        assertEquals(1, result.getRetryCount());
        assertTrue(result.shouldRetry());
    }

    @Test
    @DisplayName("UNAUTHORIZED 오류 처리 - 재로그인 요구")
    void handleUnauthorized() {
        // given
        String requestId = "request-004";

        // when
        P2PNetworkErrorHandler.ErrorHandlingResult result =
            errorHandler.handleError(P2PNetworkError.UNAUTHORIZED, requestId);

        // then
        assertTrue(result.requiresRelogin());
        assertEquals(ErrorHandlingAction.RELOGIN, result.getAction());
        assertEquals("재로그인이 필요합니다", result.getMessage());
        assertFalse(result.shouldRetry());
    }

    @Test
    @DisplayName("STATE_CONFLICT 오류 처리 - 서버 동기화")
    void handleStateConflict() {
        // given
        String requestId = "request-005";

        // when
        P2PNetworkErrorHandler.ErrorHandlingResult result =
            errorHandler.handleError(P2PNetworkError.STATE_CONFLICT, requestId);

        // then
        assertTrue(result.requiresSync());
        assertEquals(ErrorHandlingAction.SYNC, result.getAction());
        assertEquals("서버 상태와 동기화합니다", result.getMessage());
        assertFalse(result.shouldRetry());
    }

    @Test
    @DisplayName("RATE_LIMIT 오류 처리 - Throttling")
    void handleRateLimit() {
        // given
        String requestId = "request-006";

        // when
        P2PNetworkErrorHandler.ErrorHandlingResult result =
            errorHandler.handleError(P2PNetworkError.RATE_LIMIT, requestId);

        // then
        assertEquals(ErrorHandlingAction.THROTTLE, result.getAction());
        assertEquals("요청 제한이 적용되었습니다", result.getMessage());
        assertFalse(result.shouldRetry());
    }

    @Test
    @DisplayName("SERVER_ERROR 오류 처리 - 오프라인 큐잉")
    void handleServerError() {
        // given
        String requestId = "request-007";

        // when
        P2PNetworkErrorHandler.ErrorHandlingResult result =
            errorHandler.handleError(P2PNetworkError.SERVER_ERROR, requestId);

        // then
        assertEquals(ErrorHandlingAction.QUEUE, result.getAction());
        assertTrue(result.getMessage().contains("오프라인 큐에 추가되었습니다"));
        assertTrue(result.getMessage().contains(requestId));
        assertFalse(result.shouldRetry());
    }

    @Test
    @DisplayName("서로 다른 요청 ID의 재시도 카운터는 독립적으로 관리")
    void independentRetryCounters() {
        // given
        String requestId1 = "request-008";
        String requestId2 = "request-009";

        // when
        errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId1);
        errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId1);

        P2PNetworkErrorHandler.ErrorHandlingResult result1 =
            errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId1);
        P2PNetworkErrorHandler.ErrorHandlingResult result2 =
            errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId2);

        // then
        assertEquals(3, result1.getRetryCount());
        assertEquals(1, result2.getRetryCount());
    }

    @Test
    @DisplayName("동시성 테스트 - 서로 다른 요청 ID로 동시 처리")
    void concurrentErrorHandling() throws InterruptedException {
        // given - 실제 환경처럼 서로 다른 요청 ID로 동시에 처리
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        // when - 각 스레드가 고유한 요청 ID로 처리
        for (int i = 0; i < threadCount; i++) {
            final int requestIndex = i;
            executorService.submit(() -> {
                try {
                    startLatch.await();
                    String requestId = "concurrent-request-" + requestIndex;
                    P2PNetworkErrorHandler.ErrorHandlingResult result =
                        errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId);
                    if (result.shouldRetry()) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        endLatch.await(5, TimeUnit.SECONDS);
        executorService.shutdown();

        // then - 모든 요청이 첫 번째 재시도에 성공해야 함
        assertEquals(threadCount, successCount.get(),
            "서로 다른 요청 ID는 각각 독립적으로 재시도할 수 있어야 합니다");
    }

    @Test
    @DisplayName("ErrorHandlingResult - shouldRetry 메서드 테스트")
    void errorHandlingResult_shouldRetry() {
        // when
        P2PNetworkErrorHandler.ErrorHandlingResult retryResult =
            P2PNetworkErrorHandler.ErrorHandlingResult.retry(1);
        P2PNetworkErrorHandler.ErrorHandlingResult failResult =
            P2PNetworkErrorHandler.ErrorHandlingResult.failure("실패");

        // then
        assertTrue(retryResult.shouldRetry());
        assertFalse(failResult.shouldRetry());
    }

    @Test
    @DisplayName("ErrorHandlingResult - requiresRelogin 메서드 테스트")
    void errorHandlingResult_requiresRelogin() {
        // when
        P2PNetworkErrorHandler.ErrorHandlingResult reloginResult =
            P2PNetworkErrorHandler.ErrorHandlingResult.reloginRequired();
        P2PNetworkErrorHandler.ErrorHandlingResult retryResult =
            P2PNetworkErrorHandler.ErrorHandlingResult.retry(1);

        // then
        assertTrue(reloginResult.requiresRelogin());
        assertFalse(retryResult.requiresRelogin());
    }

    @Test
    @DisplayName("ErrorHandlingResult - requiresSync 메서드 테스트")
    void errorHandlingResult_requiresSync() {
        // when
        P2PNetworkErrorHandler.ErrorHandlingResult syncResult =
            P2PNetworkErrorHandler.ErrorHandlingResult.syncRequired();
        P2PNetworkErrorHandler.ErrorHandlingResult retryResult =
            P2PNetworkErrorHandler.ErrorHandlingResult.retry(1);

        // then
        assertTrue(syncResult.requiresSync());
        assertFalse(retryResult.requiresSync());
    }

    @Test
    @DisplayName("여러 오류 타입을 순차적으로 처리")
    void handleMultipleErrorTypes() {
        // given
        String requestId = "request-010";

        // when & then
        P2PNetworkErrorHandler.ErrorHandlingResult timeoutResult =
            errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId);
        assertEquals(ErrorHandlingAction.RETRY, timeoutResult.getAction());

        P2PNetworkErrorHandler.ErrorHandlingResult unauthorizedResult =
            errorHandler.handleError(P2PNetworkError.UNAUTHORIZED, requestId);
        assertEquals(ErrorHandlingAction.RELOGIN, unauthorizedResult.getAction());

        P2PNetworkErrorHandler.ErrorHandlingResult conflictResult =
            errorHandler.handleError(P2PNetworkError.STATE_CONFLICT, requestId);
        assertEquals(ErrorHandlingAction.SYNC, conflictResult.getAction());

        P2PNetworkErrorHandler.ErrorHandlingResult rateLimitResult =
            errorHandler.handleError(P2PNetworkError.RATE_LIMIT, requestId);
        assertEquals(ErrorHandlingAction.THROTTLE, rateLimitResult.getAction());

        P2PNetworkErrorHandler.ErrorHandlingResult serverErrorResult =
            errorHandler.handleError(P2PNetworkError.SERVER_ERROR, requestId);
        assertEquals(ErrorHandlingAction.QUEUE, serverErrorResult.getAction());
    }

    @Test
    @DisplayName("재시도 카운터 초기화 후 다시 재시도 가능")
    void retryAfterReset() {
        // given
        String requestId = "request-011";

        // 최대 재시도 도달
        for (int i = 0; i < 4; i++) {
            errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId);
        }

        // when - 초기화 후 다시 시도
        errorHandler.resetRetryCount(requestId);
        P2PNetworkErrorHandler.ErrorHandlingResult result =
            errorHandler.handleError(P2PNetworkError.TIMEOUT, requestId);

        // then
        assertTrue(result.shouldRetry());
        assertEquals(1, result.getRetryCount());
    }

    @Test
    @DisplayName("존재하지 않는 요청 ID 초기화는 에러 없이 처리")
    void resetNonExistentRequestId() {
        // given
        String nonExistentId = "non-existent-request";

        // when & then - 예외 발생하지 않아야 함
        assertDoesNotThrow(() -> errorHandler.resetRetryCount(nonExistentId));
    }
}
