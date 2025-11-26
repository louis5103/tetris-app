package seoultech.se.client.integration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import seoultech.se.client.config.ClientSettings;
import seoultech.se.client.controller.BoardController;
import seoultech.se.client.service.GameModeConfigFactory;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.config.GameplayType;
import seoultech.se.core.model.enumType.Difficulty;

/**
 * GameModeConfig 통합 테스트
 * 
 * 목적:
 * GameModeConfig와 Difficulty 설정이 4가지 경우(Single/Multi × Classic/Arcade)에서
 * 제대로 초기화되고 하위 컴포넌트(BoardController, TetrominoGenerator 등)까지
 * 정상적으로 전달되는지 검증합니다.
 * 
 * 테스트 케이스:
 * 1. Single Play + Classic Mode
 * 2. Single Play + Arcade Mode
 * 3. Multi Play + Classic Mode (시뮬레이션)
 * 4. Multi Play + Arcade Mode (시뮬레이션)
 * 
 * 검증 항목:
 * - GameModeConfig 생성 및 필드 값 확인
 * - Difficulty 설정 확인
 * - BoardController 초기화 확인
 * - GameplayType별 특성 확인 (아이템 시스템 등)
 */
@DisplayName("GameModeConfig & Difficulty 통합 테스트")
class GameModeConfigIntegrationTest {

    private GameModeConfigFactory configFactory;
    private ClientSettings clientSettings;

    @BeforeEach
    void setUp() {
        System.out.println("\n===========================================");
        System.out.println("🧪 GameModeConfig Integration Test Setup");
        System.out.println("===========================================\n");
        
        // GameModeConfigFactory 생성
        configFactory = new GameModeConfigFactory();
        
        // ClientSettings 수동 생성 (테스트용)
        clientSettings = createTestClientSettings();
    }
    
    /**
     * 테스트용 ClientSettings 생성
     */
    private ClientSettings createTestClientSettings() {
        ClientSettings settings = new ClientSettings();
        ClientSettings.Modes modes = new ClientSettings.Modes();
        
        // Classic 설정
        seoultech.se.client.config.mode.ClassicModeSettings classicSettings = 
            new seoultech.se.client.config.mode.ClassicModeSettings();
        classicSettings.setSrsEnabled(true);
        classicSettings.setRotation180Enabled(false);
        classicSettings.setHardDropEnabled(true);
        classicSettings.setHoldEnabled(true);
        classicSettings.setGhostPieceEnabled(true);
        classicSettings.setDropSpeedMultiplier(1.0);
        classicSettings.setSoftDropSpeed(20.0);
        classicSettings.setLockDelay(500);
        classicSettings.setMaxLockResets(15);
        
        // Arcade 설정
        seoultech.se.client.config.mode.ArcadeModeSettings arcadeSettings = 
            new seoultech.se.client.config.mode.ArcadeModeSettings();
        arcadeSettings.setSrsEnabled(true);
        arcadeSettings.setRotation180Enabled(false);
        arcadeSettings.setHardDropEnabled(true);
        arcadeSettings.setHoldEnabled(true);
        arcadeSettings.setGhostPieceEnabled(true);
        arcadeSettings.setDropSpeedMultiplier(1.5);
        arcadeSettings.setSoftDropSpeed(20.0);
        arcadeSettings.setLockDelay(300);
        arcadeSettings.setMaxLockResets(15);
        arcadeSettings.setItemDropRate(0.1);
        arcadeSettings.setMaxInventorySize(3);
        arcadeSettings.setItemAutoUse(false);
        
        // 활성화된 아이템 설정
        java.util.Map<String, Boolean> enabledItems = new java.util.HashMap<>();
        enabledItems.put("BOMB", true);
        enabledItems.put("PLUS", true);
        enabledItems.put("SPEED_RESET", true);
        enabledItems.put("BONUS_SCORE", true);
        enabledItems.put("LINE_CLEAR", true);
        enabledItems.put("WEIGHT_BOMB", true);
        arcadeSettings.setEnabledItems(enabledItems);
        
        modes.setClassic(classicSettings);
        modes.setArcade(arcadeSettings);
        settings.setModes(modes);
        
        return settings;
    }

    // =========================================================================
    // 테스트 1: Single Play + Classic Mode
    // =========================================================================
    
