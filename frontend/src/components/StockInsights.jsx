import { useEffect, useState } from 'react'
import TimeSeriesChart from './TimeSeriesChart'
import { getInstrumentPrices } from '../api/instruments'
import { getRisk } from '../api/risk'
import { formatCurrency, formatPercent } from '../utils/format'
import './StockInsights.css'

// Simple, transparent thresholds on annualized volatility for a plain-English risk label.
function riskLevel(annualizedVolatility) {
  if (annualizedVolatility == null) return null
  if (annualizedVolatility < 0.2) return { label: 'Low', cls: 'badge-good' }
  if (annualizedVolatility < 0.4) return { label: 'Medium', cls: 'badge-warn' }
  return { label: 'High', cls: 'badge-bad' }
}

export default function StockInsights({ symbol, quote }) {
  const [prices, setPrices] = useState(null)
  const [risk, setRisk] = useState(null)

  useEffect(() => {
    if (!symbol) return
    let cancelled = false
    getInstrumentPrices(symbol, '3M')
      .then((data) => !cancelled && setPrices(data))
      .catch(() => !cancelled && setPrices([]))
    getRisk(symbol)
      .then((data) => !cancelled && setRisk(data))
      .catch(() => !cancelled && setRisk(null))
    return () => {
      cancelled = true
    }
  }, [symbol])

  if (!symbol) return null

  const risk_ = riskLevel(risk?.annualizedVolatility)
  const change = quote?.changePercent

  return (
    <div className="stock-insights">
      <div className="stock-insights-header">
        <span className="stock-insights-title">{symbol} insights</span>
        {risk_ && (
          <span className={`badge ${risk_.cls}`}>
            <span className="badge-dot" />
            Risk: {risk_.label}
          </span>
        )}
      </div>

      <div className="stock-insights-kpis">
        <div className="stock-insights-kpi">
          <span className="text-muted">Current price</span>
          <span className="tabular" style={{ fontWeight: 600 }}>
            {quote ? formatCurrency(quote.current) : '—'}
          </span>
        </div>
        <div className="stock-insights-kpi">
          <span className="text-muted">Daily change</span>
          <span className={`tabular ${change > 0 ? 'text-positive' : change < 0 ? 'text-negative' : ''}`} style={{ fontWeight: 600 }}>
            {change != null ? formatPercent(change, { signed: true }) : '—'}
          </span>
        </div>
      </div>

      {prices && prices.length > 1 ? (
        <TimeSeriesChart data={prices} xKey="date" yKey="close" height={140} valueFormatter={formatCurrency} />
      ) : (
        <p className="text-muted" style={{ fontSize: 12.5 }}>No chart data available yet.</p>
      )}
    </div>
  )
}
