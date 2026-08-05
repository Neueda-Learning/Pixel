import './PeriodToggle.css'

export default function PeriodToggle({ options = ['1M', '3M', '6M', '1Y', 'ALL'], value, onChange }) {
  return (
    <div className="period-toggle" role="group" aria-label="Select time period">
      {options.map((opt) => (
        <button
          key={opt}
          type="button"
          className={`period-toggle-btn${opt === value ? ' active' : ''}`}
          onClick={() => onChange(opt)}
        >
          {opt}
        </button>
      ))}
    </div>
  )
}
