package net.ddns.adambravo79.nextdns.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@Service
public class NextDnsClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String profileId;

    public NextDnsClient(WebClient.Builder builder,
                         @Value("${nextdns.api.key:#{null}}") String apiKey,
                         @Value("${nextdns.profile-id:#{null}}") String profileId,
                         @Value("${nextdns.base-url:https://api.nextdns.io}") String baseUrl) {
        
        if (apiKey == null || profileId == null) {
            throw new IllegalStateException("NEXTDNS_API_KEY e NEXTDNS_PROFILE_ID são obrigatórios!");
        }
        
        this.apiKey = apiKey;
        this.profileId = profileId;
        
        this.webClient = builder
                .baseUrl(baseUrl)
                .build();
    }

    public Mono<Void> bloquearYoutube() {
        log.info("Enviando requisição para BLOQUEAR YouTube - Profile: {}", profileId);
        
        return webClient.post()
                .uri("/profiles/{id}/parentalControl/services", profileId)
                .header("X-Api-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("id", "youtube"))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> 
                    response.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            if (response.statusCode().value() == 400) {
                                log.info("YouTube já estava bloqueado (código 400)");
                            } else {
                                log.warn("Erro {} ao bloquear: {}", response.statusCode().value(), errorBody);
                            }
                            return Mono.empty();
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                    response.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("Erro interno NextDNS ao bloquear: {}", errorBody);
                            return Mono.error(new RuntimeException("Erro no servidor NextDNS: " + errorBody));
                        }))
                .toBodilessEntity()
                .then();
    }

    public Mono<Void> liberarYoutube() {
        log.info("Enviando requisição para LIBERAR YouTube - Profile: {}", profileId);
        
        return webClient.delete()
                .uri("/profiles/{id}/parentalControl/services/youtube", profileId)
                .header("X-Api-Key", apiKey)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response ->
                    response.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            if (response.statusCode().value() == 404) {
                                log.info("YouTube já estava liberado (código 404)");
                            } else {
                                log.warn("Erro {} ao liberar: {}", response.statusCode().value(), errorBody);
                            }
                            return Mono.empty();
                        }))
                .onStatus(HttpStatusCode::is5xxServerError, response ->
                    response.bodyToMono(String.class)
                        .flatMap(errorBody -> {
                            log.error("Erro interno NextDNS ao liberar: {}", errorBody);
                            return Mono.error(new RuntimeException("Erro no servidor NextDNS: " + errorBody));
                        }))
                .toBodilessEntity()
                .then();
    }
}