package com.teya.ledger.dto;

import com.teya.ledger.model.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransactionRequest(
        @NotNull(message = "Transaction type is required")
        TransactionType type,

        @Positive(message = "Amount must be positive")
        long amount,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey
) {}
