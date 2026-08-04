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
- ⬜ API layer (`src/api/*`) + `useApi` data-fetching hook
- ⬜ Dashboard page (KPIs, allocation donut, performance chart, holdings/transactions tables)
- ⬜ Instrument detail page (price chart, profile, risk panel, news)
- ⬜ Transactions page (add form, history table)
- ⬜ Responsive polish pass