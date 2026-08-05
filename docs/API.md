# API Contract

See [README.md](../README.md) for the full summary, and `/swagger-ui.html` (backend running) for
interactive, always-current docs generated from the code.

```
GET    /api/instruments                                list known instruments
GET    /api/instruments/{symbol}/prices?period=1M|3M|6M|1Y|ALL   price series (chart)

GET    /api/portfolio                                  current holdings + valuation
GET    /api/portfolio/summary                          totals + allocation by asset type
GET    /api/portfolio/performance?period=1M|3M|6M|1Y|ALL   portfolio value over time (chart)

GET    /api/market/quote/{symbol}                       live quote (Finnhub, cached, DB fallback)
GET    /api/market/profile/{symbol}                      company profile (Finnhub, cached, DB fallback)
GET    /api/market/news/{symbol}                         company news (Finnhub, cached)
GET    /api/market/search?q=                             symbol/company search

GET    /api/risk/{symbol}                                risk metrics + BUY/HOLD/AVOID recommendation

GET    /api/transactions?period=3M|6M|1Y|ALL             list history
POST   /api/transactions                                 add buy/sell (validated)
DELETE /api/transactions/{id}                             remove
```

AI chatbot / LLM features are out of scope for this project.
