import { Link } from 'react-router-dom'
import { formatCurrency, formatDate, formatNumber } from '../utils/format'
import './DataTable.css'

export default function TransactionsTable({ transactions, onDelete, onEdit, compact = false }) {
  if (!transactions || transactions.length === 0) {
    return <p className="text-muted">No transactions in this period.</p>
  }

  return (
    <div className="scroll-x">
      <table className="data-table">
        <thead>
          <tr>
            <th>Date</th>
            <th>Symbol</th>
            <th>Type</th>
            <th className="num">Qty</th>
            <th className="num">Price</th>
            <th className="num">Total</th>
            {!compact && <th></th>}
          </tr>
        </thead>
        <tbody>
          {transactions.map((tx) => {
            const total = Number(tx.quantity) * Number(tx.price)
            const isBuy = tx.txType === 'BUY'
            return (
              <tr key={tx.id}>
                <td className="text-secondary">{formatDate(tx.executedAt)}</td>
                <td>
                  <Link to={`/instruments/${tx.symbol}`} className="symbol-link">
                    {tx.symbol}
                  </Link>
                </td>
                <td>
                  <span className={isBuy ? 'text-positive' : 'text-negative'} style={{ fontWeight: 600 }}>
                    {tx.txType}
                  </span>
                </td>
                <td className="num tabular">{formatNumber(tx.quantity, 0)}</td>
                <td className="num tabular">{formatCurrency(tx.price)}</td>
                <td className="num tabular">{formatCurrency(total)}</td>
                {!compact && (
                  <td className="row-actions">
                    <button className="btn btn-ghost" onClick={() => onEdit(tx)} aria-label={`Edit transaction ${tx.id}`}>
                      Edit
                    </button>
                    <button
                      className="btn btn-ghost"
                      onClick={() => onDelete(tx.id)}
                      aria-label={`Delete transaction ${tx.id}`}
                    >
                      Delete
                    </button>
                  </td>
                )}
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
