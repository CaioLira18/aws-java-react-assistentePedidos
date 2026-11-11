import type { Dish } from '../types'
import { parseDishes } from './dishes'
import { GENERIC_ERROR, readErrorMessage } from './http'
import { createSseParser } from './sse'

export type DoneInfo = { outcome: string; iterations: number }

export type ChatHandlers = {
  onToken: (text: string) => void
  onDishes: (dishes: Dish[]) => void
  onDone: (info: DoneInfo) => void
  onError: (message: string) => void
}

function isAbort(err: unknown): boolean {
  return err instanceof DOMException && err.name === 'AbortError'
}

function safeJson(raw: string): Record<string, unknown> | null {
  try {
    const value: unknown = JSON.parse(raw)
    return typeof value === 'object' && value !== null ? (value as Record<string, unknown>) : null
  } catch {
    return null
  }
}

export async function streamChat(
  message: string,
  sessionId: string,
  handlers: ChatHandlers,
  signal: AbortSignal,
): Promise<void> {
  let res: Response
  try {
    res = await fetch('/api/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        'X-Session-Id': sessionId,
      },
      body: JSON.stringify({ message }),
      signal,
    })
  } catch (err) {
    if (!isAbort(err)) handlers.onError(GENERIC_ERROR)
    return
  }

  if (!res.ok || !res.body) {
    handlers.onError(await readErrorMessage(res))
    return
  }

  const parser = createSseParser((event) => {
    const data = safeJson(event.data)
    if (event.event === 'token' && typeof data?.text === 'string') {
      handlers.onToken(data.text)
    } else if (event.event === 'dishes') {
      handlers.onDishes(parseDishes(data))
    } else if (event.event === 'done') {
      handlers.onDone({ outcome: String(data?.outcome ?? ''), iterations: Number(data?.iterations ?? 0) })
    } else if (event.event === 'error') {
      handlers.onError(typeof data?.message === 'string' ? data.message : GENERIC_ERROR)
    }
  })

  const reader = res.body.getReader()
  const decoder = new TextDecoder()
  try {
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      parser.feed(decoder.decode(value, { stream: true }))
    }
    parser.feed(decoder.decode())
  } catch (err) {
    if (!isAbort(err)) handlers.onError(GENERIC_ERROR)
  }
}