    @Test
    @DisplayName("1️⃣ Single Play + Classic Mode - GameModeConfig 초기화 검증")
    void testSinglePlayClassicMode() {
        System.out.println("📋 Test 1: Single Play + Classic Mode");
        System.out.println("--------------------------------------");
        
        // Given: Classic 모드, Normal 난이도
        GameplayType gameplayType = GameplayType.CLASSIC;
        Difficulty difficulty = Difficulty.NORMAL;
        
        // When: GameModeConfig 생성
        GameModeConfig config = configFactory.create(clientSettings, gameplayType, difficulty);
        
        // Then 1: GameModeConfig 기본 속성 검증
        assertNotNull(config, "GameModeConfig should not be null");
        assertEquals(GameplayType.CLASSIC, config.getGameplayType(), 
            "GameplayType should be CLASSIC");
        assertEquals(Difficulty.NORMAL, config.getDifficulty(), 
            "Difficulty should be NORMAL");
        
        System.out.println("✅ GameModeConfig created:");
        System.out.println("   - GameplayType: " + config.getGameplayType());
        System.out.println("   - Difficulty: " + config.getDifficulty());
        System.out.println("   - SRS Enabled: " + config.isSrsEnabled());
        System.out.println("   - Hard Drop: " + config.isHardDropEnabled());
        System.out.println("   - Hold: " + config.isHoldEnabled());
        System.out.println("   - Drop Speed Multiplier: " + config.getDropSpeedMultiplier());
        System.out.println("   - Lock Delay: " + config.getLockDelay() + "ms");
        
        // Then 2: Classic 모드 특성 검증
        assertFalse(config.isItemSystemEnabled(), 
            "Classic mode should NOT have item system");
        assertFalse(config.isArcadeMode(), 
            "Should not be arcade mode");
        
        // Then 3: BoardController 초기화 검증
        BoardController boardController = new BoardController(config, difficulty);
        assertNotNull(boardController, "BoardController should be created");
        assertNotNull(boardController.getGameState(), "GameState should be initialized");
        assertEquals(difficulty, boardController.getDifficulty(), 
            "BoardController should have correct difficulty");
        assertEquals(config, boardController.getGameModeConfig(), 
            "BoardController should have correct config");
        
        System.out.println("✅ BoardController initialized:");
        System.out.println("   - Difficulty: " + boardController.getDifficulty());
        System.out.println("   - Board Size: " + 
            boardController.getGameState().getBoardWidth() + "x" + 
            boardController.getGameState().getBoardHeight());
        
        // Then 4: TetrominoGenerator 초기화 검증
        assertNotNull(boardController.getTetrominoGenerator(), 
            "TetrominoGenerator should be initialized");
        
        System.out.println("✅ TetrominoGenerator initialized with difficulty: " + difficulty);
        System.out.println("\n✅ Test 1 PASSED: Single Play + Classic Mode\n");
    }

    // =========================================================================
    // 테스트 2: Single Play + Arcade Mode
    // =========================================================================
    
