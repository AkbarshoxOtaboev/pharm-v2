import { createI18n } from 'vue-i18n'
import uz from './locales/uz.json'
import ru from './locales/ru.json'
import uzCyrl from './locales/uz-Cyrl.json'

export const SUPPORTED_LOCALES = ['uz', 'ru', 'uz-Cyrl'] as const
export type AppLocale = (typeof SUPPORTED_LOCALES)[number]

export const LOCALE_OPTIONS: { value: AppLocale; short: string; labelKey: string }[] = [
  { value: 'uz', short: 'O‘z', labelKey: 'lang.uz' },
  { value: 'ru', short: 'RU', labelKey: 'lang.ru' },
  { value: 'uz-Cyrl', short: 'Ўз', labelKey: 'lang.uzCyrl' },
]

export function isAppLocale(value: string | null | undefined): value is AppLocale {
  return value === 'uz' || value === 'ru' || value === 'uz-Cyrl'
}

export function htmlLang(locale: AppLocale) {
  return locale === 'ru' ? 'ru' : 'uz'
}

export function intlLocale(locale: AppLocale) {
  if (locale === 'ru') return 'ru-RU'
  if (locale === 'uz-Cyrl') return 'uz-Cyrl-UZ'
  return 'uz-UZ'
}

export const i18n = createI18n({
  legacy: false,
  locale: 'uz',
  fallbackLocale: 'uz',
  messages: {
    uz,
    ru,
    'uz-Cyrl': uzCyrl,
  },
})

export function enumLabel(group: string, value?: string | null, empty = '—') {
  if (!value) return empty
  const key = `enums.${group}.${value}`
  return i18n.global.te(key) ? String(i18n.global.t(key)) : value
}
