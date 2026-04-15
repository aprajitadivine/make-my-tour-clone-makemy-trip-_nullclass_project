import { createContext, useContext, useState, useCallback, useEffect } from 'react'
import axios from 'axios'
import { STORAGE_KEY_DARK_MODE, STORAGE_KEY_TOKEN } from '../constants'

/**
 * ThemeContext provides dark/light mode state and a toggle function.
 *
 * Dual-layer persistence strategy (Feature 5 – OLED-Optimized Dark Mode):
 *  1. localStorage – applied immediately on page load (no flash of wrong theme)
 *  2. User profile (PATCH /api/users/me/theme) – synced when authenticated so
 *     the preference persists across devices.
 *
 * The 'dark' class is toggled on the <html> element so Tailwind's
 * darkMode: 'class' strategy picks it up for all utility classes.
 */
const ThemeContext = createContext(null)

export function ThemeProvider({ children }) {
  const [isDark, setIsDark] = useState(() => {
    return localStorage.getItem(STORAGE_KEY_DARK_MODE) === 'true'
  })

  useEffect(() => {
    const root = document.documentElement
    if (isDark) {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }
    localStorage.setItem(STORAGE_KEY_DARK_MODE, String(isDark))
  }, [isDark])

  const toggleTheme = useCallback(async () => {
    const next = !isDark
    setIsDark(next)

    const token = localStorage.getItem(STORAGE_KEY_TOKEN)
    if (token) {
      try {
        await axios.patch(
          '/api/users/me/theme',
          { darkMode: next },
          { headers: { Authorization: `Bearer ${token}` } }
        )
      } catch {
        // Non-critical – localStorage already persists the preference
      }
    }
  }, [isDark])

  return (
    <ThemeContext.Provider value={{ isDark, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme must be used within a ThemeProvider')
  return ctx
}

export default ThemeContext
