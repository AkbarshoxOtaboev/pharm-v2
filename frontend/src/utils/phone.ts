export const UZ_PHONE_PLACEHOLDER = '+998-(__)-___-__-__'

/** Returns up to 9 national digits (without the 998 country code). */
export function uzPhoneDigits(input: string): string {
  const value = input.trim()
  let digits = value.replace(/\D/g, '')
  if (value.startsWith('+998') || (digits.length > 9 && digits.startsWith('998'))) {
    digits = digits.slice(3)
  }
  return digits.slice(0, 9)
}

export function formatUzPhone(input: string): string {
  const d = uzPhoneDigits(input)
  if (!d) return ''
  let out = `+998-(${d.slice(0, 2)}`
  if (d.length > 2) out += `)-${d.slice(2, 5)}`
  if (d.length > 5) out += `-${d.slice(5, 7)}`
  if (d.length > 7) out += `-${d.slice(7, 9)}`
  return out
}
