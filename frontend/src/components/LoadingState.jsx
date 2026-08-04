export default function LoadingState({ height = 200, label = 'Loading…' }) {
  return (
    <div style={{ padding: 'var(--space-2) 0' }}>
      <div className="skeleton" style={{ height, width: '100%' }} aria-busy="true" aria-label={label} />
    </div>
  )
}
