import api from './client'

export const getTransactions = (period = 'ALL') =>
  api.get('/transactions', { params: { period } }).then((r) => r.data)

export const addTransaction = (tx) => api.post('/transactions', tx).then((r) => r.data)

export const deleteTransaction = (id) => api.delete(`/transactions/${id}`)
