package net.ddns.adambravo79.nextdns.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.ddns.adambravo79.nextdns.dto.events.DeviceStatusEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentalControlProducer {

    private final KafkaSender<String, DeviceStatusEvent> kafkaSender;
    
    private static final String TOPIC = "nextdns.status.events";

    public Mono<Void> notifyStatusChange(String email, String action, Integer minutes) {
        DeviceStatusEvent event = new DeviceStatusEvent(email, action, minutes, LocalDateTime.now());

        SenderRecord<String, DeviceStatusEvent, String> record = SenderRecord.create(
                new ProducerRecord<>(TOPIC, email, event), 
                email // Correlation metadata
        );

        return kafkaSender.send(Mono.just(record))
            .doOnNext(r -> log.info("Kafka: Evento enviado com sucesso para: {}", email))
            .doOnError(e -> log.error("Kafka Erro: ", e))
            .then();
    }
}