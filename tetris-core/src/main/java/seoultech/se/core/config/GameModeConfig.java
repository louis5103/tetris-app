package seoultech.se.core.config;

import java.util.HashMap;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;
import seoultech.se.core.item.ItemConfig;

/**
 * 게임 모드 설정 객체
 * 
 * 모든 게임 설정을 담는 불변 객체입니다.
 * Builder 패턴을 사용하여 유연하게 설정을 구성할 수 있습니다.
 * 
 * 설계 원칙:
 * - 불변성(Immutability): 생성 후 수정 불가
 * - 프리셋 제공: 일반적인 설정 조합을 메서드로 제공
 * - 확장성: customSettings로 모드별 커스텀 설정 지원
 * 
 * 사용 예시:
 * 
 * 1. 프리셋 사용:
 * GameModeConfig config = GameModeConfig.classic();
 * 
 * 2. 커스텀 빌드:
 * GameModeConfig config = GameModeConfig.builder()
 *     .hardDropEnabled(false)
 *     .dropSpeedMultiplier(0.5)
 *     .holdLimit(1)
 *     .customSettings(Map.of("itemDropRate", 0.8))
 *     .build();
 */
@Getter
@Builder
public class GameModeConfig {
    
    // ========== 보드 설정 ==========
    
    /**
     * 보드 너비 (기본: 10)
     */
    @Builder.Default
    private final int boardWidth = 10;
    
    /**
     * 보드 높이 (기본: 20)
     */
    @Builder.Default
    private final int boardHeight = 20;
    
    /**
     * 시작 레벨 (기본: 1)
     */
    @Builder.Default
    private final int startLevel = 1;
    
    
    // ========== 게임플레이 설정 ==========
    
    /**
     * 하드드롭 활성화 여부 (기본: true)
     * false일 경우 하드드롭 명령 무시
     */
    @Builder.Default
    private final boolean hardDropEnabled = true;
    
    /**
     * 홀드 기능 활성화 여부 (기본: true)
     */
    @Builder.Default
    private final boolean holdEnabled = true;
    
    /**
     * 홀드 제한 횟수 (기본: 0 = 무제한)
     * 0: 무제한
     * 1: 턴당 1회
     * -1: 비활성화
     */
    @Builder.Default
    private final int holdLimit = 0;
    
    /**
     * 고스트 블록 표시 여부 (기본: true)
     */
    @Builder.Default
    private final boolean ghostPieceEnabled = true;
    
    /**
     * 낙하 속도 배율 (기본: 1.0)
     * 0.5: 느린 속도
     * 1.0: 기본 속도
     * 2.0: 빠른 속도
     */
    @Builder.Default
    private final double dropSpeedMultiplier = 1.0;
    
    /**
     * 락 딜레이 시간 (밀리초, 기본: 500ms)
     * 블록이 바닥에 닿은 후 고정되기까지의 시간
     */
    @Builder.Default
    private final int lockDelay = 500;
    
    /**
     * 최대 락 리셋 횟수 (기본: 15)
     * 무한 회전 방지를 위한 제한
     */
    @Builder.Default
    private final int maxLockResets = 15;
    
    /**
     * SRS(Super Rotation System) 활성화 여부 (기본: true)
     * true: SRS 회전 시스템 사용
     * false: 기본 회전 시스템 사용
     */
    @Builder.Default
    private final boolean srsEnabled = true;
    
    /**
     * 180도 회전 허용 여부 (기본: false)
     * true: 180도 회전 명령 허용
     * false: 90도 회전만 허용
     */
    @Builder.Default
    private final boolean rotation180Enabled = false;
    
    /**
     * 소프트 드롭 속도 배율 (기본: 20.0)
     * 사용자가 아래 방향키를 누를 때 블록이 내려가는 속도
     */
    @Builder.Default
    private final double softDropSpeed = 20.0;
    
    /**
     * 게임플레이 타입 (기본: CLASSIC)
     * CLASSIC: 전통적인 테트리스
     * ARCADE: 빠르고 박진감 넘치는 모드
     */
    @Builder.Default
    private final GameplayType gameplayType = GameplayType.CLASSIC;
    
    /**
     * 아이템 설정 (아케이드 모드용)
     * null이면 아이템 시스템 비활성화
     */
    @Builder.Default
    private final ItemConfig itemConfig = null;
    
    // ========== 확장 설정 ==========
    
    /**
     * 모드별 커스텀 설정
     * 
     * 예시:
     * - ItemMode: "itemDropRate" -> 0.8
     * - MultiMode: "attackMultiplier" -> 1.5
     */
    @Builder.Default
    private final Map<String, Object> customSettings = new HashMap<>();
    
    // ========== 헬퍼 메서드 ==========
    
    /**
     * 커스텀 설정 값 가져오기
     * 
     * @param key 설정 키
     * @param defaultValue 기본값
     * @return 설정 값 또는 기본값
     */
    public <T> T getCustomSetting(String key, T defaultValue) {
        @SuppressWarnings("unchecked")
        T value = (T) customSettings.get(key);
        return value != null ? value : defaultValue;
    }
    
    // ========== 프리셋 ==========
    
    /**
     * 클래식 모드 설정
     * 모든 설정이 기본값
     */
    public static GameModeConfig classic() {
        return GameModeConfig.builder()
            .gameplayType(GameplayType.CLASSIC)
            .srsEnabled(true)
            .build();
    }
    
    /**
     * 클래식 모드 설정 (SRS 옵션 지정)
     * 
     * @param srsEnabled SRS 활성화 여부
     * @return 클래식 모드 설정
     */
    public static GameModeConfig classic(boolean srsEnabled) {
        return GameModeConfig.builder()
            .gameplayType(GameplayType.CLASSIC)
            .srsEnabled(srsEnabled)
            .build();
    }
    
    /**
     * 아케이드 모드 설정
     * - 빠른 낙하 속도 (1.5배)
     * - 짧은 락 딜레이 (300ms)
     * - SRS 활성화
     * - 아이템 시스템 활성화 (10% 드롭률)
     * - 높은 점수 배율
     */
    public static GameModeConfig arcade() {
        return GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .dropSpeedMultiplier(1.5)
            .lockDelay(300)
            .srsEnabled(true)
            .itemConfig(ItemConfig.arcadeDefault())
            .build();
    }
    
    /**
     * 하드 모드 설정
     * - 빠른 낙하 속도 (2배)
     * - 짧은 락 딜레이 (250ms)
     * - 홀드 비활성화
     */
    public static GameModeConfig hardMode() {
        return GameModeConfig.builder()
            .dropSpeedMultiplier(2.0)
            .lockDelay(250)
            .holdEnabled(false)
            .build();
    }
    
    /**
     * 릴렉스 모드 설정
     * - 느린 낙하 속도 (0.5배)
     * - 긴 락 딜레이 (1000ms)
     * - 모든 보조 기능 활성화
     */
    public static GameModeConfig relaxMode() {
        return GameModeConfig.builder()
            .dropSpeedMultiplier(0.5)
            .lockDelay(1000)
            .maxLockResets(30)
            .build();
    }
}
