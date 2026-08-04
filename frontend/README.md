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
- ⬜ Transactions page (add form, history table)
- ⬜ Responsive polish pass