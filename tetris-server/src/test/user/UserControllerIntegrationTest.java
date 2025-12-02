package seoultech.se.backend.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("회원가입, 로그인, 보호 API 인증, 토큰 재발급 통합 테스트")
    void authFlowTest() throws Exception {
        String uniqueEmail = "testuser" + System.currentTimeMillis() + "@example.com";

        // 회원가입
        SignUpRequestDto signUp = new SignUpRequestDto();
        java.lang.reflect.Field nameField = SignUpRequestDto.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(signUp, "testuser");
        java.lang.reflect.Field emailField = SignUpRequestDto.class.getDeclaredField("email");
        emailField.setAccessible(true);
        emailField.set(signUp, uniqueEmail);
        java.lang.reflect.Field passwordField = SignUpRequestDto.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        passwordField.set(signUp, "testpassword123");

        mockMvc.perform(post("/tetris/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUp)))
                .andExpect(status().isCreated());

        // 로그인
        LoginRequestDto login = new LoginRequestDto();
        login.setEmail(uniqueEmail);
        login.setPassword("testpassword123");

        ResultActions loginResult = mockMvc.perform(post("/tetris/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.refreshToken").exists());

        String accessToken = loginResult.andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(accessToken).get("token").asText();
        String refreshToken = objectMapper.readTree(accessToken).get("refreshToken").asText();

        // Access Token 재발급 테스트 (RefreshTokenRequestDto 사용)
        RefreshTokenRequestDto refreshDto = new RefreshTokenRequestDto();
        refreshDto.setRefreshToken(refreshToken);

        mockMvc.perform(post("/tetris/users/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());

        // 보호 API 접근 (로그아웃) - SecurityContext에서 이메일 자동 추출
        mockMvc.perform(post("/tetris/users/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("잘못된 비밀번호로 로그인 실패 테스트")
    void loginWithWrongPassword() throws Exception {
        String uniqueEmail = "testuser2" + System.currentTimeMillis() + "@example.com";

        // 먼저 회원가입
        SignUpRequestDto signUp = new SignUpRequestDto();
        java.lang.reflect.Field nameField = SignUpRequestDto.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(signUp, "testuser2");
        java.lang.reflect.Field emailField = SignUpRequestDto.class.getDeclaredField("email");
        emailField.setAccessible(true);
        emailField.set(signUp, uniqueEmail);
        java.lang.reflect.Field passwordField = SignUpRequestDto.class.getDeclaredField("password");
        passwordField.setAccessible(true);
        passwordField.set(signUp, "correctpassword");

        mockMvc.perform(post("/tetris/users/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUp)))
                .andExpect(status().isCreated());

        // 잘못된 비밀번호로 로그인 시도
        LoginRequestDto login = new LoginRequestDto();
        login.setEmail(uniqueEmail);
        login.setPassword("wrongpassword");

        mockMvc.perform(post("/tetris/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("존재하지 않는 사용자 로그인 실패 테스트")
    void loginWithNonExistentUser() throws Exception {
        LoginRequestDto login = new LoginRequestDto();
        login.setEmail("nonexistent@example.com");
        login.setPassword("anypassword");

        mockMvc.perform(post("/tetris/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("유효하지 않은 Refresh Token으로 재발급 실패 테스트")
    void reissueWithInvalidRefreshToken() throws Exception {
        RefreshTokenRequestDto refreshDto = new RefreshTokenRequestDto();
        refreshDto.setRefreshToken("invalid.refresh.token");

        mockMvc.perform(post("/tetris/users/reissue")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("JWT 토큰 없이 보호된 API 접근 실패 테스트")
    void accessProtectedApiWithoutToken() throws Exception {
        mockMvc.perform(post("/tetris/users/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("잘못된 JWT 토큰으로 보호된 API 접근 실패 테스트")
    void accessProtectedApiWithInvalidToken() throws Exception {
        mockMvc.perform(post("/tetris/users/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isForbidden());
    }
}
