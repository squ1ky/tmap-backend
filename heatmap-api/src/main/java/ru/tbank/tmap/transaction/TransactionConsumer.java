package ru.tbank.tmap.transaction;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.shared.h3.H3IndexService;

import java.util.ArrayList;
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

        List<TransactionRow> rows = new ArrayList<>(events.size());
        for (int i = 0; i < events.size(); i++) {
            TransactionEvent event = events.get(i);
            try {
                rows.add(toRow(event));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid transaction at batch index {} (id={}): {}",
                        i, event.transactionId(), e.getMessage());
                throw new BatchListenerFailedException(
                        "Failed to map transaction " + event.transactionId(), e, i
                );
            }
        }

        int inserted = batchWriter.insertBatch(rows);
        log.info("Consumed batch: received={}, inserted={}", events.size(), inserted);
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.NPathComplexity"})
    private TransactionRow toRow(TransactionEvent event) {
        if (event.transactionId() == null) {
            throw new IllegalArgumentException("transactionId must not be null");
        }
        if (event.venueId() == null) {
            throw new IllegalArgumentException("venueId must not be null");
        }
        if (event.amount() == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        VenueCategory category = VenueCategory.fromString(
                event.category() != null ? event.category() : ""
        );
        if (event.occurredAt() == null) {
            throw new IllegalArgumentException("occurredAt must not be null");
        }

        if (event.amount().signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive, got " + event.amount());
        }
        if (event.lat() < -90.0 || event.lat() > 90.0) {
            throw new IllegalArgumentException("lat must be in [-90, 90], got " + event.lat());
        }
        if (event.lng() < -180.0 || event.lng() > 180.0) {
            throw new IllegalArgumentException("lng must be in [-180, 180], got " + event.lng());
        }

        return new TransactionRow(
                event.transactionId(),
                event.venueId(),
                event.amount(),
                event.lat(),
                event.lng(),
                h3IndexService.toH3(event.lat(), event.lng(), H3Resolution.RES_7),
                h3IndexService.toH3(event.lat(), event.lng(), H3Resolution.RES_8),
                h3IndexService.toH3(event.lat(), event.lng(), H3Resolution.RES_9),
                category,
                event.occurredAt()
        );
    }
}
