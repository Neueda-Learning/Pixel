import { useState } from 'react'
import useApi from '../hooks/useApi'
import { getTransactions, addTransaction, deleteTransaction } from '../api/transactions'
import TransactionForm from '../components/TransactionForm'
import TransactionsTable from '../components/TransactionsTable'
import PeriodToggle from '../components/PeriodToggle'
import LoadingState from '../components/LoadingState'
import ErrorState, { extractErrorMessage } from '../components/ErrorState'
import './Transactions.css'

export default function Transactions() {
  const [period, setPeriod] = useState('ALL')
  const [submitting, setSubmitting] = useState(false)
  const [formError, setFormError] = useState(null)

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
            <div className="card-title">History</div>
          </div>
          <PeriodToggle options={['3M', '6M', '1Y', 'ALL']} value={period} onChange={setPeriod} />
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
          />
        )}
      </section>
    </div>
  )
}
