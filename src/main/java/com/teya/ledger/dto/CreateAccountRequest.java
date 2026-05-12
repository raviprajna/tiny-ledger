package com.teya.ledger.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAccountRequest(
        @NotBlank(message = "Account name is required")
        String name
) {}