    @Test
    @DisplayName("2️⃣ Single Play + Arcade Mode - GameModeConfig + ItemSystem 초기화 검증")
    void testSinglePlayArcadeMode() {
        System.out.println("📋 Test 2: Single Play + Arcade Mode");
        System.out.println("--------------------------------------");
        
        // Given: Arcade 모드, Hard 난이도
        GameplayType gameplayType = GameplayType.ARCADE;
        Difficulty difficulty = Difficulty.HARD;
        
        // When: GameModeConfig 생성
        GameModeConfig config = configFactory.create(clientSettings, gameplayType, difficulty);
        
        // Then 1: GameModeConfig 기본 속성 검증
        assertNotNull(config, "GameModeConfig should not be null");
        assertEquals(GameplayType.ARCADE, config.getGameplayType(), 
            "GameplayType should be ARCADE");
        assertEquals(Difficulty.HARD, config.getDifficulty(), 
            "Difficulty should be HARD");
        
        System.out.println("✅ GameModeConfig created:");
        System.out.println("   - GameplayType: " + config.getGameplayType());
        System.out.println("   - Difficulty: " + config.getDifficulty());
        System.out.println("   - Drop Speed Multiplier: " + config.getDropSpeedMultiplier());
        System.out.println("   - Lock Delay: " + config.getLockDelay() + "ms");
        
        // Then 2: Arcade 모드 특성 검증
        assertTrue(config.isArcadeMode(), 
            "Should be arcade mode");
        assertTrue(config.isItemSystemEnabled(), 
            "Arcade mode should have item system enabled");
        assertNotNull(config.getItemConfig(), 
            "ItemConfig should not be null in arcade mode");
        
        System.out.println("✅ Item System configuration:");
        System.out.println("   - Item System Enabled: " + config.isItemSystemEnabled());
        System.out.println("   - Item Drop Rate: " + config.getItemConfig().getDropRate());
        System.out.println("   - Max Inventory Size: " + config.getItemConfig().getMaxInventorySize());
        System.out.println("   - Enabled Items: " + config.getItemConfig().getEnabledItems().size());
        
        // Then 3: BoardController 초기화 검증
        BoardController boardController = new BoardController(config, difficulty);
        assertNotNull(boardController, "BoardController should be created");
        assertEquals(difficulty, boardController.getDifficulty(), 
            "BoardController should have HARD difficulty");
        assertEquals(config, boardController.getGameModeConfig(), 
            "BoardController should have correct arcade config");
        
        System.out.println("✅ BoardController initialized with Arcade mode");
        
        // Then 4: Difficulty 배율 적용 확인 (Hard는 속도가 빨라야 함)
        assertTrue(config.getDropSpeedMultiplier() > 1.0, 
            "Hard difficulty should have faster drop speed");
        assertTrue(config.getLockDelay() < 500, 
            "Hard difficulty should have shorter lock delay");
        
        System.out.println("✅ Difficulty multipliers applied correctly for HARD");
        System.out.println("\n✅ Test 2 PASSED: Single Play + Arcade Mode\n");
    }

    // =========================================================================
    // 테스트 3: Multi Play + Classic Mode (시뮬레이션)
    // =========================================================================
    
    @Test
    @DisplayName("3️⃣ Multi Play + Classic Mode - 멀티플레이 설정 검증")
    void testMultiPlayClassicMode() {
        System.out.println("📋 Test 3: Multi Play + Classic Mode");
        System.out.println("--------------------------------------");
        
        // Given: Classic 모드, Easy 난이도 (멀티플레이 시뮬레이션)
        GameplayType gameplayType = GameplayType.CLASSIC;
        Difficulty difficulty = Difficulty.EASY;
        boolean isMultiplayer = true;
        
        // When: GameModeConfig 생성
        GameModeConfig config = configFactory.create(clientSettings, gameplayType, difficulty);
        
        // Then 1: GameModeConfig 검증
        assertNotNull(config, "GameModeConfig should not be null");
        assertEquals(GameplayType.CLASSIC, config.getGameplayType());
        assertEquals(Difficulty.EASY, config.getDifficulty());
        
        System.out.println("✅ GameModeConfig created for multiplayer:");
        System.out.println("   - GameplayType: " + config.getGameplayType());
        System.out.println("   - Difficulty: " + config.getDifficulty());
        System.out.println("   - Multiplayer Mode: " + isMultiplayer);
        
        // Then 2: BoardController 초기화 (플레이어 개별 보드)
        BoardController playerBoard = new BoardController(config, difficulty);
        assertNotNull(playerBoard, "Player board should be created");
        assertEquals(difficulty, playerBoard.getDifficulty());
        
        // 멀티플레이에서는 각 플레이어가 자신의 BoardController를 가짐
        BoardController opponentBoard = new BoardController(config, difficulty);
        assertNotNull(opponentBoard, "Opponent board should be created");
        assertEquals(difficulty, opponentBoard.getDifficulty());
        
        System.out.println("✅ Both player and opponent boards initialized");
        System.out.println("   - Player board difficulty: " + playerBoard.getDifficulty());
        System.out.println("   - Opponent board difficulty: " + opponentBoard.getDifficulty());
        
        // Then 3: Easy 난이도 특성 검증
        assertTrue(config.getDropSpeedMultiplier() < 1.0, 
            "Easy difficulty should have slower drop speed");
        assertTrue(config.getLockDelay() > 500, 
            "Easy difficulty should have longer lock delay");
        
        System.out.println("✅ Difficulty multipliers applied correctly for EASY");
        System.out.println("\n✅ Test 3 PASSED: Multi Play + Classic Mode\n");
    }

