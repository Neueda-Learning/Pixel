import { useEffect, useMemo, useRef } from 'react'
import './MessageList.css'

function timeLabel(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' })
}

export default function MessageList({ messages, isTyping, error }) {
  const scrollRef = useRef(null)

  const hasMessages = messages.length > 0
  const stableMessages = useMemo(() => messages, [messages])

  useEffect(() => {
    const node = scrollRef.current
    if (!node) return
    node.scrollTop = node.scrollHeight
  }, [stableMessages, isTyping])

  return (
    <div className="chat-message-list" ref={scrollRef} aria-live="polite" aria-label="Conversation messages">
      {!hasMessages && (
        <div className="chat-empty-state">
          <h4>No messages yet</h4>
          <p>Start with a quick chip or ask for your portfolio summary.</p>
        </div>
      )}

      {stableMessages.map((message) => (
        <article key={message.id} className={`chat-row chat-row-${message.role}`}>
          {message.role === 'bot' && <span className="chat-avatar" aria-hidden="true">🤖</span>}
          <div className={`chat-bubble chat-bubble-${message.role}`}>
            <p>{message.text}</p>
            <time dateTime={message.timestamp}>{timeLabel(message.timestamp)}</time>
          </div>
        </article>
      ))}

      {isTyping && (
        <article className="chat-row chat-row-bot" aria-label="Bot typing">
          <div className="chat-bubble chat-bubble-bot chat-typing-indicator">
            <span />
            <span />
            <span />
          </div>
        </article>
      )}

      {error && <p className="chat-error-state">{error}</p>}
    </div>
  )
}
