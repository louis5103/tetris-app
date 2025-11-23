package server; // 패키지명이 짧아짐

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TetrisServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(TetrisServerApplication.class, args);
    }
}
