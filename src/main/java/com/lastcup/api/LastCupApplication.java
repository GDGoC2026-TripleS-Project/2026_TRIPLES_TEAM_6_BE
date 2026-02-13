package com.lastcup.api;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableJpaAuditing
@EnableScheduling
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LastCupApplication {

    public static void main(String[] args) {
        // DataSource·EntityManagerFactory 등 빈 생성보다 먼저 JVM 타임존을 설정한다.
        // @PostConstruct 에서 설정하면 JDBC 커넥션 초기화 이후가 될 수 있어 DATE 컬럼 시프트가 발생한다.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        SpringApplication.run(LastCupApplication.class, args);
    }
}
