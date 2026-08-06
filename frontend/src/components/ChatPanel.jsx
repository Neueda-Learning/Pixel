import { useState } from 'react'
import { sendChatMessage } from '../api/chat'
import { extractErrorMessage } from './ErrorState'
import './ChatPanel.css'

export default function ChatPanel() {
  const [open, setOpen] = useState(false)
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  async function handleSubmit(e) {
    e.preventDefault()
    const text = input.trim()
    if (!text || loading) return

    setMessages((prev) => [...prev, { role: 'user', text }])
    setInput('')
    setLoading(true)
    setError(null)

    try {
      const { reply } = await sendChatMessage(text)
      setMessages((prev) => [...prev, { role: 'assistant', text: reply }])
    } catch (err) {
      setError(extractErrorMessage(err, 'Could not reach the assistant.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="chat-panel-root">
      {open && (
        <div className="chat-panel">
          <div className="chat-panel-header">
            <span>Portfolio Assistant</span>
            <button className="chat-panel-close" onClick={() => setOpen(false)} aria-label="Close chat">
              ×
            </button>
          </div>

          <div className="chat-panel-messages">
            {messages.length === 0 && !loading && (
              <p className="text-muted chat-panel-empty">
                Try "What's my portfolio worth?", "What's my best performer?", "Should I rebalance?", or "What's the
                risk on AAPL?"
              </p>
            )}
            {messages.map((m, i) => (
              <div key={i} className={`chat-bubble chat-bubble-${m.role}`}>
                {m.text}
              </div>
            ))}
            {loading && <div className="chat-bubble chat-bubble-assistant chat-bubble-loading">Thinking…</div>}
            {error && <p className="chat-panel-error">{error}</p>}
          </div>

          <form className="chat-panel-input" onSubmit={handleSubmit}>
            <input
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="Ask a question…"
              disabled={loading}
            />
            <button className="btn" type="submit" disabled={loading || !input.trim()}>
              Send
            </button>
          </form>
        </div>
      )}

      <button className="chat-panel-toggle" onClick={() => setOpen((o) => !o)} aria-label="Toggle portfolio assistant">
        {open ? (
          '×'
        ) : (
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path
              d="M4 5h16v11H8l-4 4V5z"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        )}
      </button>
    </div>
  )
}
