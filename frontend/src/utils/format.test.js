import { describe, expect, it } from 'vitest'
import {
  formatCurrency,
  formatCompactCurrency,
  formatPercent,
  formatRatioAsPercent,
  formatNumber,
  formatDate,
  formatShortDate,
  formatYear,
  formatDateTime,
} from './format'

describe('formatCurrency', () => {
  it('formats a positive number as USD currency', () => {
    expect(formatCurrency(1234.5)).toBe('$1,234.50')
  })

  it('returns the placeholder for null/undefined', () => {
    expect(formatCurrency(null)).toBe('—')
    expect(formatCurrency(undefined)).toBe('—')
  })

  it('formats zero correctly', () => {
    expect(formatCurrency(0)).toBe('$0.00')
  })
})

describe('formatCompactCurrency', () => {
  it('abbreviates large numbers', () => {
    expect(formatCompactCurrency(1500000)).toBe('$1.5M')
  })

  it('returns the placeholder for null', () => {
    expect(formatCompactCurrency(null)).toBe('—')
  })
})

describe('formatPercent', () => {
  it('formats without a sign by default', () => {
    expect(formatPercent(12.345)).toBe('12.35%')
  })

  it('adds a + sign for positive values when signed is true', () => {
    expect(formatPercent(5, { signed: true })).toBe('+5.00%')
  })

  it('does not add a sign for negative values when signed is true', () => {
    expect(formatPercent(-5, { signed: true })).toBe('-5.00%')
  })

  it('returns the placeholder for null', () => {
    expect(formatPercent(null)).toBe('—')
  })
})

describe('formatRatioAsPercent', () => {
  it('multiplies the ratio by 100 before formatting', () => {
    expect(formatRatioAsPercent(0.1234)).toBe('12.34%')
  })

  it('returns the placeholder for null', () => {
    expect(formatRatioAsPercent(null)).toBe('—')
  })
})

describe('formatNumber', () => {
  it('formats with the default 2 digits', () => {
    expect(formatNumber(3)).toBe('3.00')
  })

  it('formats with a custom digit count', () => {
    expect(formatNumber(3.14159, 4)).toBe('3.1416')
  })

  it('returns the placeholder for null', () => {
    expect(formatNumber(null)).toBe('—')
  })
})

describe('date formatters', () => {
  const iso = '2026-03-15T12:00:00Z'

  it('formatDate produces a long month/day/year string', () => {
    expect(formatDate(iso)).toBe('Mar 15, 2026')
  })

  it('formatShortDate omits the year', () => {
    expect(formatShortDate(iso)).toBe('Mar 15')
  })

  it('formatYear returns only the year', () => {
    expect(formatYear(iso)).toBe('2026')
  })

  it('formatDateTime includes a time component', () => {
    expect(formatDateTime(iso)).toMatch(/Mar 15, 2026/)
  })

  it('all date formatters return the placeholder for falsy input', () => {
    expect(formatDate(null)).toBe('—')
    expect(formatShortDate(undefined)).toBe('—')
    expect(formatYear('')).toBe('—')
    expect(formatDateTime(null)).toBe('—')
  })
})
