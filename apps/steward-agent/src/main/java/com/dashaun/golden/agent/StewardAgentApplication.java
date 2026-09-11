package com.dashaun.golden.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StewardProperties.class)
public class StewardAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(StewardAgentApplication.class, args);
    }
}
