package com.teya.ledger.service;

import com.teya.ledger.dto.AccountResponse;
import com.teya.ledger.dto.CreateAccountRequest;
import com.teya.ledger.exception.AccountNotFoundException;
import com.teya.ledger.model.Account;
import com.teya.ledger.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;

    public AccountServiceImpl(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        Account account = new Account(UUID.randomUUID(), request.name());
        account = accountRepository.save(account);
        return AccountResponse.from(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return AccountResponse.from(account);
    }
}
