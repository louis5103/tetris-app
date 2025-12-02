package seoultech.se.backend.settings;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettingRepository extends JpaRepository<SettingEntity, Long>{

    Optional<SettingEntity> findByEmail(String email);
    
}