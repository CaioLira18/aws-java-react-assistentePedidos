import { useEffect, useState } from 'react'
import { getSessionId } from './session'
import './App.css'

type Status = 'loading' | 'ok' | 'error'

const LABELS: Record<Status, string> = {
  loading: 'verificando...',
  ok: 'conectado',
  error: 'indisponível',
}

function App() {
  const [status, setStatus] = useState<Status>('loading')

  useEffect(() => {
    const controller = new AbortController()
    fetch('/api/cart', {
      headers: { 'X-Session-Id': getSessionId() },
      signal: controller.signal,
    })
      .then((res) => setStatus(res.ok ? 'ok' : 'error'))
      .catch((err: Error) => {
        if (err.name !== 'AbortError') setStatus('error')
      })
    return () => controller.abort()
  }, [])

  return (
    <main>
      <h1>Assistente de Pedidos</h1>
      <p>Backend: {LABELS[status]}</p>
    </main>
  )
}

export default App