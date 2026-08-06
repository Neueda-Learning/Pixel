# Pixel — Backend

Spring Boot 3.2.5 / Java 17 REST API for the Pixel Portfolio Manager.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Spring Boot 3.2.5 |
| Language | Java 17 |
| Database | MySQL 8 (via Spring Data JPA / Hibernate) |
| Caching | Caffeine (in-memory, TTL-based) |
| API docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven 3 |
| Container | Docker |

## Package Structure

```
com.pixel.portfolio/
├── config/           # CORS, cache, and RestTemplate configuration
├── controller/       # REST controllers (Instrument, Market, Portfolio, Risk, Transaction, Chat)
├── dto/              # API request/response objects (never exposes JPA entities directly)
├── exception/        # Global exception handler + custom exception types
├── integration/
│   ├── finnhub/      # Raw Finnhub API response models (deserialized from Finnhub JSON)
│   └── twelvedata/   # Raw Twelve Data API response models (time_series endpoint)
├── loader/           # HistoricalDataLoader — CSV ingestion + Twelve Data backfill on startup
├── model/            # JPA entities: Instrument, PriceHistory, Transaction
├── repository/       # Spring Data JPA repositories
├── service/          # Business logic: Portfolio, Transaction, Instrument, Market, Risk, ChatBot
│   └── risk/         # RiskMath — pure statistical calculations (volatility, Sharpe, beta, drawdown)
│   TwelveDataHistoricalService # Fetches daily OHLCV history from Twelve Data for a symbol
└── util/             # PeriodUtil — maps period strings (1M, 3M, …) to start dates
```

## Running Locally (without Docker)

Prerequisites: Java 17+, Maven 3, MySQL 8 running locally.

```bash
# 1. Create the database
mysql -u root -p -e "CREATE DATABASE portfolio; CREATE USER 'portfolio'@'localhost' IDENTIFIED BY 'changeme'; GRANT ALL ON portfolio.* TO 'portfolio'@'localhost';"

# 2. (Optional) set your Finnhub API key
export FINNHUB_API_KEY=your_key_here

# 3. Set your Twelve Data API key — REQUIRED for the portfolio/instrument price
#    history charts (1M/3M/6M/1Y/ALL). Get a free key at https://twelvedata.com/.
#    Without it, charts stay empty for any portfolio symbol not already seeded.
export TWELVEDATA_API_KEY=your_key_here

# 4. Build and run
cd backend
mvn spring-boot:run
```

API available at `http://localhost:8080`. Swagger UI at `http://localhost:8080/swagger-ui.html`.

## Running with Docker

```bash
# From the repo root
cp .env.example .env   # fill in FINNHUB_API_KEY, TWELVEDATA_API_KEY, and DB passwords
docker compose up --build
```

## Configuration

All values come from environment variables (see `application.yml` and root `.env.example`):

| Property | Env var | Default |
|----------|---------|---------|
| DB URL | `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/portfolio` |
| DB user | `SPRING_DATASOURCE_USERNAME` | `portfolio` |
| DB password | `SPRING_DATASOURCE_PASSWORD` | `changeme` |
| Finnhub key | `FINNHUB_API_KEY` | *(empty — degrades gracefully)* |
| Twelve Data key | `TWELVEDATA_API_KEY` | *(empty — no price history backfill)* |
| CORS origins | `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |
| Seed CSV dir | `SEED_DIR` | `../infra/db/seed` |

> **Chart data requires `TWELVEDATA_API_KEY`.** The performance/price-history charts on the
> Dashboard and Instrument Detail pages are populated from the `price_history` table, which is
> filled either from seed CSVs or, for any portfolio symbol not covered by a CSV, from the
> [Twelve Data](https://twelvedata.com/) `time_series` endpoint. Sign up for a free API key and
> set `TWELVEDATA_API_KEY` (locally or in `.env`) or the chart will simply have no data for that
> symbol. The free tier is rate-limited to 8 requests/minute, so `HistoricalDataLoader` and
> `TransactionService` pace backfill calls 8 seconds apart.

## Historical Data Loading

On every startup, `HistoricalDataLoader` scans `SEED_DIR` for `<SYMBOL>.csv` files and
bulk-inserts price history into `price_history` (idempotent). It then derives the list of
symbols actually held in the portfolio from the `transaction` table
(`TransactionRepository.findDistinctSymbols()`) — there is no hardcoded demo/synthetic symbol
list. For any of those symbols still missing `price_history` rows, it fetches real daily OHLCV
data from the Twelve Data API via `TwelveDataHistoricalService` and persists it, so each symbol
is only ever fetched once. Adding a brand-new symbol via a transaction (`TransactionService.add`)
triggers the same on-demand backfill immediately, without waiting for a restart.

To load real data ahead of time (e.g. from Kaggle), drop CSVs into `infra/db/seed/` and restart.
Expected columns: `Date, Open, High, Low, Close, Adj Close, Volume`.

## Running Tests

```bash
cd backend
mvn test
```

The suite uses JUnit 5 + Mockito (`spring-boot-starter-test`) across four classes in
`src/test/java/com/pixel/portfolio/`:

| Test class | Location | Covers |
|------------|----------|--------|
| `PortfolioServiceTest` | `service/` | Average-cost calculations, buy/sell position tracking, and holdings summary aggregation |
| `TransactionServiceTest` | `service/` | Transaction CRUD, lot-based SELL pricing (`buyTransactionId`/`buyPrice` resolution), FIFO fallback for unassigned sells, open-lot remaining-quantity tracking, CSV/batch import ordering, and validation errors (over-selling, invalid lot references, missing lot/price) |
| `RiskMathTest` | `service/risk/` | Pure statistical calculations — volatility, Sharpe ratio, beta, max drawdown |
| `ChatBotServiceTest` | `service/` | Intent matching and reply formatting against real `PortfolioService`/`RiskService` data |

`TransactionServiceTest` mocks `TransactionRepository`, `InstrumentRepository`,
`PriceHistoryRepository`, and `TwelveDataHistoricalService` with Mockito
(`@ExtendWith(MockitoExtension.class)`, `@Mock`/`@InjectMocks`) so it runs without a database.

CI (`.github/workflows/ci.yml`) runs `mvn -B test` on every push to `main` and on every pull
request, uploading the Surefire reports as a build artifact.

## Key Design Decisions

- **Holdings are derived, not stored.** The `Transaction` ledger is the source of truth;
  holdings are computed on every request using the average-cost method.
- **Finnhub is best-effort.** All market data calls fall back to the `price_history` DB
  table if Finnhub is unavailable or the API key is not set. The app never hard-fails
  on a missing external dependency.
- **Historical data is portfolio-scoped, not hardcoded.** `HistoricalDataLoader` derives which
  symbols need chart data from the `transaction` table, and backfills only those from Twelve
  Data — no fixed demo symbol list, no synthetically generated prices.
- **Caching is server-side.** Finnhub quote/profile/news responses are cached with
  Caffeine to respect rate limits. Cache TTLs are configured in `CacheConfig`.
- **No holdings table.** Avoids sync issues between transactions and a derived holdings
  snapshot; all portfolio math is deterministic from the ledger.
- **The chat assistant is rule-based, not an LLM.** `ChatBotService` matches user
  messages against a fixed set of keyword/threshold heuristics and answers using the
  same `PortfolioService`/`RiskService` methods the REST API already exposes — zero
  hallucination risk, no external API key, no cost per query. See
  [docs/ARCHITECTURE.md#chat-assistant--ai-integration](../docs/ARCHITECTURE.md#chat-assistant--ai-integration)
  for the full rationale and LLM upgrade path.
