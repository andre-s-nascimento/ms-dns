package net.ddns.adambravo79.nextdns.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.nextdns.client.NextDnsClient;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NextDnsService {

    private final NextDnsClient client;
    private final TaskScheduler taskScheduler;

    /**
     * Método auxiliar para extrair o IP real de forma segura,
     * considerando o Apache2 como Proxy Reverso.
     */
    private String getClientIp(ServerWebExchange exchange) {
        // Tenta pegar o IP real enviado pelo Apache
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Pode vir uma lista de IPs, pegamos o primeiro
            return xForwardedFor.split(",")[0].trim();
        }

        // Fallback: se não houver proxy, tenta o endereço remoto direto com segurança
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "IP-DESCONHECIDO";
    }

    public Mono<Void> bloquearComLog(ServerWebExchange exchange) {
        String ip = getClientIp(exchange);
        log.info("[{}] - Requisição de BLOQUEIO às {}", ip, LocalDateTime.now());
        return client.bloquearYoutube();
    }

    public Mono<Void> liberarComLog(ServerWebExchange exchange) {
        String ip = getClientIp(exchange);
        log.info("[{}] - Requisição de LIBERAÇÃO às {}", ip, LocalDateTime.now());
        return client.liberarYoutube();
    }

    public void liberarTemporario(int minutos, ServerWebExchange exchange) {
        String ip = getClientIp(exchange);
        log.info("[{}] - ACESSO TEMPORÁRIO iniciado por {} min às {}", ip, minutos, LocalDateTime.now());

        // 1. Libera agora
        client.liberarYoutube().subscribe();

        // 2. Agenda o bloqueio automático
        taskScheduler.schedule(() -> {
            log.info("[{}] - Timer finalizado. Bloqueando YouTube automaticamente...", ip);
            client.bloquearYoutube().subscribe();
        }, Instant.now().plus(Duration.ofMinutes(minutos)));
    }
}