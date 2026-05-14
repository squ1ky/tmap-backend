package ru.tbank.tmap.infrastructure.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;
import ru.tbank.tmap.transaction.application.command.TransactionEvent;

@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private static final long RETRY_BACKOFF_MS = 1000L;
    private static final long RETRY_MAX_ATTEMPTS = 3L;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TransactionEvent>
            transactionKafkaListenerContainerFactory(
                    ConsumerFactory<String, TransactionEvent> consumerFactory,
                    DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, TransactionEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.setCommonErrorHandler(errorHandler);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        return factory;
    }

    @Bean
    public DefaultErrorHandler transactionErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        DefaultErrorHandler handler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(RETRY_BACKOFF_MS, RETRY_MAX_ATTEMPTS)
        );

        handler.addNotRetryableExceptions(
                DeserializationException.class,
                IllegalArgumentException.class
        );
        return handler;
    }
}
