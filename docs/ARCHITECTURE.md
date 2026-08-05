# Architecture

React (Vite) ── HTTP/JSON ──> Spring Boot REST API ──> MySQL
                                      |
                                      +-- MarketDataService (Finnhub, cached, DB fallback)
                                      +-- RiskService (metrics + BUY/HOLD/AVOID)

Data sources:
- price_history table  <- bulk-loaded CSV / synthetic seed (backbone; works offline)
- MarketDataService    <- Finnhub API (live, best-effort, degrades gracefully to DB)
