import { useEffect, useState } from 'react'
import { getOpenLots } from '../api/transactions'
import { formatCurrency, formatDate } from '../utils/format'
import './EditTransactionModal.css'

export default function EditTransactionModal({ transaction, onSave, onClose, saving }) {
  const [values, setValues] = useState(null)
  const [errors, setErrors] = useState({})
  const [lots, setLots] = useState([])
  const [lotsLoading, setLotsLoading] = useState(false)

  useEffect(() => {
    if (!transaction) return
    setValues({
      symbol: transaction.symbol,
      txType: transaction.txType,
      quantity: String(Math.trunc(Number(transaction.quantity))),
      price: String(transaction.price),
      buyTransactionId: transaction.buyTransactionId != null ? String(transaction.buyTransactionId) : '',
      executedAt: new Date(transaction.executedAt).toISOString().slice(0, 10),
    })
    setErrors({})
  }, [transaction])

  const isSell = values?.txType === 'SELL'

  useEffect(() => {
    if (!transaction || !values || !isSell || !values.symbol) {
      setLots([])
      return
    }
    let cancelled = false
    setLotsLoading(true)
    getOpenLots(values.symbol, transaction.id)
      .then((data) => !cancelled && setLots(data))
      .catch(() => !cancelled && setLots([]))
      .finally(() => !cancelled && setLotsLoading(false))
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [transaction, isSell, values?.symbol])

  if (!transaction || !values) return null

  const selectedLot = lots.find((l) => String(l.transactionId) === String(values.buyTransactionId))

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }))

  const setTxType = (e) => setValues((v) => ({ ...v, txType: e.target.value, buyTransactionId: '' }))

  const setSymbol = (e) => setValues((v) => ({ ...v, symbol: e.target.value, buyTransactionId: '' }))

  const validate = () => {
    const errs = {}
    if (!values.symbol.trim()) errs.symbol = 'Symbol is required'
    const qty = Number(values.quantity)
    if (!values.quantity || qty <= 0) errs.quantity = 'Quantity must be positive'
    else if (!Number.isInteger(qty)) errs.quantity = 'Quantity must be a whole number'
    else if (isSell && selectedLot && qty > Number(selectedLot.remainingQuantity)) {
      errs.quantity = `Only ${Number(selectedLot.remainingQuantity)} share(s) available in this lot`
    }
    if (!values.price || Number(values.price) <= 0) errs.price = 'Price must be positive'
    if (isSell && !values.buyTransactionId) errs.buyTransactionId = 'Select which buy lot you are selling from'
    if (!values.executedAt) errs.executedAt = 'Date is required'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!validate()) return
    onSave(transaction.id, {
      symbol: values.symbol.trim().toUpperCase(),
      txType: values.txType,
      quantity: Number(values.quantity),
      price: Number(values.price),
      buyTransactionId: isSell ? Number(values.buyTransactionId) : undefined,
      executedAt: new Date(`${values.executedAt}T00:00:00Z`).toISOString(),
    })
  }

  return (
    <div className="modal-overlay" onMouseDown={onClose}>
      <div className="modal-card" onMouseDown={(e) => e.stopPropagation()}>
        <div className="card-header">
          <div className="card-title">Edit transaction</div>
          <button type="button" className="btn btn-ghost" onClick={onClose} aria-label="Close">
            ✕
          </button>
        </div>
        <form onSubmit={handleSubmit} noValidate className="tx-form">
          <div className="tx-form-grid">
            <div className="field">
              <label htmlFor="edit-symbol">Symbol</label>
              <input id="edit-symbol" className="input" value={values.symbol} onChange={setSymbol} />
              {errors.symbol && <span className="field-error">{errors.symbol}</span>}
            </div>
            <div className="field">
              <label htmlFor="edit-type">Type</label>
              <select id="edit-type" className="select" value={values.txType} onChange={setTxType}>
                <option value="BUY">Buy</option>
                <option value="SELL">Sell</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="edit-qty">Quantity</label>
              <input
                id="edit-qty"
                className="input"
                type="number"
                step="1"
                min="1"
                max={isSell && selectedLot ? Number(selectedLot.remainingQuantity) : undefined}
                value={values.quantity}
                onChange={set('quantity')}
              />
              {errors.quantity && <span className="field-error">{errors.quantity}</span>}
            </div>
            {isSell && (
              <div className="field">
                <label htmlFor="edit-buy-lot">Buy price</label>
                <select
                  id="edit-buy-lot"
                  className="select"
                  value={values.buyTransactionId}
                  onChange={set('buyTransactionId')}
                  disabled={lotsLoading}
                >
                  <option value="">{lotsLoading ? 'Loading…' : 'Select a lot…'}</option>
                  {lots.map((lot) => (
                    <option key={lot.transactionId} value={lot.transactionId}>
                      {formatDate(lot.executedAt)} — {formatCurrency(lot.price)} ({Number(lot.remainingQuantity)} avail)
                    </option>
                  ))}
                </select>
                {errors.buyTransactionId && <span className="field-error">{errors.buyTransactionId}</span>}
              </div>
            )}
            <div className="field">
              <label htmlFor="edit-price">{isSell ? 'Sell price' : 'Price'}</label>
              <input id="edit-price" className="input" type="number" step="any" min="0" value={values.price} onChange={set('price')} />
              {errors.price && <span className="field-error">{errors.price}</span>}
            </div>
            <div className="field">
              <label htmlFor="edit-date">Date</label>
              <input id="edit-date" className="input" type="date" value={values.executedAt} onChange={set('executedAt')} />
              {errors.executedAt && <span className="field-error">{errors.executedAt}</span>}
            </div>
          </div>
          <div className="modal-actions">
            <button type="button" className="btn" onClick={onClose}>
              Cancel
            </button>
            <button type="submit" className="btn btn-primary" disabled={saving}>
              {saving ? 'Saving…' : 'Save changes'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
