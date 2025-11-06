import { useEffect, useMemo, useState } from 'react'
import PropTypes from 'prop-types'
import { LanguageContext } from './languageContextValue'

const STORAGE_KEY = 'cafeteria-lang'
const DEFAULT_LANG = 'es'

const translations = {
  es: {
    dashboard: 'Panel',
    inventory: 'Inventario',
    pos: 'Punto de Venta',
    reports: 'Reportes',
    profile: 'Perfil',
    settings: 'Ajustes',
    notifications: 'Notificaciones',
    salesToday: 'Ventas Hoy',
    ordersToday: 'Órdenes Hoy',
    ticketAverage: 'Ticket Promedio',
    productsAvailable: 'Productos Disponibles',
    theme: 'Tema',
    language: 'Idioma',
    enablePush: 'Habilitar Notificaciones Push',
    light: 'Claro',
    dark: 'Oscuro',
    spanish: 'Español',
    english: 'Inglés',
    loyaltyPoints: 'Puntos de Lealtad',
    ledger: 'Historial',
    export: 'Exportar',
    salesVsPeople: 'Ventas vs Afluencia',
  },
  en: {
    dashboard: 'Dashboard',
    inventory: 'Inventory',
    pos: 'POS',
    reports: 'Reports',
    profile: 'Profile',
    settings: 'Settings',
    notifications: 'Notifications',
    salesToday: 'Sales Today',
    ordersToday: 'Orders Today',
    ticketAverage: 'Average Ticket',
    productsAvailable: 'Products Available',
    theme: 'Theme',
    language: 'Language',
    enablePush: 'Enable Push Notifications',
    light: 'Light',
    dark: 'Dark',
    spanish: 'Spanish',
    english: 'English',
    loyaltyPoints: 'Loyalty Points',
    ledger: 'Ledger',
    export: 'Export',
    salesVsPeople: 'Sales vs Footfall',
  },
}

export const LanguageProvider = ({ children }) => {
  const [language, setLanguage] = useState(() => localStorage.getItem(STORAGE_KEY) || DEFAULT_LANG)

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, language)
  }, [language])

  const value = useMemo(() => {
    const dictionary = translations[language] || translations[DEFAULT_LANG]
    const t = (key) => dictionary[key] ?? key
    return { language, setLanguage, t }
  }, [language])

  return <LanguageContext.Provider value={value}>{children}</LanguageContext.Provider>
}

LanguageProvider.propTypes = {
  children: PropTypes.node,
}
