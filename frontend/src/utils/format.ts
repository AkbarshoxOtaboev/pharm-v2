import { i18n } from '@/i18n'

export function money(v?: number | string | null) {
  const n = Number(v ?? 0)
  if (!Number.isFinite(n)) return '0'
  const negative = n < 0
  const abs = Math.abs(n)
  const [intRaw, frac] = abs.toString().split('.')
  const grouped = intRaw.replace(/\B(?=(\d{3})+(?!\d))/g, ' ')
  const body = frac ? `${grouped}.${frac}` : grouped
  return negative ? `-${body}` : body
}

export function apiError(e: unknown, fallback = 'errors.generic') {
  const { t, te } = i18n.global
  const err = e as {
    response?: { data?: { error?: { code?: string; message?: string } } }
    message?: string
  }
  const code = err.response?.data?.error?.code
  const message = err.response?.data?.error?.message || err.message

  if (code && te(`errors.codes.${code}`)) return String(t(`errors.codes.${code}`))
  if (message && te(`errors.codes.${message}`)) return String(t(`errors.codes.${message}`))
  if (message && te(message)) return String(t(message))

  if (message) {
    const mapped = mapBackendMessage(message)
    if (mapped) return mapped
  }

  if (te(fallback)) return String(t(fallback))
  return message || String(t('errors.generic'))
}

function mapBackendMessage(message: string) {
  const { t } = i18n.global
  const lower = message.toLowerCase()
  if (lower.includes('not found')) return String(t('errors.codes.NOT_FOUND'))
  if (lower.includes('login yoki parol') || lower.includes('неверный логин')) {
    return String(t('errors.codes.BAD_CREDENTIALS'))
  }
  if (lower.includes("ruxsat yo") || lower.includes('access denied') || lower.includes('нет доступа')) {
    return String(t('errors.codes.ACCESS_DENIED'))
  }
  if (
    lower.includes('недостаточно') ||
    lower.includes('not enough stock') ||
    lower.includes('yetarli emas')
  ) {
    return String(t('errors.insufficientStock'))
  }
  if (lower.includes("joriy parol") || lower.includes('текущий пароль')) {
    return String(t('errors.wrongCurrentPassword'))
  }
  if (lower.includes('refresh token')) return String(t('errors.invalidRefreshToken'))
  if (lower.includes('bloklangan') || lower.includes('заблокирован') || lower.includes('locked')) {
    return String(t('errors.codes.ACCOUNT_LOCKED'))
  }
  if (lower.includes('fayl yuklash') || lower.includes('could not') && lower.includes('file')) {
    return String(t('errors.fileUpload'))
  }
  return null
}

export function statusTone(status?: string) {
  const s = (status || '').toUpperCase()
  if (['ACTIVE', 'DELIVERED', 'CONFIRMED', 'RETURNED'].includes(s)) return 'success' as const
  if (['NEW', 'PENDING', 'PROCESSING', 'SHIPPED'].includes(s)) return 'brand' as const
  if (['CANCELLED', 'DELETED', 'BLOCKED', 'INACTIVE'].includes(s)) return 'error' as const
  return 'gray' as const
}
