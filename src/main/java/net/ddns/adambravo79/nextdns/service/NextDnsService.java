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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class NextDnsService {

    private final NextDnsClient client;
    private final TaskScheduler taskScheduler;
    
    // Armazena timers ativos por sessão (token + ip)
    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();

    private String getClientIp(ServerWebExchange exchange) {
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        if (remoteAddress != null && remoteAddress.getAddress() != null) {
            return remoteAddress.getAddress().getHostAddress();
        }

        return "IP-DESCONHECIDO";
    }
    
    private String getSessionKey(String token, String ip) {
        return token + "|" + ip;
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

    public Mono<Void> liberarTemporario(int minutos, ServerWebExchange exchange, String token) {
        String ip = getClientIp(exchange);
        String sessionKey = getSessionKey(token, ip);
        
        log.info("[{}] - ACESSO TEMPORÁRIO iniciado por {} min às {}", ip, minutos, LocalDateTime.now());

        // Cancela timer existente para esta sessão
        if (activeTimers.containsKey(sessionKey)) {
            ScheduledFuture<?> existingTimer = activeTimers.remove(sessionKey);
            if (existingTimer != null) {
                existingTimer.cancel(false);
                log.info("[{}] - Timer anterior cancelado", ip);
            }
        }

        // Libera agora
        return client.liberarYoutube()
            .doOnSuccess(v -> log.info("[{}] - YouTube liberado com sucesso", ip))
            .doOnError(e -> log.error("[{}] - Erro ao liberar YouTube: {}", ip, e.getMessage()))
            .then(Mono.fromRunnable(() -> {
                // Agenda o bloqueio automático
                ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                    log.info("[{}] - Timer de {} min finalizado. Bloqueando YouTube automaticamente...", ip, minutos);
                    client.bloquearYoutube()
                        .doOnSuccess(v -> {
                            log.info("[{}] - YouTube bloqueado automaticamente após timer", ip);
                            activeTimers.remove(sessionKey);
                        })
                        .doOnError(e -> log.error("[{}] - Erro ao bloquear automaticamente: {}", ip, e.getMessage()))
                        .subscribe();
                }, Instant.now().plus(Duration.ofMinutes(minutos)));
                
                activeTimers.put(sessionKey, future);
                log.info("[{}] - Timer de {} minutos agendado com sucesso", ip, minutos);
            }));
    }
}