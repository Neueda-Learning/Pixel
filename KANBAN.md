# Kanban Board — Pixel Portfolio Manager

Track what has shipped in **v1.0.0**, what shipped in **v2.0.0**, and what is planned for future
releases.

---

## ✅ Done — v2.0.0 (cut from `develop`)

### Epic 6 — Transactions Module Revamp
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 36 | Symbol autocomplete, live price indicator, CSV import, edit transaction, custom date range | `ff426be` | 2026-08-06 |
| 37 | CSV import modal — drag-and-drop, sample download, icon actions | `debe6f3` | 2026-08-06 |
| 38 | Reject SELL orders that exceed current holdings | `2cc23f5` | 2026-08-06 |
| 39 | Manual buy/sell pricing with dates and lot-based FIFO sell picker | `417c3be` | 2026-08-06 |
| 40 | Unit tests — `TransactionServiceTest` (lot pricing, FIFO fallback, import ordering) | `a9c4cc9` | 2026-08-06 |

### Epic 7 — Live Data & UI Polish
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 41 | Portfolio-scoped historical data loading (Twelve Data backfill, no synthetic/demo data) | `8ba5ec8` | 2026-08-05 |
| 42 | Gain/loss color-coded arrows across KPI cards and holdings table | `9da6d35` | 2026-08-05 |
| 43 | Persistent footer, live stock ticker header, chart year-axis ticks | `67a4bd9` | 2026-08-05 |
| 44 | Dashboard fixes — whole-number qty, top-holdings KPI list, scroll-to-top on route change | `28e3272` | 2026-08-06 |
| 45 | Fix stock ticker items being unclickable (broken instrument-detail navigation) | `561a837` | 2026-08-06 |

### Epic 8 — AI Chatbot
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 46 | AI chatbot widget (`frontend/src/chatbot/`) — rule-based intents + Groq LLM fallback, live portfolio/risk/market data context | `0d8bbf6` | 2026-08-06 |
| 47 | Holdings CSV export and transactions/holdings table pagination | `0d8bbf6` | 2026-08-06 |
| 48 | Chatbot documented in frontend/backend READMEs | `d439613` | 2026-08-06 |

### Epic 9 — Testing, Coverage & CI
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 49 | Explicit CI test stage running on every pull request (not just `main`) | `b874bbb` | 2026-08-06 |
| 50 | Full backend + frontend test suite (`TransactionServiceTest`, Vitest/RTL component tests) | `a9c4cc9` | 2026-08-06 |
| 51 | jsdom pinned to 26.1.0 for Node 20 CI compatibility | `5c16800` | 2026-08-06 |
| 52 | JaCoCo (backend) + `@vitest/coverage-v8` (frontend) coverage tooling, uploaded as CI artifacts | `491ded7` | 2026-08-06 |

### Epic 10 — Security & Compliance Documentation
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 53 | `docs/COMPLIANCE.md` — US market regulatory/non-functional roadmap | `230400a` | 2026-08-06 |
| 54 | `docs/SECURITY.md` — OWASP Top 10 threat map + required PR security notes | `71c080e` | 2026-08-06 |
| 55 | Root `.env.example` with every variable actually read by `docker-compose.yml` | `64283e9` | 2026-08-06 |

---

## ✅ Done — v1.0.0 (released 2026-08-05)

### Epic 1 — Foundation & Infrastructure
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 1 | `Instrument` + `PriceHistory` JPA entities and repositories | `e0e8714` | 2026-08-04 |
| 2 | `HistoricalDataLoader` — CSV ingestion + synthetic 2-year seed | `b076cfc` | 2026-08-04 |
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

### Epic 5 — Documentation
| # | Feature | Commit | Date |
|---|---------|--------|------|
| 30 | `docs/API.md` — full endpoint reference with request/response examples | `0543acd` | 2026-08-05 |
| 31 | `docs/ARCHITECTURE.md` — component diagram, data flows, design decisions | `0543acd` | 2026-08-05 |
| 32 | `backend/README.md` — package structure, local setup, config, design notes | *(this release)* | 2026-08-05 |
| 33 | `frontend/README.md` — feature summary, dev/build commands, stack | `def74e2` | 2026-08-04 |
| 34 | Root `README.md` — quickstart, env vars, data loading guide | `05972a3` | 2026-08-04 |
| 35 | `KANBAN.md` — this file | *(this release)* | 2026-08-05 |

---

## 🔮 Backlog — Planned for Future Releases

| # | Feature | Priority | Notes |
|---|---------|----------|-------|
| 56 | User authentication (JWT / Spring Security) | High | Multi-user portfolios; currently single-user, no auth on any endpoint (see `docs/SECURITY.md` A01/A07) |
| 57 | TLS/HTTPS termination + encryption at rest | High | Backend and nginx both serve plain HTTP today; see `docs/SECURITY.md` A02, `docs/COMPLIANCE.md` |
| 58 | Portfolio benchmarking vs SPY | High | Show alpha vs index on performance chart |
| 59 | Dependency vulnerability scanning in CI | Medium | `npm audit` / OWASP Dependency-Check Maven plugin; see `docs/SECURITY.md` A06 |
| 60 | Dividend tracking and yield calculation | Medium | Add `DIVIDEND` tx type + income metrics |
| 61 | Watchlist — track symbols without holding them | Medium | Separate from portfolio holdings |
| 62 | Price alerts (WebSocket or polling) | Medium | Notify when price crosses threshold |
| 63 | Transaction ledger CSV export | Medium | Holdings CSV export shipped in v2.0.0 (`exportHoldingsCsv`); transactions history still lacks an export button |
| 64 | Audit logging for transaction mutations | Medium | See `docs/SECURITY.md` A09 |
| 65 | Dark / light theme toggle | Low | Design tokens already in place |
| 66 | Portfolio comparison (multiple portfolios) | Low | Requires auth first |
| 67 | Migrate `ddl-auto: update` → Flyway migrations | Low | Safer schema management for production |
| 68 | E2E tests (Playwright) | Low | Cover dashboard, add transaction, detail flows |
