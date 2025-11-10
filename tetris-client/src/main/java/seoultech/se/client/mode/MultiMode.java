package seoultech.se.client.mode;

import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import seoultech.se.core.GameState;
import seoultech.se.core.config.GameModeConfig;
import seoultech.se.core.mode.GameMode;
import seoultech.se.core.mode.GameModeType;

/**
 * 멀티플레이어 모드
 * 
 * 온라인 네트워크를 통해 다른 플레이어와 함께 플레이하는 모드입니다.
 * 
 * 특징:
 * - 네트워크 통신 필요
 * - 공격/방어 시스템 (라인 클리어 시 상대에게 공격)
 * - 실시간 동기화
 * - 방 생성 및 참여 시스템
 * 
 * 설계 원칙:
 * 1. 네트워크 의존성 (Network Dependency)
 *    - 온라인 연결 필수
 *    - WebSocket 또는 HTTP 통신
 * 
 * 2. 확장성 (Extensibility)
 *    - 향후 다양한 멀티플레이 모드 지원
 *    - 팀전, 배틀로얄 등 확장 가능
 * 
 * 3. 명확한 책임 (Single Responsibility)
 *    - 멀티플레이어 게임 로직만 담당
 *    - 싱글플레이어 로직과 완전 분리
 * 
 * 현재 상태:
 * - 기본 구조만 구현됨
 * - 네트워크 통신 로직은 향후 구현 예정
 * - 현재는 SingleMode와 동일하게 동작
 * 
 * TODO (Phase 7+):
 * - WebSocket 연결 관리
 * - 방 생성/참여 로직
 * - 공격/방어 시스템
 * - 실시간 동기화
 * - 연결 끊김 처리
 * 
 * 사용 예시:
 * 
 * // Spring에서 자동 주입
 * @Autowired
 * private MultiMode multiMode;
 * 
 * // 설정 적용
 * multiMode.setConfig(GameModeConfig.classic());
 * 
 * // BoardController에 설정
 * boardController.setGameMode(multiMode);
 */
@Component
@Getter
@Setter
public class MultiMode implements GameMode {
    
    /**
     * 게임 모드 설정
     * 외부에서 주입 가능하도록 Setter 제공
     */
    private GameModeConfig config = GameModeConfig.classic(); // 기본값: 클래식 모드
    
    /**
     * 게임 상태 참조 (초기화 시 설정)
     */
    private GameState gameState;
    
    /**
     * 온라인 연결 상태
     * TODO: 실제 네트워크 연결 상태 관리 구현
     */
    private boolean connected = false;
    
    /**
     * 방 ID (멀티플레이 룸)
     * TODO: 실제 방 관리 시스템 구현
     */
    private String roomId;
    
    /**
     * 기본 생성자 (Classic 모드)
     */
    public MultiMode() {
        this(GameModeConfig.classic());
    }
    
    /**
     * GameModeConfig를 받는 생성자
     * 
     * @param config 게임 모드 설정
     */
    public MultiMode(GameModeConfig config) {
        this.config = config;
        System.out.println("👥 MultiMode created with config: " + 
            (config.getGameplayType() != null ? config.getGameplayType() : "CLASSIC") +
            ", SRS: " + config.isSrsEnabled());
    }
    
    // ========== GameMode 인터페이스 구현 ==========
    
    @Override
    public GameModeType getType() {
        return GameModeType.MULTI;
    }
    
    @Override
    public GameModeConfig getConfig() {
        return config;
    }
    
    @Override
    public void initialize(GameState initialState) {
        this.gameState = initialState;
        
        System.out.println("👥 MultiMode initialized");
        System.out.println("   GameplayType: " + 
            (config.getGameplayType() != null ? config.getGameplayType() : "CLASSIC"));
        System.out.println("   SRS Enabled: " + config.isSrsEnabled());
        
        // TODO: 네트워크 연결 초기화
        // - WebSocket 연결
        // - 방 참여
        // - 초기 동기화
    }
    
    @Override
    public void cleanup() {
        System.out.println("👥 MultiMode cleanup");
        
        // TODO: 네트워크 연결 정리
        // - WebSocket 종료
        // - 방 나가기
        // - 리소스 해제
        
        this.connected = false;
        this.roomId = null;
    }
    
    @Override
    public boolean isOnlineRequired() {
        return true;  // 멀티플레이어는 항상 온라인 필요
    }
    
    // ========== 멀티플레이어 전용 메서드 ==========
    
    /**
     * 네트워크 연결을 시도합니다
     * 
     * TODO: 실제 구현
     * 
     * @param serverUrl 서버 URL
     * @return 연결 성공 여부
     */
    public boolean connect(String serverUrl) {
        System.out.println("👥 Connecting to server: " + serverUrl);
        // TODO: WebSocket 연결 로직
        return false;
    }
    
    /**
     * 방을 생성합니다
     * 
     * TODO: 실제 구현
     * 
     * @param roomName 방 이름
     * @return 생성된 방 ID
     */
    public String createRoom(String roomName) {
        System.out.println("👥 Creating room: " + roomName);
        // TODO: 방 생성 로직
        return null;
    }
    
    /**
     * 방에 참여합니다
     * 
     * TODO: 실제 구현
     * 
     * @param roomId 방 ID
     * @return 참여 성공 여부
     */
    public boolean joinRoom(String roomId) {
        System.out.println("👥 Joining room: " + roomId);
        // TODO: 방 참여 로직
        return false;
    }
    
    /**
     * 공격을 전송합니다 (라인 클리어 시)
     * 
     * TODO: 실제 구현
     * 
     * @param linesCleared 클리어한 라인 수
     */
    public void sendAttack(int linesCleared) {
        if (linesCleared > 0) {
            System.out.println("👥 Sending attack: " + linesCleared + " lines");
            // TODO: 공격 전송 로직
        }
    }
    
    /**
     * 공격을 수신했을 때 처리합니다
     * 
     * TODO: 실제 구현
     * 
     * @param attackLines 받은 공격 라인 수
     */
    public void receiveAttack(int attackLines) {
        if (attackLines > 0) {
            System.out.println("👥 Received attack: " + attackLines + " lines");
            // TODO: 공격 수신 처리 (방해 라인 추가)
        }
    }
}
