package net.ddns.adambravo79.nextdns.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.nextdns.service.NextDnsService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping({ "/nextdnsapp", "/" })
@RequiredArgsConstructor
@Slf4j
public class ParentalGuiController {

    private final NextDnsService service;

    // Mapeia a entrada principal
    @GetMapping(value = { "", "/", "/gui" })
    public Mono<String> showGui() {
        return Mono.just("index");
    }

    @PostMapping("/gui/bloquear")
    public Mono<String> bloquear(ServerWebExchange exchange) {
        String token = exchange.getRequest().getQueryParams().getFirst("token");
        return service.bloquearComLog(exchange)
                .thenReturn("redirect:/gui?status=bloqueado&token=" + (token != null ? token : ""));
    }

    @PostMapping("/gui/liberar")
    public Mono<String> liberar(ServerWebExchange exchange) {
        String token = exchange.getRequest().getQueryParams().getFirst("token");
        return service.liberarComLog(exchange)
                .thenReturn("redirect:/gui?status=liberado&token=" + (token != null ? token : ""));
    }

    @GetMapping("/gui/timer")
    public Mono<String> timer(@RequestParam("minutos") int minutos, ServerWebExchange exchange) {
        String token = exchange.getRequest().getQueryParams().getFirst("token");
        service.liberarTemporario(minutos, exchange);

        String redirectPath = "redirect:/gui?status=timer_iniciado&minutos=" + minutos;
        if (token != null)
            redirectPath += "&token=" + token;

        return Mono.just(redirectPath);
    }

}