package seoultech.se.server.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequestDto {
    //--- property ---//
    @NotBlank(message = "이름을 입력하세요.")
    @Size(max = 20, message = "이름은 20자 이하여야 합니다.")
    private String name;

    @NotBlank(message = "이메일을 입력하세요.")
    @Email
    private String email;

    @NotBlank(message = "비밀번호를 입력하세요.")
    @Size(min = 8, message = "비밀번호는 8자리 이상이어야 합니다.")
    private String password;

    //--- Method ---//
    public UserEntity toEntity() {
        return UserEntity.builder()
            .name(this.name)
            .email(this.email)
            .password(this.password).build();
    }
    
}
