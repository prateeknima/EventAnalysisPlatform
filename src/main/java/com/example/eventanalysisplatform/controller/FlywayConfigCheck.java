package com.example.eventanalysisplatform.controller;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfigCheck {

    @Bean
    @ConditionalOnBean(Flyway.class)
    CommandLineRunner flywayCheck(Flyway flyway) {
        return args -> {
            System.out.println("Flyway bean found");
            System.out.println("Flyway = " + flyway);
        };
    }
}
