package com.lastcup.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class LastCupApplication {

    public static void main(String[] args) {
        SpringApplication.run(LastCupApplication.class, args);
    }
}
