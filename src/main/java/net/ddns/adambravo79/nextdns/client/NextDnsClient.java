package net.ddns.adambravo79.nextdns.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class NextDnsClient {

    private final WebClient webClient;

    // Se não achar a chave, usa o valor padrão após o ':'
    @Value("${nextdns.api.key:9e0a61afeeb2dbe9adc8b322832ef344fd04487e}")
    private String apiKey;

    @Value("${nextdns.profile-id:452169}")
    private String profileId;

    public NextDnsClient(WebClient.Builder builder,
                         @Value("${nextdns.base-url:https://api.nextdns.io}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    // BLOQUEAR: POST /profiles/{id}/parentalControl/services {id: "youtube"}
    public Mono<Void> bloquearYoutube() {
        return webClient.post()
                .uri("/profiles/{id}/parentalControl/services", profileId)
                .header("X-Api-Key", apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("id", "youtube"))
                .retrieve()
                // Se o NextDNS disser "já tá bloqueado" (400), a gente ignora o erro
                .onStatus(status -> status.value() == 400, response -> Mono.empty())
                .toBodilessEntity()
                .then();
    }

    // LIBERAR: DELETE /profiles/{id}/parentalControl/services/youtube
    public Mono<Void> liberarYoutube() {
        return webClient.delete()
                .uri("/profiles/{id}/parentalControl/services/youtube", profileId)
                .header("X-Api-Key", apiKey)
                .header("User-Agent", "curl/7.81.0") // O "pulo do gato" aqui
                .retrieve()
                // Se o YouTube já estiver liberado (404), não queremos erro 500 no Java
                .onStatus(status -> status.is4xxClientError(), resp -> Mono.empty())
                .toBodilessEntity()
                .then();
    }
}