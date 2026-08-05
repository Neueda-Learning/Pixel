import { NavLink } from 'react-router-dom'
import './Sidebar.css'

const NAV_ITEMS = [
  {
    to: '/',
    label: 'Dashboard',
    end: true,
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
        <path d="M4 13h6V4H4v9Zm0 7h6v-5H4v5Zm10 0h6V11h-6v9Zm0-16v5h6V4h-6Z" fill="currentColor" />
      </svg>
    ),
  },
  {
    to: '/transactions',
    label: 'Transactions',
    icon: (
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
        <path
          d="M7 3v14M7 17l-3-3M7 17l3-3M17 21V7M17 7l-3 3M17 7l3 3"
          stroke="currentColor"
          strokeWidth="1.8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    ),
  },
]

export default function Sidebar({ open, onNavigate }) {
  return (
    <aside className={`sidebar${open ? ' open' : ''}`}>
      <div className="sidebar-brand">
        <span className="sidebar-brand-mark">P</span>
        <span className="sidebar-brand-name">Portfolio Manager</span>
      </div>
      <nav className="sidebar-nav">
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            end={item.end}
            onClick={onNavigate}
            className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}
          >
            {item.icon}
            {item.label}
          </NavLink>
        ))}
      </nav>
      <div className="sidebar-footer">
        <span className="text-muted"></span>
      </div>
    </aside>
  )
}
