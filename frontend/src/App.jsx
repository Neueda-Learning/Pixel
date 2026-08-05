import { Routes, Route } from 'react-router-dom'
import Layout from './components/Layout'
import Dashboard from './pages/Dashboard'
import Transactions from './pages/Transactions'
import InstrumentDetail from './pages/InstrumentDetail'
import RiskAnalysis from './pages/RiskAnalysis'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Dashboard />} />
        <Route path="/transactions" element={<Transactions />} />
        <Route path="/instruments/:symbol" element={<InstrumentDetail />} />
        <Route path="/risk" element={<RiskAnalysis />} />
      </Route>
    </Routes>
  )
}

