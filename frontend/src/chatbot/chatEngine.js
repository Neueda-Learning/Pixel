import { chatbotRules, MAX_MESSAGE_LENGTH, ruleById } from './chatbotRules'
import { matchIntent, normalizeInput } from './intentMatcher'

const YES_PATTERN = /^(yes|y|sure|ok|okay|please|go ahead)$/i
const NO_PATTERN = /^(no|n|not now|nope|skip)$/i

const actionHandlers = {
  portfolioSummary: (services, context) => services.buildPortfolioSummaryData(context),
  topHoldings: (services, context) => services.buildTopHoldingsData(context),
  riskAnalysis: (services, context) => services.buildRiskData(context),
  marketNews: (services, context) => services.buildMarketNewsData(context),
  rebalanceSuggestion: (services, context) => services.buildRebalanceData(context),
}

export function createInitialSessionState() {
  return {
    turn: 0,
    lastIntent: null,
    lastSymbol: null,
    pendingDetailIntent: null,
    defaultSymbol: 'AAPL',
  }
}

function pickTemplate(intent, turn, key = 'responseTemplates') {
  const templates = intent[key] ?? []
  if (templates.length === 0) return ''
  return templates[turn % templates.length]
}

function renderTemplate(template, values = {}) {
  return template.replace(/{{\s*([a-zA-Z0-9_]+)\s*}}/g, (_, token) => {
    const value = values[token]
    return value == null || value === '' ? '—' : String(value)
  })
}

function normalizeResponseSpacing(text) {
  return String(text ?? '')
    .split('\n')
    .map((line) => line.replace(/\s+/g, ' ').trim())
    .filter((line, index, arr) => line.length > 0 || (index > 0 && arr[index - 1].length > 0))
    .join('\n')
    .trim()
}

async function resolveIntentResponse(intent, sessionState, services) {
  let payload = {}
  if (intent.action) {
    const action = actionHandlers[intent.action]
    if (typeof action !== 'function') {
      throw new Error(`Unknown action handler: ${intent.action}`)
    }
    payload = await action(services, sessionState)
  }

  const template = pickTemplate(intent, sessionState.turn)
  const message = normalizeResponseSpacing(renderTemplate(template, payload))

  return {
    intent,
    message,
    payload,
    setPendingDetailIntent: Boolean(intent.detailTemplate),
  }
}

function buildDetailResponse(intent, sessionState) {
  if (!intent?.detailTemplate || !sessionState?.lastPayload) return null
  return normalizeResponseSpacing(renderTemplate(intent.detailTemplate, sessionState.lastPayload))
}

export async function generateBotReply(userInput, sessionState, services) {
  const text = String(userInput ?? '')
  const trimmed = text.trim()
  const normalized = normalizeInput(text)
  const nextState = {
    ...(sessionState ?? createInitialSessionState()),
    turn: (sessionState?.turn ?? 0) + 1,
  }

  if (!trimmed) {
    return {
      text: 'Please type a question first.',
      intentId: 'fallback',
      nextState,
      error: 'empty_input',
    }
  }

  if (trimmed.length > MAX_MESSAGE_LENGTH) {
    return {
      text: `Please keep messages under ${MAX_MESSAGE_LENGTH} characters so I can respond accurately.`,
      intentId: 'fallback',
      nextState,
      error: 'max_length',
    }
  }

  if (nextState.pendingDetailIntent && YES_PATTERN.test(normalized)) {
    const detailIntent = ruleById[nextState.pendingDetailIntent]
    const detailText = buildDetailResponse(detailIntent, nextState)
    nextState.pendingDetailIntent = null
    nextState.lastIntent = detailIntent?.id ?? nextState.lastIntent
    return {
      text: detailText ?? 'I do not have additional detail right now.',
      intentId: detailIntent?.id ?? 'fallback',
      nextState,
    }
  }

  if (nextState.pendingDetailIntent && NO_PATTERN.test(normalized)) {
    nextState.pendingDetailIntent = null
    return {
      text: 'No problem. Ask another portfolio question whenever you are ready.',
      intentId: 'help',
      nextState,
    }
  }

  if (nextState.lastIntent && /\b(more\s+details?|details?)\b/i.test(normalized)) {
    const detailIntent = ruleById[nextState.lastIntent]
    const detailText = buildDetailResponse(detailIntent, nextState)
    if (detailText) {
      return {
        text: detailText,
        intentId: detailIntent.id,
        nextState,
      }
    }
  }

  const match = matchIntent(text, { rules: chatbotRules.filter((r) => r.id !== 'fallback') })
  const intent = match.intent

  if (intent.id === 'fallback') {
    if (typeof services.answerGeneralQuestion === 'function') {
      try {
        const generalAnswer = await services.answerGeneralQuestion(trimmed, nextState)
        if (generalAnswer) {
          nextState.lastIntent = 'general_qa'
          nextState.pendingDetailIntent = null
          return {
            text: generalAnswer,
            intentId: 'general_qa',
            nextState,
            diagnostics: match.diagnostics,
          }
        }
      } catch (_error) {
        // Fall through to deterministic fallback if external QA is unavailable.
      }
    }

    nextState.lastIntent = 'fallback'
    return {
      text: pickTemplate(intent, nextState.turn),
      intentId: 'fallback',
      nextState,
      diagnostics: match.diagnostics,
    }
  }

  try {
    const resolved = await resolveIntentResponse(intent, nextState, services)

    nextState.lastIntent = intent.id
    nextState.lastPayload = resolved.payload
    nextState.pendingDetailIntent = resolved.setPendingDetailIntent ? intent.id : null
    if (resolved.payload?.symbol) {
      nextState.lastSymbol = resolved.payload.symbol
    }

    return {
      text: resolved.message,
      intentId: intent.id,
      nextState,
      diagnostics: match.diagnostics,
    }
  } catch (_error) {
    const errorTemplate = pickTemplate(intent, nextState.turn, 'errorTemplates') || pickTemplate(ruleById.fallback, nextState.turn)
    nextState.lastIntent = intent.id
    nextState.pendingDetailIntent = null
    return {
      text: errorTemplate,
      intentId: intent.id,
      nextState,
      error: 'action_failed',
      diagnostics: match.diagnostics,
    }
  }
}
