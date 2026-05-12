package com.teya.ledger.controller;

import com.teya.ledger.dto.BalanceResponse;
import com.teya.ledger.dto.TransactionRequest;
import com.teya.ledger.dto.TransactionResponse;
import com.teya.ledger.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts/{accountId}")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> recordTransaction(
            @PathVariable UUID accountId,
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = ledgerService.recordTransaction(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable UUID accountId) {
        return ResponseEntity.ok(ledgerService.getBalance(accountId));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getTransactionHistory(
            @PathVariable UUID accountId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ledgerService.getTransactionHistory(accountId, pageable));
    }
}
