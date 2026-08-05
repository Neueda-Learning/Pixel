import { ResponsiveContainer, LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip } from 'recharts'
import ChartTooltip from './ChartTooltip'
import { formatShortDate } from '../utils/format'

export default function TimeSeriesChart({
  data,
  xKey = 'date',
  yKey = 'value',
  color = 'var(--chart-1)',
  height = 280,
  valueFormatter,
  yDomain = ['auto', 'auto'],
}) {
  return (
    <ResponsiveContainer width="100%" height={height}>
      <LineChart data={data} margin={{ top: 8, right: 8, bottom: 0, left: 0 }}>
        <CartesianGrid vertical={false} stroke="var(--chart-grid)" strokeDasharray="0" />
        <XAxis
          dataKey={xKey}
          tickFormatter={formatShortDate}
          tick={{ fill: 'var(--text-muted)', fontSize: 11.5 }}
          axisLine={{ stroke: 'var(--chart-axis)' }}
          tickLine={false}
          minTickGap={40}
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
        <Line
          type="monotone"
          dataKey={yKey}
          stroke={color}
          strokeWidth={2}
          dot={false}
          activeDot={{ r: 4, strokeWidth: 2, stroke: 'var(--bg-surface)' }}
          name="Value"
          isAnimationActive={false}
        />
      </LineChart>
    </ResponsiveContainer>
  )
}
