package net.ddns.adambravo79.nextdns.domain.repository;

import net.ddns.adambravo79.nextdns.domain.entity.NextDnsConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NextDnsConfigRepository extends JpaRepository<NextDnsConfig, Long> {
    // Aqui usamos o "Double Underscore" ou navegação de propriedade do Spring Data
    // Ele busca na entidade User (dentro de NextDnsConfig) pelo campo Email
    Optional<NextDnsConfig> findByUserEmail(String email);
}