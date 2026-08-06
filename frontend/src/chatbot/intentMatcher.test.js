import { describe, expect, it } from 'vitest'
import { matchIntent, normalizeInput } from './intentMatcher'

describe('intentMatcher', () => {
  it('normalizes case, punctuation, and extra spaces', () => {
    expect(normalizeInput('  Hello, Portfolio!!!  ')).toBe('hello portfolio')
  })

  it('matches greeting intent with regex and keywords', () => {
    const result = matchIntent('Hey there')
    expect(result.intent.id).toBe('greeting')
    expect(result.usedFallback).toBe(false)
  })

  it('matches portfolio summary intent with higher confidence than generic terms', () => {
    const result = matchIntent('Can I get my portfolio summary and total value?')
    expect(result.intent.id).toBe('portfolio_summary')
    expect(result.score).toBeGreaterThan(0.55)
  })

  it('falls back when no intent passes threshold', () => {
    const result = matchIntent('zxy qwr ptt unknown topic')
    expect(result.intent.id).toBe('fallback')
    expect(result.usedFallback).toBe(true)
  })
})
