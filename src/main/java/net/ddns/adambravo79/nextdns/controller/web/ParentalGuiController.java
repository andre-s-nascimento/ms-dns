package net.ddns.adambravo79.nextdns.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.nextdns.security.TokenValidator;
import net.ddns.adambravo79.nextdns.service.NextDnsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ParentalGuiController {

    private final NextDnsService service;
    private final TokenValidator tokenValidator;

    @GetMapping(value = {"/", "/gui"})
    public Mono<String> showGui(@RequestParam(required = false) String token,
                                 @RequestParam(required = false) String error,
                                 org.springframework.ui.Model model) {
        if (!tokenValidator.isValid(token)) {
            model.addAttribute("error", "unauthorized");
            return Mono.just("index");
        }
        
        model.addAttribute("hasValidToken", true);
        if (error != null) {
            model.addAttribute("error", error);
        }
        return Mono.just("index");
    }

    @PostMapping("/gui/bloquear")
    public Mono<String> bloquear(@RequestParam(required = false) String token,
                                  ServerWebExchange exchange) {
        if (!tokenValidator.isValid(token)) {
            log.warn("Tentativa de bloqueio sem token válido");
            return Mono.just("redirect:/gui?error=unauthorized");
        }
        
        return service.bloquearComLog(exchange)
                .thenReturn("redirect:/gui?status=bloqueado&token=" + (token != null ? token : ""));
    }

    @PostMapping("/gui/liberar")
    public Mono<String> liberar(@RequestParam(required = false) String token,
                                 ServerWebExchange exchange) {
        if (!tokenValidator.isValid(token)) {
            log.warn("Tentativa de liberação sem token válido");
            return Mono.just("redirect:/gui?error=unauthorized");
        }
        
        return service.liberarComLog(exchange)
                .thenReturn("redirect:/gui?status=liberado&token=" + (token != null ? token : ""));
    }

    @GetMapping("/gui/timer")
    public Mono<String> timer(@RequestParam("minutos") int minutos,
                               @RequestParam(required = false) String token,
                               ServerWebExchange exchange) {
        if (!tokenValidator.isValid(token)) {
            log.warn("Tentativa de timer sem token válido");
            return Mono.just("redirect:/gui?error=unauthorized");
        }
        
        if (minutos < 1 || minutos > 480) {
            return Mono.just("redirect:/gui?error=invalid_minutes&token=" + (token != null ? token : ""));
        }
        
        return service.liberarTemporario(minutos, exchange, token)
                .thenReturn("redirect:/gui?status=timer_iniciado&minutos=" + minutos + "&token=" + (token != null ? token : ""));
    }
}