import { useState, useRef } from 'react'
import useApi from '../hooks/useApi'
import { getTransactions, addTransaction, deleteTransaction, importTransactions } from '../api/transactions'
import TransactionForm from '../components/TransactionForm'
import TransactionsTable from '../components/TransactionsTable'
import PeriodToggle from '../components/PeriodToggle'
import LoadingState from '../components/LoadingState'
import ErrorState, { extractErrorMessage } from '../components/ErrorState'
import './Transactions.css'

export default function Transactions() {
  const [period, setPeriod] = useState('ALL')
  const [submitting, setSubmitting] = useState(false)
  const [importing, setImporting] = useState(false)
  const [formError, setFormError] = useState(null)
  const [importMsg, setImportMsg] = useState(null)
  const fileInputRef = useRef(null)

  const tx = useApi(() => getTransactions(period), [period])

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

  const handleImport = async (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setImporting(true)
    setImportMsg(null)
    setFormError(null)
    try {
      const result = await importTransactions(file)
      setImportMsg(`✓ Imported ${result.imported} transaction${result.imported !== 1 ? 's' : ''} successfully.`)
      tx.reload()
    } catch (err) {
      setFormError(extractErrorMessage(err, 'Import failed. Check your CSV format.'))
    } finally {
      setImporting(false)
      e.target.value = ''
    }
  }

  return (
    <div className="transactions-page">
      <section className="card card-pad">
        <div className="card-header">
          <div>
            <div className="card-title">Add holding</div>
            <div className="card-subtitle">Record a purchase — your portfolio is derived automatically</div>
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
            <div className="card-title">History</div>
            {importMsg && <div className="tx-import-msg">{importMsg}</div>}
          </div>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
            <PeriodToggle options={['3M', '6M', '1Y', 'ALL']} value={period} onChange={setPeriod} />
            <button
              className="action-btn"
              onClick={() => fileInputRef.current?.click()}
              disabled={importing}
              title="Import transactions from CSV"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M12 16V4m0 0 4 4m-4-4-4 4M3 17v2a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-2" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
              {importing ? 'Importing…' : 'Import CSV'}
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept=".csv,text/csv"
              style={{ display: 'none' }}
              onChange={handleImport}
            />
          </div>
        </div>
        <p className="text-muted" style={{ fontSize: 12, marginBottom: 'var(--space-3)' }}>
          CSV format: <code>symbol,quantity,price,date</code> — e.g. <code>AAPL,10,175.50,2024-01-15</code> (date accepts YYYY-MM-DD or YYYY-MM-DDT00:00:00Z)
        </p>
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
          />
        )}
      </section>
    </div>
  )
}
