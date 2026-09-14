import { computed } from 'vue'
import { i18n, isAppLocale, htmlLang, type AppLocale } from '@/i18n'

const STORAGE_KEY = 'pharm-locale'

function readStoredLocale(): AppLocale | null {
  try {
    const value = localStorage.getItem(STORAGE_KEY)
    if (isAppLocale(value)) return value
  } catch {
    /* ignore */
  }
  return null
}

export function applyLocale(locale: AppLocale) {
  i18n.global.locale.value = locale
  document.documentElement.lang = htmlLang(locale)
  try {
    localStorage.setItem(STORAGE_KEY, locale)
  } catch {
    /* ignore */
  }
}

export function initLocale() {
  applyLocale(readStoredLocale() || 'uz')
}

export function useLocale() {
  const locale = computed(() => i18n.global.locale.value as AppLocale)

  function setLocale(next: AppLocale) {
    applyLocale(next)
  }

  return { locale, setLocale }
}
