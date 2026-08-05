import { useEffect, useRef, useState } from 'react'
import { searchSymbols } from '../api/market'
import './SymbolAutocomplete.css'

const DEBOUNCE_MS = 250

/** Searchable symbol/company autocomplete backed by Finnhub search. Only a selected result is a valid value. */
export default function SymbolAutocomplete({ value, onSelect, error, disabled }) {
  const [query, setQuery] = useState(value ? `${value.description} (${value.symbol})` : '')
  const [results, setResults] = useState([])
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const debounceRef = useRef(null)
  const containerRef = useRef(null)

  useEffect(() => {
    setQuery(value ? `${value.description} (${value.symbol})` : '')
  }, [value])

  useEffect(() => {
    const onClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) setOpen(false)
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [])

  const handleChange = (e) => {
    const q = e.target.value
    setQuery(q)
    onSelect(null)
    if (debounceRef.current) clearTimeout(debounceRef.current)
    if (q.trim().length < 1) {
      setResults([])
      setOpen(false)
      return
    }
    debounceRef.current = setTimeout(async () => {
      setLoading(true)
      try {
        const data = await searchSymbols(q.trim())
        setResults(data || [])
        setOpen(true)
      } catch {
        setResults([])
      } finally {
        setLoading(false)
      }
    }, DEBOUNCE_MS)
  }

  const handlePick = (result) => {
    onSelect(result)
    setQuery(`${result.description} (${result.symbol})`)
    setOpen(false)
    setResults([])
  }

  return (
    <div className="symbol-autocomplete" ref={containerRef}>
      <input
        className="input"
        placeholder="Search company or ticker (e.g. Apple, AAPL)"
        value={query}
        onChange={handleChange}
        onFocus={() => results.length > 0 && setOpen(true)}
        autoComplete="off"
        disabled={disabled}
      />
      {open && (
        <div className="symbol-autocomplete-menu">
          {loading && <div className="symbol-autocomplete-item text-muted">Searching…</div>}
          {!loading && results.length === 0 && (
            <div className="symbol-autocomplete-item text-muted">No matches</div>
          )}
          {!loading &&
            results.map((r) => (
              <button
                type="button"
                key={r.symbol}
                className="symbol-autocomplete-item"
                onClick={() => handlePick(r)}
              >
                <span>{r.description}</span>
                <span className="symbol-autocomplete-symbol">{r.symbol}</span>
              </button>
            ))}
        </div>
      )}
      {error && <span className="field-error">{error}</span>}
    </div>
  )
}
