const STYLES = {
  BUY: { cls: 'badge-good', label: 'BUY' },
  HOLD: { cls: 'badge-warn', label: 'HOLD' },
  AVOID: { cls: 'badge-bad', label: 'AVOID' },
}

export default function RecommendationBadge({ recommendation }) {
  const style = STYLES[recommendation] || STYLES.HOLD
  return (
    <span className={`badge ${style.cls}`}>
      <span className="badge-dot" />
      {style.label}
    </span>
  )
}
