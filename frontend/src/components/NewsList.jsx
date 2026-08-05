import { formatDateTime } from '../utils/format'
import './NewsList.css'

export default function NewsList({ items }) {
  if (!items || items.length === 0) {
    return <p className="text-muted">No recent news available.</p>
  }

  return (
    <ul className="news-list">
      {items.map((item) => (
        <li key={item.id} className="news-item">
          {item.image && (
            <img
              src={item.image}
              alt=""
              className="news-image"
              onError={(e) => {
                e.target.style.display = 'none'
              }}
            />
          )}
          <div className="news-body">
            <a href={item.url} target="_blank" rel="noreferrer" className="news-headline">
              {item.headline}
            </a>
            <div className="news-meta text-muted">
              {item.source} · {formatDateTime(item.publishedAt)}
            </div>
          </div>
        </li>
      ))}
    </ul>
  )
}
