package seoultech.se.backend.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResultDto {
    private Long id;
    private String token;
    private String refreshToken;

    public LoginResultDto(Long id, String token) {
        this.id = id;
        this.token = token;
    }

    public LoginResultDto(Long id, String token, String refreshToken) {
        this.id = id;
        this.token = token;
        this.refreshToken = refreshToken;
    }

    public static LoginResultDto toDto(UserEntity entity, String token) {
        return new LoginResultDto(entity.getId(), token);
    }

    public static LoginResultDto toDto(UserEntity entity, String token, String refreshToken) {
        return new LoginResultDto(entity.getId(), token, refreshToken);
    }
}
