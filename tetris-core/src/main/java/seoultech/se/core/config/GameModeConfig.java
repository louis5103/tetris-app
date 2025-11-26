package seoultech.se.core.config;

import lombok.Builder;
import lombok.Getter;
import seoultech.se.core.engine.item.ItemConfig;
import seoultech.se.core.model.enumType.Difficulty;

/**
 * 게임 모드 설정 객체 (리팩토링 완료)
 * 
 * ClientSettings(사용자 저장 설정) + Difficulty(런타임 선택)를 통합하여
 * 게임 실행 시점의 최종 설정을 담는 불변 객체입니다.
 * 
 * 설계 원칙:
 * - 불변성(Immutability): 생성 후 수정 불가
 * - ClientSettings 기반: YML 파일과 1:1 매핑된 설정 사용
 * - Difficulty 통합: 난이도에 따른 배율이 이미 적용된 최종 값 저장
 * 
 * 사용 예시:
 * ```java
 * // ClientSettings + Difficulty → GameModeConfig 변환
 * GameModeConfig config = GameModeConfig.fromClassicSettings(
 *     classicSettings, 
 *     Difficulty.HARD
 * );
 * 
 * // 또는 직접 빌드
 * GameModeConfig config = GameModeConfig.builder()
 *     .gameplayType(GameplayType.CLASSIC)
 *     .srsEnabled(true)
 *     .difficulty(Difficulty.NORMAL)
 *     .build();
 * ```
 */
@Getter
@Builder
public class GameModeConfig {
    
    // ========== 회전 시스템 ==========
    
    /**
     * SRS(Super Rotation System) 활성화 여부
     */
    @Builder.Default
    private final boolean srsEnabled = true;
    
    /**
     * 180도 회전 허용 여부
     */
    @Builder.Default
    private final boolean rotation180Enabled = false;
    
    // ========== 기능 활성화 ==========
    
    /**
     * 하드드롭 활성화 여부
     */
    @Builder.Default
    private final boolean hardDropEnabled = true;
    
    /**
     * 홀드 기능 활성화 여부
     */
    @Builder.Default
    private final boolean holdEnabled = true;
    
    /**
     * 고스트 블록 표시 여부
     */
    @Builder.Default
    private final boolean ghostPieceEnabled = true;
    
    // ========== 속도 설정 ==========
    
    /**
     * 낙하 속도 배율
     * ClientSettings의 dropSpeedMultiplier * Difficulty 배율
     */
    @Builder.Default
    private final double dropSpeedMultiplier = 1.0;
    
    /**
     * 소프트 드롭 속도
     */
    @Builder.Default
    private final double softDropSpeed = 20.0;
    
    // ========== 락 시스템 ==========
    
    /**
     * 락 딜레이 시간 (밀리초)
     * ClientSettings의 lockDelay * Difficulty 배율
     */
    @Builder.Default
    private final int lockDelay = 500;
    
    /**
     * 최대 락 리셋 횟수
     */
    @Builder.Default
    private final int maxLockResets = 15;
    
    // ========== 게임 타입 ==========
    
    /**
     * 게임플레이 타입 (CLASSIC or ARCADE)
     */
    @Builder.Default
    private final GameplayType gameplayType = GameplayType.CLASSIC;
    
    /**
     * 난이도 (EASY, NORMAL, HARD)
     */
    @Builder.Default
    private final Difficulty difficulty = Difficulty.NORMAL;
    
    /**
     * 아이템 설정 (아케이드 모드 전용)
     * null이면 아이템 시스템 비활성화
     */
    @Builder.Default
    private final ItemConfig itemConfig = null;
    
    // ========== 헬퍼 메서드 ==========
    
    /**
     * 아이템 시스템 활성화 여부 확인
     * 
     * @return 아이템 시스템이 활성화되어 있으면 true
     */
    public boolean isItemSystemEnabled() {
        return itemConfig != null && itemConfig.isEnabled();
    }
    
    /**
     * 아케이드 모드 여부 확인
     * 
     * @return 게임플레이 타입이 ARCADE이면 true
     */
    public boolean isArcadeMode() {
        return gameplayType == GameplayType.ARCADE;
    }
    
    // ========== 하위 호환성 프리셋 (Deprecated) ==========
    
    /**
     * @deprecated ClientSettings + Difficulty 기반 생성 권장
     * 하위 호환성을 위해 유지
     */
    @Deprecated
    public static GameModeConfig classic() {
        return GameModeConfig.builder()
            .gameplayType(GameplayType.CLASSIC)
            .srsEnabled(true)
            .difficulty(Difficulty.NORMAL)
            .build();
    }
    
    /**
     * @deprecated ClientSettings + Difficulty 기반 생성 권장
     * 하위 호환성을 위해 유지
     */
    @Deprecated
    public static GameModeConfig arcade() {
        return GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .dropSpeedMultiplier(1.5)
            .lockDelay(300)
            .srsEnabled(true)
            .itemConfig(ItemConfig.arcadeDefault())
            .difficulty(Difficulty.NORMAL)
            .build();
    }
}
