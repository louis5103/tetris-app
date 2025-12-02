# 🎮 Tetris Desktop Application# 🎮 Tetris Desktop Application (Module Integration Framework)

> Java 21 LTS + Spring Boot + JavaFX Multi-module Project

> **Java 21 LTS + Spring Boot 3.5.6 + JavaFX 21** 멀티모듈 통합 프로젝트

## 📋 목차

## 📋 목차

- [🎯 프로젝트 개요](#-프로젝트-개요)- [🎯 프로젝트 개요](#-프로젝트-개요)

- [🏗️ 모듈 구조](#️-모듈-구조)  - [🏗️ 모듈 구조](#️-모듈-구조)  

- [🚀 빠른 시작](#-빠른-시작)- [🚀 실행 방법](#-실행-방법)

- [📜 스크립트 사용법](#-스크립트-사용법)- [📝 개발 가이드](#-개발-가이드)

- [📚 문서](#-문서)- [🌿 브랜치 네이밍 규칙](#-브랜치-네이밍-규칙)

- [👥 팀 정보](#-팀-정보)

## � 프로젝트 개요

---

**Spring Boot + JavaFX 통합 아키텍처**를 기반으로 한 멀티모듈 개발 프레임워크입니다.

## 🎯 프로젝트 개요

> 🚀 **통합 실행 방법**: `cd tetris-client && ../gradlew run`

**Spring Boot + JavaFX 통합 아키텍처**를 기반으로 한 멀티모듈 개발 프레임워크입니다.> 

> 📚 **상세한 개발 가이드**: [DEVELOPMENT.md](./DEVELOPMENT.md)

### ✨ 주요 특징> 

- **🔗 하이브리드 아키텍처**: JavaFX GUI + Spring Boot DI 컨테이너> 🏗️ **아키텍처 상세**: [ARCHITECTURE.md](readme_files/ARCHITECTURE.md)

- **📦 모듈화**: Core, Backend, Client 계층별 분리

- **🎯 Version Catalog**: 중앙화된 의존성 관리## 🏗️ 모듈 구조

- **🚀 Java 21**: Virtual Threads, 최신 언어 기능 활용

```

---tetris-app/

├── tetris-core/          # 핵심 비즈니스 로직 (공통 라이브러리)

## 🏗️ 모듈 구조├── tetris-backend/       # Spring Boot 웹 서버

├── tetris-client/        # JavaFX 데스크톱 클라이언트  

```├── tetris-swing/         # Swing GUI (옵션)

tetris-app/└── build.gradle.kts      # 루트 프로젝트 설정

├── tetris-core/          # 🎯 핵심 비즈니스 로직 (순수 Java)```

├── tetris-backend/       # ⚙️ Spring Boot 웹 서비스 레이어

├── tetris-client/        # 🖥️ JavaFX + Spring Boot 메인 애플리케이션### 📦 각 모듈별 역할

└── script_files/         # 📜 실행 스크립트들

```- **tetris-core** - 공통 비즈니스 로직 (순수 Java 라이브러리)

  - 도메인 모델 및 핵심 로직 구현

### 의존성 관계  - 다른 모듈에서 공통으로 사용하는 유틸리티

```- **tetris-backend** - Spring Boot 웹 서버

tetris-client → tetris-backend → tetris-core  - REST API 제공 (`@RestController`)

           ↘ → tetris-core  - 비즈니스 서비스 레이어 (`@Service`) 

```  - 데이터 접근 및 영속성 관리

- **tetris-client** - JavaFX 데스크톱 클라이언트

---  - **통합 실행 진입점** (`@SpringBootApplication`)

  - JavaFX GUI 컨트롤러 (`@Component`)

## 🚀 빠른 시작  - Spring Boot + JavaFX 통합 아키텍처



### 1️⃣ 통합 실행 (권장)### 🔄 통합 아키텍처 동작 방식

```bash

./gradlew :tetris-client:run1. **JavaFX Application 시작** → `TetrisApplication.java`

```2. **Spring Boot Context 초기화** → `init()` 메서드에서 DI 컨테이너 생성

→ JavaFX GUI + Spring Boot DI 컨테이너 통합 시작3. **컴포넌트 스캔** → 백엔드와 클라이언트 패키지 전체 스캔

4. **의존성 주입** → JavaFX 컨트롤러에서 Spring 서비스 사용

### 2️⃣ 백엔드 독립 실행

```bash  ```java

./gradlew :tetris-backend:bootRun// 통합 실행 예시: JavaFX에서 Spring 서비스 사용

```@Component

→ REST API 서버 시작 (http://localhost:8080)public class MainController {

    @Autowired

### 3️⃣ 전체 빌드    private GameService gameService;  // 백엔드 서비스 자동 주입

```bash    

./gradlew clean build    @FXML

```    private void handleAction() {

        String status = gameService.getStatus();  // Spring DI 활용

---        // JavaFX UI 업데이트

    }

## 📜 스크립트 사용법}

```

### 기본 실행 스크립트들}

```bash```

# 빌드 + 클라이언트 실행

./script_files/build-and-run.sh## 🏗️ 아키텍처



# 클라이언트만 실행 (JAR 방식)📄 **[상세 아키텍처 가이드](./ARCHITECTURE.md)**를 확인하세요!

./script_files/run-tetris.sh

### 🎆 특징

# 백엔드 서버만 실행

./script_files/run-backend.sh- **하이브리드 아키텍처**: JavaFX가 메인, Spring Boot가 DI 컨테이너

```- **계층형 모듈**: Core → Backend → Client 단방향 의존성

- **프레임워크 격리**: Core 로직은 순수 Java로 구현

### 통합 개발 도구- **의존성 주입**: JavaFX 컨트롤러에서 `@Autowired` 사용

```bash

# 다양한 개발 작업을 위한 통합 스크립트```

./tetris.sh [build|client|backend|test|env]🎯 tetris-core      # 순수 Java 도메인 로직

    ↓

# 예시⚙️ tetris-backend   # Spring Boot 서비스 레이어  

./tetris.sh build     # 프로젝트 빌드    ↓

./tetris.sh client    # 클라이언트 실행  🖥️ tetris-client    # JavaFX + Spring Boot 통합

./tetris.sh backend   # 백엔드 서버 실행```

./tetris.sh test      # 테스트 실행

./tetris.sh env       # 개발 환경 체크### 🛠️ 기술 스택

```

- **Java 21 LTS** - Virtual Threads 지원

---- **Spring Boot 3.3.3** - 서비스 레이어

- **JavaFX 21** - 모던 Desktop GUI

## 📚 문서- **Gradle 8.5** - 빌드 도구

- **H2 Database** - 인메모리 데이터베이스 (선택적)

### 📖 상세 가이드

- **[DEVELOPMENT.md](./DEVELOPMENT.md)** - 개발자 상세 가이드## 🚀 실행 방법

- **[ARCHITECTURE.md](./ARCHITECTURE.md)** - 프로젝트 아키텍처

### ⭐ 1. 통합 실행 (추천)

### 🔧 설정 파일

- **[gradle/libs.versions.toml](./gradle/libs.versions.toml)** - Version Catalog 의존성 관리**Spring Boot + JavaFX 통합 실행:**

```bash

---cd tetris-client

../gradlew run

## 🛠️ 개발 환경```

- ✅ JavaFX GUI 애플리케이션 시작

### 필수 요구사항- ✅ Spring Boot DI 컨테이너 자동 초기화  

- **Java 21 LTS** (Amazon Corretto 권장)- ✅ 모든 모듈이 통합된 환경에서 실행

- **Gradle 8.12+** (Wrapper 제공)

### 🌐 2. 백엔드 독립 실행

### 권장 IDE

- **VS Code** + Java Extension Pack + Lombok**Spring Boot 웹 서버 실행:**

- **IntelliJ IDEA** + Spring Boot + JavaFX 플러그인```bash

cd tetris-backend  

### 환경 확인../gradlew bootRun

```bash```

./tetris.sh env- ✅ REST API 서버 시작 (http://localhost:8080)

```- ✅ H2 데이터베이스 콘솔 활성화

- ✅ 백엔드 개발/테스트 환경

---

**API 테스트:**

## 🌿 브랜치 네이밍 규칙```bash

curl http://localhost:8080/api/status

``````

feature/{이슈번호}/{기능명}     # 새 기능 개발

bugfix/{이슈번호}/{버그명}      # 버그 수정  ### 🖥️ 3. 전체 빌드

refactor/{이슈번호}/{리팩터링명} # 코드 리팩터링

hotfix/{이슈번호}/{긴급수정명}  # 긴급 수정```bash

```# 루트에서 모든 모듈 빌드

./gradlew build

**예시**:

- `feature/24/modulization-each-domain`# 실행 가능한 JAR 생성

- `bugfix/31/gradle-build-error`./gradlew bootJar

- `refactor/42/version-catalog-migration````



---### 🛠️ 개발 모드 실행



## 👥 팀 정보```bash

# 통합 개발 (핫 리로드)

- **프로젝트**: SeoulTech SE Team 9 - Tetris Applicationcd tetris-client

- **기술 스택**: Java 21 LTS + Spring Boot 3.5.6 + JavaFX 21../gradlew run --continuous

- **빌드 시스템**: Gradle 8.12 + Version Catalog

- **아키텍처**: 멀티모듈 하이브리드 (Spring Boot + JavaFX)# 백엔드 개발 (자동 재시작)  

cd tetris-backend

---../gradlew bootRun --continuous

```

## 📞 문의 및 기여

## 📝 개발 가이드

프로젝트와 관련된 질문이나 기여 사항이 있으시면 팀 저장소의 Issues를 활용해 주세요.

> 🚀 **상세한 개발 가이드는 [DEVELOPMENT.md](readme_files/DEVELOPMENT.md)를 참고하세요!**

**Happy Coding!** 🎉
### 👥 팀 개발 워크플로우

**백엔드 개발자:**
```bash
cd tetris-backend
../gradlew bootRun  # 독립 실행으로 API 개발
```

**프론트엔드 개발자:**  
```bash
cd tetris-client
../gradlew run     # 통합 실행으로 UI 개발
```

**통합 테스트:**
```bash
cd tetris-client
../gradlew run     # 전체 시스템 통합 테스트
```

### 🏗️ 아키텍처 특징

📄 **[상세 아키텍처 가이드](readme_files/ARCHITECTURE.md)**

- **하이브리드 아키텍처**: JavaFX가 메인, Spring Boot가 DI 컨테이너
- **계층형 모듈**: Core → Backend → Client 단방향 의존성  
- **프레임워크 격리**: Core 로직은 순수 Java로 구현
- **의존성 주입**: JavaFX 컨트롤러에서 `@Autowired` 사용

```
🎯 tetris-core      # 순수 Java 도메인 로직
    ↓
⚙️ tetris-backend   # Spring Boot 서비스 레이어  
    ↓
🖥️ tetris-client    # JavaFX + Spring Boot 통합
```

### 🛠️ 기술 스택

- **Java 21 LTS** - Virtual Threads 지원
- **Spring Boot 3.3.3** - 서비스 레이어 및 DI
- **JavaFX 21** - 모던 Desktop GUI
- **Gradle 8.5** - 멀티모듈 빌드 시스템
- **H2 Database** - 개발용 인메모리 데이터베이스


**Java 21 LTS + Spring Boot + Version Catalog 기반 멀티모듈 테트리스 게임**


[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-8.12-blue.svg)](https://gradle.org/)
[![Lombok](https://img.shields.io/badge/Lombok-1.18.30-red.svg)](https://projectlombok.org/)

## 📋 목차

- [프로젝트 개요](#-프로젝트-개요)
- [빠른 시작](#-빠른-시작)
- [아키텍처](#️-아키텍처)
- [개발 환경 설정](#-개발-환경-설정)
- [빌드 및 실행](#-빌드-및-실행)
- [개발 가이드](#-개발-가이드)
- [팀 협업](#-팀-협업)

---

## 🎯 프로젝트 개요

서울과기대 소프트웨어공학 팀 프로젝트로 개발하는 **현대적인 테트리스 게임**입니다.

### ✨ 주요 특징
- **Java 21 LTS**: 최신 장기지원 버전으로 성능과 안정성 확보
- **멀티모듈 아키텍처**: 깔끔한 계층 분리와 재사용성
- **Version Catalog**: Type-Safe 의존성 관리
- **Spring Boot**: 엔터프라이즈급 DI 컨테이너와 서비스 레이어
- **JavaFX**: 모던한 데스크톱 UI
- **Lombok**: 보일러플레이트 코드 자동 생성

### 🏗️ 모듈 구조
```
tetris-app/
├── tetris-core/     🎯 게임 로직 (순수 Java)
├── tetris-backend/  ⚙️ 서비스 레이어 (Spring Boot)  
└── tetris-client/   🖥️ 데스크톱 앱 (JavaFX + Spring)
```

---

## 🚀 빠른 시작

### 1. 필수 요구사항
- **Java 21 LTS** (Corretto, OpenJDK 등)
- **Git**

### 2. 프로젝트 클론 및 실행
```bash
# 레포지토리 클론
git clone https://github.com/louis5103/se-teris-team-9.git
cd se-teris-team-9

# 프로젝트 빌드
./gradlew build

# 백엔드 서버 실행 (개발/테스트용)
./gradlew :tetris-backend:bootRun

# 데스크톱 클라이언트 실행
./gradlew :tetris-client:run
```

### 3. 개발 환경 (VS Code)
```bash
# VS Code에서 프로젝트 열기
code .

# 권장 확장 프로그램 자동 설치됨:
# - Java Extension Pack
# - Lombok Annotations Support  
# - Spring Boot Tools
```

---

## 🏗️ 아키텍처

### 📦 모듈별 역할

#### 🎯 tetris-core
- **목적**: 게임 핵심 로직
- **기술**: 순수 Java (외부 의존성 최소)
- **포함**: 블록, 보드, 점수 시스템, 게임 규칙

#### ⚙️ tetris-backend  
- **목적**: 서비스 레이어 및 데이터 관리
- **기술**: Spring Boot, JPA, H2 Database
- **포함**: 게임 상태 관리, 점수 저장, REST API

#### 🖥️ tetris-client
- **목적**: 사용자 인터페이스
- **기술**: JavaFX, Spring Boot (DI)
- **포함**: 게임 화면, 사용자 입력 처리

### 🔄 의존성 관계
```
tetris-client → tetris-backend → tetris-core
```

---

## 🛠 개발 환경 설정

### Java 21 설치 (macOS)
```bash
# Homebrew 사용
brew install openjdk@21

# 또는 Amazon Corretto
brew install --cask corretto21
```

### IDE 설정

#### VS Code
프로젝트를 열면 자동으로 권장 설정이 적용됩니다:
- Java 21 자동 인식
- Lombok 어노테이션 처리
- Gradle 프로젝트 구성

#### IntelliJ IDEA
1. **File → Open** → 프로젝트 루트 선택
2. **Gradle** 프로젝트로 import
3. **Project SDK**: Java 21 설정
4. **Lombok Plugin** 활성화

---

## 🔨 빌드 및 실행

### 전체 프로젝트 빌드
```bash
./gradlew build
```

### 모듈별 작업
```bash
# Core 모듈 테스트
./gradlew :tetris-core:test

# Backend 서버 실행
./gradlew :tetris-backend:bootRun

# Client 실행
./gradlew :tetris-client:run
```

### JAR 파일 생성
```bash
# 모든 모듈의 JAR 생성
./gradlew assemble

# 실행 가능한 Spring Boot JAR (Backend)
./gradlew :tetris-backend:bootJar
```

---

## 💻 개발 가이드

### 코드 스타일
- **들여쓰기**: 4 spaces
- **인코딩**: UTF-8
- **Line ending**: LF
- **Lombok**: `@Data`, `@Builder`, `@Slf4j` 적극 활용

### 테스트 작성
```java
// JUnit 5 + AssertJ 스타일
@Test
void should_CreateBlock_When_ValidInput() {
    // Given
    BlockType type = BlockType.I;
    
    // When  
    Block block = Block.builder()
        .type(type)
        .build();
    
    // Then
    assertThat(block.getType()).isEqualTo(type);
}
```

### Version Catalog 의존성 관리

이 프로젝트는 **Gradle Version Catalog**를 사용하여 중앙화된 의존성 관리를 수행합니다.

#### 의존성 추가 방법:
1. **gradle/libs.versions.toml**에 라이브러리 정의
```toml
[versions]
jackson = "2.17.0"

[libraries]
common-jackson = { module = "com.fasterxml.jackson.core:jackson-core", version.ref = "jackson" }

[bundles]  
common-json = ["common-jackson", "common-jackson-databind"]
```

2. **build.gradle.kts**에서 사용
```kotlin
dependencies {
    implementation(libs.common.jackson)        # 단일 라이브러리
    testImplementation(libs.bundles.common.testing)  # Bundle 사용
}
```

> 📚 **상세한 의존성 관리 가이드**: [DEVELOPMENT.md](./DEVELOPMENT.md#version-catalog-의존성-관리)

---

## 🤝 팀 협업

### 브랜치 전략
```
main                    # 최종 릴리즈
├── develop            # 개발 통합 브랜치
├── feature/이슈번호/기능명  # 기능 개발
└── bugfix/이슈번호/설명    # 버그 수정
```

### 커밋 컨벤션
```
feat: 새로운 기능 추가
fix: 버그 수정  
docs: 문서 수정
style: 코드 포맷팅
refactor: 코드 리팩토링
test: 테스트 코드
chore: 빌드/설정 변경
```

### Pull Request 프로세스
1. 이슈 생성 및 브랜치 생성
2. 기능 개발 및 테스트
3. PR 생성 (리뷰어 지정)
4. 코드 리뷰 및 승인
5. `develop` 브랜치 머지

---

## 🔍 문제 해결

### 빌드 실패 시
```bash
# Gradle 데몬 재시작
./gradlew --stop
./gradlew clean build
```

### IDE에서 Lombok 인식 안 될 때
1. VS Code: Java Language Server 재시작
2. IntelliJ: Lombok 플러그인 확인

### 포트 충돌 (8080)
서버 포트가 이미 사용 중인 경우:

**환경변수로 변경 (권장):**
```bash
# macOS/Linux
export SERVER_PORT=8081

# Windows (PowerShell)  
$env:SERVER_PORT=8081

# Windows (CMD)
set SERVER_PORT=8081
```

**또는 설정 파일에서 직접 변경:**
```properties
# tetris-backend/src/main/resources/application.properties
server.port=8081
```

---

## 📚 추가 자료

- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [JavaFX Documentation](https://openjfx.io/openjfx-docs/)
- [Lombok Features](https://projectlombok.org/features/all)
- [Gradle User Manual](https://docs.gradle.org/current/userguide/userguide.html)

---

## 👥 팀 정보

**서울과학기술대학교 소프트웨어공학 팀 9**

- 프로젝트 기간: 2024년 2학기
- 기술 스택: Java 21, Spring Boot, JavaFX, Gradle
- 목표: 현대적인 아키텍처로 구현한 테트리스 게임

---

## 📄 라이선스

이 프로젝트는 교육 목적으로 개발되었습니다.

## 📞 문의 및 기여

- **이슈 리포팅**: GitHub Issues 활용
- **개발 문의**: 팀 개발자 또는 프로젝트 관리자 연락
- **기여 가이드**: Pull Request 템플릿 준수

---

**🎯 Quick Start:**  
```bash
git clone [repository-url]
cd tetris-app
cd tetris-client && ../gradlew run
```

**💡 개발 팁:** 통합 실행(`tetris-client:run`)으로 전체 시스템을 확인하면서 개발하세요!
Feat/123/Add-User            # 대문자 사용
feat/123/add_user            # 언더스코어 사용
feat/123/add.user            # 점 사용