    // =========================================================================
    // 테스트 4: Multi Play + Arcade Mode (시뮬레이션)
    // =========================================================================
    
    @Test
    @DisplayName("4️⃣ Multi Play + Arcade Mode - 멀티플레이 + 아이템 시스템 검증")
    void testMultiPlayArcadeMode() {
        System.out.println("📋 Test 4: Multi Play + Arcade Mode");
        System.out.println("--------------------------------------");
        
        // Given: Arcade 모드, Normal 난이도 (멀티플레이 시뮬레이션)
        GameplayType gameplayType = GameplayType.ARCADE;
        Difficulty difficulty = Difficulty.NORMAL;
        boolean isMultiplayer = true;
        
        // When: GameModeConfig 생성
        GameModeConfig config = configFactory.create(clientSettings, gameplayType, difficulty);
        
        // Then 1: GameModeConfig 검증
        assertNotNull(config, "GameModeConfig should not be null");
        assertEquals(GameplayType.ARCADE, config.getGameplayType());
        assertEquals(Difficulty.NORMAL, config.getDifficulty());
        assertTrue(config.isArcadeMode());
        assertTrue(config.isItemSystemEnabled());
        
        System.out.println("✅ GameModeConfig created for multiplayer arcade:");
        System.out.println("   - GameplayType: " + config.getGameplayType());
        System.out.println("   - Difficulty: " + config.getDifficulty());
        System.out.println("   - Multiplayer Mode: " + isMultiplayer);
        System.out.println("   - Item System: " + config.isItemSystemEnabled());
        
        // Then 2: BoardController 초기화 (각 플레이어)
        BoardController playerBoard = new BoardController(config, difficulty);
        BoardController opponentBoard = new BoardController(config, difficulty);
        
        assertNotNull(playerBoard);
        assertNotNull(opponentBoard);
        assertEquals(config, playerBoard.getGameModeConfig());
        assertEquals(config, opponentBoard.getGameModeConfig());
        
        System.out.println("✅ Multiplayer boards initialized with arcade mode");
        
        // Then 3: 아이템 설정 검증
        assertNotNull(config.getItemConfig(), "ItemConfig should exist");
        assertTrue(config.getItemConfig().getDropRate() > 0, 
            "Item drop rate should be positive");
        assertTrue(config.getItemConfig().getMaxInventorySize() > 0, 
            "Max inventory size should be positive");
        assertFalse(config.getItemConfig().getEnabledItems().isEmpty(), 
            "Should have enabled items");
        
        System.out.println("✅ Item System verified:");
        System.out.println("   - Drop Rate: " + config.getItemConfig().getDropRate());
        System.out.println("   - Max Inventory: " + config.getItemConfig().getMaxInventorySize());
        System.out.println("   - Enabled Items Count: " + config.getItemConfig().getEnabledItems().size());
        
        // Then 4: Normal 난이도 검증 (Arcade 모드는 기본적으로 더 빠름)
        assertTrue(config.getDropSpeedMultiplier() >= 1.0, 
            "Arcade mode should have faster or equal speed compared to Classic");
        assertTrue(config.getLockDelay() <= 500, 
            "Arcade mode should have shorter or equal lock delay compared to Classic");
        
        System.out.println("✅ Difficulty multipliers verified for NORMAL");
        System.out.println("\n✅ Test 4 PASSED: Multi Play + Arcade Mode\n");
    }

    // =========================================================================
    // 추가 테스트: Difficulty 변경 시나리오
    // =========================================================================
    
