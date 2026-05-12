package com.teya.ledger;

import com.teya.ledger.dto.AccountResponse;
import com.teya.ledger.dto.BalanceResponse;
import com.teya.ledger.dto.CreateAccountRequest;
import com.teya.ledger.dto.TransactionRequest;
import com.teya.ledger.dto.TransactionResponse;
import com.teya.ledger.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// RANDOM_PORT starts a real embedded server so TestRestTemplate can make actual HTTP calls.
// DirtiesContext resets the application context (and in-memory DB) between tests for full isolation.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class LedgerApiE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    private UUID accountId;

    @BeforeEach
    void setUp() {
        CreateAccountRequest request = new CreateAccountRequest("Test Account");
        ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                "/api/accounts", request, AccountResponse.class);
        accountId = response.getBody().id();
    }

    @Test
    @DisplayName("1. Create account and verify it exists with zero balance")
    void createAccountAndVerify() {
        CreateAccountRequest request = new CreateAccountRequest("New Shop");
        ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
                "/api/accounts", request, AccountResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().name()).isEqualTo("New Shop");
        assertThat(response.getBody().balance()).isZero();
        assertThat(response.getBody().id()).isNotNull();
    }

    @Test
    @DisplayName("2. Deposit increases balance correctly")
    void depositIncreasesBalance() {
        recordTransaction(accountId, TransactionType.DEPOSIT, 5000, UUID.randomUUID().toString());

        ResponseEntity<BalanceResponse> balance = restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, accountId);

        assertThat(balance.getBody().balance()).isEqualTo(5000);
    }

    @Test
    @DisplayName("3. Withdrawal decreases balance correctly")
    void withdrawalDecreasesBalance() {
        recordTransaction(accountId, TransactionType.DEPOSIT, 10000, UUID.randomUUID().toString());
        recordTransaction(accountId, TransactionType.WITHDRAWAL, 3000, UUID.randomUUID().toString());

        ResponseEntity<BalanceResponse> balance = restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, accountId);

        assertThat(balance.getBody().balance()).isEqualTo(7000);
    }

    @Test
    @DisplayName("4. Withdrawal exceeding balance returns insufficient funds error")
    void withdrawalExceedingBalanceReturnsInsufficientFunds() {
        recordTransaction(accountId, TransactionType.DEPOSIT, 1000, UUID.randomUUID().toString());

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/accounts/{id}/transactions",
                new TransactionRequest(TransactionType.WITHDRAWAL, 5000, UUID.randomUUID().toString()),
                String.class, accountId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Insufficient funds");
        assertThat(response.getBody()).contains("available 1000");
    }

    @Test
    @DisplayName("5. Zero amount is rejected with validation error")
    void zeroAmountIsRejected() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/accounts/{id}/transactions",
                new TransactionRequest(TransactionType.DEPOSIT, 0, UUID.randomUUID().toString()),
                String.class, accountId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("6. Negative amount is rejected with validation error")
    void negativeAmountIsRejected() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/accounts/{id}/transactions",
                new TransactionRequest(TransactionType.DEPOSIT, -100, UUID.randomUUID().toString()),
                String.class, accountId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("7. Duplicate idempotency key returns original response without reprocessing")
    void duplicateIdempotencyKeyIsIdempotent() {
        String idempotencyKey = UUID.randomUUID().toString();
        TransactionRequest request = new TransactionRequest(TransactionType.DEPOSIT, 5000, idempotencyKey);

        ResponseEntity<TransactionResponse> first = restTemplate.postForEntity(
                "/api/accounts/{id}/transactions", request, TransactionResponse.class, accountId);
        ResponseEntity<TransactionResponse> second = restTemplate.postForEntity(
                "/api/accounts/{id}/transactions", request, TransactionResponse.class, accountId);

        assertThat(first.getBody().id()).isEqualTo(second.getBody().id());

        ResponseEntity<BalanceResponse> balance = restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, accountId);
        assertThat(balance.getBody().balance()).isEqualTo(5000);
    }

    @Test
    @DisplayName("8. Transaction history returns entries in reverse chronological order")
    void transactionHistoryIsOrdered() {
        String key1 = UUID.randomUUID().toString();
        String key2 = UUID.randomUUID().toString();
        String key3 = UUID.randomUUID().toString();

        recordTransaction(accountId, TransactionType.DEPOSIT, 1000, key1);
        recordTransaction(accountId, TransactionType.DEPOSIT, 2000, key2);
        recordTransaction(accountId, TransactionType.WITHDRAWAL, 500, key3);

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/accounts/{id}/transactions", String.class, accountId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        int pos1 = body.indexOf(key3);
        int pos2 = body.indexOf(key2);
        int pos3 = body.indexOf(key1);
        assertThat(pos1).isLessThan(pos2);
        assertThat(pos2).isLessThan(pos3);
    }

    @Test
    @DisplayName("9. Multiple accounts are isolated from each other")
    void multipleAccountsAreIsolated() {
        CreateAccountRequest request = new CreateAccountRequest("Second Account");
        ResponseEntity<AccountResponse> secondAccount = restTemplate.postForEntity(
                "/api/accounts", request, AccountResponse.class);
        UUID secondId = secondAccount.getBody().id();

        recordTransaction(accountId, TransactionType.DEPOSIT, 10000, UUID.randomUUID().toString());
        recordTransaction(secondId, TransactionType.DEPOSIT, 3000, UUID.randomUUID().toString());

        ResponseEntity<BalanceResponse> balance1 = restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, accountId);
        ResponseEntity<BalanceResponse> balance2 = restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, secondId);

        assertThat(balance1.getBody().balance()).isEqualTo(10000);
        assertThat(balance2.getBody().balance()).isEqualTo(3000);
    }

    @Test
    @DisplayName("10. Concurrent deposits maintain balance consistency under contention")
    void concurrentDepositsAreConsistent() throws InterruptedException {
        int threadCount = 10;
        long depositAmount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
                            "/api/accounts/{id}/transactions",
                            new TransactionRequest(TransactionType.DEPOSIT, depositAmount, UUID.randomUUID().toString()),
                            TransactionResponse.class, accountId);
                    if (response.getStatusCode() == HttpStatus.CREATED) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        ResponseEntity<BalanceResponse> balance = restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, accountId);

        assertThat(balance.getBody().balance()).isEqualTo(successCount.get() * depositAmount);
    }

    private void recordTransaction(UUID accId, TransactionType type, long amount, String idempotencyKey) {
        restTemplate.postForEntity(
                "/api/accounts/{id}/transactions",
                new TransactionRequest(type, amount, idempotencyKey),
                TransactionResponse.class, accId);
    }
}
