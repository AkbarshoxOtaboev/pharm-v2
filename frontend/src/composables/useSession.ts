import { onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { getTokenExpMs, isTokenExpired } from '@/utils/jwt'
import {
  IDLE_TIMEOUT_MS,
  REFRESH_BEFORE_EXPIRY_MS,
  getIdleRemainingMs,
  isSessionIdle,
  touchActivity,
} from '@/utils/sessionIdle'

const ACTIVITY_EVENTS = ['mousedown', 'mousemove', 'keydown', 'scroll', 'touchstart', 'click'] as const

let idleTimer: ReturnType<typeof setTimeout> | null = null
let refreshTimer: ReturnType<typeof setTimeout> | null = null
let started = false
let onForceLogout: (() => void) | null = null

function scheduleIdleCheck() {
  if (!started) return
  if (idleTimer) clearTimeout(idleTimer)
  const remaining = getIdleRemainingMs()
  idleTimer = setTimeout(() => {
    if (isSessionIdle()) {
      onForceLogout?.()
    } else {
      scheduleIdleCheck()
    }
  }, Math.max(1000, remaining))
}

function scheduleProactiveRefresh(getAccessToken: () => string | null, refresh: () => Promise<boolean>) {
  if (refreshTimer) clearTimeout(refreshTimer)
  const token = getAccessToken()
  const exp = getTokenExpMs(token)
  if (exp == null) return

  const runIn = exp - Date.now() - REFRESH_BEFORE_EXPIRY_MS
  if (runIn <= 0) {
    if (isSessionIdle()) {
      onForceLogout?.()
      return
    }
    void refresh().then((ok) => {
      if (ok) scheduleProactiveRefresh(getAccessToken, refresh)
      else onForceLogout?.()
    })
    return
  }

  refreshTimer = setTimeout(() => {
    if (isSessionIdle()) {
      onForceLogout?.()
      return
    }
    if (isTokenExpired(getAccessToken())) {
      void refresh().then((ok) => {
        if (ok) scheduleProactiveRefresh(getAccessToken, refresh)
        else onForceLogout?.()
      })
      return
    }
    void refresh().then((ok) => {
      if (ok) scheduleProactiveRefresh(getAccessToken, refresh)
    })
  }, runIn)
}

function onActivity() {
  touchActivity()
  scheduleIdleCheck()
}

/**
 * Start idle logout + proactive token refresh while the user is logged in.
 * Call once from App.vue.
 */
export function useSessionGuard() {
  const auth = useAuthStore()
  const router = useRouter()

  async function forceLogout() {
    await auth.clearSession()
    if (!window.location.pathname.includes('/login')) {
      await router.push('/login')
    }
  }

  function start() {
    if (started) return
    started = true
    onForceLogout = () => {
      void forceLogout()
    }
    for (const ev of ACTIVITY_EVENTS) {
      window.addEventListener(ev, onActivity, { passive: true })
    }
    touchActivity()
    scheduleIdleCheck()
    scheduleProactiveRefresh(
      () => auth.accessToken,
      () => auth.refreshTokens(),
    )
  }

  function stop() {
    started = false
    onForceLogout = null
    if (idleTimer) clearTimeout(idleTimer)
    if (refreshTimer) clearTimeout(refreshTimer)
    idleTimer = null
    refreshTimer = null
    for (const ev of ACTIVITY_EVENTS) {
      window.removeEventListener(ev, onActivity)
    }
  }

  watch(
    () => auth.isAuthenticated,
    (ok) => {
      if (ok) start()
      else stop()
    },
    { immediate: true },
  )

  watch(
    () => auth.accessToken,
    () => {
      if (auth.isAuthenticated && started) {
        scheduleProactiveRefresh(
          () => auth.accessToken,
          () => auth.refreshTokens(),
        )
      }
    },
  )

  onUnmounted(stop)
}

export { IDLE_TIMEOUT_MS, isSessionIdle, touchActivity }
