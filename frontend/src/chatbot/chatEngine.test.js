import { describe, expect, it } from 'vitest'
import { createInitialSessionState, generateBotReply } from './chatEngine'

const mockServices = {
  buildPortfolioSummaryData: async () => ({
    totalValue: '$125K',
    totalGainLoss: '+$4.2K',
    totalGainLossPct: '+3.2%',
    holdingsCount: '8',
    symbol: 'AAPL',
  }),
  buildTopHoldingsData: async () => ({
    topHoldings: 'AAPL ($42K), MSFT ($31K), NVDA ($22K)',
    symbol: 'AAPL',
  }),
  buildRiskData: async () => ({
    symbol: 'AAPL',
    recommendation: 'HOLD',
    annualizedVolatility: '22.10%',
    annualizedReturn: '+10.40%',
    maxDrawdown: '-14.22%',
    sharpeRatio: '1.12',
    beta: '0.92',
    rsi14: '54.1',
    trend: 'Uptrend',
    rationale: 'Momentum is neutral-to-positive.',
    disclaimer: 'Not financial advice.',
  }),
  buildMarketNewsData: async () => ({
    symbol: 'AAPL',
    headlines: 'Headline A | Headline B | Headline C',
  }),
  buildRebalanceData: async () => ({
    rebalanceSummary: 'Trim top position and diversify.',
    rebalanceDetail: 'Add staggered buys in underweight sectors.',
  }),
}

describe('chatEngine', () => {
  it('returns fallback message for empty input', async () => {
    const reply = await generateBotReply('   ', createInitialSessionState(), mockServices)
    expect(reply.intentId).toBe('fallback')
    expect(reply.error).toBe('empty_input')
  })

  it('returns max-length guardrail message', async () => {
    const longText = 'a'.repeat(501)
    const reply = await generateBotReply(longText, createInitialSessionState(), mockServices)
    expect(reply.intentId).toBe('fallback')
    expect(reply.error).toBe('max_length')
  })

  it('handles portfolio summary intent with templated placeholders', async () => {
    const reply = await generateBotReply('portfolio summary', createInitialSessionState(), mockServices)
    expect(reply.intentId).toBe('portfolio_summary')
    expect(reply.text).toContain('$125K')
    expect(reply.nextState.lastSymbol).toBe('AAPL')
  })

  it('supports yes follow-up for detail prompts', async () => {
    const first = await generateBotReply('top risk', createInitialSessionState(), mockServices)
    const second = await generateBotReply('yes', first.nextState, mockServices)

    expect(first.nextState.pendingDetailIntent).toBe('risk_analysis')
    expect(second.intentId).toBe('risk_analysis')
    expect(second.text).toContain('More detail for AAPL')
  })

  it('uses fallback intent for unknown request', async () => {
    const reply = await generateBotReply('blargle quantum sandwich', createInitialSessionState(), mockServices)
    expect(reply.intentId).toBe('fallback')
  })
})
