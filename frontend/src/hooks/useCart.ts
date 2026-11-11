import { useCallback, useEffect, useState } from 'react'
import { addToCart, clearCart, fetchCart, removeFromCart, setQuantity } from '../api/cart'
import type { Cart } from '../types'

const EMPTY: Cart = { items: [], totalItems: 0, totalCents: 0 }

function messageOf(e: unknown): string {
  return e instanceof Error ? e.message : 'Erro inesperado.'
}

export function useCart() {
  const [cart, setCart] = useState<Cart>(EMPTY)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const refresh = useCallback(async () => {
    try {
      setCart(await fetchCart())
      setError(null)
    } catch (e) {
      setError(messageOf(e))
    }
  }, [])

  const run = useCallback(async (action: () => Promise<Cart>) => {
    setBusy(true)
    try {
      setCart(await action())
      setError(null)
    } catch (e) {
      setError(messageOf(e))
    } finally {
      setBusy(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  return {
    cart,
    error,
    busy,
    refresh,
    add: (dishId: number) => run(() => addToCart(dishId)),
    changeQuantity: (dishId: number, quantity: number) => run(() => setQuantity(dishId, quantity)),
    remove: (dishId: number) => run(() => removeFromCart(dishId)),
    clear: () => run(() => clearCart()),
  }
}