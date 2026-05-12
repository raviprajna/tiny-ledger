package com.teya.ledger.exception;

import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {

    public InsufficientFundsException(UUID accountId, long requested, long available) {
        super(String.format("Insufficient funds in account %s: requested %d, available %d",
                accountId, requested, available));
    }
}
