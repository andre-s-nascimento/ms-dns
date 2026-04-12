package net.ddns.adambravo79.nextdns.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "nextdns_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // Adicionado para facilitar o cadastro inicial de chaves
public class NextDnsConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String profileId;

    @Column(nullable = false)
    private String encryptedApiKey;

    // Vincula esta configuração a um usuário específico
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}