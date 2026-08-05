import { Link } from 'react-router-dom'
import { formatCurrency, formatNumber, formatPercent } from '../utils/format'
import './DataTable.css'

export default function HoldingsTable({ holdings }) {
  if (!holdings || holdings.length === 0) {
    return <p className="text-muted">No holdings yet — add a transaction to get started.</p>
  }

  return (
    <div className="scroll-x">
      <table className="data-table">
        <thead>
          <tr>
            <th>Symbol</th>
            <th>Name</th>
            <th className="num">Qty</th>
            <th className="num">Avg cost</th>
            <th className="num">Price</th>
            <th className="num">Market value</th>
            <th className="num">Gain/loss</th>
          </tr>
        </thead>
        <tbody>
          {holdings.map((h) => {
            const positive = h.gainLoss >= 0
            return (
              <tr key={h.symbol}>
                <td>
                  <Link to={`/instruments/${h.symbol}`} className="symbol-link">
                    {h.symbol}
                  </Link>
                </td>
                <td className="text-secondary">{h.name}</td>
                <td className="num tabular">{formatNumber(h.quantity, 0)}</td>
                <td className="num tabular">{formatCurrency(h.avgCost)}</td>
                <td className="num tabular">{formatCurrency(h.currentPrice)}</td>
                <td className="num tabular">{formatCurrency(h.marketValue)}</td>
                <td className="num tabular">
                  <span className={positive ? 'text-positive' : 'text-negative'}>
                    {positive ? '▲ ' : '▼ '}
                    {formatCurrency(h.gainLoss)} ({formatPercent(h.gainLossPct, { signed: true })})
                  </span>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
