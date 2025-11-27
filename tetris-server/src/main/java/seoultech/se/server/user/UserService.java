package seoultech.se.server.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;


/** TO STUDY
 * 1. PasswordEncoder
 * 2. throw & catch
 * 3. IllegalArgumentExcption
 * 4. Builder
 */

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final seoultech.se.server.config.JwtUtil jwtUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public SignUpResultDto signUp(SignUpRequestDto dto) {

        // Validate email
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 사용중인 이메일입니다.");
        }

        // Validate Password
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        // Dto to Entity
        UserEntity newUser = UserEntity.builder().name(dto.getName()).email(dto.getEmail()).password(encodedPassword).build();

        // save
        UserEntity savedUser = userRepository.save(newUser);
        
        // Entitiy to ResultDto
        SignUpResultDto result = SignUpResultDto.toDto(savedUser);

        return result;
    }

    @Transactional
    public LoginResultDto login(LoginRequestDto requestDto) {
        // 요청된 email이 있는지 확인
        UserEntity user = userRepository.findByEmail(
            requestDto.getEmail()).orElseThrow(() -> new IllegalArgumentException("사용자가 없습니다."));

        // 해당 email과 비밀번호 일치하는지 확인
        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 틀립니다.");
        }

        user.login();

        // Access, Refresh Token 생성
        String accessToken = jwtUtil.generateToken(user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getEmail());

        // Refresh Token 저장 (upsert)
        refreshTokenRepository.findByEmail(user.getEmail())
            .ifPresentOrElse(
                entity -> { entity.setRefreshToken(refreshToken); refreshTokenRepository.save(entity); },
                () -> {
                    RefreshTokenEntity entity = new RefreshTokenEntity();
                    entity.setEmail(user.getEmail());
                    entity.setRefreshToken(refreshToken);
                    refreshTokenRepository.save(entity);
                }
            );

        return LoginResultDto.toDto(user, accessToken, refreshToken);
    }

    // Refresh Token으로 Access Token 재발급
    public String reissueAccessToken(String refreshToken) {
        RefreshTokenEntity entity = refreshTokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다."));
        // refreshToken 유효성 검사
        if (!jwtUtil.validateToken(refreshToken, entity.getEmail())) {
            throw new IllegalArgumentException("만료되었거나 유효하지 않은 리프레시 토큰입니다.");
        }
        return jwtUtil.generateToken(entity.getEmail());
    }

    @Transactional
    public String logout(String email) {
        // 요청된 email이 있는지 확인
        UserEntity user = userRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("사용자가 없습니다."));

        user.logout();

        // Refresh Token 삭제 (중요!)
        refreshTokenRepository.findByEmail(email).ifPresent(refreshTokenRepository::delete);

        return new String("로그아웃 성공");
    }
    
}
