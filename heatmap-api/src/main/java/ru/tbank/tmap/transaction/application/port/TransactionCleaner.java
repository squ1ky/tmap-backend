package ru.tbank.tmap.transaction.application.port;

import java.util.UUID;

public interface TransactionCleaner {

    void deleteByVenueId(UUID venueId);
}
