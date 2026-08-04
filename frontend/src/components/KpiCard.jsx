import './KpiCard.css'

export default function KpiCard({ label, value, delta, deltaPositive, icon }) {
  return (
    <div className="card card-pad kpi-card">
      <div className="kpi-card-top">
        <span className="kpi-card-label">{label}</span>
        {icon && <span className="kpi-card-icon">{icon}</span>}
      </div>
      <div className="kpi-card-value tabular">{value}</div>
      {delta != null && (
        <div className={`kpi-card-delta ${deltaPositive ? 'text-positive' : 'text-negative'}`}>
          {deltaPositive ? '▲' : '▼'} {delta}
        </div>
      )}
    </div>
  )
}
