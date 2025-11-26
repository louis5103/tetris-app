package seoultech.se.client.service;

import org.springframework.stereotype.Service;

/**
 * 인증 서비스
 *
 * 책임:
 * - JWT 토큰 관리
 * - 사용자 인증 상태 관리
 *
 * TODO: 실제 인증 서버와 통합 시 확장 필요
 */
@Service
public class AuthService {

    private String currentToken;
    private String currentUserId;

    /**
     * 임시 토큰 생성 (개발용)
     *
     * 실제 환경에서는 인증 서버에서 JWT를 받아와야 합니다.
     *
     * @param userId 사용자 ID
     * @return JWT 토큰
     */
    public String generateTemporaryToken(String userId) {
        this.currentUserId = userId;
        // 임시 토큰 (실제로는 서버에서 받아야 함)
        this.currentToken = "temp_jwt_" + userId + "_" + System.currentTimeMillis();
        System.out.println("✅ [AuthService] Temporary token generated for user: " + userId);
        return this.currentToken;
    }

    /**
     * 현재 토큰 반환
     *
     * @return 현재 JWT 토큰 (없으면 빈 문자열)
     */
    public String getCurrentToken() {
        if (currentToken == null) {
            // 토큰이 없으면 게스트 토큰 생성
            return generateTemporaryToken("guest_" + System.currentTimeMillis());
        }
        return currentToken;
    }

    /**
     * 현재 사용자 ID 반환
     *
     * @return 현재 사용자 ID
     */
    public String getCurrentUserId() {
        return currentUserId != null ? currentUserId : "guest";
    }

    /**
     * 토큰 클리어 (로그아웃)
     */
    public void clearToken() {
        this.currentToken = null;
        this.currentUserId = null;
        System.out.println("🔓 [AuthService] Token cleared");
    }

    /**
     * 토큰 유효성 확인
     *
     * @return 토큰이 유효하면 true
     */
    public boolean isTokenValid() {
        // TODO: 실제 JWT 검증 로직 구현
        return currentToken != null && !currentToken.isEmpty();
    }
}
