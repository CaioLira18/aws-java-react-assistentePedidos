import { getSessionId } from '../session'

export const GENERIC_ERROR = 'Não foi possível falar com o assistente. Tente novamente.'

export class ApiError extends Error {}

export async function readErrorMessage(res: Response): Promise<string> {
  try {
    const body: unknown = await res.json()
    if (typeof body === 'object' && body !== null && 'error' in body && typeof body.error === 'string') {
      return body.error
    }
  } catch {
    // o corpo não era JSON
  }
  return GENERIC_ERROR
}

export async function apiRequest<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: { 'Content-Type': 'application/json', 'X-Session-Id': getSessionId() },
  })
  if (!res.ok) throw new ApiError(await readErrorMessage(res))
  return (await res.json()) as T
}