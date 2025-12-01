package seoultech.se.core.engine.item;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.random.RandomGenerator;

/**
 * ItemManager의 RandomGenerator 통합 테스트
 * 
 * 목적:
 * - new Random() 대신 RandomGenerator 사용 확인
 * - Seed 기반 재현 가능한 아이템 생성 검증
 */
@DisplayName("ItemManager RandomGenerator 통합 테스트")
class ItemManagerRandomGeneratorTest {

    @Test
    @DisplayName("동일한 Seed로 생성한 ItemManager는 동일한 아이템을 생성한다")
    void testSameSeededItemManagersGenerateSameItems() {
        // given: Seed 12345
        RandomGenerator rng1 = new RandomGenerator(12345L);
        ItemManager manager1 = new ItemManager(10, EnumSet.allOf(ItemType.class), rng1);
        
        // when: 아이템 10개 생성
        Set<ItemType> items1 = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            items1.add(manager1.generateRandomItemType());
        }
        
        System.out.println("Seed 12345 - Items: " + items1);
        
        // given: 같은 Seed 12345
        RandomGenerator rng2 = new RandomGenerator(12345L);
        ItemManager manager2 = new ItemManager(10, EnumSet.allOf(ItemType.class), rng2);
        
        // when: 아이템 10개 생성
        Set<ItemType> items2 = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            items2.add(manager2.generateRandomItemType());
        }
        
        System.out.println("Seed 12345 - Items: " + items2);
        
        // then: 동일한 순서로 생성되었으므로 같은 아이템 집합
        assertEquals(items1, items2, "동일한 Seed는 동일한 아이템을 생성해야 함");
    }
    
    @Test
    @DisplayName("동일한 Seed로 순차 생성하면 정확히 같은 순서로 아이템이 생성된다")
    void testSequentialItemGenerationWithSameSeed() {
        // given: Seed 99999
        RandomGenerator rng1 = new RandomGenerator(99999L);
        ItemManager manager1 = new ItemManager(10, EnumSet.allOf(ItemType.class), rng1);
        
        // when: 순차적으로 5개 생성
        ItemType[] sequence1 = new ItemType[5];
        for (int i = 0; i < 5; i++) {
            sequence1[i] = manager1.generateRandomItemType();
        }
        
        System.out.println("First run:");
        for (int i = 0; i < 5; i++) {
            System.out.println("  Item " + i + ": " + sequence1[i]);
        }
        
        // given: 같은 Seed 99999
        RandomGenerator rng2 = new RandomGenerator(99999L);
        ItemManager manager2 = new ItemManager(10, EnumSet.allOf(ItemType.class), rng2);
        
        // when: 순차적으로 5개 생성
        ItemType[] sequence2 = new ItemType[5];
        for (int i = 0; i < 5; i++) {
            sequence2[i] = manager2.generateRandomItemType();
        }
        
        System.out.println("Second run:");
        for (int i = 0; i < 5; i++) {
            System.out.println("  Item " + i + ": " + sequence2[i]);
        }
        
        // then: 정확히 같은 순서
        assertArrayEquals(sequence1, sequence2, "동일한 Seed는 동일한 순서로 아이템을 생성해야 함");
    }
    
    @Test
    @DisplayName("다른 Seed는 다른 아이템 순서를 생성할 수 있다")
    void testDifferentSeedsGenerateDifferentSequences() {
        // given: Seed 1000
        RandomGenerator rng1 = new RandomGenerator(1000L);
        ItemManager manager1 = new ItemManager(10, EnumSet.allOf(ItemType.class), rng1);
        
        ItemType first1 = manager1.generateRandomItemType();
        
        // given: Seed 2000
        RandomGenerator rng2 = new RandomGenerator(2000L);
        ItemManager manager2 = new ItemManager(10, EnumSet.allOf(ItemType.class), rng2);
        
        ItemType first2 = manager2.generateRandomItemType();
        
        System.out.println("Seed 1000 - First item: " + first1);
        System.out.println("Seed 2000 - First item: " + first2);
        
        // then: 다를 수 있음 (확률적으로는 같을 수도 있지만 대부분 다름)
        assertNotNull(first1);
        assertNotNull(first2);
    }
    
    @Test
    @DisplayName("특정 아이템만 활성화하면 해당 아이템만 생성된다")
    void testOnlyEnabledItemsAreGenerated() {
        // given: LINE_CLEAR만 활성화
        Set<ItemType> enabledItems = EnumSet.of(ItemType.LINE_CLEAR);
        RandomGenerator rng = new RandomGenerator(55555L);
        ItemManager manager = new ItemManager(10, enabledItems, rng);
        
        // when: 여러 번 생성
        for (int i = 0; i < 20; i++) {
            ItemType item = manager.generateRandomItemType();
            
            // then: 항상 LINE_CLEAR
            assertEquals(ItemType.LINE_CLEAR, item, 
                "활성화된 아이템만 생성되어야 함 (iteration " + i + ")");
        }
    }
    
    @Test
    @DisplayName("활성화된 아이템이 없으면 null 반환")
    void testNoEnabledItemsReturnsNull() {
        // given: 아이템 비활성화
        Set<ItemType> emptySet = EnumSet.noneOf(ItemType.class);
        RandomGenerator rng = new RandomGenerator();
        ItemManager manager = new ItemManager(10, emptySet, rng);
        
        // when
        ItemType item = manager.generateRandomItemType();
        
        // then
        assertNull(item, "활성화된 아이템이 없으면 null 반환");
    }
    
    @Test
    @DisplayName("기본 생성자는 RandomGenerator를 사용한다")
    void testDefaultConstructorUsesRandomGenerator() {
        // given: 기본 생성자
        ItemManager manager = new ItemManager();
        
        // when: 아이템 생성
        ItemType item1 = manager.generateRandomItemType();
        ItemType item2 = manager.generateRandomItemType();
        
        System.out.println("Default constructor - Item1: " + item1 + ", Item2: " + item2);
        
        // then: null이 아님 (기본적으로 모든 아이템 활성화)
        assertNotNull(item1, "기본 생성자도 아이템 생성 가능");
        assertNotNull(item2, "기본 생성자도 아이템 생성 가능");
    }
    
    @Test
    @DisplayName("Seed를 사용하여 특정 아이템 시퀀스 재현")
    void testSpecificItemSequenceReproduction() {
        // given: Seed 7777로 아이템 시퀀스 생성
        RandomGenerator rng1 = new RandomGenerator(7777L);
        ItemManager manager1 = new ItemManager(10, EnumSet.allOf(ItemType.class), rng1);
        
        StringBuilder sequence1 = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            ItemType item = manager1.generateRandomItemType();
            sequence1.append(item).append(",");
        }
        
        System.out.println("Seed 7777 sequence: " + sequence1);
        
        // given: 같은 Seed 7777
        RandomGenerator rng2 = new RandomGenerator(7777L);
        ItemManager manager2 = new ItemManager(10, EnumSet.allOf(ItemType.class), rng2);
        
        StringBuilder sequence2 = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            ItemType item = manager2.generateRandomItemType();
            sequence2.append(item).append(",");
        }
        
        System.out.println("Seed 7777 sequence: " + sequence2);
        
        // then: 정확히 재현됨
        assertEquals(sequence1.toString(), sequence2.toString(), 
            "Seed 7777은 항상 동일한 시퀀스를 생성해야 함");
    }
}
