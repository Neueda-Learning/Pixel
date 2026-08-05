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