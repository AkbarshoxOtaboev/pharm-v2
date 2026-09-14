/** No user activity for this long → logout (even if token still valid). */
export const IDLE_TIMEOUT_MS = 30 * 60 * 1000

/** Refresh access token this many ms before it expires, if user is active. */
export const REFRESH_BEFORE_EXPIRY_MS = 2 * 60 * 1000

let lastActivityAt = Date.now()

export function touchActivity() {
  lastActivityAt = Date.now()
}

export function isSessionIdle(timeoutMs = IDLE_TIMEOUT_MS): boolean {
  return Date.now() - lastActivityAt >= timeoutMs
}

export function getLastActivityAt() {
  return lastActivityAt
}

export function getIdleRemainingMs(timeoutMs = IDLE_TIMEOUT_MS): number {
  return timeoutMs - (Date.now() - lastActivityAt)
}
