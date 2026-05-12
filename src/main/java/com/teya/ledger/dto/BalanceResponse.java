package com.teya.ledger.dto;

import java.util.UUID;

public record BalanceResponse(
        UUID accountId,
        long balance
) {}
