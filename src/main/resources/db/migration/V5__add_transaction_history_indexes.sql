DROP INDEX idx_transactions_from_account_id;
DROP INDEX idx_transactions_to_account_id;

CREATE INDEX idx_transactions_from_account_created_at ON transactions (from_account_id, created_at);
CREATE INDEX idx_transactions_to_account_created_at ON transactions (to_account_id, created_at);
