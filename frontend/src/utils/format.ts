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
  if (['PENDING', 'PROCESSING'].includes(s)) return 'warning' as const
  if (['NEW', 'SHIPPED'].includes(s)) return 'brand' as const
  if (['CANCELLED', 'DELETED', 'BLOCKED', 'INACTIVE'].includes(s)) return 'error' as const
  return 'gray' as const
}

export function statusSelectClass(status?: string) {
  const tone = statusTone(status)
  return {
    'border-brand-300 bg-brand-50 text-brand-700 focus:border-brand-300 focus:ring-brand-500/10 dark:border-brand-500/40 dark:bg-brand-500/15 dark:text-brand-400':
      tone === 'brand',
    'border-success-300 bg-success-50 text-success-700 focus:border-success-300 focus:ring-success-500/10 dark:border-success-500/40 dark:bg-success-500/15 dark:text-success-500':
      tone === 'success',
    'border-warning-300 bg-warning-50 text-warning-700 focus:border-warning-300 focus:ring-warning-500/10 dark:border-warning-500/40 dark:bg-warning-500/15 dark:text-warning-500':
      tone === 'warning',
    'border-error-300 bg-error-50 text-error-700 focus:border-error-300 focus:ring-error-500/10 dark:border-error-500/40 dark:bg-error-500/15 dark:text-error-500':
      tone === 'error',
    'border-gray-300 bg-gray-100 text-gray-700 focus:border-gray-300 focus:ring-gray-500/10 dark:border-gray-600 dark:bg-white/5 dark:text-gray-300':
      tone === 'gray',
  }
}
