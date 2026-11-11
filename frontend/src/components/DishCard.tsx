import { formatBrl } from '../lib/money'
import type { Dish } from '../types'
import './DishCard.css'

type Props = { dish: Dish; onAdd: (dishId: number) => void; disabled: boolean }

export default function DishCard({ dish, onAdd, disabled }: Props) {
  return (
    <article className="dish-card">
      <h3>{dish.name}</h3>
      <p className="dish-desc">{dish.description}</p>
      <ul className="tags">
        {dish.dietaryTags.map((tag) => (
          <li key={tag}>{tag.replace('-', ' ')}</li>
        ))}
      </ul>
      <div className="dish-footer">
        <span className="price">{formatBrl(dish.priceCents)}</span>
        <span className="prep">{dish.prepMinutes} min</span>
        <button type="button" onClick={() => onAdd(dish.id)} disabled={disabled}>
          Adicionar
        </button>
      </div>
    </article>
  )
}