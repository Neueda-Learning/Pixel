const currencyFormatter = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })
const compactCurrencyFormatter = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  notation: 'compact',
  maximumFractionDigits: 1,
})

export const formatCurrency = (value) => (value == null ? '—' : currencyFormatter.format(value))

export const formatCompactCurrency = (value) => (value == null ? '—' : compactCurrencyFormatter.format(value))

export const formatPercent = (value, { signed = false } = {}) => {
  if (value == null) return '—'
  const sign = signed && value > 0 ? '+' : ''
  return `${sign}${value.toFixed(2)}%`
}

export const formatRatioAsPercent = (value, { signed = false } = {}) => {
  if (value == null) return '—'
  return formatPercent(value * 100, { signed })
}

export const formatNumber = (value, digits = 2) => (value == null ? '—' : Number(value).toFixed(digits))

export const formatDate = (value) => {
  if (!value) return '—'
  const d = new Date(value)
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

export const formatShortDate = (value) => {
  if (!value) return '—'
  const d = new Date(value)
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

export const formatYear = (value) => {
  if (!value) return '—'
  const d = new Date(value)
  return d.toLocaleDateString('en-US', { year: 'numeric' })
}

export const formatDateTime = (value) => {
  if (!value) return '—'
  const d = new Date(value)
  return d.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}
