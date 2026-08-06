CREATE TABLE IF NOT EXISTS instrument (
    symbol      VARCHAR(20) PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    asset_type  VARCHAR(20)  NOT NULL,
    currency    VARCHAR(10)  DEFAULT 'USD'
);

CREATE TABLE IF NOT EXISTS transaction (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    symbol       VARCHAR(20),
    tx_type      VARCHAR(10) NOT NULL,
    quantity     NUMERIC(18,4) NOT NULL,
    price        NUMERIC(18,4) NOT NULL,
    buy_price    NUMERIC(18,4),
    buy_transaction_id BIGINT,
    fees         NUMERIC(18,4) DEFAULT 0,
    executed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes        VARCHAR(500),
    CONSTRAINT fk_transaction_symbol FOREIGN KEY (symbol) REFERENCES instrument(symbol)
);

CREATE TABLE IF NOT EXISTS price_history (
    symbol     VARCHAR(20),
    trade_date DATE,
    open       NUMERIC(18,4),
    high       NUMERIC(18,4),
    low        NUMERIC(18,4),
    close      NUMERIC(18,4),
    adj_close  NUMERIC(18,4),
    volume     BIGINT,
    PRIMARY KEY (symbol, trade_date)
);
CREATE INDEX idx_price_symbol_date ON price_history(symbol, trade_date);
