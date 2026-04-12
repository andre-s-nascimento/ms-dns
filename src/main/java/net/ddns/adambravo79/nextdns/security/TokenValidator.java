package net.ddns.adambravo79.nextdns.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TokenValidator {

    @Value("${APP_SECRET_TOKEN:}")
    private String validToken;

    public boolean isValid(String token) {
        if (validToken == null || validToken.isEmpty()) {
            log.error("APP_SECRET_TOKEN não configurado! O sistema não pode validar requisições.");
            return false;
        }
        
        boolean isValid = token != null && validToken.equals(token);
        
        if (!isValid) {
            log.warn("Tentativa de acesso com token inválido (recebido: '{}', esperado: '{}')", 
                token != null ? "***" : "null", "***");
        }
        
        return isValid;
    }
}