# Pixel — Portfolio Manager

A full-stack portfolio tracker: Spring Boot 3.2.5 / Java 17 backend, React (Vite)
frontend, MySQL 8. Track buy/sell transactions with lot-based FIFO sell pricing,
see derived holdings and performance, pull live quotes/profiles/news from
Finnhub, get transparent rule-based risk metrics with a BUY/HOLD/AVOID
recommendation, and ask an AI chatbot questions about your portfolio.

Educational project — nothing here is financial advice.

## Architecture

```
React (Vite) ── HTTP/JSON ──> Spring Boot REST API ──> MySQL
                                      |
                                      +-- MarketDataService (Finnhub proxy, cached, DB fallback)
                                      +-- RiskService (volatility/Sharpe/drawdown/beta + recommendation)
                                      +-- TwelveDataHistoricalService (live daily OHLCV backfill)
```

- **Historical prices** (charts, risk metrics) come from the `price_history` table.
  `HistoricalDataLoader` derives which symbols need data from the transaction
  ledger and backfills real daily OHLCV from the Twelve Data API on demand — no
  synthetic or hardcoded demo data.
- **Live quote, company profile, and news** are proxied through the backend from
  Finnhub's free REST API (the API key never reaches the browser) and cached
  server-side to stay under Finnhub's rate limits.
- **Holdings are derived, not stored** — computed from the transaction ledger
  using the average-cost method.
- **AI chatbot** (`frontend/src/chatbot/`) answers portfolio/risk/market
  questions using live data from the existing API, with Groq as an LLM
  fallback for open-ended questions — no new backend endpoints required.

## Release

**v2.0.0** — in progress, cut from `develop`. See [KANBAN.md](KANBAN.md) for the
full feature history, including what shipped in v1.0.0.

## Feature status

- ✅ **Foundation & Infra** — MySQL schema, entities, repositories, `HistoricalDataLoader`, CORS, docker-compose, Dockerfiles, CI (test + coverage on every PR).
- ✅ **Market Data** — Finnhub proxy (quote, profile, news, search), Caffeine cache, graceful DB fallback.
- ✅ **Risk Engine** — Annualised volatility, Sharpe ratio, max drawdown, beta, BUY/HOLD/AVOID recommendation.
- ✅ **Portfolio domain** — Transaction ledger, average-cost holdings derivation, summary, performance history, lot-based FIFO sell pricing, CSV import.
- ✅ **Frontend** — Dashboard, instrument detail, transactions UI (symbol autocomplete, custom date range, CSV import), live stock ticker, responsive layout.
- ✅ **AI Chatbot** — floating widget answering portfolio questions over live data, Groq-backed for open-ended queries.
- ✅ **Testing & Coverage** — JUnit/Mockito backend suite, Vitest/RTL frontend suite, JaCoCo + v8 coverage reports uploaded from CI.
- ✅ **Docs** — Swagger UI (`/swagger-ui.html`), `docs/API.md`, `docs/ARCHITECTURE.md`, `docs/SECURITY.md`, `docs/COMPLIANCE.md`.

## Quickstart

```bash
cp .env.example .env
# edit .env and set FINNHUB_API_KEY and TWELVEDATA_API_KEY (both have free tiers)
docker compose up --build
```

| Service  | URL                                    |
|----------|-----------------------------------------|
| Frontend | http://localhost:18173                  |
| Backend  | http://localhost:18080                  |
| Swagger  | http://localhost:18080/swagger-ui.html  |
| MySQL    | localhost:33061                         |

Host ports are configurable via `DB_PORT`, `BACKEND_PORT`, `FRONTEND_PORT` in
`.env` — defaults were picked away from common dev ports (`3306`/`5173`/`8080`)
to avoid clashing with other services on a shared box (e.g. an EC2 dev VM).
When the frontend is reached from a different host (not `localhost`), update
`CORS_ALLOWED_ORIGINS` to include that origin, e.g.
`http://<ec2-host>:${FRONTEND_PORT}`.

The app starts with an empty portfolio. Add a transaction (or import a CSV) for
a symbol and, with `TWELVEDATA_API_KEY` set, `HistoricalDataLoader` fetches real
daily price history for it automatically — no seed data required.

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
| `TWELVEDATA_API_KEY`           | *(empty)*                        | Twelve Data API key — required for price-history/risk charts on any symbol not already backfilled |
| `DB_PORT`                      | `33061`                          | Host port mapped to MySQL's `3306` in the container  |
| `BACKEND_PORT`                 | `18080`                          | Host port mapped to the backend's `8080` in the container |
| `FRONTEND_PORT`                | `18173`                          | Host port mapped to the frontend's `80` in the container |
| `CORS_ALLOWED_ORIGINS`         | `http://localhost:18173`         | Comma-separated origins allowed to call the API directly (must include the host/port the frontend is actually served from) |
| `VITE_GROQ_API_KEY`            | *(empty)*                        | Groq API key baked into the frontend build for the AI chatbot's LLM fallback |

## Development

**Backend** (Java 17, Maven):
```bash
cd backend
mvn test             # JUnit + Mockito, JaCoCo coverage report at target/site/jacoco/index.html
mvn spring-boot:run
```

**Frontend** (Node 20, Vite):
```bash
cd frontend
npm install
npm run dev            # http://localhost:5173, proxies /api to localhost:8080
npm run test:coverage  # Vitest + RTL, v8 coverage report at coverage/index.html
```

See [backend/README.md](backend/README.md) and [frontend/README.md](frontend/README.md)
for full details, and [docs/SECURITY.md](docs/SECURITY.md) /
[docs/COMPLIANCE.md](docs/COMPLIANCE.md) for the current security/compliance posture.

## Disclaimer

Risk metrics and recommendations are rule-based and computed from historical
prices only. Educational use only — not financial advice.