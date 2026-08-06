import { chatbotRules, INTENT_THRESHOLD, ruleById } from './chatbotRules'

export function normalizeInput(input = '') {
  return input
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
}

function countKeywordMatches(normalizedInput, keywords = []) {
  if (!normalizedInput || keywords.length === 0) return 0
  return keywords.reduce((count, keyword) => {
    const token = normalizeInput(keyword)
    return token && normalizedInput.includes(token) ? count + 1 : count
  }, 0)
}

function countRegexMatches(text, patterns = []) {
  if (!text || patterns.length === 0) return 0
  return patterns.reduce((count, pattern) => (pattern.test(text) ? count + 1 : count), 0)
}

export function scoreIntent(rawInput, normalizedInput, rule) {
  const regexMatches = countRegexMatches(rawInput, rule.patterns)
  const keywordMatches = countKeywordMatches(normalizedInput, rule.keywords)
  const keywordCoverage = rule.keywords?.length ? keywordMatches / rule.keywords.length : 0

  const regexBoost = rule.confidence?.regexBoost ?? 0.75
  const keywordWeight = rule.confidence?.keywordWeight ?? 0.3

  return {
    score: regexMatches * regexBoost + keywordCoverage * keywordWeight,
    regexMatches,
    keywordMatches,
  }
}

export function matchIntent(input, options = {}) {
  const rawInput = String(input ?? '')
  const normalized = normalizeInput(rawInput)
  const rules = options.rules ?? chatbotRules.filter((rule) => rule.id !== 'fallback')
  const fallbackRule = options.fallbackRule ?? ruleById.fallback
  const globalThreshold = options.threshold ?? INTENT_THRESHOLD

  let best = { rule: fallbackRule, score: 0, regexMatches: 0, keywordMatches: 0 }

  for (const rule of rules) {
    const result = scoreIntent(rawInput, normalized, rule)
    if (result.score > best.score) {
      best = { rule, ...result }
    }
  }

  const threshold = Math.max(globalThreshold, best.rule.confidence?.threshold ?? 0)
  if (best.score < threshold) {
    return {
      intent: fallbackRule,
      score: best.score,
      normalized,
      usedFallback: true,
      diagnostics: {
        bestRuleId: best.rule.id,
        bestRegexMatches: best.regexMatches,
        bestKeywordMatches: best.keywordMatches,
        threshold,
      },
    }
  }

  return {
    intent: best.rule,
    score: best.score,
    normalized,
    usedFallback: false,
    diagnostics: {
      bestRuleId: best.rule.id,
      bestRegexMatches: best.regexMatches,
      bestKeywordMatches: best.keywordMatches,
      threshold,
    },
  }
}
