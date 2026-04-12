package net.ddns.adambravo79.nextdns.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.nextdns.client.NextDnsClient;
import net.ddns.adambravo79.nextdns.domain.entity.NextDnsConfig;
import net.ddns.adambravo79.nextdns.domain.repository.NextDnsConfigRepository;
import net.ddns.adambravo79.nextdns.producer.ParentalControlProducer;
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
    private final NextDnsConfigRepository configRepository;
    private final TaskScheduler taskScheduler;
    private final ParentalControlProducer kafkaProducer;
    
    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();

    private Mono<NextDnsConfig> getConfig(String email) {
        return Mono.fromCallable(() -> configRepository.findByUserEmail(email))
                .flatMap(opt -> opt.map(Mono::just)
                        .orElseGet(() -> Mono.error(new RuntimeException("Configuração NextDNS não encontrada para: " + email))));
    }

    private String getClientIp(ServerWebExchange exchange) {
        if (exchange == null) return "API-DIRECT";
        String xForwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
        return (remoteAddress != null && remoteAddress.getAddress() != null) 
                ? remoteAddress.getAddress().getHostAddress() : "IP-DESCONHECIDO";
    }

    public Mono<Void> bloquearComLog(ServerWebExchange exchange, String email) {
        String ip = getClientIp(exchange);
        log.info("[{}] - Usuário {} solicitou BLOQUEIO às {}", ip, email, LocalDateTime.now());
        
        return getConfig(email)
                .flatMap(config -> client.bloquearYoutube(config.getProfileId(), config.getEncryptedApiKey()))
                .then(kafkaProducer.notifyStatusChange(email, "BLOQUEADO", 0));
    }

    public Mono<Void> liberarComLog(ServerWebExchange exchange, String email) {
        String ip = getClientIp(exchange);
        log.info("[{}] - Usuário {} solicitou LIBERAÇÃO às {}", ip, email, LocalDateTime.now());
        
        return getConfig(email)
                .flatMap(config -> client.liberarYoutube(config.getProfileId(), config.getEncryptedApiKey()))
                .then(kafkaProducer.notifyStatusChange(email, "LIBERADO", 0));
    }

    public Mono<Void> liberarTemporario(int minutos, ServerWebExchange exchange, String email) {
        String ip = getClientIp(exchange);
        String sessionKey = email + "|" + ip;
        
        log.info("[{}] - ACESSO TEMPORÁRIO para {} iniciado por {} min", ip, email, minutos);

        if (activeTimers.containsKey(sessionKey)) {
            ScheduledFuture<?> existingTimer = activeTimers.remove(sessionKey);
            if (existingTimer != null) {
                existingTimer.cancel(false);
            }
        }

        return getConfig(email).flatMap(config -> 
            client.liberarYoutube(config.getProfileId(), config.getEncryptedApiKey())
                .then(kafkaProducer.notifyStatusChange(email, "TEMPORARIO_INICIO", minutos))
                .then(Mono.fromRunnable(() -> {
                    ScheduledFuture<?> future = taskScheduler.schedule(() -> {
                        log.info("[{}] - Timer finalizado para {}. Bloqueando...", ip, email);
                        
                        client.bloquearYoutube(config.getProfileId(), config.getEncryptedApiKey())
                            .then(kafkaProducer.notifyStatusChange(email, "BLOQUEIO_AUTOMATICO", 0))
                            .doOnSuccess(v -> activeTimers.remove(sessionKey))
                            .subscribe();
                    }, Instant.now().plus(Duration.ofMinutes(minutos)));
                    
                    activeTimers.put(sessionKey, future);
                }))
        );
    }
}