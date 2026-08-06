import { useEffect, useMemo, useState } from 'react'
import MessageList from './MessageList'
import MessageInput from './MessageInput'
import { STARTER_CHIPS } from '../../chatbot/chatbotRules'
import { createInitialSessionState, generateBotReply } from '../../chatbot/chatEngine'
import {
  answerGeneralQuestion,
  buildMarketNewsData,
  buildPortfolioSummaryData,
  buildRebalanceData,
  buildRiskData,
  buildTopHoldingsData,
} from '../../chatbot/services/chatDataService'
import { clearChatState, loadChatState, saveChatState } from '../../chatbot/storage'
import './ChatWidget.css'

const BOT_SERVICES = {
  answerGeneralQuestion,
  buildPortfolioSummaryData,
  buildTopHoldingsData,
  buildRiskData,
  buildMarketNewsData,
  buildRebalanceData,
}

function createMessage(role, text, meta = {}) {
  return {
    id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
    role,
    text,
    timestamp: new Date().toISOString(),
    ...meta,
  }
}

function formatRiskReply(text) {
  const symbolMatch = text.match(/for\s+([A-Z.]{1,10}),/i) || text.match(/risk check for\s+([A-Z.]{1,10}):/i)
  const recommendationMatch = text.match(/risk view is\s+([A-Z]+)/i) || text.match(/:\s*([A-Z]+)\./i)
  const volMatch = text.match(/volatility\s+([0-9.%-]+)/i)
  const drawdownMatch = text.match(/drawdown\s+(-?[0-9.%-]+)/i)
  const sharpeMatch = text.match(/sharpe\s+([a-z\s]*?)?([0-9]+\.?[0-9]*)/i)

  const symbol = symbolMatch?.[1]
  const recommendation = recommendationMatch?.[1]
  const volatility = volMatch?.[1]
  const drawdown = drawdownMatch?.[1]
  const sharpe = sharpeMatch?.[2]

  if (!symbol || !recommendation || !volatility || !drawdown || !sharpe) {
    return text
  }

  const rationale = text
    .replace(/.*?\.\s*/s, '')
    .trim()

  return [
    `Risk Summary - ${symbol}`,
    '----------------',
    `Recommendation: ${recommendation}`,
    `Volatility: ${volatility}`,
    `Max Drawdown: ${drawdown}`,
    `Sharpe Ratio: ${sharpe}`,
    rationale ? `Why: ${rationale}` : '',
  ]
    .filter(Boolean)
    .join('\n')
}

export default function ChatWidget() {
  const [isOpen, setIsOpen] = useState(false)
  const [messages, setMessages] = useState([])
  const [draft, setDraft] = useState('')
  const [isTyping, setIsTyping] = useState(false)
  const [sessionState, setSessionState] = useState(createInitialSessionState)
  const [lastError, setLastError] = useState('')
  const [isHydrated, setIsHydrated] = useState(false)

  useEffect(() => {
    const stored = loadChatState()
    setMessages(stored.messages)
    setSessionState(stored.sessionState)
    setIsHydrated(true)
  }, [])

  useEffect(() => {
    if (!isHydrated) return
    saveChatState({ messages, sessionState })
  }, [isHydrated, messages, sessionState])

  useEffect(() => {
    const onKeyDown = (event) => {
      if (event.key === 'Escape') setIsOpen(false)
    }
    if (isOpen) {
      window.addEventListener('keydown', onKeyDown)
    }
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [isOpen])

  useEffect(() => {
    const onBeforeUnload = () => {
      clearChatState()
    }

    window.addEventListener('beforeunload', onBeforeUnload)
    return () => window.removeEventListener('beforeunload', onBeforeUnload)
  }, [])

  const quickReplies = useMemo(() => STARTER_CHIPS, [])

  const sendMessage = async (raw) => {
    const text = (raw ?? draft).trim()
    if (!text) return

    setDraft('')
    setLastError('')

    const userMessage = createMessage('user', text)
    setMessages((prev) => [...prev, userMessage])

    setIsTyping(true)
    try {
      const reply = await generateBotReply(text, sessionState, BOT_SERVICES)

      await new Promise((resolve) => {
        window.setTimeout(resolve, 450)
      })

      const formattedText = reply.intentId === 'risk_analysis' ? formatRiskReply(reply.text) : reply.text

      const botMessage = createMessage('bot', formattedText, {
        intentId: reply.intentId,
        diagnostics: reply.diagnostics,
      })

      setMessages((prev) => [...prev, botMessage])
      setSessionState(reply.nextState)
      if (reply.error === 'action_failed') {
        setLastError('Some live data was unavailable. You are seeing a graceful fallback response.')
      }
    } catch (_error) {
      setMessages((prev) => [
        ...prev,
        createMessage(
          'bot',
          'Something went wrong while processing that message. Please try again, or ask for help to see supported commands.',
          { intentId: 'fallback' },
        ),
      ])
      setLastError('Unexpected error while generating a response.')
    } finally {
      setIsTyping(false)
    }
  }

  const openAndSeed = () => {
    setIsOpen(true)
    if (messages.length === 0) {
      setMessages([
        createMessage(
          'bot',
          "Hi! I'm Pixel AI, your portfolio assistant. Ask me about your holdings, stock prices, risk, or trading basics.",
          { intentId: 'greeting' },
        ),
      ])
    }
  }

  return (
    <>
      <button
        className="chat-launcher"
        type="button"
        onClick={openAndSeed}
        aria-label="Open portfolio assistant"
      >
        <span className="chat-launcher-icon" aria-hidden="true">🤖</span>
        <span className="chat-launcher-badge">AI</span>
      </button>

      <section className={`chat-panel ${isOpen ? 'chat-panel-open' : ''}`} aria-hidden={!isOpen}>
        <header className="chat-header">
          <div className="chat-title-wrap">
            <span className="chat-header-logo" aria-hidden="true">🤖</span>
            <div>
              <h3>Pixel AI</h3>
              <p>Portfolio Assistant</p>
            </div>
          </div>
          <button className="icon-btn chat-close-btn" type="button" onClick={() => setIsOpen(false)} aria-label="Close chat">
            ×
          </button>
        </header>

        <MessageList messages={messages} isTyping={isTyping} error={lastError} />

        <div className="chat-quick-replies chat-quick-replies-bottom" aria-label="Quick replies">
          {quickReplies.map((chip) => (
            <button key={chip} type="button" className="chat-chip" onClick={() => sendMessage(chip)} disabled={isTyping}>
              {chip}
            </button>
          ))}
        </div>

        <MessageInput
          value={draft}
          onChange={setDraft}
          onSend={() => sendMessage()}
          disabled={isTyping}
          maxLength={500}
        />
      </section>
    </>
  )
}
