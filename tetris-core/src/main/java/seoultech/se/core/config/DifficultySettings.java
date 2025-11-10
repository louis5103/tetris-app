package seoultech.se.core.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 난이도 설정값을 담는 POJO (Plain Old Java Object)
 * 
 * Spring에 의존하지 않아 Core 모듈에서 사용 가능합니다.
 * application.yml의 설정값을 매핑하고, 게임 로직에서 난이도별 조정에 사용됩니다.
 * 
 * <p>주요 기능:</p>
 * <ul>
 *   <li>I형 블록 출현 확률 조정 (iBlockMultiplier)</li>
 *   <li>레벨업 시 속도 증가율 조정 (speedIncreaseMultiplier)</li>
 *   <li>점수 배율 조정 (scoreMultiplier)</li>
 *   <li>락 딜레이 시간 조정 (lockDelayMultiplier)</li>
 * </ul>
 * 
 * <p>검증 규칙:</p>
 * <ul>
 *   <li>모든 multiplier 값: 0.1 ~ 3.0 범위</li>
 *   <li>displayName: null 불가</li>
 * </ul>
 * 
 * @author SeoulTech SE Team 9
 * @since Phase 1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DifficultySettings {
    
    /**
     * 난이도 표시 이름 (예: "쉬움", "보통", "어려움")
     */
    @NotNull(message = "Display name cannot be null")
    private String displayName;
    
    /**
     * I형 블록 출현 확률 배율
     * 
     * <p>기본 확률은 1/7 (약 14.3%)입니다.</p>
     * <ul>
     *   <li>1.0: 기본값 (14.3%)</li>
     *   <li>1.2: 20% 증가 (17.1%)</li>
     *   <li>0.8: 20% 감소 (11.4%)</li>
     * </ul>
     * <p>Note: @Min/@Max는 정수만 지원하므로 실제 값 × 100으로 저장</p>
     */
    @Min(value = 10, message = "I-block multiplier must be at least 0.1")
    @Max(value = 300, message = "I-block multiplier must not exceed 3.0")
    @Builder.Default
    private double iBlockMultiplier = 1.0;
    
    /**
     * 레벨업 시 속도 증가율 배율
     * 
     * <p>레벨이 오를수록 블록 낙하 속도가 빨라지는 정도를 조정합니다.</p>
     * <ul>
     *   <li>1.0: 기본값</li>
     *   <li>0.8: 20% 완만 (Easy 모드)</li>
     *   <li>1.2: 20% 급격 (Hard 모드)</li>
     * </ul>
     * <p>Note: @Min/@Max는 정수만 지원하므로 실제 값 × 100으로 저장</p>
     */
    @Min(value = 10, message = "Speed increase multiplier must be at least 0.1")
    @Max(value = 300, message = "Speed increase multiplier must not exceed 3.0")
    @Builder.Default
    private double speedIncreaseMultiplier = 1.0;
    
    /**
     * 점수 배율 (SRS 표준 준수)
     * 
     * <p>획득하는 점수에 곱해지는 배율입니다.</p>
     * <p>높은 난이도 = 높은 보상 (업계 표준 원칙)</p>
     * <ul>
     *   <li>0.5: Easy 모드 (초보자 친화적)</li>
     *   <li>1.0: Normal 모드 (기본 배율)</li>
     *   <li>1.5: Hard 모드 (숙련자 보상)</li>
     *   <li>2.0: Expert 모드 (전문가 보상)</li>
     * </ul>
     */
    @Min(value = 10, message = "Score multiplier must be at least 0.1")
    @Max(value = 300, message = "Score multiplier must not exceed 3.0")
    @Builder.Default
    private double scoreMultiplier = 1.0;
    
    /**
     * 락 딜레이 배율
     * 
     * <p>블록이 바닥에 닿은 후 고정되기까지의 시간 배율입니다.</p>
     * <ul>
     *   <li>1.0: 기본값 (500ms)</li>
     *   <li>1.2: 20% 증가 (600ms - Easy 모드)</li>
     *   <li>0.8: 20% 감소 (400ms - Hard 모드)</li>
     * </ul>
     * <p>Note: @Min/@Max는 정수만 지원하므로 실제 값 × 100으로 저장</p>
     */
    @Min(value = 10, message = "Lock delay multiplier must be at least 0.1")
    @Max(value = 300, message = "Lock delay multiplier must not exceed 3.0")
    @Builder.Default
    private double lockDelayMultiplier = 1.0;
    
    // =========================================================================
    // Factory Methods - 기본 프리셋 생성
    // =========================================================================
    
    /**
     * Easy 모드 기본 설정 생성 (SRS 표준)
     * 
     * <p>특징:</p>
     * <ul>
     *   <li>I형 블록 20% 증가</li>
     *   <li>속도 증가 20% 완만</li>
     *   <li>점수 50% (초보자 친화적 - 낮은 배율)</li>
     *   <li>락 딜레이 20% 증가</li>
     * </ul>
     * 
     * @return Easy 모드 설정
     */
    public static DifficultySettings createEasyDefaults() {
        return DifficultySettings.builder()
            .displayName("쉬움")
            .iBlockMultiplier(1.2)
            .speedIncreaseMultiplier(0.8)
            .scoreMultiplier(0.5)  // SRS 표준: 낮은 난이도 = 낮은 배율
            .lockDelayMultiplier(1.2)
            .build();
    }
    
    /**
     * Normal 모드 기본 설정 생성
     * 
     * <p>모든 값이 기본값(1.0)입니다.</p>
     * 
     * @return Normal 모드 설정
     */
    public static DifficultySettings createNormalDefaults() {
        return DifficultySettings.builder()
            .displayName("보통")
            .iBlockMultiplier(1.0)
            .speedIncreaseMultiplier(1.0)
            .scoreMultiplier(1.0)
            .lockDelayMultiplier(1.0)
            .build();
    }
    
    /**
     * Hard 모드 기본 설정 생성 (SRS 표준)
     * 
     * <p>특징:</p>
     * <ul>
     *   <li>I형 블록 20% 감소</li>
     *   <li>속도 증가 20% 급격</li>
     *   <li>점수 150% (숙련자 보상 - 높은 배율)</li>
     *   <li>락 딜레이 20% 감소</li>
     * </ul>
     * 
     * @return Hard 모드 설정
     */
    public static DifficultySettings createHardDefaults() {
        return DifficultySettings.builder()
            .displayName("어려움")
            .iBlockMultiplier(0.8)
            .speedIncreaseMultiplier(1.2)
            .scoreMultiplier(1.5)  // SRS 표준: 높은 난이도 = 높은 배율
            .lockDelayMultiplier(0.8)
            .build();
    }
    
    /**
     * Expert 모드 기본 설정 생성 (SRS 표준)
     * 
     * <p>특징:</p>
     * <ul>
     *   <li>I형 블록 40% 감소 (매우 불리)</li>
     *   <li>속도 증가 50% 급격 (매우 빠른 가속)</li>
     *   <li>점수 200% (전문가 보상 - 최고 배율)</li>
     *   <li>락 딜레이 40% 감소 (매우 빠른 고정)</li>
     * </ul>
     * 
     * @return Expert 모드 설정
     */
    public static DifficultySettings createExpertDefaults() {
        return DifficultySettings.builder()
            .displayName("전문가")
            .iBlockMultiplier(0.6)
            .speedIncreaseMultiplier(1.5)
            .scoreMultiplier(2.0)  // SRS 표준: 최고 난이도 = 최고 배율
            .lockDelayMultiplier(0.6)
            .build();
    }
    
    // =========================================================================
    // Validation Methods
    // =========================================================================
    
    /**
     * 설정값 유효성 검증
     * 
     * <p>검증 항목:</p>
     * <ul>
     *   <li>displayName이 null이 아닌지</li>
     *   <li>모든 multiplier가 0.1 ~ 3.0 범위 내인지</li>
     * </ul>
     * 
     * @throws IllegalArgumentException 검증 실패 시
     */
    public void validate() {
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be null or empty");
        }
        
        validateMultiplier("I-block multiplier", iBlockMultiplier);
        validateMultiplier("Speed increase multiplier", speedIncreaseMultiplier);
        validateMultiplier("Score multiplier", scoreMultiplier);
        validateMultiplier("Lock delay multiplier", lockDelayMultiplier);
    }
    
    /**
     * 개별 multiplier 값 검증
     * 
     * @param name multiplier 이름
     * @param value multiplier 값
     * @throws IllegalArgumentException 범위를 벗어난 경우
     */
    private void validateMultiplier(String name, double value) {
        if (value < 0.1 || value > 3.0) {
            throw new IllegalArgumentException(
                name + " must be between 0.1 and 3.0, but was: " + value
            );
        }
    }
    
    // =========================================================================
    // Helper Methods
    // =========================================================================
    
    /**
     * 설정 정보를 문자열로 반환
     * 
     * @return 설정 정보 문자열
     */
    @Override
    public String toString() {
        return String.format(
            "DifficultySettings{name='%s', I-block=%.2fx, speed=%.2fx, score=%.2fx, lockDelay=%.2fx}",
            displayName, iBlockMultiplier, speedIncreaseMultiplier, 
            scoreMultiplier, lockDelayMultiplier
        );
    }
}
