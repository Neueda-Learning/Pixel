import api from './client'

export const getHoldings = () => api.get('/portfolio').then((r) => r.data)

export const getPortfolioSummary = () => api.get('/portfolio/summary').then((r) => r.data)

export const getPortfolioPerformance = (period = '6M') =>
  api.get('/portfolio/performance', { params: { period } }).then((r) => r.data)

export const exportPortfolio = () =>
  api.get('/portfolio/export', { responseType: 'blob' }).then((r) => {
    const url = window.URL.createObjectURL(new Blob([r.data]))
    const link = document.createElement('a')
    link.href = url
    const disposition = r.headers['content-disposition'] || ''
    const match = disposition.match(/filename="?([^"]+)"?/)
    link.download = match ? match[1] : 'portfolio-export.csv'
    link.click()
    window.URL.revokeObjectURL(url)
  })

