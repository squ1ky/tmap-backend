package ru.tbank.tmap.generator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.tbank.tmap.generator.domain.Venue;
import ru.tbank.tmap.generator.repository.VenueRepository;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class VenueCache {

    private final VenueRepository venueRepository;
    private final AtomicReference<List<Venue>> venues = new AtomicReference<>(List.of());

    @Scheduled(fixedRateString = "${app.generator.venue-cache-refresh-ms:60000}")
    public void refresh() {
        List<Venue> activeVenues = venueRepository.findAllActive();
        venues.set(activeVenues);
        log.info("Venue cache refreshed: {} active venues", activeVenues.size());
    }

    public Venue getRandomVenue() {
        List<Venue> currentVenue = venues.get();
        if (currentVenue.isEmpty()) {
            throw new IllegalStateException("No active venues in cache");
        }
        int index = ThreadLocalRandom.current().nextInt(currentVenue.size());
        return currentVenue.get(index);
    }

    public boolean isEmpty() {
        return venues.get().isEmpty();
    }
}
