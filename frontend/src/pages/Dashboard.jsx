import { useEffect, useMemo, useState } from 'react'
import useApi from '../hooks/useApi'
import { getPortfolioSummary, getPortfolioPerformance, getHoldings } from '../api/portfolio'
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
  const [holdingsPage, setHoldingsPage] = useState(1)
  const [recentPage, setRecentPage] = useState(1)

  const summary = useApi(getPortfolioSummary, [])
  const performance = useApi(() => getPortfolioPerformance(period), [period])
  const holdings = useApi(getHoldings, [])
  const recentTx = useApi(() => getTransactions('ALL'), [])

  const s = summary.data
  const totalPositive = (s?.totalGainLoss ?? 0) >= 0

  const sortedHoldings = useMemo(
    () => [...(holdings.data ?? [])].sort((a, b) => b.marketValue - a.marketValue),
    [holdings.data],
  )

  const sortedRecentTransactions = useMemo(
    () => [...(recentTx.data ?? [])].sort((a, b) => new Date(b.executedAt) - new Date(a.executedAt)),
    [recentTx.data],
  )

  useEffect(() => {
    setHoldingsPage(1)
  }, [sortedHoldings.length])

  useEffect(() => {
    setRecentPage(1)
  }, [sortedRecentTransactions.length])

  const exportHoldingsCsv = () => {
    if (!sortedHoldings.length) return

    const headers = ['Symbol', 'Name', 'Quantity', 'Avg Cost', 'Current Price', 'Market Value', 'Gain/Loss', 'Gain/Loss %']
    const rows = sortedHoldings.map((h) => [
      h.symbol,
      h.name,
      h.quantity,
      h.avgCost,
      h.currentPrice,
      h.marketValue,
      h.gainLoss,
      h.gainLossPct,
    ])

    const csv = [headers, ...rows]
      .map((row) =>
        row
          .map((cell) => {
            const text = String(cell ?? '')
            return /[",\n]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text
          })
          .join(','),
      )
      .join('\n')

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `holdings-${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="dashboard">
      <section className="grid kpi-grid">
        {summary.loading ? (
          <>
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
            <KpiCard
              label="Holdings"
              value={s.holdingsCount}
              list={
                holdings.data && holdings.data.length > 0
                  ? sortedHoldings.map((h) => ({
                        symbol: h.symbol,
                        positive: h.gainLoss >= 0,
                        changeLabel: formatPercent(h.gainLossPct, { signed: true }),
                      }))
                  : undefined
              }
            />
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
            <PeriodToggle options={['1M', '3M', '6M', '1Y', 'ALL']} value={period} onChange={setPeriod} />
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
            <TimeSeriesChart data={performance.data} valueFormatter={formatCurrency} />
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
            <div className="card-subtitle">Derived from your transaction history</div>
          </div>
          <button type="button" className="btn btn-primary" onClick={exportHoldingsCsv} disabled={!sortedHoldings.length}>
            Export CSV
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
          <HoldingsTable
            holdings={sortedHoldings}
            page={holdingsPage}
            pageSize={6}
            onPageChange={setHoldingsPage}
          />
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
            transactions={sortedRecentTransactions}
            compact
            page={recentPage}
            pageSize={5}
            onPageChange={setRecentPage}
          />
        )}
      </section>
    </div>
  )
}
