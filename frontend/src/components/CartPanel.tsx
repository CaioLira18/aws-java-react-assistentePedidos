import { MAX_QUANTITY_PER_ITEM } from '../api/cart'
import { formatBrl } from '../lib/money'
import type { Cart } from '../types'
import './CartPanel.css'

type Props = {
  cart: Cart
  error: string | null
  busy: boolean
  onChangeQuantity: (dishId: number, quantity: number) => void
  onRemove: (dishId: number) => void
  onClear: () => void
}

export default function CartPanel({ cart, error, busy, onChangeQuantity, onRemove, onClear }: Props) {
  return (
    <aside className="cart" aria-label="Carrinho">
      <h2>Carrinho</h2>
      {error && (
        <p role="alert" className="cart-error">
          {error}
        </p>
      )}
      {cart.items.length === 0 ? (
        <p className="cart-empty">Seu carrinho está vazio.</p>
      ) : (
        <>
          <ul className="cart-lines">
            {cart.items.map((line) => (
              <li key={line.dishId}>
                <div className="line-name">{line.name}</div>
                <div className="line-controls">
                  <button
                    type="button"
                    aria-label={`Diminuir ${line.name}`}
                    disabled={busy || line.quantity <= 1}
                    onClick={() => onChangeQuantity(line.dishId, line.quantity - 1)}
                  >
                    −
                  </button>
                  <span>{line.quantity}</span>
                  <button
                    type="button"
                    aria-label={`Aumentar ${line.name}`}
                    disabled={busy || line.quantity >= MAX_QUANTITY_PER_ITEM}
                    onClick={() => onChangeQuantity(line.dishId, line.quantity + 1)}
                  >
                    +
                  </button>
                  <span className="line-total">{formatBrl(line.lineTotalCents)}</span>
                  <button
                    type="button"
                    aria-label={`Remover ${line.name}`}
                    disabled={busy}
                    onClick={() => onRemove(line.dishId)}
                  >
                    ✕
                  </button>
                </div>
              </li>
            ))}
          </ul>
          <div className="cart-total">
            <span>Total ({cart.totalItems} itens)</span>
            <strong>{formatBrl(cart.totalCents)}</strong>
          </div>
          <button type="button" disabled={busy} onClick={onClear}>
            Limpar carrinho
          </button>
        </>
      )}
    </aside>
  )
}