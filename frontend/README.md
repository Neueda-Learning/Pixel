# Pixel — Frontend

React (Vite) frontend for the Pixel Portfolio Manager. Talks to the Spring Boot
backend at `/api/*` (proxied to `http://localhost:8080` in dev, to the `backend`
container via nginx in production).

## Stack

- React 18 + React Router 7
- Vite 5
- Recharts (charts)
- Axios (API client)

## Development

```bash
npm install
npm run dev        # http://localhost:5173, proxies /api to localhost:8080
npm run build       # production build to dist/
```

## Testing

The frontend uses [Vitest](https://vitest.dev/) with `jsdom` and
[React Testing Library](https://testing-library.com/docs/react-testing-library/intro/) for
component tests.

```bash
npm run test           # run the full suite once (used by CI)
npm run test:watch     # watch mode for local development
npm run test:coverage  # run the suite with a v8 coverage report
```

### Coverage

`npm run test:coverage` uses [`@vitest/coverage-v8`](https://vitest.dev/guide/coverage.html) to
produce:

- a text summary printed to the terminal (lines/branches/functions/statements %)
- an HTML report at `frontend/coverage/index.html` for browsing per-file line/branch hits

Current coverage is ~43% (driven by the four test files above; components/pages without a
`*.test.jsx` file show as uncovered). We'll cover more components/pages in future releases. The
`coverage/` folder is generated output — it's listed in the root `.gitignore` and never
committed. CI runs `npm run test:coverage` on every push/PR and uploads `frontend/coverage/` as
the `frontend-coverage-report` build artifact so reviewers can
download and browse it without it living in git.

Test files live alongside the code they cover (`*.test.js` / `*.test.jsx`):

| Test file | Covers |
|-----------|--------|
| `src/utils/format.test.js` | Pure formatter functions — currency, percent, ratio, number, and date formatting, including the `'—'` placeholder for null/undefined input |
| `src/components/TransactionForm.test.jsx` | Add-transaction form: BUY vs SELL field visibility, fetching/listing open lots for a symbol, validation (missing lot, quantity exceeding remaining lot quantity), and the submitted payload shape for both BUY and SELL |
| `src/components/EditTransactionModal.test.jsx` | Edit-transaction modal: pre-filling values from the transaction being edited, fetching open lots excluding the transaction itself, lot-based quantity validation, and the submitted payload/id on save |
| `src/components/CsvImportModal.test.jsx` | CSV import: header validation, per-row validation (invalid type, missing/invalid `buyPrice` on SELL rows), the valid-row count, and submitting only the valid parsed rows to the import API |

External calls (`src/api/*`) are mocked with `vi.mock` so tests run fully offline. CI
(`.github/workflows/ci.yml`) runs `npm run test` on every push to `main` and on every pull request.

## AI Chatbot

A floating AI chat widget (`src/chatbot/`, mounted via `ChatWidget` in `Layout.jsx` so it's
available on every page) answers portfolio questions using live account data — portfolio summary,
top holdings, risk analysis, market news, and rebalance suggestions — pulled from the existing
`portfolio`/`risk`/`market` API modules (`chatbot/services/chatDataService.js`; no new backend
endpoints were added). For open-ended questions, `chatDataService.js`'s `answerGeneralQuestion()`
calls the [Groq](https://groq.com/) chat completions API (`https://api.groq.com/openai/v1/chat/completions`,
model `llama-3.1-8b-instant`) directly from the browser, passing a system prompt ("Pixel AI, an
educational portfolio assistant") plus a live portfolio snapshot (total value, gain/loss, top
holdings) as context.

Other chatbot behavior: localStorage persistence of messages/session state
(`chatbot/storage.js`), max message length + empty-input guardrails, quick-reply starter chips,
and "yes/more details" follow-ups that expand on the last answer. See
[src/chatbot/README.md](src/chatbot/README.md) for the full module guide, and
`chatEngine.test.js` / `intentMatcher.test.js` for test coverage.

**Environment variable:** `VITE_GROQ_API_KEY` (set in the root `.env`, passed through as a Docker
build arg in `docker-compose.yml`/`frontend/Dockerfile`). Since it's a `VITE_`-prefixed var, it's
baked into the client bundle at build time — the Groq key is visible to anyone who inspects the
built frontend.

## Progress

- ✅ Project scaffold: Vite + React Router, design tokens (`styles/theme.css`,
  `styles/base.css`), routing skeleton (`/`, `/transactions`,
  `/instruments/:symbol`) with placeholder pages.
- ✅ Layout, Sidebar, Topbar — responsive app shell with a collapsible mobile
  nav (breakpoint at 900px).
- ✅ API layer (`src/api/client.js` + instruments/market/portfolio/risk/transactions
  modules) and `useApi` data-fetching hook (`{data, error, loading, reload}`),
  wired directly against the live backend.
- ✅ Dashboard page — KPI cards, allocation donut, portfolio performance chart,
  holdings table, recent transactions table.
- ✅ Instrument detail page — company profile/quote header, price history
  chart, risk panel (8 stat tiles + BUY/HOLD/AVOID recommendation), news feed.
- ✅ Transactions page — validated add-transaction form (buy/sell, qty, price,
  fees, date) and a deletable, period-filterable history table.
- ✅ Responsive polish pass — verified breakpoints for mobile nav (900px),
  KPI grid (800px), dashboard chart layout (1000px), and risk tile grid (700px).
- ✅ Gain/loss visual treatment — `KpiCard` "Total gain / loss" value and the
  `HoldingsTable` gain/loss column now render in green with a `▲` arrow for
  profit and red with a `▼` arrow for loss (using the existing `--positive` /
  `--negative` theme tokens), matching the pattern already used for quote
  deltas and BUY/SELL labels elsewhere in the app.
- ✅ Chart gain/loss styling — `TimeSeriesChart` (used by both the Dashboard
  performance chart and the Instrument Detail price history chart) now renders
  as a filled area chart that auto-colors green with a soft green gradient
  shadow when the period is up, or red with a red gradient shadow when the
  period is down (Google Finance style), based on comparing the first and
  last values in the series.
- ✅ Chart year-axis ticks — `TimeSeriesChart`'s x-axis switches from
  "Jan 5"-style ticks to year-only ticks once the plotted data spans more
  than ~1 year (e.g. the "ALL"/max period), so long ranges don't show
  repetitive day-level labels.
- ✅ Live stock ticker — `StockTicker`, a scrolling marquee of 15 well-known
  symbols (AAPL, MSFT, GOOGL, AMZN, TSLA, META, NVDA, NFLX, JPM, V, DIS, KO,
  PEP, WMT, BA) with live price + % change, polling the existing
  `/api/market/quote/{symbol}` endpoint (Finnhub-backed, server-cached) every
  30s. Each ticker item links to that symbol's Instrument Detail page. Shown
  above the `Topbar` on every page via `Layout`.
- ✅ Persistent footer — `Footer`, a fixed copyright bar shown on every page
  via `Layout`.
- ✅ AI chatbot — floating `ChatWidget` answering portfolio questions over
  live data, backed by Groq AI for open-ended questions, mounted globally via
  `Layout`. See the "AI Chatbot" section above.

> **Note:** Chart data depends on the backend having a `TWELVEDATA_API_KEY`
> configured (see [backend/README.md](../backend/README.md#configuration)).
> Without it, the performance/price-history charts render empty for any
> portfolio symbol that isn't already seeded with historical data.

## Feature summary

The frontend is feature-complete against the backend API contract:

- **Dashboard** (`/`) — portfolio KPIs, allocation donut, performance chart
  (period toggle), holdings table, and the 5 most recent transactions.
- **Instrument detail** (`/instruments/:symbol`) — company profile & live
  quote, price history chart (period toggle), rule-based risk panel, and
  recent news.
- **Transactions** (`/transactions`) — add buy/sell transactions with
  client-side validation, and a period-filterable, deletable history table.
- Responsive app shell (collapsible mobile sidebar, live stock ticker header,
  persistent footer) and shared design tokens for consistent theming across
  all pages.
- **AI chatbot** — floating widget answering portfolio questions over live
  data, backed by Groq AI (see "AI Chatbot" above).