package seoultech.se.client;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import seoultech.se.server.TetrisServerApplication; // Import the server's main class

@Component
public class ServerManager {

    private ConfigurableApplicationContext serverContext;

    public void startServer() {
        if (serverContext != null && serverContext.isRunning()) {
            System.out.println("Tetris Server is already running.");
            return;
        }

        System.out.println("🚀 Starting embedded Tetris Server...");
        
        // 시스템 속성 임시 저장 및 제거 (tetris-client의 web-application-type=none 영향 제거)
        String originalWebAppType = System.getProperty("spring.main.web-application-type");
        System.clearProperty("spring.main.web-application-type");
        
        SpringApplication serverApp = new SpringApplication(TetrisServerApplication.class);
        
        // Spring Boot 3.x: 명시적으로 웹 애플리케이션 타입 설정
        serverApp.setWebApplicationType(org.springframework.boot.WebApplicationType.SERVLET);
        System.out.println("   📌 Web application type set to: SERVLET");
        
        // 명시적으로 웹 서버 활성화 (application.properties보다 우선)
        java.util.Map<String, Object> defaultProperties = new java.util.HashMap<>();
        defaultProperties.put("spring.main.web-application-type", "servlet");
        defaultProperties.put("server.port", "8091");
        serverApp.setDefaultProperties(defaultProperties);
        System.out.println("   📌 Default properties set: web-application-type=servlet, port=8091");
        
        // Set specific profiles for the embedded server, e.g., to activate web components and a specific port
        serverApp.setAdditionalProfiles("p2p-relay", "embedded-server"); // New profiles for embedded server
        System.out.println("   📌 Active profiles: p2p-relay, embedded-server");
        
        // You might want to set a default port here if not configured via application.yml
        // serverApp.setDefaultProperties(Collections.singletonMap("server.port", "8081"));

        serverContext = serverApp.run();
        
        // 시스템 속성 복원
        if (originalWebAppType != null) {
            System.setProperty("spring.main.web-application-type", originalWebAppType);
        }
        
        String port = serverContext.getEnvironment().getProperty("server.port");
        String webAppType = serverContext.getEnvironment().getProperty("spring.main.web-application-type");
        System.out.println("✅ Embedded Tetris Server started");
        System.out.println("   📌 Configured port: " + port);
        System.out.println("   📌 Web application type: " + webAppType);
        System.out.println("   📌 Context running: " + serverContext.isRunning());
        System.out.println("   📌 Bean count: " + serverContext.getBeanDefinitionCount());
    }

    public void stopServer() {
        if (serverContext != null && serverContext.isRunning()) {
            System.out.println("🛑 Shutting down embedded Tetris Server...");
            serverContext.close();
            System.out.println("✅ Embedded Tetris Server shut down.");
        }
    }
}
