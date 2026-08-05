import { useState } from 'react'
import './TransactionForm.css'

const today = () => new Date().toISOString().slice(0, 10)

// Stocks + ETFs available in the platform
const SYMBOLS = [
  // Large-cap stocks
  { value: 'AAPL',  label: 'AAPL — Apple Inc.', type: 'STOCK' },
  { value: 'MSFT',  label: 'MSFT — Microsoft Corp.', type: 'STOCK' },
  { value: 'GOOGL', label: 'GOOGL — Alphabet Inc.', type: 'STOCK' },
  { value: 'AMZN',  label: 'AMZN — Amazon.com Inc.', type: 'STOCK' },
  { value: 'TSLA',  label: 'TSLA — Tesla Inc.', type: 'STOCK' },
  { value: 'META',  label: 'META — Meta Platforms', type: 'STOCK' },
  { value: 'NVDA',  label: 'NVDA — NVIDIA Corp.', type: 'STOCK' },
  { value: 'JPM',   label: 'JPM — JPMorgan Chase', type: 'STOCK' },
  { value: 'V',     label: 'V — Visa Inc.', type: 'STOCK' },
  { value: 'NFLX',  label: 'NFLX — Netflix Inc.', type: 'STOCK' },
  { value: 'JNJ',   label: 'JNJ — Johnson & Johnson', type: 'STOCK' },
  { value: 'UNH',   label: 'UNH — UnitedHealth Group', type: 'STOCK' },
  { value: 'XOM',   label: 'XOM — Exxon Mobil', type: 'STOCK' },
  { value: 'MA',    label: 'MA — Mastercard Inc.', type: 'STOCK' },
  { value: 'PG',    label: 'PG — Procter & Gamble', type: 'STOCK' },
  { value: 'HD',    label: 'HD — Home Depot', type: 'STOCK' },
  { value: 'ABBV',  label: 'ABBV — AbbVie Inc.', type: 'STOCK' },
  { value: 'CVX',   label: 'CVX — Chevron Corp.', type: 'STOCK' },
  { value: 'LLY',   label: 'LLY — Eli Lilly & Co.', type: 'STOCK' },
  { value: 'BAC',   label: 'BAC — Bank of America', type: 'STOCK' },
  // ETFs
  { value: 'SPY',   label: 'SPY — SPDR S&P 500 ETF', type: 'ETF' },
  { value: 'QQQ',   label: 'QQQ — Invesco QQQ (NASDAQ)', type: 'ETF' },
  { value: 'VTI',   label: 'VTI — Vanguard Total Market', type: 'ETF' },
  { value: 'VOO',   label: 'VOO — Vanguard S&P 500', type: 'ETF' },
  { value: 'IWM',   label: 'IWM — iShares Russell 2000', type: 'ETF' },
  { value: 'GLD',   label: 'GLD — SPDR Gold Shares', type: 'ETF' },
  { value: 'TLT',   label: 'TLT — iShares 20+ Yr Treasury', type: 'ETF' },
  { value: 'EEM',   label: 'EEM — iShares Emerging Markets', type: 'ETF' },
  { value: 'VNQ',   label: 'VNQ — Vanguard Real Estate', type: 'ETF' },
  { value: 'ARKK',  label: 'ARKK — ARK Innovation ETF', type: 'ETF' },
  { value: 'XLK',   label: 'XLK — Technology Select SPDR', type: 'ETF' },
  { value: 'XLF',   label: 'XLF — Financial Select SPDR', type: 'ETF' },
]

const EMPTY = { symbol: '', quantity: '', price: '', executedAt: today() }

export default function TransactionForm({ onSubmit, submitting }) {
  const [values, setValues] = useState(EMPTY)
  const [errors, setErrors] = useState({})

  const set = (field) => (e) => setValues((v) => ({ ...v, [field]: e.target.value }))

  const validate = () => {
    const errs = {}
    if (!values.symbol) errs.symbol = 'Symbol is required'
    if (!values.quantity || !Number.isInteger(Number(values.quantity)) || Number(values.quantity) <= 0)
      errs.quantity = 'Quantity must be a positive whole number'
    if (!values.price || Number(values.price) <= 0) errs.price = 'Price must be positive'
    if (!values.executedAt) errs.executedAt = 'Date is required'
    setErrors(errs)
    return Object.keys(errs).length === 0
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    if (!validate()) return
    onSubmit({
      symbol: values.symbol,
      txType: 'BUY',
      quantity: parseInt(values.quantity, 10),
      price: Number(values.price),
      fees: 0,
      executedAt: new Date(`${values.executedAt}T00:00:00Z`).toISOString(),
    }).then(() => setValues(EMPTY))
  }

  const stocks = SYMBOLS.filter((s) => s.type === 'STOCK')
  const etfs   = SYMBOLS.filter((s) => s.type === 'ETF')

  return (
    <form className="tx-form" onSubmit={handleSubmit} noValidate>
      <div className="tx-form-grid">
        <div className="field" style={{ gridColumn: 'span 2' }}>
          <label htmlFor="tx-symbol">Symbol</label>
          <select id="tx-symbol" className="select" value={values.symbol} onChange={set('symbol')}>
            <option value="">— Select a symbol —</option>
            <optgroup label="Stocks">
              {stocks.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </optgroup>
            <optgroup label="ETFs">
              {etfs.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
            </optgroup>
          </select>
          {errors.symbol && <span className="field-error">{errors.symbol}</span>}
        </div>

        <div className="field">
          <label htmlFor="tx-qty">Quantity (shares)</label>
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
          <label htmlFor="tx-price">Price per share</label>
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
          <label htmlFor="tx-date">Purchase date</label>
          <input id="tx-date" className="input" type="date" value={values.executedAt} onChange={set('executedAt')} />
          {errors.executedAt && <span className="field-error">{errors.executedAt}</span>}
        </div>
      </div>

      <button type="submit" className="btn btn-primary" disabled={submitting}>
        {submitting ? 'Adding…' : 'Add holding'}
      </button>
    </form>
  )
}

