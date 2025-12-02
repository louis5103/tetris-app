package seoultech.se.core.concurrent;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 🧪 Java 23 Virtual Threads 테스트 클래스
 * 
 * Virtual Threads의 성능과 동작을 확인할 수 있는 테스트
 */
public class VirtualThreadsTest {
    
    public static void main(String[] args) {
        System.out.println("🧵 Java 23 Virtual Threads 테스트 시작");
        System.out.println("Java 버전: " + System.getProperty("java.version"));
        
        TetrisGameThreadManager threadManager = new TetrisGameThreadManager();
        
        try {
            // 1. Virtual Thread 정보 출력
            threadManager.printThreadInfo();
            
            // 2. 게임 루프 시뮬레이션
            System.out.println("\n🎮 게임 루프 시뮬레이션 시작...");
            CompletableFuture<Void> gameLoop = threadManager.startGameLoop(() -> {
                // 게임 로직 시뮬레이션
                // System.out.print("."); // 너무 많은 출력 방지
            });
            
            // 3. 블록 낙하 타이머 시뮬레이션
            System.out.println("⬇️ 블록 낙하 타이머 시작...");
            CompletableFuture<Void> dropTimer = threadManager.startBlockDropTimer(() -> {
                System.out.println("📦 블록 한 칸 낙하!");
            }, Duration.ofSeconds(1));
            
            // 4. 사운드 효과 비동기 재생
            System.out.println("\n🎵 사운드 효과 테스트...");
            threadManager.playSoundAsync("block_drop.wav");
            threadManager.playSoundAsync("line_clear.wav");
            threadManager.playSoundAsync("level_up.wav");
            
            // 5. 점수 계산 비동기 처리
            System.out.println("\n🧮 점수 계산 테스트...");
            CompletableFuture<Integer> scoreCalc1 = threadManager.calculateScoreAsync(1, 1);
            CompletableFuture<Integer> scoreCalc2 = threadManager.calculateScoreAsync(4, 5); // Tetris!
            
            scoreCalc1.thenAccept(score -> 
                System.out.println("🎯 1라인 클리어 점수: " + score));
            scoreCalc2.thenAccept(score -> 
                System.out.println("🎯 Tetris 점수: " + score));
            
            // 6. 구조화된 동시성 테스트
            System.out.println("\n🌊 구조화된 동시성 테스트...");
            CompletableFuture<Void> gameTurn = threadManager.processGameTurn(
                () -> System.out.println("  ➡️ 블록 이동"),
                () -> System.out.println("  🔍 충돌 검사"),
                () -> System.out.println("  🎨 UI 업데이트")
            );
            
            // 7. 게임 상태 업데이트 테스트
            System.out.println("\n🔄 게임 상태 업데이트 테스트...");
            threadManager.updateGameState(() -> {
                System.out.println("  📊 게임 상태가 안전하게 업데이트됨");
            });
            
            // 8. 잠시 대기하여 Virtual Thread들이 작업하도록 함
            System.out.println("\n⏰ 5초간 Virtual Threads 작업 관찰...");
            Thread.sleep(5000);
            
            // 9. 게임 종료
            System.out.println("\n🛑 게임 종료 테스트...");
            threadManager.stopGame();
            
            // 10. 모든 CompletableFuture 완료 대기
            CompletableFuture.allOf(gameLoop, dropTimer, gameTurn).join();
            
            System.out.println("\n✅ Java 23 Virtual Threads 테스트 완료!");
            
        } catch (InterruptedException e) {
            System.err.println("❌ 테스트 중단됨: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("❌ 테스트 오류: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 🧵 Virtual Thread vs Platform Thread 성능 비교
     */
    public static void performanceComparison() {
        System.out.println("\n🏁 Virtual Thread vs Platform Thread 성능 비교");
        
        int taskCount = 10000;
        
        // Platform Threads 테스트
        long platformStart = System.currentTimeMillis();
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(100)) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(10); // 블로킹 작업 시뮬레이션
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
        long platformTime = System.currentTimeMillis() - platformStart;
        
        // Virtual Threads 테스트
        long virtualStart = System.currentTimeMillis();
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < taskCount; i++) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(10); // 블로킹 작업 시뮬레이션
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        }
        long virtualTime = System.currentTimeMillis() - virtualStart;
        
        System.out.println("📊 성능 비교 결과 (" + taskCount + "개 작업):");
        System.out.println("  🧵 Platform Threads: " + platformTime + "ms");
        System.out.println("  🚀 Virtual Threads: " + virtualTime + "ms");
        System.out.println("  ⚡ 성능 향상: " + 
                         Math.round(((double)(platformTime - virtualTime) / platformTime) * 100) + "%");
    }
}
