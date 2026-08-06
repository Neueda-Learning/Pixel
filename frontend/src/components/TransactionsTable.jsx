import { Link } from 'react-router-dom'
import { formatCurrency, formatDate, formatNumber } from '../utils/format'
import TablePagination from './TablePagination'
import './DataTable.css'

export default function TransactionsTable({
  transactions,
  onDelete,
  onEdit,
  compact = false,
  page = 1,
  pageSize = compact ? 5 : 10,
  onPageChange = () => {},
}) {
  if (!transactions || transactions.length === 0) {
    return <p className="text-muted">No transactions in this period.</p>
  }

  const totalPages = Math.max(1, Math.ceil(transactions.length / pageSize))
  const safePage = Math.min(Math.max(page, 1), totalPages)
  const start = (safePage - 1) * pageSize
  const pageTransactions = transactions.slice(start, start + pageSize)

  return (
    <>
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
            {pageTransactions.map((tx) => {
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
                      <button
                        className="icon-btn"
                        onClick={() => onEdit(tx)}
                        aria-label={`Edit transaction ${tx.id}`}
                        title="Edit"
                      >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                          <path
                            d="M4 20h4L18.5 9.5a1.5 1.5 0 000-2.12l-1.88-1.88a1.5 1.5 0 00-2.12 0L4 15.5V20z"
                            stroke="currentColor"
                            strokeWidth="1.6"
                            strokeLinejoin="round"
                            strokeLinecap="round"
                          />
                        </svg>
                      </button>
                      <button
                        className="icon-btn icon-btn-danger"
                        onClick={() => onDelete(tx)}
                        aria-label={`Delete transaction ${tx.id}`}
                        title="Delete"
                      >
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                          <path
                            d="M5 7h14M9 7V5a1 1 0 011-1h4a1 1 0 011 1v2m-1 0v12a1 1 0 01-1 1h-4a1 1 0 01-1-1V7h6z"
                            stroke="currentColor"
                            strokeWidth="1.6"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                          />
                        </svg>
                      </button>
                    </td>
                  )}
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      <TablePagination
        page={safePage}
        pageSize={pageSize}
        totalItems={transactions.length}
        onPageChange={onPageChange}
      />
    </>
  )
}
