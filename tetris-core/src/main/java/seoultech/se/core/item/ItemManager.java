package seoultech.se.core.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import seoultech.se.core.GameState;

/**
 * 아이템 관리자 (Stateless)
 *
 * Phase 2: Req2 준수 - 10줄 카운터 기반 아이템 생성
 * Stateless 리팩토링: 모든 상태를 GameState로 이동
 *
 * 게임 내 아이템의 생성, 관리를 담당하는 클래스입니다.
 *
 * 주요 기능:
 * - 10줄 카운터: 10줄 클리어마다 아이템 생성 (Req2 명세)
 * - 아이템 활성화 관리: 설정에 따라 아이템 활성화/비활성화
 * - 랜덤 아이템 선택: 활성화된 아이템 중 무작위 선택
 *
 * 설계 원칙:
 * - Factory Pattern: 아이템 생성을 중앙화
 * - Thread-Safe: 불변 설정만 보유
 * - Stateless: 모든 상태는 GameState에 저장
 */
public class ItemManager {

    /**
     * 아이템 생성 간격 (줄 수)
     * Req2 명세: 10줄마다 아이템 생성
     */
    private static final int LINES_PER_ITEM = 10;

    /**
     * 아이템 팩토리 맵
     * 각 아이템 타입에 대한 팩토리 함수를 저장
     */
    private final Map<ItemType, Item> itemPrototypes;

    /**
     * 아이템 드롭 확률 (읽기 전용 설정값)
     */
    private final double itemDropRate;

    /**
     * 활성화된 아이템 타입 목록 (읽기 전용 설정값)
     */
    private final Set<ItemType> enabledItemTypes;

    /**
     * 랜덤 생성기 (Thread-safe)
     */
    private final Random random;
    
    /**
     * 생성자
     *
     * @param itemDropRate 아이템 드롭 확률 (0.0 ~ 1.0)
     * @param enabledItemTypes 활성화할 아이템 타입들
     */
    public ItemManager(double itemDropRate, Set<ItemType> enabledItemTypes) {
        this.itemDropRate = itemDropRate;
        this.enabledItemTypes = ConcurrentHashMap.newKeySet();
        this.enabledItemTypes.addAll(enabledItemTypes != null ? enabledItemTypes : EnumSet.allOf(ItemType.class));
        this.random = new Random();
        this.itemPrototypes = new ConcurrentHashMap<>();

        // 프로토타입 등록 (팩토리 패턴)
        registerPrototypes();

        System.out.println("✅ ItemManager initialized (Stateless) - Drop Rate: " + (int)(itemDropRate * 100) +
            "%, Enabled Items: " + this.enabledItemTypes);
    }

    /**
     * 기본 생성자 (모든 아이템 활성화, 10% 드롭률)
     */
    public ItemManager() {
        this(0.1, EnumSet.allOf(ItemType.class));
    }
    
    /**
     * 프로토타입 등록
     * Phase 4: 모든 아이템 등록
     */
    private void registerPrototypes() {
        // Phase 3: LINE_CLEAR 아이템 등록
        registerItem(new seoultech.se.core.item.impl.LineClearItem());
        
        // Phase 4: WEIGHT_BOMB 아이템 등록
        registerItem(new seoultech.se.core.item.impl.WeightBombItem());
        
        // Phase 5: 추가 아이템들 등록
        registerItem(new seoultech.se.core.item.impl.PlusItem());
        registerItem(new seoultech.se.core.item.impl.SpeedResetItem());
        registerItem(new seoultech.se.core.item.impl.BonusScoreItem());
        registerItem(new seoultech.se.core.item.impl.BombItem());
        
        System.out.println("📦 ItemManager: All items registered");
    }
    
    /**
     * 아이템 프로토타입 등록
     * 
     * @param item 등록할 아이템
     */
    public void registerItem(Item item) {
        itemPrototypes.put(item.getType(), item);
        System.out.println("📦 Item registered: " + item.getType());
    }
    
    
    /**
     * 아이템이 활성화되었는지 확인
     * 
     * @param itemType 아이템 타입
     * @return 활성화 여부
     */
    public boolean isItemEnabled(ItemType itemType) {
        return enabledItemTypes.contains(itemType);
    }
    
    
    /**
     * 아이템 드롭 확률 반환
     * 
     * @return 드롭 확률
     */
    public double getItemDropRate() {
        return itemDropRate;
    }
    
    /**
     * 활성화된 아이템 목록 반환
     * 
     * @return 활성화된 아이템 타입 집합
     */
    public Set<ItemType> getEnabledItems() {
        return Collections.unmodifiableSet(enabledItemTypes);
    }
    
