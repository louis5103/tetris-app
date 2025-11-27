package seoultech.se.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Tetris Multiplayer Game Server Application
 * WebSocket 기반 멀티플레이어 게임 서버
 *
 * - 세션 기반 게임 룸 관리
 * - 실시간 게임 상태 동기화
 * - 사용자 인증 (JWT)
 * - Phase 1: 세션 타임아웃 스케줄링 활성화
 */
@SpringBootApplication(scanBasePackages = {
    "seoultech.se.server",    // 서버 패키지 (컨트롤러, 서비스, WebSocket 등)
    "seoultech.se.core"       // 코어 패키지 (공통 로직)
})
@EnableScheduling  // Phase 1: 스케줄링 활성화 (세션 타임아웃 정리용)
public class TetrisServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TetrisServerApplication.class, args);
    }
}
