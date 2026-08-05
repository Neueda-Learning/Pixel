import { useRef, useState } from 'react'
import { importTransactions } from '../api/transactions'
import './CsvImportPanel.css'

const EXPECTED_HEADERS = ['symbol', 'txtype', 'quantity', 'price', 'date']

function parseCsv(text) {
  const lines = text.split(/\r?\n/).filter((l) => l.trim().length > 0)
  if (lines.length === 0) return { rows: [], headerError: 'File is empty' }

  const headers = lines[0].split(',').map((h) => h.trim().toLowerCase())
  const missing = EXPECTED_HEADERS.filter((h) => !headers.includes(h))
  if (missing.length > 0) {
    return { rows: [], headerError: `Missing column(s): ${missing.join(', ')}` }
  }

  const rows = lines.slice(1).map((line, idx) => {
    const cells = line.split(',').map((c) => c.trim())
    const raw = {}
    headers.forEach((h, i) => (raw[h] = cells[i] ?? ''))
    return { rowNumber: idx + 2, raw, ...validateRow(raw) }
  })
  return { rows, headerError: null }
}

function validateRow(raw) {
  const errors = []
  const symbol = raw.symbol?.trim().toUpperCase()
  if (!symbol) errors.push('symbol is required')

  const txType = raw.txtype?.trim().toUpperCase()
  if (txType !== 'BUY' && txType !== 'SELL') errors.push('txType must be BUY or SELL')

  const quantity = Number(raw.quantity)
  if (!raw.quantity || !Number.isInteger(quantity) || quantity <= 0) {
    errors.push('quantity must be a positive whole number')
  }

  const price = Number(raw.price)
  if (!raw.price || Number.isNaN(price) || price <= 0) errors.push('price must be positive')

  const dateMs = Date.parse(raw.date)
  if (!raw.date || Number.isNaN(dateMs)) errors.push('date is invalid')

  return {
    valid: errors.length === 0,
    errors,
    parsed: errors.length === 0 ? { symbol, txType, quantity, price, executedAt: new Date(dateMs).toISOString() } : null,
  }
}

export default function CsvImportPanel({ onImported }) {
  const [rows, setRows] = useState([])
  const [headerError, setHeaderError] = useState(null)
  const [importing, setImporting] = useState(false)
  const [result, setResult] = useState(null)
  const fileInputRef = useRef(null)

  const handleFile = (e) => {
    const file = e.target.files?.[0]
    if (!file) return
    setResult(null)
    const reader = new FileReader()
    reader.onload = () => {
      const { rows: parsedRows, headerError: err } = parseCsv(String(reader.result))
      setRows(parsedRows)
      setHeaderError(err)
    }
    reader.readAsText(file)
  }

  const validCount = rows.filter((r) => r.valid).length

  const handleImport = async () => {
    const validRows = rows.filter((r) => r.valid).map((r) => r.parsed)
    if (validRows.length === 0) return
    setImporting(true)
    try {
      const imported = await importTransactions(validRows)
      setResult({ count: imported.length })
      setRows([])
      if (fileInputRef.current) fileInputRef.current.value = ''
      onImported?.()
    } catch {
      setResult({ error: 'Import failed. Please check the file and try again.' })
    } finally {
      setImporting(false)
    }
  }

  return (
    <div className="csv-import">
      <div className="csv-import-controls">
        <input
          ref={fileInputRef}
          type="file"
          accept=".csv,text/csv"
          onChange={handleFile}
          className="csv-import-file"
        />
        <span className="text-muted" style={{ fontSize: 12 }}>
          Columns: symbol, txType, quantity, price, date
        </span>
      </div>

      {headerError && <p className="field-error">{headerError}</p>}

      {rows.length > 0 && (
        <>
          <div className="scroll-x">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Row</th>
                  <th>Symbol</th>
                  <th>Type</th>
                  <th className="num">Qty</th>
                  <th className="num">Price</th>
                  <th>Date</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.rowNumber}>
                    <td className="text-secondary">{r.rowNumber}</td>
                    <td>{r.raw.symbol}</td>
                    <td>{r.raw.txtype}</td>
                    <td className="num tabular">{r.raw.quantity}</td>
                    <td className="num tabular">{r.raw.price}</td>
                    <td>{r.raw.date}</td>
                    <td>
                      {r.valid ? (
                        <span className="text-positive">Valid</span>
                      ) : (
                        <span className="text-negative" title={r.errors.join('; ')}>
                          {r.errors.join('; ')}
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="csv-import-actions">
            <span className="text-muted" style={{ fontSize: 12.5 }}>
              {validCount} of {rows.length} row(s) valid
            </span>
            <button type="button" className="btn btn-primary" onClick={handleImport} disabled={importing || validCount === 0}>
              {importing ? 'Importing…' : `Import ${validCount} transaction(s)`}
            </button>
          </div>
        </>
      )}

      {result?.count != null && <p className="text-positive">Imported {result.count} transaction(s).</p>}
      {result?.error && <p className="field-error">{result.error}</p>}
    </div>
  )
}