    /**
     * 아이템을 드롭할지 결정 (Deprecated - Req2에서는 10줄 카운터 사용)
     * 
     * @return 아이템을 드롭하면 true
     * @deprecated Req2 명세에 따라 10줄 카운터 기반으로 변경됨
     */
    @Deprecated
    public boolean shouldDropItem() {
        return random.nextDouble() < itemDropRate;
    }
    
    /**
     * 라인 클리어 시 아이템 드롭 체크 (Req2 명세 - Stateless)
     *
     * 10줄을 클리어할 때마다 아이템을 생성합니다.
     * 확률 기반이 아닌 카운터 기반입니다.
     *
     * Stateless: GameState의 linesUntilNextItem을 읽고 업데이트된 GameState를 반환
     *
     * @param state 현재 게임 상태
     * @param linesCleared 이번에 클리어된 줄 수
     * @return 업데이트된 게임 상태 (아이템 생성 시 nextBlockItemType 설정됨)
     */
    public GameState checkAndGenerateItem(GameState state, int linesCleared) {
        if (linesCleared <= 0 || state == null) {
            return state;
        }

        GameState newState = state.deepCopy();
        int remaining = newState.getLinesUntilNextItem() - linesCleared;

        if (remaining <= 0) {
            // LINES_PER_ITEM 줄 달성! 아이템 생성
            ItemType itemType = generateRandomItemType();

            if (itemType != null) {
                newState.setNextBlockItemType(itemType);
                System.out.println("🎁 [ItemManager] Item generated after " + LINES_PER_ITEM + " lines: " + itemType);
            }

            // 카운터 리셋
            newState.setLinesUntilNextItem(LINES_PER_ITEM);
        } else {
            // 카운터만 갱신
            newState.setLinesUntilNextItem(remaining);
            System.out.println("📊 [ItemManager] Lines cleared: " + linesCleared +
                ", remaining: " + remaining);
        }

        return newState;
    }
    
    /**
     * 랜덤 아이템 타입 생성
     * 활성화된 아이템 중에서 무작위로 하나를 선택합니다.
     * 
     * @return 생성된 아이템 타입, 활성화된 아이템이 없으면 null
     */
    public ItemType generateRandomItemType() {
        if (enabledItemTypes.isEmpty()) {
            System.out.println("⚠️ [ItemManager] No enabled items to generate");
            return null;
        }
        
        List<ItemType> enabledList = new ArrayList<>(enabledItemTypes);
        ItemType randomType = enabledList.get(random.nextInt(enabledList.size()));
        
        return randomType;
    }
    
    /**
     * 랜덤 아이템 생성 (Deprecated)
     * 
     * @return 생성된 아이템, 활성화된 아이템이 없으면 null
     * @deprecated Phase 2에서 generateRandomItemType()으로 변경됨
     */
    @Deprecated
    public Item generateRandomItem() {
        ItemType itemType = generateRandomItemType();
        if (itemType == null) {
            return null;
        }
        
        Item prototype = itemPrototypes.get(itemType);
        if (prototype != null) {
            return prototype;
        }
        
        System.out.println("⚠️ No prototype found for item type: " + itemType);
        return null;
    }
    
    /**
     * 특정 타입의 아이템 가져오기
     * 
     * @param itemType 아이템 타입
     * @return 아이템 인스턴스
     */
    public Item getItem(ItemType itemType) {
        return itemPrototypes.get(itemType);
    }
    
    /**
     * 아이템 사용
     * 
     * @param item 사용할 아이템
     * @param gameState 게임 상태
     * @param row 행
     * @param col 열
     * @return 아이템 효과
     */
    public ItemEffect useItem(Item item, GameState gameState, int row, int col) {
        if (item == null || !item.isEnabled()) {
            System.out.println("⚠️ Cannot use item: " + (item != null ? item.getType() : "null"));
            return ItemEffect.none();
        }
        
        ItemEffect effect = item.apply(gameState, row, col);
        
        if (effect.isSuccess()) {
            System.out.println("✨ Item used successfully: " + item.getType() + 
                " - Blocks cleared: " + effect.getBlocksCleared() + 
                ", Bonus score: " + effect.getBonusScore());
        }
        
        return effect;
    }
    
    
    /**
     * 현재 상태 출력
     * 
     * @return 상태 문자열
     */
    @Override
    public String toString() {
        return String.format("ItemManager[DropRate=%.1f%%, EnabledItems=%s]",
            itemDropRate * 100,
            enabledItemTypes.stream()
                .map(ItemType::getDisplayName)
                .collect(Collectors.joining(", ")));
    }
}
