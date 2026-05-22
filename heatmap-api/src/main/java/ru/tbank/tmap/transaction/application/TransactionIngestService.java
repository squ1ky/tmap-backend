package ru.tbank.tmap.transaction.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.transaction.application.command.TransactionEvent;
import ru.tbank.tmap.transaction.application.exception.InvalidTransactionEventException;
import ru.tbank.tmap.transaction.application.port.TransactionWriter;
import ru.tbank.tmap.transaction.domain.Transaction;
import ru.tbank.tmap.venue.domain.VenueCategory;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionIngestService {

    private final TransactionWriter transactionWriter;

    @Transactional
    public int ingest(List<TransactionEvent> events) {
        if (events.isEmpty()) {
            return 0;
        }

        List<Transaction> records = new ArrayList<>(events.size());
        for (int i = 0; i < events.size(); i++) {
            TransactionEvent event = events.get(i);
            try {
                records.add(toTransaction(event));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid transaction at batch index {} (id={}): {}",
                        i, event.transactionId(), e.getMessage());
                throw new InvalidTransactionEventException(
                        i, "Failed to map transaction " + event.transactionId(), e
                );
            }
        }

        int inserted = transactionWriter.insertBatch(records);
        log.info("Ingested batch: received={}, inserted={}", events.size(), inserted);
        return inserted;
    }

    private Transaction toTransaction(TransactionEvent event) {
        VenueCategory category = event.category() != null
                ? VenueCategory.fromString(event.category())
                : null;

        return new Transaction(
                event.transactionId(),
                event.venueId(),
                event.amount(),
                event.lat(),
                event.lng(),
                category,
                event.occurredAt()
        );
    }
}
