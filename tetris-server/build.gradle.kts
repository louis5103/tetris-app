/*
 * Tetris Server Module
 * WebSocket 기반 멀티플레이어 게임 서버
 * - WebSocket 통신 처리
 * - 게임 세션 관리
 * - 실시간 동기화
 * - 클라이언트 메시지 핸들링
 */
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    `java-library`
    application
}

description = "Tetris Multiplayer Game Server (WebSocket)"

// 애플리케이션 메인 클래스 설정
application {
    mainClass.set("seoultech.se.server.TetrisServerApplication")
}

dependencies {
    // Core 모듈 의존성
    implementation(project(":tetris-core"))
    implementation(project(":tetris-backend"))

    // ============================================================================
    // SERVER MODULE SPECIFIC DEPENDENCIES
    // ============================================================================

    // Spring Boot Web & WebSocket
    implementation(libs.backend.spring.boot.starter)
    implementation(libs.backend.spring.boot.starter.web)
    implementation("org.springframework.boot:spring-boot-starter-websocket")

    // JSON 처리
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Validation
    implementation(libs.backend.spring.boot.starter.validation)

    // Actuator (모니터링)
    implementation(libs.backend.spring.boot.starter.actuator)

    // JPA & Database
    implementation(libs.backend.spring.boot.starter.data.jpa)
    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly(libs.backend.h2.database)  // H2 for embedded-server profile

    // Security & JWT
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Development Tools
    implementation(libs.bundles.backend.development)
    annotationProcessor(libs.backend.spring.boot.configuration.processor)

    // ============================================================================
    // COMMON DEPENDENCIES
    // ============================================================================

    // Lombok
    compileOnly(libs.common.lombok)
    annotationProcessor(libs.common.lombok)
    testCompileOnly(libs.common.lombok)
    testAnnotationProcessor(libs.common.lombok)

    // Testing Dependencies
    testImplementation(libs.backend.spring.boot.starter.test) {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-websocket")
}

// JAR 빌드 설정
tasks.jar {
    archiveBaseName.set("tetris-server")
    enabled = true
}

// bootRun 설정 (로컬 개발용 - dev 프로파일, MySQL 사용)
tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    systemProperty("spring.profiles.active", "dev")
    systemProperty("server.port", "8090")
}

// bootJar 빌드 설정
tasks.bootJar {
    archiveBaseName.set("tetris-server-standalone")
    enabled = true
    archiveClassifier.set("boot")
    
    // application-embedded-server.yml 또는 application-dev.yml 사용 (profile 기반)
    
    // MANIFEST에 프로파일을 설정하지 않음 (명령줄 인자로만 제어)
    // 이렇게 하면 EmbeddedServerManager의 -Dspring.profiles.active가 작동함
}

// 테스트 설정
tasks.test {
    useJUnitPlatform()
}
