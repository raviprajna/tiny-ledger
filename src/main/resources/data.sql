-- Seed data for manual testing
-- Account: Alice's Shop (balance: £500.00)
INSERT INTO accounts (id, name, balance, created_at, version)
VALUES ('a1000000-0000-0000-0000-000000000001', 'Alice Shop', 50000, CURRENT_TIMESTAMP, 0);

-- Account: Bob's Cafe (balance: £100.00)
INSERT INTO accounts (id, name, balance, created_at, version)
VALUES ('a2000000-0000-0000-0000-000000000002', 'Bob Cafe', 10000, CURRENT_TIMESTAMP, 0);

-- Account: Charlie's Bakery (balance: £0.00 — empty account for withdrawal failure testing)
INSERT INTO accounts (id, name, balance, created_at, version)
VALUES ('a3000000-0000-0000-0000-000000000003', 'Charlie Bakery', 0, CURRENT_TIMESTAMP, 0);

-- Transactions for Alice's Shop (deposit £700, withdraw £200 = £500 balance)
INSERT INTO transactions (id, account_id, type, amount, idempotency_key, created_at)
VALUES ('de100000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'DEPOSIT', 70000, 'seed-alice-deposit-1', CURRENT_TIMESTAMP);

INSERT INTO transactions (id, account_id, type, amount, idempotency_key, created_at)
VALUES ('da200000-0000-0000-0000-000000000002', 'a1000000-0000-0000-0000-000000000001', 'WITHDRAWAL', 20000, 'seed-alice-withdrawal-1', CURRENT_TIMESTAMP);

-- Transactions for Bob's Cafe (deposit £150, withdraw £50 = £100 balance)
INSERT INTO transactions (id, account_id, type, amount, idempotency_key, created_at)
VALUES ('de300000-0000-0000-0000-000000000003', 'a2000000-0000-0000-0000-000000000002', 'DEPOSIT', 15000, 'seed-bob-deposit-1', CURRENT_TIMESTAMP);

INSERT INTO transactions (id, account_id, type, amount, idempotency_key, created_at)
VALUES ('da400000-0000-0000-0000-000000000004', 'a2000000-0000-0000-0000-000000000002', 'WITHDRAWAL', 5000, 'seed-bob-withdrawal-1', CURRENT_TIMESTAMP);
