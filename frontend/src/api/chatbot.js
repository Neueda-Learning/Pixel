import api from './client'

export const sendChat = (message) => api.post('/chat', { message }).then((r) => r.data)
