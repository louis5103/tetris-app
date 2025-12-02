/*
 * Tetris Application - Root Build Configuration
 * Java 21 LTS + Version Catalog 기반 의존성 관리
 */
plugins {
    java
    // Version Catalog에서 플러그인 참조 (Type-Safe)
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.javafx) apply false
    alias(libs.plugins.versions)
}

// 모든 하위 모듈에 공통 적용될 설정
subprojects {
    group = "seoultech.se"
    version = "1.0.0-SNAPSHOT"
    
    apply(plugin = "java")
    
    // ✨ Java 21 LTS 설정 (장기 지원 안정 버전)
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
    
    repositories {
        mavenCentral()
    }
    
    // ⚙️ 테스트 설정 (모든 모듈 공통)
    tasks.withType<Test> {
        useJUnitPlatform()
        
        // 테스트 실행 환경 설정
        maxHeapSize = "1g"
        maxParallelForks = 1  // 일관된 병렬 실행 설정
        
        // 테스트 로깅 (표준화)
        testLogging {
            events("passed", "skipped", "failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showStandardStreams = false
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
        
        // 실패 시 즉시 중단하지 않고 모든 테스트 실행
        failFast = false
    }
    
    // 📦 컴파일 설정  
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21) // Java 21 LTS 명시적 설정
        // Java 21 최적화 컴파일러 옵션 (Preview 기능 제거하여 Lombok 호환성 확보)
        options.compilerArgs.addAll(listOf(
            "-Xlint:all",
            "-Xlint:-serial",
            "-parameters" // 매개변수 이름 보존 (Spring DI에 유용)
        ))
    }
    
    // 🚀 실행 설정
    tasks.withType<JavaExec> {
        // Java 21 최적화 옵션 (Preview 기능 제거하여 안정성 확보)
        jvmArgs(
            "--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseZGC", // ZGC 가비지 컬렉터 (Java 21에서 안정화)
            "-XX:+UnlockDiagnosticVMOptions"
        )
    }
}

// 🎯 프로젝트 정보
description = "Tetris Desktop Game - Java 21 LTS Multi-module Application (JavaFX + Spring Boot)"

// 📦 종속성 업데이트 확인 설정
// 💡 [수정된 부분] 아래 블록의 문법을 더 간결하게 수정하여 타입 참조 오류를 해결했습니다.
tasks.named("dependencyUpdates") {
    val task = this as com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
    task.checkForGradleUpdate = true
    task.outputFormatter = "json"
    task.outputDir = "build/dependencyUpdates"
    task.reportfileName = "report"
    task.checkConstraints = true
    
    task.rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

// ⚡ Gradle 성능 최적화
tasks.wrapper {
    gradleVersion = "8.12"
    distributionType = Wrapper.DistributionType.BIN
}
