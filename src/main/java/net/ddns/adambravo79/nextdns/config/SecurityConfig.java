package net.ddns.adambravo79.nextdns.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.CsrfSpec;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;

@Configuration
public class SecurityConfig {

        @Bean
        public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
                RedirectServerLogoutSuccessHandler logoutHandler = new RedirectServerLogoutSuccessHandler();
logoutHandler.setLogoutSuccessUrl(URI.create("/login"));

                return http
                                .csrf(CsrfSpec::disable)
                                .authorizeExchange(exchanges -> exchanges
                                                .pathMatchers("/login/**", "/oauth2/**", "/actuator/**", "/actuator/health/**",
                                                                "/default-ui.css")
                                                .permitAll()
                                                .anyExchange().authenticated())
                                .oauth2Login(oauth2 -> {
                                })
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessHandler(logoutHandler)
                                                )
                                .build();
        }
}