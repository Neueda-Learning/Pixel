# Rule-Based Portfolio Chatbot

Reusable, production-ready chat module for a portfolio app.

## What this module includes

- Floating launcher and expandable panel (desktop + mobile)
- Header with online status and close behavior
- Scrollable conversation area with timestamps
- User/bot bubbles, bot typing indicator, quick-reply chips
- Enter to send, Shift+Enter newline, Escape to close
- localStorage persistence for messages and session context
- Deterministic rule-based bot engine (no LLM, no AI APIs)
- Guardrails: fallback, invalid/empty input, max message length
- Live-data integration from portfolio/risk/market endpoints with graceful fallback on errors

## Folder structure

```text
src/
  chatbot/
    README.md
    chatbotRules.js
    intentMatcher.js
    chatEngine.js
    storage.js
    services/
      chatDataService.js
  components/
    chatbot/
      ChatWidget.jsx
      ChatWidget.css
      MessageList.jsx
      MessageList.css
      MessageInput.jsx
      MessageInput.css
```

## Integration steps

1. Copy the folders above into your target project.
2. Wire data-service functions in `src/chatbot/services/chatDataService.js` to your own API layer.
3. Mount `<ChatWidget />` once at app-shell level (for global availability).
4. Ensure base tokens/classes exist (`--accent`, `--bg-surface`, `.btn`, `.icon-btn`, `.live-dot`).
5. Run tests to validate matcher and fallback behavior.

## Intent config model

Each intent entry in `chatbotRules.js` supports:

- `id`
- `patterns` (regex array)
- `keywords`
- `confidence` scoring rules
- `responseTemplates`
- optional `errorTemplates`
- optional `detailTemplate`
- optional `action` handler key

Matcher strategy in `intentMatcher.js`:

1. Normalize input (lowercase, trim, punctuation removal, compact spaces)
2. Score regex matches and keyword overlap
3. Pick highest-scoring intent
4. Apply threshold, else fallback intent

## Add a new intent example

Add this block to `chatbotRules.js`:

```js
{
  id: 'dividend_insight',
  patterns: [/\b(dividend|yield|payout)\b/i],
  keywords: ['dividend', 'yield', 'payout'],
  confidence: {
    regexBoost: 0.82,
    keywordWeight: 0.3,
    threshold: 0.56,
  },
  action: 'dividendInsight',
  responseTemplates: [
    'Dividend view for {{symbol}}: yield {{yield}}, payout ratio {{payoutRatio}}.',
  ],
  errorTemplates: [
    'I could not load dividend data right now. Please try again later.',
  ],
}
```

Then:

1. Add `dividendInsight` to `actionHandlers` in `chatEngine.js`.
2. Implement `buildDividendData` in `services/chatDataService.js`.
3. Add tests for matching and fallback.

## Example tests and expected outputs

- Input: `Hey there`
  Expected intent: `greeting`
  Expected: not fallback

- Input: `Can I get my portfolio summary?`
  Expected intent: `portfolio_summary`
  Expected output contains placeholders resolved from live data

- Input: `zxy qwr ptt`
  Expected intent: `fallback`
  Expected fallback guidance message

- Input sequence:
  1) `top risk`
  2) `yes`
  Expected: second message returns detail template for last risk payload
