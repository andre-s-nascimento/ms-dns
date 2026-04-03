package net.ddns.adambravo79.nextdns.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@Slf4j
public class SecurityConfig {

    @Value("${auth.secret-token:G25@}")
    private String secretToken;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/favicon.ico", "/error").permitAll() // Libera caminhos críticos
                        .anyExchange().permitAll()
                )
                // O FILTRO DA SENHA
                // Dentro do seu SecurityWebFilterChain
                .addFilterAt((exchange, chain) -> {
                    String path = exchange.getRequest().getURI().getPath();

                    // Libera caminhos públicos
                    if (path.equals("/favicon.ico") || path.equals("/error")) {
                        return chain.filter(exchange);
                    }

                    // Busca o token APENAS no Header ou na URL
                    String tokenHeader = exchange.getRequest().getHeaders().getFirst("X-App-Token");
                    String tokenParam = exchange.getRequest().getQueryParams().getFirst("token");

                    String currentToken = (tokenHeader != null) ? tokenHeader : tokenParam;

                    if (secretToken.equals(currentToken)) {
                        return chain.filter(exchange);
                    }

                    // Se falhar a validação
                    String ip = exchange.getRequest().getRemoteAddress() != null ?
                            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";

                    log.warn("BLOQUEADO: Token inválido ou ausente para {} no IP: [{}].", path, ip);
                    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                    return exchange.getResponse().setComplete();
                }, SecurityWebFiltersOrder.FIRST)
                .build();
    }
}