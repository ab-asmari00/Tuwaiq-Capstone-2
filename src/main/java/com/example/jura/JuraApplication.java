package com.example.jura;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JuraApplication {

    @Bean
    public Clock juraClock() {
        return Clock.system(ZoneId.of("Asia/Riyadh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(JuraApplication.class, args);
    }

}
