package net.ddns.adambravo79.nextdns.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.nextdns.client.NextDnsClient;
import net.ddns.adambravo79.nextdns.security.TokenValidator;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/parental")
@RequiredArgsConstructor
@Slf4j
public class ParentalController {

    private final NextDnsClient nextDnsClient;
    private final TokenValidator tokenValidator;

    @PostMapping("/youtube/bloquear")
    public Mono<String> bloquear(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!tokenValidator.isValid(token)) {
            log.warn("API: Tentativa de bloqueio sem token válido");
            return Mono.just("ERRO: Token inválido ou não informado");
        }
        
        log.info("API: Solicitação de bloqueio do YouTube recebida.");
        return nextDnsClient.bloquearYoutube()
                .then(Mono.just("YouTube BLOQUEADO com sucesso!"));
    }

    @DeleteMapping("/youtube/liberar")
    public Mono<String> liberar(@RequestHeader(value = "X-Auth-Token", required = false) String token) {
        if (!tokenValidator.isValid(token)) {
            log.warn("API: Tentativa de liberação sem token válido");
            return Mono.just("ERRO: Token inválido ou não informado");
        }
        
        log.info("API: Solicitação de liberação do YouTube recebida.");
        return nextDnsClient.liberarYoutube()
                .then(Mono.just("YouTube LIBERADO com sucesso!"));
    }
}