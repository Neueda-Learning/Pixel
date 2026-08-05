import StatTile from './StatTile'
import RecommendationBadge from './RecommendationBadge'
import { formatNumber, formatRatioAsPercent } from '../utils/format'
import './RiskPanel.css'

export default function RiskPanel({ risk }) {
  if (!risk) return null

  return (
    <div className="risk-panel">
      <div className="risk-panel-header">
        <RecommendationBadge recommendation={risk.recommendation} />
        <span className="text-muted" style={{ fontSize: 12 }}>
          Based on {risk.dataPoints} trading days as of {risk.asOf}
        </span>
      </div>

      <p className="risk-rationale">{risk.rationale}</p>

      <div className="risk-tiles">
        <StatTile label="Volatility (ann.)" value={formatRatioAsPercent(risk.annualizedVolatility)} />
        <StatTile
          label="Return (ann.)"
          value={formatRatioAsPercent(risk.annualizedReturn, { signed: true })}
        />
        <StatTile label="Sharpe ratio" value={formatNumber(risk.sharpeRatio)} />
        <StatTile label="Max drawdown" value={formatRatioAsPercent(risk.maxDrawdown)} />
        <StatTile label="Beta vs SPY" value={risk.beta != null ? formatNumber(risk.beta) : '—'} />
        <StatTile label="RSI (14)" value={risk.rsi14 != null ? formatNumber(risk.rsi14, 1) : '—'} />
        <StatTile label="SMA 50" value={risk.sma50 != null ? `$${formatNumber(risk.sma50)}` : '—'} />
        <StatTile
          label="SMA 200 / Trend"
          value={risk.sma200 != null ? `$${formatNumber(risk.sma200)}` : '—'}
          hint={risk.trend}
        />
      </div>

      <p className="risk-disclaimer text-muted">{risk.disclaimer}</p>
    </div>
  )
}
