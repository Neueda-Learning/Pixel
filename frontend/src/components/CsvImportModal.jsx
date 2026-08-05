import { useRef, useState } from 'react'
import { importTransactions } from '../api/transactions'
import './CsvImportModal.css'

const EXPECTED_HEADERS = ['symbol', 'txtype', 'quantity', 'price', 'date']

const SAMPLE_CSV = `symbol,txType,quantity,price,date
AAPL,BUY,10,187.32,2026-01-15
MSFT,SELL,5,412.50,2026-02-03
`

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

function downloadSampleCsv() {
  const blob = new Blob([SAMPLE_CSV], { type: 'text/csv' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = 'transactions-sample.csv'
  a.click()
  URL.revokeObjectURL(url)
}

export default function CsvImportModal({ open, onClose, onImported }) {
  const [rows, setRows] = useState([])
  const [headerError, setHeaderError] = useState(null)
  const [importing, setImporting] = useState(false)
  const [result, setResult] = useState(null)
  const [dragActive, setDragActive] = useState(false)
  const fileInputRef = useRef(null)

  if (!open) return null

  const readFile = (file) => {
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

  const handleDrop = (e) => {
    e.preventDefault()
    setDragActive(false)
    readFile(e.dataTransfer.files?.[0])
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
      onImported?.()
    } catch {
      setResult({ error: 'Import failed. Please check the file and try again.' })
    } finally {
      setImporting(false)
    }
  }

  const handleClose = () => {
    setRows([])
    setHeaderError(null)
    setResult(null)
    if (fileInputRef.current) fileInputRef.current.value = ''
    onClose()
  }

  return (
    <div className="modal-overlay" onMouseDown={handleClose}>
      <div className="modal-card csv-modal-card" onMouseDown={(e) => e.stopPropagation()}>
        <div className="card-header">
          <div>
            <div className="card-title">Import from CSV</div>
            <div className="card-subtitle">Bulk-import historical transactions from a file</div>
          </div>
          <button type="button" className="btn btn-ghost" onClick={handleClose} aria-label="Close">
            ✕
          </button>
        </div>

        <div
          className={`csv-dropzone${dragActive ? ' active' : ''}`}
          onDragOver={(e) => {
            e.preventDefault()
            setDragActive(true)
          }}
          onDragLeave={() => setDragActive(false)}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          role="button"
          tabIndex={0}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv,text/csv"
            onChange={(e) => readFile(e.target.files?.[0])}
            hidden
          />
          <p>Drag and drop a CSV file here, or click to choose a file</p>
          <span className="text-muted" style={{ fontSize: 12 }}>
            Columns: symbol, txType, quantity, price, date
          </span>
        </div>

        <button type="button" className="btn btn-ghost csv-sample-btn" onClick={downloadSampleCsv}>
          Download sample CSV
        </button>

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
              <button
                type="button"
                className="btn btn-primary"
                onClick={handleImport}
                disabled={importing || validCount === 0}
              >
                {importing ? 'Importing…' : `Import ${validCount} transaction(s)`}
              </button>
            </div>
          </>
        )}

        {result?.count != null && <p className="text-positive">Imported {result.count} transaction(s).</p>}
        {result?.error && <p className="field-error">{result.error}</p>}
      </div>
    </div>
  )
}
