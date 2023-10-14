package com.github.olegbal.javastellarppbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JavaStellarPpBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(JavaStellarPpBotApplication.class, args);
    }

}
