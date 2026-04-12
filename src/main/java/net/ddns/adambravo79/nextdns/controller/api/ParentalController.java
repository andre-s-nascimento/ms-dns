package net.ddns.adambravo79.nextdns.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.nextdns.service.NextDnsService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/parental")
@RequiredArgsConstructor
@Slf4j
public class ParentalController {

    private final NextDnsService service;

    @PostMapping("/youtube/bloquear")
    public Mono<String> bloquear(@RequestHeader(value = "X-Api-Email") String email) {
        log.info("API: Solicitação de bloqueio para o usuário: {}", email);
        return service.bloquearComLog(null, email)
                .thenReturn("YouTube BLOQUEADO com sucesso para " + email)
                .onErrorResume(e -> Mono.just("ERRO: " + e.getMessage()));
    }

    @DeleteMapping("/youtube/liberar")
    public Mono<String> liberar(@RequestHeader(value = "X-Api-Email") String email) {
        log.info("API: Solicitação de liberação para o usuário: {}", email);
        return service.liberarComLog(null, email)
                .thenReturn("YouTube LIBERADO com sucesso para " + email)
                .onErrorResume(e -> Mono.just("ERRO: " + e.getMessage()));
    }
}