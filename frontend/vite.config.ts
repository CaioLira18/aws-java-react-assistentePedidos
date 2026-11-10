import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'

function mockChat(): Plugin {
  return {
    name: 'mock-chat',
    configureServer(server) {
      server.middlewares.use('/api/chat', (req, res) => {
        if (req.method !== 'POST') {
          res.statusCode = 405
          res.end()
          return
        }
        req.resume()
        res.writeHead(200, { 'Content-Type': 'text/event-stream', 'Cache-Control': 'no-cache' })
        const words = 'Esta é uma resposta simulada para testar o streaming da tela de chat.'.split(' ')
        let i = 0
        const timer = setInterval(() => {
          if (i < words.length) {
            const text = (i === 0 ? '' : ' ') + words[i++]
            res.write(`event:token\ndata:${JSON.stringify({ text })}\n\n`)
          } else {
            res.write('event:done\ndata:{"outcome":"COMPLETED","iterations":1}\n\n')
            clearInterval(timer)
            res.end()
          }
        }, 120)
        res.on('close', () => clearInterval(timer))
      })
    },
  }
}

export default defineConfig(({ mode }) => ({
  plugins: [react(), ...(mode === 'mock' ? [mockChat()] : [])],
  server: {
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
}))