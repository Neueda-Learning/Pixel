# Architecture — Pixel Portfolio Manager

## System Overview

```
Browser (React/Vite)
        │  HTTP/JSON  (/api/*)
        ▼
Spring Boot REST API  (:8080)
        │
        ├── PortfolioService      ── TransactionRepository ──┐
        ├── TransactionService    ── TransactionRepository   │
        ├── InstrumentService     ── InstrumentRepository    │──▶ MySQL 8
        ├── MarketDataService ────── PriceHistoryRepository  │
        │       │                   InstrumentRepository ────┘
        │       └── FinnhubMarketDataService ──▶ Finnhub REST API
        │                            (cached, graceful fallback to DB)
        └── RiskService ──────────── PriceHistoryRepository
                └── RiskMath (pure stats: σ, Sharpe, β, drawdown)
```

## Layers

### React Frontend (`frontend/`)
- Single-page application built with Vite + React 18 + React Router 7.
- All API calls go through `src/api/` modules (Axios); never fetches Finnhub directly.
- `useApi` hook provides `{ data, loading, error, reload }` for every data-fetching component.
- Recharts powers all charts (time-series performance, allocation donut).
- Nginx serves the production build and proxies `/api/*` to the backend container.

### Spring Boot Backend (`backend/`)
- Stateless REST API; every response is a DTO — JPA entities never escape the service layer.
- Controllers delegate entirely to services; no business logic in controllers.
- `GlobalExceptionHandler` maps domain exceptions to consistent JSON error responses.

### Database (MySQL 8)
Three tables:

| Table | Purpose |
|-------|---------|
| `instrument` | Reference data — symbol, name, asset type |
| `price_history` | Daily OHLCV rows per symbol (composite PK: symbol + trade_date) |
| `transaction` | Buy/sell ledger — source of truth for all holdings math |

There is **no holdings table**. Holdings are derived on every request from the transaction
ledger using the average-cost method in `PortfolioService`.

### Finnhub Integration
`FinnhubMarketDataService` implements `MarketDataService` and calls:
- `/quote/{symbol}` → live price, change, % change
- `/stock/profile2?symbol=` → company profile (name, sector, market cap, logo)
- `/company-news` → recent headlines
- `/search` → symbol lookup

All responses are cached server-side with Caffeine (configured in `CacheConfig`).
If Finnhub is unavailable or `FINNHUB_API_KEY` is unset, every method falls back to
the `price_history` / `instrument` tables — the app never fails hard on a missing key.

## Data Flow — Portfolio Value

```
User adds BUY transaction
        │
        ▼
transaction table (ledger)
        │
        ▼ (on GET /api/portfolio)
PortfolioService.getHoldings()
  ├── groups transactions by symbol
  ├── computes qty + avg cost via average-cost method
  ├── calls MarketDataService.getQuote(symbol) for live price
  │       └── falls back to MAX(trade_date) from price_history if Finnhub fails
  └── returns HoldingDto list with market value, gain/loss, gain/loss %
```

## Data Flow — Risk Metrics

```
GET /api/risk/{symbol}
        │
        ▼
RiskService.getRisk(symbol)
  ├── loads ~1 year of daily closes from price_history
  ├── computes daily log returns
  ├── RiskMath.annualisedVolatility()   — σ × √252
  ├── RiskMath.sharpeRatio()            — (mean return − rf) / σ   (rf = 0 for simplicity)
  ├── RiskMath.maxDrawdown()            — peak-to-trough % decline
  ├── RiskMath.beta()                   — covariance(stock, SPY) / variance(SPY)
  └── recommendation: BUY / HOLD / AVOID based on rule thresholds
```

## Historical Data Seed

`HistoricalDataLoader` (runs once at startup via `ApplicationRunner`):
1. Scans `SEED_DIR` for `<SYMBOL>.csv` files.
2. For each CSV whose symbol isn't already loaded, bulk-inserts into `price_history`.
3. For any of the 25 known demo symbols still missing data, generates ~2 years of
   synthetic random-walk prices so the app is functional without real data.

Synthetic data is clearly logged as `[SYNTHETIC]` and is intended for demos only.

## Caching Strategy

| Cache name | TTL | What it stores |
|-----------|-----|---------------|
| `quotes` | 5 min | Finnhub live quote per symbol |
| `profiles` | 1 hour | Finnhub company profile per symbol |
| `news` | 15 min | Finnhub news list per symbol |

## CI / CD

GitHub Actions (`.github/workflows/`) runs on every push:
- Backend: `mvn test` (Java 17)
- Frontend: `npm ci && npm run build` (Node 20)

Docker images are built with multi-stage `Dockerfile`s in `backend/` and `frontend/`.
`docker-compose.yml` at the repo root wires all three services (db, backend, frontend).

