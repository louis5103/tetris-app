package seoultech.se.client.service;

import org.springframework.stereotype.Service;

/**
 * 인증 서비스
 *
 * 책임:
 * - JWT 토큰 관리 (Access Token, Refresh Token)
 * - 사용자 인증 상태 관리
 * - 현재 로그인된 사용자 정보 저장
 */
@Service
public class AuthService {

    private String accessToken;
    private String refreshToken;
    private Long currentUserId;
    private String currentUserEmail;
    private String currentUserName;

    /**
     * 로그인 성공 시 사용자 정보 및 토큰 저장
     *
     * @param userId 사용자 ID
     * @param email 사용자 이메일
     * @param name 사용자 이름
     * @param token Access Token
     * @param refreshToken Refresh Token
     */
    public void setAuthenticatedUser(Long userId, String email, String name, String token, String refreshToken) {
        this.currentUserId = userId;
        this.currentUserEmail = email;
        this.currentUserName = name;
        this.accessToken = token;
        this.refreshToken = refreshToken;

        System.out.println("✅ [AuthService] User authenticated");
        System.out.println("   - User ID: " + userId);
        System.out.println("   - Email: " + email);
        System.out.println("   - Name: " + name);
        System.out.println("   - Token: " + (token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
    }

    /**
     * 임시 토큰 생성 (게스트/개발용)
     *
     * @param userId 사용자 ID
     * @return JWT 토큰
     */
    public String generateTemporaryToken(String userId) {
        this.accessToken = "temp_jwt_" + userId + "_" + System.currentTimeMillis();
        System.out.println("✅ [AuthService] Temporary token generated for user: " + userId);
        return this.accessToken;
    }

    /**
     * 현재 Access Token 반환
     *
     * @return 현재 JWT Access Token (없으면 null 반환)
     */
    public String getCurrentToken() {
        return accessToken;
    }

    /**
     * 현재 Refresh Token 반환
     *
     * @return 현재 Refresh Token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * 현재 사용자 ID 반환
     *
     * @return 현재 사용자 ID
     */
    public Long getCurrentUserId() {
        return currentUserId;
    }

    /**
     * 현재 사용자 ID를 문자열로 반환
     *
     * @return 현재 사용자 ID 문자열
     */
    public String getCurrentUserIdString() {
        return currentUserId != null ? String.valueOf(currentUserId) : "guest";
    }

    /**
     * 현재 사용자 이메일 반환
     *
     * @return 현재 사용자 이메일
     */
    public String getCurrentUserEmail() {
        return currentUserEmail;
    }

    /**
     * 현재 사용자 이름 반환
     *
     * @return 현재 사용자 이름
     */
    public String getCurrentUserName() {
        return currentUserName;
    }

    /**
     * 로그인 여부 확인
     *
     * @return 로그인되어 있으면 true
     */
    public boolean isAuthenticated() {
        return currentUserId != null && accessToken != null && !accessToken.isEmpty();
    }

    /**
     * 토큰 유효성 확인
     *
     * @return 토큰이 유효하면 true
     */
    public boolean isTokenValid() {
        // TODO: 실제 JWT 검증 로직 구현 (토큰 만료 시간 확인 등)
        return accessToken != null && !accessToken.isEmpty();
    }

    /**
     * 로그아웃 (토큰 및 사용자 정보 클리어)
     */
    public void clearAuth() {
        this.accessToken = null;
        this.refreshToken = null;
        this.currentUserId = null;
        this.currentUserEmail = null;
        this.currentUserName = null;
        System.out.println("🔓 [AuthService] Auth cleared (logged out)");
    }

    /**
     * Access Token 갱신
     *
     * @param newAccessToken 새로운 Access Token
     */
    public void updateAccessToken(String newAccessToken) {
        this.accessToken = newAccessToken;
        System.out.println("🔄 [AuthService] Access token updated");
    }
}
