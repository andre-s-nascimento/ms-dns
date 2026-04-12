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
public class KafkaReactiveConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Bean
    public KafkaSender<String, DeviceStatusEvent> kafkaSender() {
        // 1. Configuramos o Jackson para aceitar as datas do Java 8 (LocalDateTime)
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

        // 2. Criamos o JsonSerializer manualmente com esse mapper
        JsonSerializer<DeviceStatusEvent> jsonSerializer = new JsonSerializer<>(mapper);
        jsonSerializer.setAddTypeInfo(false); // Substitui o ADD_TYPE_INFO_HEADERS

        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // Note que aqui não passamos mais o .class, pois vamos usar a instância abaixo

        SenderOptions<String, DeviceStatusEvent> senderOptions = SenderOptions.<String, DeviceStatusEvent>create(props)
                .withValueSerializer(jsonSerializer);

        return KafkaSender.create(senderOptions);
    }
}