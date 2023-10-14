package com.github.olegbal.javastellarppbot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stellar.sdk.Server;

import java.util.List;

@Configuration
public class StellarConfiguration {

    @Value("${stellar.horizons}")
    private List<String> horizonUrls;

    @Bean
    public List<Server> horizonServers() {
        return horizonUrls.stream()
                .map(Server::new)
                .toList();
    }
}
