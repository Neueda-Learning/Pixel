import './Footer.css'

export default function Footer() {
  const year = new Date().getFullYear()
  return (
    <footer className="app-footer">
      <div className="footer-inner">
        <div className="footer-brand">
          <span className="footer-brand-mark">P</span>
          <span className="footer-brand-name">Pixel</span>
        </div>
        <p className="footer-copy">
          &copy; {year} Pixel Portfolio Manager. All rights reserved.
          <span className="footer-sep">·</span>
          Educational use only — not financial advice.
        </p>
        <div className="footer-links">
          <span>Built with React &amp; Spring Boot</span>
          <span className="footer-sep">·</span>
          <span>Data via Finnhub</span>
        </div>
      </div>
    </footer>
  )
}
