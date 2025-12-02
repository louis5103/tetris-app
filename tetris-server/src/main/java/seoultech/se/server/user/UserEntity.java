package seoultech.se.server.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//TODO: Builder 공부하기 

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false)
    private Long id;

    @Column(name = "name")
    @NotBlank
    @Size(max = 20)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "login-status")
    @Enumerated(EnumType.STRING)
    private LoginStatus status = LoginStatus.LOGOUT;

    @Builder
    public UserEntity(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    //TODO: 중간평가 이후 수정 및 JWT & Security 도입 예정
    public void login() {
        this.status = LoginStatus.LOGIN;
    }
    //TODO: 중간평가 이후 수정 및 JWT & Security 도입 예정
    public void logout() {
        this.status = LoginStatus.LOGOUT;
    }
}
