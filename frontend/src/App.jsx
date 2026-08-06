import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import ScrollToTop from './components/ScrollToTop'
import Dashboard from './pages/Dashboard'
import Transactions from './pages/Transactions'
import InstrumentDetail from './pages/InstrumentDetail'

export default function App() {
  return (
    <>
      <ScrollToTop />
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<Dashboard />} />
          <Route path="/transactions" element={<Transactions />} />
          <Route path="/instruments/:symbol" element={<InstrumentDetail />} />
        </Route>
      </Routes>
    </>
  )
}
