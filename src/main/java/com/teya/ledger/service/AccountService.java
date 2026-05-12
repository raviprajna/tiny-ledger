package com.teya.ledger.service;

import com.teya.ledger.dto.AccountResponse;
import com.teya.ledger.dto.CreateAccountRequest;
import java.util.UUID;

public interface AccountService {

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse getAccount(UUID accountId);
}
