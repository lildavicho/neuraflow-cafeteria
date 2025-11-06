import { useEffect, useMemo, useState } from 'react'
import PropTypes from 'prop-types'
import { ThemeContext } from './themeContextValue'

const STORAGE_KEY = 'cafeteria-theme'

const prefersDark = () => globalThis.matchMedia?.('(prefers-color-scheme: dark)').matches

export const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState(() => localStorage.getItem(STORAGE_KEY) || 'light')

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, theme)
    const root = document.documentElement
    if (theme === 'dark') {
      root.classList.add('dark')
    } else {
      root.classList.remove('dark')
    }
  }, [theme])

  const toggleTheme = () => setTheme((prev) => (prev === 'dark' ? 'light' : 'dark'))

  const value = useMemo(() => ({ theme, toggleTheme, setTheme }), [theme])

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>
}

ThemeProvider.propTypes = {
  children: PropTypes.node,
}
