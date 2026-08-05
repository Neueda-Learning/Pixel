import { useState } from 'react'
import { useParams } from 'react-router-dom'
import useApi from '../hooks/useApi'
import { getInstrumentPrices } from '../api/instruments'
import { getProfile, getQuote, getNews } from '../api/market'
import { getRisk } from '../api/risk'
import ProfileCard from '../components/ProfileCard'
import RiskPanel from '../components/RiskPanel'
import NewsList from '../components/NewsList'
import TimeSeriesChart from '../components/TimeSeriesChart'
import PeriodToggle from '../components/PeriodToggle'
import LoadingState from '../components/LoadingState'
import ErrorState, { extractErrorMessage } from '../components/ErrorState'
import { formatCurrency } from '../utils/format'
import './InstrumentDetail.css'

const METRICS = [
  { key: 'close',  label: 'Close' },
  { key: 'open',   label: 'Open' },
  { key: 'high',   label: 'High' },
  { key: 'low',    label: 'Low' },
  { key: 'volume', label: 'Volume' },
]

export default function InstrumentDetail() {
  const { symbol } = useParams()
  const [period, setPeriod] = useState('6M')
  const [metric, setMetric] = useState('close')

  const prices = useApi(() => getInstrumentPrices(symbol, period), [symbol, period])
  const profile = useApi(() => getProfile(symbol), [symbol])
  const quote = useApi(() => getQuote(symbol), [symbol])
  const risk = useApi(() => getRisk(symbol), [symbol])
  const news = useApi(() => getNews(symbol), [symbol])

  const chartData = (prices.data || []).map((p) => ({ date: p.date, value: p[metric] ?? p.close, ...p }))
  const isVolume = metric === 'volume'

  return (
    <div className="instrument-detail">
      <section className="card card-pad">
        {profile.loading || quote.loading ? (
          <LoadingState height={60} />
        ) : profile.error ? (
          <ErrorState
            message={extractErrorMessage(profile.error, 'Could not load company profile.')}
            onRetry={profile.reload}
          />
        ) : (
          <ProfileCard profile={profile.data} quote={quote.error ? null : quote.data} />
        )}
      </section>

      <section className="card card-pad">
        <div className="card-header">
          <div>
            <div className="card-title">Price history</div>
            <div className="card-subtitle">Historical OHLCV data</div>
          </div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', alignItems: 'center' }}>
            {/* Metric selector */}
            <div className="metric-selector">
              {METRICS.map((m) => (
                <button
                  key={m.key}
                  className={`chart-type-btn${metric === m.key ? ' active' : ''}`}
                  onClick={() => setMetric(m.key)}
                >
                  {m.label}
                </button>
              ))}
            </div>
            <PeriodToggle value={period} onChange={setPeriod} />
          </div>
        </div>
        {prices.loading ? (
          <LoadingState height={300} />
        ) : prices.error ? (
          <ErrorState
            message={extractErrorMessage(prices.error, 'Could not load price history.')}
            onRetry={prices.reload}
          />
        ) : chartData.length === 0 ? (
          <p className="text-muted">No price history for this period.</p>
        ) : (
          <TimeSeriesChart
            data={chartData}
            valueFormatter={isVolume ? (v) => (v == null ? '—' : `${(v / 1e6).toFixed(1)}M`) : formatCurrency}
            color={isVolume ? 'var(--chart-2)' : 'var(--chart-1)'}
            height={320}
            showTypeSelector
            defaultType={isVolume ? 'Bar' : 'Candle'}
          />
        )}
      </section>

      <section className="card card-pad">
        <div className="card-header">
          <div>
            <div className="card-title">Risk</div>
            <div className="card-subtitle">Rule-based, computed from historical prices</div>
          </div>
        </div>
        {risk.loading ? (
          <LoadingState height={220} />
        ) : risk.error ? (
          <ErrorState
            message={extractErrorMessage(risk.error, 'Could not load risk metrics.')}
            onRetry={risk.reload}
          />
        ) : (
          <RiskPanel risk={risk.data} />
        )}
      </section>

      <section className="card card-pad">
        <div className="card-header">
          <div className="card-title">News</div>
        </div>
        {news.loading ? (
          <LoadingState height={160} />
        ) : news.error ? (
          <ErrorState message="Could not load news." onRetry={news.reload} />
        ) : (
          <NewsList items={news.data} />
        )}
      </section>
    </div>
  )
}
