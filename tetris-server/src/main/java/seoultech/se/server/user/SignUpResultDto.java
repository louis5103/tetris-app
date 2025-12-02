package seoultech.se.server.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class SignUpResultDto {

    private Long id;
    private String name;
    private String email;

    public static SignUpResultDto toDto(UserEntity entity) {
        return new SignUpResultDto(entity.getId(), entity.getName(), entity.getEmail());
    }

}
