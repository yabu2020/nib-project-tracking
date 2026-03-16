package com.nib.projecttracking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class NibProjectTrackingApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(NibProjectTrackingApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        // Specify the main application class for WAR deployment
        return builder.sources(NibProjectTrackingApplication.class);
    }
}