package ru.tbank.tmap.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.service.H3IndexService;
import ru.tbank.tmap.kafka.event.TransactionEvent;
import ru.tbank.tmap.repository.jdbc.TransactionBatchWriter;
import ru.tbank.tmap.repository.model.TransactionRow;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionConsumer {

    private final H3IndexService h3IndexService;
    private final TransactionBatchWriter batchWriter;

    @KafkaListener(
            topics = "${app.kafka.transactions-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "transactionKafkaListenerContainerFactory"
    )
    @Transactional
    public void onBatch(List<TransactionEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        List<TransactionRow> rows = events.stream()
                .map(this::toRow)
                .toList();

        int inserted = batchWriter.insertBatch(rows);
        log.info("Consumed batch: received={}, inserted={}", events.size(), inserted);
    }

    private TransactionRow toRow(TransactionEvent event) {
        return new TransactionRow(
                event.transactionId(),
                event.venueId(),
                event.amount(),
                event.lat(),
                event.lng(),
                h3IndexService.toH3(event.lat(), event.lng(), H3IndexService.RES_7),
                h3IndexService.toH3(event.lat(), event.lng(), H3IndexService.RES_8),
                h3IndexService.toH3(event.lat(), event.lng(), H3IndexService.RES_9),
                event.category(),
                event.occurredAt()
        );
    }
}
