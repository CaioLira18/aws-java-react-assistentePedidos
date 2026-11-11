import type { Dish } from '../types'

export const MAX_CARDS = 6

function isDish(value: unknown): value is Dish {
  if (typeof value !== 'object' || value === null) return false
  const d = value as Record<string, unknown>
  return (
    typeof d.id === 'number' &&
    typeof d.name === 'string' &&
    typeof d.description === 'string' &&
    typeof d.priceCents === 'number' &&
    typeof d.prepMinutes === 'number' &&
    Array.isArray(d.dietaryTags) &&
    d.dietaryTags.every((tag) => typeof tag === 'string')
  )
}

export function parseDishes(data: unknown): Dish[] {
  if (typeof data !== 'object' || data === null || !('dishes' in data) || !Array.isArray(data.dishes)) {
    return []
  }
  return data.dishes.filter(isDish)
}

export function mergeDishes(current: Dish[], found: Dish[]): Dish[] {
  const ids = new Set(current.map((d) => d.id))
  const merged = [...current]
  for (const dish of found) {
    if (!ids.has(dish.id) && merged.length < MAX_CARDS) {
      ids.add(dish.id)
      merged.push(dish)
    }
  }
  return merged
}