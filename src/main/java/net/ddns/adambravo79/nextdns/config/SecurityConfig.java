package net.ddns.adambravo79.nextdns.config;

import lombok.extern.slf4j.Slf4j; 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import java.net.InetSocketAddress;

@Slf4j // <-- Anotação que cria a variável 'log' automaticamente
@Configuration
@EnableWebFluxSecurity
@Order(-1)
public class SecurityConfig {

    @Value("${APP_SECRET_TOKEN:default_token}")
    private String secretToken;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeExchange(exchanges -> exchanges
                .matchers(tokenMatcher()).permitAll()
                .anyExchange().denyAll()
            )
            .build();
    }

    private ServerWebExchangeMatcher tokenMatcher() {
        return exchange -> {
            // 1. Captura o IP real (Proxy Aware)
            String remoteIp = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
            if (remoteIp == null) {
                InetSocketAddress addr = exchange.getRequest().getRemoteAddress();
                remoteIp = (addr != null && addr.getAddress() != null) ? addr.getAddress().getHostAddress() : "Desconhecido";
            }

            // 2. Captura os dados da requisição
            String providedToken = exchange.getRequest().getQueryParams().getFirst("token");
            String path = exchange.getRequest().getPath().value();

            // 3. LOGS ESTRUTURADOS com Slf4j
            // {} são placeholders que evitam concatenação desnecessária de strings
            log.info("Tentativa de acesso na rota: {}", path);
            log.debug("IP do Cliente: {}", remoteIp);
            
            // Cuidado: Em produção, evite logar tokens reais. Aqui deixamos para o seu debug.
            log.info("Token esperado: [{}] | Token recebido: [{}]", 
                secretToken.trim(), 
                (providedToken != null ? providedToken.trim() : "NULL"));

            // 4. Validação
            if (providedToken != null && secretToken.trim().equals(providedToken.trim())) {
                log.info("✅ ACESSO CONCEDIDO para IP: {}", remoteIp);
                return ServerWebExchangeMatcher.MatchResult.match();
            }

            log.warn("❌ ACESSO NEGADO: Token inválido ou ausente. IP: {}", remoteIp);
            return ServerWebExchangeMatcher.MatchResult.notMatch();
        };
    }
}