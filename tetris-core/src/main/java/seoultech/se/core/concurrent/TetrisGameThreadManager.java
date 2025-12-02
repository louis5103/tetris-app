package seoultech.se.core.concurrent;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 🧵 Java 23 Virtual Threads와 향상된 concurrent 기능 활용
 * 
 * Java 23의 새로운 기능들:
 * - Virtual Threads (Project Loom)
 * - Structured Concurrency 
 * - Scoped Values
 * - 향상된 synchronized 성능
 */
public class TetrisGameThreadManager {
    
    private final ExecutorService virtualThreadExecutor;
    private final ReentrantLock gameStateLock;
    private volatile boolean gameRunning;
    
    // 🧵 Java 23 Virtual Threads Executor
    public TetrisGameThreadManager() {
        // Virtual Threads를 사용한 Executor 생성
        this.virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.gameStateLock = new ReentrantLock();
        this.gameRunning = false;
    }
    
    /**
     * 🎮 게임 루프를 Virtual Thread로 실행
     */
    public CompletableFuture<Void> startGameLoop(Runnable gameLogic) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("🧵 게임 루프 시작 - Virtual Thread: " + Thread.currentThread());
            
            gameRunning = true;
            while (gameRunning) {
                try {
                    gameLogic.run();
                    
                    // Virtual Thread는 블로킹에 최적화됨
                    Thread.sleep(16); // ~60 FPS
                    
                } catch (InterruptedException e) {
                    System.out.println("🛑 게임 루프 중단됨");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            System.out.println("🎮 게임 루프 종료");
        }, virtualThreadExecutor);
    }
    
    /**
     * 🎯 블록 자동 낙하를 Virtual Thread로 처리
     */
    public CompletableFuture<Void> startBlockDropTimer(Runnable dropLogic, Duration interval) {
        return CompletableFuture.runAsync(() -> {
            System.out.println("⬇️ 블록 낙하 타이머 시작 - Virtual Thread: " + Thread.currentThread());
            
            while (gameRunning) {
                try {
                    Thread.sleep(interval.toMillis());
                    
                    // 🔒 Java 23 향상된 synchronized 성능
                    synchronized (this) {
                        if (gameRunning) {
                            dropLogic.run();
                        }
                    }
                    
                } catch (InterruptedException e) {
                    System.out.println("⏰ 낙하 타이머 중단됨");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, virtualThreadExecutor);
    }
    
    /**
     * 🎵 사운드 효과를 Virtual Thread로 비동기 처리
     */
    public void playSoundAsync(String soundName) {
        virtualThreadExecutor.submit(() -> {
            System.out.println("🎵 사운드 재생: " + soundName + 
                             " - Virtual Thread: " + Thread.currentThread());
            
            try {
                // 사운드 파일 로딩 및 재생 시뮬레이션
                Thread.sleep(100); // Virtual Thread는 블로킹 최적화
                System.out.println("✅ 사운드 재생 완료: " + soundName);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }
    
    /**
     * 🧮 점수 계산을 Virtual Thread로 병렬 처리
     */
    public CompletableFuture<Integer> calculateScoreAsync(int linesCleared, int level) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("🧮 점수 계산 시작 - Virtual Thread: " + Thread.currentThread());
            
            try {
                // 복잡한 점수 계산 시뮬레이션
                Thread.sleep(50);
                
                int baseScore = switch (linesCleared) {
                    case 1 -> 100;
                    case 2 -> 300;
                    case 3 -> 500;
                    case 4 -> 800; // Tetris!
                    default -> 0;
                };
                
                int finalScore = baseScore * level;
                System.out.println("📊 점수 계산 완료: " + finalScore);
                
                return finalScore;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return 0;
            }
        }, virtualThreadExecutor);
    }
    
    /**
     * 🔄 게임 상태를 안전하게 업데이트 (향상된 Lock 성능)
     */
    public void updateGameState(Runnable stateUpdater) {
        // Java 23에서 향상된 ReentrantLock 성능
        gameStateLock.lock();
        try {
            stateUpdater.run();
        } finally {
            gameStateLock.unlock();
        }
    }
    
    /**
     * 🌊 여러 작업을 구조화된 동시성으로 처리 
     * (Java 23 Structured Concurrency 스타일)
     */
    public CompletableFuture<Void> processGameTurn(
            Runnable moveBlock,
            Runnable checkCollision, 
            Runnable updateUI) {
        
        // 여러 작업을 병렬로 수행하되 구조화된 방식으로
        CompletableFuture<Void> moveTask = CompletableFuture.runAsync(moveBlock, virtualThreadExecutor);
        CompletableFuture<Void> collisionTask = CompletableFuture.runAsync(checkCollision, virtualThreadExecutor);
        
        return CompletableFuture.allOf(moveTask, collisionTask)
                .thenRunAsync(updateUI, virtualThreadExecutor);
    }
    
    /**
     * 🛑 게임 종료 및 Virtual Thread 정리
     */
    public void stopGame() {
        System.out.println("🛑 게임 종료 중...");
        
        gameRunning = false;
        
        // Virtual Thread Executor 정리
        virtualThreadExecutor.shutdown();
        try {
            if (!virtualThreadExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                System.out.println("⚠️ Virtual Thread 강제 종료");
                virtualThreadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            virtualThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("✅ 게임 종료 완료");
    }
    
    /**
     * 📊 Virtual Thread 상태 모니터링
     */
    public void printThreadInfo() {
        System.out.println("🧵 현재 스레드 정보:");
        System.out.println("  - 스레드 이름: " + Thread.currentThread().getName());
        System.out.println("  - Virtual Thread 여부: " + Thread.currentThread().isVirtual());
        System.out.println("  - 게임 실행 중: " + gameRunning);
    }
    
    // Getter
    public boolean isGameRunning() {
        return gameRunning;
    }
}
