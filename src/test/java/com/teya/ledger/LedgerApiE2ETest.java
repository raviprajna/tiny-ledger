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
    @DisplayName("1. Create account and verify it exists")
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
        recordTransaction(accountId, TransactionType.DEPOSIT, 5000, "dep-1");

        ResponseEntity<BalanceResponse> balance = restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, accountId);

        assertThat(balance.getBody().balance()).isEqualTo(5000);
    }

    @Test
    @DisplayName("3. Withdrawal decreases balance correctly")
    void withdrawalDecreasesBalance() {
        recordTransaction(accountId, TransactionType.DEPOSIT, 10000, "dep-2");
        recordTransaction(accountId, TransactionType.WITHDRAWAL, 3000, "wd-1");

        ResponseEntity<BalanceResponse> balance = restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, accountId);

        assertThat(balance.getBody().balance()).isEqualTo(7000);
    }

    @Test
    @DisplayName("4. Withdrawal exceeding balance returns 400")
    void withdrawalExceedingBalanceFails() {
        recordTransaction(accountId, TransactionType.DEPOSIT, 1000, "dep-3");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/accounts/{id}/transactions",
                new TransactionRequest(TransactionType.WITHDRAWAL, 5000, "wd-fail"),
                String.class, accountId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Insufficient funds");
    }

    @Test
    @DisplayName("5. Zero amount is rejected")
    void zeroAmountIsRejected() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/accounts/{id}/transactions",
                new TransactionRequest(TransactionType.DEPOSIT, 0, "zero-1"),
                String.class, accountId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("6. Negative amount is rejected")
    void negativeAmountIsRejected() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/accounts/{id}/transactions",
                new TransactionRequest(TransactionType.DEPOSIT, -100, "neg-1"),
                String.class, accountId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @DisplayName("7. Duplicate idempotency key returns same response without double-processing")
    void duplicateIdempotencyKeyIsIdempotent() {
        TransactionRequest request = new TransactionRequest(TransactionType.DEPOSIT, 5000, "idempotent-1");

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
        recordTransaction(accountId, TransactionType.DEPOSIT, 1000, "hist-1");
        recordTransaction(accountId, TransactionType.DEPOSIT, 2000, "hist-2");
        recordTransaction(accountId, TransactionType.WITHDRAWAL, 500, "hist-3");

        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/accounts/{id}/transactions", String.class, accountId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        int pos1 = body.indexOf("hist-3");
        int pos2 = body.indexOf("hist-2");
        int pos3 = body.indexOf("hist-1");
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

        recordTransaction(accountId, TransactionType.DEPOSIT, 10000, "iso-1");
        recordTransaction(secondId, TransactionType.DEPOSIT, 3000, "iso-2");

        ResponseEntity<BalanceResponse> balance1 = restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, accountId);
        ResponseEntity<BalanceResponse> balance2 = restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, secondId);

        assertThat(balance1.getBody().balance()).isEqualTo(10000);
        assertThat(balance2.getBody().balance()).isEqualTo(3000);
    }

    @Test
    @DisplayName("10. Concurrent deposits maintain balance consistency")
    void concurrentDepositsAreConsistent() throws InterruptedException {
        int threadCount = 10;
        long depositAmount = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
                            "/api/accounts/{id}/transactions",
                            new TransactionRequest(TransactionType.DEPOSIT, depositAmount, "concurrent-" + idx),
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
