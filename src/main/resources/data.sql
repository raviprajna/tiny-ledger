-- Seed data for testing
-- Account: Alice's Shop
INSERT INTO accounts (id, name, balance, created_at, version)
VALUES ('a1000000-0000-0000-0000-000000000001', 'Alice Shop', 50000, CURRENT_TIMESTAMP, 0);

-- Account: Bob's Cafe
INSERT INTO accounts (id, name, balance, created_at, version)
VALUES ('a2000000-0000-0000-0000-000000000002', 'Bob Cafe', 10000, CURRENT_TIMESTAMP, 0);

-- Transactions for Alice's Shop
INSERT INTO transactions (id, account_id, type, amount, idempotency_key, created_at)
VALUES ('b1000000-0000-0000-0000-000000000001', 'a1000000-0000-0000-0000-000000000001', 'DEPOSIT', 50000, 'seed-alice-deposit-1', CURRENT_TIMESTAMP);

-- Transactions for Bob's Cafe
INSERT INTO transactions (id, account_id, type, amount, idempotency_key, created_at)
VALUES ('b2000000-0000-0000-0000-000000000002', 'a2000000-0000-0000-0000-000000000002', 'DEPOSIT', 10000, 'seed-bob-deposit-1', CURRENT_TIMESTAMP);
