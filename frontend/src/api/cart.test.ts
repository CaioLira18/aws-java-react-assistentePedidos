import { afterEach, expect, it, vi } from 'vitest'
import { addToCart } from './cart'

const emptyCart = { items: [], totalItems: 0, totalCents: 0 }

afterEach(() => vi.unstubAllGlobals())

it('sends the session header and the body', async () => {
  const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify(emptyCart), { status: 200 }))
  vi.stubGlobal('fetch', fetchMock)

  await addToCart(7, 2)

  const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit]
  expect(url).toBe('/api/cart/items')
  expect(init.method).toBe('POST')
  expect(init.body).toBe('{"dishId":7,"quantity":2}')
  expect((init.headers as Record<string, string>)['X-Session-Id']).toEqual(expect.any(String))
})

it('turns a backend error into the error message', async () => {
  vi.stubGlobal(
    'fetch',
    vi.fn().mockResolvedValue(new Response(JSON.stringify({ error: 'Máximo de 10 unidades' }), { status: 400 })),
  )

  await expect(addToCart(7)).rejects.toThrow('Máximo de 10 unidades')
})