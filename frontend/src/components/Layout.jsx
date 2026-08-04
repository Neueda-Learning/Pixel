import { useState } from 'react'
import { Outlet, useLocation, useParams } from 'react-router-dom'
import Sidebar from './Sidebar'
import Topbar from './Topbar'
import './Layout.css'

function titleFor(pathname, params) {
  if (pathname === '/') return 'Dashboard'
  if (pathname === '/transactions') return 'Transactions'
  if (params.symbol) return params.symbol.toUpperCase()
  return 'Portfolio Manager'
}

export default function Layout() {
  const [navOpen, setNavOpen] = useState(false)
  const location = useLocation()
  const params = useParams()

  return (
    <div className="app-shell">
      <Sidebar open={navOpen} onNavigate={() => setNavOpen(false)} />
      {navOpen && <div className="scrim" onClick={() => setNavOpen(false)} />}
      <div className="app-main">
        <Topbar title={titleFor(location.pathname, params)} onMenuClick={() => setNavOpen((o) => !o)} />
        <main className="app-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
