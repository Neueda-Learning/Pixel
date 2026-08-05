import api from './client'

export const getTransactions = (period = 'ALL') =>
  api.get('/transactions', { params: { period } }).then((r) => r.data)

export const addTransaction = (tx) => api.post('/transactions', tx).then((r) => r.data)

export const deleteTransaction = (id) => api.delete(`/transactions/${id}`)

export const importTransactions = (file) => {
  const data = new FormData()
  data.append('file', file)
  return api.post('/transactions/import', data, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }).then((r) => r.data)
}

