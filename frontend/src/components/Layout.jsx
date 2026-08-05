import { useState } from 'react'
import { Outlet, useLocation, useParams } from 'react-router-dom'
import Sidebar from './Sidebar'
import Topbar from './Topbar'
import StockTicker from './StockTicker'
import Footer from './Footer'
import ChatBot from './ChatBot'
import './Layout.css'

function titleFor(pathname, params) {
  if (pathname === '/') return 'Dashboard'
  if (pathname === '/transactions') return 'Transactions'
  if (pathname === '/risk') return 'Risk Analysis'
  if (params.symbol) return params.symbol.toUpperCase()
  return 'Pixel'
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
        <StockTicker />
        <Topbar title={titleFor(location.pathname, params)} onMenuClick={() => setNavOpen((o) => !o)} />
        <main className="app-content">
          <Outlet />
        </main>
        <Footer />
      </div>
      <ChatBot />
    </div>
  )
}

