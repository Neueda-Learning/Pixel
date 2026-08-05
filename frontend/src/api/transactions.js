import api from './client'

export const getTransactions = (period = 'ALL', { from, to } = {}) =>
  api.get('/transactions', { params: { period, from, to } }).then((r) => r.data)

export const addTransaction = (tx) => api.post('/transactions', tx).then((r) => r.data)

export const updateTransaction = (id, tx) => api.put(`/transactions/${id}`, tx).then((r) => r.data)

export const importTransactions = (txs) => api.post('/transactions/import', txs).then((r) => r.data)

export const deleteTransaction = (id) => api.delete(`/transactions/${id}`)
