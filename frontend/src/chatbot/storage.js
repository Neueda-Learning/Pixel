import { createInitialSessionState } from './chatEngine'

export const CHAT_STORAGE_KEY = 'pixel.chatbot.v1'

export function loadChatState() {
  try {
    const raw = localStorage.getItem(CHAT_STORAGE_KEY)
    if (!raw) {
      return {
        messages: [],
        sessionState: createInitialSessionState(),
      }
    }

    const parsed = JSON.parse(raw)
    return {
      messages: Array.isArray(parsed.messages) ? parsed.messages : [],
      sessionState: {
        ...createInitialSessionState(),
        ...(parsed.sessionState ?? {}),
      },
    }
  } catch (_error) {
    return {
      messages: [],
      sessionState: createInitialSessionState(),
    }
  }
}

export function saveChatState(state) {
  const payload = {
    messages: state.messages ?? [],
    sessionState: state.sessionState ?? createInitialSessionState(),
  }
  localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(payload))
}

export function clearChatState() {
  localStorage.removeItem(CHAT_STORAGE_KEY)
}
