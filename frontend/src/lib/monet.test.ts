import { expect, it } from 'vitest'
import { formatBrl } from './money'

it('formats cents as Brazilian reais', () => {
  expect(formatBrl(3190).replace(/\u00a0/g, ' ')).toBe('R$ 31,90')
  expect(formatBrl(0).replace(/\u00a0/g, ' ')).toBe('R$ 0,00')
})