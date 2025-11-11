/*
 * Tetris Core Module
 * 🎯 순수 Java 알고리즘 및 도메인 로직
 * - Tetris 게임 핵심 알고리즘
 * - 블록, 보드, 점수 등 도메인 모델
 * - 외부 의존성 없는 POJO 구현
 */
plugins {
    `java-library`  // 다른 모듈에서 라이브러리로 사용
}

description = "Tetris Core Domain Logic"

dependencies {
    // 🌱 Spring Framework (Configuration 및 DI용)
    implementation(libs.backend.spring.boot.starter)
    
    // 📊 Utility Libraries
    implementation(libs.common.commons.lang3)
    
    // ✅ Validation (설정값 검증용)
    implementation(libs.common.jakarta.validation.api)
    
    // 🛠️ Development Tools (공통 의존성)
    compileOnly(libs.common.lombok)
    annotationProcessor(libs.common.lombok)
    testCompileOnly(libs.common.lombok)
    testAnnotationProcessor(libs.common.lombok)
    
    // 🧪 Testing Dependencies (공통 번들)
    testImplementation(libs.bundles.common.testing)
}

// 📦 JAR 생성 설정
tasks.jar {
    archiveBaseName.set("tetris-core")
    manifest {
        attributes(
            "Implementation-Title" to "Tetris Core",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "SeoulTech SE Team 9"
        )
    }
}

// ✅ 테스트 실행 설정 (루트에서 상속받아 일관성 확보)
tasks.test {
    useJUnitPlatform()
    
    // 추가 설정이 필요한 경우에만 여기서 오버라이드
    // 기본 설정은 루트 build.gradle.kts에서 상속됨
}
