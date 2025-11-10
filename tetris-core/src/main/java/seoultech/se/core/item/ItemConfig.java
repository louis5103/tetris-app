package seoultech.se.core.item;

import java.util.EnumSet;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;

/**
 * 아이템 설정
 * 
 * 아케이드 모드에서 사용되는 아이템 시스템의 설정을 담는 불변 객체입니다.
 * 
 * 주요 설정:
 * - 아이템 드롭 확률
 * - 활성화된 아이템 타입 목록
 * - 아이템 인벤토리 크기
 * 
 * 설계 원칙:
 * - 불변성(Immutability): 생성 후 수정 불가
 * - Builder Pattern: 유연한 설정 구성
 */
@Getter
@Builder
public class ItemConfig {
    
    /**
     * 아이템 드롭 확률 (0.0 ~ 1.0)
     * 라인 클리어 시 아이템이 드롭될 확률
     */
    @Builder.Default
    private final double dropRate = 0.1; // 기본 10%
    
    /**
     * 활성화된 아이템 타입 목록
     * 이 목록에 포함된 아이템만 드롭됩니다
     */
    @Builder.Default
    private final Set<ItemType> enabledItems = EnumSet.allOf(ItemType.class);
    
    /**
     * 아이템 인벤토리 최대 크기
     * 플레이어가 보유할 수 있는 최대 아이템 수
     */
    @Builder.Default
    private final int maxInventorySize = 3;
    
    /**
     * 아이템 자동 사용 여부
     * true: 획득 시 즉시 사용
     * false: 인벤토리에 저장
     */
    @Builder.Default
    private final boolean autoUse = false;
    
    /**
     * 기본 아케이드 모드 아이템 설정
     * 
     * @return 모든 아이템이 활성화된 기본 설정
     */
    public static ItemConfig arcadeDefault() {
        return ItemConfig.builder()
            .dropRate(0.1)
            .enabledItems(EnumSet.allOf(ItemType.class))
            .maxInventorySize(3)
            .autoUse(false)
            .build();
    }
    
    /**
     * 아이템 비활성화 설정
     * 
     * @return 아이템 시스템이 완전히 비활성화된 설정
     */
    public static ItemConfig disabled() {
        return ItemConfig.builder()
            .dropRate(0.0)
            .enabledItems(EnumSet.noneOf(ItemType.class))
            .maxInventorySize(0)
            .autoUse(false)
            .build();
    }
    
    /**
     * 특정 아이템만 활성화
     * 
     * @param itemTypes 활성화할 아이템 타입들
     * @return 커스텀 아이템 설정
     */
    public static ItemConfig withItems(ItemType... itemTypes) {
        Set<ItemType> items = EnumSet.noneOf(ItemType.class);
        for (ItemType type : itemTypes) {
            items.add(type);
        }
        return ItemConfig.builder()
            .dropRate(0.1)
            .enabledItems(items)
            .maxInventorySize(3)
            .autoUse(false)
            .build();
    }
    
    /**
     * 아이템이 활성화되었는지 확인
     * 
     * @param itemType 아이템 타입
     * @return 활성화 여부
     */
    public boolean isItemEnabled(ItemType itemType) {
        return enabledItems.contains(itemType);
    }
    
    /**
     * 아이템 시스템이 활성화되었는지 확인
     * 
     * @return 활성화 여부
     */
    public boolean isEnabled() {
        return dropRate > 0 && !enabledItems.isEmpty();
    }
}
