import { useState, useEffect } from 'react'
import './AddClientModal.css'

const EMPTY = { name: '', email: '', phone: '', riskProfile: 'MODERATE', notes: '' }

export default function AddClientModal({ onClose }) {
  const [form, setForm] = useState(EMPTY)
  const [saved, setSaved] = useState(false)

  // Load existing client from localStorage
  useEffect(() => {
    const stored = localStorage.getItem('pixelClient')
    if (stored) { try { setForm(JSON.parse(stored)) } catch {} }
  }, [])

  const handleChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }))

  const handleSubmit = (e) => {
    e.preventDefault()
    localStorage.setItem('pixelClient', JSON.stringify(form))
    setSaved(true)
    setTimeout(() => { setSaved(false); onClose() }, 1000)
  }

  return (
    <div className="modal-overlay" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal-box">
        <div className="modal-header">
          <div>
            <h2 className="modal-title">Client Profile</h2>
            <p className="modal-subtitle">Your information is stored locally on this device.</p>
          </div>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <form className="modal-form" onSubmit={handleSubmit}>
          <div className="form-row">
            <label>Full Name *</label>
            <input name="name" value={form.name} onChange={handleChange} required placeholder="e.g. John Smith" />
          </div>
          <div className="form-row">
            <label>Email</label>
            <input name="email" type="email" value={form.email} onChange={handleChange} placeholder="john@example.com" />
          </div>
          <div className="form-row">
            <label>Phone</label>
            <input name="phone" value={form.phone} onChange={handleChange} placeholder="+1 (555) 000-0000" />
          </div>
          <div className="form-row">
            <label>Risk Profile</label>
            <select name="riskProfile" value={form.riskProfile} onChange={handleChange}>
              <option value="CONSERVATIVE">Conservative — low risk, stable returns</option>
              <option value="MODERATE">Moderate — balanced risk/reward</option>
              <option value="AGGRESSIVE">Aggressive — high risk, high potential</option>
            </select>
          </div>
          <div className="form-row">
            <label>Notes</label>
            <textarea name="notes" value={form.notes} onChange={handleChange} rows={3} placeholder="Investment goals, preferences..." />
          </div>
          <div className="modal-actions">
            <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
            <button type="submit" className={`btn btn-primary ${saved ? 'saved' : ''}`}>
              {saved ? '✓ Saved!' : 'Save Profile'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
