import { useEffect, useState } from 'react'
import SymbolAutocomplete from './SymbolAutocomplete'
import { getOpenLots } from '../api/transactions'
import { formatCurrency, formatDate } from '../utils/format'
import './TransactionForm.css'

const todayIso = () => new Date().toISOString().slice(0, 10)

const EMPTY = { instrument: null, txType: 'BUY', quantity: '', price: '', buyTransactionId: '', executedAt: todayIso() }

export default function TransactionForm({ onSubmit, submitting }) {
  const [values, setValues] = useState(EMPTY)
  const [errors, setErrors] = useState({})
  const [lots, setLots] = useState([])
  const [lotsLoading, setLotsLoading] = useState(false)

  const isSell = values.txType === 'SELL'

  useEffect(() => {
    if (!isSell || !values.instrument) {
      setLots([])
      return
    }
    let cancelled = false
    setLotsLoading(true)
    getOpenLots(values.instrument.symbol)
      .then((data) => !cancelled && setLots(data))
      .catch(() => !cancelled && setLots([]))
      .finally(() => !cancelled && setLotsLoading(false))
    return () => {
      cancelled = true
    }
  }, [isSell, values.instrument])

  const selectedLot = lots.find((l) => String(l.transactionId) === String(values.buyTransactionId))

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }))

  const setTxType = (e) => setValues((v) => ({ ...v, txType: e.target.value, buyTransactionId: '' }))

  const setInstrument = (inst) => setValues((v) => ({ ...v, instrument: inst, buyTransactionId: '' }))

  const validate = () => {
    const errs = {}
    if (!values.instrument) errs.symbol = 'Select a valid stock from the search results'
    const qty = Number(values.quantity)
    if (!values.quantity || qty <= 0) errs.quantity = 'Quantity must be positive'
    else if (!Number.isInteger(qty)) errs.quantity = 'Quantity must be a whole number'
    else if (isSell && selectedLot && qty > Number(selectedLot.remainingQuantity)) {
      errs.quantity = `Only ${Number(selectedLot.remainingQuantity)} share(s) available in this lot`
    }
    if (!values.price || Number(values.price) <= 0) {
      errs.price = isSell ? 'Sell price must be positive' : 'Price must be positive'
    }
    if (isSell && !values.buyTransactionId) {
      errs.buyTransactionId = 'Select which buy lot you are selling from'
    }
    if (!values.executedAt) errs.executedAt = 'Date is required'
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
      price: Number(values.price),
      buyTransactionId: isSell ? Number(values.buyTransactionId) : undefined,
      executedAt: new Date(`${values.executedAt}T00:00:00Z`).toISOString(),
    }).then(() => {
      setValues(EMPTY)
      setLots([])
    })
  }

  return (
    <form className="tx-form" onSubmit={handleSubmit} noValidate>
      <div className="tx-form-grid">
        <div className="field" style={{ gridColumn: 'span 2' }}>
          <label htmlFor="tx-symbol">Symbol</label>
          <SymbolAutocomplete value={values.instrument} onSelect={setInstrument} error={errors.symbol} />
        </div>

        <div className="field">
          <label htmlFor="tx-type">Type</label>
          <select id="tx-type" className="select" value={values.txType} onChange={setTxType}>
            <option value="BUY">Buy</option>
            <option value="SELL">Sell</option>
          </select>
        </div>

        {isSell && (
          <div className="field">
            <label htmlFor="tx-buy-lot">Buy price</label>
            <select
              id="tx-buy-lot"
              className="select"
              value={values.buyTransactionId}
              onChange={set('buyTransactionId')}
              disabled={!values.instrument || lotsLoading}
            >
              <option value="">{lotsLoading ? 'Loading…' : 'Select a lot…'}</option>
              {lots.map((lot) => (
                <option key={lot.transactionId} value={lot.transactionId}>
                  {formatDate(lot.executedAt)} — {formatCurrency(lot.price)} ({Number(lot.remainingQuantity)} avail)
                </option>
              ))}
            </select>
            {!lotsLoading && values.instrument && lots.length === 0 && (
              <span className="field-error">No shares of {values.instrument.symbol} available to sell</span>
            )}
            {errors.buyTransactionId && <span className="field-error">{errors.buyTransactionId}</span>}
          </div>
        )}

        <div className="field">
          <label htmlFor="tx-qty">Quantity</label>
          <input
            id="tx-qty"
            className="input"
            type="number"
            step="1"
            min="1"
            max={isSell && selectedLot ? Number(selectedLot.remainingQuantity) : undefined}
            placeholder="10"
            value={values.quantity}
            onChange={set('quantity')}
          />
          {errors.quantity && <span className="field-error">{errors.quantity}</span>}
        </div>

        <div className="field">
          <label htmlFor="tx-price">{isSell ? 'Sell price' : 'Price'}</label>
          <input
            id="tx-price"
            className="input"
            type="number"
            step="any"
            min="0"
            placeholder="0.00"
            value={values.price}
            onChange={set('price')}
          />
          {errors.price && <span className="field-error">{errors.price}</span>}
        </div>

        <div className="field">
          <label htmlFor="tx-date">Date</label>
          <input id="tx-date" className="input" type="date" value={values.executedAt} onChange={set('executedAt')} />
          {errors.executedAt && <span className="field-error">{errors.executedAt}</span>}
        </div>
      </div>

      <button type="submit" className="btn btn-primary tx-form-submit" disabled={submitting}>
        {submitting ? 'Adding…' : 'Add transaction'}
      </button>

    </form>
  )
}
