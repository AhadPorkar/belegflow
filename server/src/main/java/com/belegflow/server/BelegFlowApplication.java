package com.belegflow.server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import java.time.Clock;
@SpringBootApplication @EnableScheduling public class BelegFlowApplication {
    public static void main(String[] args) {
        SpringApplication.run(BelegFlowApplication.class,args);
    }
    @Bean Clock clock() {
        return Clock.systemUTC();
    }
}
