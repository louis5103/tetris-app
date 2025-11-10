# FINAL_SYSTEM_REQUIREMENTS (Part 2)

**프로젝트**: Tetris Multi-Module Architecture  
**버전**: 6.0 (Production Ready - 최종 점검 완료)  
**작성일**: 2025-11-06  
**최종 업데이트**: 2025-11-06  
**승인 상태**: ✅ 최종 승인 (프로덕션 개발 시작 가능)  
**목적**: Spring Boot 기반 아키텍처 구축 및 디자인 패턴 적용

**📌 이 문서는 Part 1의 연속입니다.**  
**Part 1**: 섹션 1-3 (시스템 요구사항, 변경 파일 목록, 아키텍처 설계)  
**Part 2**: 섹션 4-7 (디자인 패턴, 멀티플레이어 통신, UI 이벤트, 상세 구현)

---

## 📋 목차 (Part 2)

4. [디자인 패턴 적용 (Design Patterns)](#4-디자인-패턴-적용-design-patterns)
5. [멀티플레이어 통신 (Multiplayer)](#5-멀티플레이어-통신-multiplayer)
6. [UI 이벤트 시스템 (UI Events)](#6-ui-이벤트-시스템-ui-events)
7. [모듈별 상세 구현 (Implementation)](#7-모듈별-상세-구현-implementation)

---

## 4. 디자인 패턴 적용 (Design Patterns)

### 4.1 Strategy 패턴 (PlayType 분리)

#### 4.1.1 패턴 개요

**목적**: PlayType (Single/Multi)에 따라 다른 동작을 캡슐화

**구조**:
```
PlayTypeStrategy (Interface)
    ├── SinglePlayStrategy (로컬 전용)
    └── MultiPlayStrategy (서버 통신)
```

**장점**:
- PlayType 추가 시 기존 코드 수정 불필요 (Open/Closed Principle)
- 각 모드의 로직이 독립적으로 관리됨
- 런타임에 전략 변경 가능

---

#### 4.1.2 Interface 정의

```java
package seoultech.se.client.strategy;

import seoultech.se.core.GameState;
import seoultech.se.client.dto.GameCommand;
import seoultech.se.client.mode.PlayType;

/**
 * PlayType별 동작을 정의하는 Strategy Interface
 * 
 * Single 모드: 로컬에서만 실행
 * Multi 모드: 서버 통신 + Reconciliation
 */
public interface PlayTypeStrategy {
    
    /**
     * Command 실행 전 처리
     * 
     * @param command 실행할 Command
     * @return true: 로컬 실행 허용, false: 실행 차단
     */
    boolean beforeCommand(GameCommand command);
    
    /**
     * Command 실행 후 처리
     * 
     * @param command 실행된 Command
     * @param result 실행 결과 GameState
     */
    void afterCommand(GameCommand command, GameState result);
    
    /**
     * 서버 상태 업데이트 수신
     * 
     * @param serverState 서버에서 받은 GameState
     */
    void onServerStateUpdate(GameState serverState);
    
    /**
     * 라인 클리어 발생 시 처리 (공격 전송 등)
     * 
     * @param state 현재 GameState
     */
    void onLineClear(GameState state);
    
    /**
     * 공격 수신 시 처리
     * 
     * @param lines 공격 라인 수
     * @param fromPlayerId 공격자 ID
     */
    void onAttackReceived(int lines, String fromPlayerId);
    
    /**
     * 초기화
     */
    void initialize();
    
    /**
     * 정리 (종료 시)
     */
    void cleanup();
    
    /**
     * PlayType 반환
     * 
     * @return 현재 Strategy의 PlayType
     */
    PlayType getType();
}
```

---

#### 4.1.3 SinglePlayStrategy 구현

```java
package seoultech.se.client.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import seoultech.se.client.dto.GameCommand;
import seoultech.se.client.mode.PlayType;
import seoultech.se.core.GameState;

/**
 * 싱글 플레이 전략
 * 
 * 특징:
 * - 서버 통신 없음
 * - 모든 로직을 로컬에서 처리
 * - beforeCommand()는 항상 true 반환 (차단 없음)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tetris.play-type", havingValue = "LOCAL_SINGLE", matchIfMissing = true)
public class SinglePlayStrategy implements PlayTypeStrategy {
    
    @Override
    public boolean beforeCommand(GameCommand command) {
        // 싱글 플레이는 모든 Command를 로컬에서 즉시 실행
        log.trace("Single mode: executing command locally: {}", command.getCommandType());
        return true;
    }
    
    @Override
    public void afterCommand(GameCommand command, GameState result) {
        // 싱글 플레이는 별도 처리 불필요
        log.trace("Single mode: command completed: {}", command.getCommandType());
    }
    
    @Override
    public void onServerStateUpdate(GameState serverState) {
        // 싱글 플레이는 서버 상태 없음
        log.warn("Single mode: received unexpected server state update");
    }
    
    @Override
    public void onLineClear(GameState state) {
        // 싱글 플레이는 공격 전송 없음
        int lines = state.getLastLinesCleared();
        log.debug("Single mode: cleared {} lines (no attack sent)", lines);
    }
    
    @Override
    public void onAttackReceived(int lines, String fromPlayerId) {
        // 싱글 플레이는 공격 수신 없음
        log.warn("Single mode: received unexpected attack from {}", fromPlayerId);
    }
    
    @Override
    public void initialize() {
        log.info("SinglePlayStrategy initialized");
    }
    
    @Override
    public void cleanup() {
        log.info("SinglePlayStrategy cleanup");
    }
    
    @Override
    public PlayType getType() {
        return PlayType.LOCAL_SINGLE;
    }
}
```

---

#### 4.1.4 MultiPlayStrategy 구현 (핵심)

```java
package seoultech.se.client.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import seoultech.se.client.config.TetrisGameConfig;
import seoultech.se.client.dto.GameCommand;
import seoultech.se.client.exception.StateConflictException;
import seoultech.se.client.mode.PlayType;
import seoultech.se.client.service.NetworkService;
import seoultech.se.core.GameState;
import seoultech.se.core.Tetromino;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 멀티플레이 전략
 * 
 * 특징:
 * - 모든 Command를 서버에 전송
 * - Client-Side Prediction (로컬 예측)
 * - State Reconciliation (서버 상태로 동기화)
 * - Command Throttling (16ms 간격)
 * 
 * Thread-safe:
 * - AtomicInteger sequenceNumber
 * - ConcurrentHashMap pendingCommands
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "tetris.play-type", havingValue = "ONLINE_MULTI")
public class MultiPlayStrategy implements PlayTypeStrategy {
    
    private final NetworkService networkService;
    private final TetrisGameConfig config;
    
    // Thread-safe 시퀀스 번호
    private final AtomicInteger sequenceNumber = new AtomicInteger(0);
    
    // 대기 중인 Command들 (서버 응답 대기)
    private final ConcurrentHashMap<Integer, PendingCommand> pendingCommands = new ConcurrentHashMap<>();
    
    // Command Throttling (16ms = 60 FPS)
    private final ConcurrentHashMap<String, Long> lastSentTime = new ConcurrentHashMap<>();
    private static final long THROTTLE_MS = 16;
    
    // 타임아웃 설정 (5초)
    private static final long TIMEOUT_MS = 5000;
    
    @Autowired
    public MultiPlayStrategy(NetworkService networkService, TetrisGameConfig config) {
        this.networkService = networkService;
        this.config = config;
    }
    
    @Override
    public boolean beforeCommand(GameCommand command) {
        try {
            // Step 1: Throttling 체크
            if (!checkThrottle(command.getCommandType().toString())) {
                log.trace("Command throttled: {}", command.getCommandType());
                return false; // 너무 빠른 전송, 무시
            }
            
            // Step 2: Sequence Number 할당
            int seq = sequenceNumber.getAndIncrement();
            command.setSequenceNumber(seq);
            command.setPlayerId(config.getPlayerId());
            command.setTimestamp(System.currentTimeMillis());
            
            // Step 3: 서버 전송
            networkService.sendCommand(command);
            log.debug("Command sent to server: seq={}, type={}", seq, command.getCommandType());
            
            // Step 4: Pending Commands에 추가
            PendingCommand pending = new PendingCommand(command, System.currentTimeMillis());
            pendingCommands.put(seq, pending);
            
            // Step 5: 로컬 예측 허용
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send command to server", e);
            // 네트워크 오류 시에도 로컬 예측은 허용
            return true;
        }
    }
    
    @Override
    public void afterCommand(GameCommand command, GameState predictedState) {
        // 예측 결과 저장
        int seq = command.getSequenceNumber();
        PendingCommand pending = pendingCommands.get(seq);
        
        if (pending != null) {
            pending.setPredictedState(predictedState);
            log.debug("Predicted state saved: seq={}, score={}", seq, predictedState.getScore());
        }
    }
    
    @Override
    public void onServerStateUpdate(GameState serverState) {
        int serverSeq = serverState.getLastProcessedSequence();
        log.debug("Server state received: seq={}, score={}", serverSeq, serverState.getScore());
        
        // Step 1: 처리된 Commands 제거
        pendingCommands.keySet().removeIf(seq -> seq <= serverSeq);
        
        // Step 2: State Reconciliation (예측 vs 실제 비교)
        PendingCommand processed = pendingCommands.get(serverSeq);
        if (processed != null && processed.getPredictedState() != null) {
            GameState predictedState = processed.getPredictedState();
            
            // Step 3: Mismatch 검사
            if (!statesMatch(predictedState, serverState)) {
                log.warn("❌ State mismatch detected! seq={}", serverSeq);
                log.warn("  Predicted: score={}, lines={}", 
                    predictedState.getScore(), predictedState.getLines());
                log.warn("  Server: score={}, lines={}", 
                    serverState.getScore(), serverState.getLines());
                
                // Step 4: 서버 상태로 강제 동기화
                throw new StateConflictException(
                    "State mismatch at sequence " + serverSeq,
                    serverState
                );
            } else {
                log.debug("✅ State prediction correct: seq={}", serverSeq);
            }
        }
        
        // Step 5: 타임아웃된 Commands 체크
        checkPendingTimeouts();
    }
    
    @Override
    public void onLineClear(GameState state) {
        // 2줄 이상 클리어 시 공격 전송
        int linesCleared = state.getLastLinesCleared();
        
        if (linesCleared >= 2) {
            int attackLines = calculateAttack(linesCleared, state);
            
            if (attackLines > 0) {
                networkService.sendAttack(attackLines);
                log.info("⚔️ Attack sent: {} lines", attackLines);
            }
        }
    }
    
    @Override
    public void onAttackReceived(int lines, String fromPlayerId) {
        log.info("🛡️ Attack received: {} lines from {}", lines, fromPlayerId);
        // BoardController가 실제 처리 (다음 블록 고정 시 바닥에서 줄 추가)
    }
    
    @Override
    public void initialize() {
        log.info("MultiPlayStrategy initialized: playerId={}", config.getPlayerId());
        sequenceNumber.set(0);
        pendingCommands.clear();
        lastSentTime.clear();
    }
    
    @Override
    public void cleanup() {
        log.info("MultiPlayStrategy cleanup");
        pendingCommands.clear();
        lastSentTime.clear();
    }
    
    @Override
    public PlayType getType() {
        return PlayType.ONLINE_MULTI;
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * Command Throttling 체크 (16ms 간격)
     */
    private boolean checkThrottle(String commandType) {
        long now = System.currentTimeMillis();
        Long last = lastSentTime.get(commandType);
        
        if (last != null && (now - last) < THROTTLE_MS) {
            return false; // 너무 빠름
        }
        
        lastSentTime.put(commandType, now);
        return true;
    }
    
    /**
     * 상태 일치 여부 검사
     */
    private boolean statesMatch(GameState predicted, GameState server) {
        // Critical 필드만 비교
        return predicted.getScore() == server.getScore()
            && predicted.getLevel() == server.getLevel()
            && predicted.getLines() == server.getLines()
            && tetrominoMatch(predicted.getCurrentTetromino(), server.getCurrentTetromino());
    }
    
    /**
     * Tetromino 일치 여부
     */
    private boolean tetrominoMatch(Tetromino a, Tetromino b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        
        return a.getType() == b.getType()
            && a.getX() == b.getX()
            && a.getY() == b.getY()
            && a.getRotation() == b.getRotation();
    }
    
    /**
     * 타임아웃된 Commands 체크 및 재전송
     */
    private void checkPendingTimeouts() {
        long now = System.currentTimeMillis();
        
        pendingCommands.entrySet().removeIf(entry -> {
            PendingCommand pending = entry.getValue();
            
            if (now - pending.getSentTime() > TIMEOUT_MS) {
                log.warn("⏱️ Command timeout: seq={}, type={}", 
                    entry.getKey(), pending.getCommand().getCommandType());
                
                // 재전송 (최대 3회)
                if (pending.getRetryCount() < 3) {
                    pending.incrementRetryCount();
                    networkService.sendCommand(pending.getCommand());
                    log.info("Retrying command: seq={}, retry={}", 
                        entry.getKey(), pending.getRetryCount());
                    return false; // 유지
                } else {
                    log.error("❌ Command failed after 3 retries: seq={}", entry.getKey());
                    return true; // 제거
                }
            }
            
            return false;
        });
    }
    
    /**
     * 공격 라인 수 계산
     */
    private int calculateAttack(int linesCleared, GameState state) {
        int attack = 0;
        
        // 기본 공격
        switch (linesCleared) {
            case 2: attack = 1; break;
            case 3: attack = 2; break;
            case 4: attack = 4; break; // Tetris
        }
        
        // T-Spin 보너스
        if (state.isLastLockWasTSpin()) {
            attack += 2;
        }
        
        // Combo 보너스
        int combo = state.getComboCount();
        if (combo > 0) {
            attack += Math.min(combo / 2, 3); // 최대 +3
        }
        
        // Back-to-Back 보너스
        if (state.getBackToBackCount() > 0) {
            attack += 1;
        }
        
        return attack;
    }
    
    // ========== Inner Class ==========
    
    /**
     * Pending Command DTO (대기 중인 Command)
     */
    private static class PendingCommand {
        private final GameCommand command;
        private final long sentTime;
        private GameState predictedState;
        private int retryCount = 0;
        
        public PendingCommand(GameCommand command, long sentTime) {
            this.command = command;
            this.sentTime = sentTime;
        }
        
        public GameCommand getCommand() {
            return command;
        }
        
        public long getSentTime() {
            return sentTime;
        }
        
        public GameState getPredictedState() {
            return predictedState;
        }
        
        public void setPredictedState(GameState predictedState) {
            this.predictedState = predictedState;
        }
        
        public int getRetryCount() {
            return retryCount;
        }
        
        public void incrementRetryCount() {
            this.retryCount++;
        }
    }
}
```

---

### 4.2 Proxy 패턴 (네트워크 재연결)

#### 4.2.1 패턴 개요

**목적**: 네트워크 장애 시 자동 재연결 및 오프라인 큐잉

**구조**:
```
NetworkService (Interface)
    ├── NetworkServiceImpl (실제 통신)
    └── NetworkServiceProxy (@Primary, 래퍼)
```

**장점**:
- 네트워크 장애에 대한 투명한 처리
- 오프라인 큐잉으로 데이터 손실 방지
- 5초 간격 자동 재연결

---

#### 4.2.2 NetworkService Interface

```java
package seoultech.se.client.service;

import seoultech.se.client.dto.GameCommand;

/**
 * 네트워크 통신 인터페이스
 */
public interface NetworkService {
    
    /**
     * Command 전송
     */
    void sendCommand(GameCommand command);
    
    /**
     * 공격 전송
     */
    void sendAttack(int attackLines);
    
    /**
     * 서버 연결 확인 (Ping)
     */
    void ping();
}
```

---

#### 4.2.3 NetworkServiceProxy 구현 (핵심)

```java
package seoultech.se.client.proxy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import seoultech.se.client.config.TetrisGameConfig;
import seoultech.se.client.dto.GameCommand;
import seoultech.se.client.exception.NetworkException;
import seoultech.se.client.service.NetworkService;

import javax.annotation.PreDestroy;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * NetworkService Proxy (자동 재연결 + 오프라인 큐잉)
 * 
 * 특징:
 * - 네트워크 장애 시 자동으로 오프라인 모드 전환
 * - 5초 간격으로 자동 재연결 시도
 * - 오프라인 큐에 최대 1000개 항목 저장
 * - 재연결 성공 시 자동으로 큐 Flush
 * 
 * Thread-safe:
 * - AtomicBoolean connected
 * - ConcurrentLinkedQueue offlineQueue
 */
@Slf4j
@Service
@Primary
public class NetworkServiceProxy implements NetworkService {
    
    private final NetworkService realService;
    private final TetrisGameConfig config;
    
    // Thread-safe 연결 상태
    private final AtomicBoolean connected = new AtomicBoolean(false);
    
    // 오프라인 큐 (Thread-safe)
    private final ConcurrentLinkedQueue<QueuedItem> offlineQueue = new ConcurrentLinkedQueue<>();
    
    // 재연결 스케줄러
    private final ScheduledExecutorService reconnectScheduler = 
        Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> reconnectTask;
    
    // 설정
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final long RECONNECT_INTERVAL_MS = 5000; // 5초
    
    @Autowired
    public NetworkServiceProxy(
        @Qualifier("networkServiceImpl") NetworkService realService,
        TetrisGameConfig config
    ) {
        this.realService = realService;
        this.config = config;
        
        // 초기 연결 시도
        checkConnection();
    }
    
    @Override
    public void sendCommand(GameCommand command) {
        if (connected.get()) {
            try {
                realService.sendCommand(command);
                log.trace("Command sent: {}", command.getCommandType());
            } catch (NetworkException e) {
                log.error("Failed to send command", e);
                handleDisconnection();
                queueItem(new QueuedItem(QueuedItemType.COMMAND, command));
            }
        } else {
            log.debug("Offline - queuing command: {}", command.getCommandType());
            queueItem(new QueuedItem(QueuedItemType.COMMAND, command));
        }
    }
    
    @Override
    public void sendAttack(int attackLines) {
        if (connected.get()) {
            try {
                realService.sendAttack(attackLines);
                log.debug("Attack sent: {} lines", attackLines);
            } catch (NetworkException e) {
                log.error("Failed to send attack", e);
                handleDisconnection();
                queueItem(new QueuedItem(QueuedItemType.ATTACK, attackLines));
            }
        } else {
            log.debug("Offline - queuing attack: {} lines", attackLines);
            queueItem(new QueuedItem(QueuedItemType.ATTACK, attackLines));
        }
    }
    
    @Override
    public void ping() {
        try {
            realService.ping();
            
            // 재연결 성공!
            if (!connected.get()) {
                log.info("✅ Reconnected to server");
                connected.set(true);
                stopReconnectTask();
                flushOfflineQueue();
            }
            
        } catch (NetworkException e) {
            // 연결 끊김 감지
            if (connected.get()) {
                log.warn("⚠️ Lost connection to server");
                handleDisconnection();
            }
        }
    }
    
    // ========== Private Helper Methods ==========
    
    /**
     * 초기 연결 체크
     */
    private void checkConnection() {
        try {
            realService.ping();
            connected.set(true);
            log.info("✅ Connected to server: {}", config.getNetwork().getServerUrl());
        } catch (Exception e) {
            log.warn("⚠️ Failed to connect to server", e);
            handleDisconnection();
        }
    }
    
    /**
     * 연결 끊김 처리
     */
    private void handleDisconnection() {
        if (connected.compareAndSet(true, false)) {
            log.error("❌ Disconnected from server - entering offline mode");
            startReconnectTask();
        }
    }
    
    /**
     * 재연결 태스크 시작
     */
    private void startReconnectTask() {
        if (reconnectTask == null || reconnectTask.isDone()) {
            log.info("🔄 Starting reconnect task (every {}ms)", RECONNECT_INTERVAL_MS);
            
            reconnectTask = reconnectScheduler.scheduleAtFixedRate(
                this::ping,
                RECONNECT_INTERVAL_MS,
                RECONNECT_INTERVAL_MS,
                TimeUnit.MILLISECONDS
            );
        }
    }
    
    /**
     * 재연결 태스크 중지
     */
    private void stopReconnectTask() {
        if (reconnectTask != null && !reconnectTask.isDone()) {
            log.info("⏹️ Stopping reconnect task");
            reconnectTask.cancel(false);
            reconnectTask = null;
        }
    }
    
    /**
     * 항목 큐잉 (크기 제한 적용)
     */
    private void queueItem(QueuedItem item) {
        if (offlineQueue.size() >= MAX_QUEUE_SIZE) {
            // 큐가 가득 찼으면 가장 오래된 항목 제거
            QueuedItem removed = offlineQueue.poll();
            log.warn("⚠️ Offline queue full - removed oldest item: {}", removed);
        }
        
        offlineQueue.offer(item);
        log.debug("Queued item: {} (queue size: {})", item.getType(), offlineQueue.size());
    }
    
    /**
     * 오프라인 큐 Flush (재연결 시)
     */
    private void flushOfflineQueue() {
        int flushedCount = 0;
        
        while (!offlineQueue.isEmpty()) {
            QueuedItem item = offlineQueue.poll();
            
            try {
                if (item.getType() == QueuedItemType.COMMAND) {
                    realService.sendCommand((GameCommand) item.getData());
                } else if (item.getType() == QueuedItemType.ATTACK) {
                    realService.sendAttack((Integer) item.getData());
                }
                
                flushedCount++;
                
            } catch (NetworkException e) {
                log.error("Failed to flush queued item", e);
                // 다시 큐에 넣기
                offlineQueue.offer(item);
                break; // 더 이상 시도하지 않음
            }
        }
        
        log.info("📤 Flushed {} items from offline queue", flushedCount);
    }
    
    /**
     * 연결 상태 확인
     */
    public boolean isConnected() {
        return connected.get();
    }
    
    /**
     * 오프라인 큐 크기
     */
    public int getQueueSize() {
        return offlineQueue.size();
    }
    
    /**
     * 종료 시 정리
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down NetworkServiceProxy");
        stopReconnectTask();
        reconnectScheduler.shutdownNow();
        offlineQueue.clear();
    }
    
    // ========== Inner Classes ==========
    
    /**
     * 큐잉된 항목 타입
     */
    private enum QueuedItemType {
        COMMAND,
        ATTACK
    }
    
    /**
     * 큐잉된 항목 DTO
     */
    private static class QueuedItem {
        private final QueuedItemType type;
        private final Object data;
        
        public QueuedItem(QueuedItemType type, Object data) {
            this.type = type;
            this.data = data;
        }
        
        public QueuedItemType getType() {
            return type;
        }
        
        public Object getData() {
            return data;
        }
        
        @Override
        public String toString() {
            return "QueuedItem{type=" + type + ", data=" + data + "}";
        }
    }
}
```

---

### 4.3 Observer 패턴 (UI 이벤트 시스템)

#### 4.3.1 패턴 개요

**목적**: UI 이벤트를 비동기적으로 순차 처리

**구조**:
```
UIEventHandler (Observer)
    ├── PriorityQueue<UIEvent> (우선순위 큐)
    └── ScheduledExecutorService (스케줄러)
```

**특징**:
- 우선순위 기반 순차 표시
- 각 이벤트마다 duration 설정
- Thread-safe (AtomicBoolean + synchronized)

---

#### 4.3.2 UIEvent DTO

```java
package seoultech.se.client.event;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

/**
 * UI 이벤트 DTO
 */
@Data
@Builder
public class UIEvent {
    
    /**
     * 이벤트 타입
     */
    private UIEventType type;
    
    /**
     * 우선순위 (높을수록 먼저 표시)
     */
    private int priority;
    
    /**
     * 표시 시간 (ms)
     */
    private long duration;
    
    /**
     * 생성 시간
     */
    private long timestamp;
    
    /**
     * 시퀀스 ID (순서 보장용)
     */
    private int sequenceId;
    
    /**
     * 이벤트 데이터
     */
    private Map<String, Object> data;
}
```

---

#### 4.3.3 UIEventType Enum

```java
package seoultech.se.client.event;

/**
 * UI 이벤트 타입
 */
public enum UIEventType {
    // Critical Events (서버 생성)
    LINE_CLEAR(15, 800),
    T_SPIN(14, 1000),
    COMBO(12, 600),
    LEVEL_UP(13, 1200),
    PERFECT_CLEAR(16, 2000),
    GAME_OVER(20, 3000),
    
    // Multiplayer Events (서버 생성)
    ATTACK_SENT(10, 500),
    ATTACK_RECEIVED(10, 1000),
    
    // Local Events (클라이언트 생성)
    BLOCK_MOVE(1, 50),
    BLOCK_ROTATE(1, 50),
    BLOCK_LOCK(5, 100),
    GHOST_PIECE_UPDATE(1, 50),
    HOLD_SWAP(5, 200);
    
    private final int defaultPriority;
    private final long defaultDuration;
    
    UIEventType(int defaultPriority, long defaultDuration) {
        this.defaultPriority = defaultPriority;
        this.defaultDuration = defaultDuration;
    }
    
    public int getDefaultPriority() {
        return defaultPriority;
    }
    
    public long getDefaultDuration() {
        return defaultDuration;
    }
}
```

---

#### 4.3.4 UIEventHandler 구현 (Thread-safe)

```java
package seoultech.se.client.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * UI 이벤트 핸들러 (Thread-safe)
 * 
 * 특징:
 * - 우선순위 기반 순차 표시
 * - 비동기 스케줄링
 * - Race Condition 방지 (AtomicBoolean + synchronized)
 * 
 * Thread-safe 구현:
 * 1. AtomicBoolean isProcessing (CAS 패턴)
 * 2. synchronized (lock) { eventQueue.addAll() }
 * 3. 단일 스레드 스케줄러
 */
@Slf4j
@Component
public class UIEventHandler {
    
    // Thread-safe 처리 상태
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    
    // 우선순위 큐 (높은 우선순위 먼저)
    private final PriorityQueue<UIEvent> eventQueue = new PriorityQueue<>(
        Comparator.comparingInt(UIEvent::getPriority).reversed()
            .thenComparingInt(UIEvent::getSequenceId)
    );
    
    // synchronized용 Lock 객체
    private final Object lock = new Object();
    
    // 스케줄러 (단일 스레드)
    private final ScheduledExecutorService scheduler = 
        Executors.newSingleThreadScheduledExecutor();
    
    /**
     * 단일 이벤트 처리
     */
    public void handle(UIEvent event) {
        handleEvents(List.of(event));
    }
    
    /**
     * 다중 이벤트 처리 (서버에서 받은 Critical Events)
     */
    public void handleEvents(List<UIEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        
        // Step 1: Queue에 추가 (synchronized로 동기화)
        synchronized (lock) {
            eventQueue.addAll(events);
            log.debug("Added {} events to queue. Total: {}", events.size(), eventQueue.size());
        }
        
        // Step 2: 처리 시작 (CAS 패턴으로 Race Condition 방지)
        if (isProcessing.compareAndSet(false, true)) {
            log.debug("Starting event processing");
            processNextEvent();
        } else {
            log.debug("Event processing already in progress");
        }
    }
    
    /**
     * 다음 이벤트 처리 (재귀 스케줄링)
     */
    private void processNextEvent() {
        UIEvent event;
        
        // Step 1: Queue에서 꺼내기 (synchronized)
        synchronized (lock) {
            event = eventQueue.poll();
            
            if (event == null) {
                // 더 이상 처리할 이벤트 없음
                isProcessing.set(false);
                log.debug("Event processing completed");
                return;
            }
        }
        
        // Step 2: 이벤트 표시
        displayEvent(event);
        
        // Step 3: 다음 이벤트 스케줄링
        long duration = event.getDuration();
        scheduler.schedule(
            this::processNextEvent,
            duration,
            TimeUnit.MILLISECONDS
        );
        
        log.debug("Scheduled next event after {}ms", duration);
    }
    
    /**
     * 이벤트 표시 (실제 UI 업데이트)
     */
    private void displayEvent(UIEvent event) {
        log.info("Displaying event: type={}, priority={}, duration={}ms", 
            event.getType(), event.getPriority(), event.getDuration());
        
        // JavaFX Platform.runLater()로 UI 스레드에서 안전하게 업데이트
        // 실제 이벤트 렌더링은 BoardController에서 처리
        Platform.runLater(() -> {
            eventPublisher.publishEvent(event);
        });
    }
    
    /**
     * 큐 크기 확인
     */
    public int getQueueSize() {
        synchronized (lock) {
            return eventQueue.size();
        }
    }
    
    /**
     * 큐 비우기
     */
    public void clearQueue() {
        synchronized (lock) {
            eventQueue.clear();
            log.info("Event queue cleared");
        }
    }
    
    /**
     * 종료 시 정리
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down UIEventHandler");
        scheduler.shutdownNow();
        clearQueue();
    }
}
```

---

### 4.4 패턴 적용 요약

| 패턴 | 위치 | 목적 | 핵심 클래스 |
|------|------|------|------------|
| **Strategy** | Client | PlayType 분리 | PlayTypeStrategy, SinglePlayStrategy, MultiPlayStrategy |
| **Proxy** | Client | 네트워크 재연결 | NetworkServiceProxy |
| **Observer** | Client | UI 이벤트 처리 | UIEventHandler |
| **Factory** | Core | GameEngine 생성 | GameEngineFactory (선택) |
| **Builder** | Core | 불변 객체 생성 | GameState.Builder |

---

## 5. 멀티플레이어 통신 (Multiplayer)

### 5.1 통신 프로토콜

#### 5.1.1 Command 전송 (Client → Server)

**엔드포인트**: `POST /api/game/command`

**Request Body**:
```json
{
  "commandType": "MOVE_LEFT",
  "sequenceNumber": 42,
  "playerId": "player123",
  "timestamp": 1730899200000
}
```

**Request Headers**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

---

#### 5.1.2 GameUpdateResponse (Server → Client)

**Response Body**:
```json
{
  "success": true,
  "sequenceNumber": 42,
  "timestamp": 1730899200050,
  "state": {
    "score": 1200,
    "level": 3,
    "lines": 15,
    "currentTetromino": {
      "type": "T",
      "x": 4,
      "y": 10,
      "rotation": 0
    },
    "grid": [[0, 0, ...], ...],
    "nextPieces": [...],
    "lastProcessedSequence": 42
  },
  "events": [
    {
      "type": "LINE_CLEAR",
      "priority": 15,
      "duration": 800,
      "sequenceId": 100,
      "timestamp": 1730899200050,
      "data": {
        "lines": 4,
        "score": 800
      }
    }
  ]
}
```

---

#### 5.1.3 Attack 전송 (Client → Server)

**엔드포인트**: `POST /api/game/attack`

**Request Body**:
```json
{
  "attackLines": 4,
  "fromPlayerId": "player123",
  "toPlayerId": "player456",
  "timestamp": 1730899200100
}
```

---

### 5.2 WebSocket (Server Push)

#### 5.2.1 연결 설정

**엔드포인트**: `ws://localhost:8080/ws/game`

**프로토콜**: STOMP over WebSocket

**구독 토픽**:
- `/topic/game/{playerId}`: 개인 이벤트
- `/topic/game/global`: 전체 공지

---

#### 5.2.2 Attack Event (Server Push)

```json
{
  "type": "ATTACK_RECEIVED",
  "priority": 10,
  "duration": 1000,
  "timestamp": 1730899200200,
  "data": {
    "lines": 2,
    "from": "player456"
  }
}
```

---

### 5.3 Client-Side Prediction 흐름

#### 5.3.1 시퀀스 다이어그램

```
[User Input]
     │
     ▼
[BoardController.executeCommand()]
     │
     ├─→ beforeCommand() ─→ 서버 전송 (비동기)
     │
     ├─→ Local Event 생성 ─→ 즉시 표시 ⚡
     │
     ├─→ gameEngine.execute() ─→ 로컬 예측
     │
     ├─→ afterCommand() ─→ 예측 결과 저장
     │
     └─→ renderState() ─→ UI 업데이트 ⚡

[비동기 서버 응답]
     │
     ▼
[onServerUpdate()]
     │
     ├─→ onServerStateUpdate() ─→ Reconciliation
     │
     ├─→ handleEvents() ─→ Critical Events 표시
     │
     └─→ renderState() ─→ 최종 동기화
```

**전체 시간**: ~150ms (사용자는 즉시 반응 느낌)

---

### 5.4 State Reconciliation 알고리즘

#### 5.4.1 Mismatch 검사

```java
private boolean statesMatch(GameState predicted, GameState server) {
    // Critical 필드만 비교
    return predicted.getScore() == server.getScore()
        && predicted.getLevel() == server.getLevel()
        && predicted.getLines() == server.getLines()
        && tetrominoMatch(predicted.getCurrentTetromino(), 
                         server.getCurrentTetromino());
}
```

---

#### 5.4.2 Mismatch 발생 시 처리

```
1. 로그 기록
   ❌ State mismatch detected! seq=42
   Predicted: score=1200, lines=15
   Server: score=1150, lines=14

2. StateConflictException 발생

3. BoardController.forceStateUpdate()
   - 서버 상태로 강제 동기화
   - UI 업데이트

4. Pending Commands 재실행 (선택)
   - seq > 42인 Commands
   - 서버 상태 기준으로 재예측
```

---

### 5.5 Command Throttling (16ms)

#### 5.5.1 필요성

**문제**: 100명이 60 FPS로 Command 전송 시
- 요청 수: 100명 × 60 req/s = **6,000 req/s**
- 서버 부하 과다

**해결**: Throttling (16ms 간격)
- 실제 전송: 100명 × 3.75 req/s = **375 req/s**
- 부하 감소: **94%**

---

#### 5.5.2 구현

```java
private boolean checkThrottle(String commandType) {
    long now = System.currentTimeMillis();
    Long last = lastSentTime.get(commandType);
    
    if (last != null && (now - last) < THROTTLE_MS) {
        return false; // 너무 빠름, 무시
    }
    
    lastSentTime.put(commandType, now);
    return true; // 전송 허용
}
```

---

### 5.6 네트워크 오류 처리

#### 5.6.1 오류 타입

| 오류 | HTTP 상태 | 처리 방식 |
|------|-----------|----------|
| **Timeout** | 408 | 재전송 (최대 3회) |
| **Unauthorized** | 401 | 재로그인 요구 |
| **State Conflict** | 409 | 서버 상태로 동기화 |
| **Rate Limit** | 429 | Throttling 강화 |
| **Server Error** | 500 | 오프라인 큐잉 |

---

#### 5.6.2 Graceful Degradation

```java
try {
    networkService.sendCommand(command);
} catch (NetworkException e) {
    log.error("Network error", e);
    
    // 1. 오프라인 큐에 저장
    offlineQueue.offer(command);
    
    // 2. 사용자에게 알림
    showNotification("네트워크 연결이 끊어졌습니다. 오프라인 모드로 전환합니다.");
    
    // 3. 자동 재연결 시도
    startReconnectTask();
    
    // 4. 로컬 예측은 계속 허용
    return true;
}
```

---

### 5.7 멀티플레이어 공격 시스템

#### 5.7.1 공격 계산

```java
private int calculateAttack(int linesCleared, GameState state) {
    int attack = 0;
    
    // 기본 공격
    switch (linesCleared) {
        case 2: attack = 1; break;
        case 3: attack = 2; break;
        case 4: attack = 4; break; // Tetris
    }
    
    // T-Spin 보너스 (+2)
    if (state.isLastLockWasTSpin()) {
        attack += 2;
    }
    
    // Combo 보너스 (최대 +3)
    int combo = state.getComboCount();
    if (combo > 0) {
        attack += Math.min(combo / 2, 3);
    }
    
    // Back-to-Back 보너스 (+1)
    if (state.getBackToBackCount() > 0) {
        attack += 1;
    }
    
    return attack;
}
```

---

#### 5.7.2 공격 예시

| 상황 | 라인 | T-Spin | Combo | B2B | 총 공격 |
|------|------|--------|-------|-----|---------|
| 2줄 클리어 | 1 | - | - | - | **1** |
| 3줄 클리어 | 2 | - | - | - | **2** |
| Tetris | 4 | - | - | - | **4** |
| T-Spin Double | 1 | +2 | - | - | **3** |
| Tetris + 5 Combo | 4 | - | +2 | - | **6** |
| Tetris + B2B | 4 | - | - | +1 | **5** |
| T-Spin Triple + 10 Combo + B2B | 2 | +2 | +3 | +1 | **8** |

**최대 공격**: T-Spin Triple + 10+ Combo + B2B = **8줄**

---

### 5.8 성능 지표

#### 5.8.1 응답 시간 목표

| 작업 | 평균 | 최대 | 측정 방법 |
|------|------|------|----------|
| Command 전송 | <50ms | <100ms | @Measured |
| State Update | <100ms | <200ms | @Measured |
| Local Event 표시 | <50ms | N/A | Stopwatch |

---

#### 5.8.2 처리량 목표

- **동시 접속**: 1000명
- **총 요청 수**: 375 req/s (Throttling 적용 시)
- **에러율**: <1%

---

## 6. UI 이벤트 시스템 (UI Events)

### 6.1 Hybrid 방식 (Critical + Local)

#### 6.1.1 이벤트 분류

| 타입 | 생성 위치 | 특징 | 예시 |
|------|----------|------|------|
| **Critical Events** | 서버 | 점수 계산 포함, 일관성 보장 | LINE_CLEAR, T_SPIN, LEVEL_UP |
| **Local Events** | 클라이언트 | 즉시 피드백, 점수 계산 없음 | BLOCK_MOVE, BLOCK_ROTATE |

---

#### 6.1.2 설계 근거

**Q: 왜 두 가지로 나누는가?**

**A**: 성능 + 일관성 균형
- **Critical Events**: 서버에서 생성하여 멀티플레이어 간 동기화
- **Local Events**: 클라이언트에서 즉시 생성하여 반응성 향상

**트레이드오프**:
- ✅ 장점: 즉시 반응 + 일관성 보장
- ❌ 단점: 시스템 복잡도 증가

---

### 6.2 이벤트 우선순위

#### 6.2.1 우선순위 정의

```
GAME_OVER(20)        ← 최고 우선순위
  ↓
PERFECT_CLEAR(16)
  ↓
LINE_CLEAR(15)
  ↓
T_SPIN(14)
  ↓
LEVEL_UP(13)
  ↓
COMBO(12)
  ↓
ATTACK_SENT(10)
ATTACK_RECEIVED(10)
  ↓
BLOCK_LOCK(5)
HOLD_SWAP(5)
  ↓
BLOCK_MOVE(1)        ← 최저 우선순위
BLOCK_ROTATE(1)
GHOST_PIECE_UPDATE(1)
```

---

#### 6.2.2 우선순위 적용 시나리오

**시나리오**: Hard Drop으로 4줄 클리어 + Level Up

```
생성된 이벤트:
1. BLOCK_LOCK (우선순위 5)
2. LINE_CLEAR (우선순위 15) ← 서버 생성
3. LEVEL_UP (우선순위 13) ← 서버 생성

표시 순서:
1. LINE_CLEAR (우선순위 15) ← 먼저 표시
2. LEVEL_UP (우선순위 13)
3. BLOCK_LOCK (우선순위 5) ← 마지막 표시
```

---

### 6.3 순차 표시 알고리즘

#### 6.3.1 PriorityQueue 사용

```java
// 우선순위 큐 정의
private final PriorityQueue<UIEvent> eventQueue = new PriorityQueue<>(
    Comparator.comparingInt(UIEvent::getPriority).reversed()  // 높은 순
        .thenComparingInt(UIEvent::getSequenceId)              // 같으면 순서대로
);
```

---

#### 6.3.2 처리 흐름

```
1. handleEvents(List<UIEvent> events)
   │
   ├─→ synchronized (lock) {
   │     eventQueue.addAll(events);
   │   }
   │
   └─→ if (isProcessing.compareAndSet(false, true)) {
         processNextEvent();
       }

2. processNextEvent()
   │
   ├─→ event = eventQueue.poll()
   │
   ├─→ displayEvent(event)
   │
   └─→ scheduler.schedule(
         processNextEvent,
         event.getDuration(),
         MILLISECONDS
       )
```

**핵심**: 재귀 스케줄링으로 순차 실행

---

### 6.4 Critical Events 상세

#### 6.4.1 LINE_CLEAR Event

```json
{
  "type": "LINE_CLEAR",
  "priority": 15,
  "duration": 800,
  "data": {
    "lines": 4,
    "score": 800,
    "level": 3
  }
}
```

**애니메이션**: 노란색 텍스트 페이드아웃 (0.8초)

---

#### 6.4.2 T_SPIN Event

```json
{
  "type": "T_SPIN",
  "priority": 14,
  "duration": 1000,
  "data": {
    "spinType": "full",
    "bonus": 400,
    "lines": 2
  }
}
```

**애니메이션**: 마젠타 텍스트 회전 + 페이드아웃 (1초)

---

#### 6.4.3 COMBO Event

```json
{
  "type": "COMBO",
  "priority": 12,
  "duration": 600,
  "data": {
    "combo": 5,
    "bonus": 750
  }
}
```

**애니메이션**: 오렌지 텍스트 확대 + 페이드아웃 (0.6초)

---

#### 6.4.4 LEVEL_UP Event

```json
{
  "type": "LEVEL_UP",
  "priority": 13,
  "duration": 1200,
  "data": {
    "newLevel": 4,
    "requiredLines": 40
  }
}
```

**애니메이션**: 시안 텍스트 상승 + 페이드인 (1.2초)

---

#### 6.4.5 PERFECT_CLEAR Event

```json
{
  "type": "PERFECT_CLEAR",
  "priority": 16,
  "duration": 2000,
  "data": {
    "bonus": 9000,
    "level": 3
  }
}
```

**애니메이션**: 금색 텍스트 폭발 효과 + 회전 (2초)

---

### 6.5 Local Events 상세

#### 6.5.1 BLOCK_MOVE Event

```json
{
  "type": "BLOCK_MOVE",
  "priority": 1,
  "duration": 50,
  "data": {
    "direction": "LEFT",
    "newX": 4
  }
}
```

**애니메이션**: 즉시 렌더링 (50ms)

---

#### 6.5.2 BLOCK_ROTATE Event

```json
{
  "type": "BLOCK_ROTATE",
  "priority": 1,
  "duration": 50,
  "data": {
    "direction": "CW",
    "newRotation": 1
  }
}
```

**애니메이션**: 회전 애니메이션 (50ms)

---

#### 6.5.3 BLOCK_LOCK Event

```json
{
  "type": "BLOCK_LOCK",
  "priority": 5,
  "duration": 100,
  "data": {
    "x": 4,
    "y": 18
  }
}
```

**애니메이션**: 블록 고정 플래시 (100ms)

---

### 6.6 이벤트 생성자

#### 6.6.1 CriticalEventGenerator (서버)

```java
@Component
public class CriticalEventGenerator {
    
    private final AtomicInteger eventSequenceId = new AtomicInteger(0);
    
    public List<UIEvent> generate(GameState oldState, GameState newState) {
        List<UIEvent> events = new ArrayList<>();
        
        // 1. Line Clear
        if (newState.getLastLinesCleared() > 0) {
            events.add(generateLineClearEvent(newState));
        }
        
        // 2. T-Spin
        if (newState.isLastLockWasTSpin()) {
            events.add(generateTSpinEvent(newState));
        }
        
        // 3. Combo
        if (newState.getComboCount() > 0) {
            events.add(generateComboEvent(newState));
        }
        
        // 4. Level Up
        if (newState.getLevel() > oldState.getLevel()) {
            events.add(generateLevelUpEvent(newState));
        }
        
        // 5. Perfect Clear
        if (newState.isLastIsPerfectClear()) {
            events.add(generatePerfectClearEvent(newState));
        }
        
        return events;
    }
    
    private UIEvent generateLineClearEvent(GameState state) {
        return UIEvent.builder()
            .type(UIEventType.LINE_CLEAR)
            .priority(15)
            .duration(800)
            .sequenceId(eventSequenceId.getAndIncrement())
            .timestamp(System.currentTimeMillis())
            .data(Map.of(
                "lines", state.getLastLinesCleared(),
                "score", calculateScore(state),
                "level", state.getLevel()
            ))
            .build();
    }
    
    // ... 나머지 생성 메서드
}
```

---

#### 6.6.2 LocalUIEventGenerator (클라이언트)

```java
@Component
public class LocalUIEventGenerator {
    
    private final AtomicInteger eventSequenceId = new AtomicInteger(0);
    
    public UIEvent generateLocalEvent(GameCommand command, GameState state) {
        switch (command.getCommandType()) {
            case MOVE_LEFT:
            case MOVE_RIGHT:
                return generateMoveEvent(command, state);
                
            case ROTATE_CW:
            case ROTATE_CCW:
                return generateRotateEvent(command, state);
                
            case HARD_DROP:
            case SOFT_DROP:
                return generateLockEvent(command, state);
                
            default:
                return null;
        }
    }
    
    private UIEvent generateMoveEvent(GameCommand command, GameState state) {
        return UIEvent.builder()
            .type(UIEventType.BLOCK_MOVE)
            .priority(1)
            .duration(50)
            .sequenceId(eventSequenceId.getAndIncrement())
            .timestamp(System.currentTimeMillis())
            .data(Map.of(
                "direction", command.getCommandType().toString(),
                "newX", state.getCurrentTetromino().getX()
            ))
            .build();
    }
    
    // ... 나머지 생성 메서드
}
```

---

### 6.7 Thread-safe 보장

#### 6.7.1 동시성 문제

**문제**: 여러 스레드에서 동시에 이벤트 추가 시 Race Condition

```java
// ❌ Thread-unsafe
private boolean isProcessing = false;

public void handleEvents(List<UIEvent> events) {
    eventQueue.addAll(events);  // Race Condition!
    
    if (!isProcessing) {         // Race Condition!
        isProcessing = true;
        processNextEvent();
    }
}
```

---

#### 6.7.2 해결책

```java
// ✅ Thread-safe
private final AtomicBoolean isProcessing = new AtomicBoolean(false);
private final Object lock = new Object();

public void handleEvents(List<UIEvent> events) {
    // 1. Queue 접근 시 synchronized
    synchronized (lock) {
        eventQueue.addAll(events);
    }
    
    // 2. CAS 패턴으로 원자적 상태 변경
    if (isProcessing.compareAndSet(false, true)) {
        processNextEvent();
    }
}
```

---

## 7. 모듈별 상세 구현 (Implementation)

### 7.1 tetris-core 모듈

#### 7.1.1 GameEngine Interface

```java
package seoultech.se.core;

/**
 * 게임 엔진 인터페이스 (다형성 지원)
 * 
 * 구현:
 * - ClassicGameEngine: 표준 테트리스
 * - ArcadeGameEngine: 아이템 테트리스
 */
public interface GameEngine {
    
    /**
     * 테트로미노를 그리드에 고정
     * 
     * @param state 현재 GameState
     * @return 업데이트된 GameState (라인 클리어, 점수 계산 포함)
     */
    GameState lockTetromino(GameState state);
    
    /**
     * 왼쪽 이동 시도
     */
    GameState tryMoveLeft(GameState state);
    
    /**
     * 오른쪽 이동 시도
     */
    GameState tryMoveRight(GameState state);
    
    /**
     * 회전 시도
     */
    GameState tryRotate(GameState state, RotationDirection direction);
    
    /**
     * 소프트 드롭 (한 칸 아래로)
     */
    GameState softDrop(GameState state);
    
    /**
     * 하드 드롭 (바닥까지)
     */
    GameState hardDrop(GameState state);
    
    /**
     * Hold (보관)
     */
    GameState hold(GameState state);
}
```

---

#### 7.1.2 ClassicGameEngine 구현

```java
package seoultech.se.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 표준 테트리스 엔진
 */
@Component
@ConditionalOnProperty(name = "tetris.game.item.enabled", havingValue = "false")
public class ClassicGameEngine implements GameEngine {
    
    @Override
    public GameState lockTetromino(GameState state) {
        // 1. 테트로미노를 그리드에 고정
        GameState locked = fixTetrominoToGrid(state);
        
        // 2. 라인 클리어 체크
        locked = checkAndClearLines(locked);
        
        // 3. 다음 테트로미노 생성
        locked = spawnNextTetromino(locked);
        
        // 4. Game Over 체크
        if (isGameOver(locked)) {
            locked = locked.toBuilder().gameOver(true).build();
        }
        
        return locked;
    }
    
    @Override
    public GameState tryMoveLeft(GameState state) {
        Tetromino current = state.getCurrentTetromino();
        Tetromino moved = current.moveLeft();
        
        if (isValid(moved, state.getGrid())) {
            return state.toBuilder()
                .currentTetromino(moved)
                .build();
        }
        
        return state; // 이동 불가
    }
    
    @Override
    public GameState tryMoveRight(GameState state) {
        Tetromino current = state.getCurrentTetromino();
        Tetromino moved = current.moveRight();
        
        if (isValid(moved, state.getGrid())) {
            return state.toBuilder()
                .currentTetromino(moved)
                .build();
        }
        
        return state; // 이동 불가
    }
    
    @Override
    public GameState tryRotate(GameState state, RotationDirection direction) {
        Tetromino current = state.getCurrentTetromino();
        Tetromino rotated = current.rotate(direction);
        
        // SRS (Super Rotation System) 적용
        Tetromino adjusted = applySRS(rotated, state.getGrid());
        
        if (adjusted != null) {
            // T-Spin 체크
            boolean isTSpin = checkTSpin(adjusted, state.getGrid());
            
            return state.toBuilder()
                .currentTetromino(adjusted)
                .lastRotationWasTSpin(isTSpin)
                .build();
        }
        
        return state; // 회전 불가
    }
    
    @Override
    public GameState softDrop(GameState state) {
        Tetromino current = state.getCurrentTetromino();
        Tetromino dropped = current.moveDown();
        
        if (isValid(dropped, state.getGrid())) {
            return state.toBuilder()
                .currentTetromino(dropped)
                .build();
        }
        
        // 바닥에 도달 → Lock
        return lockTetromino(state);
    }
    
    @Override
    public GameState hardDrop(GameState state) {
        Tetromino current = state.getCurrentTetromino();
        int dropDistance = 0;
        
        // 바닥까지 이동
        while (isValid(current.moveDown(), state.getGrid())) {
            current = current.moveDown();
            dropDistance++;
        }
        
        // 점수 추가 (하드 드롭 보너스)
        int bonus = dropDistance * 2;
        
        GameState dropped = state.toBuilder()
            .currentTetromino(current)
            .score(state.getScore() + bonus)
            .build();
        
        // Lock
        return lockTetromino(dropped);
    }
    
    @Override
    public GameState hold(GameState state) {
        if (state.isHoldUsed()) {
            return state; // 이미 사용함
        }
        
        Tetromino current = state.getCurrentTetromino();
        Tetromino held = state.getHoldPiece();
        
        if (held == null) {
            // 처음 Hold
            return state.toBuilder()
                .holdPiece(current)
                .currentTetromino(spawnTetromino(state.getNextPieces().get(0)))
                .holdUsed(true)
                .build();
        } else {
            // Hold 교환
            return state.toBuilder()
                .holdPiece(current)
                .currentTetromino(spawnTetromino(held.getType()))
                .holdUsed(true)
                .build();
        }
    }
    
    // ========== Private Helper Methods ==========
    
    private GameState fixTetrominoToGrid(GameState state) {
        // 그리드에 테트로미노 고정
        int[][] newGrid = copyGrid(state.getGrid());
        Tetromino current = state.getCurrentTetromino();
        
        for (int i = 0; i < current.getShape().length; i++) {
            for (int j = 0; j < current.getShape()[i].length; j++) {
                if (current.getShape()[i][j] != 0) {
                    int gridX = current.getX() + j;
                    int gridY = current.getY() + i;
                    newGrid[gridY][gridX] = current.getType().ordinal() + 1;
                }
            }
        }
        
        return state.toBuilder().grid(newGrid).build();
    }
    
    private GameState checkAndClearLines(GameState state) {
        int[][] grid = state.getGrid();
        int linesCleared = 0;
        
        // 클리어할 라인 찾기
        for (int i = grid.length - 1; i >= 0; i--) {
            if (isLineFull(grid[i])) {
                clearLine(grid, i);
                linesCleared++;
                i++; // 다시 체크
            }
        }
        
        if (linesCleared == 0) {
            return state;
        }
        
        // 점수 계산
        int score = calculateScore(linesCleared, state);
        
        // Combo 체크
        int combo = state.getComboCount() + 1;
        
        return state.toBuilder()
            .grid(grid)
            .lines(state.getLines() + linesCleared)
            .score(state.getScore() + score)
            .lastLinesCleared(linesCleared)
            .comboCount(combo)
            .build();
    }
    
    private int calculateScore(int linesCleared, GameState state) {
        int baseScore;
        
        switch (linesCleared) {
            case 1: baseScore = 100; break;
            case 2: baseScore = 300; break;
            case 3: baseScore = 500; break;
            case 4: baseScore = 800; break; // Tetris
            default: baseScore = 0;
        }
        
        // 레벨 배수
        int score = baseScore * state.getLevel();
        
        // T-Spin 보너스
        if (state.isLastLockWasTSpin()) {
            score += 400 * state.getLevel();
        }
        
        // Combo 보너스
        int combo = state.getComboCount();
        if (combo > 0) {
            score += 50 * combo * state.getLevel();
        }
        
        return score;
    }
    
    private boolean isValid(Tetromino tetromino, int[][] grid) {
        // 경계 체크 + 충돌 체크
        for (int i = 0; i < tetromino.getShape().length; i++) {
            for (int j = 0; j < tetromino.getShape()[i].length; j++) {
                if (tetromino.getShape()[i][j] != 0) {
                    int gridX = tetromino.getX() + j;
                    int gridY = tetromino.getY() + i;
                    
                    // 경계 체크
                    if (gridX < 0 || gridX >= grid[0].length || 
                        gridY < 0 || gridY >= grid.length) {
                        return false;
                    }
                    
                    // 충돌 체크
                    if (grid[gridY][gridX] != 0) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    private Tetromino applySRS(Tetromino rotated, int[][] grid) {
        // Super Rotation System (벽 차기)
        int[][] offsets = {{0, 0}, {1, 0}, {-1, 0}, {0, -1}, {0, 1}};
        
        for (int[] offset : offsets) {
            Tetromino adjusted = rotated.move(offset[0], offset[1]);
            if (isValid(adjusted, grid)) {
                return adjusted;
            }
        }
        
        return null; // 회전 불가
    }
    
    private boolean checkTSpin(Tetromino tetromino, int[][] grid) {
        // T-Spin 판정 (3-corner rule)
        if (tetromino.getType() != TetrominoType.T) {
            return false;
        }
        
        int corners = 0;
        int[][] cornerOffsets = {{-1, -1}, {1, -1}, {-1, 1}, {1, 1}};
        
        for (int[] offset : cornerOffsets) {
            int x = tetromino.getX() + offset[0];
            int y = tetromino.getY() + offset[1];
            
            if (x < 0 || x >= grid[0].length || y < 0 || y >= grid.length ||
                grid[y][x] != 0) {
                corners++;
            }
        }
        
        return corners >= 3;
    }
}
```

---

### 7.2 tetris-client 모듈

#### 7.2.1 BoardController (핵심)

```java
package seoultech.se.client.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import seoultech.se.client.dto.GameCommand;
import seoultech.se.client.dto.GameUpdateResponse;
import seoultech.se.client.event.LocalUIEventGenerator;
import seoultech.se.client.event.UIEvent;
import seoultech.se.client.event.UIEventHandler;
import seoultech.se.client.exception.NetworkException;
import seoultech.se.client.exception.StateConflictException;
import seoultech.se.client.exception.ValidationException;
import seoultech.se.client.strategy.PlayTypeStrategy;
import seoultech.se.core.GameEngine;
import seoultech.se.core.GameState;

/**
 * 게임 보드 컨트롤러 (Orchestrator)
 * 
 * 책임:
 * - Command 실행
 * - Strategy 패턴 적용 (Single/Multi)
 * - UI 이벤트 생성 및 처리
 * - GameEngine 위임
 */
@Slf4j
@Component
public class BoardController {
    
    private final GameEngine gameEngine;
    private final PlayTypeStrategy playTypeStrategy;
    private final UIEventHandler eventHandler;
    private final LocalUIEventGenerator localEventGen;
    
    // 게임 상태
    private GameState currentState;
    
    @Autowired
    public BoardController(
        GameEngine gameEngine,
        PlayTypeStrategy playTypeStrategy,
        UIEventHandler eventHandler,
        LocalUIEventGenerator localEventGen
    ) {
        this.gameEngine = gameEngine;
        this.playTypeStrategy = playTypeStrategy;
        this.eventHandler = eventHandler;
        this.localEventGen = localEventGen;
    }
    
    /**
     * Command 실행 (핵심 메서드)
     */
    public void executeCommand(GameCommand command) {
        try {
            // Step 1: beforeCommand (서버 전송 - Multi만)
            boolean shouldExecute = playTypeStrategy.beforeCommand(command);
            if (!shouldExecute) {
                log.debug("Command blocked by strategy: {}", command.getCommandType());
                return;
            }
            
            // Step 2: Local Event 생성 및 즉시 표시
            UIEvent localEvent = localEventGen.generateLocalEvent(command, currentState);
            if (localEvent != null) {
                eventHandler.handle(localEvent);
            }
            
            // Step 3: 로컬 예측 (GameEngine 실행)
            GameState newState = executeGameLogic(command, currentState);
            
            // Step 4: afterCommand (예측 저장 - Multi만)
            playTypeStrategy.afterCommand(command, newState);
            
            // Step 5: 상태 업데이트 및 렌더링
            updateState(newState);
            
        } catch (NetworkException e) {
            handleNetworkError(e);
        } catch (ValidationException e) {
            showErrorMessage("잘못된 조작입니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error in executeCommand", e);
            showErrorMessage("오류가 발생했습니다. 다시 시도해주세요.");
        }
    }
    
    /**
     * 게임 로직 실행 (GameEngine 위임)
     */
    private GameState executeGameLogic(GameCommand command, GameState state) {
        switch (command.getCommandType()) {
            case MOVE_LEFT:
                return gameEngine.tryMoveLeft(state);
            case MOVE_RIGHT:
                return gameEngine.tryMoveRight(state);
            case ROTATE_CW:
                return gameEngine.tryRotate(state, RotationDirection.CLOCKWISE);
            case ROTATE_CCW:
                return gameEngine.tryRotate(state, RotationDirection.COUNTER_CLOCKWISE);
            case SOFT_DROP:
                return gameEngine.softDrop(state);
            case HARD_DROP:
                return gameEngine.hardDrop(state);
            case HOLD:
                return gameEngine.hold(state);
            default:
                throw new ValidationException("Unknown command type: " + command.getCommandType());
        }
    }
    
    /**
     * 서버 응답 수신 (멀티플레이어)
     */
    public void onServerUpdate(GameUpdateResponse response) {
        log.debug("Received server update: seq={}, events={}", 
            response.getSequenceNumber(), response.getEvents().size());
        
        try {
            // Step 1: State Reconciliation (Multi만)
            playTypeStrategy.onServerStateUpdate(response.getState());
            
            // Step 2: Critical Events 처리
            if (!response.getEvents().isEmpty()) {
                eventHandler.handleEvents(response.getEvents());
            }
            
            // Step 3: 상태 업데이트
            updateState(response.getState());
            
        } catch (StateConflictException e) {
            log.warn("State conflict detected, forcing server state", e);
            forceStateUpdate(e.getServerState());
        }
    }
    
    /**
     * 강제 상태 업데이트 (Mismatch 시)
     */
    public void forceStateUpdate(GameState serverState) {
        log.warn("Forcing state update from server");
        this.currentState = serverState;
        renderState(serverState);
    }
    
    /**
     * 네트워크 오류 처리
     */
    private void handleNetworkError(NetworkException e) {
        log.error("Network error: {}", e.getMessage());
        showNotification("네트워크 연결이 끊어졌습니다. 오프라인 모드로 전환합니다.");
    }
    
    /**
     * 상태 업데이트 + 렌더링
     */
    private void updateState(GameState newState) {
        this.currentState = newState;
        renderState(newState);
    }
    
    /**
     * UI 렌더링 (JavaFX)
     */
    private void renderState(GameState state) {
        // Platform.runLater()로 UI Thread에서 실행
        // 실제 구현은 BoardView에서 처리
    }
    
    /**
     * 에러 메시지 표시
     */
    private void showErrorMessage(String message) {
        log.error("Error: {}", message);
        // UI에 에러 표시
    }
    
    /**
     * 알림 표시
     */
    private void showNotification(String message) {
        log.info("Notification: {}", message);
        // UI에 알림 표시
    }
}
```

---

### 7.3 tetris-backend 모듈

#### 7.3.1 GameService (핵심)

```java
package seoultech.se.backend.game;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import seoultech.se.backend.dto.GameCommand;
import seoultech.se.backend.dto.GameUpdateResponse;
import seoultech.se.backend.event.CriticalEventGenerator;
import seoultech.se.backend.exception.ValidationException;
import seoultech.se.backend.security.CheatDetectionService;
import seoultech.se.core.GameEngine;
import seoultech.se.core.GameState;

import java.util.List;

/**
 * 게임 서비스 (Server Authoritative)
 */
@Slf4j
@Service
public class GameService {
    
    private final GameEngine gameEngine;
    private final GameStateStore stateStore;
    private final CriticalEventGenerator eventGenerator;
    private final CheatDetectionService cheatDetection;
    
    @Autowired
    public GameService(
        GameEngine gameEngine,
        GameStateStore stateStore,
        CriticalEventGenerator eventGenerator,
        CheatDetectionService cheatDetection
    ) {
        this.gameEngine = gameEngine;
        this.stateStore = stateStore;
        this.eventGenerator = eventGenerator;
        this.cheatDetection = cheatDetection;
    }
    
    /**
     * Command 처리 (핵심 메서드)
     */
    @Transactional
    public GameUpdateResponse processCommand(GameCommand command) {
        String playerId = command.getPlayerId();
        int seq = command.getSequenceNumber();
        
        log.debug("Processing command: seq={}, type={}, player={}", 
            seq, command.getCommandType(), playerId);
        
        try {
            // Step 1: 게임 상태 로드
            GameState oldState = stateStore.get(playerId);
            if (oldState == null) {
                throw new ValidationException("Game state not found for player: " + playerId);
            }
            
            // Step 2: Command 검증
            cheatDetection.validateCommand(command, oldState);
            
            // Step 3: GameEngine 실행
            GameState newState = executeGameLogic(command, oldState);
            
            // Step 4: 상태 변화 검증 (Cheating Detection)
            cheatDetection.validateStateTransition(oldState, newState);
            
            // Step 5: Sequence Number 업데이트
            newState = newState.toBuilder()
                .lastProcessedSequence(seq)
                .build();
            
            // Step 6: Critical Events 생성
            List<UIEvent> events = eventGenerator.generate(oldState, newState);
            
            // Step 7: 상태 저장
            stateStore.save(playerId, newState);
            
            // Step 8: 응답 생성
            GameUpdateResponse response = GameUpdateResponse.builder()
                .success(true)
                .sequenceNumber(seq)
                .timestamp(System.currentTimeMillis())
                .state(newState)
                .events(events)
                .build();
            
            log.info("✅ Command processed: seq={}, score={}, events={}", 
                seq, newState.getScore(), events.size());
            
            return response;
            
        } catch (Exception e) {
            log.error("❌ Failed to process command", e);
            throw e;
        }
    }
    
    /**
     * GameEngine 실행
     */
    private GameState executeGameLogic(GameCommand command, GameState state) {
        switch (command.getCommandType()) {
            case MOVE_LEFT:
                return gameEngine.tryMoveLeft(state);
            case MOVE_RIGHT:
                return gameEngine.tryMoveRight(state);
            case ROTATE_CW:
                return gameEngine.tryRotate(state, RotationDirection.CLOCKWISE);
            case ROTATE_CCW:
                return gameEngine.tryRotate(state, RotationDirection.COUNTER_CLOCKWISE);
            case SOFT_DROP:
                return gameEngine.softDrop(state);
            case HARD_DROP:
                return gameEngine.hardDrop(state);
            case HOLD:
                return gameEngine.hold(state);
            default:
                throw new ValidationException("Unknown command type: " + command.getCommandType());
        }
    }
}
```

---

## 7.4 실제 구현된 컴포넌트 명세

### 7.4.1 Core 모듈 실제 클래스

#### A. RandomGenerator (난수 생성기)

**위치**: `seoultech.se.core.random.RandomGenerator`

**역할**: 시드 기반 재현 가능한 난수 생성

**주요 메서드**:
```java
public class RandomGenerator {
    /**
     * 시드 설정
     * @param seed 시드 값
     */
    public void setSeed(long seed)
    
    /**
     * 다음 정수 반환
     * @param bound 상한값 (exclusive)
     * @return 0 ~ bound-1 사이의 정수
     */
    public int nextInt(int bound)
    
    /**
     * 다음 더블 반환
     * @return 0.0 ~ 1.0 사이의 실수
     */
    public double nextDouble()
    
    /**
     * 리스트 셔플
     * @param list 셔플할 리스트
     */
    public <T> void shuffle(List<T> list)
}
```

**사용 예**:
```java
RandomGenerator rng = new RandomGenerator();
rng.setSeed(12345L);  // 재현 가능한 게임
int random = rng.nextInt(7);  // 0-6
```

---

#### B. TetrominoGenerator (테트로미노 생성기)

**위치**: `seoultech.se.core.random.TetrominoGenerator`

**역할**: 7-bag 시스템으로 테트로미노 생성

**주요 메서드**:
```java
public class TetrominoGenerator {
    /**
     * 생성자
     * @param randomGenerator 난수 생성기
     * @param difficulty 난이도
     */
    public TetrominoGenerator(RandomGenerator randomGenerator, Difficulty difficulty)
    
    /**
     * 다음 테트로미노 타입 반환
     * @return 테트로미노 타입
     */
    public TetrominoType next()
    
    /**
     * 다음 N개 테트로미노 미리보기
     * @param count 미리보기 개수
     * @return 테트로미노 타입 배열
     */
    public TetrominoType[] preview(int count)
    
    /**
     * 현재 Bag 상태 반환 (디버그용)
     */
    public List<TetrominoType> getCurrentBag()
    
    /**
     * 난이도 변경
     * @param difficulty 새 난이도
     */
    public void setDifficulty(Difficulty difficulty)
}
```

**7-bag 알고리즘**:
```
1. 7개 테트로미노 (I,O,T,S,Z,J,L)를 Bag에 넣음
2. Bag을 셔플
3. 하나씩 꺼내서 반환
4. Bag이 비면 다시 채우고 셔플
→ 연속 13개까지 같은 블록이 나오지 않음 보장
```

---

#### C. Difficulty (난이도 Enum)

**위치**: `seoultech.se.core.model.enumType.Difficulty`

**역할**: 난이도별 설정 제공 (기본 파라미터)

**참고**: 
- 이 Enum은 기본 게임 파라미터를 정의합니다
- 점수 배율은 `DifficultySettings` 클래스에서 관리됩니다 (UR-2.2, FR-5.2 참조)
- EASY: 0.5x, NORMAL: 1.0x, HARD: 1.5x, EXPERT: 2.0x (SRS 표준)

**정의**:
```java
public enum Difficulty {
    EASY(1000, 500, 15, 0.1),        // 초급 (10% 드롭, 점수 0.5x)
    NORMAL(700, 300, 10, 0.15),      // 중급 (15% 드롭, 점수 1.0x)
    HARD(400, 200, 5, 0.2),          // 고급 (20% 드롭, 점수 1.5x)
    EXPERT(300, 150, 3, 0.25);       // 전문가 (25% 드롭, 점수 2.0x)
    // 참고: 모든 드롭 확률은 30% 이하로 제한 (UR-2.3, FR-4.1)
    
    private final int fallInterval;     // 낙하 간격 (ms)
    private final int lockDelay;        // 고정 지연 (ms)
    private final int maxLockResets;   // 최대 Lock Reset 횟수
    private final double itemDropRate;  // 아이템 드롭 확률 (0.0 ~ 0.3)
    
    /**
     * 레벨에 따른 낙하 간격 계산
     * @param level 레벨
     * @return 낙하 간격 (ms)
     */
    public int getFallIntervalForLevel(int level) {
        // 레벨이 오를수록 감소 (최소 50ms)
        return Math.max(50, fallInterval - (level * 50));
    }
}
```

---

#### D. ItemType (아이템 타입 Enum)

**위치**: `seoultech.se.core.item.ItemType`

**역할**: 아케이드 모드의 아이템 타입 정의

**정의**:
```java
public enum ItemType {
    BOMB_ITEM("폭탄", "하단 2줄 삭제", ItemEffect.BOMB),
    BONUS_SCORE_ITEM("보너스", "점수 500점 추가", ItemEffect.BONUS_SCORE),
    SPEED_RESET_ITEM("속도 초기화", "낙하 속도 초기화", ItemEffect.SPEED_RESET),
    PLUS_ITEM("플러스", "보드 양옆 한 줄씩 추가", ItemEffect.PLUS);
    
    private final String displayName;
    private final String description;
    private final ItemEffect effect;
    
    /**
     * 아이콘 파일 경로 반환
     */
    public String getIconPath() {
        return "/image/items/" + name().toLowerCase() + ".png";
    }
}
```

---

### 7.4.2 Client 모듈 실제 클래스

#### A. GameLoopManager (게임 루프 관리자)

**위치**: `seoultech.se.client.ui.GameLoopManager`

**역할**: 60 FPS 게임 루프 실행

**주요 메서드**:
```java
public class GameLoopManager {
    private AnimationTimer timer;
    private long lastUpdate = 0;
    private static final long FRAME_DURATION = 16_666_667;  // 60 FPS (ns)
    
    /**
     * 게임 루프 시작
     * @param updateCallback 매 프레임 실행될 콜백
     */
    public void start(Runnable updateCallback) {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= FRAME_DURATION) {
                    updateCallback.run();
                    lastUpdate = now;
                }
            }
        };
        timer.start();
    }
    
    /**
     * 게임 루프 중지
     */
    public void stop() {
        if (timer != null) {
            timer.stop();
        }
    }
}
```

---

#### B. InputHandler (입력 처리기)

**위치**: `seoultech.se.client.ui.InputHandler`

**역할**: 키보드 입력을 GameCommand로 변환

**주요 메서드**:
```java
public class InputHandler {
    private Map<KeyCode, GameAction> keyMappings;
    private BoardController boardController;
    
    /**
     * 키 이벤트 처리
     */
    public void handleKeyPress(KeyEvent event) {
        KeyCode keyCode = event.getCode();
        GameAction action = keyMappings.get(keyCode);
        
        if (action != null) {
            GameCommand command = createCommand(action);
            boardController.executeCommand(command);
        }
    }
    
    /**
     * 커맨드 생성
     */
    private GameCommand createCommand(GameAction action) {
        switch (action) {
            case MOVE_LEFT:
                return new MoveCommand(Direction.LEFT);
            case MOVE_RIGHT:
                return new MoveCommand(Direction.RIGHT);
            case ROTATE_CW:
                return new RotateCommand(RotationDirection.CW);
            case ROTATE_CCW:
                return new RotateCommand(RotationDirection.CCW);
            case HARD_DROP:
                return new HardDropCommand();
            case HOLD:
                return new HoldCommand();
            default:
                return null;
        }
    }
    
    /**
     * 키 매핑 설정
     */
    public void setKeyMapping(GameAction action, KeyCode keyCode) {
        keyMappings.put(keyCode, action);
    }
}
```

---

#### C. SettingsService (설정 관리 서비스)

**위치**: `seoultech.se.client.service.SettingsService`

**역할**: 게임 설정 저장 및 로드

**주요 메서드**:
```java
@Service
public class SettingsService {
    private static final String SETTINGS_FILE = "tetris_settings";
    
    /**
     * 설정 저장
     * @param settings 설정 맵
     */
    public void saveSettings(Map<String, Object> settings) {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(SETTINGS_FILE))) {
            oos.writeObject(settings);
        }
    }
    
    /**
     * 설정 로드
     * @return 설정 맵
     */
    public Map<String, Object> loadSettings() {
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(SETTINGS_FILE))) {
            return (Map<String, Object>) ois.readObject();
        } catch (Exception e) {
            return getDefaultSettings();
        }
    }
    
    /**
     * 기본 설정 반환
     */
    private Map<String, Object> getDefaultSettings() {
        Map<String, Object> defaults = new HashMap<>();
        defaults.put("difficulty", Difficulty.NORMAL);
        defaults.put("musicVolume", 0.5);
        defaults.put("sfxVolume", 0.7);
        defaults.put("keyBindings", getDefaultKeyBindings());
        return defaults;
    }
}
```

---

#### D. NotificationManager (알림 관리자)

**위치**: `seoultech.se.client.ui.NotificationManager`

**역할**: 게임 내 알림 표시

**주요 메서드**:
```java
public class NotificationManager {
    private VBox notificationContainer;
    
    /**
     * 알림 표시
     * @param message 메시지
     * @param duration 표시 시간 (ms)
     * @param type 알림 타입 (INFO, WARNING, ERROR)
     */
    public void show(String message, int duration, NotificationType type) {
        Platform.runLater(() -> {
            Label notification = createNotification(message, type);
            notificationContainer.getChildren().add(notification);
            
            // 페이드 인 애니메이션
            FadeTransition fadeIn = new FadeTransition(
                Duration.millis(300), notification);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();
            
            // 자동 제거 (duration 후)
            PauseTransition pause = new PauseTransition(
                Duration.millis(duration));
            pause.setOnFinished(e -> {
                FadeTransition fadeOut = new FadeTransition(
                    Duration.millis(300), notification);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e2 -> 
                    notificationContainer.getChildren().remove(notification));
                fadeOut.play();
            });
            pause.play();
        });
    }
    
    /**
     * 알림 레이블 생성
     */
    private Label createNotification(String message, NotificationType type) {
        Label label = new Label(message);
        label.getStyleClass().add("notification");
        label.getStyleClass().add("notification-" + type.name().toLowerCase());
        return label;
    }
}
```

---

### 7.4.3 Backend 모듈 실제 클래스

#### A. ScoreRepository (점수 저장소)

**위치**: `seoultech.se.backend.score.ScoreRepository`

**역할**: Spring Data JPA를 통한 점수 저장

**정의**:
```java
@Repository
public interface ScoreRepository extends JpaRepository<ScoreEntity, Long> {
    
    /**
     * 게임 모드별 상위 N개 점수 조회
     * @param gameMode 게임 모드
     * @param pageable 페이징 정보
     * @return 점수 목록
     */
    @Query("SELECT s FROM ScoreEntity s WHERE s.gameMode = :gameMode " +
           "ORDER BY s.score DESC")
    List<ScoreEntity> findTopByGameMode(
        @Param("gameMode") GameMode gameMode, 
        Pageable pageable);
    
    /**
     * 플레이어별 최고 점수 조회
     * @param playerName 플레이어 이름
     * @param gameMode 게임 모드
     * @return 최고 점수
     */
    @Query("SELECT MAX(s.score) FROM ScoreEntity s " +
           "WHERE s.playerName = :playerName AND s.gameMode = :gameMode")
    Optional<Long> findMaxScoreByPlayerAndMode(
        @Param("playerName") String playerName,
        @Param("gameMode") GameMode gameMode);
    
    /**
     * 특정 기간 내 점수 조회
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 점수 목록
     */
    List<ScoreEntity> findByCreatedAtBetween(
        LocalDateTime startDate, 
        LocalDateTime endDate);
}
```

---

#### B. ScoreEntity (점수 엔티티)

**위치**: `seoultech.se.backend.score.ScoreEntity`

**역할**: 점수 데이터베이스 엔티티

**정의**:
```java
@Entity
@Table(name = "scores", indexes = {
    @Index(name = "idx_score", columnList = "score DESC"),
    @Index(name = "idx_player_mode", columnList = "playerName, gameMode")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScoreEntity extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String playerName;
    
    @Column(nullable = false)
    private Long score;
    
    @Column(nullable = false)
    private Integer linesCleared;
    
    @Column(nullable = false)
    private Integer level;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameMode gameMode;  // CLASSIC, ARCADE
    
    @Column
    private Integer playTimeSeconds;  // 플레이 시간 (초)
    
    @Column
    private Integer maxCombo;  // 최대 콤보
    
    @Column
    private Boolean isPerfectClear;  // 퍼펙트 클리어 여부
}
```

---

### 7.4.4 유틸리티 클래스

#### A. ColorMapper (색상 변환기)

**위치**: `seoultech.se.client.util.ColorMapper`

**역할**: Core의 Color Enum을 JavaFX Color로 변환

**정의**:
```java
public class ColorMapper {
    private static final Map<seoultech.se.core.model.enumType.Color, javafx.scene.paint.Color> COLOR_MAP = 
        Map.of(
            seoultech.se.core.model.enumType.Color.CYAN, javafx.scene.paint.Color.CYAN,
            seoultech.se.core.model.enumType.Color.YELLOW, javafx.scene.paint.Color.YELLOW,
            seoultech.se.core.model.enumType.Color.PURPLE, javafx.scene.paint.Color.PURPLE,
            seoultech.se.core.model.enumType.Color.GREEN, javafx.scene.paint.Color.GREEN,
            seoultech.se.core.model.enumType.Color.RED, javafx.scene.paint.Color.RED,
            seoultech.se.core.model.enumType.Color.BLUE, javafx.scene.paint.Color.BLUE,
            seoultech.se.core.model.enumType.Color.ORANGE, javafx.scene.paint.Color.ORANGE
        );
    
    /**
     * Core Color를 JavaFX Color로 변환
     */
    public static javafx.scene.paint.Color toJavaFXColor(
        seoultech.se.core.model.enumType.Color coreColor) {
        return COLOR_MAP.getOrDefault(coreColor, javafx.scene.paint.Color.GRAY);
    }
}
```

---

## 7.5 테스트 요구사항 및 품질 기준

### 7.5.1 테스트 커버리지 목표

#### 단위 테스트 커버리지
```
전체 목표: 80% 이상

모듈별 목표:
- tetris-core: 90% 이상 (핵심 게임 로직)
- tetris-client: 70% 이상 (UI 제외)
- tetris-backend: 85% 이상 (API 로직)

제외 항목:
- JavaFX Controller (UI 테스트는 E2E로)
- DTO/Entity (단순 데이터 클래스)
- Configuration 클래스
```

#### 중요 클래스 필수 테스트
```
Priority 1 (100% 커버리지 필수):
- GameEngine 구현체 (ClassicGameEngine, ArcadeGameEngine)
- PlayTypeStrategy 구현체
- NetworkServiceProxy
- UIEventHandler
- CheatDetectionService

Priority 2 (90% 커버리지):
- ItemManager
- TetrominoGenerator
- GameStateReconciliator

Priority 3 (80% 커버리지):
- SettingsService
- NotificationManager
- ColorMapper
```

### 7.5.2 단위 테스트 요구사항

#### GameEngine 테스트 케이스
```java
@SpringBootTest(classes = {ClassicGameEngine.class})
class ClassicGameEngineTest {
    
    @Autowired
    private GameEngine gameEngine;
    
    @Test
    @DisplayName("왼쪽 이동 - 정상 케이스")
    void testMoveLeft_success() {
        // Given: 중앙에 블록 배치
        GameState state = createInitialState();
        
        // When: 왼쪽 이동
        GameState result = gameEngine.tryMoveLeft(state);
        
        // Then: x 좌표 1 감소
        assertEquals(state.getCurrentTetromino().getX() - 1, 
                     result.getCurrentTetromino().getX());
    }
    
    @Test
    @DisplayName("왼쪽 이동 - 벽 충돌")
    void testMoveLeft_wallCollision() {
        // Given: 왼쪽 끝에 블록 배치
        GameState state = createStateAtLeftWall();
        
        // When: 왼쪽 이동 시도
        GameState result = gameEngine.tryMoveLeft(state);
        
        // Then: 상태 변경 없음
        assertEquals(state, result);
    }
    
    @Test
    @DisplayName("T-Spin 감지 - T-Spin Double")
    void testTSpinDetection_tSpinDouble() {
        // Given: T-Spin 가능한 보드 상태
        GameState state = createTSpinSetup();
        
        // When: 회전 후 락
        GameState rotated = gameEngine.tryRotate(state, RotationDirection.CLOCKWISE);
        GameState locked = gameEngine.lockTetromino(rotated);
        
        // Then: T-Spin 플래그 true, 라인 2줄 클리어
        assertTrue(locked.isTSpin());
        assertEquals(2, locked.getLastClearedLines());
    }
    
    @Test
    @DisplayName("콤보 카운트 증가")
    void testComboCounter_increase() {
        // Given: 콤보 0 상태
        GameState state = createInitialState();
        
        // When: 2회 연속 라인 클리어
        state = clearLineAndLock(state); // 콤보 1
        state = clearLineAndLock(state); // 콤보 2
        
        // Then: 콤보 2
        assertEquals(2, state.getComboCount());
    }
}
```

#### PlayTypeStrategy 테스트 케이스
```java
@SpringBootTest
class MultiPlayStrategyTest {
    
    @Mock
    private NetworkServiceProxy networkProxy;
    
    @InjectMocks
    private MultiPlayStrategy strategy;
    
    @Test
    @DisplayName("Command Throttling - 16ms 간격")
    void testCommandThrottling() {
        // Given
        GameCommand command = GameCommand.MOVE_LEFT;
        
        // When: 16ms 이내 재전송
        boolean first = strategy.beforeCommand(command);
        boolean second = strategy.beforeCommand(command); // 즉시 재전송
        
        // Then: 첫 번째는 허용, 두 번째는 거부
        assertTrue(first);
        assertFalse(second);
        
        // When: 16ms 대기 후 재전송
        Thread.sleep(17);
        boolean third = strategy.beforeCommand(command);
        
        // Then: 허용
        assertTrue(third);
    }
    
    @Test
    @DisplayName("State Reconciliation - 불일치 감지")
    void testStateReconciliation_mismatch() {
        // Given: 로컬 예측 상태
        GameState localState = createLocalState(score: 1000);
        
        // Mock: 서버 응답 (다른 점수)
        GameState serverState = createServerState(score: 950);
        
        // When: 서버 상태 수신
        strategy.onServerStateUpdate(serverState);
        
        // Then: 
        // 1. Mismatch 로그 기록
        // 2. 서버 상태 우선 적용
        // 3. Pending Commands 재실행
        verify(logger).warn(contains("State mismatch detected"));
        assertEquals(950, strategy.getCurrentState().getScore());
    }
}
```

### 7.5.3 통합 테스트 요구사항

#### 전체 게임 흐름 테스트
```java
@SpringBootTest
@AutoConfigureMockMvc
class GameFlowIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    @DisplayName("전체 게임 플레이 시나리오")
    void testCompleteGameFlow() throws Exception {
        // 1. 로그인
        String token = login("player1", "password");
        
        // 2. 게임 시작
        mockMvc.perform(post("/api/game/start")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\": \"CLASSIC\", \"difficulty\": \"NORMAL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").exists());
        
        // 3. Command 전송
        mockMvc.perform(post("/api/game/command")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\": \"MOVE_LEFT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.score").value(0));
        
        // 4. 라인 클리어 (점수 증가)
        mockMvc.perform(post("/api/game/command")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\": \"HARD_DROP\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.score").value(greaterThan(0)));
        
        // 5. 게임 종료
        mockMvc.perform(post("/api/game/end")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
```

### 7.5.4 성능 테스트 요구사항

#### JMeter 시나리오
```xml
<!-- jmeter-test-plan.jmx -->
<TestPlan>
    <ThreadGroup name="동시 접속 테스트">
        <threads>1000</threads>
        <rampUp>10</rampUp> <!-- 10초에 걸쳐 1000명 접속 -->
        <duration>300</duration> <!-- 5분 실행 -->
    </ThreadGroup>
    
    <HTTPSampler name="게임 시작">
        <path>/api/game/start</path>
        <method>POST</method>
    </HTTPSampler>
    
    <HTTPSampler name="Command 전송">
        <path>/api/game/command</path>
        <method>POST</method>
        <loops>60</loops> <!-- 1분간 60개 Command -->
    </HTTPSampler>
    
    <Assertions>
        <ResponseAssertion>
            <responseTime>100</responseTime> <!-- 평균 100ms 이하 -->
        </ResponseAssertion>
        <ResponseAssertion>
            <errorRate>1</errorRate> <!-- 에러율 1% 이하 -->
        </ResponseAssertion>
    </Assertions>
</TestPlan>
```

#### 성능 목표
```
부하 조건:
- 동시 접속: 1000명
- Command 전송 빈도: 60개/분 per 플레이어

목표:
- 평균 응답 시간: <100ms
- 95 percentile: <200ms
- 99 percentile: <500ms
- 에러율: <1%
- CPU 사용률: <70%
- 메모리 사용률: <80%
```

### 7.5.5 E2E 테스트 요구사항

#### TestFX 시나리오
```java
@ExtendWith(ApplicationExtension.class)
class TetrisE2ETest extends ApplicationTest {
    
    @Override
    public void start(Stage stage) throws Exception {
        // JavaFX 애플리케이션 시작
        new TetrisClientApplication().start(stage);
    }
    
    @Test
    @DisplayName("싱글 플레이 전체 플레이")
    void testSinglePlayerGameplay(FxRobot robot) {
        // 1. 메인 메뉴에서 싱글 플레이 선택
        robot.clickOn("#singlePlayerButton");
        
        // 2. 난이도 선택 (NORMAL)
        robot.clickOn("#normalDifficultyButton");
        
        // 3. 게임 시작
        robot.clickOn("#startGameButton");
        
        // 4. 키 입력 (왼쪽 이동)
        robot.press(KeyCode.LEFT);
        robot.release(KeyCode.LEFT);
        
        // 5. 검증: 블록 이동 확인
        verifyBlockPosition(robot, expectedX: 3);
        
        // 6. 하드 드롭
        robot.press(KeyCode.SPACE);
        robot.release(KeyCode.SPACE);
        
        // 7. 검증: 점수 증가
        verifyScore(robot, greaterThan(0));
        
        // 8. 게임 종료 (ESC)
        robot.press(KeyCode.ESCAPE);
        robot.release(KeyCode.ESCAPE);
        
        // 9. 검증: 메인 메뉴 복귀
        verifyNode(robot, "#mainMenuPane", isVisible());
    }
}
```

### 7.5.6 코드 품질 기준

#### 정적 분석 도구
```yaml
# sonar-project.properties
sonar.projectKey=tetris-app
sonar.sources=src/main/java
sonar.tests=src/test/java
sonar.java.binaries=build/classes

# 품질 게이트
sonar.qualitygate.wait=true

# 기준
sonar.coverage.jacoco.xmlReportPaths=build/reports/jacoco/test/jacocoTestReport.xml
sonar.junit.reportPaths=build/test-results/test

# 제약
sonar.coverage.minimum=80
sonar.duplicated_lines_density.maximum=3
sonar.sqale.rating.maximum=A
```

#### Checkstyle 규칙
```xml
<!-- checkstyle.xml -->
<module name="Checker">
    <module name="TreeWalker">
        <module name="LineLength">
            <property name="max" value="120"/>
        </module>
        <module name="MethodLength">
            <property name="max" value="50"/>
        </module>
        <module name="ParameterNumber">
            <property name="max" value="7"/>
        </module>
        <module name="CyclomaticComplexity">
            <property name="max" value="10"/>
        </module>
        <module name="JavadocMethod">
            <property name="scope" value="public"/>
        </module>
    </module>
</module>
```

#### 코드 리뷰 체크리스트
```
✅ 기능 요구사항 충족
✅ 단위 테스트 작성 (80% 이상)
✅ JavaDoc 작성 (public method)
✅ 예외 처리 구현
✅ 로깅 추가 (INFO/WARN/ERROR)
✅ Thread-safety 확인 (동시성 코드)
✅ SonarQube 경고 해결
✅ Checkstyle 위반 0건
```

---

## 🎯 Part 2 요약

### 완성된 섹션
✅ **4. 디자인 패턴 적용**: Strategy, Proxy, Observer 패턴  
✅ **5. 멀티플레이어 통신**: 프로토콜, Reconciliation, Throttling  
✅ **6. UI 이벤트 시스템**: Hybrid 방식, 우선순위, Thread-safe  
✅ **7. 모듈별 상세 구현**: Core, Client, Backend 핵심 클래스  
✅ **7.4. 실제 구현된 컴포넌트 명세**: 15개 주요 클래스 상세 문서화

### 문서화된 주요 컴포넌트
**Core 모듈** (4개):
- RandomGenerator: 시드 기반 난수 생성
- TetrominoGenerator: 7-bag 시스템
- Difficulty: 난이도별 설정
- ItemType: 아케이드 아이템

**Client 모듈** (5개):
- GameLoopManager: 60 FPS 게임 루프
- InputHandler: 키보드 입력 처리
- SettingsService: 설정 관리
- NotificationManager: 알림 표시
- ColorMapper: 색상 변환

**Backend 모듈** (2개):
- ScoreRepository: JPA 저장소
- ScoreEntity: 점수 엔티티

### 다음 단계
📌 **Part 3 예정**: 섹션 8-12 + 부록 A-E
- Spring Boot 설정
- 검증 체크리스트
- 설계 결정 및 트레이드오프
- 위험 관리
- 배포 전략
- 부록 (우선순위, 체크리스트, 용어집, FAQ 등)

---

**END OF PART 2**

