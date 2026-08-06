import { getHoldings, getPortfolioSummary } from '../../api/portfolio'
import { getRisk } from '../../api/risk'
import { getNews } from '../../api/market'
import { formatCompactCurrency, formatPercent, formatNumber, formatRatioAsPercent } from '../../utils/format'

const GROQ_API_URL = 'https://api.groq.com/openai/v1/chat/completions'
const GROQ_MODEL = 'llama-3.1-8b-instant'

function toTopHoldings(holdings, limit = 3) {
  return [...(holdings ?? [])]
    .sort((a, b) => (b.marketValue ?? 0) - (a.marketValue ?? 0))
    .slice(0, limit)
}

export async function buildPortfolioSummaryData() {
  const [summary, holdings] = await Promise.all([getPortfolioSummary(), getHoldings()])
  const top = toTopHoldings(holdings, 3)

  return {
    totalValue: formatCompactCurrency(summary.totalValue),
    totalGainLoss: formatCompactCurrency(summary.totalGainLoss),
    totalGainLossPct: formatPercent(summary.totalGainLossPct, { signed: true }),
    holdingsCount: String(summary.holdingsCount ?? 0),
    topHoldings: top,
    symbol: top[0]?.symbol,
  }
}

export async function buildTopHoldingsData() {
  const holdings = await getHoldings()
  const top = toTopHoldings(holdings, 3)

  const rows = top.map((h) => {
    const sign = (h.gainLossPct ?? 0) >= 0 ? '▲' : '▼'
    const perfLabel = (h.gainLossPct ?? 0) >= 0 ? 'Gain' : 'Loss'
    return `• ${h.symbol} - ${formatCompactCurrency(h.marketValue)} (${sign} ${perfLabel} ${formatPercent(h.gainLossPct, { signed: true })})`
  })

  const holdingsCard = top.length
    ? ['Your Current Holdings', `Total: ${holdings.length} position(s)`, ...rows].join('\n')
    : 'Your Current Holdings\nTotal: 0 position(s)\nNo open positions yet.'

  return {
    topHoldings: top.length === 0 ? 'none yet' : top.map((h) => `${h.symbol} (${formatCompactCurrency(h.marketValue)})`).join(', '),
    holdingsCard,
    positionsCount: String(holdings.length),
    symbol: top[0]?.symbol,
  }
}

export async function buildRiskData(context) {
  const symbol = context.lastSymbol || context.defaultSymbol || 'AAPL'
  const risk = await getRisk(symbol)
  return {
    symbol,
    recommendation: String(risk.recommendation ?? 'HOLD'),
    annualizedVolatility: formatRatioAsPercent(risk.annualizedVolatility),
    annualizedReturn: formatRatioAsPercent(risk.annualizedReturn, { signed: true }),
    maxDrawdown: formatRatioAsPercent(risk.maxDrawdown),
    sharpeRatio: formatNumber(risk.sharpeRatio),
    beta: risk.beta == null ? '—' : formatNumber(risk.beta),
    rsi14: risk.rsi14 == null ? '—' : formatNumber(risk.rsi14, 1),
    trend: risk.trend ?? 'n/a',
    rationale: risk.rationale ?? '',
    disclaimer: risk.disclaimer ?? '',
  }
}

export async function buildMarketNewsData(context) {
  const symbol = context.lastSymbol || context.defaultSymbol || 'AAPL'
  const news = await getNews(symbol)
  const headlines = (news ?? []).slice(0, 3).map((item) => item.headline)
  return {
    symbol,
    headlines: headlines.length ? headlines.join(' | ') : 'No fresh headlines at the moment.',
  }
}

export async function buildRebalanceData() {
  const holdings = await getHoldings()
  const top = toTopHoldings(holdings, 3)

  if (top.length === 0) {
    return {
      rebalanceSummary: 'No holdings available yet. Add positions first, then rebalance by target weights.',
      rebalanceDetail: 'Once you have holdings, compare current weights to target ranges and trim overweight positions first.',
    }
  }

  const total = top.reduce((sum, item) => sum + (item.marketValue ?? 0), 0)
  const lead = top[0]
  const leadWeight = total > 0 ? ((lead.marketValue ?? 0) / total) * 100 : 0

  return {
    rebalanceSummary: `${lead.symbol} is your largest concentration at ${leadWeight.toFixed(1)}% of your top-3 basket. Consider trimming 2-5% and spreading to uncorrelated sectors.`,
    rebalanceDetail:
      'Practical steps: define target sector weights, trim any single position above your cap, and add exposure gradually in 2-3 staggered trades to reduce timing risk.',
  }
}

async function buildPortfolioContextBrief() {
  const [summary, holdings] = await Promise.all([getPortfolioSummary(), getHoldings()])
  const top = toTopHoldings(holdings, 3)

  const topText = top.length
    ? top.map((h) => `${h.symbol} (${formatCompactCurrency(h.marketValue)})`).join(', ')
    : 'none'

  return [
    `Total portfolio value: ${formatCompactCurrency(summary.totalValue)}`,
    `Total gain/loss: ${formatCompactCurrency(summary.totalGainLoss)} (${formatPercent(summary.totalGainLossPct, { signed: true })})`,
    `Holdings count: ${summary.holdingsCount ?? 0}`,
    `Top holdings: ${topText}`,
  ].join('\n')
}

export async function answerGeneralQuestion(question) {
  const apiKey = String(import.meta.env.VITE_GROQ_API_KEY ?? '').trim()
  if (!apiKey) return null

  const portfolioContext = await buildPortfolioContextBrief().catch(() => 'Portfolio context unavailable.')

  const response = await fetch(GROQ_API_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${apiKey}`,
    },
    body: JSON.stringify({
      model: GROQ_MODEL,
      temperature: 0.2,
      max_tokens: 220,
      messages: [
        {
          role: 'system',
          content:
            'You are Pixel AI, an educational portfolio assistant. Give clear, concise answers. Use portfolio context when relevant. Do not give guaranteed-return claims or personalized financial advice. If asked for risky or uncertain actions, provide risk-aware educational guidance.',
        },
        {
          role: 'system',
          content: `User portfolio snapshot:\n${portfolioContext}`,
        },
        {
          role: 'user',
          content: question,
        },
      ],
    }),
  })

  if (!response.ok) {
    return null
  }

  const data = await response.json()
  const text = data?.choices?.[0]?.message?.content
  if (!text) return null
  return String(text).trim()
}
