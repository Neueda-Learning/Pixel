import { useState } from 'react'
import useApi from '../hooks/useApi'
import { getPortfolioSummary, getPortfolioPerformance, getHoldings, exportPortfolio } from '../api/portfolio'
import { getTransactions } from '../api/transactions'
import KpiCard from '../components/KpiCard'
import AllocationDonut from '../components/AllocationDonut'
import TimeSeriesChart from '../components/TimeSeriesChart'
import PeriodToggle from '../components/PeriodToggle'
import HoldingsTable from '../components/HoldingsTable'
import TransactionsTable from '../components/TransactionsTable'
import LoadingState from '../components/LoadingState'
import ErrorState, { extractErrorMessage } from '../components/ErrorState'
import { formatCurrency, formatPercent } from '../utils/format'
import './Dashboard.css'

export default function Dashboard() {
  const [period, setPeriod] = useState('6M')
  const [exporting, setExporting] = useState(false)

  const summary = useApi(getPortfolioSummary, [])
  const performance = useApi(() => getPortfolioPerformance(period), [period])
  const holdings = useApi(getHoldings, [])
  const recentTx = useApi(() => getTransactions('ALL'), [])

  const s = summary.data
  const totalPositive = (s?.totalGainLoss ?? 0) >= 0

  const holdingItems = holdings.data || []
  const stockCount = holdingItems.filter((h) => h.assetType === 'STOCK').length
  const etfCount = holdingItems.filter((h) => h.assetType === 'ETF').length
  const bondCount = holdingItems.filter((h) => h.assetType === 'BOND').length

  const handleExport = async () => {
    setExporting(true)
    try { await exportPortfolio() } finally { setExporting(false) }
  }

  return (
    <div className="dashboard">
      <section className="grid kpi-grid">
        {summary.loading ? (
          <>
            <LoadingState height={92} />
            <LoadingState height={92} />
            <LoadingState height={92} />
            <LoadingState height={92} />
          </>
        ) : summary.error ? (
          <div className="card card-pad" style={{ gridColumn: '1 / -1' }}>
            <ErrorState
              message={extractErrorMessage(summary.error, 'Could not load portfolio summary.')}
              onRetry={summary.reload}
            />
          </div>
        ) : (
          <>
            <KpiCard label="Total portfolio value" value={formatCurrency(s.totalValue)} />
            <KpiCard
              label="Total gain / loss"
              value={formatCurrency(s.totalGainLoss)}
              valuePositive={totalPositive}
              delta={formatPercent(s.totalGainLossPct, { signed: true })}
              deltaPositive={totalPositive}
            />
            <KpiCard label="Holdings" value={s.holdingsCount} />
            <KpiCard label="Cost basis" value={formatCurrency(s.totalCost)} />
          </>
        )}
      </section>

      <section className="grid dashboard-charts">
        <div className="card card-pad">
          <div className="card-header">
            <div>
              <div className="card-title">Portfolio performance</div>
              <div className="card-subtitle">Value over time</div>
            </div>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <PeriodToggle options={['1M', '3M', '6M', '1Y', 'ALL']} value={period} onChange={setPeriod} />
            </div>
          </div>
          {performance.loading ? (
            <LoadingState height={280} />
          ) : performance.error ? (
            <ErrorState
              message={extractErrorMessage(performance.error, 'Could not load performance history.')}
              onRetry={performance.reload}
            />
          ) : performance.data.length === 0 ? (
            <p className="text-muted">No performance history yet.</p>
          ) : (
            <TimeSeriesChart
              data={performance.data}
              valueFormatter={formatCurrency}
              color="var(--chart-1)"
              showTypeSelector
              defaultType="Area"
            />
          )}
        </div>

        <div className="card card-pad">
          <div className="card-header">
            <div>
              <div className="card-title">Allocation</div>
              <div className="card-subtitle">By asset type</div>
            </div>
          </div>
          {summary.loading ? (
            <LoadingState height={260} />
          ) : summary.error ? (
            <ErrorState message="Could not load allocation." onRetry={summary.reload} />
          ) : s.allocation.length === 0 ? (
            <p className="text-muted">No holdings to allocate yet.</p>
          ) : (
            <AllocationDonut data={s.allocation} />
          )}
        </div>
      </section>

      <section className="card card-pad">
        <div className="card-header">
          <div>
            <div className="card-title">Holdings</div>
            <div className="card-subtitle" style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {!holdings.loading && holdingItems.length > 0 && (
                <>
                  {stockCount > 0 && <span className="holdings-badge">📈 {stockCount} Stock{stockCount !== 1 ? 's' : ''}</span>}
                  {etfCount > 0  && <span className="holdings-badge">🗂️ {etfCount} ETF{etfCount !== 1 ? 's' : ''}</span>}
                  {bondCount > 0 && <span className="holdings-badge">💵 {bondCount} Bond{bondCount !== 1 ? 's' : ''}</span>}
                </>
              )}
            </div>
          </div>
          <button className="action-btn primary" onClick={handleExport} disabled={exporting}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M12 3v12m0 0-4-4m4 4 4-4M3 17v2a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-2" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
            {exporting ? 'Exporting…' : 'Export Portfolio'}
          </button>
        </div>
        {holdings.loading ? (
          <LoadingState height={200} />
        ) : holdings.error ? (
          <ErrorState
            message={extractErrorMessage(holdings.error, 'Could not load holdings.')}
            onRetry={holdings.reload}
          />
        ) : (
          <HoldingsTable holdings={holdings.data} />
        )}
      </section>

      <section className="card card-pad">
        <div className="card-header">
          <div>
            <div className="card-title">Recent transactions</div>
          </div>
        </div>
        {recentTx.loading ? (
          <LoadingState height={160} />
        ) : recentTx.error ? (
          <ErrorState
            message={extractErrorMessage(recentTx.error, 'Could not load transactions.')}
            onRetry={recentTx.reload}
          />
        ) : (
          <TransactionsTable
            transactions={[...recentTx.data]
              .sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt))
              .slice(0, 5)}
            compact
          />
        )}
      </section>
    </div>
  )
}
