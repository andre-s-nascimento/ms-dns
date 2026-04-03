package net.ddns.adambravo79.nextdns.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.nextdns.client.NextDnsClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/parental")
@RequiredArgsConstructor
@Slf4j
public class ParentalController {

    private final NextDnsClient nextDnsClient;

    @PostMapping("/youtube/bloquear")
    public Mono<String> bloquear() {
        log.info("Solicitação de bloqueio do YouTube recebida.");
        return nextDnsClient.bloquearYoutube()
                .then(Mono.just("YouTube BLOQUEADO com sucesso!"));
    }

    @DeleteMapping("/youtube/liberar")
    public Mono<String> liberar() {
        log.info("Solicitação de liberação do YouTube recebida.");
        return nextDnsClient.liberarYoutube()
                .then(Mono.just("YouTube LIBERADO com sucesso!"));
    }
}