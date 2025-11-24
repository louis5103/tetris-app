package seoultech.se.backend.network;

import org.springframework.http.HttpStatus;

/**
 * Tetris P2P 모드 네트워크 오류 정의
 *
 * 각 오류 타입별로 HTTP 상태 코드와 처리 방식을 정의합니다.
 */
public enum P2PNetworkError {

    /**
     * 타임아웃 오류 (408 Request Timeout)
     * 처리방식: 재전송 (최대 3회)
     */
    TIMEOUT(HttpStatus.REQUEST_TIMEOUT, "네트워크 타임아웃이 발생했습니다", RetryStrategy.RETRY_MAX_3),

    /**
     * 인증 오류 (401 Unauthorized)
     * 처리방식: 재로그인 요구
     */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 만료되었습니다", RetryStrategy.RELOGIN_REQUIRED),

    /**
     * 상태 충돌 오류 (409 Conflict)
     * 처리방식: 서버 상태로 동기화
     */
    STATE_CONFLICT(HttpStatus.CONFLICT, "게임 상태가 동기화되지 않았습니다", RetryStrategy.SYNC_WITH_SERVER),

    /**
     * 속도 제한 오류 (429 Too Many Requests)
     * 처리방식: Throttling 강화
     */
    RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "요청 제한을 초과했습니다", RetryStrategy.THROTTLING),

    /**
     * 서버 오류 (500 Internal Server Error)
     * 처리방식: 오프라인 큐잉
     */
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다", RetryStrategy.OFFLINE_QUEUING);

    private final HttpStatus httpStatus;
    private final String message;
    private final RetryStrategy retryStrategy;

    P2PNetworkError(HttpStatus httpStatus, String message, RetryStrategy retryStrategy) {
        this.httpStatus = httpStatus;
        this.message = message;
        this.retryStrategy = retryStrategy;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getStatusCode() {
        return httpStatus.value();
    }

    public String getMessage() {
        return message;
    }

    public RetryStrategy getRetryStrategy() {
        return retryStrategy;
    }

    /**
     * HTTP 상태 코드로 해당하는 오류 타입을 찾습니다
     */
    public static P2PNetworkError fromStatusCode(int statusCode) {
        for (P2PNetworkError error : values()) {
            if (error.getStatusCode() == statusCode) {
                return error;
            }
        }
        return SERVER_ERROR; // 기본값
    }

    /**
     * 재시도 전략
     */
    public enum RetryStrategy {
        RETRY_MAX_3("재전송 (최대 3회)"),
        RELOGIN_REQUIRED("재로그인 요구"),
        SYNC_WITH_SERVER("서버 상태로 동기화"),
        THROTTLING("Throttling 강화"),
        OFFLINE_QUEUING("오프라인 큐잉");

        private final String description;

        RetryStrategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
