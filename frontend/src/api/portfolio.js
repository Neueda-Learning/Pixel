import api from './client'

export const getHoldings = () => api.get('/portfolio').then((r) => r.data)

export const getPortfolioSummary = () => api.get('/portfolio/summary').then((r) => r.data)

export const getPortfolioPerformance = (period = '6M') =>
  api.get('/portfolio/performance', { params: { period } }).then((r) => r.data)
