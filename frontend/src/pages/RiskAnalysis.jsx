import { useState } from 'react'
import useApi from '../hooks/useApi'
import { getHoldings } from '../api/portfolio'
import { getRisk } from '../api/risk'
import RiskPanel from '../components/RiskPanel'
import LoadingState from '../components/LoadingState'
import ErrorState, { extractErrorMessage } from '../components/ErrorState'
import { formatCurrency, formatPercent } from '../utils/format'
import './RiskAnalysis.css'

function RiskBadge({ level }) {
  const map = {
    BUY: { cls: 'good', label: '🟢 BUY' },
    HOLD: { cls: 'warn', label: '🟡 HOLD' },
    AVOID: { cls: 'bad', label: '🔴 AVOID' },
  }
  const { cls, label } = map[level] || map.HOLD
  return <span className={`risk-badge risk-badge--${cls}`}>{label}</span>
}

function HoldingRiskCard({ holding }) {
  const risk = useApi(() => getRisk(holding.symbol), [holding.symbol])
  const positive = holding.gainLoss >= 0 || (holding.gainLoss?.toNumber?.() ?? holding.gainLoss) >= 0
  const gl = typeof holding.gainLoss === 'object' ? holding.gainLoss.toFixed(2) : Number(holding.gainLoss).toFixed(2)
  const glPct = typeof holding.gainLossPct === 'object' ? holding.gainLossPct.toFixed(2) : Number(holding.gainLossPct).toFixed(2)

  return (
    <div className="risk-card">
      <div className="risk-card-header">
        <div>
          <div className="risk-card-symbol">{holding.symbol}</div>
          <div className="risk-card-name">{holding.name}</div>
        </div>
        <div className="risk-card-stats">
          <span className="risk-card-type">{holding.assetType}</span>
          <span className={`risk-card-gl ${positive ? 'pos' : 'neg'}`}>
            {positive ? '▲' : '▼'} {formatCurrency(Math.abs(gl))} ({Math.abs(glPct)}%)
          </span>
          {!risk.loading && !risk.error && risk.data?.recommendation && (
            <RiskBadge level={risk.data.recommendation} />
          )}
        </div>
      </div>
      {risk.loading ? (
        <LoadingState height={140} />
      ) : risk.error ? (
        <ErrorState message={extractErrorMessage(risk.error, 'Could not load risk data.')} onRetry={risk.reload} />
      ) : (
        <RiskPanel risk={risk.data} />
      )}
    </div>
  )
}

export default function RiskAnalysis() {
  const holdings = useApi(getHoldings, [])

  const items = holdings.data || []
  const totalValue = items.reduce((s, h) => s + Number(h.marketValue), 0)
  const atRisk = items.filter((h) => Number(h.gainLoss) < 0)
  const profitable = items.filter((h) => Number(h.gainLoss) >= 0)

  return (
    <div className="risk-analysis">
      <div className="risk-analysis-hero">
        <div>
          <h2 className="risk-analysis-title">Portfolio Risk Analysis</h2>
          <p className="risk-analysis-subtitle">
            Rule-based risk metrics computed from historical price data. Not financial advice.
          </p>
        </div>
      </div>

      {/* Summary strip */}
      {!holdings.loading && items.length > 0 && (
        <div className="risk-summary-strip">
          <div className="risk-strip-item">
            <div className="risk-strip-value">{items.length}</div>
            <div className="risk-strip-label">Total Positions</div>
          </div>
          <div className="risk-strip-item">
            <div className="risk-strip-value text-positive">{profitable.length}</div>
            <div className="risk-strip-label">Profitable</div>
          </div>
          <div className="risk-strip-item">
            <div className="risk-strip-value text-negative">{atRisk.length}</div>
            <div className="risk-strip-label">At a Loss</div>
          </div>
          <div className="risk-strip-item">
            <div className="risk-strip-value">{formatCurrency(totalValue)}</div>
            <div className="risk-strip-label">Portfolio Value</div>
          </div>
        </div>
      )}

      {/* Per-holding risk panels */}
      <div className="risk-cards-grid">
        {holdings.loading ? (
          <>
            <LoadingState height={280} />
            <LoadingState height={280} />
            <LoadingState height={280} />
          </>
        ) : holdings.error ? (
          <ErrorState
            message={extractErrorMessage(holdings.error, 'Could not load holdings.')}
            onRetry={holdings.reload}
          />
        ) : items.length === 0 ? (
          <div className="card card-pad">
            <p className="text-muted">No holdings yet — add transactions to see risk analysis.</p>
          </div>
        ) : (
          items.map((h) => <HoldingRiskCard key={h.symbol} holding={h} />)
        )}
      </div>

      {/* Risk education */}
      <div className="card card-pad risk-education">
        <div className="card-title" style={{ marginBottom: 16 }}>Understanding the Metrics</div>
        <div className="risk-edu-grid">
          <div className="risk-edu-item">
            <div className="risk-edu-label">Volatility (Ann.)</div>
            <div className="risk-edu-desc">Standard deviation of daily returns, annualized. Higher = more price swings.</div>
          </div>
          <div className="risk-edu-item">
            <div className="risk-edu-label">Sharpe Ratio</div>
            <div className="risk-edu-desc">Return per unit of risk. &gt;1 is good, &gt;2 is excellent.</div>
          </div>
          <div className="risk-edu-item">
            <div className="risk-edu-label">Max Drawdown</div>
            <div className="risk-edu-desc">Largest peak-to-trough decline. Measures worst-case loss.</div>
          </div>
          <div className="risk-edu-item">
            <div className="risk-edu-label">Beta vs SPY</div>
            <div className="risk-edu-desc">Sensitivity to market moves. &gt;1 amplifies S&P 500 swings.</div>
          </div>
          <div className="risk-edu-item">
            <div className="risk-edu-label">RSI (14)</div>
            <div className="risk-edu-desc">&gt;70 = overbought, &lt;30 = oversold. Momentum indicator.</div>
          </div>
          <div className="risk-edu-item">
            <div className="risk-edu-label">SMA Trend</div>
            <div className="risk-edu-desc">Price vs 50/200-day averages. Golden cross = bullish signal.</div>
          </div>
        </div>
        <p className="text-muted" style={{ marginTop: 16, fontSize: 12 }}>
          ⚠️ All metrics are educational only. Past performance does not guarantee future results. Consult a licensed financial advisor for investment decisions.
        </p>
      </div>
    </div>
  )
}
