package seoultech.se.core.engine;

import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.item.ItemManager;
import seoultech.se.core.item.ItemType;

/**
 * 아케이드 게임 엔진
 * 
 * ClassicGameEngine을 확장하여 아이템 시스템을 추가합니다.
 * 
 * 추가 기능:
 * - 10줄 클리어마다 아이템 생성
 * - 줄 삭제 아이템 ('L') 지원
 * - 무게추 아이템 지원
 * 
 * 설계 원칙:
 * - Template Method Pattern: ClassicGameEngine을 확장
 * - Strategy Pattern: 아이템별로 다른 전략 적용
 * - Open/Closed Principle: 새 아이템 추가 시 기존 코드 수정 불필요
 */
public class ArcadeGameEngine extends ClassicGameEngine {
    
    /**
     * 아이템 관리자
     */
    private ItemManager itemManager;
    
    /**
     * 게임 모드 설정
     */
    private GameModeConfig config;
    
    // ========== 생성자 및 초기화 ==========
    
    /**
     * 기본 생성자
     */
    public ArcadeGameEngine() {
        super();
        this.itemManager = null;
        this.config = null;
    }
    
    /**
     * ItemManager를 주입받는 생성자
     * 
     * @param itemManager 아이템 관리자
     */
    public ArcadeGameEngine(ItemManager itemManager) {
        super();
        this.itemManager = itemManager;
        this.config = null;
    }
    
    /**
     * 게임 엔진 초기화
     * 
     * @param config 게임 모드 설정
     */
    @Override
    public void initialize(GameModeConfig config) {
        super.initialize(config);
        this.config = config;
        
        // ItemManager가 이미 주입되었으면 초기화하지 않음
        if (itemManager == null && config != null && config.getItemConfig() != null) {
            this.itemManager = new ItemManager(
                config.getItemConfig().getDropRate(),
                config.getItemConfig().getEnabledItems()
            );
        }
        
        if (itemManager != null) {
            System.out.println("✅ [ArcadeGameEngine] Initialized (Arcade Mode - Items Enabled)");
            System.out.println("   - Item drop rate: " + (int)(itemManager.getItemDropRate() * 100) + "%");
            System.out.println("   - Enabled items: " + itemManager.getEnabledItems());
        } else {
            System.out.println("⚠️ [ArcadeGameEngine] Initialized but ItemManager is null!");
        }
    }
    
    /**
     * 아이템 시스템 활성화 여부
     * 
     * @return Arcade 모드는 항상 true
     */
    @Override
    public boolean isItemSystemEnabled() {
        return itemManager != null;
    }
    
    /**
     * 아이템 매니저 반환
     * 
     * @return 아이템 매니저
     */
    public ItemManager getItemManager() {
        return itemManager;
    }
    
    // ========== 아이템 시스템 오버라이드 ==========
    
    /**
     * 아래로 이동 시도 (무게추 블록 제거 지원)
     * 
     * Phase 4: 무게추가 떨어질 때마다 아래 블록 제거
     * 
     * @param state 현재 게임 상태
     * @param isSoftDrop 수동 DOWN 입력 여부
     * @return 새로운 게임 상태
     */
    @Override
    public GameState tryMoveDown(GameState state, boolean isSoftDrop) {
        // Phase 4: 무게추 낙하 중 블록 제거
        if (state.getCurrentTetromino().getType() == seoultech.se.core.model.enumType.TetrominoType.WEIGHT_BOMB) {
            // 이동 전에 아래 블록 제거
            int blocksCleared = seoultech.se.core.item.impl.WeightBombItem.processWeightBombFall(state);
            
            if (blocksCleared > 0) {
                // 점수 추가 (블록당 10점)
                state.addScore(blocksCleared * 10);
            }
        }
        
        // 기본 이동 처리
        return super.tryMoveDown(state, isSoftDrop);
    }
    
    /**
     * 테트로미노를 보드에 고정하고 라인 클리어 처리 (아이템 지원)
     * 
     * ClassicGameEngine의 lockTetromino를 오버라이드하여 아이템 로직 추가:
     * 1. 무게추 최종 처리 (Phase 4)
     * 2. 기본 고정 처리 (ClassicGameEngine)
     * 3. 'L' 마커 줄 삭제 (Phase 3)
     * 4. 라인 클리어 시 아이템 드롭 체크 (10줄마다)
     * 
     * @param state 현재 게임 상태
     * @return 고정이 완료된 새로운 게임 상태
     */
    @Override
    public GameState lockTetromino(GameState state) {
        // 1. Phase 4: 무게추 최종 처리 (고정 전)
        int weightBombScore = 0;
        if (state.getCurrentTetromino().getType() == seoultech.se.core.model.enumType.TetrominoType.WEIGHT_BOMB) {
            // 무게추 위치 계산
            int[] weightBombX = seoultech.se.core.item.impl.WeightBombItem.getWeightBombXPositions(state);
            int weightBombY = state.getCurrentY();
            
            // 수직 경로의 모든 블록 제거
            int blocksCleared = seoultech.se.core.item.impl.WeightBombItem.clearVerticalPath(
                state, weightBombX, weightBombY
            );
            
            // 점수 계산 (블록당 10점)
            weightBombScore = blocksCleared * 10;
            
            System.out.println("⚓ [ArcadeGameEngine] WEIGHT_BOMB final clear: " + 
                blocksCleared + " blocks, " + weightBombScore + " points");
        }
        
        // 2. 기본 고정 처리 (부모 클래스)
        GameState newState = super.lockTetromino(state);
        
        // Phase 4: 무게추 점수 추가
        if (weightBombScore > 0) {
            newState.addScore(weightBombScore);
        }
        
        // 3. 'L' 마커 줄 삭제 처리 (Phase 3)
        if (itemManager != null) {
            java.util.List<Integer> markedLines = 
                seoultech.se.core.item.impl.LineClearItem.findAndClearMarkedLines(newState);
            
            if (!markedLines.isEmpty()) {
                // 'L' 마커 줄 삭제
                int blocksCleared = 
                    seoultech.se.core.item.impl.LineClearItem.clearLines(newState, markedLines);
                
                // 점수 추가 (줄당 100점 기본 + 블록당 10점)
                long lineBonus = markedLines.size() * 100 * newState.getLevel();
                long blockBonus = blocksCleared * 10;
                newState.addScore(lineBonus + blockBonus);
                
                // 라인 카운트 추가 (레벨업 진행을 위해)
                newState.addLinesCleared(markedLines.size());
                
                System.out.println("Ⓛ [ArcadeGameEngine] LINE_CLEAR effect: " + 
                    markedLines.size() + " line(s), " + blocksCleared + " blocks");
                System.out.println("   - Line bonus: " + lineBonus);
                System.out.println("   - Block bonus: " + blockBonus);
            }
        }
        
        // 4. 아이템 드롭 체크 (10줄마다)
        // 주의: 기본 라인 클리어 + 'L' 마커 라인 클리어 모두 포함
        if (itemManager != null && newState.getLastLinesCleared() > 0) {
            ItemType droppedItem = itemManager.checkAndGenerateItem(newState.getLastLinesCleared());
            
            if (droppedItem != null) {
                // 다음 블록에 아이템 타입 설정
                newState.setNextBlockItemType(droppedItem);
                System.out.println("🎁 [ArcadeGameEngine] Item dropped: " + droppedItem);
            }
        }
        
        // Phase 4: 무게추 상태 초기화
        newState.setWeightBombLocked(false);
        
        return newState;
    }
}
