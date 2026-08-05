import { useEffect, useState } from 'react'
import SymbolAutocomplete from './SymbolAutocomplete'
import StockInsights from './StockInsights'
import { getQuote } from '../api/market'
import { formatCurrency } from '../utils/format'
import './TransactionForm.css'

const EMPTY = { instrument: null, txType: 'BUY', quantity: '' }

export default function TransactionForm({ onSubmit, submitting }) {
  const [values, setValues] = useState(EMPTY)
  const [quote, setQuote] = useState(null)
  const [quoteLoading, setQuoteLoading] = useState(false)
  const [errors, setErrors] = useState({})

  useEffect(() => {
    if (!values.instrument) {
      setQuote(null)
      return
    }
    let cancelled = false
    setQuoteLoading(true)
    getQuote(values.instrument.symbol)
      .then((data) => !cancelled && setQuote(data))
      .catch(() => !cancelled && setQuote(null))
      .finally(() => !cancelled && setQuoteLoading(false))
    return () => {
      cancelled = true
    }
  }, [values.instrument])

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }))

  const validate = () => {
    const errs = {}
    if (!values.instrument) errs.symbol = 'Select a valid stock from the search results'
    const qty = Number(values.quantity)
    if (!values.quantity || qty <= 0) errs.quantity = 'Quantity must be positive'
    else if (!Number.isInteger(qty)) errs.quantity = 'Quantity must be a whole number'
    if (values.instrument && (!quote || quote.current == null)) errs.symbol = 'Live price unavailable for this symbol'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!validate()) return
    onSubmit({
      symbol: values.instrument.symbol,
      txType: values.txType,
      quantity: Number(values.quantity),
      price: quote.current,
    }).then(() => {
      setValues(EMPTY)
      setQuote(null)
    })
  }

  return (
    <form className="tx-form" onSubmit={handleSubmit} noValidate>
      <div className="tx-form-grid">
        <div className="field" style={{ gridColumn: 'span 2' }}>
          <label htmlFor="tx-symbol">Symbol</label>
          <SymbolAutocomplete
            value={values.instrument}
            onSelect={(inst) => setValues((v) => ({ ...v, instrument: inst }))}
            error={errors.symbol}
          />
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
            step="1"
            min="1"
            placeholder="10"
            value={values.quantity}
            onChange={set('quantity')}
          />
          {errors.quantity && <span className="field-error">{errors.quantity}</span>}
        </div>

        <div className="field">
          <label htmlFor="tx-price">
            <span className="live-dot" aria-hidden="true" />
            Price (live)
          </label>
          <input
            id="tx-price"
            className="input"
            value={quoteLoading ? 'Loading…' : quote ? formatCurrency(quote.current) : ''}
            readOnly
            disabled
          />
        </div>
      </div>

      {values.instrument && <StockInsights symbol={values.instrument.symbol} quote={quote} />}

      <button type="submit" className="btn btn-primary tx-form-submit" disabled={submitting}>
        {submitting ? 'Adding…' : 'Add transaction'}
      </button>
    </form>
  )
}
