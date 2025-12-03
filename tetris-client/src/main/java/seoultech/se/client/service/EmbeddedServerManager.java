package seoultech.se.client.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * 내장 Tetris 서버를 관리하는 서비스
 * 클라이언트 애플리케이션 시작 시 tetris-server를 자동으로 실행
 */
@Slf4j
@Service
public class EmbeddedServerManager {
    
    private Process serverProcess;
    private static final String SERVER_JAR_NAME = "tetris-server.jar";  // 패키징 시 리네임됨
    private static final int SERVER_PORT = 8090;
    private static final int SERVER_STARTUP_TIMEOUT_SECONDS = 30;
    
    @PostConstruct
    public void startServer() {
        try {
            log.info("🚀 Starting embedded Tetris server...");
            
            // 서버 JAR 파일 경로 찾기
            File serverJar = findServerJar();
            if (serverJar == null || !serverJar.exists()) {
                log.warn("⚠️ Server JAR not found. Running in client-only mode.");
                return;
            }
            
            // 포트가 이미 사용 중인지 확인
            if (isPortInUse(SERVER_PORT)) {
                log.info("✅ Server is already running on port {}", SERVER_PORT);
                return;
            }
            
            // 서버 프로세스 시작 (embedded-server 프로파일 명시적 지정)
            ProcessBuilder processBuilder = new ProcessBuilder(
                "java",
                "-Xmx1024m",
                "-Dserver.port=" + SERVER_PORT,
                "-Dspring.profiles.active=embedded-server",
                "-jar",
                serverJar.getAbsolutePath()
            );
            
            // 서버 로그를 클라이언트 로그에 통합
            processBuilder.redirectErrorStream(true);
            
            serverProcess = processBuilder.start();
            
            // 서버 출력 로깅 (백그라운드 스레드) - 중요한 로그만 표시
            Thread logThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(serverProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        // 중요한 로그만 출력 (시작/종료, 에러, 경고)
                        if (line.contains("Started TetrisServerApplication") ||
                            line.contains("ERROR") ||
                            line.contains("WARN") ||
                            line.contains("Table") ||
                            line.contains("create table") ||
                            line.contains("Tomcat started")) {
                            log.info("[Server] {}", line);
                        } else {
                            // 나머지는 DEBUG 레벨로
                            log.debug("[Server] {}", line);
                        }
                    }
                } catch (IOException e) {
                    log.error("Error reading server output", e);
                }
            });
            logThread.setDaemon(true);
            logThread.setName("ServerLogReader");
            logThread.start();
            
            // 서버 시작 대기
            if (waitForServerStartup()) {
                log.info("✅ Embedded Tetris server started successfully on port {}", SERVER_PORT);
            } else {
                log.warn("⚠️ Server startup timeout. Check server logs for details.");
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to start embedded server", e);
        }
    }
    
    @PreDestroy
    public void stopServer() {
        if (serverProcess != null && serverProcess.isAlive()) {
            log.info("🛑 Stopping embedded Tetris server...");
            serverProcess.destroy();
            
            try {
                // 정상 종료 대기 (최대 10초)
                if (!serverProcess.waitFor(10, TimeUnit.SECONDS)) {
                    log.warn("⚠️ Server didn't stop gracefully, forcing termination...");
                    serverProcess.destroyForcibly();
                }
                log.info("✅ Embedded server stopped");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while stopping server", e);
            }
        }
    }
    
    /**
     * 서버 JAR 파일 찾기
     * - 개발 환경: ../tetris-server/build/libs/
     * - 배포 환경: app 디렉토리 내
     */
    private File findServerJar() {
        try {
            // 방법 1: java.class.path에서 현재 JAR 경로 추출
            String classPath = System.getProperty("java.class.path");
            log.info("ClassPath: {}", classPath);
            
            File appDir = null;
            
            if (classPath != null) {
                // ClassPath가 여러 JAR를 포함할 수 있음 (콜론 또는 세미콜론으로 구분)
                String[] paths = classPath.split("[;:]");
                for (String path : paths) {
                    if (path.endsWith(".jar")) {
                        File jarFile = new File(path);
                        if (jarFile.getName().startsWith("tetris-desktop-app")) {
                            appDir = jarFile.getParentFile();
                            log.info("Detected JAR execution, app directory: {}", appDir.getAbsolutePath());
                            break;
                        }
                    }
                }
            }
            
            // 방법 2: ProtectionDomain 사용 (fallback)
            if (appDir == null) {
                try {
                    String jarPath = EmbeddedServerManager.class
                            .getProtectionDomain()
                            .getCodeSource()
                            .getLocation()
                            .toURI()
                            .getPath();
                    
                    if (jarPath != null && !jarPath.isEmpty()) {
                        File currentJar = new File(jarPath);
                        appDir = currentJar.getParentFile();
                        log.info("Using ProtectionDomain, app directory: {}", appDir.getAbsolutePath());
                    }
                } catch (Exception e) {
                    log.debug("ProtectionDomain method failed", e);
                }
            }
            
            if (appDir == null) {
                log.warn("Could not determine app directory");
                return null;
            }
            
            // 배포 환경: 같은 디렉토리에서 서버 JAR 찾기
            File serverJar = new File(appDir, SERVER_JAR_NAME);
            if (serverJar.exists()) {
                log.info("✅ Found server JAR: {}", serverJar.getAbsolutePath());
                return serverJar;
            }
            
            // 개발 환경: ../tetris-server/build/libs/ 경로 확인
            String userDir = System.getProperty("user.dir");
            if (userDir != null) {
                // 먼저 리네임된 파일명으로 시도
                Path devServerPath = Paths.get(userDir)
                        .getParent()
                        .resolve("tetris-server")
                        .resolve("build")
                        .resolve("libs")
                        .resolve(SERVER_JAR_NAME);
                
                if (devServerPath.toFile().exists()) {
                    log.info("✅ Found server JAR (dev): {}", devServerPath);
                    return devServerPath.toFile();
                }
                
                // fallback: 원래 파일명으로 시도
                devServerPath = Paths.get(userDir)
                        .getParent()
                        .resolve("tetris-server")
                        .resolve("build")
                        .resolve("libs")
                        .resolve("tetris-server-standalone-1.0.0-SNAPSHOT-boot.jar");
                
                if (devServerPath.toFile().exists()) {
                    log.info("✅ Found server JAR (dev): {}", devServerPath);
                    return devServerPath.toFile();
                }
            }
            
            log.warn("❌ Server JAR not found. Checked directory: {}", appDir.getAbsolutePath());
            return null;
            
        } catch (Exception e) {
            log.error("Error finding server JAR", e);
            return null;
        }
    }
    
    /**
     * 포트가 사용 중인지 확인
     */
    private boolean isPortInUse(int port) {
        try (java.net.Socket socket = new java.net.Socket()) {
            socket.connect(new java.net.InetSocketAddress("localhost", port), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 서버가 시작될 때까지 대기
     */
    private boolean waitForServerStartup() {
        int attempts = 0;
        int maxAttempts = SERVER_STARTUP_TIMEOUT_SECONDS * 2; // 0.5초 간격
        
        while (attempts < maxAttempts) {
            if (isPortInUse(SERVER_PORT)) {
                return true;
            }
            
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            attempts++;
        }
        
        return false;
    }
}
