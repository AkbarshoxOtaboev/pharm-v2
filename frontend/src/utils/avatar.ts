export const DEFAULT_AVATARS = [
  'default-1',
  'default-2',
  'default-3',
  'default-4',
  'default-5',
  'default-6',
] as const

export type DefaultAvatar = (typeof DEFAULT_AVATARS)[number]

export function resolveAvatarUrl(avatar?: string | null): string {
  const value = (avatar || 'default-1').trim()
  if (value.startsWith('default-')) {
    return `/avatars/${value}.svg`
  }
  if (value.startsWith('http://') || value.startsWith('https://') || value.startsWith('/')) {
    return value
  }
  return `/api/v1/files/${value}`
}
