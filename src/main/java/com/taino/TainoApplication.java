package com.taino;

import com.taino.config.TainoProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(TainoProperties.class)
public class TainoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TainoApplication.class, args);
    }
}
