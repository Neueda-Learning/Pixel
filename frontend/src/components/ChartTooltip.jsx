import './ChartTooltip.css'

export default function ChartTooltip({ active, payload, label, formatter, labelFormatter }) {
  if (!active || !payload || !payload.length) return null
  return (
    <div className="chart-tooltip">
      {label != null && (
        <div className="chart-tooltip-label">{labelFormatter ? labelFormatter(label) : label}</div>
      )}
      {payload.map((entry, i) => (
        <div key={i} className="chart-tooltip-row">
          <span className="chart-tooltip-dot" style={{ background: entry.color || entry.payload?.fill }} />
          <span className="chart-tooltip-name">{entry.name}</span>
          <span className="chart-tooltip-value tabular">
            {formatter ? formatter(entry.value, entry) : entry.value}
          </span>
        </div>
      ))}
    </div>
  )
}