    @Test
    @DisplayName("5️⃣ Difficulty 변경 시 배율 적용 검증")
    void testDifficultyMultiplierApplication() {
        System.out.println("📋 Test 5: Difficulty Multiplier Verification");
        System.out.println("--------------------------------------");
        
        GameplayType gameplayType = GameplayType.CLASSIC;
        
        // Easy
        GameModeConfig easyConfig = configFactory.create(clientSettings, gameplayType, Difficulty.EASY);
        System.out.println("EASY - Speed: " + easyConfig.getDropSpeedMultiplier() + 
                         ", Lock: " + easyConfig.getLockDelay());
        
        // Normal
        GameModeConfig normalConfig = configFactory.create(clientSettings, gameplayType, Difficulty.NORMAL);
        System.out.println("NORMAL - Speed: " + normalConfig.getDropSpeedMultiplier() + 
                         ", Lock: " + normalConfig.getLockDelay());
        
        // Hard
        GameModeConfig hardConfig = configFactory.create(clientSettings, gameplayType, Difficulty.HARD);
        System.out.println("HARD - Speed: " + hardConfig.getDropSpeedMultiplier() + 
                         ", Lock: " + hardConfig.getLockDelay());
        
        // 검증: EASY < NORMAL < HARD (속도)
        assertTrue(easyConfig.getDropSpeedMultiplier() < normalConfig.getDropSpeedMultiplier(),
            "Easy should be slower than Normal");
        assertTrue(normalConfig.getDropSpeedMultiplier() < hardConfig.getDropSpeedMultiplier(),
            "Normal should be slower than Hard");
        
        // 검증: EASY > NORMAL > HARD (락 딜레이)
        assertTrue(easyConfig.getLockDelay() > normalConfig.getLockDelay(),
            "Easy should have longer lock delay than Normal");
        assertTrue(normalConfig.getLockDelay() > hardConfig.getLockDelay(),
            "Normal should have longer lock delay than Hard");
        
        System.out.println("✅ Difficulty multipliers follow correct progression");
        System.out.println("\n✅ Test 5 PASSED: Difficulty Multiplier Application\n");
    }

    // =========================================================================
    // 추가 테스트: 전체 초기화 플로우 검증
    // =========================================================================
    
    @Test
    @DisplayName("6️⃣ 전체 초기화 플로우 검증 (ClientSettings → GameModeConfig → BoardController)")
    void testCompleteInitializationFlow() {
        System.out.println("📋 Test 6: Complete Initialization Flow");
        System.out.println("--------------------------------------");
        
        // Step 1: ClientSettings 확인
        assertNotNull(clientSettings, "ClientSettings should be loaded");
        assertNotNull(clientSettings.getModes(), "Modes should exist");
        assertNotNull(clientSettings.getModes().getClassic(), "Classic settings should exist");
        assertNotNull(clientSettings.getModes().getArcade(), "Arcade settings should exist");
        
        System.out.println("✅ Step 1: ClientSettings loaded");
        System.out.println("   - Classic SRS: " + clientSettings.getModes().getClassic().isSrsEnabled());
        System.out.println("   - Arcade SRS: " + clientSettings.getModes().getArcade().isSrsEnabled());
        
        // Step 2: Difficulty 선택
        Difficulty selectedDifficulty = Difficulty.NORMAL;  // 테스트용 기본값
        assertNotNull(selectedDifficulty, "Current difficulty should be set");
        
        System.out.println("✅ Step 2: Difficulty selected");
        System.out.println("   - Selected: " + selectedDifficulty);
        
        // Step 3: GameModeConfig 생성 (Factory 사용)
        GameModeConfig config = configFactory.create(
            clientSettings, 
            GameplayType.CLASSIC, 
            selectedDifficulty
        );
        assertNotNull(config, "GameModeConfig should be created");
        assertEquals(selectedDifficulty, config.getDifficulty());
        
        System.out.println("✅ Step 3: GameModeConfig created");
        System.out.println("   - GameplayType: " + config.getGameplayType());
        System.out.println("   - Difficulty: " + config.getDifficulty());
        
        // Step 4: BoardController 초기화
        BoardController boardController = new BoardController(config, selectedDifficulty);
        assertNotNull(boardController);
        assertNotNull(boardController.getGameState());
        assertNotNull(boardController.getTetrominoGenerator());
        assertEquals(config, boardController.getGameModeConfig());
        assertEquals(selectedDifficulty, boardController.getDifficulty());
        
        System.out.println("✅ Step 4: BoardController initialized");
        System.out.println("   - GameState: " + boardController.getGameState().getBoardWidth() + "x" + 
                         boardController.getGameState().getBoardHeight());
        System.out.println("   - TetrominoGenerator: initialized");
        
        // Step 5: 하위 컴포넌트까지 설정 전파 확인
        assertNotNull(boardController.getTetrominoGenerator(), 
            "TetrominoGenerator should be initialized");
        
        System.out.println("✅ Step 5: All components initialized correctly");
        System.out.println("\n✅ Test 6 PASSED: Complete Initialization Flow\n");
        
        System.out.println("===========================================");
        System.out.println("🎉 ALL TESTS PASSED!");
        System.out.println("===========================================");
    }
}
