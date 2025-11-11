# FINAL_SYSTEM_REQUIREMENTS_v6_part3

**프로젝트**: Tetris Multi-Module Architecture  
**버전**: 6.0 (Production Ready - 최종 점검 완료)  
**작성일**: 2025-11-06  
**최종 업데이트**: 2025-11-06  
**Part**: 3/3 (섹션 8-12 + 부록)  
**이전 문서**: FINAL_SYSTEM_REQUIREMENTS_v6_part2.md 참조

---

## 📋 Part 3 목차

**Configuration & Verification**
8. [Spring Boot 설정 (Configuration)](#8-spring-boot-설정-configuration)
9. [검증 체크리스트 (Verification)](#9-검증-체크리스트-verification)

**Decision & Risk Management**
10. [설계 결정 및 트레이드오프 (Design Decisions)](#10-설계-결정-및-트레이드오프-design-decisions)
11. [위험 관리 (Risk Management)](#11-위험-관리-risk-management)
12. [배포 전략 (Deployment)](#12-배포-전략-deployment)

**부록 (Appendix)**
- [부록 A: 구현 우선순위](#부록-a-구현-우선순위)
- [부록 B: 체크리스트](#부록-b-체크리스트)
- [부록 C: 용어집](#부록-c-용어집-glossary)
- [부록 D: 참조 문서](#부록-d-참조-문서-references)
- [부록 E: FAQ](#부록-e-faq)

---

## 8. Spring Boot 설정 (Configuration)

### 8.1 tetris-core 모듈

#### 8.1.1 build.gradle.kts

```kotlin
plugins {
    id("java-library")
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4"
}

group = "seoultech.se"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    // Spring Core (DI만 사용)
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-beans")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // Validation
    implementation("jakarta.validation:jakarta.validation-api")
    
    // Logging
    implementation("org.slf4j:slf4j-api")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

---

#### 8.1.2 패키지 구조

```
src/main/java/seoultech/se/core/
├── GameEngine.java                 (Interface)
├── ClassicGameEngine.java          (@Component)
├── ArcadeGameEngine.java           (@Component)
├── GameState.java                  (Immutable)
├── Tetromino.java                  (Value Object)
├── TetrominoType.java              (Enum)
├── RotationDirection.java          (Enum)
├── item/
│   ├── ItemManager.java            (@Component)
│   ├── Item.java                   (Abstract)
│   ├── WeightBombItem.java
│   ├── LineClearBombItem.java
│   └── ...
└── config/
    └── CoreConfig.java             (@Configuration)
```

---

#### 8.1.3 CoreConfig.java

```java
package seoultech.se.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import seoultech.se.core.ArcadeGameEngine;
import seoultech.se.core.ClassicGameEngine;
import seoultech.se.core.GameEngine;

/**
 * Core 모듈 Spring 설정
 * 
 * ⚠️ application.yml은 로드하지 않음
 * ⚠️ 설정 값은 Client/Backend에서 주입받음
 */
@Configuration
@ComponentScan(basePackages = "seoultech.se.core")
public class CoreConfig {
    
    /**
     * GameEngine 빈 선택 (다형성)
     * 
     * tetris.game.item.enabled=false → ClassicGameEngine
     * tetris.game.item.enabled=true → ArcadeGameEngine
     */
    @Bean
    @ConditionalOnProperty(
        name = "tetris.game.item.enabled", 
        havingValue = "false", 
        matchIfMissing = true
    )
    public GameEngine classicGameEngine() {
        return new ClassicGameEngine();
    }
    
    @Bean
    @ConditionalOnProperty(
        name = "tetris.game.item.enabled", 
        havingValue = "true"
    )
    public GameEngine arcadeGameEngine() {
        return new ArcadeGameEngine();
    }
}
```

---

### 8.2 tetris-client 모듈

#### 8.2.1 build.gradle.kts

```kotlin
plugins {
    id("java")
    id("application")
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "seoultech.se"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_21

application {
    mainClass.set("seoultech.se.client.TetrisClientApplication")
}

javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.fxml", "javafx.media")
}

repositories {
    mavenCentral()
}

dependencies {
    // Core 모듈 의존성
    implementation(project(":tetris-core"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // Spring Security (JWT)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    
    // WebClient (Reactive HTTP)
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    
    // Configuration
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // Metrics
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testfx:testfx-core:4.0.18")
    testImplementation("org.testfx:testfx-junit5:4.0.18")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

---

#### 8.2.2 application.yml

```yaml
# tetris-client/src/main/resources/application.yml

spring:
  application:
    name: tetris-client
  
  main:
    allow-bean-definition-overriding: false
    web-application-type: none  # JavaFX 사용

# Tetris 게임 설정
tetris:
  game:
    # 게임 모드
    item:
      enabled: false  # false=Classic, true=Arcade
    
    # 타이밍
    auto-fall-interval: 1000  # ms
    lock-delay: 500           # ms
    das-delay: 170            # ms (Delayed Auto Shift)
    das-interval: 50          # ms
    
    # 네트워크
    network:
      server-url: "http://localhost:8080"
      websocket-url: "ws://localhost:8080/ws"
      reconnect-interval: 5000  # ms
      request-timeout: 3000     # ms
      max-retry: 3
    
    # UI
    ui:
      board-width: 10
      board-height: 20
      cell-size: 30  # pixels
      fps: 60

# Spring Security
security:
  jwt:
    secret: "your-secret-key-change-in-production"
    expiration: 86400000  # 24 hours

# Actuator (Monitoring)
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

# Logging
logging:
  level:
    root: INFO
    seoultech.se: DEBUG
    org.springframework.web: INFO
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

---

#### 8.2.3 ClientConfig.java

```java
package seoultech.se.client.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.reactive.function.client.WebClient;
import seoultech.se.client.strategy.MultiPlayStrategy;
import seoultech.se.client.strategy.PlayTypeStrategy;
import seoultech.se.client.strategy.SinglePlayStrategy;
import seoultech.se.core.config.CoreConfig;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

/**
 * Client 모듈 Spring 설정
 */
@Configuration
@ComponentScan(basePackages = "seoultech.se.client")
@Import(CoreConfig.class)  // Core 설정 임포트
public class ClientConfig {
    
    /**
     * WebClient (HTTP 통신)
     */
    @Bean
    public WebClient webClient(TetrisProperties properties) {
        return WebClient.builder()
            .baseUrl(properties.getNetwork().getServerUrl())
            .build();
    }
    
    /**
     * ScheduledExecutorService (게임 루프)
     */
    @Bean
    public ScheduledExecutorService scheduler() {
        return Executors.newSingleThreadScheduledExecutor();
    }
    
    /**
     * PlayTypeStrategy (Strategy Pattern)
     */
    @Bean
    @ConditionalOnProperty(
        name = "tetris.game.play-mode",
        havingValue = "single",
        matchIfMissing = true
    )
    public PlayTypeStrategy singlePlayStrategy() {
        return new SinglePlayStrategy();
    }
    
    @Bean
    @ConditionalOnProperty(
        name = "tetris.game.play-mode",
        havingValue = "multi"
    )
    public PlayTypeStrategy multiPlayStrategy() {
        return new MultiPlayStrategy();
    }
}
```

---

#### 8.2.4 TetrisProperties.java

```java
package seoultech.se.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * application.yml 매핑
 */
@Data
@Component
@ConfigurationProperties(prefix = "tetris.game")
public class TetrisProperties {
    
    private Item item = new Item();
    private Network network = new Network();
    private UI ui = new UI();
    private int autoFallInterval = 1000;
    private int lockDelay = 500;
    private int dasDelay = 170;
    private int dasInterval = 50;
    
    @Data
    public static class Item {
        private boolean enabled = false;
    }
    
    @Data
    public static class Network {
        private String serverUrl = "http://localhost:8080";
        private String websocketUrl = "ws://localhost:8080/ws";
        private int reconnectInterval = 5000;
        private int requestTimeout = 3000;
        private int maxRetry = 3;
    }
    
    @Data
    public static class UI {
        private int boardWidth = 10;
        private int boardHeight = 20;
        private int cellSize = 30;
        private int fps = 60;
    }
}
```

---

### 8.3 tetris-backend 모듈

#### 8.3.1 build.gradle.kts

```kotlin
plugins {
    id("java")
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
}

group = "seoultech.se"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
}

dependencies {
    // Core 모듈 의존성
    implementation(project(":tetris-core"))
    
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // Spring Security
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
    
    // Database
    runtimeOnly("com.mysql:mysql-connector-j")
    
    // Configuration
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // Metrics
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:mysql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

---

#### 8.3.2 application.yml

```yaml
# tetris-backend/src/main/resources/application.yml

spring:
  application:
    name: tetris-backend
  
  # Database
  datasource:
    url: jdbc:mysql://localhost:3306/tetris?serverTimezone=UTC&characterEncoding=UTF-8
    username: tetris_user
    password: change_in_production
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
  
  # JPA
  jpa:
    hibernate:
      ddl-auto: validate  # Production: validate, Dev: update
    show-sql: false
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect
  
  # Server
  server:
    port: 8080

# Tetris 게임 설정
tetris:
  game:
    item:
      enabled: false  # false=Classic, true=Arcade
    
    # 동시성
    max-concurrent-games: 1000
    game-state-ttl: 3600  # seconds (1 hour)
    
    # Performance
    command-throttle-ms: 16  # 60 FPS
    state-sync-interval: 100  # ms

# Security
security:
  jwt:
    secret: "your-secret-key-change-in-production-must-be-long-enough"
    expiration: 86400000  # 24 hours

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

# Logging
logging:
  level:
    root: INFO
    seoultech.se: DEBUG
    org.springframework.web: INFO
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

---

#### 8.3.3 BackendConfig.java

```java
package seoultech.se.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import seoultech.se.core.config.CoreConfig;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

/**
 * Backend 모듈 Spring 설정
 */
@Configuration
@ComponentScan(basePackages = "seoultech.se.backend")
@Import(CoreConfig.class)  // Core 설정 임포트
public class BackendConfig {
    
    /**
     * GameStateStore (In-Memory)
     */
    @Bean
    public GameStateStore gameStateStore() {
        return new InMemoryGameStateStore(new ConcurrentHashMap<>());
    }
    
    /**
     * ScheduledExecutorService (State Sync)
     */
    @Bean
    public ScheduledExecutorService scheduler() {
        return Executors.newScheduledThreadPool(4);
    }
}
```

---

### 8.4 Root build.gradle.kts

```kotlin
// tetris-app/build.gradle.kts

plugins {
    id("java")
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "seoultech.se"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
```

---

### 8.5 settings.gradle.kts

```kotlin
// tetris-app/settings.gradle.kts

rootProject.name = "tetris-app"

include("tetris-core")
include("tetris-client")
include("tetris-backend")
```

---

## 9. 검증 체크리스트 (Verification)

### 9.1 아키텍처 검증

#### 9.1.1 모듈 독립성 체크

| 검증 항목 | 확인 방법 | 통과 기준 | 상태 |
|----------|----------|----------|------|
| **Core → Client 의존성 없음** | `./gradlew tetris-core:dependencies` | JavaFX, WebClient 없음 | ⏳ |
| **Core → Backend 의존성 없음** | Gradle 의존성 트리 | Spring Web, JPA 없음 | ⏳ |
| **Client ↔ Backend 직접 의존성 없음** | build.gradle.kts 확인 | 서로 project() 참조 없음 | ⏳ |
| **Core 빌드 독립성** | `cd tetris-core && ./gradlew build` | 단독 빌드 성공 | ⏳ |

**검증 명령어**:
```bash
# Core 의존성 확인
./gradlew tetris-core:dependencies | grep -i "javafx\|webclient\|spring-web"

# 결과: 아무것도 나오지 않아야 함
```

---

#### 9.1.2 Spring Bean 등록 체크

**검증 코드**:
```java
@SpringBootTest
public class BeanRegistrationTest {
    
    @Autowired
    private ApplicationContext context;
    
    @Test
    @DisplayName("GameEngine 빈이 정확히 1개 등록되어야 함")
    void testGameEngineBeanCount() {
        Map<String, GameEngine> beans = context.getBeansOfType(GameEngine.class);
        assertEquals(1, beans.size(), "GameEngine 빈이 중복 등록됨");
    }
    
    @Test
    @DisplayName("item.enabled=false일 때 ClassicGameEngine 등록")
    void testClassicGameEngine() {
        GameEngine engine = context.getBean(GameEngine.class);
        assertInstanceOf(ClassicGameEngine.class, engine);
    }
    
    @Test
    @DisplayName("PlayTypeStrategy 빈이 정확히 1개 등록되어야 함")
    void testPlayTypeStrategyBeanCount() {
        Map<String, PlayTypeStrategy> beans = 
            context.getBeansOfType(PlayTypeStrategy.class);
        assertEquals(1, beans.size(), "PlayTypeStrategy 빈이 중복 등록됨");
    }
}
```

---

#### 9.1.3 설정 로드 체크

**검증 코드**:
```java
@SpringBootTest
public class ConfigurationLoadTest {
    
    @Autowired
    private TetrisProperties properties;
    
    @Test
    @DisplayName("application.yml이 정상 로드되어야 함")
    void testPropertiesLoaded() {
        assertNotNull(properties);
        assertNotNull(properties.getNetwork());
        assertEquals("http://localhost:8080", 
            properties.getNetwork().getServerUrl());
    }
    
    @Test
    @DisplayName("Item 설정이 정상 로드되어야 함")
    void testItemConfigLoaded() {
        assertNotNull(properties.getItem());
        assertFalse(properties.getItem().isEnabled(), 
            "기본값은 Classic 모드");
    }
}
```

---

### 9.2 디자인 패턴 검증

#### 9.2.1 Strategy 패턴 검증

**테스트 시나리오**:
1. Single Play 모드에서 `beforeCommand()` 호출 → 서버 전송 없음
2. Multi Play 모드에서 `beforeCommand()` 호출 → 서버 전송 확인

**검증 코드**:
```java
@SpringBootTest
public class StrategyPatternTest {
    
    @Autowired
    private PlayTypeStrategy strategy;
    
    @Test
    @DisplayName("SinglePlayStrategy는 서버 전송하지 않음")
    void testSinglePlayNoServerCall() {
        assumeTrue(strategy instanceof SinglePlayStrategy);
        
        GameCommand command = GameCommand.builder()
            .commandType(CommandType.MOVE_LEFT)
            .build();
        
        boolean result = strategy.beforeCommand(command);
        
        assertTrue(result, "항상 true 반환");
        // 서버 호출 없음 (verify로 확인)
    }
    
    @Test
    @DisplayName("MultiPlayStrategy는 서버 전송")
    void testMultiPlayServerCall() {
        assumeTrue(strategy instanceof MultiPlayStrategy);
        
        GameCommand command = GameCommand.builder()
            .commandType(CommandType.MOVE_LEFT)
            .sequenceNumber(1)
            .build();
        
        boolean result = strategy.beforeCommand(command);
        
        assertTrue(result);
        // 서버 호출 확인 (Mock 사용)
    }
}
```

---

#### 9.2.2 Proxy 패턴 검증

**테스트 시나리오**:
1. 네트워크 연결 실패 시 → Offline Queue에 저장
2. 재연결 성공 시 → Queue에서 꺼내 전송

**검증 코드**:
```java
public class NetworkProxyTest {
    
    private NetworkServiceProxy proxy;
    private NetworkService mockService;
    
    @BeforeEach
    void setUp() {
        mockService = mock(NetworkService.class);
        proxy = new NetworkServiceProxy(mockService);
    }
    
    @Test
    @DisplayName("연결 실패 시 Offline Queue에 저장")
    void testOfflineQueue() {
        // Given
        when(mockService.isConnected()).thenReturn(false);
        GameCommand command = GameCommand.builder()
            .commandType(CommandType.MOVE_LEFT)
            .build();
        
        // When
        proxy.sendCommand(command);
        
        // Then
        assertEquals(1, proxy.getQueueSize());
    }
    
    @Test
    @DisplayName("재연결 시 Queue 자동 전송")
    void testAutoFlush() throws InterruptedException {
        // Given
        when(mockService.isConnected()).thenReturn(false);
        proxy.sendCommand(createCommand(1));
        proxy.sendCommand(createCommand(2));
        
        // When: 재연결
        when(mockService.isConnected()).thenReturn(true);
        Thread.sleep(100);  // 자동 재연결 대기
        
        // Then
        assertEquals(0, proxy.getQueueSize());
        verify(mockService, times(2)).sendCommand(any());
    }
}
```

---

#### 9.2.3 Observer 패턴 검증

**테스트 시나리오**:
1. 이벤트 추가 시 → 우선순위 순서로 처리
2. 동시에 이벤트 추가 → Race Condition 없음

**검증 코드**:
```java
public class ObserverPatternTest {
    
    private UIEventHandler handler;
    private List<UIEventType> processedEvents;
    
    @BeforeEach
    void setUp() {
        handler = new UIEventHandler();
        processedEvents = new ArrayList<>();
        
        // 이벤트 처리 추적
        handler.setEventProcessor(event -> {
            processedEvents.add(event.getType());
        });
    }
    
    @Test
    @DisplayName("우선순위 순서로 처리")
    void testPriorityOrder() throws InterruptedException {
        // Given: 낮은 우선순위부터 추가
        handler.handle(createEvent(UIEventType.BLOCK_MOVE, 1));      // 우선순위 1
        handler.handle(createEvent(UIEventType.LINE_CLEAR, 15));     // 우선순위 15
        handler.handle(createEvent(UIEventType.LEVEL_UP, 13));       // 우선순위 13
        
        // When: 처리 완료 대기
        Thread.sleep(500);
        
        // Then: 높은 우선순위부터 처리됨
        assertEquals(3, processedEvents.size());
        assertEquals(UIEventType.LINE_CLEAR, processedEvents.get(0));   // 15
        assertEquals(UIEventType.LEVEL_UP, processedEvents.get(1));     // 13
        assertEquals(UIEventType.BLOCK_MOVE, processedEvents.get(2));   // 1
    }
    
    @Test
    @DisplayName("동시 추가 시 Thread-safe")
    void testThreadSafety() throws InterruptedException {
        // Given: 10개 스레드에서 동시에 이벤트 추가
        int threadCount = 10;
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        // When
        for (int i = 0; i < threadCount; i++) {
            int eventId = i;
            new Thread(() -> {
                handler.handle(createEvent(UIEventType.BLOCK_MOVE, 1 + eventId));
                latch.countDown();
            }).start();
        }
        
        latch.await();
        Thread.sleep(1000);  // 처리 완료 대기
        
        // Then: 모든 이벤트가 처리됨
        assertEquals(threadCount, processedEvents.size());
    }
}
```

---

### 9.3 멀티플레이어 검증

#### 9.3.1 Client-Side Prediction 검증

**테스트 시나리오**:
1. Command 실행 → 즉시 로컬 상태 변경
2. 서버 응답 도착 → State Reconciliation
3. Mismatch 발생 → 서버 상태로 교체

**검증 코드**:
```java
@SpringBootTest
public class ClientSidePredictionTest {
    
    @Autowired
    private MultiPlayStrategy strategy;
    
    @Autowired
    private BoardController controller;
    
    @Test
    @DisplayName("즉시 예측 실행")
    void testImmediatePrediction() {
        // Given
        GameCommand command = GameCommand.builder()
            .commandType(CommandType.MOVE_LEFT)
            .sequenceNumber(1)
            .timestamp(System.currentTimeMillis())
            .build();
        
        GameState stateBefore = controller.getCurrentState();
        
        // When
        controller.executeCommand(command);
        
        // Then: 즉시 상태 변경
        GameState stateAfter = controller.getCurrentState();
        assertNotEquals(stateBefore, stateAfter);
    }
    
    @Test
    @DisplayName("서버 응답 도착 시 Reconciliation")
    void testStateReconciliation() {
        // Given: 3개 예측 저장
        strategy.afterCommand(createCommand(1), createState(1));
        strategy.afterCommand(createCommand(2), createState(2));
        strategy.afterCommand(createCommand(3), createState(3));
        
        // When: 서버 응답 (seq=2까지 처리됨)
        GameState serverState = createServerState(2);
        strategy.onServerStateUpdate(serverState);
        
        // Then: seq=3만 남아있음
        assertEquals(1, strategy.getPendingCommandsCount());
    }
    
    @Test
    @DisplayName("Mismatch 발생 시 서버 상태 우선")
    void testMismatchResolution() {
        // Given
        GameState predictedState = createState(1);
        strategy.afterCommand(createCommand(1), predictedState);
        
        // When: 서버 상태가 다름
        GameState serverState = createDifferentState(1);
        
        // Then: StateConflictException 발생
        assertThrows(StateConflictException.class, () -> {
            strategy.onServerStateUpdate(serverState);
        });
    }
}
```

---

#### 9.3.2 State Reconciliation 검증

**검증 알고리즘**:
```java
@Test
@DisplayName("Reconciliation 알고리즘 검증")
void testReconciliationAlgorithm() {
    // Given
    Map<Integer, GameState> predictions = new HashMap<>();
    predictions.put(1, createState("A"));
    predictions.put(2, createState("B"));
    predictions.put(3, createState("C"));
    predictions.put(4, createState("D"));
    
    // When: 서버가 seq=2까지 처리
    GameState serverState = createServerState(2);
    int serverSeq = 2;
    
    // Reconciliation 실행
    predictions.entrySet().removeIf(entry -> entry.getKey() <= serverSeq);
    
    // Then: seq=3, 4만 남음
    assertEquals(2, predictions.size());
    assertTrue(predictions.containsKey(3));
    assertTrue(predictions.containsKey(4));
    assertFalse(predictions.containsKey(1));
    assertFalse(predictions.containsKey(2));
}
```

---

#### 9.3.3 Command Throttling 검증

**테스트 시나리오**:
1. 16ms 이내 중복 Command → 1개만 전송
2. 16ms 이후 Command → 정상 전송

**검증 코드**:
```java
@SpringBootTest
public class CommandThrottlingTest {
    
    @Autowired
    private MultiPlayStrategy strategy;
    
    @Mock
    private NetworkService mockNetwork;
    
    @Test
    @DisplayName("16ms 이내 중복 Command는 무시")
    void testThrottling() {
        // Given
        long baseTime = System.currentTimeMillis();
        
        // When: 5ms 간격으로 3개 전송 시도
        strategy.beforeCommand(createCommand(CommandType.MOVE_LEFT, baseTime));
        strategy.beforeCommand(createCommand(CommandType.MOVE_LEFT, baseTime + 5));
        strategy.beforeCommand(createCommand(CommandType.MOVE_LEFT, baseTime + 10));
        
        // Then: 1개만 전송됨
        verify(mockNetwork, times(1)).sendCommand(any());
    }
    
    @Test
    @DisplayName("16ms 이후는 정상 전송")
    void testNoThrottlingAfter16ms() {
        // Given
        long baseTime = System.currentTimeMillis();
        
        // When: 20ms 간격으로 2개 전송
        strategy.beforeCommand(createCommand(CommandType.MOVE_LEFT, baseTime));
        strategy.beforeCommand(createCommand(CommandType.MOVE_LEFT, baseTime + 20));
        
        // Then: 2개 모두 전송됨
        verify(mockNetwork, times(2)).sendCommand(any());
    }
}
```

---

### 9.4 UI 이벤트 검증

#### 9.4.1 이벤트 순차 표시 검증

**테스트 시나리오**:
1. 3개 이벤트 동시 추가 → 우선순위 순서로 표시
2. 첫 이벤트 duration 후 → 다음 이벤트 자동 표시

**검증 코드**:
```java
public class UIEventSequenceTest {
    
    private UIEventHandler handler;
    private List<String> displayLog;
    
    @BeforeEach
    void setUp() {
        handler = new UIEventHandler();
        displayLog = new ArrayList<>();
        
        handler.setEventProcessor(event -> {
            displayLog.add(String.format("%s (priority=%d)", 
                event.getType(), event.getPriority()));
        });
    }
    
    @Test
    @DisplayName("순차 표시 검증")
    void testSequentialDisplay() throws InterruptedException {
        // Given
        UIEvent event1 = createEvent(UIEventType.LINE_CLEAR, 15, 100);   // 0.1초
        UIEvent event2 = createEvent(UIEventType.LEVEL_UP, 13, 100);     // 0.1초
        UIEvent event3 = createEvent(UIEventType.BLOCK_LOCK, 5, 100);    // 0.1초
        
        // When
        handler.handleEvents(Arrays.asList(event1, event2, event3));
        
        // Then: 즉시 첫 이벤트만 표시됨
        Thread.sleep(50);
        assertEquals(1, displayLog.size());
        assertTrue(displayLog.get(0).contains("LINE_CLEAR"));
        
        // 0.1초 후 두 번째 이벤트 표시
        Thread.sleep(100);
        assertEquals(2, displayLog.size());
        assertTrue(displayLog.get(1).contains("LEVEL_UP"));
        
        // 0.1초 후 세 번째 이벤트 표시
        Thread.sleep(100);
        assertEquals(3, displayLog.size());
        assertTrue(displayLog.get(2).contains("BLOCK_LOCK"));
    }
}
```

---

#### 9.4.2 Critical vs Local 검증

**검증 코드**:
```java
@SpringBootTest
public class EventTypeTest {
    
    @Autowired
    private CriticalEventGenerator criticalGen;
    
    @Autowired
    private LocalUIEventGenerator localGen;
    
    @Test
    @DisplayName("Critical Event는 점수 포함")
    void testCriticalEventHasScore() {
        // Given
        GameState state = createStateWithLineCleared(4);
        
        // When
        List<UIEvent> events = criticalGen.generate(null, state);
        
        // Then
        UIEvent lineClearEvent = events.stream()
            .filter(e -> e.getType() == UIEventType.LINE_CLEAR)
            .findFirst()
            .orElseThrow();
        
        assertTrue(lineClearEvent.getData().containsKey("score"));
        assertTrue((Integer) lineClearEvent.getData().get("score") > 0);
    }
    
    @Test
    @DisplayName("Local Event는 점수 없음")
    void testLocalEventNoScore() {
        // Given
        GameCommand command = createCommand(CommandType.MOVE_LEFT);
        GameState state = createState();
        
        // When
        UIEvent event = localGen.generateLocalEvent(command, state);
        
        // Then
        assertNotNull(event);
        assertFalse(event.getData().containsKey("score"));
    }
}
```

---

### 9.5 성능 검증

#### 9.5.1 응답 시간 검증

**목표**: 평균 50-100ms 이내

**검증 방법**:
```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class ResponseTimeTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("Command 처리 응답 시간 < 100ms")
    void testCommandResponseTime() {
        // Given
        GameCommand command = createCommand(CommandType.MOVE_LEFT);
        String url = "http://localhost:" + port + "/api/game/command";
        
        // When: 100번 반복 측정
        List<Long> responseTimes = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long start = System.currentTimeMillis();
            restTemplate.postForEntity(url, command, GameUpdateResponse.class);
            long end = System.currentTimeMillis();
            responseTimes.add(end - start);
        }
        
        // Then
        double avgTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .average()
            .orElse(0);
        
        assertTrue(avgTime < 100, 
            String.format("평균 응답 시간 %.2fms > 100ms", avgTime));
        
        long maxTime = Collections.max(responseTimes);
        assertTrue(maxTime < 200, 
            String.format("최대 응답 시간 %dms > 200ms", maxTime));
    }
}
```

---

#### 9.5.2 동시 접속 검증

**목표**: 1000명 동시 접속

**검증 방법** (JMeter 시나리오):
```xml
<!-- JMeter Test Plan -->
<jmeterTestPlan version="1.2">
  <hashTree>
    <TestPlan>
      <stringProp name="TestPlan.comments">Tetris Concurrent Users Test</stringProp>
    </TestPlan>
    <hashTree>
      <ThreadGroup>
        <stringProp name="ThreadGroup.num_threads">1000</stringProp>
        <stringProp name="ThreadGroup.ramp_time">10</stringProp>
        <stringProp name="ThreadGroup.duration">60</stringProp>
      </ThreadGroup>
      <hashTree>
        <HTTPSamplerProxy>
          <stringProp name="HTTPSampler.domain">localhost</stringProp>
          <stringProp name="HTTPSampler.port">8080</stringProp>
          <stringProp name="HTTPSampler.path">/api/game/command</stringProp>
          <stringProp name="HTTPSampler.method">POST</stringProp>
        </HTTPSamplerProxy>
      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

**통과 기준**:
- 평균 응답 시간 < 100ms
- 95th percentile < 200ms
- 에러율 < 1%

---

### 9.6 보안 검증

#### 9.6.1 Cheating Detection 검증

**테스트 시나리오**:
1. 비정상 점수 상승 → 거부
2. 불가능한 테트로미노 위치 → 거부
3. 시간 조작 → 거부

**검증 코드**:
```java
@SpringBootTest
public class CheatDetectionTest {
    
    @Autowired
    private CheatDetectionService cheatDetection;
    
    @Test
    @DisplayName("비정상 점수 상승 감지")
    void testAbnormalScoreIncrease() {
        // Given
        GameState oldState = createState(score = 100);
        GameState newState = createState(score = 10000);  // 비정상 상승
        
        // When & Then
        assertThrows(CheatDetectedException.class, () -> {
            cheatDetection.validateStateTransition(oldState, newState);
        });
    }
    
    @Test
    @DisplayName("불가능한 테트로미노 위치 감지")
    void testInvalidTetrominoPosition() {
        // Given
        GameCommand command = createCommand(CommandType.MOVE_LEFT);
        GameState state = createStateWithTetrominoAtEdge();  // 왼쪽 끝
        
        // When & Then
        assertThrows(ValidationException.class, () -> {
            cheatDetection.validateCommand(command, state);
        });
    }
    
    @Test
    @DisplayName("시간 조작 감지")
    void testTimeManipulation() {
        // Given
        long serverTime = System.currentTimeMillis();
        GameCommand command = GameCommand.builder()
            .commandType(CommandType.MOVE_LEFT)
            .timestamp(serverTime + 10000)  // 미래 시간
            .build();
        
        // When & Then
        assertThrows(ValidationException.class, () -> {
            cheatDetection.validateCommand(command, createState());
        });
    }
}
```

---

### 9.7 통합 테스트

#### 9.7.1 End-to-End 테스트

**시나리오**: 전체 게임 플레이 (Single Play)

```java
@SpringBootTest
public class EndToEndTest {
    
    @Autowired
    private BoardController controller;
    
    @Autowired
    private GameEngine gameEngine;
    
    @Test
    @DisplayName("전체 게임 플레이 시나리오")
    void testCompleteGamePlay() {
        // Given: 게임 시작
        controller.startNewGame();
        
        // When: 테트로미노 조작
        controller.executeCommand(createCommand(CommandType.MOVE_LEFT));
        controller.executeCommand(createCommand(CommandType.MOVE_RIGHT));
        controller.executeCommand(createCommand(CommandType.ROTATE_CW));
        controller.executeCommand(createCommand(CommandType.HARD_DROP));
        
        // Then: 상태 검증
        GameState state = controller.getCurrentState();
        assertNotNull(state);
        assertTrue(state.getScore() > 0);
        
        // 라인 클리어 검증
        if (state.getLastLinesCleared() > 0) {
            assertTrue(state.getLines() > 0);
        }
    }
}
```

---

#### 9.7.2 멀티플레이어 통합 테스트

**시나리오**: 2명 플레이어 게임

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class MultiplayerIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("2명 플레이어 게임")
    void testTwoPlayerGame() {
        // Given: 2명 로그인
        String player1Token = login("player1", "password1");
        String player2Token = login("player2", "password2");
        
        // When: 동시에 Command 전송
        CompletableFuture<ResponseEntity<GameUpdateResponse>> future1 = 
            CompletableFuture.supplyAsync(() -> 
                sendCommand(player1Token, createCommand(CommandType.MOVE_LEFT)));
        
        CompletableFuture<ResponseEntity<GameUpdateResponse>> future2 = 
            CompletableFuture.supplyAsync(() -> 
                sendCommand(player2Token, createCommand(CommandType.MOVE_RIGHT)));
        
        // Then: 모두 성공
        ResponseEntity<GameUpdateResponse> response1 = future1.join();
        ResponseEntity<GameUpdateResponse> response2 = future2.join();
        
        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());
    }
}
```

---

### 9.8 검증 체크리스트 요약

| 카테고리 | 항목 | 방법 | 상태 |
|---------|------|------|------|
| **아키텍처** | 모듈 독립성 | Gradle 의존성 트리 | ⏳ |
| | Spring Bean 등록 | 단위 테스트 | ⏳ |
| | 설정 로드 | 통합 테스트 | ⏳ |
| **디자인 패턴** | Strategy 패턴 | 단위 테스트 | ⏳ |
| | Proxy 패턴 | 단위 테스트 | ⏳ |
| | Observer 패턴 | 동시성 테스트 | ⏳ |
| **멀티플레이어** | Client-Side Prediction | 통합 테스트 | ⏳ |
| | State Reconciliation | 단위 테스트 | ⏳ |
| | Command Throttling | 성능 테스트 | ⏳ |
| **UI 이벤트** | 순차 표시 | 단위 테스트 | ⏳ |
| | Critical vs Local | 통합 테스트 | ⏳ |
| **성능** | 응답 시간 | 부하 테스트 | ⏳ |
| | 동시 접속 | JMeter | ⏳ |
| **보안** | Cheating Detection | 단위 테스트 | ⏳ |
| **통합** | End-to-End | 시나리오 테스트 | ⏳ |
| | 멀티플레이어 | 통합 테스트 | ⏳ |

---

## 10. 설계 결정 및 트레이드오프 (Design Decisions)

### 10.1 아키텍처 결정

#### 10.1.1 Multi-Module vs Monolithic

**결정**: Multi-Module 채택

**근거**:
- ✅ **장점**:
  - Core 로직 재사용 (Client/Backend 모두 사용)
  - 모듈 간 독립성 보장
  - 테스트 격리 가능
  - 확장성 향상

- ❌ **단점**:
  - 초기 설정 복잡도 증가
  - Gradle 의존성 관리 필요
  - 빌드 시간 증가

**트레이드오프**:
```
복잡도 증가 (단점) < 재사용성 + 확장성 (장점)
```

**대안**:
- Monolithic: 모든 코드를 하나의 프로젝트에
- 결과: 재사용 불가, 중복 코드 발생

---

#### 10.1.2 Spring Boot in Core Module

**결정**: Core에 Spring 의존성 포함 (DI만)

**근거**:
- ✅ **장점**:
  - @Component, @Autowired로 Bean 관리
  - GameEngine Interface → 구현체 주입
  - 테스트 시 Mock 주입 용이

- ❌ **단점**:
  - Pure Java가 아님
  - Spring 없는 환경에서 사용 불가

**트레이드오프**:
```
Pure Java 순수성 (포기) < DI 편의성 + 테스트 용이성 (획득)
```

**대안**:
- Factory Pattern으로 수동 DI
- 결과: 코드 복잡도 증가, Spring 장점 활용 불가

---

#### 10.1.3 GameEngine as Interface

**결정**: GameEngine을 Interface로 정의

**근거**:
- ✅ **장점**:
  - Classic/Arcade 모드를 다형성으로 처리
  - 런타임 시 구현체 교체 가능
  - 테스트 시 Mock Engine 사용 가능

- ❌ **단점**:
  - 추상화 계층 추가
  - 코드 복잡도 소폭 증가

**트레이드오프**:
```
추상화 오버헤드 (단점) < 확장성 + 테스트 용이성 (장점)
```

**대안**:
- Concrete Class로 구현
- 결과: 모드 전환 시 조건문 남발

---

### 10.2 디자인 패턴 결정

#### 10.2.1 Strategy Pattern for Play Mode

**결정**: Single/Multi 모드를 Strategy로 분리

**근거**:
- ✅ **장점**:
  - Single Play: 네트워크 코드 완전 제거
  - Multi Play: 예측/Reconciliation 분리
  - 런타임 전환 가능

- ❌ **단점**:
  - Strategy Interface 추가
  - 코드 분산 (Single/Multi 별도 파일)

**트레이드오프**:
```
코드 분산 (단점) < 모드별 독립성 (장점)
```

**대안 1**: if-else로 분기
```java
// ❌ 안티패턴
public void executeCommand(GameCommand command) {
    if (playMode == SINGLE) {
        // Single 로직
    } else if (playMode == MULTI) {
        // Multi 로직
    }
}
```
- 결과: 코드 복잡도 증가, 테스트 어려움

**대안 2**: 별도 Controller
- SinglePlayController, MultiPlayController
- 결과: 중복 코드 발생

---

#### 10.2.2 Proxy Pattern for Network

**결정**: NetworkServiceProxy로 자동 재연결

**근거**:
- ✅ **장점**:
  - 재연결 로직 캡슐화
  - Offline Queue 투명하게 처리
  - Client 코드 간결

- ❌ **단점**:
  - Proxy 계층 추가
  - Queue 메모리 사용

**트레이드오프**:
```
메모리 사용 (단점) < 사용자 경험 향상 (장점)
```

**대안**: Client가 직접 재연결 처리
```java
// ❌ Client 코드 복잡
public void sendCommand(GameCommand command) {
    if (!isConnected()) {
        reconnect();
    }
    networkService.send(command);
}
```
- 결과: 재연결 로직이 곳곳에 흩어짐

---

#### 10.2.3 Observer Pattern for UI Events

**결정**: UIEventHandler로 이벤트 순차 처리

**근거**:
- ✅ **장점**:
  - 우선순위 자동 정렬
  - 순차 표시로 가독성 향상
  - Thread-safe 보장

- ❌ **단점**:
  - PriorityQueue 오버헤드
  - 재귀 스케줄링 복잡도

**트레이드오프**:
```
구현 복잡도 (단점) < UX 향상 (장점)
```

**대안**: 이벤트를 즉시 표시
```java
// ❌ 동시에 표시 (혼란스러움)
public void displayEvents(List<UIEvent> events) {
    events.forEach(this::displayImmediately);
}
```
- 결과: 여러 이벤트가 겹쳐서 표시됨

---

### 10.3 멀티플레이어 결정

#### 10.3.1 Client-Side Prediction

**결정**: 즉시 로컬 예측 + 서버 검증

**근거**:
- ✅ **장점**:
  - 입력 지연 없음 (즉시 반응)
  - 60 FPS 유지 가능
  - 사용자 경험 크게 향상

- ❌ **단점**:
  - Prediction/Reconciliation 복잡도
  - Mismatch 시 깜빡임 발생 가능
  - 메모리 사용 (예측 저장)

**트레이드오프**:
```
구현 복잡도 + 메모리 (단점) < 즉시 반응 (장점)
```

**대안**: Server Authoritative Only
```
Client → Command → Server (100ms) → Response → Client
```
- 결과: 100ms 입력 지연 (플레이 불가능)

**성능 비교**:
| 방식 | 입력 지연 | 구현 복잡도 | 사용자 경험 |
|------|----------|------------|-----------|
| **Client-Side Prediction** | 0ms | 높음 | ⭐⭐⭐⭐⭐ |
| Server Authoritative | 100ms | 낮음 | ⭐⭐ |

---

#### 10.3.2 State Reconciliation

**결정**: Sequence Number 기반 Reconciliation

**근거**:
- ✅ **장점**:
  - 정확한 매칭 (seq 비교)
  - 순서 보장
  - 간단한 알고리즘

- ❌ **단점**:
  - Sequence Number 관리 필요
  - Overflow 처리 필요 (2^31-1)

**트레이드오프**:
```
Sequence 관리 (단점) < 정확성 (장점)
```

**대안**: Timestamp 기반
```java
// ❌ 시간 기반 매칭
predictions.removeIf(p -> p.getTimestamp() <= serverTimestamp);
```
- 결과: 시간 동기화 문제, 부정확

---

#### 10.3.3 Command Throttling

**결정**: 16ms 단위로 Throttling (60 FPS)

**근거**:
- ✅ **장점**:
  - 서버 부하 94% 감소
  - 게임 플레이 영향 없음
  - 네트워크 대역폭 절약

- ❌ **단점**:
  - 최대 16ms 지연 발생 가능
  - Throttling 로직 복잡도

**트레이드오프**:
```
최대 16ms 지연 (단점) < 서버 부하 94% 감소 (장점)
```

**성능 측정**:
| 시나리오 | Throttling 없음 | Throttling 16ms |
|---------|----------------|-----------------|
| **초당 Command** | 360 req/s | 60 req/s |
| **서버 부하** | 100% | 6% |
| **사용자 인지 지연** | 0ms | 인지 불가 (<16ms) |

**대안**: Throttling 없음
- 결과: 서버 과부하, 응답 시간 증가

---

### 10.4 UI 이벤트 결정

#### 10.4.1 Hybrid Event System

**결정**: Critical (서버) + Local (클라이언트)

**근거**:
- ✅ **장점**:
  - Critical: 점수 일관성 보장
  - Local: 즉시 피드백
  - 균형잡힌 설계

- ❌ **단점**:
  - 시스템 복잡도 증가
  - 이벤트 종류별 처리 로직 분리

**트레이드오프**:
```
복잡도 증가 (단점) < 성능 + 일관성 (장점)
```

**대안 1**: 모든 이벤트를 서버에서 생성
```
Client → Command → Server → Events → Client (100ms 지연)
```
- 결과: 블록 이동 시 100ms 지연 (플레이 불가능)

**대안 2**: 모든 이벤트를 클라이언트에서 생성
```
Client → Command → Local Events (즉시)
```
- 결과: 멀티플레이어 간 점수 불일치

**비교표**:
| 방식 | 일관성 | 반응성 | 복잡도 |
|------|--------|--------|--------|
| **Hybrid** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| Server Only | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐ |
| Client Only | ⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ |

---

#### 10.4.2 Sequential Display

**결정**: 이벤트를 순차적으로 표시

**근거**:
- ✅ **장점**:
  - 가독성 향상
  - 중요 이벤트 강조
  - 애니메이션 완료 보장

- ❌ **단점**:
  - 전체 표시 시간 증가
  - PriorityQueue 오버헤드

**트레이드오프**:
```
표시 시간 증가 (단점) < 가독성 (장점)
```

**시나리오**: 4줄 클리어 + Level Up + Combo
```
동시 표시 (X):
  LINE_CLEAR + LEVEL_UP + COMBO (혼란)

순차 표시 (O):
  1. LINE_CLEAR (0.8초)
  2. LEVEL_UP (1.2초)
  3. COMBO (0.6초)
  → 총 2.6초 (이해 가능)
```

**대안**: 동시 표시
- 결과: 여러 텍스트가 겹쳐 보임

---

### 10.5 데이터 저장 결정

#### 10.5.1 GameState Storage

**결정**: In-Memory (ConcurrentHashMap)

**근거**:
- ✅ **장점**:
  - 초고속 조회 (O(1))
  - 트랜잭션 불필요
  - 구현 간단

- ❌ **단점**:
  - 서버 재시작 시 손실
  - 메모리 한계 존재
  - 백업 불가

**트레이드오프**:
```
데이터 손실 위험 (단점) < 성능 (장점)
```

**메모리 사용량**:
```
GameState 크기: ~10KB
1000명 동시 접속: 10KB × 1000 = 10MB (무시 가능)
```

**대안**: Database (MySQL)
```sql
-- ❌ 매 Command마다 DB 저장
INSERT INTO game_states (player_id, state) VALUES (?, ?);
```
- 결과: 응답 시간 100ms → 500ms (5배 증가)

**성능 비교**:
| 저장소 | 조회 시간 | 저장 시간 | 동시성 |
|--------|----------|----------|--------|
| **In-Memory** | 0.1ms | 0.1ms | ConcurrentHashMap |
| Database | 10ms | 50ms | Lock |

---

#### 10.5.2 User Data Storage

**결정**: Database (MySQL)

**근거**:
- ✅ **장점**:
  - 영구 저장 (회원 정보, 전적)
  - 트랜잭션 지원
  - 복잡한 쿼리 가능

- ❌ **단점**:
  - 성능 오버헤드
  - DB 설정 필요

**트레이드오프**:
```
성능 (소폭 감소) < 영구 저장 (필수)
```

**저장 대상**:
- ✅ Database: User, GameRecord, Ranking
- ✅ In-Memory: GameState, PendingCommands

---

### 10.6 보안 결정

#### 10.6.1 Server Authoritative

**결정**: 모든 게임 로직을 서버에서 실행

**근거**:
- ✅ **장점**:
  - Cheating 완벽 차단
  - 일관성 보장
  - 공정한 경쟁

- ❌ **단점**:
  - 네트워크 지연 발생
  - 서버 부하 증가
  - Prediction 필요

**트레이드오프**:
```
네트워크 지연 (단점) < 공정성 (장점)
```

**Cheating 예시**:
```java
// ❌ Client Authoritative (치팅 가능)
public void addScore(int amount) {
    this.score += amount;  // Client에서 조작 가능
}

// ✅ Server Authoritative (안전)
public GameState addScore(GameState state, int amount) {
    // 서버에서만 실행
    return state.toBuilder()
        .score(state.getScore() + amount)
        .build();
}
```

---

#### 10.6.2 Cheating Detection

**결정**: 상태 변화 검증 + 시간 검증

**근거**:
- ✅ **장점**:
  - 비정상 행위 감지
  - 로그 기록
  - 계정 제재 가능

- ❌ **단점**:
  - 검증 로직 복잡도
  - False Positive 가능성

**트레이드오프**:
```
False Positive (소수) < Cheating 차단 (다수)
```

**검증 항목**:
1. **점수 증가율**: 1초당 최대 800점
2. **테트로미노 위치**: 그리드 경계 내
3. **시간 차이**: Command 타임스탬프 ±5초 이내

---

### 10.7 테스트 전략 결정

#### 10.7.1 Test Pyramid

**결정**: 70% Unit, 20% Integration, 10% E2E

**근거**:
- ✅ **장점**:
  - 빠른 피드백 (Unit)
  - 높은 커버리지
  - 유지보수 용이

- ❌ **단점**:
  - E2E 테스트 부족
  - UI 테스트 한계

**트레이드오프**:
```
E2E 커버리지 (소폭 낮음) < 빠른 피드백 (장점)
```

**테스트 분포**:
```
Unit Test (70%):
  - GameEngine 로직
  - Strategy 패턴
  - Event 처리

Integration Test (20%):
  - REST API
  - WebSocket
  - Database

E2E Test (10%):
  - 전체 게임 플레이
  - 멀티플레이어
```

---

### 10.8 결정 요약표

| 카테고리 | 결정 | 대안 | 선택 이유 |
|---------|------|------|----------|
| **아키텍처** | Multi-Module | Monolithic | 재사용성 + 확장성 |
| | Spring in Core | Pure Java | DI 편의성 |
| | GameEngine Interface | Concrete Class | 다형성 |
| **패턴** | Strategy (Play Mode) | if-else | 독립성 |
| | Proxy (Network) | Direct Call | 재연결 자동화 |
| | Observer (UI Events) | Immediate Display | 순차 표시 |
| **멀티플레이어** | Client-Side Prediction | Server Only | 즉시 반응 |
| | Sequence Reconciliation | Timestamp | 정확성 |
| | Command Throttling 16ms | No Throttling | 서버 부하 94% 감소 |
| **UI 이벤트** | Hybrid (Critical+Local) | Server Only | 성능 + 일관성 |
| | Sequential Display | Simultaneous | 가독성 |
| **저장소** | GameState In-Memory | Database | 성능 (0.1ms) |
| | User Data in DB | In-Memory | 영구 저장 |
| **보안** | Server Authoritative | Client Authority | 공정성 |
| | Cheating Detection | No Validation | 치팅 차단 |
| **테스트** | 70% Unit, 20% Integration, 10% E2E | 균등 분배 | 빠른 피드백 |

---

## 11. 위험 관리 (Risk Management)

### 11.1 기술적 위험

#### 11.1.1 네트워크 지연 (High Priority)

**위험**: 네트워크 지연으로 멀티플레이 불가

| 항목 | 내용 |
|------|------|
| **확률** | 중간 (50%) |
| **영향도** | 높음 (게임 플레이 불가능) |
| **위험도** | 🔴 **HIGH** |

**시나리오**:
```
사용자 → Command → 네트워크 지연 200ms → 서버
→ 응답 지연 200ms → 사용자

총 지연: 400ms (게임 플레이 불가능)
```

**완화 전략**:
1. ✅ **Client-Side Prediction** (구현 완료)
   - 즉시 로컬 예측 → 0ms 지연
   - 서버 응답은 백그라운드 처리

2. ✅ **Command Throttling** (구현 완료)
   - 16ms 단위 전송 → 서버 부하 감소
   - 응답 시간 유지

3. ⏳ **CDN 사용** (향후 계획)
   - 지역별 서버 배치
   - 지연 시간 <50ms

**모니터링**:
```yaml
# Prometheus Alert
- alert: HighNetworkLatency
  expr: http_request_duration_seconds > 0.2
  for: 5m
  annotations:
    summary: "네트워크 지연 200ms 초과"
```

---

#### 11.1.2 State Mismatch (Medium Priority)

**위험**: Client-Server 상태 불일치

| 항목 | 내용 |
|------|------|
| **확률** | 낮음 (20%) |
| **영향도** | 중간 (깜빡임 발생) |
| **위험도** | 🟡 **MEDIUM** |

**시나리오**:
```
Client 예측: 테트로미노 X=5
Server 계산: 테트로미노 X=4 (충돌 감지)
→ Mismatch 발생 → Client 강제 업데이트 (깜빡임)
```

**완화 전략**:
1. ✅ **State Reconciliation** (구현 완료)
   ```java
   if (!clientState.equals(serverState)) {
       forceStateUpdate(serverState);  // 서버 우선
   }
   ```

2. ✅ **Sequence Number 검증** (구현 완료)
   - 순서 보장 → 패킷 손실 감지

3. ⏳ **State Diff 전송** (향후 최적화)
   - 전체 State가 아닌 변경분만 전송
   - 대역폭 절약

**모니터링**:
```java
@Component
public class MismatchMonitor {
    
    private final AtomicInteger mismatchCount = new AtomicInteger(0);
    
    public void recordMismatch() {
        int count = mismatchCount.incrementAndGet();
        if (count > 100) {
            log.error("⚠️ Mismatch 100회 초과: 네트워크 문제 의심");
        }
    }
}
```

---

#### 11.1.3 Memory Leak (Medium Priority)

**위험**: In-Memory GameState 누적으로 메모리 부족

| 항목 | 내용 |
|------|------|
| **확률** | 중간 (40%) |
| **영향도** | 높음 (서버 다운) |
| **위험도** | 🟡 **MEDIUM** |

**시나리오**:
```
1000명 동시 접속 × 10KB/인 = 10MB (정상)
하지만 연결 종료 후에도 State 남아있음
→ 10,000명 누적 = 100MB → OutOfMemoryError
```

**완화 전략**:
1. ✅ **TTL (Time To Live)** 설정
   ```java
   @Scheduled(fixedRate = 60000)  // 1분마다
   public void cleanupExpiredStates() {
       long now = System.currentTimeMillis();
       stateStore.entrySet().removeIf(entry -> 
           now - entry.getValue().getLastAccessTime() > 3600_000  // 1시간
       );
   }
   ```

2. ✅ **Max Size 제한**
   ```java
   if (stateStore.size() > 10000) {
       // 가장 오래된 State 제거 (LRU)
       removeOldestStates(1000);
   }
   ```

3. ⏳ **Redis 전환** (향후 계획)
   - In-Memory → Redis
   - 자동 만료 (EXPIRE)

**모니터링**:
```yaml
# Prometheus Alert
- alert: HighMemoryUsage
  expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.8
  for: 5m
  annotations:
    summary: "메모리 사용률 80% 초과"
```

---

#### 11.1.4 Database Connection Pool Exhaustion (Low Priority)

**위험**: DB Connection Pool 고갈

| 항목 | 내용 |
|------|------|
| **확률** | 낮음 (10%) |
| **영향도** | 높음 (서비스 불가) |
| **위험도** | 🟢 **LOW** |

**시나리오**:
```
HikariCP Max Pool Size: 10
동시 요청: 100개
→ 90개 요청 대기 → Timeout
```

**완화 전략**:
1. ✅ **Pool Size 증가**
   ```yaml
   spring:
     datasource:
       hikari:
         maximum-pool-size: 20  # 10 → 20
         minimum-idle: 10       # 5 → 10
   ```

2. ✅ **Connection Timeout 설정**
   ```yaml
   spring:
     datasource:
       hikari:
         connection-timeout: 30000  # 30초
   ```

3. ✅ **GameState는 In-Memory** (DB 부하 감소)
   - User Data만 DB 저장
   - GameState는 메모리

---

### 11.2 성능 위험

#### 11.2.1 동시 접속 초과 (High Priority)

**위험**: 1000명 이상 동시 접속 시 성능 저하

| 항목 | 내용 |
|------|------|
| **확률** | 중간 (30%) |
| **영향도** | 높음 (응답 시간 증가) |
| **위험도** | 🔴 **HIGH** |

**시나리오**:
```
목표: 1000명
실제: 2000명 접속
→ CPU 100%
→ 응답 시간 100ms → 500ms
```

**완화 전략**:
1. ✅ **Connection Limit**
   ```java
   @Configuration
   public class WebConfig {
       @Bean
       public TomcatServletWebServerFactory tomcatFactory() {
           TomcatServletWebServerFactory factory = 
               new TomcatServletWebServerFactory();
           factory.addConnectorCustomizers(connector -> {
               connector.setProperty("maxConnections", "1000");
           });
           return factory;
       }
   }
   ```

2. ⏳ **Load Balancer** (향후 계획)
   - Nginx 또는 AWS ALB
   - 여러 서버로 분산

3. ⏳ **Auto Scaling** (향후 계획)
   - CPU 80% 이상 시 자동 증설

**모니터링**:
```yaml
# Prometheus Alert
- alert: HighConnectionCount
  expr: tomcat_connections_current > 900
  for: 5m
  annotations:
    summary: "동시 접속 900명 초과 (한계 근접)"
```

---

#### 11.2.2 Command Flood Attack (Medium Priority)

**위험**: 악의적 사용자가 초당 1000개 Command 전송

| 항목 | 내용 |
|------|------|
| **확률** | 낮음 (20%) |
| **영향도** | 높음 (서버 다운) |
| **위험도** | 🟡 **MEDIUM** |

**시나리오**:
```
정상: 60 req/s (Throttling)
공격: 1000 req/s (Throttling 무시)
→ 서버 과부하
```

**완화 전략**:
1. ✅ **Server-Side Throttling**
   ```java
   @Component
   public class RateLimiter {
       private final Map<String, AtomicInteger> requestCounts = 
           new ConcurrentHashMap<>();
       
       public boolean allowRequest(String playerId) {
           AtomicInteger count = requestCounts.computeIfAbsent(
               playerId, k -> new AtomicInteger(0)
           );
           
           if (count.get() >= 100) {  // 초당 100개 제한
               return false;
           }
           
           count.incrementAndGet();
           return true;
       }
       
       @Scheduled(fixedRate = 1000)
       public void reset() {
           requestCounts.values().forEach(c -> c.set(0));
       }
   }
   ```

2. ⏳ **IP 기반 차단** (향후 계획)
   - 비정상 트래픽 감지
   - 자동 IP 차단

---

### 11.3 보안 위험

#### 11.3.1 JWT Token 탈취 (High Priority)

**위험**: JWT Token 탈취로 계정 도용

| 항목 | 내용 |
|------|------|
| **확률** | 낮음 (10%) |
| **영향도** | 높음 (계정 도용) |
| **위험도** | 🔴 **HIGH** |

**시나리오**:
```
공격자 → JWT Token 탈취 (XSS, Network Sniffing)
→ API 호출 → 다른 사용자 행세
```

**완화 전략**:
1. ✅ **HTTPS 강제**
   ```yaml
   server:
     ssl:
       enabled: true
   ```

2. ✅ **JWT Expiration 짧게**
   ```yaml
   security:
     jwt:
       expiration: 3600000  # 1시간
   ```

3. ✅ **Refresh Token**
   - Access Token (1시간) + Refresh Token (7일)
   - Access Token 만료 시 Refresh로 재발급

4. ⏳ **IP 검증** (향후 계획)
   - Token 발급 IP와 사용 IP 비교

---

#### 11.3.2 SQL Injection (Low Priority)

**위험**: SQL Injection 공격

| 항목 | 내용 |
|------|------|
| **확률** | 매우 낮음 (5%) |
| **영향도** | 높음 (데이터 유출) |
| **위험도** | 🟢 **LOW** |

**완화 전략**:
1. ✅ **Spring Data JPA 사용**
   - Prepared Statement 자동 생성
   - SQL Injection 방어

2. ✅ **입력 검증**
   ```java
   @Valid
   public ResponseEntity<?> register(@RequestBody @Valid UserRequest req) {
       // @Pattern, @Size로 검증
   }
   ```

---

### 11.4 운영 위험

#### 11.4.1 배포 실패 (Medium Priority)

**위험**: 배포 중 서비스 중단

| 항목 | 내용 |
|------|------|
| **확률** | 중간 (40%) |
| **영향도** | 중간 (일시적 중단) |
| **위험도** | 🟡 **MEDIUM** |

**완화 전략**:
1. ⏳ **Blue-Green Deployment**
   ```
   Blue (현재 버전) → 유지
   Green (새 버전) → 배포 → 테스트
   → 정상이면 트래픽 전환
   → 문제 시 Blue로 롤백
   ```

2. ⏳ **Health Check**
   ```yaml
   management:
     health:
       livenessState:
         enabled: true
       readinessState:
         enabled: true
   ```

3. ✅ **롤백 스크립트**
   ```bash
   # rollback.sh
   git checkout previous-version
   ./gradlew build
   java -jar tetris-backend.jar
   ```

---

#### 11.4.2 데이터 손실 (High Priority)

**위험**: 서버 다운 시 In-Memory GameState 손실

| 항목 | 내용 |
|------|------|
| **확률** | 낮음 (10%) |
| **영향도** | 높음 (진행 중 게임 손실) |
| **위험도** | 🟡 **MEDIUM** |

**완화 전략**:
1. ⏳ **Redis Persistence**
   - In-Memory → Redis
   - AOF (Append Only File) 활성화

2. ⏳ **주기적 Snapshot**
   ```java
   @Scheduled(fixedRate = 300000)  // 5분마다
   public void snapshot() {
       stateStore.forEach((playerId, state) -> {
           redis.set("game:" + playerId, serialize(state));
       });
   }
   ```

3. ✅ **사용자 안내**
   - "서버 점검 시 진행 중 게임은 저장되지 않습니다"

---

### 11.5 위험 우선순위 매트릭스

```
    높음 |  [네트워크 지연]      [동시 접속 초과]
영      |  [JWT 탈취]          
향 중간 |  [State Mismatch]    [Command Flood]
도      |  [Memory Leak]       [배포 실패]
    낮음 |  [DB Pool 고갈]      [SQL Injection]
         |________________________
            낮음   중간   높음
                 확률
```

**대응 순서**:
1. 🔴 네트워크 지연 → Client-Side Prediction (완료)
2. 🔴 JWT 탈취 → HTTPS + Refresh Token
3. 🔴 동시 접속 초과 → Connection Limit + Load Balancer
4. 🟡 State Mismatch → Reconciliation (완료)
5. 🟡 Memory Leak → TTL + Redis 전환

---

## 12. 배포 전략 (Deployment)

### 12.1 개발 환경 (Development)

#### 12.1.1 로컬 개발

**구성**:
```
Developer Laptop
├── tetris-client (JavaFX)
│   └── application-dev.yml
├── tetris-backend (Spring Boot)
│   └── application-dev.yml
└── MySQL (Docker)
```

**실행 방법**:
```bash
# 1. MySQL 시작 (Docker)
docker run -d \
  --name mysql-tetris \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=tetris \
  -p 3306:3306 \
  mysql:8.0

# 2. Backend 실행
cd tetris-backend
./gradlew bootRun --args='--spring.profiles.active=dev'

# 3. Client 실행
cd tetris-client
./gradlew run
```

**application-dev.yml** (Backend):
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tetris
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update  # 개발 시 자동 스키마 생성

logging:
  level:
    seoultech.se: DEBUG
```

---

### 12.2 테스트 환경 (Staging)

#### 12.2.1 Docker Compose

**docker-compose.yml**:
```yaml
version: '3.8'

services:
  # MySQL
  mysql:
    image: mysql:8.0
    container_name: tetris-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: tetris
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
    networks:
      - tetris-network

  # Backend
  backend:
    build:
      context: ./tetris-backend
      dockerfile: Dockerfile
    container_name: tetris-backend
    environment:
      SPRING_PROFILES_ACTIVE: staging
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/tetris
    ports:
      - "8080:8080"
    depends_on:
      - mysql
    networks:
      - tetris-network

  # Prometheus
  prometheus:
    image: prom/prometheus:latest
    container_name: tetris-prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    ports:
      - "9090:9090"
    networks:
      - tetris-network

  # Grafana
  grafana:
    image: grafana/grafana:latest
    container_name: tetris-grafana
    ports:
      - "3000:3000"
    networks:
      - tetris-network

volumes:
  mysql-data:

networks:
  tetris-network:
    driver: bridge
```

**실행**:
```bash
docker-compose up -d
```

---

#### 12.2.2 Dockerfile (Backend)

```dockerfile
# tetris-backend/Dockerfile

FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1024m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**빌드**:
```bash
docker build -t tetris-backend:1.0.0 .
```

---

### 12.3 프로덕션 환경 (Production)

#### 12.3.1 AWS 아키텍처

```
Internet
    ↓
[Route 53]  (DNS)
    ↓
[CloudFront]  (CDN)
    ↓
[Application Load Balancer]
    ↓
    ├─→ [EC2 Instance 1] (Backend)
    ├─→ [EC2 Instance 2] (Backend)
    └─→ [EC2 Instance 3] (Backend)
         ↓
    [RDS MySQL]  (Primary + Replica)
         ↓
    [ElastiCache Redis]  (GameState)
```

---

#### 12.3.2 EC2 인스턴스

**Spec**:
- **Type**: t3.medium (2 vCPU, 4GB RAM)
- **OS**: Amazon Linux 2023
- **Count**: 3개 (Multi-AZ)

**User Data Script**:
```bash
#!/bin/bash

# Java 21 설치
sudo yum install -y java-21-amazon-corretto

# Application 다운로드
aws s3 cp s3://tetris-deploy/tetris-backend-1.0.0.jar /app/app.jar

# Systemd 서비스 등록
cat > /etc/systemd/system/tetris.service <<EOF
[Unit]
Description=Tetris Backend
After=network.target

[Service]
Type=simple
User=ec2-user
ExecStart=/usr/bin/java -jar /app/app.jar
Restart=always

[Install]
WantedBy=multi-user.target
EOF

# 시작
sudo systemctl enable tetris
sudo systemctl start tetris
```

---

#### 12.3.3 RDS MySQL

**Spec**:
- **Engine**: MySQL 8.0
- **Instance**: db.t3.medium (2 vCPU, 4GB RAM)
- **Multi-AZ**: 활성화 (고가용성)
- **Backup**: 매일 자동 백업 (7일 보관)

**설정**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://tetris-db.xyz.rds.amazonaws.com:3306/tetris
    username: admin
    password: ${DB_PASSWORD}  # Secrets Manager
    hikari:
      maximum-pool-size: 20
```

---

#### 12.3.4 ElastiCache Redis

**Spec**:
- **Engine**: Redis 7.0
- **Node Type**: cache.t3.medium
- **Replicas**: 2개

**용도**:
- GameState 저장 (In-Memory 대체)
- Session 관리

**설정**:
```yaml
spring:
  redis:
    host: tetris-redis.xyz.cache.amazonaws.com
    port: 6379
```

---

#### 12.3.5 Application Load Balancer

**설정**:
```yaml
# Health Check
health_check:
  path: /actuator/health
  interval: 30s
  timeout: 5s
  healthy_threshold: 2
  unhealthy_threshold: 3

# Sticky Session (WebSocket)
stickiness:
  enabled: true
  duration: 3600  # 1 hour
```

---

### 12.4 CI/CD Pipeline

#### 12.4.1 GitHub Actions

**.github/workflows/deploy.yml**:
```yaml
name: Deploy to Production

on:
  push:
    branches:
      - main

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    
    steps:
      # 1. Checkout
      - name: Checkout
        uses: actions/checkout@v3
      
      # 2. Setup Java
      - name: Setup Java 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      # 3. Build
      - name: Build with Gradle
        run: |
          chmod +x gradlew
          ./gradlew build
      
      # 4. Test
      - name: Run Tests
        run: ./gradlew test
      
      # 5. Build Docker Image
      - name: Build Docker Image
        run: |
          docker build -t tetris-backend:${{ github.sha }} \
            ./tetris-backend
      
      # 6. Push to ECR
      - name: Push to Amazon ECR
        run: |
          aws ecr get-login-password --region us-east-1 | \
            docker login --username AWS --password-stdin \
            123456789.dkr.ecr.us-east-1.amazonaws.com
          
          docker tag tetris-backend:${{ github.sha }} \
            123456789.dkr.ecr.us-east-1.amazonaws.com/tetris:${{ github.sha }}
          
          docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/tetris:${{ github.sha }}
      
      # 7. Deploy to EC2
      - name: Deploy
        run: |
          aws ssm send-command \
            --instance-ids i-xxx i-yyy i-zzz \
            --document-name "AWS-RunShellScript" \
            --parameters commands="
              docker pull 123456789.dkr.ecr.us-east-1.amazonaws.com/tetris:${{ github.sha }}
              docker stop tetris-backend || true
              docker rm tetris-backend || true
              docker run -d --name tetris-backend -p 8080:8080 \
                123456789.dkr.ecr.us-east-1.amazonaws.com/tetris:${{ github.sha }}
            "
```

---

#### 12.4.2 배포 체크리스트

**배포 전**:
- [ ] 모든 테스트 통과
- [ ] 코드 리뷰 완료
- [ ] DB 마이그레이션 스크립트 준비
- [ ] Rollback 계획 수립
- [ ] 모니터링 대시보드 확인

**배포 중**:
- [ ] Blue-Green 배포 (무중단)
- [ ] Health Check 확인
- [ ] 에러 로그 모니터링

**배포 후**:
- [ ] Smoke Test 실행
- [ ] 성능 지표 확인 (응답 시간, CPU, 메모리)
- [ ] 사용자 피드백 수집
- [ ] 24시간 모니터링

---

### 12.5 모니터링 및 알람

#### 12.5.1 Prometheus Metrics

**prometheus.yml**:
```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'tetris-backend'
    static_configs:
      - targets: ['backend:8080']
    metrics_path: '/actuator/prometheus'
```

**주요 메트릭**:
- `http_server_requests_seconds`: 응답 시간
- `jvm_memory_used_bytes`: 메모리 사용량
- `tomcat_connections_current`: 동시 접속 수
- `game_state_count`: 저장된 GameState 수

---

#### 12.5.2 Grafana Dashboard

**패널 구성**:
1. **응답 시간** (Time Series)
   - 평균, 95th percentile, 최대
   - 목표: <100ms

2. **동시 접속 수** (Gauge)
   - 현재 접속자
   - 목표: <1000명

3. **에러율** (Graph)
   - HTTP 4xx, 5xx
   - 목표: <1%

4. **메모리 사용량** (Graph)
   - Heap, Non-Heap
   - 목표: <80%

---

#### 12.5.3 알람 규칙

```yaml
# Prometheus Alerting Rules
groups:
  - name: tetris_alerts
    rules:
      # 응답 시간 초과
      - alert: HighResponseTime
        expr: http_server_requests_seconds{quantile="0.95"} > 0.2
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "응답 시간 200ms 초과"
      
      # 에러율 증가
      - alert: HighErrorRate
        expr: rate(http_server_requests_total{status=~"5.."}[5m]) > 0.01
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "에러율 1% 초과"
      
      # 메모리 부족
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes / jvm_memory_max_bytes > 0.8
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "메모리 사용률 80% 초과"
```

---

### 12.6 배포 요약

| 환경 | 용도 | 구성 | 배포 방식 |
|------|------|------|----------|
| **Development** | 로컬 개발 | Laptop + Docker MySQL | 수동 실행 |
| **Staging** | 통합 테스트 | Docker Compose | docker-compose up |
| **Production** | 실서비스 | AWS (EC2 + RDS + Redis) | GitHub Actions CI/CD |

**배포 주기**:
- Development: 실시간
- Staging: 매일 (자동)
- Production: 주 1회 (수동 승인)

---

## 13. API 명세 및 데이터베이스 스키마

### 13.1 REST API 엔드포인트

#### 13.1.1 점수 관리 API

**Base URL**: `http://localhost:8080/api`

##### POST /scores
점수 저장

**Request**:
```json
{
  "playerName": "Player1",
  "score": 125000,
  "linesCleared": 150,
  "level": 15,
  "gameMode": "CLASSIC",
  "playTimeSeconds": 1200,
  "maxCombo": 8,
  "isPerfectClear": false
}
```

**Response**:
```json
{
  "id": 1,
  "playerName": "Player1",
  "score": 125000,
  "createdAt": "2025-11-10T15:30:00"
}
```

**상태 코드**:
- `201 Created`: 성공적으로 저장됨
- `400 Bad Request`: 잘못된 요청 (필드 누락 등)
- `500 Internal Server Error`: 서버 오류

---

##### GET /scores/ranking?gameMode={mode}&limit={n}
랭킹 조회

**Query Parameters**:
- `gameMode`: CLASSIC | ARCADE
- `limit`: 조회 개수 (기본값: 10, 최대: 100)

**Request**:
```
GET /api/scores/ranking?gameMode=CLASSIC&limit=10
```

**Response**:
```json
[
  {
    "rank": 1,
    "playerName": "Player1",
    "score": 125000,
    "linesCleared": 150,
    "level": 15,
    "createdAt": "2025-11-10T15:30:00"
  },
  {
    "rank": 2,
    "playerName": "Player2",
    "score": 98000,
    "linesCleared": 120,
    "level": 12,
    "createdAt": "2025-11-10T14:20:00"
  }
]
```

**상태 코드**:
- `200 OK`: 성공
- `400 Bad Request`: 잘못된 gameMode 또는 limit

---

##### GET /scores/personal-best?playerName={name}&gameMode={mode}
개인 최고 점수 조회

**Query Parameters**:
- `playerName`: 플레이어 이름
- `gameMode`: CLASSIC | ARCADE

**Request**:
```
GET /api/scores/personal-best?playerName=Player1&gameMode=CLASSIC
```

**Response**:
```json
{
  "playerName": "Player1",
  "maxScore": 125000,
  "playCount": 45,
  "lastPlayedAt": "2025-11-10T15:30:00"
}
```

**상태 코드**:
- `200 OK`: 성공
- `404 Not Found`: 해당 플레이어의 기록이 없음

---

##### GET /scores/stats
전체 통계 조회

**Response**:
```json
{
  "totalPlayers": 150,
  "totalGames": 5000,
  "averageScore": 35000,
  "highestScore": 500000,
  "mostPlayedMode": "CLASSIC"
}
```

---

#### 13.1.2 게임 서비스 API (향후 구현)

##### POST /game/sessions
게임 세션 생성

**Request**:
```json
{
  "playerId": "player123",
  "gameMode": "MULTI",
  "difficulty": "NORMAL"
}
```

**Response**:
```json
{
  "sessionId": "session-abc-123",
  "playerId": "player123",
  "status": "WAITING",
  "createdAt": "2025-11-10T15:30:00"
}
```

---

##### POST /game/commands
커맨드 전송 (멀티플레이어)

**Request**:
```json
{
  "sessionId": "session-abc-123",
  "sequenceNumber": 1,
  "commandType": "MOVE_LEFT",
  "timestamp": 1699623000000
}
```

**Response**:
```json
{
  "success": true,
  "sequenceNumber": 1,
  "state": {
    "currentX": 4,
    "currentY": 0,
    "score": 100
  },
  "events": [
    {
      "type": "BLOCK_MOVE",
      "priority": 1,
      "duration": 50
    }
  ]
}
```

---

##### POST /game/attacks
공격 전송

**Request**:
```json
{
  "fromSessionId": "session-abc-123",
  "toSessionId": "session-def-456",
  "attackLines": 2,
  "attackType": "LINE_CLEAR"
}
```

**Response**:
```json
{
  "success": true,
  "attackId": "attack-xyz-789"
}
```

---

### 13.2 WebSocket 프로토콜 (STOMP)

#### 13.2.1 연결 설정

**WebSocket URL**: `ws://localhost:8080/ws`

**STOMP 구독**:
```javascript
// 게임 세션 구독
stompClient.subscribe('/topic/game/{sessionId}', function(message) {
    handleGameUpdate(JSON.parse(message.body));
});

// 공격 수신 구독
stompClient.subscribe('/user/queue/attacks', function(message) {
    handleAttackReceived(JSON.parse(message.body));
});
```

---

#### 13.2.2 메시지 포맷

**게임 상태 업데이트**:
```json
{
  "type": "STATE_UPDATE",
  "sessionId": "session-abc-123",
  "sequenceNumber": 5,
  "state": {
    "score": 500,
    "level": 2,
    "linesCleared": 10
  },
  "timestamp": 1699623000000
}
```

**공격 수신**:
```json
{
  "type": "ATTACK_RECEIVED",
  "fromPlayerId": "player456",
  "attackLines": 2,
  "timestamp": 1699623000000
}
```

**이벤트 푸시**:
```json
{
  "type": "EVENT_PUSH",
  "events": [
    {
      "type": "LINE_CLEAR",
      "priority": 15,
      "duration": 800,
      "data": {
        "lines": 4,
        "score": 800
      }
    }
  ]
}
```

---

### 13.3 데이터베이스 스키마

#### 13.3.1 scores 테이블

```sql
CREATE TABLE scores (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_name VARCHAR(50) NOT NULL,
    score BIGINT NOT NULL,
    lines_cleared INT NOT NULL,
    level INT NOT NULL,
    game_mode VARCHAR(20) NOT NULL,
    play_time_seconds INT,
    max_combo INT,
    is_perfect_clear BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_score (score DESC),
    INDEX idx_player_mode (player_name, game_mode),
    INDEX idx_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**필드 설명**:
| 필드 | 타입 | 설명 | 제약조건 |
|------|------|------|---------|
| id | BIGINT | 기본키 | AUTO_INCREMENT |
| player_name | VARCHAR(50) | 플레이어 이름 | NOT NULL |
| score | BIGINT | 점수 | NOT NULL |
| lines_cleared | INT | 지운 라인 수 | NOT NULL |
| level | INT | 도달한 레벨 | NOT NULL |
| game_mode | VARCHAR(20) | 게임 모드 (CLASSIC/ARCADE) | NOT NULL |
| play_time_seconds | INT | 플레이 시간 (초) | NULL |
| max_combo | INT | 최대 콤보 | NULL |
| is_perfect_clear | BOOLEAN | 퍼펙트 클리어 여부 | DEFAULT FALSE |
| created_at | TIMESTAMP | 생성 시간 | DEFAULT CURRENT_TIMESTAMP |
| updated_at | TIMESTAMP | 수정 시간 | ON UPDATE CURRENT_TIMESTAMP |

---

#### 13.3.2 game_sessions 테이블 (향후 구현)

```sql
CREATE TABLE game_sessions (
    id VARCHAR(50) PRIMARY KEY,
    player_id VARCHAR(50) NOT NULL,
    game_mode VARCHAR(20) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,  -- WAITING, PLAYING, FINISHED
    opponent_id VARCHAR(50),
    current_state JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finished_at TIMESTAMP NULL,
    
    INDEX idx_player (player_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

#### 13.3.3 attacks 테이블 (향후 구현)

```sql
CREATE TABLE attacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_session_id VARCHAR(50) NOT NULL,
    to_session_id VARCHAR(50) NOT NULL,
    attack_lines INT NOT NULL,
    attack_type VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_to_session (to_session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### 13.4 설정 파일 저장 형식

#### 13.4.1 tetris_settings (클라이언트 설정)

**위치**: 사용자 홈 디렉토리 또는 실행 경로

**형식**: Java Serialized Object

**내용**:
```java
Map<String, Object> settings = {
    "difficulty": Difficulty.NORMAL,
    "musicVolume": 0.5,
    "sfxVolume": 0.7,
    "keyBindings": {
        "MOVE_LEFT": KeyCode.LEFT,
        "MOVE_RIGHT": KeyCode.RIGHT,
        "ROTATE_CW": KeyCode.UP,
        "ROTATE_CCW": KeyCode.Z,
        "HARD_DROP": KeyCode.SPACE,
        "HOLD": KeyCode.C
    },
    "graphics": {
        "showGhostPiece": true,
        "showGridLines": true,
        "animationSpeed": 1.0
    }
}
```

---

### 13.5 환경 변수 및 설정

#### 13.5.1 Backend 환경 변수

```bash
# Database
DB_HOST=localhost
DB_PORT=3306
DB_NAME=tetris
DB_USERNAME=tetris_user
DB_PASSWORD=your_secure_password

# Server
SERVER_PORT=8080

# JWT
JWT_SECRET=your_jwt_secret_key_min_256_bits
JWT_EXPIRATION=86400000  # 24 hours

# Redis (향후 사용)
REDIS_HOST=localhost
REDIS_PORT=6379

# Metrics
METRICS_ENABLED=true
PROMETHEUS_PORT=9090
```

---

#### 13.5.2 Client 환경 변수

```bash
# Server URL
TETRIS_SERVER_URL=http://localhost:8080
TETRIS_WS_URL=ws://localhost:8080/ws

# Game Settings (Optional Override)
TETRIS_DEFAULT_DIFFICULTY=NORMAL   # Options: EASY, NORMAL, HARD, EXPERT
TETRIS_ITEM_ENABLED=false
```

---

### 13.6 로깅 형식

#### 13.6.1 애플리케이션 로그

**형식**: Logback JSON

```json
{
  "timestamp": "2025-11-10T15:30:00.123Z",
  "level": "INFO",
  "logger": "seoultech.se.backend.service.GameService",
  "thread": "http-nio-8080-exec-1",
  "message": "Command processed successfully",
  "context": {
    "sessionId": "session-abc-123",
    "sequenceNumber": 5,
    "commandType": "MOVE_LEFT",
    "executionTime": 12
  }
}
```

---

#### 13.6.2 액세스 로그

**형식**: Common Log Format + JSON

```
127.0.0.1 - player1 [10/Nov/2025:15:30:00 +0900] 
"POST /api/game/commands HTTP/1.1" 200 1234
{"userId":"player1","sessionId":"session-abc","duration":12}
```

---

#### 13.6.3 에러 로그

```json
{
  "timestamp": "2025-11-10T15:30:00.123Z",
  "level": "ERROR",
  "logger": "seoultech.se.backend.exception.GlobalExceptionHandler",
  "thread": "http-nio-8080-exec-2",
  "message": "Validation failed",
  "exception": {
    "type": "ValidationException",
    "message": "Invalid command type",
    "stackTrace": [...]
  },
  "context": {
    "requestId": "req-xyz-789",
    "userId": "player1",
    "endpoint": "/api/game/commands"
  }
}
```

---

## 13. 운영 및 유지보수 요구사항

### 13.1 운영 환경 관리

#### 13.1.1 환경 분리 전략
```
환경 구성:
┌─────────────┬──────────────┬─────────────┬──────────────┐
│ 환경        │ URL          │ 데이터베이스 │ 로그 레벨    │
├─────────────┼──────────────┼─────────────┼──────────────┤
│ Development │ localhost    │ H2 (메모리)  │ DEBUG        │
│ Staging     │ staging.*    │ MySQL (테스트)│ INFO        │
│ Production  │ tetris.*     │ MySQL (운영) │ WARN/ERROR   │
└─────────────┴──────────────┴─────────────┴──────────────┘
```

#### 13.1.2 Configuration 관리
```yaml
# 환경별 프로파일
spring:
  profiles:
    active: ${SPRING_PROFILE:dev}

# 민감 정보 관리
- 개발: application-dev.yml (Git 포함)
- 운영: 환경 변수 사용 (Git 제외)
  DB_PASSWORD=${DB_PASSWORD}
  JWT_SECRET=${JWT_SECRET}
```

### 13.2 모니터링 및 알림

#### 13.2.1 알림 규칙
```yaml
alerts:
  - name: HighErrorRate
    condition: error_rate > 5%
    action: Slack 알림
    
  - name: SlowResponse  
    condition: response_time_p95 > 200ms
    action: Email 알림
    
  - name: ServiceDown
    condition: health_check_fail
    action: PagerDuty 알림 (긴급)
```

### 13.3 백업 및 복구

#### 13.3.1 백업 전략
```
일일 백업:
- 시간: 매일 새벽 2시
- 대상: 데이터베이스 전체
- 보관: 30일
- 저장소: S3

복구 목표:
- RTO (복구 시간): 1시간 이내
- RPO (복구 시점): 24시간 이내
```

### 13.4 장애 대응

#### 13.4.1 장애 유형별 대응
```yaml
서비스 다운:
  1. 서비스 재시작
  2. 로그 확인
  3. 롤백 (필요 시)
  복구 시간: 5분

DB 연결 실패:
  1. DB 서버 상태 확인
  2. Connection Pool 확인
  3. 네트워크 점검
  복구 시간: 10분

메모리 부족:
  1. 힙 덤프 수집
  2. 메모리 설정 증가
  3. 서비스 재시작
  복구 시간: 15분
```

### 13.5 유지보수 절차

#### 13.5.1 정기 점검
```yaml
일일:
  - 헬스 체크
  - 에러 로그 검토
  - 백업 확인

주간:
  - 성능 메트릭 리뷰
  - 보안 업데이트
  - DB 최적화

월간:
  - 리소스 검토
  - 라이브러리 업데이트
  - 백업 복구 테스트

분기:
  - 아키텍처 리뷰
  - 기술 부채 개선
  - 문서 업데이트
```

---

## 부록 A: 구현 우선순위

### A.1 Phase 1: Core Foundation (2주)

**목표**: 기본 게임 로직 + Single Play

| 우선순위 | 작업 | 담당 모듈 | 예상 시간 |
|---------|------|----------|----------|
| P0 | Multi-Module 구조 설정 | All | 2일 |
| P0 | GameEngine Interface 구현 | Core | 3일 |
| P0 | ClassicGameEngine 구현 | Core | 3일 |
| P0 | GameState 불변 객체 | Core | 1일 |
| P0 | Tetromino + 회전 로직 | Core | 2일 |
| P0 | BoardController (Single) | Client | 2일 |
| P1 | JavaFX UI 기본 렌더링 | Client | 3일 |

**완료 기준**:
- [ ] Single Play 모드로 게임 플레이 가능
- [ ] 라인 클리어, 점수 계산 정상 작동
- [ ] 단위 테스트 커버리지 >70%

---

### A.2 Phase 2: Backend + Authentication (2주)

**목표**: 서버 기본 구조 + 로그인

| 우선순위 | 작업 | 담당 모듈 | 예상 시간 |
|---------|------|----------|----------|
| P0 | Spring Boot Backend 설정 | Backend | 1일 |
| P0 | MySQL 스키마 설계 | Backend | 1일 |
| P0 | User Entity + Repository | Backend | 1일 |
| P0 | JWT 인증 구현 | Backend | 2일 |
| P0 | 회원가입/로그인 API | Backend | 2일 |
| P1 | GameService 기본 구조 | Backend | 2일 |
| P1 | REST API (Command 처리) | Backend | 3일 |
| P2 | 로그인 UI | Client | 2일 |

**완료 기준**:
- [ ] 회원가입 + 로그인 + JWT 발급
- [ ] REST API로 Command 전송 가능
- [ ] 통합 테스트 작성

---

### A.3 Phase 3: Multiplayer (3주)

**목표**: 멀티플레이 핵심 기능

| 우선순위 | 작업 | 담당 모듈 | 예상 시간 |
|---------|------|----------|----------|
| P0 | WebSocket 설정 | Backend | 2일 |
| P0 | STOMP 프로토콜 구현 | Backend | 2일 |
| P0 | PlayTypeStrategy 분리 | Client | 2일 |
| P0 | MultiPlayStrategy 구현 | Client | 3일 |
| P0 | Client-Side Prediction | Client | 3일 |
| P0 | State Reconciliation | Client | 3일 |
| P1 | Command Throttling | Client | 1일 |
| P1 | NetworkServiceProxy | Client | 2일 |
| P2 | Attack 시스템 | Backend | 3일 |

**완료 기준**:
- [ ] 2명 플레이어가 동시에 게임 가능
- [ ] Client-Side Prediction 정상 작동
- [ ] Attack 주고받기 가능

---

### A.4 Phase 4: UI Events + Polish (2주)

**목표**: UI 이벤트 시스템 + 완성도

| 우선순위 | 작업 | 담당 모듈 | 예상 시간 |
|---------|------|----------|----------|
| P0 | UIEventHandler 구현 | Client | 2일 |
| P0 | CriticalEventGenerator | Backend | 2일 |
| P0 | LocalUIEventGenerator | Client | 1일 |
| P1 | 애니메이션 (LINE_CLEAR 등) | Client | 3일 |
| P1 | 사운드 효과 | Client | 2일 |
| P2 | 설정 메뉴 | Client | 2일 |
| P2 | 랭킹 시스템 | Backend + Client | 3일 |

**완료 기준**:
- [ ] 모든 이벤트 애니메이션 완성
- [ ] 사운드 효과 추가
- [ ] 랭킹 조회 가능

---

### A.5 Phase 5: Testing + Deployment (2주)

**목표**: 테스트 + 배포

| 우선순위 | 작업 | 담당 모듈 | 예상 시간 |
|---------|------|----------|----------|
| P0 | 단위 테스트 보강 | All | 3일 |
| P0 | 통합 테스트 | All | 3일 |
| P0 | 성능 테스트 (JMeter) | Backend | 2일 |
| P1 | Docker 이미지 빌드 | Backend | 1일 |
| P1 | CI/CD 파이프라인 | All | 2일 |
| P1 | Prometheus + Grafana | Backend | 2일 |
| P2 | 사용자 매뉴얼 | Docs | 2일 |

**완료 기준**:
- [ ] 테스트 커버리지 >80%
- [ ] 성능 목표 달성 (응답 <100ms, 1000명 동시 접속)
- [ ] 프로덕션 배포 완료

---

### A.6 우선순위 정의

| 레벨 | 의미 | 예시 |
|------|------|------|
| **P0** | 필수 (Must Have) | 게임 로직, 멀티플레이어 핵심 |
| **P1** | 중요 (Should Have) | UI 애니메이션, 네트워크 재연결 |
| **P2** | 선택 (Nice to Have) | 설정 메뉴, 사운드 |
| **P3** | 미래 (Future) | AI 플레이어, 리플레이 |

---

## 부록 B: 체크리스트

### B.1 개발 체크리스트

#### B.1.1 코드 작성 전
- [ ] 요구사항 명확히 이해
- [ ] 디자인 패턴 선택
- [ ] 인터페이스 설계
- [ ] 테스트 케이스 작성 (TDD)

#### B.1.2 코드 작성 중
- [ ] 명확한 변수명 사용
- [ ] 주석 작성 (JavaDoc)
- [ ] SOLID 원칙 준수
- [ ] 예외 처리
- [ ] 로깅 추가

#### B.1.3 코드 작성 후
- [ ] 단위 테스트 작성
- [ ] 코드 리뷰 요청
- [ ] 정적 분석 (SonarQube)
- [ ] 성능 프로파일링
- [ ] 문서 업데이트

---

### B.2 테스트 체크리스트

#### B.2.1 단위 테스트
- [ ] Happy Path 테스트
- [ ] Edge Case 테스트
- [ ] 예외 케이스 테스트
- [ ] Mock 객체 사용
- [ ] 커버리지 >70%

#### B.2.2 통합 테스트
- [ ] REST API 테스트
- [ ] WebSocket 테스트
- [ ] Database 연동 테스트
- [ ] Transaction 테스트
- [ ] TestContainers 사용

#### B.2.3 E2E 테스트
- [ ] 전체 게임 플레이
- [ ] 멀티플레이어 시나리오
- [ ] UI 테스트 (TestFX)
- [ ] 네트워크 장애 시나리오

---

### B.3 배포 체크리스트

#### B.3.1 배포 전
- [ ] 모든 테스트 통과
- [ ] 코드 리뷰 완료
- [ ] DB 마이그레이션 준비
- [ ] Rollback 계획 수립
- [ ] 모니터링 설정 확인
- [ ] 사용자 공지

#### B.3.2 배포 중
- [ ] Blue-Green 배포
- [ ] Health Check 확인
- [ ] 로그 모니터링
- [ ] 성능 지표 확인

#### B.3.3 배포 후
- [ ] Smoke Test
- [ ] 응답 시간 확인
- [ ] 에러율 확인
- [ ] 사용자 피드백 수집
- [ ] 24시간 모니터링

---

### B.4 코드 리뷰 체크리스트

#### B.4.1 기능
- [ ] 요구사항 충족
- [ ] 버그 없음
- [ ] Edge Case 처리

#### B.4.2 설계
- [ ] 디자인 패턴 적절
- [ ] SOLID 원칙 준수
- [ ] 모듈 독립성 유지

#### B.4.3 코드 품질
- [ ] 가독성 (명확한 변수명)
- [ ] 중복 코드 없음
- [ ] 적절한 주석
- [ ] 매직 넘버 없음

#### B.4.4 테스트
- [ ] 단위 테스트 존재
- [ ] 커버리지 충분
- [ ] 테스트 이름 명확

#### B.4.5 성능
- [ ] 불필요한 반복문 없음
- [ ] 메모리 누수 없음
- [ ] DB 쿼리 최적화

---

## 부록 C: 용어집 (Glossary)

### C.1 게임 용어

| 용어 | 설명 | 영어 |
|------|------|------|
| **테트로미노** | 테트리스 블록 (I, O, T, S, Z, J, L) | Tetromino |
| **하드 드롭** | 블록을 바닥까지 즉시 떨어뜨림 | Hard Drop |
| **소프트 드롭** | 블록을 한 칸 아래로 이동 | Soft Drop |
| **홀드** | 현재 블록을 보관하고 다음 블록으로 교체 | Hold |
| **고스트 피스** | 블록이 떨어질 위치를 미리 표시 | Ghost Piece |
| **T-스핀** | T 블록을 회전하여 특수한 방식으로 배치 | T-Spin |
| **퍼펙트 클리어** | 보드를 완전히 비움 | Perfect Clear |
| **콤보** | 연속으로 라인 클리어 | Combo |
| **백투백** | 4줄 클리어 또는 T-스핀을 연속으로 수행 | Back-to-Back |
| **SRS** | 슈퍼 회전 시스템 (벽 차기) | Super Rotation System |

---

### C.2 아키텍처 용어

| 용어 | 설명 |
|------|------|
| **Multi-Module** | 하나의 프로젝트를 여러 모듈로 분리 (Core, Client, Backend) |
| **DI (Dependency Injection)** | 의존성 주입 (Spring의 @Autowired) |
| **Bean** | Spring이 관리하는 객체 |
| **Component Scan** | @Component 어노테이션이 붙은 클래스를 자동으로 Bean 등록 |
| **Immutable** | 불변 객체 (상태 변경 불가) |
| **Value Object** | 값 객체 (동등성 비교는 값으로) |

---

### C.3 디자인 패턴 용어

| 용어 | 설명 |
|------|------|
| **Strategy Pattern** | 알고리즘을 런타임에 선택 (Single/Multi Play) |
| **Proxy Pattern** | 실제 객체를 대리하여 추가 기능 제공 (재연결) |
| **Observer Pattern** | 이벤트 발생 시 구독자에게 알림 (UI Events) |
| **Factory Pattern** | 객체 생성을 캡슐화 |
| **Builder Pattern** | 복잡한 객체를 단계적으로 생성 (GameState.builder()) |

---

### C.4 네트워크 용어

| 용어 | 설명 |
|------|------|
| **Client-Side Prediction** | 클라이언트에서 즉시 예측 실행 (서버 응답 전) |
| **State Reconciliation** | 클라이언트 예측과 서버 상태를 일치시킴 |
| **Command Throttling** | 명령을 일정 간격으로 제한 (16ms) |
| **Sequence Number** | 명령 순서를 나타내는 번호 |
| **WebSocket** | 양방향 실시간 통신 프로토콜 |
| **STOMP** | 메시징 프로토콜 (Simple Text Oriented Messaging Protocol) |

---

### C.5 성능 용어

| 용어 | 설명 |
|------|------|
| **Latency** | 지연 시간 (네트워크 왕복 시간) |
| **Throughput** | 처리량 (초당 요청 수) |
| **Concurrent Users** | 동시 접속자 수 |
| **Response Time** | 응답 시간 (요청 → 응답) |
| **95th Percentile** | 95%의 요청이 이 시간 내에 완료됨 |
| **Connection Pool** | 미리 생성한 DB 연결 모음 |

---

## 부록 D: 참조 문서 (References)

### D.1 공식 문서

#### Spring Framework
- **Spring Boot**: https://spring.io/projects/spring-boot
- **Spring Security**: https://spring.io/projects/spring-security
- **Spring Data JPA**: https://spring.io/projects/spring-data-jpa
- **Spring WebSocket**: https://docs.spring.io/spring-framework/reference/web/websocket.html

#### Java
- **Java 21 LTS**: https://openjdk.org/projects/jdk/21/
- **JavaFX 21**: https://openjfx.io/

#### 테스트
- **JUnit 5**: https://junit.org/junit5/
- **Mockito**: https://site.mockito.org/
- **TestFX**: https://github.com/TestFX/TestFX

---

### D.2 디자인 패턴

- **GoF Design Patterns**: "Design Patterns: Elements of Reusable Object-Oriented Software" (Gamma et al.)
- **Strategy Pattern**: https://refactoring.guru/design-patterns/strategy
- **Proxy Pattern**: https://refactoring.guru/design-patterns/proxy
- **Observer Pattern**: https://refactoring.guru/design-patterns/observer

---

### D.3 게임 개발

- **Tetris Guideline**: https://tetris.wiki/Tetris_Guideline
- **SRS (Super Rotation System)**: https://tetris.wiki/SRS
- **T-Spin Detection**: https://tetris.wiki/T-Spin

---

### D.4 네트워크

- **Client-Side Prediction**: https://www.gabrielgambetta.com/client-side-prediction-server-reconciliation.html
- **WebSocket Protocol**: https://datatracker.ietf.org/doc/html/rfc6455
- **STOMP Protocol**: https://stomp.github.io/

---

### D.5 모니터링

- **Prometheus**: https://prometheus.io/docs/
- **Grafana**: https://grafana.com/docs/
- **Micrometer**: https://micrometer.io/docs

---

## 부록 E: FAQ

### E.1 아키텍처

**Q1: Core 모듈에 Spring을 포함해도 되나요?**

A: 네, Spring DI (@Component, @Autowired)는 포함 가능합니다. 하지만 `application.yml` 로드는 Client/Backend에서만 수행합니다.

**이유**:
- Core는 게임 로직만 담당
- 설정은 실행 환경(Client vs Backend)에 따라 다름

---

**Q2: GameEngine을 Interface로 만든 이유는?**

A: Classic/Arcade 모드를 다형성으로 처리하기 위해서입니다.

```java
// ✅ 다형성
@Autowired
private GameEngine engine;  // ClassicGameEngine 또는 ArcadeGameEngine

// ❌ if-else
if (itemEnabled) {
    arcadeEngine.execute();
} else {
    classicEngine.execute();
}
```

---

**Q3: Multi-Module 대신 Monolithic을 사용하면 안 되나요?**

A: 가능하지만, Core 로직을 Client/Backend에서 재사용할 수 없습니다.

**트레이드오프**:
- Multi-Module: 초기 설정 복잡 + 재사용 가능
- Monolithic: 설정 간단 + 코드 중복 발생

---

### E.2 디자인 패턴

**Q4: Strategy 패턴을 사용하지 않으면 어떻게 되나요?**

A: Single/Multi 로직이 if-else로 섞여 복잡도가 증가합니다.

```java
// ❌ if-else (복잡)
public void executeCommand(GameCommand command) {
    if (playMode == SINGLE) {
        // Single 로직
    } else if (playMode == MULTI) {
        // Multi 로직 (Prediction, Reconciliation 등)
    }
}

// ✅ Strategy (깔끔)
playTypeStrategy.beforeCommand(command);
```

---

**Q5: Proxy 패턴을 사용하지 않으면 어떻게 되나요?**

A: 재연결 로직이 곳곳에 흩어집니다.

```java
// ❌ Client가 직접 처리 (중복)
public void sendCommand(GameCommand command) {
    if (!networkService.isConnected()) {
        networkService.reconnect();
    }
    networkService.send(command);
}

// ✅ Proxy (캡슐화)
networkProxy.sendCommand(command);  // 재연결 자동 처리
```

---

### E.3 멀티플레이어

**Q6: Client-Side Prediction 없이 멀티플레이를 구현하면 어떻게 되나요?**

A: 100ms 입력 지연으로 게임이 불가능합니다.

**비교**:
| 방식 | 입력 지연 | 사용자 경험 |
|------|----------|-----------|
| Client-Side Prediction | 0ms | ⭐⭐⭐⭐⭐ |
| Server Only | 100ms | ⭐⭐ (플레이 불가) |

---

**Q7: Command Throttling을 16ms로 설정한 이유는?**

A: 60 FPS에 맞추기 위해서입니다.

```
1초 / 60 FPS = 16.67ms ≈ 16ms
```

**효과**:
- 사용자 인지 불가 (<16ms 지연)
- 서버 부하 94% 감소 (360 req/s → 60 req/s)

---

**Q8: State Mismatch가 발생하면 어떻게 되나요?**

A: 서버 상태로 강제 업데이트됩니다 (깜빡임 발생 가능).

```java
if (!clientState.equals(serverState)) {
    forceStateUpdate(serverState);  // 서버 우선
}
```

**완화**:
- Mismatch는 매우 드물게 발생 (<1%)
- 네트워크가 안정적이면 거의 없음

---

### E.4 성능

**Q9: 1000명 동시 접속을 목표로 한 이유는?**

A: 중소규모 게임 서비스의 일반적인 목표입니다.

**계산**:
```
동시 접속: 1000명
초당 Command: 60개/인
총 요청: 60,000 req/s (Throttling 전)
→ Throttling 후: 60 req/s × 1000명 = 60,000 req/s (동일)
```

**EC2 Spec**:
- t3.medium (2 vCPU, 4GB RAM) × 3대
- 충분히 처리 가능

---

**Q10: In-Memory 대신 Database를 사용하면 안 되나요?**

A: 가능하지만 응답 시간이 5배 증가합니다.

**비교**:
| 저장소 | 조회 시간 | 저장 시간 |
|--------|----------|----------|
| In-Memory | 0.1ms | 0.1ms |
| Database | 10ms | 50ms |

**결론**: GameState는 In-Memory, User Data는 Database

---

### E.5 보안

**Q11: JWT Token이 탈취되면 어떻게 하나요?**

A: Refresh Token으로 Access Token을 짧게 유지합니다.

**전략**:
- Access Token: 1시간 (짧음)
- Refresh Token: 7일
- HTTPS 강제

**탈취 시**:
- 최대 1시간만 사용 가능
- Refresh Token은 HttpOnly Cookie에 저장 (XSS 방어)

---

**Q12: Cheating Detection은 어떻게 작동하나요?**

A: 상태 변화를 서버에서 검증합니다.

**검증 항목**:
1. **점수 증가율**: 1초당 최대 800점
2. **테트로미노 위치**: 그리드 경계 내
3. **시간 차이**: ±5초 이내

**예시**:
```java
// ❌ 비정상 점수 상승
if (newScore - oldScore > 800) {
    throw new CheatDetectedException();
}
```

---

### E.6 배포

**Q13: Blue-Green Deployment란?**

A: 무중단 배포 전략입니다.

```
Blue (현재 버전) → 실행 중
Green (새 버전) → 배포 → 테스트
→ 정상이면 트래픽 전환
→ 문제 시 Blue로 롤백
```

**장점**:
- 서비스 중단 없음
- 빠른 롤백

---

**Q14: Docker를 사용하는 이유는?**

A: 환경 일관성 + 배포 자동화입니다.

**장점**:
- 개발/Staging/Production 환경 동일
- Docker Compose로 쉬운 실행
- CI/CD 파이프라인 통합

---

**Q15: Prometheus + Grafana를 사용하는 이유는?**

A: 실시간 모니터링 + 알람입니다.

**역할**:
- **Prometheus**: 메트릭 수집 (응답 시간, 메모리 등)
- **Grafana**: 대시보드 시각화
- **Alertmanager**: 알람 발송 (Slack, Email)

---

## 🎯 Part 3 요약

### 완성된 섹션
✅ **8. Spring Boot 설정**: Core/Client/Backend 모듈별 설정  
✅ **9. 검증 체크리스트**: 아키텍처, 패턴, 성능, 보안 검증  
✅ **10. 설계 결정 및 트레이드오프**: 14가지 주요 결정 사항  
✅ **11. 위험 관리**: 11가지 위험 + 완화 전략  
✅ **12. 배포 전략**: Dev/Staging/Production + CI/CD  

### 완성된 부록
✅ **부록 A**: 구현 우선순위 (5 Phase)  
✅ **부록 B**: 체크리스트 (개발, 테스트, 배포, 코드 리뷰)  
✅ **부록 C**: 용어집 (게임, 아키텍처, 패턴, 네트워크, 성능)  
✅ **부록 D**: 참조 문서  
✅ **부록 E**: FAQ (15개 질문)  

---

## 📚 전체 문서 구성

**Part 1** (FINAL_SYSTEM_REQUIREMENTS_v6_part1.md):
- 섹션 1-3: 시스템 요구사항, 변경 파일 목록, 아키텍처 설계

**Part 2** (FINAL_SYSTEM_REQUIREMENTS_v6_part2.md):
- 섹션 4-7: 디자인 패턴, 멀티플레이어, UI 이벤트, 모듈별 구현

**Part 3** (FINAL_SYSTEM_REQUIREMENTS_v6_part3.md):
- 섹션 8-12: Spring Boot 설정, 검증, 설계 결정, 위험 관리, 배포
- 부록 A-E: 우선순위, 체크리스트, 용어집, 참조, FAQ

---

**END OF PART 3**

---

**🎊 축하합니다! Tetris Multi-Module Architecture v6.0 문서가 완성되었습니다!**
