package seoultech.se.backend.network;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class P2PNetworkErrorTest {

    @Test
    @DisplayName("TIMEOUT 오류 타입 속성 확인")
    void timeout_properties() {
        // given
        P2PNetworkError error = P2PNetworkError.TIMEOUT;

        // then
        assertEquals(HttpStatus.REQUEST_TIMEOUT, error.getHttpStatus());
        assertEquals(408, error.getStatusCode());
        assertEquals("네트워크 타임아웃이 발생했습니다", error.getMessage());
        assertEquals(P2PNetworkError.RetryStrategy.RETRY_MAX_3, error.getRetryStrategy());
        assertEquals("재전송 (최대 3회)", error.getRetryStrategy().getDescription());
    }

    @Test
    @DisplayName("UNAUTHORIZED 오류 타입 속성 확인")
    void unauthorized_properties() {
        // given
        P2PNetworkError error = P2PNetworkError.UNAUTHORIZED;

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, error.getHttpStatus());
        assertEquals(401, error.getStatusCode());
        assertEquals("인증이 만료되었습니다", error.getMessage());
        assertEquals(P2PNetworkError.RetryStrategy.RELOGIN_REQUIRED, error.getRetryStrategy());
        assertEquals("재로그인 요구", error.getRetryStrategy().getDescription());
    }

    @Test
    @DisplayName("STATE_CONFLICT 오류 타입 속성 확인")
    void stateConflict_properties() {
        // given
        P2PNetworkError error = P2PNetworkError.STATE_CONFLICT;

        // then
        assertEquals(HttpStatus.CONFLICT, error.getHttpStatus());
        assertEquals(409, error.getStatusCode());
        assertEquals("게임 상태가 동기화되지 않았습니다", error.getMessage());
        assertEquals(P2PNetworkError.RetryStrategy.SYNC_WITH_SERVER, error.getRetryStrategy());
        assertEquals("서버 상태로 동기화", error.getRetryStrategy().getDescription());
    }

    @Test
    @DisplayName("RATE_LIMIT 오류 타입 속성 확인")
    void rateLimit_properties() {
        // given
        P2PNetworkError error = P2PNetworkError.RATE_LIMIT;

        // then
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, error.getHttpStatus());
        assertEquals(429, error.getStatusCode());
        assertEquals("요청 제한을 초과했습니다", error.getMessage());
        assertEquals(P2PNetworkError.RetryStrategy.THROTTLING, error.getRetryStrategy());
        assertEquals("Throttling 강화", error.getRetryStrategy().getDescription());
    }

    @Test
    @DisplayName("SERVER_ERROR 오류 타입 속성 확인")
    void serverError_properties() {
        // given
        P2PNetworkError error = P2PNetworkError.SERVER_ERROR;

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, error.getHttpStatus());
        assertEquals(500, error.getStatusCode());
        assertEquals("서버 오류가 발생했습니다", error.getMessage());
        assertEquals(P2PNetworkError.RetryStrategy.OFFLINE_QUEUING, error.getRetryStrategy());
        assertEquals("오프라인 큐잉", error.getRetryStrategy().getDescription());
    }

    @Test
    @DisplayName("HTTP 상태 코드로 오류 타입 찾기 - TIMEOUT(408)")
    void fromStatusCode_timeout() {
        // when
        P2PNetworkError error = P2PNetworkError.fromStatusCode(408);

        // then
        assertEquals(P2PNetworkError.TIMEOUT, error);
    }

    @Test
    @DisplayName("HTTP 상태 코드로 오류 타입 찾기 - UNAUTHORIZED(401)")
    void fromStatusCode_unauthorized() {
        // when
        P2PNetworkError error = P2PNetworkError.fromStatusCode(401);

        // then
        assertEquals(P2PNetworkError.UNAUTHORIZED, error);
    }

    @Test
    @DisplayName("HTTP 상태 코드로 오류 타입 찾기 - STATE_CONFLICT(409)")
    void fromStatusCode_stateConflict() {
        // when
        P2PNetworkError error = P2PNetworkError.fromStatusCode(409);

        // then
        assertEquals(P2PNetworkError.STATE_CONFLICT, error);
    }

    @Test
    @DisplayName("HTTP 상태 코드로 오류 타입 찾기 - RATE_LIMIT(429)")
    void fromStatusCode_rateLimit() {
        // when
        P2PNetworkError error = P2PNetworkError.fromStatusCode(429);

        // then
        assertEquals(P2PNetworkError.RATE_LIMIT, error);
    }

    @Test
    @DisplayName("HTTP 상태 코드로 오류 타입 찾기 - SERVER_ERROR(500)")
    void fromStatusCode_serverError() {
        // when
        P2PNetworkError error = P2PNetworkError.fromStatusCode(500);

        // then
        assertEquals(P2PNetworkError.SERVER_ERROR, error);
    }

    @Test
    @DisplayName("매핑되지 않은 상태 코드는 SERVER_ERROR로 기본 반환")
    void fromStatusCode_unmapped_returnsServerError() {
        // when
        P2PNetworkError error404 = P2PNetworkError.fromStatusCode(404);
        P2PNetworkError error503 = P2PNetworkError.fromStatusCode(503);

        // then
        assertEquals(P2PNetworkError.SERVER_ERROR, error404);
        assertEquals(P2PNetworkError.SERVER_ERROR, error503);
    }

    @Test
    @DisplayName("모든 오류 타입이 정의되어 있는지 확인")
    void allErrorTypes_exist() {
        // when
        P2PNetworkError[] errors = P2PNetworkError.values();

        // then
        assertEquals(5, errors.length);
        assertTrue(containsError(errors, P2PNetworkError.TIMEOUT));
        assertTrue(containsError(errors, P2PNetworkError.UNAUTHORIZED));
        assertTrue(containsError(errors, P2PNetworkError.STATE_CONFLICT));
        assertTrue(containsError(errors, P2PNetworkError.RATE_LIMIT));
        assertTrue(containsError(errors, P2PNetworkError.SERVER_ERROR));
    }

    @Test
    @DisplayName("모든 재시도 전략이 정의되어 있는지 확인")
    void allRetryStrategies_exist() {
        // when
        P2PNetworkError.RetryStrategy[] strategies = P2PNetworkError.RetryStrategy.values();

        // then
        assertEquals(5, strategies.length);
        assertTrue(containsStrategy(strategies, P2PNetworkError.RetryStrategy.RETRY_MAX_3));
        assertTrue(containsStrategy(strategies, P2PNetworkError.RetryStrategy.RELOGIN_REQUIRED));
        assertTrue(containsStrategy(strategies, P2PNetworkError.RetryStrategy.SYNC_WITH_SERVER));
        assertTrue(containsStrategy(strategies, P2PNetworkError.RetryStrategy.THROTTLING));
        assertTrue(containsStrategy(strategies, P2PNetworkError.RetryStrategy.OFFLINE_QUEUING));
    }

    private boolean containsError(P2PNetworkError[] errors, P2PNetworkError target) {
        for (P2PNetworkError error : errors) {
            if (error == target) {
                return true;
            }
        }
        return false;
    }

    private boolean containsStrategy(P2PNetworkError.RetryStrategy[] strategies, P2PNetworkError.RetryStrategy target) {
        for (P2PNetworkError.RetryStrategy strategy : strategies) {
            if (strategy == target) {
                return true;
            }
        }
        return false;
    }
}
