CREATE TABLE IF NOT EXISTS transactions (
    id         SERIAL PRIMARY KEY,
    account_id BIGINT        NOT NULL,
    amount     NUMERIC(12,2) NOT NULL,
    country    VARCHAR(5)    NOT NULL,
    tx_time    TIMESTAMP     NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_tx_account_time ON transactions(account_id, tx_time DESC);

CREATE TABLE IF NOT EXISTS alerts (
    id          SERIAL PRIMARY KEY,
    tx_id       INT           NOT NULL REFERENCES transactions(id),
    account_id  BIGINT        NOT NULL,
    amount      NUMERIC(12,2) NOT NULL,
    country     VARCHAR(5)    NOT NULL,
    reason      VARCHAR(200)  NOT NULL,
    status      VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMP     NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMP
);
