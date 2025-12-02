package seoultech.se.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private JwtUtil jwtUtil;
    private final String testEmail = "test@example.com";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // @Value 어노테이션 값을 ReflectionTestUtils로 설정
        ReflectionTestUtils.setField(jwtUtil, "secret",
            "tetris-secret-key-for-p2p-game-mode-authentication-at-least-256-bits");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 24시간
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiration", 604800000L); // 7일
    }

    @Test
    @DisplayName("Access Token 생성 테스트")
    void generateToken() {
        // given & when
        String token = jwtUtil.generateToken(testEmail);

        // then
        assertNotNull(token);
        assertTrue(token.length() > 0);
    }

    @Test
    @DisplayName("Refresh Token 생성 테스트")
    void generateRefreshToken() {
        // given & when
        String refreshToken = jwtUtil.generateRefreshToken(testEmail);

        // then
        assertNotNull(refreshToken);
        assertTrue(refreshToken.length() > 0);
    }

    @Test
    @DisplayName("토큰에서 이메일 추출 테스트")
    void extractEmail() {
        // given
        String token = jwtUtil.generateToken(testEmail);

        // when
        String extractedEmail = jwtUtil.extractEmail(token);

        // then
        assertEquals(testEmail, extractedEmail);
    }

    @Test
    @DisplayName("유효한 토큰 검증 테스트")
    void validateToken_validToken() {
        // given
        String token = jwtUtil.generateToken(testEmail);

        // when
        Boolean isValid = jwtUtil.validateToken(token, testEmail);

        // then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("만료되지 않은 토큰 검증 테스트")
    void validateToken_notExpired() {
        // given
        String token = jwtUtil.generateToken(testEmail);

        // when
        Boolean isValid = jwtUtil.validateToken(token);

        // then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("잘못된 이메일로 토큰 검증 실패 테스트")
    void validateToken_wrongEmail() {
        // given
        String token = jwtUtil.generateToken(testEmail);

        // when
        Boolean isValid = jwtUtil.validateToken(token, "wrong@example.com");

        // then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("잘못된 형식의 토큰 검증 실패 테스트")
    void validateToken_invalidFormat() {
        // given
        String invalidToken = "invalid.token.format";

        // when
        Boolean isValid = jwtUtil.validateToken(invalidToken);

        // then
        assertFalse(isValid);
    }

    @Test
    @DisplayName("Access Token과 Refresh Token이 서로 다른 토큰인지 테스트")
    void accessAndRefreshTokenAreDifferent() {
        // given & when
        String accessToken = jwtUtil.generateToken(testEmail);
        String refreshToken = jwtUtil.generateRefreshToken(testEmail);

        // then
        assertNotEquals(accessToken, refreshToken);
    }

    @Test
    @DisplayName("토큰에서 만료 시간 추출 테스트")
    void extractExpiration() {
        // given
        String token = jwtUtil.generateToken(testEmail);

        // when
        var expirationDate = jwtUtil.extractExpiration(token);

        // then
        assertNotNull(expirationDate);
        assertTrue(expirationDate.getTime() > System.currentTimeMillis());
    }
}
