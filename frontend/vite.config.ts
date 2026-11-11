
import { defineConfig, type Plugin } from 'vite'
import react from '@vitejs/plugin-react'

const MOCK_DISHES = [
  {
    id: 1,
    name: 'Prato de exemplo 1',
    description: 'Descrição de teste.',
    priceCents: 3190,
    prepMinutes: 30,
    dietaryTags: ['vegetariano', 'sem-gluten'],
  },
  {
    id: 2,
    name: 'Prato de exemplo 2',
    description: 'Outra descrição de teste.',
    priceCents: 2790,
    prepMinutes: 20,
    dietaryTags: ['vegano'],
  },
  {
    id: 3,
    name: 'Prato de exemplo 3',
    description: 'Mais uma descrição.',
    priceCents: 3590,
    prepMinutes: 40,
    dietaryTags: [],
  },
]

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

        res.writeHead(200, {
          'Content-Type': 'text/event-stream',
          'Cache-Control': 'no-cache',
          Connection: 'keep-alive',
        })

        const words =
          'Esta é uma resposta simulada para testar o streaming da tela de chat.'
            .split(' ')

        let i = 0


        const timer = setInterval(() => {
          if (i < words.length) {
            const text = (i === 0 ? '' : ' ') + words[i++]

            res.write(
              `event:token\ndata:${JSON.stringify({ text })}\n\n`,
            )
          } else {
            // Envia os pratos simulados
            res.write(
              `event:dishes\ndata:${JSON.stringify({
                dishes: MOCK_DISHES,
              })}\n\n`,
            )

            // Finaliza o streaming
            res.write(
              'event:done\ndata:{"outcome":"COMPLETED","iterations":1}\n\n',
            )

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
  plugins: [
    react(),
    ...(mode === 'mock' ? [mockChat()] : []),
  ],

  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },

  define: {
    __MOCK_DISHES__: JSON.stringify(
      mode === 'mock' ? MOCK_DISHES : [],
    ),
  },
}))
