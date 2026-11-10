import { describe, expect, it } from 'vitest'
import { createSseParser, type SseEvent } from './sse'

function collect() {
  const events: SseEvent[] = []
  return { events, parser: createSseParser((e) => events.push(e)) }
}

describe('createSseParser', () => {
  it('reassembles events split across chunks', () => {
    const { events, parser } = collect()

    parser.feed('event:token\ndata:{"te')
    parser.feed('xt":" mundo"}\n\nevent:done\ndata:{}\n\n')

    expect(events).toEqual([
      { event: 'token', data: '{"text":" mundo"}' },
      { event: 'done', data: '{}' },
    ])
  })

  it('handles CRLF, comments, a single space after the colon and multi-line data', () => {
    const { events, parser } = collect()

    parser.feed(': keep-alive\r\nevent: token\r\ndata: a\r\ndata: b\r\n\r\n')

    expect(events).toEqual([{ event: 'token', data: 'a\nb' }])
  })

  it('does not emit an incomplete event', () => {
    const { events, parser } = collect()

    parser.feed('event:token\ndata:{"text":"x"}\n')

    expect(events).toEqual([])
  })
})