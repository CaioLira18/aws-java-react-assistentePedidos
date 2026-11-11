import CartPanel from './components/CartPanel'
import Chat from './components/Chat'
import { useCart } from './hooks/useCart'
import './App.css'

function App() {
  const { cart, error, busy, refresh, add, changeQuantity, remove, clear } = useCart()

  return (
    <main className="layout">
      <h1>Assistente de Pedidos</h1>
      <Chat onAddDish={add} cartBusy={busy} onTurnFinished={refresh} />
      <CartPanel
        cart={cart}
        error={error}
        busy={busy}
        onChangeQuantity={changeQuantity}
        onRemove={remove}
        onClear={clear}
      />
    </main>
  )
}

export default App