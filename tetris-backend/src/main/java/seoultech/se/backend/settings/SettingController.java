package seoultech.se.backend.settings;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/tetris/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;
    
    /**
     * 새로운 설정값 저장
     */
    @PostMapping()
    public ResponseEntity<SettingResponseDto> saveSettings(@RequestBody SettingRequestDto newSetting) {
        SettingResponseDto savedSetting = settingService.saveSetting(newSetting);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSetting);
    }

    @GetMapping()
    public ResponseEntity<SettingResponseDto> getSettings(@RequestParam String email) {
        SettingResponseDto dto = settingService.getSettings(email);
        return ResponseEntity.ok(dto);
    }

    @PatchMapping("/{email}")
    public ResponseEntity<Void> updateSettings(@PathVariable String email, @RequestBody SettingsUpdateDto dto) {
        settingService.updateSettings(email, dto);
        return ResponseEntity.ok().build();
    }

}
