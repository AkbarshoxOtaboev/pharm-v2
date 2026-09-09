export function money(v?: number | string | null) {
  const n = Number(v ?? 0)
  return new Intl.NumberFormat('uz-UZ').format(n)
}

export function apiError(e: unknown, fallback = 'Xatolik yuz berdi') {
  const err = e as {
    response?: { data?: { error?: { message?: string } } }
    message?: string
  }
  return err.response?.data?.error?.message || err.message || fallback
}

export function statusTone(status?: string) {
  const s = (status || '').toUpperCase()
  if (['ACTIVE', 'DELIVERED', 'CONFIRMED', 'RETURNED'].includes(s)) return 'success' as const
  if (['NEW', 'PENDING', 'PROCESSING', 'SHIPPED'].includes(s)) return 'brand' as const
  if (['CANCELLED', 'DELETED', 'BLOCKED', 'INACTIVE'].includes(s)) return 'error' as const
  return 'gray' as const
}
