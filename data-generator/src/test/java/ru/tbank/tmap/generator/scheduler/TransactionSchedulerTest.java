package ru.tbank.tmap.generator.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.tbank.tmap.generator.TestFactory;
import ru.tbank.tmap.generator.config.GeneratorProperties;
import ru.tbank.tmap.generator.kafka.TransactionProducer;
import ru.tbank.tmap.generator.kafka.event.TransactionEvent;
import ru.tbank.tmap.generator.service.TransactionGenerator;
import ru.tbank.tmap.generator.service.VenueCache;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionSchedulerTest {

    @Mock
    private TransactionGenerator generator;

    @Mock
    private TransactionProducer producer;

    @Mock
    private VenueCache venueCache;

    private TransactionScheduler scheduler;

    private static final GeneratorProperties GENERATOR_PROPS = TestFactory.fixedBatchGeneratorProps(2);

    @BeforeEach
    void setUp() {
        scheduler = new TransactionScheduler(generator, producer, venueCache, GENERATOR_PROPS);
    }

    @Test
    void generateBatch_whenCacheIsEmpty_thenSkipsGeneration() {
        when(venueCache.isEmpty()).thenReturn(true);

        scheduler.generateBatch();

        verifyNoInteractions(generator);
        verifyNoInteractions(producer);
    }

    @Test
    void generateBatch_whenCacheHasVenues_thenGeneratesAndSendsBatch() {
        when(venueCache.isEmpty()).thenReturn(false);
        TransactionEvent event = TestFactory.transactionEvent();
        when(generator.generate()).thenReturn(event);

        scheduler.generateBatch();
        verify(generator, times(2)).generate();
        verify(producer, times(2)).send(event);
    }

    @Test
    void generateBatch_whenGeneratorThrows_thenDoesNotPropagate() {
        when(venueCache.isEmpty()).thenReturn(false);
        when(generator.generate()).thenThrow(new RuntimeException("DB error"));

        scheduler.generateBatch();
    }
}
