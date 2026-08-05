import { useState, useRef, useEffect } from 'react'
import { sendChat } from '../api/chatbot'
import './ChatBot.css'

const SUGGESTIONS = [
  'Show my portfolio',
  'What are my holdings?',
  'Best performer?',
  'Give me investment advice',
  'How risky is my portfolio?',
]

export default function ChatBot() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState([
    {
      role: 'bot',
      text: "👋 Hi! I'm **Pixel AI**, your portfolio assistant. Ask me anything about your holdings, stock prices, or investment insights!",
    },
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const bottomRef = useRef(null)
  const inputRef = useRef(null)

  useEffect(() => {
    if (open) {
      bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
      inputRef.current?.focus()
    }
  }, [open, messages])

  const send = async (text) => {
    const msg = (text || input).trim()
    if (!msg || loading) return
    setInput('')
    setMessages((prev) => [...prev, { role: 'user', text: msg }])
    setLoading(true)
    try {
      const { reply } = await sendChat(msg)
      setMessages((prev) => [...prev, { role: 'bot', text: reply }])
    } catch {
      setMessages((prev) => [...prev, { role: 'bot', text: 'Sorry, I could not process your request. Please try again.' }])
    } finally {
      setLoading(false)
    }
  }

  const handleKey = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send()
    }
  }

  return (
    <>
      {/* Floating button */}
      <button className={`chat-fab ${open ? 'open' : ''}`} onClick={() => setOpen((o) => !o)} aria-label="Open AI assistant">
        {open ? (
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M18 6 6 18M6 6l12 12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
          </svg>
        ) : (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path d="M12 2C6.48 2 2 6.48 2 12c0 1.54.36 3 .99 4.29L2 22l5.71-.99C9 21.64 10.46 22 12 22c5.52 0 10-4.48 10-10S17.52 2 12 2Z" fill="currentColor" opacity=".15"/>
            <path d="M12 2C6.48 2 2 6.48 2 12c0 1.54.36 3 .99 4.29L2 22l5.71-.99C9 21.64 10.46 22 12 22c5.52 0 10-4.48 10-10S17.52 2 12 2Z" stroke="currentColor" strokeWidth="1.8" strokeLinejoin="round"/>
            <circle cx="8.5" cy="12" r="1" fill="currentColor"/>
            <circle cx="12" cy="12" r="1" fill="currentColor"/>
            <circle cx="15.5" cy="12" r="1" fill="currentColor"/>
          </svg>
        )}
        <span className="chat-fab-badge">AI</span>
      </button>

      {/* Chat window */}
      {open && (
        <div className="chat-window">
          <div className="chat-header">
            <div className="chat-header-info">
              <div className="chat-avatar">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                  <path d="M12 2C6.48 2 2 6.48 2 12c0 1.54.36 3 .99 4.29L2 22l5.71-.99C9 21.64 10.46 22 12 22c5.52 0 10-4.48 10-10S17.52 2 12 2Z" fill="currentColor"/>
                </svg>
              </div>
              <div>
                <div className="chat-header-name">Pixel AI</div>
                <div className="chat-header-status">Portfolio Assistant</div>
              </div>
            </div>
            <button className="chat-close" onClick={() => setOpen(false)}>✕</button>
          </div>

          <div className="chat-messages">
            {messages.map((m, i) => (
              <div key={i} className={`chat-msg ${m.role}`}>
                <div className="chat-bubble" dangerouslySetInnerHTML={{ __html: formatMessage(m.text) }} />
              </div>
            ))}
            {loading && (
              <div className="chat-msg bot">
                <div className="chat-bubble chat-typing">
                  <span /><span /><span />
                </div>
              </div>
            )}
            <div ref={bottomRef} />
          </div>

          <div className="chat-suggestions">
            {SUGGESTIONS.map((s) => (
              <button key={s} className="chat-chip" onClick={() => send(s)}>
                {s}
              </button>
            ))}
          </div>

          <div className="chat-input-row">
            <input
              ref={inputRef}
              className="chat-input"
              placeholder="Ask about your portfolio..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKey}
              disabled={loading}
            />
            <button className="chat-send" onClick={() => send()} disabled={!input.trim() || loading}>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <path d="M22 2 11 13M22 2 15 22l-4-9-9-4 20-7Z" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
              </svg>
            </button>
          </div>
        </div>
      )}
    </>
  )
}

function formatMessage(text) {
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.*?)\*/g, '<em>$1</em>')
    .replace(/\n/g, '<br/>')
}
