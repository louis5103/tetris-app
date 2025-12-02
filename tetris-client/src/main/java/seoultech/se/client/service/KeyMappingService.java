package seoultech.se.client.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.prefs.Preferences;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javafx.scene.input.KeyCode;
import seoultech.se.client.model.GameAction;

/**
 * 키 매핑 관리 서비스
 * 
 * 사용자별 키 설정을 관리하고 저장하는 서비스입니다.
 * 
 * 주요 기능:
 * 1. 키보드 입력(KeyCode) → 게임 액션(GameAction) 변환
 * 2. 사용자 정의 키 매핑 설정
 * 3. 영구 저장 (Java Preferences API 사용)
 * 4. 기본 키 매핑 제공
 * 
 * 멀티플레이어 시나리오:
 * - 각 클라이언트가 독립적인 키 설정 사용
 * - 서버는 키 설정을 알 필요 없음 (Command만 받음)
 * - Player A: WASD 사용 → MoveCommand
 * - Player B: 화살표 사용 → MoveCommand
 * - 서버는 동일한 Command 처리
 */
@Service
public class KeyMappingService {
    private static final String PREFS_NODE = "tetris_key_mappings";
    private final Preferences preferences;
    
    // GameAction → KeyCode 매핑 (설정 및 조회용)
    private final Map<GameAction, KeyCode> actionToKey;
    
    // KeyCode → GameAction 매핑 (빠른 조회용)
    private final Map<KeyCode, GameAction> keyToAction;
    
    public KeyMappingService() {
        this.preferences = Preferences.userRoot().node(PREFS_NODE);
        this.actionToKey = new HashMap<>();
        this.keyToAction = new HashMap<>();
    }
    
    /**
     * 초기화: 저장된 키 매핑을 로드하거나 기본값 설정
     */
    @PostConstruct
    public void init() {
        loadMappings();
    }
    
    /**
     * 키보드 입력을 게임 액션으로 변환
     */
    public Optional<GameAction> getAction(KeyCode keyCode) {
        return Optional.ofNullable(keyToAction.get(keyCode));
    }
    
    /**
     * 게임 액션에 매핑된 키 조회
     */
    public Optional<KeyCode> getKey(GameAction action) {
        return Optional.ofNullable(actionToKey.get(action));
    }
    
    /**
     * 키 매핑 설정
     */
    public boolean setKeyMapping(GameAction action, KeyCode keyCode) {
        // 해당 키가 이미 다른 액션에 사용 중인지 확인
        if (keyToAction.containsKey(keyCode)) {
            GameAction existingAction = keyToAction.get(keyCode);
            if (existingAction != action) {
                // 기존 매핑 제거
                actionToKey.remove(existingAction);
            }
        }
        
        // 기존 매핑 제거 (액션이 다른 키에 매핑되어 있었다면)
        KeyCode oldKey = actionToKey.get(action);
        if (oldKey != null) {
            keyToAction.remove(oldKey);
        }
        
        // 새 매핑 설정
        actionToKey.put(action, keyCode);
        keyToAction.put(keyCode, action);
        
        // 영구 저장
        saveMappings();
        
        return true;
    }
    
    /**
     * 기본 키 매핑으로 리셋
     */
    public void resetToDefault() {
        actionToKey.clear();
        keyToAction.clear();
        setDefaultMappings();
        saveMappings();
    }
    
    /**
     * 모든 키 매핑 조회
     */
    public Map<GameAction, KeyCode> getAllMappings() {
        return Map.copyOf(actionToKey);
    }
    
    /**
     * 기본 키 매핑 설정
     */
    private void setDefaultMappings() {
        setKeyMapping(GameAction.MOVE_LEFT, KeyCode.LEFT);
        setKeyMapping(GameAction.MOVE_RIGHT, KeyCode.RIGHT);
        setKeyMapping(GameAction.MOVE_DOWN, KeyCode.DOWN);
        setKeyMapping(GameAction.ROTATE_CLOCKWISE, KeyCode.UP);
        setKeyMapping(GameAction.ROTATE_COUNTER_CLOCKWISE, KeyCode.Z);
        setKeyMapping(GameAction.HARD_DROP, KeyCode.SPACE);
        setKeyMapping(GameAction.HOLD, KeyCode.C);
        setKeyMapping(GameAction.PAUSE_RESUME, KeyCode.P);
    }
    
    /**
     * 저장된 매핑 로드
     */
    private void loadMappings() {
        boolean hasStoredMappings = false;
        
        for (GameAction action : GameAction.values()) {
            String keyName = preferences.get(action.name(), null);
            if (keyName != null) {
                try {
                    KeyCode keyCode = KeyCode.valueOf(keyName);
                    actionToKey.put(action, keyCode);
                    keyToAction.put(keyCode, action);
                    hasStoredMappings = true;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid key name in preferences: " + keyName);
                }
            }
        }
        
        if (!hasStoredMappings) {
            setDefaultMappings();
        }
    }
    
    /**
     * 현재 매핑 저장
     */
    private void saveMappings() {
        for (Map.Entry<GameAction, KeyCode> entry : actionToKey.entrySet()) {
            preferences.put(entry.getKey().name(), entry.getValue().name());
        }
    }
    
    /**
     * 디버그용: 현재 매핑 출력
     */
    public void printCurrentMappings() {
        System.out.println("🎮 Current Key Mappings:");
        for (GameAction action : GameAction.values()) {
            KeyCode key = actionToKey.get(action);
            System.out.println("  " + action + " → " + (key != null ? key : "UNASSIGNED"));
        }
    }
}
