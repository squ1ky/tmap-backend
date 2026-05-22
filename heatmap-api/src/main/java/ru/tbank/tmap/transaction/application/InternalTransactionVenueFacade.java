package ru.tbank.tmap.transaction.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tbank.tmap.transaction.api.TransactionVenueFacade;
import ru.tbank.tmap.transaction.application.port.TransactionCleaner;

@Service
@RequiredArgsConstructor
@Transactional
public class InternalTransactionVenueFacade implements TransactionVenueFacade {

    private final TransactionCleaner transactionCleaner;

    @Override
    public void deleteVenueTransactions(final UUID venueId) {
        transactionCleaner.deleteByVenueId(venueId);
    }
}
