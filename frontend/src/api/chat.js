import api from './client'

export const sendChatMessage = (message) => api.post('/chat', { message }).then((r) => r.data)
