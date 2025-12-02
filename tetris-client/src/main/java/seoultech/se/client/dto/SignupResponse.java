package seoultech.se.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 회원가입 응답 DTO
 * 서버의 SignUpResultDto와 매핑됩니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SignupResponse {
    private Long id;
    private String name;
    private String email;
}
