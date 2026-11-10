const KEY = 'session-id'

export function getSessionId(): string {
  try {
    const saved = localStorage.getItem(KEY)
    if (saved) return saved
    const created = crypto.randomUUID()
    localStorage.setItem(KEY, created)
    return created
  } catch {
    return crypto.randomUUID()
  }
}