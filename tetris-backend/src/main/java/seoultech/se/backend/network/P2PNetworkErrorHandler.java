package seoultech.se.backend.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * P2P 네트워크 오류 처리 핸들러
 *
 * 각 오류 타입에 따른 처리 로직을 구현합니다.
 */
@Component
public class P2PNetworkErrorHandler {

    private static final Logger logger = LoggerFactory.getLogger(P2PNetworkErrorHandler.class);
    private static final int MAX_RETRY_COUNT = 3;

    // 요청별 재시도 횟수 추적
    private final ConcurrentHashMap<String, AtomicInteger> retryCountMap = new ConcurrentHashMap<>();

    /**
     * 네트워크 오류 처리
     *
     * @param error 발생한 오류 타입
     * @param requestId 요청 식별자
     * @return 처리 결과
     */
    public ErrorHandlingResult handleError(P2PNetworkError error, String requestId) {
        logger.warn("P2P 네트워크 오류 발생: {} (요청 ID: {})", error, requestId);

        switch (error.getRetryStrategy()) {
            case RETRY_MAX_3:
                return handleRetry(requestId);

            case RELOGIN_REQUIRED:
                return handleRelogin();

            case SYNC_WITH_SERVER:
                return handleSync();

            case THROTTLING:
                return handleThrottling(requestId);

            case OFFLINE_QUEUING:
                return handleOfflineQueuing(requestId);

            default:
                return ErrorHandlingResult.failure("알 수 없는 오류 처리 전략입니다");
        }
    }

    /**
     * 재전송 처리 (최대 3회)
     */
    private synchronized ErrorHandlingResult handleRetry(String requestId) {
        AtomicInteger retryCount = retryCountMap.computeIfAbsent(requestId, k -> new AtomicInteger(0));
        int currentRetry = retryCount.incrementAndGet();

        if (currentRetry <= MAX_RETRY_COUNT) {
            logger.info("재시도 {}/{} - 요청 ID: {}", currentRetry, MAX_RETRY_COUNT, requestId);
            return ErrorHandlingResult.retry(currentRetry);
        } else {
            logger.error("최대 재시도 횟수 초과 - 요청 ID: {}", requestId);
            retryCountMap.remove(requestId);
            return ErrorHandlingResult.failure("최대 재시도 횟수를 초과했습니다");
        }
    }

    /**
     * 재로그인 요구 처리
     */
    private ErrorHandlingResult handleRelogin() {
        logger.info("재로그인이 필요합니다");
        return ErrorHandlingResult.reloginRequired();
    }

    /**
     * 서버 상태 동기화 처리
     */
    private ErrorHandlingResult handleSync() {
        logger.info("서버 상태와 동기화를 시작합니다");
        return ErrorHandlingResult.syncRequired();
    }

    /**
     * Throttling 강화 처리
     */
    private ErrorHandlingResult handleThrottling(String requestId) {
        logger.info("Throttling 적용 - 요청 ID: {}", requestId);
        return ErrorHandlingResult.throttled();
    }

    /**
     * 오프라인 큐잉 처리
     */
    private ErrorHandlingResult handleOfflineQueuing(String requestId) {
        logger.info("오프라인 큐에 추가 - 요청 ID: {}", requestId);
        return ErrorHandlingResult.queued(requestId);
    }

    /**
     * 요청 성공 시 재시도 카운터 초기화
     */
    public void resetRetryCount(String requestId) {
        retryCountMap.remove(requestId);
    }

    /**
     * 오류 처리 결과
     */
    public static class ErrorHandlingResult {
        private final ErrorHandlingAction action;
        private final String message;
        private final int retryCount;

        private ErrorHandlingResult(ErrorHandlingAction action, String message, int retryCount) {
            this.action = action;
            this.message = message;
            this.retryCount = retryCount;
        }

        public static ErrorHandlingResult retry(int retryCount) {
            return new ErrorHandlingResult(ErrorHandlingAction.RETRY, "재시도합니다", retryCount);
        }

        public static ErrorHandlingResult reloginRequired() {
            return new ErrorHandlingResult(ErrorHandlingAction.RELOGIN, "재로그인이 필요합니다", 0);
        }

        public static ErrorHandlingResult syncRequired() {
            return new ErrorHandlingResult(ErrorHandlingAction.SYNC, "서버 상태와 동기화합니다", 0);
        }

        public static ErrorHandlingResult throttled() {
            return new ErrorHandlingResult(ErrorHandlingAction.THROTTLE, "요청 제한이 적용되었습니다", 0);
        }

        public static ErrorHandlingResult queued(String requestId) {
            return new ErrorHandlingResult(ErrorHandlingAction.QUEUE, "오프라인 큐에 추가되었습니다: " + requestId, 0);
        }

        public static ErrorHandlingResult failure(String message) {
            return new ErrorHandlingResult(ErrorHandlingAction.FAIL, message, 0);
        }

        public ErrorHandlingAction getAction() {
            return action;
        }

        public String getMessage() {
            return message;
        }

        public int getRetryCount() {
            return retryCount;
        }

        public boolean shouldRetry() {
            return action == ErrorHandlingAction.RETRY;
        }

        public boolean requiresRelogin() {
            return action == ErrorHandlingAction.RELOGIN;
        }

        public boolean requiresSync() {
            return action == ErrorHandlingAction.SYNC;
        }
    }

    /**
     * 오류 처리 액션 타입
     */
    public enum ErrorHandlingAction {
        RETRY,      // 재시도
        RELOGIN,    // 재로그인
        SYNC,       // 동기화
        THROTTLE,   // 제한
        QUEUE,      // 큐잉
        FAIL        // 실패
    }
}
