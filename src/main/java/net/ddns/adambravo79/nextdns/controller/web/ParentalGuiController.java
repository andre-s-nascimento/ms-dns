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
@RequestMapping("/gui")
@RequiredArgsConstructor
@Slf4j
public class ParentalGuiController {

    public static final String TOKEN = "token";
    private final NextDnsService service;

    // Ajustado para aceitar o token e retornar Mono, conforme solicitado
    @GetMapping
    public Mono<String> showGui(@RequestParam(name = TOKEN, required = false) String token) {
        // A lógica de validação do token já está no SecurityConfig,
        // então aqui apenas retornamos o nome do modelo.
        return Mono.just("index");
    }

    @PostMapping("/bloquear")
    public Mono<String> bloquear(ServerWebExchange exchange) {
        // Captura o _token_ que veio na requisição atual para manter o acesso no redirect
        String token = exchange.getRequest().getQueryParams().getFirst(TOKEN);

        return service.bloquearComLog(exchange)
                .thenReturn("redirect:/gui?status=bloqueado&token=" + (token != null ? token : ""));
    }

    @PostMapping("/liberar")
    public Mono<String> liberar(ServerWebExchange exchange) {
        String token = exchange.getRequest().getQueryParams().getFirst(TOKEN);

        return service.liberarComLog(exchange)
                .thenReturn("redirect:/gui?status=liberado&token=" + (token != null ? token : ""));
    }

    @GetMapping("/timer") // Agora é GET
    public Mono<String> timer(@RequestParam("minutos") int minutos, ServerWebExchange exchange) {
        String token = exchange.getRequest().getQueryParams().getFirst(TOKEN);

        log.info("Recebido pedido de timer: {} minutos", minutos);
        service.liberarTemporario(minutos, exchange);

        return Mono.just("redirect:/gui?status=timer_iniciado&minutos=" + minutos + "&token=" + (token != null ? token : ""));
    }
}