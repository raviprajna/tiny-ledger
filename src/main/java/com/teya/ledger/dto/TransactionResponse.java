package com.teya.ledger.dto;

import com.teya.ledger.model.Transaction;
import com.teya.ledger.model.TransactionType;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        UUID accountId,
        TransactionType type,
        long amount,
        String idempotencyKey,
        Instant createdAt
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getAccountId(),
                transaction.getType(),
                transaction.getAmount(),
                transaction.getIdempotencyKey(),
                transaction.getCreatedAt()
        );
    }
}
