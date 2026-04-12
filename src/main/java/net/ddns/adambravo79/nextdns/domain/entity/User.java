package net.ddns.adambravo79.nextdns.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // Adicionado para facilitar a criação de novos usuários
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String name;

    // Campos para OAuth2 (Google/Yahoo)
    private String provider;   // ex: "google", "yahoo"
    private String providerId; // ID único retornado pelo provedor

    // Token para acesso via API (Substitui o antigo TokenValidator)
    @Column(unique = true)
    private String apiToken;

    private String role; // ex: "ROLE_USER"

    // Relacionamento com as configurações do NextDNS
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<NextDnsConfig> configs;
}