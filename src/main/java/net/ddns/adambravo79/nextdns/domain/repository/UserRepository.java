package net.ddns.adambravo79.nextdns.domain.repository;

import net.ddns.adambravo79.nextdns.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Busca para validar login ou vincular dados do OAuth2
    Optional<User> findByEmail(String email);
    
    // Busca específica para provedores sociais
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}