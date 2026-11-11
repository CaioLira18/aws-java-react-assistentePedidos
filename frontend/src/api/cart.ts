import type { Cart } from '../types'
import { apiRequest } from './http'

export const MAX_QUANTITY_PER_ITEM = 10

export const fetchCart = () => apiRequest<Cart>('/api/cart')

export const addToCart = (dishId: number, quantity = 1) =>
  apiRequest<Cart>('/api/cart/items', { method: 'POST', body: JSON.stringify({ dishId, quantity }) })

export const setQuantity = (dishId: number, quantity: number) =>
  apiRequest<Cart>(`/api/cart/items/${dishId}`, { method: 'PUT', body: JSON.stringify({ quantity }) })

export const removeFromCart = (dishId: number) =>
  apiRequest<Cart>(`/api/cart/items/${dishId}`, { method: 'DELETE' })

export const clearCart = () => apiRequest<Cart>('/api/cart', { method: 'DELETE' })