package seoultech.se.client.config;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring ApplicationContext를 어디서든 접근할 수 있게 해주는 헬퍼 클래스
 * 
 * 동작 원리:
 * 1. Spring이 이 Bean을 생성할 때 자동으로 setApplicationContext()를 호출합니다
 * 2. 그때 받은 context를 static 변수에 저장합니다
 * 3. 이후 어디서든 getApplicationContext()로 접근할 수 있습니다
 * 
 * 사용 예:
 * ApplicationContext context = ApplicationContextProvider.getApplicationContext();
 * MyService service = context.getBean(MyService.class);
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {
    
    private static ApplicationContext context;

    /**
     * Spring이 자동으로 호출하는 메서드
     * Bean 생성 시점에 ApplicationContext를 주입받습니다
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        context = applicationContext;
    }

    /**
     * 어디서든 Spring ApplicationContext에 접근할 수 있게 해줍니다
     * 
     * @return Spring ApplicationContext
     * @throws IllegalStateException Spring이 아직 초기화되지 않았을 때
     */
    public static ApplicationContext getApplicationContext() {
        if (context == null) {
            throw new IllegalStateException(
                "ApplicationContext has not been initialized yet! " +
                "Make sure Spring Boot has started properly."
            );
        }
        return context;
    }
}
