package seoultech.se.client.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import seoultech.se.client.dto.LoginRequest;
import seoultech.se.client.dto.LoginResponse;
import seoultech.se.client.dto.SignupRequest;
import seoultech.se.client.dto.SignupResponse;

/**
 * 사용자 인증 API 서비스
 *
 * 책임:
 * - 서버와의 HTTP 통신 (로그인, 회원가입, 로그아웃)
 * - 요청/응답 DTO 변환
 * - 에러 처리 및 변환
 */
@Service
public class UserApiService {

    private final RestTemplate restTemplate;
    private final String authBaseUrl;
    private final String userEndpoint;

    public UserApiService(
            @Value("${tetris.auth.base-url}") String authBaseUrl,
            @Value("${tetris.auth.user-endpoint}") String userEndpoint) {
        this.restTemplate = new RestTemplate();
        this.authBaseUrl = authBaseUrl;
        this.userEndpoint = userEndpoint;
    }

    /**
     * 회원가입
     *
     * @param request 회원가입 요청 데이터
     * @return 회원가입 응답 데이터
     * @throws ApiException 서버 통신 실패 시
     */
    public SignupResponse signup(SignupRequest request) throws ApiException {
        String url = authBaseUrl + userEndpoint + "/signup";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<SignupRequest> entity = new HttpEntity<>(request, headers);

            System.out.println("📡 [UserApiService] Calling signup API: " + url);
            System.out.println("   - Email: " + request.getEmail());
            System.out.println("   - Name: " + request.getName());

            ResponseEntity<SignupResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                SignupResponse.class
            );

            System.out.println("✅ [UserApiService] Signup successful");
            return response.getBody();

        } catch (HttpClientErrorException e) {
            // 4xx 에러 (잘못된 요청, 중복 이메일 등)
            System.err.println("❌ [UserApiService] Signup failed (client error): " + e.getStatusCode());
            throw new ApiException("회원가입 실패: " + extractErrorMessage(e.getResponseBodyAsString()), e);

        } catch (HttpServerErrorException e) {
            // 5xx 에러 (서버 오류)
            System.err.println("❌ [UserApiService] Signup failed (server error): " + e.getStatusCode());
            throw new ApiException("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", e);

        } catch (ResourceAccessException e) {
            // 네트워크 연결 실패
            System.err.println("❌ [UserApiService] Signup failed (network error): " + e.getMessage());
            throw new ApiException("서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.", e);

        } catch (Exception e) {
            // 기타 예외
            System.err.println("❌ [UserApiService] Signup failed (unexpected error): " + e.getMessage());
            e.printStackTrace();
            throw new ApiException("예상치 못한 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 로그인
     *
     * @param request 로그인 요청 데이터
     * @return 로그인 응답 데이터 (JWT 토큰 포함)
     * @throws ApiException 서버 통신 실패 시
     */
    public LoginResponse login(LoginRequest request) throws ApiException {
        String url = authBaseUrl + userEndpoint + "/login";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<LoginRequest> entity = new HttpEntity<>(request, headers);

            System.out.println("📡 [UserApiService] Calling login API: " + url);
            System.out.println("   - Email: " + request.getEmail());

            ResponseEntity<LoginResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                LoginResponse.class
            );

            System.out.println("✅ [UserApiService] Login successful");
            System.out.println("   - User ID: " + response.getBody().getId());
            return response.getBody();

        } catch (HttpClientErrorException e) {
            // 4xx 에러 (잘못된 이메일/비밀번호 등)
            System.err.println("❌ [UserApiService] Login failed (client error): " + e.getStatusCode());

            if (e.getStatusCode().value() == 401) {
                throw new ApiException("이메일 또는 비밀번호가 올바르지 않습니다.", e);
            }
            throw new ApiException("로그인 실패: " + extractErrorMessage(e.getResponseBodyAsString()), e);

        } catch (HttpServerErrorException e) {
            // 5xx 에러 (서버 오류)
            System.err.println("❌ [UserApiService] Login failed (server error): " + e.getStatusCode());
            throw new ApiException("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", e);

        } catch (ResourceAccessException e) {
            // 네트워크 연결 실패
            System.err.println("❌ [UserApiService] Login failed (network error): " + e.getMessage());
            throw new ApiException("서버에 연결할 수 없습니다. 네트워크 연결을 확인해주세요.", e);

        } catch (Exception e) {
            // 기타 예외
            System.err.println("❌ [UserApiService] Login failed (unexpected error): " + e.getMessage());
            e.printStackTrace();
            throw new ApiException("예상치 못한 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 로그아웃
     *
     * @param token JWT 토큰
     * @throws ApiException 서버 통신 실패 시
     */
    public void logout(String token) throws ApiException {
        String url = authBaseUrl + userEndpoint + "/logout";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            System.out.println("📡 [UserApiService] Calling logout API: " + url);

            restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );

            System.out.println("✅ [UserApiService] Logout successful");

        } catch (Exception e) {
            System.err.println("❌ [UserApiService] Logout failed: " + e.getMessage());
            // 로그아웃 실패는 치명적이지 않으므로 예외를 던지지 않음
            // 대신 로컬에서 토큰만 삭제하면 됨
        }
    }

    /**
     * 에러 응답 본문에서 에러 메시지 추출
     *
     * @param responseBody 응답 본문
     * @return 추출된 에러 메시지
     */
    private String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return "알 수 없는 오류";
        }

        // 간단한 메시지 추출 (필요시 JSON 파싱으로 개선 가능)
        if (responseBody.length() > 100) {
            return responseBody.substring(0, 100) + "...";
        }

        return responseBody;
    }

    /**
     * API 예외 클래스
     */
    public static class ApiException extends Exception {
        public ApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
