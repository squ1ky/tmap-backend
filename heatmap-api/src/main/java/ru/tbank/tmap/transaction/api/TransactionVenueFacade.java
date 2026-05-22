package ru.tbank.tmap.transaction.api;

import java.util.UUID;

public interface TransactionVenueFacade {

    void deleteVenueTransactions(UUID venueId);
}
