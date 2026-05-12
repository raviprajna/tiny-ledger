package com.teya.ledger.service;

import com.teya.ledger.dto.BalanceResponse;
import com.teya.ledger.dto.TransactionRequest;
import com.teya.ledger.dto.TransactionResponse;
import com.teya.ledger.exception.AccountNotFoundException;
import com.teya.ledger.exception.InsufficientFundsException;
import com.teya.ledger.model.Account;
import com.teya.ledger.model.Transaction;
import com.teya.ledger.model.TransactionType;
import com.teya.ledger.repository.AccountRepository;
import com.teya.ledger.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.UUID;

@Service
public class LedgerServiceImpl implements LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerServiceImpl.class);

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public LedgerServiceImpl(AccountRepository accountRepository,
                             TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional
    public TransactionResponse recordTransaction(UUID accountId, TransactionRequest request) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            log.warn("Duplicate transaction detected. Idempotency key '{}' already processed for account {}. Ignoring duplicate request.",
                    request.idempotencyKey(), accountId);
            return TransactionResponse.from(existing.get());
        }

        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        if (request.type() == TransactionType.DEPOSIT) {
            account.credit(request.amount());
        } else {
            try {
                account.debit(request.amount());
            } catch (IllegalStateException e) {
                throw new InsufficientFundsException(accountId, request.amount(), account.getBalance());
            }
        }

        accountRepository.save(account);

        Transaction transaction = new Transaction(accountId, request.type(), request.amount(), request.idempotencyKey());
        transaction = transactionRepository.save(transaction);

        log.info("Transaction recorded: type={}, amount={}, account={}, idempotencyKey={}",
                request.type(), request.amount(), accountId, request.idempotencyKey());

        return TransactionResponse.from(transaction);
    }

    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return new BalanceResponse(accountId, account.getBalance());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactionHistory(UUID accountId, Pageable pageable) {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException(accountId);
        }
        return transactionRepository.findByAccountIdOrderByCreatedAtDesc(accountId, pageable)
                .map(TransactionResponse::from);
    }
}
