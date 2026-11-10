import { useEffect, useRef, useState, type FormEvent } from 'react'
import { streamChat } from '../api/chatStream'
import { getSessionId } from '../session'
import './Chat.css'

const MAX_LENGTH = 500

type ChatMessage = {
  id: number
  role: 'user' | 'assistant'
  text: string
  failed?: boolean
}

export default function Chat() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [streaming, setStreaming] = useState(false)
  const nextId = useRef(1)
  const abortRef = useRef<AbortController | null>(null)
  const endRef = useRef<HTMLDivElement | null>(null)
  const inputRef = useRef<HTMLInputElement | null>(null)

  useEffect(() => () => abortRef.current?.abort(), [])

  useEffect(() => {
    endRef.current?.scrollIntoView({ block: 'end' })
  }, [messages])

  useEffect(() => {
    if (!streaming) inputRef.current?.focus()
  }, [streaming])

  async function send(event: FormEvent) {
    event.preventDefault()
    const text = input.trim()
    if (!text || streaming) return

    const assistantId = nextId.current + 1
    setMessages((prev) => [
      ...prev,
      { id: nextId.current, role: 'user', text },
      { id: assistantId, role: 'assistant', text: '' },
    ])
    nextId.current += 2
    setInput('')
    setStreaming(true)

    const controller = new AbortController()
    abortRef.current = controller

    const update = (change: (m: ChatMessage) => ChatMessage) =>
      setMessages((prev) => prev.map((m) => (m.id === assistantId ? change(m) : m)))

    await streamChat(
      text,
      getSessionId(),
      {
        onToken: (token) => update((m) => ({ ...m, text: m.text + token })),
        onDone: () => undefined,
        onError: (message) => update((m) => ({ ...m, text: message, failed: true })),
      },
      controller.signal,
    )

    if (controller.signal.aborted) {
      update((m) => ({ ...m, text: m.text || 'Resposta interrompida.' }))
    }
    abortRef.current = null
    setStreaming(false)
  }

  return (
    <section className="chat" aria-label="Chat com o assistente">
      <div className="chat-messages" role="log">
        {messages.length === 0 && (
          <p className="chat-hint">
            Peça uma sugestão, por exemplo: almoço vegetariano, sem glúten, até R$ 40, pronto em 30 minutos.
          </p>
        )}
        {messages.map((m) => (
          <div key={m.id} className={`bubble ${m.role}${m.failed ? ' failed' : ''}`}>
            {m.text || (streaming ? 'Pensando…' : '')}
          </div>
        ))}
        <div ref={endRef} />
      </div>

      <form className="chat-form" onSubmit={send}>
        <input
          ref={inputRef}
          value={input}
          onChange={(e) => setInput(e.target.value)}
          maxLength={MAX_LENGTH}
          placeholder="Digite sua mensagem"
          aria-label="Mensagem"
          disabled={streaming}
        />
        {streaming ? (
          <button type="button" onClick={() => abortRef.current?.abort()}>
            Parar
          </button>
        ) : (
          <button type="submit" disabled={!input.trim()}>
            Enviar
          </button>
        )}
      </form>
    </section>
  )
}