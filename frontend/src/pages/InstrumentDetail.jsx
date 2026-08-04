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

export default function InstrumentDetail() {
  const { symbol } = useParams()
  const [period, setPeriod] = useState('6M')

  const prices = useApi(() => getInstrumentPrices(symbol, period), [symbol, period])
  const profile = useApi(() => getProfile(symbol), [symbol])
  const quote = useApi(() => getQuote(symbol), [symbol])
  const risk = useApi(() => getRisk(symbol), [symbol])
  const news = useApi(() => getNews(symbol), [symbol])

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
            <div className="card-subtitle">Daily close, from price_history</div>
          </div>
          <PeriodToggle value={period} onChange={setPeriod} />
        </div>
        {prices.loading ? (
          <LoadingState height={300} />
        ) : prices.error ? (
          <ErrorState
            message={extractErrorMessage(prices.error, 'Could not load price history.')}
            onRetry={prices.reload}
          />
        ) : prices.data.length === 0 ? (
          <p className="text-muted">No price history for this period.</p>
        ) : (
          <TimeSeriesChart
            data={prices.data.map((p) => ({ date: p.date, value: p.close }))}
            valueFormatter={formatCurrency}
            color="var(--chart-1)"
            height={320}
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
