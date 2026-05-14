package ru.tbank.tmap.transaction.application.port;

import ru.tbank.tmap.transaction.domain.Transaction;

import java.util.List;

public interface TransactionWriter {

    /**
     * Atomically writes a batch of transactions. Duplicates by id are ignored.
     *
     * @param transactions records to insert
     * @return number of rows actually inserted
     */
    int insertBatch(List<Transaction> transactions);
}
