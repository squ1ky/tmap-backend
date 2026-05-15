package ru.tbank.tmap.transaction.infrastructure.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.listener.BatchListenerFailedException;
import ru.tbank.tmap.transaction.application.TransactionIngestService;
import ru.tbank.tmap.transaction.application.command.TransactionEvent;
import ru.tbank.tmap.transaction.application.exception.InvalidTransactionEventException;

@ExtendWith(MockitoExtension.class)
class TransactionConsumerTest {

    private static final UUID TRANSACTION_ID_1 = UUID.fromString("55555555-5555-5555-5555-555555555551");
    private static final UUID TRANSACTION_ID_2 = UUID.fromString("55555555-5555-5555-5555-555555555552");
    private static final UUID VENUE_ID = UUID.fromString("44444444-4444-4444-4444-444444444441");
    private static final BigDecimal VALID_AMOUNT = new BigDecimal("100.50");
    private static final double VALID_LAT = 55.7900;
    private static final double VALID_LNG = 49.1200;
    private static final String VALID_CATEGORY = "FOOD";
    private static final Instant VALID_OCCURRED_AT = Instant.parse("2025-01-15T12:00:00Z");

    @Mock
    private TransactionIngestService ingestService;

    @InjectMocks
    private TransactionConsumer consumer;

    @Test
    void onBatch_whenEventsAreValid_thenDelegatesToIngestService() {
        final List<TransactionEvent> events = List.of(validEvent(TRANSACTION_ID_1), validEvent(TRANSACTION_ID_2));
        when(ingestService.ingest(any())).thenReturn(2);

        assertThatCode(() -> consumer.onBatch(events)).doesNotThrowAnyException();

        final ArgumentCaptor<List<TransactionEvent>> captor = listCaptor();
        verify(ingestService).ingest(captor.capture());
        assertThat(captor.getValue()).containsExactlyElementsOf(events);
    }

    @Test
    void onBatch_whenServiceSucceeds_thenIngestServiceCalledExactlyOnce() {
        when(ingestService.ingest(any())).thenReturn(1);

        consumer.onBatch(List.of(validEvent(TRANSACTION_ID_1)));

        verify(ingestService, times(1)).ingest(any());
    }

    @Test
    void onBatch_whenEventListIsEmpty_thenDelegatesEmptyListToService() {
        when(ingestService.ingest(any())).thenReturn(0);

        consumer.onBatch(List.of());

        verify(ingestService).ingest(List.of());
    }

    @Test
    void onBatch_whenServiceThrowsInvalidTransactionEvent_thenTranslatesToBatchListenerFailedExceptionWithIndex() {
        final IllegalArgumentException cause = new IllegalArgumentException("amount must be positive");
        final InvalidTransactionEventException invalid = new InvalidTransactionEventException(
                1, "Failed to map transaction " + TRANSACTION_ID_2, cause
        );
        when(ingestService.ingest(any())).thenThrow(invalid);

        assertThatThrownBy(() -> consumer.onBatch(List.of(validEvent(TRANSACTION_ID_1), validEvent(TRANSACTION_ID_2))))
                .isInstanceOf(BatchListenerFailedException.class)
                .satisfies(e -> {
                    final BatchListenerFailedException ex = (BatchListenerFailedException) e;
                    assertThat(ex.getIndex()).isEqualTo(1);
                    assertThat(ex.getCause()).isSameAs(invalid);
                });
    }

    @Test
    void onBatch_whenServiceThrowsInvalidTransactionAtIndexZero_thenBatchListenerFailedExceptionIndexIsZero() {
        final InvalidTransactionEventException invalid = new InvalidTransactionEventException(
                0, "Failed to map transaction " + TRANSACTION_ID_1, new IllegalArgumentException("venueId must not be null")
        );
        when(ingestService.ingest(any())).thenThrow(invalid);

        assertThatThrownBy(() -> consumer.onBatch(List.of(validEvent(TRANSACTION_ID_1))))
                .isInstanceOf(BatchListenerFailedException.class)
                .satisfies(e -> assertThat(((BatchListenerFailedException) e).getIndex()).isZero());
    }

    @Test
    void onBatch_whenServiceThrowsRuntimeException_thenRethrowsAsIsWithoutWrapping() {
        final RuntimeException dbError = new RuntimeException("db down");
        when(ingestService.ingest(any())).thenThrow(dbError);

        assertThatThrownBy(() -> consumer.onBatch(List.of(validEvent(TRANSACTION_ID_1))))
                .isSameAs(dbError);
    }

    @Test
    void onBatch_whenInvalidTransactionEventIsCaught_thenNoOtherServiceCallsAreMade() {
        final InvalidTransactionEventException invalid = new InvalidTransactionEventException(
                0, "boom", new IllegalArgumentException("bad data")
        );
        when(ingestService.ingest(any())).thenThrow(invalid);

        assertThatThrownBy(() -> consumer.onBatch(List.of(validEvent(TRANSACTION_ID_1))))
                .isInstanceOf(BatchListenerFailedException.class);

        verify(ingestService, times(1)).ingest(any());
    }

    private static TransactionEvent validEvent(final UUID id) {
        return new TransactionEvent(
                id,
                VENUE_ID,
                VALID_AMOUNT,
                VALID_LAT,
                VALID_LNG,
                VALID_CATEGORY,
                VALID_OCCURRED_AT
        );
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<List<TransactionEvent>> listCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
