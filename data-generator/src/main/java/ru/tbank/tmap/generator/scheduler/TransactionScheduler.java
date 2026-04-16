package ru.tbank.tmap.generator.scheduler;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.tbank.tmap.generator.config.GeneratorProperties;
import ru.tbank.tmap.generator.kafka.event.TransactionEvent;
import ru.tbank.tmap.generator.kafka.event.TransactionProducer;
import ru.tbank.tmap.generator.service.TransactionGenerator;
import ru.tbank.tmap.generator.service.VenueCache;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionScheduler {

    private static final String THREAD_NAME = "tx-generator";
    private static final int EXECUTOR_TERMINATION_AWAIT = 5;

    private final TransactionGenerator generator;
    private final TransactionProducer producer;
    private final VenueCache venueCache;
    private final GeneratorProperties generatorProps;

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, THREAD_NAME);
        thread.setDaemon(true);
        return thread;
    });

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        log.info("Transaction scheduler started");
        scheduleNext();
    }

    @PreDestroy
    public void stop() {
        log.info("Shutting down transaction scheduler");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(EXECUTOR_TERMINATION_AWAIT, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void scheduleNext() {
        long delay = randomDelay();
        executor.schedule(this::generateBatch, delay, TimeUnit.MILLISECONDS);
    }

    private void generateBatch() {
        try {
            if (venueCache.isEmpty()) {
                log.debug("No active venues, skipping batch");
                return;
            }

            int batchSize = randomBatchSize();
            int sent = 0;

            for (int i = 0; i < batchSize; i++) {
                TransactionEvent event = generator.generate();
                producer.send(event);
                sent++;
            }

            log.info("Generated batch of {} transactions", sent);
        } catch (Exception e) {
            log.error("Error generating batch: {}", e.getMessage(), e);
        } finally {
            scheduleNext();
        }
    }

    private int randomBatchSize() {
        GeneratorProperties.Batch batch = generatorProps.batch();
        return ThreadLocalRandom.current().nextInt(
                batch.minSize(),
                batch.maxSize() + 1
        );
    }

    private long randomDelay() {
        GeneratorProperties.Batch batch = generatorProps.batch();
        return ThreadLocalRandom.current().nextLong(
                batch.minIntervalMs(),
                batch.maxIntervalMs() + 1
        );
    }
}
