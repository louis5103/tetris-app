package seoultech.se.backend.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import seoultech.se.backend.config.JwtUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserService userService;

    private UserEntity testUser;
    private SignUpRequestDto signUpDto;
    private LoginRequestDto loginDto;

    @BeforeEach
    void setUp() {
        testUser = UserEntity.builder()
                .name("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .build();

        // Reflection을 사용해서 id 설정 (테스트용)
        try {
            java.lang.reflect.Field idField = UserEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testUser, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        signUpDto = new SignUpRequestDto();
        try {
            java.lang.reflect.Field nameField = SignUpRequestDto.class.getDeclaredField("name");
            nameField.setAccessible(true);
            nameField.set(signUpDto, "testuser");

            java.lang.reflect.Field emailField = SignUpRequestDto.class.getDeclaredField("email");
            emailField.setAccessible(true);
            emailField.set(signUpDto, "test@example.com");

            java.lang.reflect.Field passwordField = SignUpRequestDto.class.getDeclaredField("password");
            passwordField.setAccessible(true);
            passwordField.set(signUpDto, "password123");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        loginDto = new LoginRequestDto();
        loginDto.setEmail("test@example.com");
        loginDto.setPassword("password123");
    }

    @Test
    @DisplayName("회원가입 성공 테스트")
    void signUp_success() {
        // given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenReturn(testUser);

        // when
        SignUpResultDto result = userService.signUp(signUpDto);

        // then
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository, times(1)).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("중복 이메일로 회원가입 실패 테스트")
    void signUp_duplicateEmail() {
        // given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // when & then
        assertThrows(IllegalArgumentException.class, () -> userService.signUp(signUpDto));
        verify(userRepository, never()).save(any(UserEntity.class));
    }

    @Test
    @DisplayName("로그인 성공 및 토큰 생성 테스트")
    void login_success() {
        // given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtUtil.generateToken(anyString())).thenReturn("access.token.here");
        when(jwtUtil.generateRefreshToken(anyString())).thenReturn("refresh.token.here");
        when(refreshTokenRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenReturn(new RefreshTokenEntity());

        // when
        LoginResultDto result = userService.login(loginDto);

        // then
        assertNotNull(result);
        assertNotNull(result.getToken());
        assertNotNull(result.getRefreshToken());
        verify(jwtUtil, times(1)).generateToken(anyString());
        verify(jwtUtil, times(1)).generateRefreshToken(anyString());
        verify(refreshTokenRepository, times(1)).save(any(RefreshTokenEntity.class));
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 실패 테스트")
    void login_wrongPassword() {
        // given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class, () -> userService.login(loginDto));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 로그인 실패 테스트")
    void login_userNotFound() {
        // given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () -> userService.login(loginDto));
    }

    @Test
    @DisplayName("로그아웃 성공 및 Refresh Token 삭제 테스트")
    void logout_success() {
        // given
        String email = "test@example.com";
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setEmail(email);
        refreshTokenEntity.setRefreshToken("refresh.token.here");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(refreshTokenRepository.findByEmail(email)).thenReturn(Optional.of(refreshTokenEntity));

        // when
        String result = userService.logout(email);

        // then
        assertEquals("로그아웃 성공", result);
        verify(refreshTokenRepository, times(1)).delete(refreshTokenEntity);
    }

    @Test
    @DisplayName("유효한 Refresh Token으로 Access Token 재발급 테스트")
    void reissueAccessToken_success() {
        // given
        String refreshToken = "valid.refresh.token";
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setEmail("test@example.com");
        refreshTokenEntity.setRefreshToken(refreshToken);

        when(refreshTokenRepository.findByRefreshToken(refreshToken))
                .thenReturn(Optional.of(refreshTokenEntity));
        when(jwtUtil.validateToken(refreshToken, "test@example.com")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com")).thenReturn("new.access.token");

        // when
        String newToken = userService.reissueAccessToken(refreshToken);

        // then
        assertNotNull(newToken);
        assertEquals("new.access.token", newToken);
        verify(jwtUtil, times(1)).generateToken("test@example.com");
    }

    @Test
    @DisplayName("유효하지 않은 Refresh Token으로 재발급 실패 테스트")
    void reissueAccessToken_invalidToken() {
        // given
        String invalidToken = "invalid.refresh.token";
        when(refreshTokenRepository.findByRefreshToken(invalidToken)).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> userService.reissueAccessToken(invalidToken));
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("만료된 Refresh Token으로 재발급 실패 테스트")
    void reissueAccessToken_expiredToken() {
        // given
        String expiredToken = "expired.refresh.token";
        RefreshTokenEntity refreshTokenEntity = new RefreshTokenEntity();
        refreshTokenEntity.setEmail("test@example.com");
        refreshTokenEntity.setRefreshToken(expiredToken);

        when(refreshTokenRepository.findByRefreshToken(expiredToken))
                .thenReturn(Optional.of(refreshTokenEntity));
        when(jwtUtil.validateToken(expiredToken, "test@example.com")).thenReturn(false);

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> userService.reissueAccessToken(expiredToken));
        verify(jwtUtil, never()).generateToken(anyString());
    }
}
