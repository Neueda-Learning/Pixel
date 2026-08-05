export default function ErrorState({ message = 'Something went wrong.', onRetry }) {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'flex-start',
        gap: 'var(--space-3)',
        padding: 'var(--space-5)',
        color: 'var(--text-secondary)',
      }}
    >
      <span>{message}</span>
      {onRetry && (
        <button className="btn" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  )
}

export function extractErrorMessage(err, fallback = 'Something went wrong.') {
  return err?.response?.data?.message || err?.message || fallback
}
