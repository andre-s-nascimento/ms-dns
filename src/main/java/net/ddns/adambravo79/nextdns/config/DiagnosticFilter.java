package net.ddns.adambravo79.nextdns.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class DiagnosticFilter {
    
    @Bean
    public WebFilter logFilter() {
        return (exchange, chain) -> {
            log.info("=== REQUEST ===");
            log.info("Path: {}", exchange.getRequest().getPath());
            log.info("URI: {}", exchange.getRequest().getURI());
            log.info("Headers: {}", exchange.getRequest().getHeaders());
            return chain.filter(exchange);
        };
    }
}