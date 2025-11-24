/*
 * Tetris Client Module (JavaFX Desktop Application)
 * 🖥️ JavaFX 21 LTS + Spring Boot DI Container 통합
 * - JavaFX가 메인 애플리케이션 (GUI)
 * - Spring Boot는 서비스 레이어 (DI 컨테이너)
 * - Java 21의 Virtual Threads, 향상된 concurrent 기능 활용
 */
plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.javafx)
    application  // JavaFX 애플리케이션
}

// 🌱 Spring Boot 설정
springBoot {
    mainClass = "seoultech.se.client.TetrisApplication"
}

description = "Tetris JavaFX Desktop Application with Java 21 LTS"

// 🎮 JavaFX 21 LTS 설정 (Java 21과 완벽 호환)
javafx {
    version = "21"
    modules = listOf(
        "javafx.controls",
        "javafx.fxml"
    )
}

// 🚀 메인 애플리케이션 설정
application {
    mainClass.set("seoultech.se.client.TetrisApplication")
}

dependencies {
    // 🎯 Core & Backend 모듈 의존성
    implementation(project(":tetris-core"))
    implementation(project(":tetris-backend"))
    
    // 🌱 Spring Boot Bundle (DI container + JPA)
    implementation(libs.bundles.client.spring)
    implementation(libs.backend.spring.boot.starter.validation)
    annotationProcessor(libs.client.spring.boot.configuration.processor)
    
    // 🗄️ H2 Database (로컬 저장소)
    runtimeOnly(libs.backend.h2.database)
    
    // 🎨 JavaFX Bundle (Desktop UI)
    implementation(libs.bundles.client.javafx)
    
    // 📊 Utility Libraries
    implementation(libs.common.commons.lang3)
    
    // ============================================================================
    // 🚀 COMMON DEPENDENCIES (모든 모듈 공통)  
    // ============================================================================
    
    // 🛠️ Development Tools
    compileOnly(libs.common.lombok)
    annotationProcessor(libs.common.lombok)
    testCompileOnly(libs.common.lombok)
    testAnnotationProcessor(libs.common.lombok)
    
    // 🧪 Testing Dependencies
    testImplementation(libs.client.spring.boot.starter.test)
    testImplementation(libs.bundles.common.testing)
    
    // 🧪 JavaFX Testing Dependencies (TestFX)
    testImplementation(libs.client.testfx.core)
    testImplementation(libs.client.testfx.junit5)
    testImplementation(libs.client.monocle)
}

// 🚀 실행 설정 (JavaFX + Java 21 최적화 - 단순화됨)
val javafxJvmArgs = listOf(
    // JavaFX 핵심 모듈 접근만 허용 (필수 최소한)
    "--add-opens", "javafx.graphics/com.sun.javafx.application=ALL-UNNAMED",
    "--add-opens", "javafx.controls/com.sun.javafx.scene.control=ALL-UNNAMED",
    
    // Spring Boot 기본 리플렉션 지원
    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
    "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED"
)

tasks.run.configure {
    jvmArgs(javafxJvmArgs)
}

// Spring Boot 실행을 위한 설정 (단순화됨)
tasks.bootRun.configure {
    jvmArgs(javafxJvmArgs)
}

// 📦 실행 가능한 JAR 설정
tasks.bootJar {
    archiveBaseName.set("tetris-desktop-app-java21")
    enabled = true
    
    // Spring Boot 자동 Main-Class 설정 사용
    // Spring Boot가 자동으로 JarLauncher를 Main-Class로 설정
    manifest {
        attributes(
            "Implementation-Title" to "Tetris Desktop Game (Java 21 LTS)",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "SeoulTech SE Team 9"
        )
    }
}

// 🧪 테스트 설정 (루트에서 상속받아 일관성 확보)
tasks.test {
    useJUnitPlatform()
    
    // JavaFX 테스트를 위한 최소 필수 설정만 추가
    jvmArgs(
        "--add-opens", "javafx.graphics/com.sun.javafx.application=ALL-UNNAMED"
    )
    
    // Spring Boot 테스트 환경 설정 + TestFX Headless 설정
    systemProperties(
        "spring.profiles.active" to "test",
        // TestFX Headless 모드 설정 (CI/CD 환경 지원)
        "testfx.robot" to "glass",
        "testfx.headless" to "true",
        "prism.order" to "sw",
        "prism.text" to "t2k",
        "glass.platform" to "Monocle",
        "monocle.platform" to "Headless"
    )
    
    // 🔍 테스트 로그를 터미널에 출력 (System.out.println 표시)
    testLogging {
        showStandardStreams = true
        events("passed", "skipped", "failed", "standardOut", "standardError")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showCauses = true
        showStackTraces = true
    }
}

// 🎯 개발 실행 태스크
tasks.register("dev") {
    group = "application"
    description = "Run the desktop application in development mode with Java 21 LTS"
    dependsOn("bootRun")
}

// 🎮 배포용 태스크
tasks.register("dist") {
    group = "distribution" 
    description = "Create distribution package for Java 21 LTS desktop application"
    dependsOn("bootJar")
    
    doLast {
        println("🎮 Tetris Desktop Application (Java 21 LTS) JAR created:")
        println("   Location: ${tasks.bootJar.get().archiveFile.get().asFile}")
        println("   Run with: java -jar ${tasks.bootJar.get().archiveFile.get().asFile.name}")
    }
}
