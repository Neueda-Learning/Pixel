import { useState } from 'react'
import useApi from '../hooks/useApi'
import { getTransactions, addTransaction, updateTransaction, deleteTransaction } from '../api/transactions'
import TransactionForm from '../components/TransactionForm'
import TransactionsTable from '../components/TransactionsTable'
import EditTransactionModal from '../components/EditTransactionModal'
import CsvImportPanel from '../components/CsvImportPanel'
import PeriodToggle from '../components/PeriodToggle'
import LoadingState from '../components/LoadingState'
import ErrorState, { extractErrorMessage } from '../components/ErrorState'
import './Transactions.css'

export default function Transactions() {
  const [period, setPeriod] = useState('ALL')
  const [dateRange, setDateRange] = useState({ from: '', to: '' })
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)

  const customRangeActive = Boolean(dateRange.from && dateRange.to)
  const tx = useApi(
    () => getTransactions(period, customRangeActive ? { from: dateRange.from, to: dateRange.to } : {}),
    [period, customRangeActive, dateRange.from, dateRange.to]
  )

  const handleAdd = async (payload) => {
    setSubmitting(true)
    setFormError(null)
    try {
      await addTransaction(payload)
      tx.reload()
    } catch (err) {
      setFormError(extractErrorMessage(err, 'Could not add transaction.'))
      throw err
    } finally {
      setSubmitting(false)
    }
  }

  const handleDelete = async (id) => {
    try {
      await deleteTransaction(id)
      tx.reload()
    } catch (err) {
      setFormError(extractErrorMessage(err, 'Could not delete transaction.'))
    }
  }

  const handleSaveEdit = async (id, payload) => {
    setSaving(true)
    try {
      await updateTransaction(id, payload)
      setEditing(null)
      tx.reload()
    } catch (err) {
      setFormError(extractErrorMessage(err, 'Could not update transaction.'))
    } finally {
      setSaving(false)
    }
  }

  const clearDateRange = () => setDateRange({ from: '', to: '' })

  return (
    <div className="transactions-page">
      <section className="card card-pad">
        <div className="card-header">
          <div>
            <div className="card-title">Add transaction</div>
            <div className="card-subtitle">Record a buy or sell — holdings are derived automatically</div>
          </div>
        </div>
        <TransactionForm onSubmit={handleAdd} submitting={submitting} />
        {formError && (
          <p className="field-error" style={{ marginTop: 'var(--space-3)' }}>
            {formError}
          </p>
        )}
      </section>

      <section className="card card-pad">
        <div className="card-header">
          <div>
            <div className="card-title">Import from CSV</div>
            <div className="card-subtitle">Bulk-import historical transactions from a CSV file</div>
          </div>
        </div>
        <CsvImportPanel onImported={tx.reload} />
      </section>

      <section className="card card-pad">
        <div className="card-header">
          <div>
            <div className="card-title">History</div>
          </div>
          <div className="tx-history-filters">
            <div className="tx-date-range">
              <input
                type="date"
                className="input"
                value={dateRange.from}
                onChange={(e) => setDateRange((r) => ({ ...r, from: e.target.value }))}
                aria-label="From date"
              />
              <span className="text-muted">to</span>
              <input
                type="date"
                className="input"
                value={dateRange.to}
                onChange={(e) => setDateRange((r) => ({ ...r, to: e.target.value }))}
                aria-label="To date"
              />
              {customRangeActive && (
                <button type="button" className="btn btn-ghost" onClick={clearDateRange}>
                  Clear
                </button>
              )}
            </div>
            <PeriodToggle options={['3M', '6M', '1Y', 'ALL']} value={period} onChange={setPeriod} />
          </div>
        </div>
        {tx.loading ? (
          <LoadingState height={240} />
        ) : tx.error ? (
          <ErrorState
            message={extractErrorMessage(tx.error, 'Could not load transactions.')}
            onRetry={tx.reload}
          />
        ) : (
          <TransactionsTable
            transactions={[...tx.data].sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt))}
            onDelete={handleDelete}
            onEdit={setEditing}
          />
        )}
      </section>

      <EditTransactionModal
        transaction={editing}
        onSave={handleSaveEdit}
        onClose={() => setEditing(null)}
        saving={saving}
      />
    </div>
  )
}
