package seoultech.se.backend.settings;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SettingService {
    
    private final SettingRepository settingRepository;

    @Transactional
    public SettingResponseDto saveSetting(SettingRequestDto requestDto) {
        // 1. email 중복 검사 
        if (settingRepository.findByEmail(requestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 email입니다.");
        }

        // 2. RequestDto -> Entity 변환
        SettingEntity entity = fromDtoToEntity(requestDto);
      
        // 3. 저장
        settingRepository.save(entity);

        return fromEntityToDto(entity);
    }

    @Transactional
    public SettingResponseDto getSettings(String email) {

        // 1. email에 해당하는 setting이 있는지 확인
        SettingEntity entity = settingRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        // 2. entity -> responseDto 변환
        SettingResponseDto dto = fromEntityToDto(entity);
        
        return dto;
    }

    @Transactional
    public void updateSettings(String email, SettingsUpdateDto dto) {
        SettingEntity entity = settingRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."));

        if (dto.getGameLevel() != null) {
            entity.setGameLevel(dto.getGameLevel());
        }
      
        if (dto.getScreenSize() != null) { 
            entity.setScreenSize(dto.getScreenSize());
        }

        if (dto.getColorMode() != null) {
            entity.setColorMode(dto.getColorMode());
        }

        if (dto.getLeftKey() != null) {
            entity.setLeftKey(dto.getLeftKey());
        }

        if (dto.getRightKey() != null) {
            entity.setRightKey(dto.getRightKey());
        }

        if (dto.getDownKey() != null) {
            entity.setDownKey(dto.getDownKey());
        }

        if (dto.getHardDownKey() != null) {
            entity.setHardDownKey(dto.getHardDownKey());
        }

        if (dto.getRotateKey() != null) {
            entity.setRotateKey(dto.getRotateKey());
        }

        if (dto.getMusicVolume() != null) {
            entity.setMusicVolume(dto.getMusicVolume());
        }

        settingRepository.save(entity);
    }

    private SettingEntity fromDtoToEntity(SettingRequestDto dto) {
        SettingEntity entity = new SettingEntity();

        entity.setGameLevel(dto.getGameLevel());
        entity.setSettingsName(dto.getSettingsName());
        entity.setEmail(dto.getEmail());
        entity.setScreenSize(dto.getScreenSize());
        entity.setColorMode(dto.getColorMode());
        entity.setLeftKey(dto.getLeftKey());
        entity.setRightKey(dto.getRightKey());
        entity.setDownKey(dto.getDownKey());
        entity.setHardDownKey(dto.getHardDownKey());
        entity.setRotateKey(dto.getRotateKey());
        entity.setMusicVolume(dto.getMusicVolume());
        return entity;
    }

    private SettingResponseDto fromEntityToDto(SettingEntity entity) {
        SettingResponseDto dto = new SettingResponseDto();

        dto.setGameLevel(entity.getGameLevel());
        dto.setSettingsName(entity.getSettingsName());
        dto.setScreenSize(entity.getScreenSize());
        dto.setColorMode(entity.getColorMode());
        dto.setLeftKey(entity.getLeftKey());
        dto.setRightKey(entity.getRightKey());
        dto.setDownKey(entity.getDownKey());
        dto.setHardDownKey(entity.getHardDownKey());
        dto.setRotateKey(entity.getRotateKey());
        dto.setMusicVolume(entity.getMusicVolume());

        return dto;
    }

}
