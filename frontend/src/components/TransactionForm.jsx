import { useState } from 'react'
import './TransactionForm.css'

const today = () => new Date().toISOString().slice(0, 10)

const EMPTY = { symbol: '', txType: 'BUY', quantity: '', price: '', fees: '', executedAt: today() }

export default function TransactionForm({ onSubmit, submitting }) {
  const [values, setValues] = useState(EMPTY)
  const [errors, setErrors] = useState({})

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }))

  const validate = () => {
    const errs = {}
    if (!values.symbol.trim()) errs.symbol = 'Symbol is required'
    if (!values.quantity || Number(values.quantity) <= 0) errs.quantity = 'Quantity must be positive'
    if (!values.price || Number(values.price) <= 0) errs.price = 'Price must be positive'
    if (!values.executedAt) errs.executedAt = 'Date is required'
    if (values.fees && Number(values.fees) < 0) errs.fees = 'Fees cannot be negative'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!validate()) return
    onSubmit({
      symbol: values.symbol.trim().toUpperCase(),
      txType: values.txType,
      quantity: Number(values.quantity),
      price: Number(values.price),
      fees: values.fees ? Number(values.fees) : 0,
      executedAt: new Date(`${values.executedAt}T00:00:00Z`).toISOString(),
    }).then(() => setValues(EMPTY))
  }

  return (
    <form className="tx-form" onSubmit={handleSubmit} noValidate>
      <div className="tx-form-grid">
        <div className="field">
          <label htmlFor="tx-symbol">Symbol</label>
          <input
            id="tx-symbol"
            className="input"
            placeholder="AAPL"
            value={values.symbol}
            onChange={set('symbol')}
            autoComplete="off"
          />
          {errors.symbol && <span className="field-error">{errors.symbol}</span>}
        </div>

        <div className="field">
          <label htmlFor="tx-type">Type</label>
          <select id="tx-type" className="select" value={values.txType} onChange={set('txType')}>
            <option value="BUY">Buy</option>
            <option value="SELL">Sell</option>
          </select>
        </div>

        <div className="field">
          <label htmlFor="tx-qty">Quantity</label>
          <input
            id="tx-qty"
            className="input"
            type="number"
            step="any"
            min="0"
            placeholder="10"
            value={values.quantity}
            onChange={set('quantity')}
          />
          {errors.quantity && <span className="field-error">{errors.quantity}</span>}
        </div>

        <div className="field">
          <label htmlFor="tx-price">Price</label>
          <input
            id="tx-price"
            className="input"
            type="number"
            step="any"
            min="0"
            placeholder="150.00"
            value={values.price}
            onChange={set('price')}
          />
          {errors.price && <span className="field-error">{errors.price}</span>}
        </div>

        <div className="field">
          <label htmlFor="tx-fees">Fees</label>
          <input
            id="tx-fees"
            className="input"
            type="number"
            step="any"
            min="0"
            placeholder="0.00"
            value={values.fees}
            onChange={set('fees')}
          />
          {errors.fees && <span className="field-error">{errors.fees}</span>}
        </div>

        <div className="field">
          <label htmlFor="tx-date">Date</label>
          <input id="tx-date" className="input" type="date" value={values.executedAt} onChange={set('executedAt')} />
          {errors.executedAt && <span className="field-error">{errors.executedAt}</span>}
        </div>
      </div>

      <button type="submit" className="btn btn-primary" disabled={submitting}>
        {submitting ? 'Adding…' : 'Add transaction'}
      </button>
    </form>
  )
}
