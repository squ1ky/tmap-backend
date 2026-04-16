package ru.tbank.tmap.generator.kafka.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.generator.config.GeneratorProperties;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionProducer {

    private final KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    private final GeneratorProperties generatorProps;

    public void send(TransactionEvent event) {
        kafkaTemplate.send(generatorProps.topic(), event.venueId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send transaction {}: {}",
                                event.transactionId(), ex.getMessage());
                    } else {
                        log.debug("Sent transaction {} to partition {}",
                                event.transactionId(), result.getRecordMetadata().partition());
                    }
                });
    }
}
