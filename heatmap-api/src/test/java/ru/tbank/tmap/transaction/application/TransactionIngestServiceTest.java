package ru.tbank.tmap.transaction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import ru.tbank.tmap.shared.geo.H3Resolution;
import ru.tbank.tmap.shared.h3.H3IndexService;
import ru.tbank.tmap.transaction.application.command.TransactionEvent;
import ru.tbank.tmap.transaction.application.exception.InvalidTransactionEventException;
import ru.tbank.tmap.transaction.application.port.TransactionWriter;
import ru.tbank.tmap.transaction.domain.Transaction;
import ru.tbank.tmap.venue.domain.VenueCategory;

@ExtendWith(MockitoExtension.class)
class TransactionIngestServiceTest {

    private static final UUID TRANSACTION_ID_1 = UUID.fromString("55555555-5555-5555-5555-555555555551");
    private static final UUID TRANSACTION_ID_2 = UUID.fromString("55555555-5555-5555-5555-555555555552");
    private static final UUID TRANSACTION_ID_3 = UUID.fromString("55555555-5555-5555-5555-555555555553");
    private static final UUID VENUE_ID = UUID.fromString("44444444-4444-4444-4444-444444444441");
    private static final double VALID_LAT = 55.7900;
    private static final double VALID_LNG = 49.1200;
    private static final long H3_RES7_RESULT = 608111111111111111L;
    private static final long H3_RES8_RESULT = 613222222222222222L;
    private static final long H3_RES9_RESULT = 617333333333333333L;
    private static final BigDecimal VALID_AMOUNT = new BigDecimal("100.50");
    private static final String VALID_CATEGORY = "FOOD";
    private static final Instant VALID_OCCURRED_AT = Instant.parse("2025-01-15T12:00:00Z");

    @Mock
    private H3IndexService h3IndexService;

    @Mock
    private TransactionWriter transactionWriter;

    @InjectMocks
    private TransactionIngestService service;

    @Test
    void ingest_whenEventsAreEmpty_thenReturnsZeroAndSkipsDependencies() {
        final int result = service.ingest(List.of());

        assertThat(result).isZero();
        verifyNoInteractions(h3IndexService, transactionWriter);
    }

    @Test
    void ingest_whenSingleValidEvent_thenMapsAllFieldsToTransaction() {
        stubH3IndexService();
        when(transactionWriter.insertBatch(any())).thenReturn(1);

        final int inserted = service.ingest(List.of(validEvent(TRANSACTION_ID_1)));

        assertThat(inserted).isEqualTo(1);
        final List<Transaction> captured = captureWrittenBatch();
        assertThat(captured).hasSize(1);
        final Transaction transaction = captured.get(0);

        assertThat(transaction.id()).isEqualTo(TRANSACTION_ID_1);
        assertThat(transaction.venueId()).isEqualTo(VENUE_ID);
        assertThat(transaction.amount()).isEqualByComparingTo(VALID_AMOUNT);
        assertThat(transaction.lat()).isEqualTo(VALID_LAT);
        assertThat(transaction.lng()).isEqualTo(VALID_LNG);
        assertThat(transaction.h3Res7()).isEqualTo(H3_RES7_RESULT);
        assertThat(transaction.h3Res8()).isEqualTo(H3_RES8_RESULT);
        assertThat(transaction.h3Res9()).isEqualTo(H3_RES9_RESULT);
        assertThat(transaction.category()).isEqualTo(VenueCategory.FOOD);
        assertThat(transaction.occurredAt()).isEqualTo(VALID_OCCURRED_AT);
    }

    @Test
    void ingest_whenValidEvent_thenComputesH3IndicesForAllResolutions() {
        stubH3IndexService();
        when(transactionWriter.insertBatch(any())).thenReturn(1);

        service.ingest(List.of(validEvent(TRANSACTION_ID_1)));

        verify(h3IndexService).toH3(VALID_LAT, VALID_LNG, H3Resolution.RES_7);
        verify(h3IndexService).toH3(VALID_LAT, VALID_LNG, H3Resolution.RES_8);
        verify(h3IndexService).toH3(VALID_LAT, VALID_LNG, H3Resolution.RES_9);
    }

    @Test
    void ingest_whenMultipleValidEvents_thenWritesBatchInOriginalOrder() {
        stubH3IndexService();
        when(transactionWriter.insertBatch(any())).thenReturn(3);

        service.ingest(List.of(
                validEvent(TRANSACTION_ID_1),
                validEvent(TRANSACTION_ID_2),
                validEvent(TRANSACTION_ID_3)
        ));

        assertThat(captureWrittenBatch())
                .extracting(Transaction::id)
                .containsExactly(TRANSACTION_ID_1, TRANSACTION_ID_2, TRANSACTION_ID_3);
    }

