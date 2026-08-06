export const MAX_MESSAGE_LENGTH = 500
export const INTENT_THRESHOLD = 0.55

export const STARTER_CHIPS = [
  'What are my holdings?',
  'Portfolio summary',
  'Top risk',
  'What is a trade?',
]

export const chatbotRules = [
  {
    id: 'greeting',
    patterns: [/\b(hi|hello|hey|good\s+(morning|afternoon|evening))\b/i],
    keywords: ['hi', 'hello', 'hey', 'good morning', 'good afternoon', 'good evening'],
    confidence: {
      regexBoost: 0.78,
      keywordWeight: 0.34,
      threshold: 0.45,
    },
    responseTemplates: [
      'Hi there. I can help with portfolio summaries, risk checks, market news, and rebalance tips.',
      'Hello. Ask me about your holdings, portfolio risk, market headlines, or rebalancing ideas.',
    ],
  },
  {
    id: 'help',
    patterns: [/\b(help|what\s+can\s+you\s+do|commands|options)\b/i],
    keywords: ['help', 'what can you do', 'commands', 'options'],
    confidence: {
      regexBoost: 0.8,
      keywordWeight: 0.3,
      threshold: 0.5,
    },
    responseTemplates: [
      'I can answer: portfolio summary, top holdings, risk analysis, market news, rebalance suggestions, and transaction help.',
      'Try one of these: "portfolio summary", "top holdings", "top risk", "market news", "rebalance tips", or "transaction help".',
    ],
  },
  {
    id: 'holdings_overview',
    patterns: [
      /\b(what\s+are\s+my\s+holdings|my\s+holdings|show\s+my\s+portfolio\s+holdings)\b/i,
      /\b(current\s+holdings|positions\s+overview)\b/i,
    ],
    keywords: ['holdings', 'positions', 'portfolio holdings', 'current holdings'],
    confidence: {
      regexBoost: 0.86,
      keywordWeight: 0.34,
      threshold: 0.56,
    },
    action: 'topHoldings',
    responseTemplates: [
      '{{holdingsCard}}',
      '{{holdingsCard}}',
    ],
    errorTemplates: ['I could not load your holdings right now. Please retry in a moment.'],
  },
  {
    id: 'portfolio_summary',
    patterns: [
      /\b(portfolio\s+summary|account\s+summary|summary\s+of\s+my\s+portfolio)\b/i,
      /\b(total\s+portfolio\s+value|overall\s+gain|pnl)\b/i,
    ],
    keywords: ['portfolio', 'summary', 'total value', 'gain', 'loss'],
    confidence: {
      regexBoost: 0.84,
      keywordWeight: 0.34,
      threshold: 0.55,
    },
    action: 'portfolioSummary',
    responseTemplates: [
      'Portfolio snapshot: total value {{totalValue}}, gain/loss {{totalGainLoss}} ({{totalGainLossPct}}), holdings {{holdingsCount}}.',
      'Current portfolio summary: value {{totalValue}} across {{holdingsCount}} holdings, with total P/L of {{totalGainLoss}} ({{totalGainLossPct}}).',
    ],
    errorTemplates: [
      'I could not load your latest portfolio summary right now. Please try again in a moment.',
    ],
  },
  {
    id: 'top_holdings',
    patterns: [/\b(top\s+holdings|largest\s+positions|biggest\s+holdings|main\s+positions)\b/i],
    keywords: ['top holdings', 'largest', 'positions', 'holdings'],
    confidence: {
      regexBoost: 0.82,
      keywordWeight: 0.32,
      threshold: 0.55,
    },
    action: 'topHoldings',
    responseTemplates: [
      '{{holdingsCard}}',
      '{{holdingsCard}}',
    ],
    errorTemplates: ['I could not fetch holdings right now. Please retry shortly.'],
  },
  {
    id: 'risk_analysis',
    patterns: [/\b(top\s+risk|risk\s+analysis|risk\s+level|volatility|drawdown|sharpe)\b/i],
    keywords: ['risk', 'volatility', 'drawdown', 'sharpe', 'beta'],
    confidence: {
      regexBoost: 0.86,
      keywordWeight: 0.34,
      threshold: 0.58,
    },
    action: 'riskAnalysis',
    responseTemplates: [
      'Risk check for {{symbol}}: {{recommendation}}. Volatility {{annualizedVolatility}}, max drawdown {{maxDrawdown}}, Sharpe {{sharpeRatio}}. {{rationale}}',
      'For {{symbol}}, risk view is {{recommendation}} with volatility {{annualizedVolatility}} and drawdown {{maxDrawdown}}. {{rationale}}',
    ],
    detailTemplate:
      'More detail for {{symbol}}: annual return {{annualizedReturn}}, beta {{beta}}, RSI(14) {{rsi14}}, trend {{trend}}. {{disclaimer}}',
    errorTemplates: ['I was not able to load risk metrics right now. Please try again soon.'],
  },
  {
    id: 'market_news',
    patterns: [/\b(market\s+news|news\s+update|headlines|latest\s+news)\b/i],
    keywords: ['market', 'news', 'headlines', 'latest'],
    confidence: {
      regexBoost: 0.8,
      keywordWeight: 0.32,
      threshold: 0.55,
    },
    action: 'marketNews',
    responseTemplates: [
      'Latest market headlines for {{symbol}}: {{headlines}}',
      'Recent {{symbol}} news: {{headlines}}',
    ],
    errorTemplates: ['I could not retrieve market headlines right now. Please try again in a bit.'],
  },
  {
    id: 'rebalance_suggestion',
    patterns: [/\b(rebalance|rebalance\s+tips|allocation\s+advice|diversif(y|ication))\b/i],
    keywords: ['rebalance', 'allocation', 'diversify', 'diversification', 'weights'],
    confidence: {
      regexBoost: 0.84,
      keywordWeight: 0.34,
      threshold: 0.58,
    },
    action: 'rebalanceSuggestion',
    responseTemplates: [
      'Rebalance idea: {{rebalanceSummary}} Would you like more details?',
      'Suggested allocation tweak: {{rebalanceSummary}} Want a more detailed breakdown?',
    ],
    detailTemplate: '{{rebalanceDetail}}',
    errorTemplates: ['I could not calculate rebalance suggestions right now. Please retry shortly.'],
  },
  {
    id: 'trade_basics',
    patterns: [
      /\b(what\s+is\s+a\s+trade|what\s+is\s+trading|define\s+trade|what\s+is\s+a\s+stock)\b/i,
      /\b(what\s+is\s+an\s+etf|what\s+is\s+portfolio\s+diversification)\b/i,
    ],
    keywords: ['trade', 'trading', 'stock', 'etf', 'diversification', 'investing basics'],
    confidence: {
      regexBoost: 0.8,
      keywordWeight: 0.34,
      threshold: 0.56,
    },
    responseTemplates: [
      'A trade is a buy or sell order for an asset such as a stock or ETF. In this app, each trade updates your transaction ledger, and your holdings are derived from that history.',
      'Trading means exchanging assets (like stocks or ETFs) by buying or selling. Your portfolio value, gains/losses, and risk metrics in Pixel are calculated from these recorded trades.',
    ],
  },
  {
    id: 'transaction_help',
    patterns: [/\b(transaction\s+help|how\s+to\s+(buy|sell)|add\s+transaction|record\s+trade)\b/i],
    keywords: ['transaction', 'buy', 'sell', 'trade', 'fees'],
    confidence: {
      regexBoost: 0.8,
      keywordWeight: 0.34,
      threshold: 0.55,
    },
    responseTemplates: [
      'To add a transaction, open Transactions, choose BUY or SELL, enter symbol, quantity, price, and date, then submit.',
      'For SELL transactions, choose the source buy lot first, then enter quantity and sell price. Need a step-by-step checklist?',
    ],
    detailTemplate:
      'Transaction checklist: 1) Confirm symbol, 2) Set BUY/SELL, 3) Add quantity and price, 4) For SELL choose buy lot, 5) Verify fees/date, 6) Submit.',
  },
  {
    id: 'fallback',
    patterns: [],
    keywords: [],
    confidence: { threshold: 0 },
    responseTemplates: [
      'I did not fully understand that. Try: portfolio summary, top holdings, top risk, market news, rebalance tips, or transaction help.',
      'I am not sure what you meant. You can ask about portfolio summary, risk analysis, market news, or rebalancing.',
    ],
  },
]

export const ruleById = Object.fromEntries(chatbotRules.map((rule) => [rule.id, rule]))
