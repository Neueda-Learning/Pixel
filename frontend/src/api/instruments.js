import api from './client'

export const getInstruments = () => api.get('/instruments').then((r) => r.data)

export const getInstrumentPrices = (symbol, period = '6M') =>
  api.get(`/instruments/${symbol}/prices`, { params: { period } }).then((r) => r.data)
