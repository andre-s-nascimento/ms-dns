package net.ddns.adambravo79.nextdns.dto.events;

import java.time.LocalDateTime;

/**
 * Evento que representa uma mudança de estado no controle parental.
 */
public record DeviceStatusEvent(
    String email,
    String action, // "BLOQUEADO", "LIBERADO", "TEMPORARIO"
    Integer durationMinutes,
    LocalDateTime timestamp
) {}
