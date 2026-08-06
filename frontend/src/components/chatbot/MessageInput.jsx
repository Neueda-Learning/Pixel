import './MessageInput.css'

export default function MessageInput({ value, onChange, onSend, disabled, maxLength = 500 }) {
  const handleKeyDown = (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault()
      onSend()
    }
  }

  return (
    <div className="chat-input-wrap">
      <div className="chat-input-row">
        <textarea
          className="chat-input"
          value={value}
          onChange={(event) => onChange(event.target.value)}
          onKeyDown={handleKeyDown}
          rows={2}
          maxLength={maxLength}
          placeholder="Ask about your holdings, market, or trading basics..."
          aria-label="Chat message"
          disabled={disabled}
        />
        <button
          className="chat-send-btn"
          onClick={onSend}
          disabled={disabled || !value.trim()}
          aria-label="Send message"
          title="Send"
          type="button"
        >
          ➤
        </button>
      </div>
    </div>
  )
}
