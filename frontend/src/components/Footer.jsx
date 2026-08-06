import './Footer.css'

export default function Footer() {
  const year = new Date().getFullYear()

  return (
    <footer className="app-footer">
      <p>&copy; {year} Pixel Portfolio Manager. All rights reserved.</p>
    </footer>
  )
}