    @Test
    void ingest_whenEventHasNegativeAmount_thenThrowsInvalidTransactionEventExceptionWithIndex() {
        stubH3IndexService();
        final TransactionEvent invalid = new TransactionEvent(
                TRANSACTION_ID_2, VENUE_ID, new BigDecimal("-1.00"), VALID_LAT, VALID_LNG, VALID_CATEGORY, VALID_OCCURRED_AT
        );

        assertThatThrownBy(() -> service.ingest(List.of(validEvent(TRANSACTION_ID_1), invalid)))
                .isInstanceOf(InvalidTransactionEventException.class)
                .satisfies(e -> assertThat(((InvalidTransactionEventException) e).getBatchIndex()).isEqualTo(1))
                .hasCauseInstanceOf(IllegalArgumentException.class);
        verify(transactionWriter, never()).insertBatch(any());
    }

    @Test
    void ingest_whenEventHasNullVenueId_thenThrowsInvalidTransactionEventException() {
        stubH3IndexService();
        final TransactionEvent invalid = new TransactionEvent(
                TRANSACTION_ID_1, null, VALID_AMOUNT, VALID_LAT, VALID_LNG, VALID_CATEGORY, VALID_OCCURRED_AT
        );

        assertThatThrownBy(() -> service.ingest(List.of(invalid)))
                .isInstanceOf(InvalidTransactionEventException.class)
                .satisfies(e -> assertThat(((InvalidTransactionEventException) e).getBatchIndex()).isZero())
                .hasCauseInstanceOf(IllegalArgumentException.class);
        verify(transactionWriter, never()).insertBatch(any());
    }

    @Test
    void ingest_whenEventHasLatOutOfRange_thenThrowsInvalidTransactionEventException() {
        stubH3IndexService();
        final TransactionEvent invalid = new TransactionEvent(
                TRANSACTION_ID_1, VENUE_ID, VALID_AMOUNT, 91.0, VALID_LNG, VALID_CATEGORY, VALID_OCCURRED_AT
        );

        assertThatThrownBy(() -> service.ingest(List.of(invalid)))
                .isInstanceOf(InvalidTransactionEventException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ingest_whenEventHasNullCategory_thenThrowsInvalidTransactionEventException() {
        stubH3IndexService();
        final TransactionEvent invalid = new TransactionEvent(
                TRANSACTION_ID_1, VENUE_ID, VALID_AMOUNT, VALID_LAT, VALID_LNG, null, VALID_OCCURRED_AT
        );

        assertThatThrownBy(() -> service.ingest(List.of(invalid)))
                .isInstanceOf(InvalidTransactionEventException.class)
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ingest_whenInvalidEventIsThirdInBatch_thenIndexInExceptionIsTwo() {
        stubH3IndexService();
        final TransactionEvent invalid = new TransactionEvent(
                TRANSACTION_ID_3, VENUE_ID, BigDecimal.ZERO, VALID_LAT, VALID_LNG, VALID_CATEGORY, VALID_OCCURRED_AT
        );

        assertThatThrownBy(() -> service.ingest(List.of(
                validEvent(TRANSACTION_ID_1),
                validEvent(TRANSACTION_ID_2),
                invalid
        )))
                .isInstanceOf(InvalidTransactionEventException.class)
                .satisfies(e -> assertThat(((InvalidTransactionEventException) e).getBatchIndex()).isEqualTo(2));
    }

    @Test
    void ingest_whenWriterReturnsCount_thenServiceReturnsSameCount() {
        stubH3IndexService();
        when(transactionWriter.insertBatch(any())).thenReturn(42);

        final int inserted = service.ingest(List.of(validEvent(TRANSACTION_ID_1)));

        assertThat(inserted).isEqualTo(42);
    }

    @Test
    void ingest_whenWriterThrowsException_thenServiceRethrowsAsIs() {
        stubH3IndexService();
        final RuntimeException dbError = new RuntimeException("db down");
        when(transactionWriter.insertBatch(any())).thenThrow(dbError);

        assertThatThrownBy(() -> service.ingest(List.of(validEvent(TRANSACTION_ID_1))))
                .isSameAs(dbError);
    }

    @Test
    void ingest_whenAllEventsAreValid_thenWriterIsCalledExactlyOnce() {
        stubH3IndexService();
        when(transactionWriter.insertBatch(any())).thenReturn(2);

        service.ingest(List.of(validEvent(TRANSACTION_ID_1), validEvent(TRANSACTION_ID_2)));

        verify(transactionWriter, times(1)).insertBatch(any());
    }

    private void stubH3IndexService() {
        when(h3IndexService.toH3(anyDouble(), anyDouble(), eq(H3Resolution.RES_7))).thenReturn(H3_RES7_RESULT);
        when(h3IndexService.toH3(anyDouble(), anyDouble(), eq(H3Resolution.RES_8))).thenReturn(H3_RES8_RESULT);
        when(h3IndexService.toH3(anyDouble(), anyDouble(), eq(H3Resolution.RES_9))).thenReturn(H3_RES9_RESULT);
    }

    @SuppressWarnings("unchecked")
    private List<Transaction> captureWrittenBatch() {
        final ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass(List.class);
        verify(transactionWriter).insertBatch(captor.capture());
        return captor.getValue();
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
}
