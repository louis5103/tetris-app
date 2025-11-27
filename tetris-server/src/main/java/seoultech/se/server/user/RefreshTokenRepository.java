package seoultech.se.server.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {
    Optional<RefreshTokenEntity> findByEmail(String email);
    Optional<RefreshTokenEntity> findByRefreshToken(String refreshToken);
}
