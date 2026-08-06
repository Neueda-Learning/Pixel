import { useId, useMemo } from 'react'
import { ResponsiveContainer, AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip } from 'recharts'
import ChartTooltip from './ChartTooltip'
import { formatShortDate, formatYear } from '../utils/format'

const YEAR_SPAN_THRESHOLD_DAYS = 365

export default function TimeSeriesChart({
  data,
  xKey = 'date',
  yKey = 'value',
  color,
  height = 280,
  valueFormatter,
  yDomain = ['auto', 'auto'],
}) {
  const gradientId = useId()

  // Auto-detect gain/loss trend (first vs. last point) and color the line + shadow
  // green for gains and red for losses, like Google Finance — unless a color is forced.
  const trendColor = useMemo(() => {
    if (color) return color
    if (!data || data.length < 2) return 'var(--chart-1)'
    const first = data[0]?.[yKey]
    const last = data[data.length - 1]?.[yKey]
    if (typeof first !== 'number' || typeof last !== 'number') return 'var(--chart-1)'
    return last >= first ? 'var(--positive)' : 'var(--negative)'
  }, [color, data, yKey])

  // Long ranges (e.g. "ALL") span multiple years, so "Jan 5"-style ticks become
  // repetitive/unreadable — switch the axis to year-only ticks past ~1 year of data.
  const spansMultipleYears = useMemo(() => {
    if (!data || data.length < 2) return false
    const first = new Date(data[0]?.[xKey]).getTime()
    const last = new Date(data[data.length - 1]?.[xKey]).getTime()
    if (Number.isNaN(first) || Number.isNaN(last)) return false
    return (last - first) / 86_400_000 > YEAR_SPAN_THRESHOLD_DAYS
  }, [data, xKey])

  const axisTickFormatter = spansMultipleYears ? formatYear : formatShortDate

  return (
    <ResponsiveContainer width="100%" height={height}>
      <AreaChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
        <defs>
          <linearGradient id={gradientId} x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor={trendColor} stopOpacity={0.28} />
            <stop offset="100%" stopColor={trendColor} stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid vertical={false} stroke="var(--chart-grid)" strokeDasharray="0" />
        <XAxis
          dataKey={xKey}
          tickFormatter={axisTickFormatter}
          tick={{ fill: 'var(--text-muted)', fontSize: 11.5 }}
          axisLine={{ stroke: 'var(--chart-axis)' }}
          tickLine={false}
          minTickGap={spansMultipleYears ? 60 : 40}
        />
        <YAxis
          tick={{ fill: 'var(--text-muted)', fontSize: 11.5 }}
          axisLine={false}
          tickLine={false}
          width={64}
          domain={yDomain}
          tickFormatter={valueFormatter}
        />
        <Tooltip
          content={<ChartTooltip formatter={valueFormatter} labelFormatter={formatShortDate} />}
          cursor={{ stroke: 'var(--chart-axis)', strokeWidth: 1 }}
        />
        <Area
          type="monotone"
          dataKey={yKey}
          stroke={trendColor}
          strokeWidth={2}
          fill={`url(#${gradientId})`}
          dot={false}
          activeDot={{ r: 4, strokeWidth: 2, stroke: 'var(--bg-surface)' }}
          name="Value"
          isAnimationActive={false}
        />
      </AreaChart>
    </ResponsiveContainer>
  )
}
