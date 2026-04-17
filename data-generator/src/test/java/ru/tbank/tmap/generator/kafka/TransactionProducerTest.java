package ru.tbank.tmap.generator.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import ru.tbank.tmap.generator.TestFactory;
import ru.tbank.tmap.generator.config.GeneratorProperties;
import ru.tbank.tmap.generator.kafka.event.TransactionEvent;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionProducerTest {

    @Mock
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;

    @Mock
    private GeneratorProperties generatorProps;

    @InjectMocks
    private TransactionProducer transactionProducer;

    private static final String TOPIC_NAME = "transactions";

    @Test
    void send_whenCalled_thenPublishesToTopicWithVenueIdKey() {
        UUID venueId = UUID.randomUUID();
        TransactionEvent event = TestFactory.transactionEvent(venueId);

        when(generatorProps.topic()).thenReturn(TOPIC_NAME);
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(new CompletableFuture<>());

        transactionProducer.send(event);

        verify(kafkaTemplate).send(TOPIC_NAME, venueId.toString(), event);
    }
}
