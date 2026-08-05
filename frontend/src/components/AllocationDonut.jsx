import { ResponsiveContainer, PieChart, Pie, Cell, Tooltip, Legend } from 'recharts'
import ChartTooltip from './ChartTooltip'
import { formatCurrency } from '../utils/format'
import './AllocationDonut.css'

const COLORS = [
  'var(--chart-1)',
  'var(--chart-2)',
  'var(--chart-3)',
  'var(--chart-4)',
  'var(--chart-5)',
  'var(--chart-6)',
]

export default function AllocationDonut({ data, height = 260 }) {
  if (!data || data.length === 0) return null

  return (
    <div className="allocation-donut">
      <ResponsiveContainer width="100%" height={height}>
        <PieChart>
          <Pie
            data={data}
            dataKey="value"
            nameKey="assetType"
            innerRadius="62%"
            outerRadius="90%"
            paddingAngle={data.length > 1 ? 2 : 0}
            stroke="var(--bg-surface)"
            strokeWidth={2}
            isAnimationActive={false}
          >
            {data.map((entry, i) => (
              <Cell key={entry.assetType} fill={COLORS[i % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip content={<ChartTooltip formatter={formatCurrency} />} />
          <Legend
            verticalAlign="bottom"
            iconType="circle"
            iconSize={8}
            formatter={(value) => <span className="donut-legend-label">{value}</span>}
          />
        </PieChart>
      </ResponsiveContainer>
    </div>
  )
}
