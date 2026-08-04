# Pixel — Portfolio Manager

A full-stack portfolio tracker: Spring Boot 3.2.5 / Java 17 backend, React (Vite)
frontend, MySQL 8. Track buy/sell transactions, see derived holdings and
performance, pull live quotes/profiles/news from Finnhub, and get transparent,
rule-based risk metrics with a BUY/HOLD/AVOID recommendation.

Educational project — nothing here is financial advice.

## Architecture

```
React (Vite) ── HTTP/JSON ──> Spring Boot REST API ──> MySQL
                                      |
                                      +-- MarketDataService (Finnhub proxy, cached, DB fallback)
                                      +-- RiskService (volatility/Sharpe/drawdown/beta + recommendation)
```

- **Historical prices** (charts, risk metrics) come from the `price_history` table,
  bulk-loaded from CSVs on startup, with a synthetic fallback so the app is never
  empty for a demo.
- **Live quote, company profile, and news** are proxied through the backend from
  Finnhub's free REST API (the API key never reaches the browser) and cached
  server-side to stay under Finnhub's rate limits.
- **Holdings are derived, not stored** — computed from the transaction ledger
  using the average-cost method.

## Current status

This repo is being built module-by-module by the team, following the existing
architecture as a reference design:

- ✅ **Foundation & Infra** — MySQL schema, `Instrument`/`PriceHistory` entities
  and repositories, `HistoricalDataLoader`, shared exception handling, CORS
  config, docker-compose, Dockerfiles, CI.
- 🚧 **Portfolio domain** (transactions, holdings, summary/performance) — in progress.
- 🚧 **Market data & Risk domain** (Finnhub proxy, risk metrics) — in progress.
- 🚧 **Frontend** (dashboard, instrument detail, transactions UI) — in progress.

## Quickstart

```bash
cp .env.example .env
# edit .env and set FINNHUB_API_KEY (free key at https://finnhub.io)
docker compose up --build
```

| Service  | URL                                    |
|----------|-----------------------------------------|
| Frontend | http://localhost:5173                   |
| Backend  | http://localhost:8080                   |
| Swagger  | http://localhost:8080/swagger-ui.html   |
| MySQL    | localhost:3306                          |

On startup, the backend generates ~2 years of synthetic daily prices for any of
its 25 demo symbols that don't already have history — AAPL, MSFT, GOOGL, TSLA,
SPY, NVDA, AMZN, META, NFLX, AMD, INTC, JPM, V, MA, JNJ, WMT, PG, DIS, KO, PEP,
XOM, BAC, ORCL, CRM, COST — so the app is never empty. This is logged clearly as
`SYNTHETIC placeholder data`. See
[Loading real historical data](#loading-real-historical-data-kaggle-csvs) below
to replace it with real Kaggle CSVs.

## Environment variables

Set in `.env` (see `.env.example`):

| Variable                     | Default                          | Purpose                                          |
|-------------------------------|-----------------------------------|---------------------------------------------------|
| `MYSQL_DATABASE`               | `portfolio`                      | Database name                                       |
| `MYSQL_USER`                   | `portfolio`                      | Database user                                       |
| `MYSQL_PASSWORD`               | —                                 | Database password                                   |
| `MYSQL_ROOT_PASSWORD`          | —                                 | Database root password                              |
| `SPRING_DATASOURCE_URL`        | `jdbc:mysql://db:3306/portfolio`| JDBC URL (backend)                                  |
| `SPRING_DATASOURCE_USERNAME`   | `portfolio`                      | DB user (backend)                                   |
| `SPRING_DATASOURCE_PASSWORD`   | —                                 | DB password (backend)                               |
| `FINNHUB_API_KEY`              | *(empty)*                        | Finnhub API key — market endpoints degrade to DB fallback / empty results if unset |
| `CORS_ALLOWED_ORIGINS`         | `http://localhost:5173`          | Comma-separated origins allowed to call the API     |
| `SEED_DIR`                     | `/app/seed` (docker) / `../infra/db/seed` (local) | Where the historical data loader looks for CSVs |

## Loading real historical data (Kaggle CSVs)

Drop daily-price CSVs into `infra/db/seed/`, one file per symbol, named
`<SYMBOL>.csv` (e.g. `AAPL.csv`). Expected columns (case-insensitive,
spaces/underscores ignored): `Date, Open, High, Low, Close, Adj Close, Volume`.

On the next `docker compose up`, `HistoricalDataLoader` picks up any CSV whose
symbol isn't already loaded and inserts its rows into `price_history`
(idempotent — rerunning won't duplicate data). It also upserts a matching
`instrument` row; well-known symbols get a real name/asset-type, anything else
defaults to `STOCK` (or `ETF` for a short list of common index ETFs).

To fully replace the synthetic demo data, clear `price_history` and
`instrument` first:

```sql
TRUNCATE price_history, instrument;
```

then restart the backend with your CSVs in place.

## Development

**Backend** (Java 17, Maven):
```bash
cd backend
mvn test
mvn spring-boot:run
```

**Frontend** (Node 20, Vite):
```bash
cd frontend
npm install
npm run dev        # http://localhost:5173, proxies /api to localhost:8080
```

## Disclaimer

Risk metrics and recommendations are rule-based and computed from historical
prices only. Educational use only — not financial advice.