import { useState } from 'react'
import {
  ResponsiveContainer,
  LineChart, Line,
  AreaChart, Area,
  BarChart, Bar, Cell,
  XAxis, YAxis, CartesianGrid, Tooltip, Legend,
} from 'recharts'
import ChartTooltip from './ChartTooltip'
import { formatShortDate } from '../utils/format'

const CHART_TYPES = ['Line', 'Area', 'Bar', 'Candle']

export default function TimeSeriesChart({
  data = [],
  xKey = 'date',
  yKey = 'value',
  color = 'var(--chart-1)',
  height = 280,
  valueFormatter,
  yDomain = ['auto', 'auto'],
  compareData,
  compareLabel,
  showTypeSelector = false,
  defaultType = 'Line',
}) {
  const [chartType, setChartType] = useState(defaultType)

  const xAxis = (
    <XAxis
      dataKey={xKey}
      tickFormatter={formatShortDate}
      tick={{ fill: 'var(--text-muted)', fontSize: 11.5 }}
      axisLine={{ stroke: 'var(--chart-axis)' }}
      tickLine={false}
      minTickGap={40}
    />
  )
  const yAxis = (
    <YAxis
      tick={{ fill: 'var(--text-muted)', fontSize: 11.5 }}
      axisLine={false}
      tickLine={false}
      width={64}
      domain={yDomain}
      tickFormatter={valueFormatter}
    />
  )
  const grid = <CartesianGrid vertical={false} stroke="var(--chart-grid)" />
  const tooltip = (
    <Tooltip
      content={<ChartTooltip formatter={valueFormatter} labelFormatter={formatShortDate} />}
      cursor={{ stroke: 'var(--chart-axis)', strokeWidth: 1 }}
    />
  )

  const renderChart = () => {
    if (chartType === 'Area') {
      return (
        <AreaChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          <defs>
            <linearGradient id="ag1" x1="0" y1="0" x2="0" y2="1">
              <stop offset="5%" stopColor={color} stopOpacity={0.25} />
              <stop offset="95%" stopColor={color} stopOpacity={0.03} />
            </linearGradient>
          </defs>
          {grid}{xAxis}{yAxis}{tooltip}
          <Area type="monotone" dataKey={yKey} stroke={color} strokeWidth={2} fill="url(#ag1)" dot={false} isAnimationActive={false} name="Value" />
          {compareData && <Area type="monotone" data={compareData} dataKey={yKey} stroke="var(--chart-2)" strokeWidth={2} fill="none" dot={false} isAnimationActive={false} name={compareLabel || 'Compare'} />}
          {compareData && <Legend />}
        </AreaChart>
      )
    }

    if (chartType === 'Bar') {
      return (
        <BarChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          {grid}{xAxis}{yAxis}{tooltip}
          <Bar dataKey={yKey} fill={color} isAnimationActive={false} name="Value" radius={[2, 2, 0, 0]} />
          {compareData && <Bar data={compareData} dataKey={yKey} fill="var(--chart-2)" isAnimationActive={false} name={compareLabel || 'Compare'} radius={[2, 2, 0, 0]} />}
          {compareData && <Legend />}
        </BarChart>
      )
    }

    if (chartType === 'Candle') {
      // Color each bar green/red based on close vs open (or vs previous close)
      const candleData = data.map((d, i) => {
        const prev = i > 0 ? data[i - 1] : null
        const openVal = d.open ?? (prev ? prev[yKey] : d[yKey])
        const closeVal = d.close ?? d[yKey]
        return { ...d, _positive: closeVal >= openVal }
      })
      return (
        <BarChart data={candleData} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
          {grid}{xAxis}{yAxis}{tooltip}
          <Bar dataKey={yKey} isAnimationActive={false} name="Price" radius={[1, 1, 0, 0]}>
            {candleData.map((d, i) => (
              <Cell key={i} fill={d._positive ? 'var(--positive)' : 'var(--negative)'} fillOpacity={0.8} />
            ))}
          </Bar>
        </BarChart>
      )
    }

    // Default: Line
    return (
      <LineChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
        {grid}{xAxis}{yAxis}{tooltip}
        <Line type="monotone" dataKey={yKey} stroke={color} strokeWidth={2} dot={false} activeDot={{ r: 4, strokeWidth: 2, stroke: 'var(--bg-surface)' }} name="Value" isAnimationActive={false} />
        {compareData && <Line type="monotone" data={compareData} dataKey={yKey} stroke="var(--chart-2)" strokeWidth={2} dot={false} isAnimationActive={false} name={compareLabel || 'Compare'} strokeDasharray="4 2" />}
        {compareData && <Legend />}
      </LineChart>
    )
  }

  return (
    <div>
      {showTypeSelector && (
        <div className="chart-type-selector">
          {CHART_TYPES.map((t) => (
            <button key={t} className={`chart-type-btn${chartType === t ? ' active' : ''}`} onClick={() => setChartType(t)}>
              {t}
            </button>
          ))}
        </div>
      )}
      <ResponsiveContainer width="100%" height={height}>
        {renderChart()}
      </ResponsiveContainer>
    </div>
  )
}

