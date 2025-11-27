package seoultech.se.server.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/tetris/users")
@RequiredArgsConstructor
public class UserController { 
    private final UserService userService;

    /**
     * 회원가입
     */
    @PostMapping("/signup")
    public ResponseEntity<SignUpResultDto> signUp(@Valid @RequestBody SignUpRequestDto newUser) {
        SignUpResultDto dto = userService.signUp(newUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResultDto> login(@RequestBody LoginRequestDto dto) {
        LoginResultDto loginResult = userService.login(dto);
        // JWT 토큰을 Authorization 헤더에도 추가 (옵션)
        return ResponseEntity.ok()
                .header("Authorization", "Bearer " + loginResult.getToken())
                .body(loginResult);
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // SecurityContext에서 인증된 사용자 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = (String) authentication.getPrincipal();

        String message = userService.logout(email);
        return ResponseEntity.ok(message);
    }

    /**
     * Refresh Token으로 Access Token 재발급
     */
    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(@Valid @RequestBody RefreshTokenRequestDto dto) {
        try {
            String newAccessToken = userService.reissueAccessToken(dto.getRefreshToken());

            // 응답 구조화
            java.util.Map<String, String> response = new java.util.HashMap<>();
            response.put("accessToken", newAccessToken);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * IllegalArgumentException 전역 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }

}
