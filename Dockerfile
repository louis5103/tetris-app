# 1. 베이스 이미지: 프로젝트와 동일한 Java 11 버전을 사용합니다.
FROM openjdk:11-jdk-slim

# 2. Java GUI에 필요한 모든 필수 시스템 라이브러리 설치
RUN apt-get update && apt-get install -y \
    # X11 그래픽 라이브러리
    libxext6 \
    libxrender1 \
    libxtst6 \
    libxi6 \
    # 폰트 렌더링 라이브러리
    libfreetype6 \
    # 폰트 설정 라이브러리 (이번 문제 해결)
    fontconfig \
    && rm -rf /var/lib/apt/lists/*

# 3. 작업 디렉터리
WORKDIR /app

# 4. 'shadowJar'로 빌드된 JAR 파일을 컨테이너에 복사합니다.
COPY tetris/build/libs/*-all.jar app.jar

# 5. 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]