package seoultech.se.server.game;

/**
 * 게임 세션 타입을 정의하는 Enum
 *
 * 사용 목적:
 * - 싱글플레이와 멀티플레이 세션을 구분
 * - GameSession의 동작 방식 결정 (게임 루프, 상태 동기화 등)
 * - GameTickService가 어떤 세션에 자동 중력을 적용할지 결정
 */
public enum SessionType {
    /**
     * 싱글플레이 세션
     * - 한 명의 플레이어만 참여
     * - 서버 측 자동 게임 루프 없음 (클라이언트가 모든 명령 전송)
     */
    SINGLE,

    /**
     * 멀티플레이 세션
     * - 두 명의 플레이어가 대전
     * - 서버 측 자동 게임 루프 활성화 (GameTickService)
     * - 서버가 주기적으로 자동 중력 적용
     * - 클라이언트는 사용자 입력만 전송
     */
    MULTI
}
