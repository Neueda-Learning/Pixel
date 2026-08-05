import './KpiCard.css'

export default function KpiCard({ label, value, delta, deltaPositive, valuePositive, icon }) {
  return (
    <div className="card card-pad kpi-card">
      <div className="kpi-card-top">
        <span className="kpi-card-label">{label}</span>
        {icon && <span className="kpi-card-icon">{icon}</span>}
      </div>
      <div
        className={`kpi-card-value tabular ${
          valuePositive != null ? (valuePositive ? 'text-positive' : 'text-negative') : ''
        }`}
      >
        {valuePositive != null && (valuePositive ? '▲ ' : '▼ ')}
        {value}
      </div>
      {delta != null && (
        <div className={`kpi-card-delta ${deltaPositive ? 'text-positive' : 'text-negative'}`}>
          {deltaPositive ? '▲' : '▼'} {delta}
        </div>
      )}
    </div>
  )
}
