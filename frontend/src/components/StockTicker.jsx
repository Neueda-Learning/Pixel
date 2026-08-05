import { useEffect, useState, useRef } from 'react'
import { getQuote } from '../api/market'
import './StockTicker.css'

// Matches the symbols available in the transaction form
const TICKER_SYMBOLS = [
  'AAPL', 'MSFT', 'GOOGL', 'AMZN', 'TSLA', 'META', 'NVDA', 'JPM', 'V', 'NFLX',
  'JNJ', 'XOM', 'MA', 'PG', 'BAC',
  'SPY', 'QQQ', 'VTI', 'VOO', 'GLD',
]

export default function StockTicker() {
  const [quotes, setQuotes] = useState([])
  const intervalRef = useRef(null)

  const fetchQuotes = async () => {
    const results = await Promise.allSettled(TICKER_SYMBOLS.map((s) => getQuote(s).then((q) => ({ symbol: s, ...q }))))
    const data = results
      .filter((r) => r.status === 'fulfilled' && r.value?.currentPrice)
      .map((r) => r.value)
    if (data.length > 0) setQuotes(data)
  }

  useEffect(() => {
    fetchQuotes()
    intervalRef.current = setInterval(fetchQuotes, 30000)
    return () => clearInterval(intervalRef.current)
  }, [])

  if (quotes.length === 0) return null

  const items = [...quotes, ...quotes] // duplicate for seamless loop

  return (
    <div className="stock-ticker" aria-label="Live stock prices">
      <div className="ticker-label">LIVE</div>
      <div className="ticker-track">
        <div className="ticker-inner">
          {items.map((q, i) => {
            const change = q.changePercent ?? 0
            const positive = change >= 0
            return (
              <span key={i} className={`ticker-item ${positive ? 'pos' : 'neg'}`}>
                <span className="ticker-symbol">{q.symbol}</span>
                <span className="ticker-price">${Number(q.currentPrice).toFixed(2)}</span>
                <span className="ticker-change">
                  {positive ? '▲' : '▼'} {Math.abs(change).toFixed(2)}%
                </span>
              </span>
            )
          })}
        </div>
      </div>
    </div>
  )
}
