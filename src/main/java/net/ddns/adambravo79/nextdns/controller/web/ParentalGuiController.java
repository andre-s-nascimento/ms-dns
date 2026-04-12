package net.ddns.adambravo79.nextdns.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.nextdns.domain.entity.NextDnsConfig;
import net.ddns.adambravo79.nextdns.domain.entity.User;
import net.ddns.adambravo79.nextdns.domain.repository.NextDnsConfigRepository;
import net.ddns.adambravo79.nextdns.domain.repository.UserRepository;
import net.ddns.adambravo79.nextdns.service.NextDnsService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ParentalGuiController {

    private final NextDnsService service;
    private final NextDnsConfigRepository configRepository;
    private final UserRepository userRepository;

    @GetMapping(value = { "/", "/gui/**" })
    public Mono<String> showGui(@AuthenticationPrincipal OAuth2User principal, Model model) {
        if (principal == null) return Mono.just("redirect:/controle-parental/login");
        
        String email = principal.getAttribute("email");
        if (email == null) return Mono.just("redirect:/controle-parental/login");

        return Mono.fromCallable(() -> configRepository.findByUserEmail(email))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalConfig -> {
                    if (optionalConfig.isEmpty()) {
                        model.addAttribute("userEmail", email);
                        return Mono.just("setup-config");
                    }
                    NextDnsConfig config = optionalConfig.get();
                    model.addAttribute("profileId", config.getProfileId());
                    model.addAttribute("userEmail", email);
                    return Mono.just("index");
                });
    }

    @PostMapping("/gui/bloquear")
    @ResponseBody
    public Mono<Void> bloquear(@AuthenticationPrincipal Object principal, ServerWebExchange exchange) {
        return service.bloquearComLog(exchange, extractEmail(principal));
    }

    @PostMapping("/gui/liberar")
    @ResponseBody
    public Mono<Void> liberar(@AuthenticationPrincipal Object principal, ServerWebExchange exchange) {
        return service.liberarComLog(exchange, extractEmail(principal));
    }

    @PostMapping(value = "/gui/timer", consumes = "application/x-www-form-urlencoded")
    @ResponseBody
    public Mono<Void> timer(@AuthenticationPrincipal Object principal, ServerWebExchange exchange) {
        String email = extractEmail(principal);
        return exchange.getFormData().flatMap(formData -> {
            String minutosStr = formData.getFirst("minutos");
            int minutos = (minutosStr != null) ? Integer.parseInt(minutosStr) : 0;
            return service.liberarTemporario(minutos, exchange, email);
        });
    }

    @PostMapping("/gui/setup")
    public Mono<String> salvarSetup(@AuthenticationPrincipal Object principal, ServerWebExchange exchange) {
        String email = extractEmail(principal);
        return exchange.getFormData().flatMap(formData -> {
            String profileId = formData.getFirst("profileId");
            String apiKey = formData.getFirst("apiKey");

            return Mono.fromCallable(() -> {
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Usuário não cadastrado: " + email));

                NextDnsConfig config = NextDnsConfig.builder()
                        .profileId(profileId)
                        .encryptedApiKey(apiKey)
                        .user(user)
                        .build();

                configRepository.save(config);
                return "redirect:/gui";
            }).subscribeOn(Schedulers.boundedElastic());
        });
    }

    private String extractEmail(Object principal) {
        if (principal instanceof OAuth2User oAuth2User) {
            return oAuth2User.getAttribute("email");
        }
        return null;
    }
}