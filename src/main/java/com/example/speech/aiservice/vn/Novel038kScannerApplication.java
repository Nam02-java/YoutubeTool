package com.example.speech.aiservice.vn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.speech.aiservice.vn"})
public class Novel038kScannerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Novel038kScannerApplication.class);
        app.setAdditionalProfiles("Novel038k"); // Configure settings in code
        app.run(args);
    }
}
