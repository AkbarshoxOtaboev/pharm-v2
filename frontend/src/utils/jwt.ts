/** Decode JWT payload without verifying signature (client-side expiry checks only). */
export function parseJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const part = token.split('.')[1]
    if (!part) return null
    const json = atob(part.replace(/-/g, '+').replace(/_/g, '/'))
    return JSON.parse(json) as Record<string, unknown>
  } catch {
    return null
  }
}

export function getTokenExpMs(token: string | null | undefined): number | null {
  if (!token) return null
  const payload = parseJwtPayload(token)
  const exp = payload?.exp
  if (typeof exp !== 'number') return null
  return exp * 1000
}

/** True if token is missing, invalid, or expires within `skewMs`. */
export function isTokenExpired(token: string | null | undefined, skewMs = 0): boolean {
  const exp = getTokenExpMs(token)
  if (exp == null) return true
  return Date.now() >= exp - skewMs
}
