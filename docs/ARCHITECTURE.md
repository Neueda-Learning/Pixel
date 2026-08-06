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

## Historical Data Loading

`HistoricalDataLoader` (runs once at startup via `CommandLineRunner`):
1. Derives the list of symbols actually held in the portfolio from the `transaction` table
   (`TransactionRepository.findDistinctSymbols()`) — no hardcoded demo symbol list.
2. For any of those symbols still missing `price_history` rows, fetches real daily OHLCV data
   from the Twelve Data API (`TwelveDataHistoricalService`) and persists it, pacing calls 8
   seconds apart to respect the free tier's 8 requests/minute limit.
3. Adding a brand-new symbol via `TransactionService.add`/`importAll` triggers the same
   on-demand backfill immediately, without waiting for a restart.

CSVs dropped into `SEED_DIR` (`infra/db/seed/<SYMBOL>.csv`) are still picked up as an optional,
faster alternative to the live API for any symbol not yet in `price_history` — useful for loading
real datasets in bulk (e.g. from Kaggle) — but this isn't required; without `TWELVEDATA_API_KEY`
or seed CSVs, portfolio symbols simply have no chart data until one of those is provided.

## Caching Strategy

| Cache name | TTL | What it stores |
|-----------|-----|---------------|
| `quotes` | 5 min | Finnhub live quote per symbol |
| `profiles` | 1 hour | Finnhub company profile per symbol |
| `news` | 15 min | Finnhub news list per symbol |

## CI / CD

GitHub Actions (`.github/workflows/ci.yml`) runs on every push to `main` and on every pull
request:
- Backend: `mvn -B test` (Java 17) with a JaCoCo coverage report, then `mvn -B package`;
  Surefire reports are uploaded as a build artifact.
- Frontend: `npm install` then `npm run test:coverage` (Vitest + v8 coverage), then `npm run
  build`; the coverage report is uploaded as a build artifact.
- Docker (main only, after backend + frontend pass): sanity-builds both Docker images.

Docker images are built with multi-stage `Dockerfile`s in `backend/` and `frontend/`.
`docker-compose.yml` at the repo root wires all three services (db, backend, frontend).

