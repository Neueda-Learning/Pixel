import api from './client'

export const getRisk = (symbol) => api.get(`/risk/${symbol}`).then((r) => r.data)
