import api from './client'

export const getQuote = (symbol) => api.get(`/market/quote/${symbol}`).then((r) => r.data)

export const getProfile = (symbol) => api.get(`/market/profile/${symbol}`).then((r) => r.data)

export const getNews = (symbol) => api.get(`/market/news/${symbol}`).then((r) => r.data)

export const searchSymbols = (q) => api.get('/market/search', { params: { q } }).then((r) => r.data)
