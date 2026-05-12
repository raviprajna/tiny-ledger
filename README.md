# Tiny Ledger

A simple ledger API for recording money movements (deposits and withdrawals), viewing balances, and transaction history.

## Quick Start

```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/accounts` | Create a new account |
| GET | `/api/accounts/{id}` | Get account details |
| POST | `/api/accounts/{id}/transactions` | Record a deposit or withdrawal |
| GET | `/api/accounts/{id}/balance` | Get current balance |
| GET | `/api/accounts/{id}/transactions` | Get transaction history (paginated) |

## Usage Examples

**Create an account:**
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"name": "My Shop"}'
```

**Deposit funds (amount in pence):**
```bash
curl -X POST http://localhost:8080/api/accounts/{id}/transactions \
  -H "Content-Type: application/json" \
  -d '{"type": "DEPOSIT", "amount": 10000, "idempotencyKey": "dep-001"}'
```

**Withdraw funds:**
```bash
curl -X POST http://localhost:8080/api/accounts/{id}/transactions \
  -H "Content-Type: application/json" \
  -d '{"type": "WITHDRAWAL", "amount": 3000, "idempotencyKey": "wd-001"}'
```

**Check balance:**
```bash
curl http://localhost:8080/api/accounts/{id}/balance
```

**View transaction history:**
```bash
curl http://localhost:8080/api/accounts/{id}/transactions?page=0&size=20
```

## Running Tests

```bash
./mvnw test
```

## Project Structure

```
src/main/java/com/teya/ledger/
├── LedgerApplication.java          # Application entry point
├── controller/                     # REST API layer — request/response handling
│   ├── AccountController.java
│   └── LedgerController.java
├── dto/                            # Data transfer objects — API contracts
│   ├── CreateAccountRequest.java
│   ├── TransactionRequest.java
│   ├── AccountResponse.java
│   ├── TransactionResponse.java
│   ├── BalanceResponse.java
│   └── ErrorResponse.java
├── model/                          # Domain entities — business rules live here
│   ├── Account.java
│   ├── Transaction.java
│   └── TransactionType.java
├── repository/                     # Data access layer — persistence abstraction
│   ├── AccountRepository.java
│   └── TransactionRepository.java
├── service/                        # Business logic — orchestration and invariants
│   ├── AccountService.java         (interface)
│   ├── AccountServiceImpl.java
│   ├── LedgerService.java          (interface)
│   └── LedgerServiceImpl.java
└── exception/                      # Error handling — consistent API error responses
    ├── AccountNotFoundException.java
    ├── InsufficientFundsException.java
    └── GlobalExceptionHandler.java
```

## Design Decisions

### Concurrency & Ordering

Transactions are processed under a pessimistic write lock on the account row. This guarantees that concurrent requests (e.g., a deposit and withdrawal arriving simultaneously) are serialized at the database level — no race conditions, no lost updates. The lock scope is minimal (single account), so different accounts never block each other.

### Idempotency

Every transaction requires an `idempotencyKey`. If the same key is submitted twice (e.g., due to a network retry), the duplicate is detected, logged, and the original response is returned without reprocessing. This prevents double-charging or double-crediting.

### Reliability

The entire transaction operation (balance update + transaction record) runs in a single database transaction. Either both succeed or both roll back — no event is ever lost. The client can safely retry on timeout knowing the idempotency key will deduplicate.

### Amounts in Minor Units

All monetary amounts are in **pence** (minor currency units) as integers. This eliminates floating-point precision issues entirely. For example, £50.00 is represented as `5000`.

## Assumptions

1. Single currency (GBP) per account — multi-currency is out of scope
2. Balance cannot go negative (no overdraft facility)
3. Account creation is a separate operation from first transaction
4. Transaction history is paginated and returns newest-first
5. Authentication/authorization is not implemented
6. The idempotency key is mandatory — callers are responsible for generating unique keys

## Testing Strategy

This project uses **E2E tests only** (10 tests covering all critical paths and edge cases). The rationale:

- The domain is small and well-bounded — every meaningful behaviour is exercisable through the API
- E2E tests verify the actual contract that consumers depend on, including serialization, HTTP status codes, and database interactions
- Unit tests on thin service methods would duplicate the E2E coverage without catching additional bugs
- Fewer tests mean faster refactoring cycles — changing internals doesn't require updating a parallel test hierarchy
- The E2E suite covers: happy paths, validation errors, insufficient funds, idempotency, account isolation, and concurrent access consistency

For a production system at scale, targeted unit tests would be added for complex business logic (e.g., fee calculations, currency conversion) where the combinatorial space exceeds what's practical to cover via API tests.
