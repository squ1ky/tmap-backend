package ru.tbank.tmap.transaction.infastructure.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.transaction.application.TransactionIngestService;
import ru.tbank.tmap.transaction.application.command.TransactionEvent;
import ru.tbank.tmap.transaction.application.exception.InvalidTransactionEventException;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {

    private final TransactionIngestService ingestService;

    @KafkaListener(
            topics = "${app.kafka.transactions-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "transactionKafkaListenerContainerFactory"
    )
    public void onBatch(List<TransactionEvent> events) {
        try {
            ingestService.ingest(events);
        } catch (InvalidTransactionEventException e) {
            throw new BatchListenerFailedException(e.getMessage(), e, e.getBatchIndex());
        }
    }
}
