import { ref } from 'vue'

export type ThemeMode = 'light' | 'dark'

const STORAGE_KEY = 'pharm-theme'
const theme = ref<ThemeMode>('light')

function readStoredTheme(): ThemeMode | null {
  try {
    const value = localStorage.getItem(STORAGE_KEY)
    if (value === 'dark' || value === 'light') return value
  } catch {
    /* ignore */
  }
  return null
}

export function applyTheme(mode: ThemeMode) {
  theme.value = mode
  const root = document.documentElement
  root.classList.toggle('dark', mode === 'dark')
  root.style.colorScheme = mode
  try {
    localStorage.setItem(STORAGE_KEY, mode)
  } catch {
    /* ignore */
  }
}

export function initTheme() {
  const stored = readStoredTheme()
  if (stored) {
    applyTheme(stored)
    return
  }
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches
  applyTheme(prefersDark ? 'dark' : 'light')
}

export function useTheme() {
  function toggleTheme() {
    applyTheme(theme.value === 'dark' ? 'light' : 'dark')
  }

  return { theme, toggleTheme, setTheme: applyTheme }
}
