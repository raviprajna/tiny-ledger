package com.teya.ledger.service;

import com.teya.ledger.dto.BalanceResponse;
import com.teya.ledger.dto.TransactionRequest;
import com.teya.ledger.dto.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface LedgerService {

    TransactionResponse recordTransaction(UUID accountId, TransactionRequest request);

    BalanceResponse getBalance(UUID accountId);

    Page<TransactionResponse> getTransactionHistory(UUID accountId, Pageable pageable);
}
