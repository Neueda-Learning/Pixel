# API Contract — Pixel Portfolio Manager v1.0.0

Interactive, always-current docs are available at `/swagger-ui.html` when the backend is running.

Base URL: `http://localhost:8080`

---

## Instruments

### `GET /api/instruments`
List all known instruments.

**Response**
```json
[
  { "symbol": "AAPL", "name": "Apple Inc.", "assetType": "STOCK" },
  { "symbol": "SPY",  "name": "SPDR S&P 500 ETF", "assetType": "ETF" }
]
```

### `GET /api/instruments/{symbol}/prices?period=1M`
Price series for a symbol. `period`: `1M` `3M` `6M` `1Y` `ALL`

**Response**
```json
[
  { "date": "2026-07-01", "close": 192.35 },
  { "date": "2026-07-02", "close": 194.10 }
]
```

---

## Portfolio

### `GET /api/portfolio`
Current holdings derived from the transaction ledger.

**Response**
```json
[
  {
    "symbol": "AAPL", "name": "Apple Inc.", "assetType": "STOCK",
    "quantity": 10.0000, "avgCost": 175.5000,
    "currentPrice": 192.3500, "marketValue": 1923.50,
    "gainLoss": 168.50, "gainLossPct": 9.60,
    "priceSource": "FINNHUB"
  }
]
```
`priceSource` is `FINNHUB`, `DB`, or `UNAVAILABLE`.

### `GET /api/portfolio/summary`
Portfolio totals and allocation breakdown by asset type.

**Response**
```json
{
  "totalValue": 15420.00,
  "totalCost": 14100.00,
  "totalGainLoss": 1320.00,
  "totalGainLossPct": 9.36,
  "holdingsCount": 5,
  "allocation": [
    { "assetType": "ETF",   "value": 5200.00, "pct": 33.72 },
    { "assetType": "STOCK", "value": 10220.00, "pct": 66.28 }
  ]
}
```

### `GET /api/portfolio/performance?period=6M`
Portfolio mark-to-market value over time. `period`: `1M` `3M` `6M` `1Y` `ALL`

**Response**
```json
[
  { "date": "2026-02-01", "value": 13500.00 },
  { "date": "2026-02-02", "value": 13620.50 }
]
```

---

## Transactions

### `GET /api/transactions?period=3M`
Transaction history. `period`: `3M` `6M` `1Y` `ALL`

**Response**
```json
[
  {
    "id": 1, "symbol": "AAPL", "txType": "BUY",
    "quantity": 10.0, "price": 175.50, "fees": 1.00,
    "executedAt": "2026-01-15T10:30:00Z"
  }
]
```

### `POST /api/transactions`
Add a buy or sell transaction.

**Request**
```json
{
  "symbol": "AAPL",
  "txType": "BUY",
  "quantity": 5.0,
  "price": 192.35,
  "fees": 0.99,
  "executedAt": "2026-08-05T09:00:00Z"
}
```

**Response** — `201 Created` with the saved `TransactionResponseDto`.

### `DELETE /api/transactions/{id}`
Remove a transaction by ID. Returns `204 No Content`.

---

## Market Data

All market endpoints proxy Finnhub and fall back to the `price_history` / `instrument`
tables when Finnhub is unavailable or `FINNHUB_API_KEY` is unset.

### `GET /api/market/quote/{symbol}`
```json
{
  "symbol": "AAPL", "current": 192.35, "change": 1.85,
  "changePct": 0.97, "high": 193.10, "low": 191.20,
  "open": 191.50, "prevClose": 190.50, "source": "FINNHUB"
}
```

### `GET /api/market/profile/{symbol}`
```json
{
  "symbol": "AAPL", "name": "Apple Inc.", "exchange": "NASDAQ",
  "industry": "Technology", "logo": "https://...", "marketCap": 2950000000000.0,
  "weburl": "https://apple.com", "country": "US", "currency": "USD"
}
```

### `GET /api/market/news/{symbol}`
```json
[
  {
    "headline": "Apple reports record Q3 earnings",
    "source": "Reuters",
    "url": "https://...",
    "summary": "...",
    "datetime": 1722873600
  }
]
```

### `GET /api/market/search?q=apple`
```json
[
  { "symbol": "AAPL", "description": "Apple Inc.", "type": "Common Stock" }
]
```

---

## Risk

### `GET /api/risk/{symbol}`
Rule-based risk metrics and a BUY / HOLD / AVOID recommendation.

**Response**
```json
{
  "symbol": "AAPL",
  "annualisedVolatility": 24.5,
  "sharpeRatio": 1.32,
  "maxDrawdown": -18.7,
  "beta": 1.12,
  "recommendation": "BUY",
  "rationale": "Low volatility, positive Sharpe, moderate drawdown"
}
```

---

## Chat Assistant

Rule-based, deterministic natural-language portfolio Q&A — no external LLM/AI service,
no API key, no network egress. Every answer is derived from live `PortfolioService` /
`RiskService` data. See [docs/ARCHITECTURE.md#chat-assistant--ai-integration](ARCHITECTURE.md#chat-assistant--ai-integration)
for the design rationale.

### `POST /api/chat`
Ask the portfolio assistant a question in plain English.

**Request**
```json
{ "message": "What's my best performer?" }
```

**Response**
```json
{
  "reply": "Your best performer is AAPL (Apple Inc.), +9.60% (+$168.50)."
}
```

Supported intents: greeting/help, risk check & buy/sell recommendation for a named
symbol (e.g. "should I buy AAPL?"), best/worst performer, allocation & rebalancing
suggestions, holdings list/count, performance over a period (1M/3M/6M/1Y/ALL), and
portfolio summary/value. Anything unmatched returns a help message listing example
questions.

---

## Error Responses

All errors follow a consistent envelope:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Instrument not found: XYZ",
  "timestamp": "2026-08-05T10:00:00Z"
}
```

| HTTP Status | When |
|-------------|------|
| `400 Bad Request` | Validation failure on `POST /api/transactions` |
| `404 Not Found` | Symbol not in `instrument` table |
| `500 Internal Server Error` | Unexpected backend error |

