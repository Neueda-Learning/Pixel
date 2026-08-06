# Kanban Board — Pixel Portfolio Manager

Track what has shipped in **v1.0.0** and what is planned for future releases.

---

## ✅ Done — v1.0.0 (released 2026-08-05)

### Epic 1 — Foundation & Infrastructure
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 1 | `Instrument` + `PriceHistory` JPA entities and repositories | `e0e8714` | 2026-08-04 |
| 2 | `HistoricalDataLoader` — CSV ingestion + on-demand Twelve Data backfill (portfolio-scoped, no synthetic data) | `b076cfc` | 2026-08-04 |
| 3 | Global exception handler (`GlobalExceptionHandler`) + `PeriodUtil` | `3b60fca` | 2026-08-04 |
| 4 | CORS config + `application.yml` with env-var-driven properties | `b0f4bdc` | 2026-08-04 |
| 5 | Dockerfiles (backend + frontend), `.env.example`, `.gitignore` | `a72342b` | 2026-08-04 |
| 6 | GitHub Actions CI — backend `mvn test` + frontend `npm build` | `aa8ae93` | 2026-08-04 |
| 7 | `docker-compose.yml` wiring db + backend + frontend | `a72342b` | 2026-08-04 |

### Epic 2 — Market Data & Risk Engine
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 8 | Finnhub integration — `FinnhubMarketDataService` (quote, profile, news, search) | `b950e81` | 2026-08-04 |
| 9 | Caffeine cache for Finnhub responses (`CacheConfig`) | `b950e81` | 2026-08-04 |
| 10 | Graceful DB fallback when Finnhub is unavailable or key is unset | `b950e81` | 2026-08-04 |
| 11 | `RiskService` + `RiskMath` — annualised volatility, Sharpe, max drawdown, beta | *(merged in PR #3)* | 2026-08-04 |
| 12 | BUY / HOLD / AVOID recommendation engine | *(merged in PR #3)* | 2026-08-04 |
| 13 | `GET /api/risk/{symbol}` endpoint | *(merged in PR #3)* | 2026-08-04 |
| 14 | `GET /api/market/quote|profile|news|search` endpoints | *(merged in PR #3)* | 2026-08-04 |

### Epic 3 — Portfolio & Transactions
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 15 | `Transaction` entity, repository, validated request/response DTOs | `0668cfd` | 2026-08-04 |
| 16 | `TransactionService` + `GET/POST/DELETE /api/transactions` | `9a3345f` | 2026-08-04 |
| 17 | `InstrumentService` + `GET /api/instruments` and `GET /api/instruments/{symbol}/prices` | `3d363fc` | 2026-08-04 |
| 18 | Average-cost holdings derivation in `PortfolioService` | `fa5c869` | 2026-08-04 |
| 19 | `GET /api/portfolio/summary` — totals + allocation by asset type | `4d66ba1` | 2026-08-04 |
| 20 | `GET /api/portfolio/performance` — historical mark-to-market chart data | `af81655` | 2026-08-04 |
| 21 | Unit tests — `PortfolioServiceTest` (buy/sell/summary) | `a2ce72a` | 2026-08-04 |
| 22 | Swagger annotations on Transaction, Portfolio, and Instrument controllers | `71407dc` | 2026-08-04 |

### Epic 4 — React Frontend
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 23 | Vite + React Router scaffold, design tokens (`theme.css`, `base.css`) | `58ad8d0` | 2026-08-04 |
| 24 | Responsive app shell — `Layout`, collapsible `Sidebar`, `Topbar` | `f22099f` | 2026-08-04 |
| 25 | Axios API layer (`src/api/`) + `useApi` data-fetching hook | `159d5d4` | 2026-08-04 |
| 26 | Dashboard page — KPI cards, allocation donut, performance chart, holdings table | `13f7d45` | 2026-08-04 |
| 27 | Instrument detail page — profile/quote header, price chart, risk panel, news feed | `fd4d71f` | 2026-08-04 |
| 28 | Transactions page — validated add-transaction form + deletable history table | `86f153e` | 2026-08-04 |
| 29 | Responsive polish — mobile nav (900 px), KPI grid (800 px), risk tiles (700 px) | `41ec316` | 2026-08-04 |

### Epic 5 — AI Integration
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 30 | `ChatBotService` — deterministic, rule-based NLP intent matcher over `PortfolioService`/`RiskService` | *(this release)* | 2026-08-06 |
| 31 | `POST /api/chat` — `ChatController` + `ChatRequestDto`/`ChatResponseDto` | *(this release)* | 2026-08-06 |
| 32 | `ChatBotServiceTest` — unit coverage for intent matching and reply formatting | *(this release)* | 2026-08-06 |
| 33 | `ChatPanel` — floating chat widget wired into `Layout`, `src/api/chat.js` | *(this release)* | 2026-08-06 |

### Epic 6 — Documentation
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 34 | `docs/API.md` — full endpoint reference with request/response examples | `0543acd` | 2026-08-05 |
| 35 | `docs/ARCHITECTURE.md` — component diagram, sequence diagrams, chat design rationale | `0543acd` | 2026-08-06 |
| 36 | `backend/README.md` — package structure, local setup, config, design notes | *(this release)* | 2026-08-06 |
| 37 | `frontend/README.md` — feature summary, dev/build commands, stack | `def74e2` | 2026-08-06 |
| 38 | Root `README.md` — quickstart, env vars, architecture diagram, AI chat assistant section | `05972a3` | 2026-08-06 |
| 39 | `KANBAN.md` — this file | *(this release)* | 2026-08-06 |

---

## 🔮 Backlog — Planned for Future Releases

| # | Feature | Priority | Notes |
|---|---------|----------|-------|
| 40 | User authentication (JWT / Spring Security) | High | Multi-user portfolios; currently single-user |
| 41 | Portfolio benchmarking vs SPY | High | Show alpha vs index on performance chart |
| 42 | Dividend tracking and yield calculation | Medium | Add `DIVIDEND` tx type + income metrics |
| 43 | Watchlist — track symbols without holding them | Medium | Separate from portfolio holdings |
| 44 | Price alerts (WebSocket or polling) | Medium | Notify when price crosses threshold |
| 45 | CSV export — transactions and holdings | Medium | Download ledger as CSV |
| 46 | LLM-backed chat upgrade (LangChain4j + tool calling) | Medium | Broader NL coverage; keeps answers grounded in real service data |
| 47 | News sentiment analysis (RAG over embedded articles) | Medium | First feature that would actually justify a RAG pipeline |
| 48 | Dark / light theme toggle | Low | Design tokens already in place |
| 49 | Portfolio comparison (multiple portfolios) | Low | Requires auth first |
| 50 | Migrate `ddl-auto: update` → Flyway migrations | Low | Safer schema management for production |
| 51 | E2E tests (Playwright) | Low | Cover dashboard, add transaction, detail flows |
