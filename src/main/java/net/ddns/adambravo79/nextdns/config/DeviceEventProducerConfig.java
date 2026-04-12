package net.ddns.adambravo79.nextdns.config;

import net.ddns.adambravo79.nextdns.dto.events.DeviceStatusEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerializer;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class DeviceEventProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public KafkaSender<String, DeviceStatusEvent> deviceStatusKafkaSender(com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        // Aproveitamos o ObjectMapper que o Spring já tem configurado (com JavaTimeModule)
        // Isso evita criar um novo mapper na mão
        
        JsonSerializer<DeviceStatusEvent> valueSerializer = new JsonSerializer<>(objectMapper);
        valueSerializer.setAddTypeInfo(false);

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        SenderOptions<String, DeviceStatusEvent> senderOptions = SenderOptions.<String, DeviceStatusEvent>create(props)
                .withValueSerializer(valueSerializer);

        return KafkaSender.create(senderOptions);
    }
}