export type Dish = {
  id: number
  name: string
  description: string
  priceCents: number
  prepMinutes: number
  dietaryTags: string[]
}

export type CartLine = {
  dishId: number
  name: string
  unitPriceCents: number
  quantity: number
  lineTotalCents: number
}

export type Cart = { items: CartLine[]; totalItems: number; totalCents: number }