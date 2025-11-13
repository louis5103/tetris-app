package seoultech.se.core.item;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.core.GameState;
import seoultech.se.core.item.impl.SpeedResetItem;

/**
 * SpeedResetItem 테스트
 * 
 * SPEED_RESET 아이템의 속도 초기화 기능을 검증합니다.
 */
@DisplayName("⚡ SPEED_RESET 아이템 테스트")
class SpeedResetItemTest {
    
    private GameState gameState;
    private SpeedResetItem speedResetItem;
    
    @BeforeEach
    void setUp() {
        gameState = new GameState(10, 20);
        speedResetItem = new SpeedResetItem();
    }
    
    @Test
    @DisplayName("속도 배율이 1.0으로 초기화되어야 함")
    void testSpeedMultiplierResetToOne() {
        // Given: 속도가 빨라진 상태
        gameState.setSoftDropSpeedMultiplier(3.5);
        assertEquals(3.5, gameState.getSoftDropSpeedMultiplier(), 0.001);
        
        // When: SPEED_RESET 아이템 사용
        ItemEffect effect = speedResetItem.apply(gameState, 0, 0);
        
        // Then: 속도가 1.0으로 리셋됨
        assertTrue(effect.isSuccess());
        assertEquals(ItemType.SPEED_RESET, effect.getItemType());
        assertEquals(1.0, gameState.getSoftDropSpeedMultiplier(), 0.001);
    }
    
    @Test
    @DisplayName("speedResetRequested 플래그가 true로 설정되어야 함")
    void testSpeedResetRequestedFlag() {
        // Given: 초기 상태
        assertFalse(gameState.isSpeedResetRequested());
        
        // When: SPEED_RESET 아이템 사용
        speedResetItem.apply(gameState, 0, 0);
        
        // Then: 플래그가 true
        assertTrue(gameState.isSpeedResetRequested());
    }
    
    @Test
    @DisplayName("보너스 점수가 100점 부여되어야 함")
    void testBonusScore() {
        // When: SPEED_RESET 아이템 사용
        ItemEffect effect = speedResetItem.apply(gameState, 0, 0);
        
        // Then: 보너스 점수 100점
        assertEquals(100, effect.getBonusScore());
    }
    
    @Test
    @DisplayName("속도가 이미 1.0일 때도 정상 작동해야 함")
    void testSpeedAlreadyNormal() {
        // Given: 속도가 이미 정상(1.0)
        gameState.setSoftDropSpeedMultiplier(1.0);
        
        // When: SPEED_RESET 아이템 사용
        ItemEffect effect = speedResetItem.apply(gameState, 0, 0);
        
        // Then: 여전히 1.0, 플래그는 true
        assertTrue(effect.isSuccess());
        assertEquals(1.0, gameState.getSoftDropSpeedMultiplier(), 0.001);
        assertTrue(gameState.isSpeedResetRequested());
    }
    
    @Test
    @DisplayName("속도가 느린 경우(0.5)에도 1.0으로 초기화되어야 함")
    void testSlowerSpeedResetToOne() {
        // Given: 속도가 느린 상태 (0.5 = 절반 속도)
        gameState.setSoftDropSpeedMultiplier(0.5);
        
        // When: SPEED_RESET 아이템 사용
        speedResetItem.apply(gameState, 0, 0);
        
        // Then: 1.0으로 리셋 (느린 것도 정상 속도로)
        assertEquals(1.0, gameState.getSoftDropSpeedMultiplier(), 0.001);
    }
    
    @Test
    @DisplayName("여러 번 사용해도 정상 작동해야 함")
    void testMultipleUses() {
        // Given: 속도가 빨라진 상태
        gameState.setSoftDropSpeedMultiplier(5.0);
        
        // When: 첫 번째 사용
        speedResetItem.apply(gameState, 0, 0);
        assertEquals(1.0, gameState.getSoftDropSpeedMultiplier(), 0.001);
        
        // 플래그 리셋 (BoardController가 처리했다고 가정)
        gameState.setSpeedResetRequested(false);
        
        // 속도가 다시 빨라짐
        gameState.setSoftDropSpeedMultiplier(7.0);
        
        // When: 두 번째 사용
        speedResetItem.apply(gameState, 0, 0);
        
        // Then: 다시 1.0으로 리셋
        assertEquals(1.0, gameState.getSoftDropSpeedMultiplier(), 0.001);
        assertTrue(gameState.isSpeedResetRequested());
    }
    
    @Test
    @DisplayName("아이템이 비활성화되면 효과가 없어야 함")
    void testDisabledItem() {
        // Given: 아이템 비활성화
        speedResetItem.setEnabled(false);
        gameState.setSoftDropSpeedMultiplier(3.0);
        
        // When: 아이템 사용 시도
        ItemEffect effect = speedResetItem.apply(gameState, 0, 0);
        
        // Then: 효과 없음
        assertFalse(effect.isSuccess());
        assertEquals(3.0, gameState.getSoftDropSpeedMultiplier(), 0.001);
        assertFalse(gameState.isSpeedResetRequested());
    }
    
    @Test
    @DisplayName("메시지에 '속도가 초기값으로 돌아갑니다' 포함되어야 함")
    void testMessage() {
        // When: SPEED_RESET 아이템 사용
        ItemEffect effect = speedResetItem.apply(gameState, 0, 0);
        
        // Then: 적절한 메시지
        assertNotNull(effect.getMessage());
        assertTrue(effect.getMessage().contains("속도"));
        assertTrue(effect.getMessage().contains("초기값"));
    }
    
    @Test
    @DisplayName("극단적인 속도 값(100.0)도 1.0으로 초기화되어야 함")
    void testExtremeSpeed() {
        // Given: 극단적으로 빠른 속도
        gameState.setSoftDropSpeedMultiplier(100.0);
        
        // When: SPEED_RESET 아이템 사용
        speedResetItem.apply(gameState, 0, 0);
        
        // Then: 1.0으로 리셋
        assertEquals(1.0, gameState.getSoftDropSpeedMultiplier(), 0.001);
    }
    
    @Test
    @DisplayName("속도 리셋 후 플래그를 수동으로 해제할 수 있어야 함")
    void testManualFlagReset() {
        // Given: SPEED_RESET 사용
        speedResetItem.apply(gameState, 0, 0);
        assertTrue(gameState.isSpeedResetRequested());
        
        // When: 플래그 수동 해제 (BoardController가 처리 완료)
        gameState.setSpeedResetRequested(false);
        
        // Then: 플래그가 false
        assertFalse(gameState.isSpeedResetRequested());
        // 속도 배율은 여전히 1.0
        assertEquals(1.0, gameState.getSoftDropSpeedMultiplier(), 0.001);
    }
}
