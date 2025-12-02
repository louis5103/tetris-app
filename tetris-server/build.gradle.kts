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

// bootJar 빌드 설정
tasks.bootJar {
    archiveBaseName.set("tetris-server-standalone")
    enabled = true
    archiveClassifier.set("boot")
}

// 테스트 설정
tasks.test {
    useJUnitPlatform()
}
