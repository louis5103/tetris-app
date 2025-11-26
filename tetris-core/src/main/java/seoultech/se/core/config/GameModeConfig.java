package seoultech.se.core.config;

import lombok.Builder;
import lombok.Getter;
import seoultech.se.core.engine.item.ItemType;
import seoultech.se.core.model.enumType.Difficulty;

import java.util.Collections;
import java.util.Set;

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
    
    // ========== 아이템 시스템 설정 (Arcade 전용) ==========
    
    /**
     * 아이템 생성 간격 (줄 수)
     * arcade.yml의 linesPerItem과 매핑
     * 예: 10이면 10줄 클리어마다 아이템 생성
     */
    @Builder.Default
    private final int linesPerItem = 10;
    
    /**
     * 아이템 드롭 확률 (0.0 ~ 1.0)
     * @deprecated 10줄 카운터 기반으로 변경됨. linesPerItem 사용
     * arcade.yml의 itemDropRate와 매핑
     */
    @Builder.Default
    @Deprecated
    private final double itemDropRate = 0.0;
    
    /**
     * 최대 인벤토리 크기
     * arcade.yml의 maxInventorySize와 매핑
     */
    @Builder.Default
    private final int maxInventorySize = 0;
    
    /**
     * 아이템 자동 사용 여부
     * arcade.yml의 itemAutoUse와 매핑
     */
    @Builder.Default
    private final boolean itemAutoUse = false;
    
    /**
     * 활성화된 아이템 타입 목록
     * arcade.yml의 enabledItems와 매핑
     */
    @Builder.Default
    private final Set<ItemType> enabledItemTypes = Collections.emptySet();
    
    // ========== 헬퍼 메서드 ==========
    
    /**
     * 아이템 시스템 활성화 여부 확인
     * 
     * @return 아이템 시스템이 활성화되어 있으면 true
     */
    public boolean isItemSystemEnabled() {
        return linesPerItem > 0 && !enabledItemTypes.isEmpty();
    }
    
    /**
     * 아케이드 모드 여부 확인
     * 
     * @return 게임플레이 타입이 ARCADE이면 true
     */
    public boolean isArcadeMode() {
        return gameplayType == GameplayType.ARCADE;
    }
    
    // ========== 기본 프리셋 헬퍼 메서드 (레거시 제거됨) ==========
    
    /**
     * 기본 Classic 설정 생성 헬퍼
     * 
     * 주의: 실제 게임에서는 GameModeConfigFactory를 사용하세요.
     * 이 메서드는 테스트용으로만 사용됩니다.
     * 
     * @return Classic 모드 기본 GameModeConfig
     */
    public static GameModeConfig createDefaultClassic() {
        return GameModeConfig.builder()
            .gameplayType(GameplayType.CLASSIC)
            .srsEnabled(true)
            .difficulty(Difficulty.NORMAL)
            .itemDropRate(0.0)
            .maxInventorySize(0)
            .itemAutoUse(false)
            .enabledItemTypes(Collections.emptySet())
            .build();
    }
    
    /**
     * 기본 Arcade 설정 생성 헬퍼
     * 
     * 주의: 실제 게임에서는 GameModeConfigFactory를 사용하세요.
     * 이 메서드는 테스트용으로만 사용됩니다.
     * 
     * @return Arcade 모드 기본 GameModeConfig
     */
    public static GameModeConfig createDefaultArcade() {
        return GameModeConfig.builder()
            .gameplayType(GameplayType.ARCADE)
            .srsEnabled(true)
            .difficulty(Difficulty.NORMAL)
            .dropSpeedMultiplier(1.0)
            .lockDelay(500)
            // 아이템 설정
            .linesPerItem(10)
            .itemDropRate(0.15)  // Deprecated
            .maxInventorySize(3)
            .itemAutoUse(false)
            .enabledItemTypes(java.util.EnumSet.of(
                ItemType.LINE_CLEAR,
                ItemType.WEIGHT_BOMB,
                ItemType.PLUS,
                ItemType.SPEED_RESET,
                ItemType.BONUS_SCORE,
                ItemType.BOMB
            ))
            .build();
    }
}
