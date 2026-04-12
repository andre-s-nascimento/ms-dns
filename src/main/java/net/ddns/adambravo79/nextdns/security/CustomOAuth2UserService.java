package net.ddns.adambravo79.nextdns.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.nextdns.domain.entity.User;
import net.ddns.adambravo79.nextdns.domain.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultReactiveOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.ReactiveOAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomOAuth2UserService implements ReactiveOAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    private final DefaultReactiveOAuth2UserService delegate = new DefaultReactiveOAuth2UserService();

    @Override
    public Mono<OAuth2User> loadUser(OAuth2UserRequest userRequest) {
        return delegate.loadUser(userRequest)
                .flatMap(oAuth2User -> {
                    String email = oAuth2User.getAttribute("email");
                    String name = oAuth2User.getAttribute("name");
                    String provider = userRequest.getClientRegistration().getRegistrationId();
                    String providerId = oAuth2User.getName(); // ID único do Google/Yahoo

                    return Mono.fromCallable(() -> {
                        return userRepository.findByEmail(email)
                                .orElseGet(() -> {
                                    log.info("Novo usuário detectado via {}: {}. Salvando no SQLite...", provider, email);
                                    User newUser = User.builder()
                                            .email(email)
                                            .name(name)
                                            .provider(provider)
                                            .providerId(providerId)
                                            .apiToken(UUID.randomUUID().toString()) // Gera o token para a API
                                            .role("ROLE_USER")
                                            .build();
                                    return userRepository.save(newUser);
                                });
                    }).thenReturn(oAuth2User);
                });
    }
}