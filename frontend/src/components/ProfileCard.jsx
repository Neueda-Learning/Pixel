import './ProfileCard.css'

export default function ProfileCard({ profile, quote }) {
  if (!profile) return null
  const positive = quote?.change != null ? quote.change >= 0 : null

  return (
    <div className="profile-card">
      <div className="profile-card-identity">
        {profile.logo ? (
          <img
            src={profile.logo}
            alt=""
            className="profile-logo"
            onError={(e) => {
              e.target.style.display = 'none'
            }}
          />
        ) : (
          <div className="profile-logo profile-logo-fallback">{profile.symbol?.[0]}</div>
        )}
        <div>
          <div className="profile-name">{profile.name || profile.symbol}</div>
          <div className="text-muted" style={{ fontSize: 12.5 }}>
            {profile.exchange || '—'} · {profile.currency || 'USD'}
          </div>
        </div>
      </div>

      {quote && (
        <div className="profile-quote">
          <span className="profile-quote-price tabular">${Number(quote.current).toFixed(2)}</span>
          {quote.change != null && (
            <span className={positive ? 'text-positive' : 'text-negative'}>
              {positive ? '▲' : '▼'} {Math.abs(quote.change).toFixed(2)} (
              {Math.abs(quote.changePercent ?? 0).toFixed(2)}%)
            </span>
          )}
          <span className="badge" style={{ background: 'var(--bg-subtle)', color: 'var(--text-muted)' }}>
            {quote.source === 'LIVE' ? 'Live' : 'From price history'}
          </span>
        </div>
      )}
    </div>
  )
}
