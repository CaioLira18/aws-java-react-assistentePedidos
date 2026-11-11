import { describe, expect, it } from 'vitest'
import type { Dish } from '../types'
import { MAX_CARDS, mergeDishes, parseDishes } from './dishes'

const dish = (id: number): Dish => ({
  id,
  name: `Prato ${id}`,
  description: 'desc',
  priceCents: 1000,
  prepMinutes: 10,
  dietaryTags: [],
})

describe('parseDishes', () => {
  it('keeps valid dishes and drops malformed ones', () => {
    const parsed = parseDishes({ dishes: [dish(1), { id: 'x' }, null, { ...dish(2), priceCents: '10' }] })

    expect(parsed).toEqual([dish(1)])
  })

  it('returns an empty list for unexpected payloads', () => {
    expect(parseDishes(null)).toEqual([])
    expect(parseDishes({ dishes: 'nope' })).toEqual([])
  })
})

describe('mergeDishes', () => {
  it('dedupes by id and caps the number of cards', () => {
    const many = Array.from({ length: MAX_CARDS + 3 }, (_, i) => dish(i + 1))

    expect(mergeDishes([dish(1)], [dish(1), dish(2)]).map((d) => d.id)).toEqual([1, 2])
    expect(mergeDishes([], many)).toHaveLength(MAX_CARDS)
  })
})