import { useState, useEffect } from 'react'
import AddClientModal from './AddClientModal'
import './Topbar.css'

export default function Topbar({ title, onMenuClick }) {
  const [showModal, setShowModal] = useState(false)
  const [clientName, setClientName] = useState('')

  useEffect(() => {
    const stored = localStorage.getItem('pixelClient')
    if (stored) {
      try { setClientName(JSON.parse(stored).name || '') } catch {}
    }
    const handler = () => {
      const s = localStorage.getItem('pixelClient')
      if (s) { try { setClientName(JSON.parse(s).name || '') } catch {} }
    }
    window.addEventListener('storage', handler)
    return () => window.removeEventListener('storage', handler)
  }, [])

  const handleModalClose = () => {
    setShowModal(false)
    const stored = localStorage.getItem('pixelClient')
    if (stored) { try { setClientName(JSON.parse(stored).name || '') } catch {} }
  }

  return (
    <>
      <header className="topbar">
        <button className="topbar-menu-btn" onClick={onMenuClick} aria-label="Toggle navigation">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M4 7h16M4 12h16M4 17h16" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
        </button>

        <div className="topbar-brand">
          <span className="topbar-brand-mark">P</span>
          <span className="topbar-brand-name">Pixel</span>
        </div>

        <h1 className="topbar-title">{title}</h1>

        <div className="topbar-right">
          {clientName && (
            <span className="topbar-welcome">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="8" r="4" stroke="currentColor" strokeWidth="1.8"/>
                <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
              </svg>
              Welcome, {clientName}
            </span>
          )}
          <button className="topbar-add-client" onClick={() => setShowModal(true)}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
              <path d="M16 11c1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3 1.34 3 3 3Zm-8 0c1.66 0 3-1.34 3-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3Zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5Zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5Z" fill="currentColor"/>
            </svg>
            Add Client
          </button>
        </div>
      </header>

      {showModal && <AddClientModal onClose={handleModalClose} />}
    </>
  )
}

