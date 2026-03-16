package com.specpulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpecPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpecPulseApplication.class, args);
    }
}
