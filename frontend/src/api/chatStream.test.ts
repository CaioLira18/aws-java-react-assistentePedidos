import { afterEach, expect, it, vi } from 'vitest'
import { streamChat, type ChatHandlers } from './chatStream'

function handlers() {
  const calls = { tokens: [] as string[], dishes: [] as number[], done: 0, errors: [] as string[] }
  const h: ChatHandlers = {
    onToken: (t) => calls.tokens.push(t),
    onDishes: (d) => calls.dishes.push(...d.map((x) => x.id)),
    onDone: () => {
      calls.done += 1
    },
    onError: (m) => calls.errors.push(m),
  }
  return { calls, h }
}

function sse(text: string, status = 200): Response {
  const body = new ReadableStream<Uint8Array>({
    start(controller) {
      controller.enqueue(new TextEncoder().encode(text))
      controller.close()
    },
  })
  return new Response(body, { status, headers: { 'Content-Type': 'text/event-stream' } })
}

afterEach(() => vi.unstubAllGlobals())

it('dispatches token, dishes and done events', async () => {
  const dish = { id: 7, name: 'Feijoada', description: 'd', priceCents: 3190, prepMinutes: 45, dietaryTags: [] }
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(
      sse(
        'event:token\ndata:{"text":"Ola"}\n\n' +
          'event:token\ndata:{"text":" mundo"}\n\n' +
          `event:dishes\ndata:${JSON.stringify({ dishes: [dish] })}\n\n` +
          'event:done\ndata:{"outcome":"COMPLETED","iterations":1}\n\n',
      ),
    ),
  )
  const { calls, h } = handlers()

  await streamChat('oi', 'sessao-12345678', h, new AbortController().signal)

  expect(calls.tokens.join('')).toBe('Ola mundo')
  expect(calls.dishes).toEqual([7])
  expect(calls.done).toBe(1)
  expect(calls.errors).toEqual([])
})

it('reports the backend error message for a rejected request', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response(JSON.stringify({ error: 'Aguarde a resposta anterior' }), { status: 429 })),
  )
  const { calls, h } = handlers()

  await streamChat('oi', 'sessao-12345678', h, new AbortController().signal)

  expect(calls.errors).toEqual(['Aguarde a resposta anterior'])
})