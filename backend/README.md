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
├── controller/       # REST controllers (Instrument, Market, Portfolio, Risk, Transaction)
├── dto/              # API request/response objects (never exposes JPA entities directly)
├── exception/        # Global exception handler + custom exception types
├── integration/
│   └── finnhub/      # Raw Finnhub API response models (deserialized from Finnhub JSON)
├── loader/           # HistoricalDataLoader — CSV ingestion + synthetic seed on startup
├── model/            # JPA entities: Instrument, PriceHistory, Transaction
├── repository/       # Spring Data JPA repositories
├── service/          # Business logic: Portfolio, Transaction, Instrument, Market, Risk
│   └── risk/         # RiskMath — pure statistical calculations (volatility, Sharpe, beta, drawdown)
└── util/             # PeriodUtil — maps period strings (1M, 3M, …) to start dates
```

## Running Locally (without Docker)

Prerequisites: Java 17+, Maven 3, MySQL 8 running locally.

```bash
# 1. Create the database
mysql -u root -p -e "CREATE DATABASE portfolio; CREATE USER 'portfolio'@'localhost' IDENTIFIED BY 'changeme'; GRANT ALL ON portfolio.* TO 'portfolio'@'localhost';"

# 2. (Optional) set your Finnhub API key
export FINNHUB_API_KEY=your_key_here

# 3. Build and run
cd backend
mvn spring-boot:run
```

API available at `http://localhost:8080`. Swagger UI at `http://localhost:8080/swagger-ui.html`.

## Running with Docker

```bash
# From the repo root
cp .env.example .env   # fill in FINNHUB_API_KEY and DB passwords
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
| CORS origins | `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |
| Seed CSV dir | `SEED_DIR` | `../infra/db/seed` |

## Historical Data Loading

On every startup, `HistoricalDataLoader` scans `SEED_DIR` for `<SYMBOL>.csv` files and
bulk-inserts price history into `price_history` (idempotent). Any of the 25 known demo
symbols that have no data yet get ~2 years of synthetic daily prices generated automatically,
so the app is never empty.

To load real data (e.g. from Kaggle), drop CSVs into `infra/db/seed/` and restart.
Expected columns: `Date, Open, High, Low, Close, Adj Close, Volume`.

## Running Tests

```bash
cd backend
mvn test
```

Tests cover `PortfolioService` — average-cost calculations, buy/sell position tracking,
and summary aggregation.

## Key Design Decisions

- **Holdings are derived, not stored.** The `Transaction` ledger is the source of truth;
  holdings are computed on every request using the average-cost method.
- **Finnhub is best-effort.** All market data calls fall back to the `price_history` DB
  table if Finnhub is unavailable or the API key is not set. The app never hard-fails
  on a missing external dependency.
- **Caching is server-side.** Finnhub quote/profile/news responses are cached with
  Caffeine to respect rate limits. Cache TTLs are configured in `CacheConfig`.
- **No holdings table.** Avoids sync issues between transactions and a derived holdings
  snapshot; all portfolio math is deterministic from the ledger.
