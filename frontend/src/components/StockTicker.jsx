import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getQuote } from '../api/market'
import { formatCurrency, formatPercent } from '../utils/format'
import './StockTicker.css'

const SYMBOLS = [
  'AAPL', 'MSFT', 'GOOGL', 'AMZN', 'TSLA', 'META', 'NVDA', 'NFLX',
  'JPM', 'V', 'DIS', 'KO', 'PEP', 'WMT', 'BA',
]
const REFRESH_MS = 30_000 // matches the backend's quote cache TTL

export default function StockTicker() {
  const [quotes, setQuotes] = useState({})

  useEffect(() => {
    let cancelled = false

    const load = () => {
      SYMBOLS.forEach((symbol) => {
        getQuote(symbol)
          .then((q) => {
            if (!cancelled) setQuotes((prev) => ({ ...prev, [symbol]: q }))
          })
          .catch(() => {}) // one symbol failing shouldn't blank the rest of the ticker
      })
    }

    load()
    const id = setInterval(load, REFRESH_MS)
    return () => {
      cancelled = true
      clearInterval(id)
    }
  }, [])

  const items = SYMBOLS.map((symbol) => ({ symbol, quote: quotes[symbol] }))
  // Duplicated so the CSS marquee can loop seamlessly at -50%.
  const track = [...items, ...items]

  return (
    <div className="stock-ticker" aria-label="Live stock ticker">
      <div className="stock-ticker-track">
        {track.map(({ symbol, quote }, i) => {
          const positive = (quote?.changePercent ?? 0) >= 0
          return (
            <Link
              to={`/instruments/${symbol}`}
              className="stock-ticker-item"
              key={`${symbol}-${i}`}
              aria-hidden={i >= items.length}
              tabIndex={i >= items.length ? -1 : undefined}
            >
              <span className="stock-ticker-symbol">{symbol}</span>
              <span className="stock-ticker-price">{quote ? formatCurrency(quote.current) : '—'}</span>
              <span className={`stock-ticker-change ${positive ? 'text-positive' : 'text-negative'}`}>
                {quote ? `${positive ? '▲' : '▼'} ${formatPercent(quote.changePercent)}` : ''}
              </span>
            </Link>
          )
        })}
      </div>
    </div>
  )
}
