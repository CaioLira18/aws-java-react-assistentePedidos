const brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function formatBrl(cents: number): string {
  return brl.format(cents / 100)
}