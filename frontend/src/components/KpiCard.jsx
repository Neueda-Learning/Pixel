import './KpiCard.css'

export default function KpiCard({ label, value, delta, deltaPositive, valuePositive, icon, list }) {
  return (
    <div className={`card card-pad kpi-card${list ? ' kpi-card-with-list' : ''}`}>
      <div className="kpi-card-main">
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

      {list && list.length > 0 && (
        <div className="kpi-card-list">
          {list.map((item) => (
            <div className="kpi-card-list-row" key={item.symbol}>
              <span className="kpi-card-list-symbol">{item.symbol}</span>
              <span className={item.positive ? 'text-positive' : 'text-negative'}>
                {item.positive ? '▲ ' : '▼ '}
                {item.changeLabel}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
